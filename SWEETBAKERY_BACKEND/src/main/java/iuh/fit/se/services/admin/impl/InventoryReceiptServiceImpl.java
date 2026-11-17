package iuh.fit.se.services.admin.impl;

import iuh.fit.se.dtos.request.InventoryReceiptItemRequest;
import iuh.fit.se.dtos.request.InventoryReceiptRequest;
import iuh.fit.se.dtos.response.InventoryReceiptItemResponse;
import iuh.fit.se.dtos.response.InventoryReceiptResponse;
import iuh.fit.se.entities.AccountCredential;
import iuh.fit.se.entities.InventoryReceipt;
import iuh.fit.se.entities.InventoryReceiptDetail;
import iuh.fit.se.entities.Pastry;
import iuh.fit.se.entities.User;
import iuh.fit.se.entities.enums.HttpCode;
import iuh.fit.se.entities.enums.PastryStatus;
import iuh.fit.se.exceptions.AppException;
import iuh.fit.se.repositories.AccountCredentialRepository;
import iuh.fit.se.repositories.InventoryReceiptRepository;
import iuh.fit.se.repositories.PastryRepository;
import iuh.fit.se.services.WarehouseInventoryService;
import iuh.fit.se.services.admin.InventoryReceiptService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryReceiptServiceImpl implements InventoryReceiptService {
    static final String INTERNAL_WORKSHOP = "Xưởng sản xuất nội bộ";

    InventoryReceiptRepository inventoryReceiptRepository;
    PastryRepository pastryRepository;
    AccountCredentialRepository accountCredentialRepository;
    WarehouseInventoryService warehouseInventoryService;

    @Override
    @Transactional
    public InventoryReceiptResponse create(InventoryReceiptRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(HttpCode.BAD_REQUEST);
        }
        String supplierName = request.getSupplierName();
        if (supplierName == null || supplierName.isBlank()) {
            supplierName = INTERNAL_WORKSHOP;
        } else {
            supplierName = supplierName.trim();
        }

        String receiptCode = request.getReceiptCode();
        if (receiptCode == null || receiptCode.isBlank()) {
            receiptCode = "NK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        }
        if (inventoryReceiptRepository.existsByReceiptCode(receiptCode)) {
            throw new AppException(HttpCode.BAD_REQUEST);
        }

        User staff = currentUser();
        InventoryReceipt receipt = new InventoryReceipt();
        receipt.setReceiptCode(receiptCode.trim());
        receipt.setSupplierName(supplierName);
        receipt.setCreatedBy(staff);
        receipt.setImportDate(LocalDateTime.now());

        List<InventoryReceiptDetail> details = new ArrayList<>();
        double total = 0.0;
        for (InventoryReceiptItemRequest item : request.getItems()) {
            if (item == null || item.getPastryId() == null || item.getQuantity() == null || item.getQuantity() <= 0
                    || item.getImportPrice() == null || item.getImportPrice() < 0) {
                throw new AppException(HttpCode.BAD_REQUEST);
            }
            Pastry pastry = pastryRepository.findById(item.getPastryId())
                    .orElseThrow(() -> new AppException(HttpCode.NOT_FOUND));
            int newStock = pastry.getStockQuantity() + item.getQuantity();
            pastry.setStockQuantity(newStock);
            if (newStock > 0 && pastry.getStatus() == PastryStatus.OUT_OF_STOCK) {
                pastry.setStatus(PastryStatus.ACTIVE);
            }
            pastryRepository.save(pastry);
            warehouseInventoryService.registerProduction(pastry, item.getQuantity(), receipt.getImportDate());

            InventoryReceiptDetail detail = new InventoryReceiptDetail();
            detail.setReceipt(receipt);
            detail.setProduct(pastry);
            detail.setQuantity(item.getQuantity());
            detail.setImportPrice(item.getImportPrice());
            details.add(detail);
            total += item.getQuantity() * item.getImportPrice();
        }

        receipt.setTotalAmount(total);
        receipt.setItems(details);
        return toResponse(inventoryReceiptRepository.save(receipt));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryReceiptResponse> findAll(Pageable pageable) {
        return inventoryReceiptRepository.findAll(pageable).map(this::toResponse);
    }

    private InventoryReceiptResponse toResponse(InventoryReceipt receipt) {
        List<InventoryReceiptItemResponse> items = new ArrayList<>();
        if (receipt.getItems() != null) {
            for (InventoryReceiptDetail detail : receipt.getItems()) {
                Pastry pastry = detail.getProduct();
                double line = detail.getQuantity() * detail.getImportPrice();
                items.add(InventoryReceiptItemResponse.builder()
                        .id(detail.getId())
                        .pastryId(pastry != null ? pastry.getId() : null)
                        .pastryName(pastry != null ? pastry.getName() : null)
                        .quantity(detail.getQuantity())
                        .importPrice(detail.getImportPrice())
                        .lineAmount(line)
                        .build());
            }
        }
        User creator = receipt.getCreatedBy();
        String creatorName = null;
        if (creator != null) {
            String first = creator.getFirstName() == null ? "" : creator.getFirstName();
            String last = creator.getLastName() == null ? "" : creator.getLastName();
            creatorName = (first + " " + last).trim();
            if (creatorName.isBlank()) {
                creatorName = creator.getId();
            }
        }
        return InventoryReceiptResponse.builder()
                .id(receipt.getId())
                .receiptCode(receipt.getReceiptCode())
                .supplierName(receipt.getSupplierName())
                .totalAmount(receipt.getTotalAmount())
                .createdByName(creatorName)
                .importDate(receipt.getImportDate())
                .items(items)
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
