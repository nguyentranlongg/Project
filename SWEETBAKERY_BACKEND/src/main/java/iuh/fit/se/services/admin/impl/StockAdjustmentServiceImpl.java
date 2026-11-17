package iuh.fit.se.services.admin.impl;

import iuh.fit.se.dtos.request.StockAdjustmentRequest;
import iuh.fit.se.dtos.response.LowStockPastryResponse;
import iuh.fit.se.dtos.response.StockAdjustmentResponse;
import iuh.fit.se.entities.AccountCredential;
import iuh.fit.se.entities.Pastry;
import iuh.fit.se.entities.StockAdjustment;
import iuh.fit.se.entities.User;
import iuh.fit.se.entities.enums.HttpCode;
import iuh.fit.se.entities.enums.PastryStatus;
import iuh.fit.se.exceptions.AppException;
import iuh.fit.se.repositories.AccountCredentialRepository;
import iuh.fit.se.repositories.PastryRepository;
import iuh.fit.se.repositories.StockAdjustmentRepository;
import iuh.fit.se.services.WarehouseInventoryService;
import iuh.fit.se.services.admin.StockAdjustmentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockAdjustmentServiceImpl implements StockAdjustmentService {
    StockAdjustmentRepository stockAdjustmentRepository;
    PastryRepository pastryRepository;
    AccountCredentialRepository accountCredentialRepository;
    WarehouseInventoryService warehouseInventoryService;

    @Override
    @Transactional(readOnly = true)
    public List<LowStockPastryResponse> findLowStock() {
        return pastryRepository.findLowStock().stream()
                .map(pastry -> LowStockPastryResponse.builder()
                        .id(pastry.getId())
                        .name(pastry.getName())
                        .stockQuantity(pastry.getStockQuantity())
                        .minThreshold(pastry.getMinThreshold())
                        .status(pastry.getStatus())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public StockAdjustmentResponse adjust(StockAdjustmentRequest request) {
        if (request == null || request.getPastryId() == null || request.getPastryId().isBlank()
                || request.getNewQuantity() == null || request.getNewQuantity() < 0
                || request.getReason() == null || request.getReason().isBlank()) {
            throw new AppException(HttpCode.BAD_REQUEST);
        }

        Pastry pastry = pastryRepository.findById(request.getPastryId())
                .orElseThrow(() -> new AppException(HttpCode.NOT_FOUND));
        int oldQuantity = pastry.getStockQuantity();
        int newQuantity = request.getNewQuantity();
        pastry.setStockQuantity(newQuantity);
        if (newQuantity <= 0) {
            pastry.setStatus(PastryStatus.OUT_OF_STOCK);
        } else if (pastry.getStatus() == PastryStatus.OUT_OF_STOCK) {
            pastry.setStatus(PastryStatus.ACTIVE);
        }
        pastryRepository.save(pastry);
        warehouseInventoryService.syncAdjustment(pastry, oldQuantity, newQuantity);

        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setProduct(pastry);
        adjustment.setOldQuantity(oldQuantity);
        adjustment.setNewQuantity(newQuantity);
        adjustment.setReason(request.getReason().trim());
        adjustment.setAdjustedBy(currentUser());
        adjustment.setAdjustedAt(LocalDateTime.now());
        return toResponse(stockAdjustmentRepository.save(adjustment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentResponse> findAll() {
        return stockAdjustmentRepository.findAllByOrderByAdjustedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private StockAdjustmentResponse toResponse(StockAdjustment adjustment) {
        Pastry pastry = adjustment.getProduct();
        User staff = adjustment.getAdjustedBy();
        String staffName = null;
        if (staff != null) {
            String first = staff.getFirstName() == null ? "" : staff.getFirstName();
            String last = staff.getLastName() == null ? "" : staff.getLastName();
            staffName = (first + " " + last).trim();
            if (staffName.isBlank()) {
                staffName = staff.getId();
            }
        }
        return StockAdjustmentResponse.builder()
                .id(adjustment.getId())
                .pastryId(pastry != null ? pastry.getId() : null)
                .pastryName(pastry != null ? pastry.getName() : null)
                .oldQuantity(adjustment.getOldQuantity())
                .newQuantity(adjustment.getNewQuantity())
                .reason(adjustment.getReason())
                .adjustedByName(staffName)
                .adjustedAt(adjustment.getAdjustedAt())
                .build();
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AppException(HttpCode.UNAUTHENTICATED);
        }
        AccountCredential account = accountCredentialRepository.findByCredential(authentication.getName());
        if (account == null || account.getUser() == null) {
            throw new AppException(HttpCode.UNAUTHENTICATED);
        }
        return account.getUser();
    }
}
