package iuh.fit.se.services.admin.impl;

import iuh.fit.se.dtos.response.EmployeePerformanceReportResponse;
import iuh.fit.se.dtos.response.PerformanceBucketResponse;
import iuh.fit.se.dtos.response.StaffPerformanceResponse;
import iuh.fit.se.entities.InventoryReceipt;
import iuh.fit.se.entities.InventoryReceiptDetail;
import iuh.fit.se.entities.Order;
import iuh.fit.se.entities.Pastry;
import iuh.fit.se.entities.Role;
import iuh.fit.se.entities.StockAdjustment;
import iuh.fit.se.entities.User;
import iuh.fit.se.entities.enums.PaymentStatus;
import iuh.fit.se.repositories.InventoryReceiptRepository;
import iuh.fit.se.repositories.OrderRepository;
import iuh.fit.se.repositories.StockAdjustmentRepository;
import iuh.fit.se.repositories.UserRepository;
import iuh.fit.se.services.admin.EmployeePerformanceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployeePerformanceServiceImpl implements EmployeePerformanceService {
    OrderRepository orderRepository;
    InventoryReceiptRepository inventoryReceiptRepository;
    StockAdjustmentRepository stockAdjustmentRepository;
    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public EmployeePerformanceReportResponse report() {
        List<Order> orders = orderRepository.findAll();
        List<InventoryReceipt> receipts = inventoryReceiptRepository.findAll();
        List<StockAdjustment> adjustments = stockAdjustmentRepository.findAll();

        long paidOrders = orders.stream().filter(order -> order.getPaymentStatus() == PaymentStatus.PAID).count();
        double fulfillmentRate = orders.isEmpty() ? 0.0 : paidOrders * 100.0 / orders.size();
        double paidRevenue = orders.stream()
                .filter(order -> order.getPaymentStatus() == PaymentStatus.PAID)
                .mapToDouble(Order::getTongTien)
                .sum();
        double importTotal = receipts.stream().mapToDouble(receipt -> value(receipt.getTotalAmount())).sum();

        List<StaffPerformanceResponse> staff = new ArrayList<>();
        long salesTx = 0;
        double salesValue = 0;
        double salesRatingSum = 0;
        int salesCount = 0;
        long importTx = 0;
        double importValue = 0;
        long importItems = 0;
        int importCount = 0;
        long inventoryTx = 0;
        double inventoryValue = 0;
        long lowStockResolved = 0;
        int inventoryCount = 0;

        for (User user : userRepository.findAll()) {
            String role = primaryRole(user);
            if (role == null) {
                continue;
            }
            StaffPerformanceResponse card = switch (role) {
                case "SALES" -> salesCard(user, orders);
                case "IMPORT_STAFF" -> importCard(user, receipts, importTotal);
                case "INVENTORY_MANAGER" -> inventoryCard(user, adjustments);
                default -> accountantCard(user, orders.size(), paidRevenue, fulfillmentRate);
            };
            staff.add(card);
            switch (role) {
                case "SALES" -> {
                    salesCount++;
                    salesTx += card.getTotalTransactions();
                    salesValue += card.getTotalValueManaged();
                    salesRatingSum += card.getPerformanceRating();
                }
                case "IMPORT_STAFF" -> {
                    importCount++;
                    importTx += card.getTotalTransactions();
                    importValue += card.getTotalValueManaged();
                    importItems += itemsFor(user, receipts);
                }
                case "INVENTORY_MANAGER" -> {
                    inventoryCount++;
                    inventoryTx += card.getTotalTransactions();
                    inventoryValue += card.getTotalValueManaged();
                    lowStockResolved += resolvedFor(user, adjustments);
                }
                default -> {
                }
            }
        }

        staff.sort(Comparator
                .comparing(StaffPerformanceResponse::getPerformanceRating, Comparator.reverseOrder())
                .thenComparing(StaffPerformanceResponse::getTotalValueManaged, Comparator.reverseOrder()));

        return EmployeePerformanceReportResponse.builder()
                .staff(staff)
                .sales(PerformanceBucketResponse.builder()
                        .staffCount((long) salesCount)
                        .totalTransactions(salesTx)
                        .totalValueManaged(salesValue)
                        .metric(salesCount == 0 ? 0.0 : salesRatingSum / salesCount)
                        .build())
                .importStaff(PerformanceBucketResponse.builder()
                        .staffCount((long) importCount)
                        .totalTransactions(importTx)
                        .totalValueManaged(importValue)
                        .metric((double) importItems)
                        .build())
                .inventory(PerformanceBucketResponse.builder()
                        .staffCount((long) inventoryCount)
                        .totalTransactions(inventoryTx)
                        .totalValueManaged(inventoryValue)
                        .metric((double) lowStockResolved)
                        .build())
                .fulfillmentRate(fulfillmentRate)
                .build();
    }

    private StaffPerformanceResponse salesCard(User user, List<Order> orders) {
        List<Order> owned = orders.stream()
                .filter(order -> order.getProcessedByEmployee() != null
                        && user.getId().equals(order.getProcessedByEmployee().getId()))
                .toList();
        long paid = owned.stream().filter(this::paidByCashOrQr).count();
        double revenue = owned.stream()
                .filter(order -> order.getPaymentStatus() == PaymentStatus.PAID)
                .mapToDouble(Order::getTongTien)
                .sum();
        double rating = owned.isEmpty() ? 0.0 : paid * 100.0 / owned.size();
        return card(user, "SALES", owned.size(), revenue, rating);
    }

    private StaffPerformanceResponse importCard(User user, List<InventoryReceipt> receipts, double importTotal) {
        List<InventoryReceipt> owned = receipts.stream()
                .filter(receipt -> receipt.getCreatedBy() != null && user.getId().equals(receipt.getCreatedBy().getId()))
                .toList();
        double spent = owned.stream().mapToDouble(receipt -> value(receipt.getTotalAmount())).sum();
        double rating = importTotal <= 0 ? 0.0 : spent * 100.0 / importTotal;
        return card(user, "IMPORT_STAFF", owned.size(), spent, rating);
    }

    private StaffPerformanceResponse inventoryCard(User user, List<StockAdjustment> adjustments) {
        List<StockAdjustment> owned = adjustments.stream()
                .filter(adjustment -> adjustment.getAdjustedBy() != null
                        && user.getId().equals(adjustment.getAdjustedBy().getId()))
                .toList();
        long resolved = owned.stream().filter(this::resolvedLowStock).count();
        double units = owned.stream()
                .mapToDouble(adjustment -> Math.abs(adjustment.getNewQuantity() - adjustment.getOldQuantity()))
                .sum();
        double rating = owned.isEmpty() ? 0.0 : resolved * 100.0 / owned.size();
        return card(user, "INVENTORY_MANAGER", owned.size(), units, rating);
    }

    private StaffPerformanceResponse accountantCard(User user, int orderCount, double paidRevenue, double fulfillmentRate) {
        return card(user, "ACCOUNTANT", orderCount, paidRevenue, fulfillmentRate);
    }

    private long itemsFor(User user, List<InventoryReceipt> receipts) {
        long total = 0;
        for (InventoryReceipt receipt : receipts) {
            if (receipt.getCreatedBy() == null || !user.getId().equals(receipt.getCreatedBy().getId())) {
                continue;
            }
            if (receipt.getItems() == null) {
                continue;
            }
            for (InventoryReceiptDetail detail : receipt.getItems()) {
                total += detail.getQuantity();
            }
        }
        return total;
    }

    private long resolvedFor(User user, List<StockAdjustment> adjustments) {
        return adjustments.stream()
                .filter(adjustment -> adjustment.getAdjustedBy() != null
                        && user.getId().equals(adjustment.getAdjustedBy().getId()))
                .filter(this::resolvedLowStock)
                .count();
    }

    private boolean resolvedLowStock(StockAdjustment adjustment) {
        Pastry pastry = adjustment.getProduct();
        int threshold = pastry == null ? 0 : pastry.getMinThreshold();
        return adjustment.getOldQuantity() <= threshold && adjustment.getNewQuantity() > threshold;
    }

    private boolean paidByCashOrQr(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            return false;
        }
        String method = order.getPaymentMethod() == null ? "" : order.getPaymentMethod().trim().toUpperCase();
        return method.equals("CASH")
                || method.equals("TIEN_MAT")
                || method.contains("QR")
                || method.contains("VNPAY")
                || method.contains("VN_PAY")
                || method.contains("BANK")
                || method.contains("TRANSFER");
    }

    private StaffPerformanceResponse card(User user, String role, long transactions, double value, double rating) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        String name = (first + " " + last).trim();
        if (name.isBlank()) {
            name = user.getEmail() != null && !user.getEmail().isBlank() ? user.getEmail() : user.getId();
        }
        return StaffPerformanceResponse.builder()
                .employeeId(user.getId())
                .fullName(name)
                .role(role)
                .totalTransactions(transactions)
                .totalValueManaged(value)
                .performanceRating(Math.round(rating * 10.0) / 10.0)
                .build();
    }

    private String primaryRole(User user) {
        if (user.getRoles() == null) {
            return null;
        }
        Set<String> names = user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet());
        if (names.contains("SALES")) {
            return "SALES";
        }
        if (names.contains("IMPORT_STAFF")) {
            return "IMPORT_STAFF";
        }
        if (names.contains("INVENTORY_MANAGER")) {
            return "INVENTORY_MANAGER";
        }
        if (names.contains("ACCOUNTANT")) {
            return "ACCOUNTANT";
        }
        return null;
    }

    private double value(Double amount) {
        return amount == null ? 0.0 : amount;
    }
}
