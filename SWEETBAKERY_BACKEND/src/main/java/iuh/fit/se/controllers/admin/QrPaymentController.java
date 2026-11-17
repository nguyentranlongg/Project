package iuh.fit.se.controllers.admin;

import iuh.fit.se.dtos.response.ApiResponse;
import iuh.fit.se.dtos.response.OrderResponse;
import iuh.fit.se.dtos.response.QrPaymentResponse;
import iuh.fit.se.entities.enums.HttpCode;
import iuh.fit.se.services.admin.OrderAdminService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/api/v1/qr-payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QrPaymentController {
    OrderAdminService orderAdminService;

    @GetMapping("/generate/{orderId}")
    ApiResponse<QrPaymentResponse> generate(@PathVariable String orderId) {
        return ApiResponse.<QrPaymentResponse>builder()
                .code(HttpCode.OK.getCODE())
                .message(HttpCode.OK.getMESSAGE())
                .data(orderAdminService.generateQrPayment(orderId))
                .build();
    }

    @PutMapping("/confirm/{orderId}")
    ApiResponse<OrderResponse> confirm(@PathVariable String orderId) {
        return ApiResponse.<OrderResponse>builder()
                .code(HttpCode.OK.getCODE())
                .message(HttpCode.OK.getMESSAGE())
                .data(orderAdminService.confirmQrPayment(orderId))
                .build();
    }
}
