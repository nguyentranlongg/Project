package iuh.fit.se.services.admin;

import iuh.fit.se.dtos.request.OrderRequest;
import iuh.fit.se.dtos.request.OrderUpdateStatusRequest;
import iuh.fit.se.dtos.response.OrderResponse;
import iuh.fit.se.dtos.response.QrPaymentResponse;

import java.util.List;

public interface OrderAdminService {
    List<OrderResponse> getAllOrders();
    
    OrderResponse getOrderById(String id);
    
    OrderResponse updateOrderStatus(String orderId, OrderUpdateStatusRequest request);

    OrderResponse createCounterOrder(OrderRequest request);

    QrPaymentResponse generateQrPayment(String orderId);

    OrderResponse confirmQrPayment(String orderId);
}

