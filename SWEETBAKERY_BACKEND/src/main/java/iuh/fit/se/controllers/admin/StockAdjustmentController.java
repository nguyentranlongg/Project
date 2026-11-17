package iuh.fit.se.controllers.admin;

import iuh.fit.se.dtos.request.StockAdjustmentRequest;
import iuh.fit.se.dtos.response.ApiResponse;
import iuh.fit.se.dtos.response.LowStockPastryResponse;
import iuh.fit.se.dtos.response.StockAdjustmentResponse;
import iuh.fit.se.entities.enums.HttpCode;
import iuh.fit.se.services.admin.StockAdjustmentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("admin/api/v1/stock-adjustments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockAdjustmentController {
    StockAdjustmentService stockAdjustmentService;

    @GetMapping("/low-stock")
    ApiResponse<List<LowStockPastryResponse>> lowStock() {
        return ApiResponse.<List<LowStockPastryResponse>>builder()
                .code(HttpCode.OK.getCODE())
                .message(HttpCode.OK.getMESSAGE())
                .data(stockAdjustmentService.findLowStock())
                .build();
    }

    @PostMapping("/adjust")
    ApiResponse<StockAdjustmentResponse> adjust(@RequestBody StockAdjustmentRequest request) {
        return ApiResponse.<StockAdjustmentResponse>builder()
                .code(HttpCode.OK.getCODE())
                .message(HttpCode.OK.getMESSAGE())
                .data(stockAdjustmentService.adjust(request))
                .build();
    }

    @GetMapping
    ApiResponse<List<StockAdjustmentResponse>> findAll() {
        return ApiResponse.<List<StockAdjustmentResponse>>builder()
                .code(HttpCode.OK.getCODE())
                .message(HttpCode.OK.getMESSAGE())
                .data(stockAdjustmentService.findAll())
                .build();
    }
}
