package iuh.fit.se.services.admin.impl;

import iuh.fit.se.dtos.request.OrderItemRequest;
import iuh.fit.se.dtos.request.OrderRequest;
import iuh.fit.se.dtos.request.OrderUpdateStatusRequest;
import iuh.fit.se.dtos.response.OrderDetailResponse;
import iuh.fit.se.dtos.response.OrderResponse;
import iuh.fit.se.dtos.response.QrPaymentResponse;
import iuh.fit.se.entities.AccountCredential;
import iuh.fit.se.entities.Order;
import iuh.fit.se.entities.OrderDetail;
import iuh.fit.se.entities.Pastry;
import iuh.fit.se.entities.User;
import iuh.fit.se.entities.enums.HttpCode;
import iuh.fit.se.entities.enums.PastryStatus;
import iuh.fit.se.entities.enums.PaymentStatus;
import iuh.fit.se.entities.enums.TrangThaiDH;
import iuh.fit.se.exceptions.AppException;
import iuh.fit.se.repositories.AccountCredentialRepository;
import iuh.fit.se.repositories.OrderRepository;
import iuh.fit.se.repositories.PastryRepository;
import iuh.fit.se.entities.enums.PaymentMethod;
import iuh.fit.se.services.VietQrService;
import iuh.fit.se.services.WarehouseInventoryService;
import iuh.fit.se.services.admin.OrderAdminService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class OrderAdminServiceImpl implements OrderAdminService {
    OrderRepository orderRepository;
    PastryRepository pastryRepository;
    AccountCredentialRepository accountCredentialRepository;
    WarehouseInventoryService warehouseInventoryService;
    VietQrService vietQrService;

    @Override
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        List<OrderResponse> out = new ArrayList<>();
        for (Order o : orders) {
            out.add(mapToOrderResponse(o));
        }
        return out;
    }

    @Override
    public OrderResponse getOrderById(String id) {
        Optional<Order> opt = orderRepository.findById(id);
        if (opt.isEmpty()) {
            throw new AppException(HttpCode.NOT_FOUND);
        }
        return mapToOrderResponse(opt.get());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderUpdateStatusRequest request) {
        Optional<Order> opt = orderRepository.findById(orderId);
        if (opt.isEmpty()) {
            throw new AppException(HttpCode.NOT_FOUND);
        }

        Order order = opt.get();
        TrangThaiDH current = order.getTrangThai();
        TrangThaiDH target = request.getTrangThai();

        boolean allowed = (current == TrangThaiDH.PENDING && target == TrangThaiDH.CONFIRMED) ||
                (current == TrangThaiDH.PAID && target == TrangThaiDH.CONFIRMED) ||
                (current == TrangThaiDH.CONFIRMED && target == TrangThaiDH.COMPLETED) ||
                (current == TrangThaiDH.REFUND_PENDING && target == TrangThaiDH.CANCELLED);

        if (!allowed) {
            throw new AppException(HttpCode.BAD_REQUEST);
        }

        order.setTrangThai(target);
        if (target == TrangThaiDH.CANCELLED && current == TrangThaiDH.REFUND_PENDING) {
            order.setRefundProofImageUrl(request.getRefundProofImageUrl());
        }
        if (target == TrangThaiDH.CONFIRMED || target == TrangThaiDH.COMPLETED) {
            order.setProcessedByEmployee(currentUser());
            if (order.getPaymentMethod() != null
                    && PaymentMethod.CASH.name().equals(PaymentMethod.normalize(order.getPaymentMethod()))) {
                order.setPaymentStatus(PaymentStatus.PAID);
            }
        }
        orderRepository.save(order);

        log.info("Order {} status updated to {}", orderId, request.getTrangThai());
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse createCounterOrder(OrderRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(HttpCode.BAD_REQUEST);
        }

        User staff = currentUser();
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setNgayDatHang(LocalDateTime.now());
        order.setTrangThai(TrangThaiDH.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod() != null
                ? PaymentMethod.normalize(request.getPaymentMethod())
                : PaymentMethod.VIETQR.name());
        order.setProcessedByEmployee(staff);

        List<OrderDetail> details = new ArrayList<>();
        double total = 0.0;
        for (OrderItemRequest item : request.getItems()) {
            if (item == null || item.getPastryId() == null || item.getQty() == null || item.getQty() <= 0) {
                continue;
            }
            Pastry pastry = pastryRepository.findById(item.getPastryId())
                    .orElseThrow(() -> new AppException(HttpCode.NOT_FOUND));
            warehouseInventoryService.consume(pastry, item.getQty());

            OrderDetail detail = new OrderDetail();
            detail.setId(UUID.randomUUID().toString());
            detail.setPastry(pastry);
            detail.setSoLuong(item.getQty());
            detail.setOrder(order);
            details.add(detail);
            total += pastry.getPrice() * item.getQty();
        }
        if (details.isEmpty()) {
            throw new AppException(HttpCode.BAD_REQUEST);
        }

        order.setTongTien(total);
        order.setOrderDetails(details);
        orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    @Override
    public QrPaymentResponse generateQrPayment(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpCode.NOT_FOUND));
        return vietQrService.generate(order);
    }

    @Override
    @Transactional
    public OrderResponse confirmQrPayment(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpCode.NOT_FOUND));
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setProcessedByEmployee(currentUser());
        order.setTrangThai(TrangThaiDH.PAID);
        if (order.getPaymentMethod() == null || order.getPaymentMethod().isBlank()) {
            order.setPaymentMethod(PaymentMethod.VIETQR.name());
        }
        orderRepository.save(order);
        return mapToOrderResponse(order);
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

    private OrderResponse mapToOrderResponse(Order o) {
        OrderResponse r = new OrderResponse();
        r.setId(o.getId());
        r.setNgayDatHang(o.getNgayDatHang());
        r.setTongTien(o.getTongTien());
        r.setPaymentMethod(o.getPaymentMethod());
        r.setPaymentStatus(o.getPaymentStatus() == null ? null : o.getPaymentStatus().name());
        r.setTrangThai(o.getTrangThai() == null ? null : o.getTrangThai().name());
        r.setCustomerAddress(o.getCustomer() != null ? o.getCustomer().getAddress() : null);

        // Add customer name
        String customerName = null;
        if (o.getCustomer() != null) {
            String firstName = o.getCustomer().getFirstName();
            String lastName = o.getCustomer().getLastName();
            if (firstName != null || lastName != null) {
                customerName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                customerName = customerName.trim();
            }
        }
        r.setCustomerName(customerName);
        r.setLyDoHuy(o.getLyDoHuy());
        r.setBankAccountName(o.getBankAccountName());
        r.setBankAccountNumber(o.getBankAccountNumber());
        r.setBankName(o.getBankName());
        r.setRefundProofImageUrl(o.getRefundProofImageUrl());

        List<OrderDetailResponse> items = new ArrayList<>();
        if (o.getOrderDetails() != null) {
            for (OrderDetail od : o.getOrderDetails()) {
                OrderDetailResponse dr = new OrderDetailResponse();
                dr.setId(od.getId());
                dr.setPastryId(od.getPastry() != null ? od.getPastry().getId() : null);
                dr.setName(od.getPastry() != null ? od.getPastry().getName() : null);
                dr.setQty(od.getSoLuong());
                dr.setPrice(od.getPastry() != null ? od.getPastry().getPrice() : null);
                dr.setImage(od.getPastry() != null ? od.getPastry().getImageUrl() : null);
                items.add(dr);
            }
        }
        r.setItems(items);
        return r;
    }
}
