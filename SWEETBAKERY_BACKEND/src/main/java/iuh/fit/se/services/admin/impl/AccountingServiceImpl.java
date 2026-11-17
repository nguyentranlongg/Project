package iuh.fit.se.services.admin.impl;

import iuh.fit.se.dtos.response.AccountingTransactionResponse;
import iuh.fit.se.dtos.response.FinancialSummaryResponse;
import iuh.fit.se.dtos.response.PaymentBreakdownResponse;
import iuh.fit.se.entities.InventoryReceipt;
import iuh.fit.se.entities.Order;
import iuh.fit.se.entities.User;
import iuh.fit.se.entities.enums.PaymentStatus;
import iuh.fit.se.repositories.InventoryReceiptRepository;
import iuh.fit.se.repositories.OrderRepository;
import iuh.fit.se.services.admin.AccountingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountingServiceImpl implements AccountingService {
    static final int RECENT_LIMIT = 50;

    OrderRepository orderRepository;
    InventoryReceiptRepository inventoryReceiptRepository;

    @Override
    @Transactional(readOnly = true)
    public FinancialSummaryResponse financialSummary() {
        double totalRevenue = value(orderRepository.sumTongTienByPaymentStatus(PaymentStatus.PAID));
        double totalImportCost = value(inventoryReceiptRepository.sumTotalAmount());

        List<Order> paidOrders = orderRepository.findByPaymentStatusOrderByNgayDatHangDesc(PaymentStatus.PAID);
        Map<String, Double> buckets = new LinkedHashMap<>();
        buckets.put("CASH", 0.0);
        buckets.put("VIETQR", 0.0);
        for (Order order : paidOrders) {
            String method = paymentBucket(order.getPaymentMethod());
            buckets.merge(method, order.getTongTien(), Double::sum);
        }
        List<PaymentBreakdownResponse> breakdown = buckets.entrySet().stream()
                .map(entry -> PaymentBreakdownResponse.builder()
                        .method(entry.getKey())
                        .amount(entry.getValue())
                        .build())
                .toList();

        List<AccountingTransactionResponse> transactions = new ArrayList<>();
        for (Order order : paidOrders) {
            transactions.add(AccountingTransactionResponse.builder()
                    .type("REVENUE")
                    .reference(order.getId())
                    .description(customerLabel(order.getCustomer()))
                    .paymentMethod(paymentBucket(order.getPaymentMethod()))
                    .amount(order.getTongTien())
                    .occurredAt(order.getNgayDatHang())
                    .build());
        }
        for (InventoryReceipt receipt : inventoryReceiptRepository.findAllByOrderByImportDateDesc()) {
            transactions.add(AccountingTransactionResponse.builder()
                    .type("EXPENSE")
                    .reference(receipt.getReceiptCode())
                    .description(receipt.getSupplierName())
                    .paymentMethod(null)
                    .amount(receipt.getTotalAmount() == null ? 0.0 : receipt.getTotalAmount())
                    .occurredAt(receipt.getImportDate())
                    .build());
        }
        transactions.sort(Comparator.comparing(
                AccountingTransactionResponse::getOccurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        if (transactions.size() > RECENT_LIMIT) {
            transactions = new ArrayList<>(transactions.subList(0, RECENT_LIMIT));
        }

        return FinancialSummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .totalImportCost(totalImportCost)
                .grossProfit(totalRevenue - totalImportCost)
                .paymentBreakdown(breakdown)
                .recentTransactions(transactions)
                .build();
    }

    private double value(Double amount) {
        return amount == null ? 0.0 : amount;
    }

    private String paymentBucket(String method) {
        if (method == null || method.isBlank()) {
            return "OTHER";
        }
        String normalized = method.trim().toUpperCase().replace(' ', '_');
        if (normalized.equals("CASH") || normalized.equals("TIEN_MAT")) {
            return "CASH";
        }
        if (normalized.contains("QR")
                || normalized.contains("VNPAY")
                || normalized.contains("VN_PAY")
                || normalized.contains("BANK")
                || normalized.contains("TRANSFER")) {
            return "VIETQR";
        }
        return "OTHER";
    }

    private String customerLabel(User customer) {
        if (customer == null) {
            return "Đơn tại quầy";
        }
        String first = customer.getFirstName() == null ? "" : customer.getFirstName();
        String last = customer.getLastName() == null ? "" : customer.getLastName();
        String name = (first + " " + last).trim();
        return name.isBlank() ? customer.getId() : name;
    }
}
