package iuh.fit.se.services.impl;

import iuh.fit.se.dtos.response.PastryCreationResponse;
import iuh.fit.se.dtos.response.WarehouseNotificationResponse;
import iuh.fit.se.entities.Pastry;
import iuh.fit.se.entities.PastryBatch;
import iuh.fit.se.entities.WarehouseNotification;
import iuh.fit.se.entities.enums.HttpCode;
import iuh.fit.se.entities.enums.PastryStatus;
import iuh.fit.se.entities.enums.WarehouseAlertType;
import iuh.fit.se.exceptions.AppException;
import iuh.fit.se.repositories.PastryBatchRepository;
import iuh.fit.se.repositories.PastryRepository;
import iuh.fit.se.repositories.WarehouseNotificationRepository;
import iuh.fit.se.services.WarehouseInventoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WarehouseInventoryServiceImpl implements WarehouseInventoryService {
    PastryRepository pastryRepository;
    PastryBatchRepository pastryBatchRepository;
    WarehouseNotificationRepository warehouseNotificationRepository;

    @Override
    @Transactional
    public void consume(Pastry pastry, int quantity) {
        expirePastry(pastry);
        if (pastry.getStockQuantity() < quantity) {
            throw new RuntimeException("Sản phẩm " + pastry.getName()
                    + " không đủ số lượng tồn kho (Còn: " + pastry.getStockQuantity() + ")");
        }
        deductBatches(pastry, quantity);
        int newStock = pastry.getStockQuantity() - quantity;
        pastry.setStockQuantity(newStock);
        if (newStock <= 0) {
            pastry.setStatus(PastryStatus.OUT_OF_STOCK);
        }
        pastryRepository.save(pastry);
        alertIfLow(pastry);
    }

    @Override
    @Transactional
    public void registerProduction(Pastry pastry, int quantity, LocalDateTime producedAt) {
        PastryBatch batch = new PastryBatch();
        batch.setPastry(pastry);
        batch.setQuantityRemaining(quantity);
        batch.setProducedAt(producedAt);
        int days = pastry.getShelfLifeDays() == null || pastry.getShelfLifeDays() <= 0 ? 2 : pastry.getShelfLifeDays();
        batch.setExpiryDate(producedAt.plusDays(days));
        pastryBatchRepository.save(batch);
    }

    @Override
    @Transactional
    public void syncAdjustment(Pastry pastry, int oldQuantity, int newQuantity) {
        if (newQuantity < oldQuantity) {
            deductBatches(pastry, oldQuantity - newQuantity);
        }
        if (newQuantity <= 0) {
            zeroBatches(pastry);
        }
        alertIfLow(pastry);
    }

    @Override
    @Scheduled(fixedDelay = 900000)
    @Transactional
    public void expireDueBatches() {
        List<PastryBatch> expired = pastryBatchRepository
                .findByQuantityRemainingGreaterThanAndExpiryDateBefore(0, LocalDateTime.now());
        Map<String, Pastry> touched = new HashMap<>();
        for (PastryBatch batch : expired) {
            Pastry pastry = batch.getPastry();
            if (pastry == null) {
                continue;
            }
            int removed = batch.getQuantityRemaining();
            batch.setQuantityRemaining(0);
            pastryBatchRepository.save(batch);
            int newStock = Math.max(0, pastry.getStockQuantity() - removed);
            pastry.setStockQuantity(newStock);
            if (newStock <= 0 && pastry.getStatus() != PastryStatus.DISCONTINUED) {
                pastry.setStatus(PastryStatus.OUT_OF_STOCK);
            }
            pastryRepository.save(pastry);
            touched.put(pastry.getId(), pastry);
        }
        for (Pastry pastry : touched.values()) {
            if (!warehouseNotificationRepository.existsByPastryIdAndTypeAndAcknowledgedIsFalse(
                    pastry.getId(), WarehouseAlertType.EXPIRY)) {
                saveAlert(WarehouseAlertType.EXPIRY, pastry,
                        "Sản phẩm " + pastry.getName() + " đã hết hạn và được đưa khỏi tủ bán.");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void attachNearestExpiry(List<PastryCreationResponse> pastries) {
        Map<String, LocalDateTime> nearest = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (PastryBatch batch : pastryBatchRepository.findByQuantityRemainingGreaterThan(0)) {
            if (batch.getPastry() == null || batch.getExpiryDate() == null) {
                continue;
            }
            String pastryId = batch.getPastry().getId();
            LocalDateTime current = nearest.get(pastryId);
            if (current == null || batch.getExpiryDate().isBefore(current)) {
                nearest.put(pastryId, batch.getExpiryDate());
            }
            if (batch.getExpiryDate().isBefore(now) && (current == null || batch.getExpiryDate().isBefore(current))) {
                nearest.put(pastryId, batch.getExpiryDate());
            }
        }
        for (PastryCreationResponse pastry : pastries) {
            pastry.setNearestExpiryDate(nearest.get(pastry.getId()));
        }
    }

    @Override
    @Transactional
    public WarehouseNotificationResponse requestProduction(String pastryId) {
        Pastry pastry = pastryRepository.findById(pastryId).orElseThrow(() -> new AppException(HttpCode.NOT_FOUND));
        if (warehouseNotificationRepository.existsByPastryIdAndTypeAndAcknowledgedIsFalse(
                pastry.getId(), WarehouseAlertType.PRODUCTION_REQUEST)) {
            throw new AppException(HttpCode.BAD_REQUEST);
        }
        String message = "Yêu cầu bếp nướng: " + pastry.getName()
                + " (Tồn: " + pastry.getStockQuantity() + ", Ngưỡng: " + pastry.getMinThreshold() + ").";
        return toResponse(saveAlert(WarehouseAlertType.PRODUCTION_REQUEST, pastry, message));
    }

    @Override
    @Transactional
    public List<WarehouseNotificationResponse> listNotifications() {
        syncCurrentLowStockAlerts();
        return warehouseNotificationRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public WarehouseNotificationResponse acknowledge(String notificationId) {
        WarehouseNotification notification = warehouseNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(HttpCode.NOT_FOUND));
        notification.setAcknowledged(true);
        return toResponse(warehouseNotificationRepository.save(notification));
    }

    private void expirePastry(Pastry pastry) {
        List<PastryBatch> batches = pastryBatchRepository
                .findByPastry_IdAndQuantityRemainingGreaterThanOrderByExpiryDateAsc(pastry.getId(), 0);
        LocalDateTime now = LocalDateTime.now();
        int removed = 0;
        for (PastryBatch batch : batches) {
            if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(now)) {
                removed += batch.getQuantityRemaining();
                batch.setQuantityRemaining(0);
                pastryBatchRepository.save(batch);
            }
        }
        if (removed > 0) {
            pastry.setStockQuantity(Math.max(0, pastry.getStockQuantity() - removed));
            if (pastry.getStockQuantity() <= 0 && pastry.getStatus() != PastryStatus.DISCONTINUED) {
                pastry.setStatus(PastryStatus.OUT_OF_STOCK);
            }
            pastryRepository.save(pastry);
        }
    }

    private void deductBatches(Pastry pastry, int quantity) {
        int left = quantity;
        LocalDateTime now = LocalDateTime.now();
        List<PastryBatch> batches = pastryBatchRepository
                .findByPastry_IdAndQuantityRemainingGreaterThanOrderByExpiryDateAsc(pastry.getId(), 0);
        for (PastryBatch batch : batches) {
            if (left <= 0) {
                break;
            }
            if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(now)) {
                continue;
            }
            int take = Math.min(left, batch.getQuantityRemaining());
            batch.setQuantityRemaining(batch.getQuantityRemaining() - take);
            pastryBatchRepository.save(batch);
            left -= take;
        }
    }

    private void zeroBatches(Pastry pastry) {
        List<PastryBatch> batches = pastryBatchRepository
                .findByPastry_IdAndQuantityRemainingGreaterThanOrderByExpiryDateAsc(pastry.getId(), 0);
        for (PastryBatch batch : batches) {
            batch.setQuantityRemaining(0);
            pastryBatchRepository.save(batch);
        }
    }

    private void syncCurrentLowStockAlerts() {
        for (Pastry pastry : pastryRepository.findLowStock()) {
            alertIfLow(pastry);
        }
    }

    private void alertIfLow(Pastry pastry) {
        if (pastry.getStockQuantity() > pastry.getMinThreshold()) {
            return;
        }
        if (warehouseNotificationRepository.existsByPastryIdAndTypeAndAcknowledgedIsFalse(
                pastry.getId(), WarehouseAlertType.LOW_STOCK)) {
            return;
        }
        String message = "Cảnh báo: Sản phẩm " + pastry.getName()
                + " sắp hết hàng (Tồn: " + pastry.getStockQuantity()
                + ", Ngưỡng: " + pastry.getMinThreshold() + ").";
        saveAlert(WarehouseAlertType.LOW_STOCK, pastry, message);
    }

    private WarehouseNotification saveAlert(WarehouseAlertType type, Pastry pastry, String message) {
        WarehouseNotification notification = new WarehouseNotification();
        notification.setType(type);
        notification.setMessage(message);
        notification.setPastryId(pastry.getId());
        notification.setPastryName(pastry.getName());
        notification.setAcknowledged(false);
        notification.setCreatedAt(LocalDateTime.now());
        return warehouseNotificationRepository.save(notification);
    }

    private WarehouseNotificationResponse toResponse(WarehouseNotification notification) {
        return WarehouseNotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .pastryId(notification.getPastryId())
                .pastryName(notification.getPastryName())
                .acknowledged(notification.isAcknowledged())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
