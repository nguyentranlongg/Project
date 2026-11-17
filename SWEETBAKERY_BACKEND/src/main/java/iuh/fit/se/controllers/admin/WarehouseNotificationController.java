package iuh.fit.se.controllers.admin;

import iuh.fit.se.dtos.request.ProductionRequest;
import iuh.fit.se.dtos.response.ApiResponse;
import iuh.fit.se.dtos.response.WarehouseNotificationResponse;
import iuh.fit.se.entities.enums.HttpCode;
import iuh.fit.se.services.WarehouseInventoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("admin/api/v1/warehouse-notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WarehouseNotificationController {
    WarehouseInventoryService warehouseInventoryService;

    @GetMapping
    ApiResponse<List<WarehouseNotificationResponse>> list() {
        return ApiResponse.<List<WarehouseNotificationResponse>>builder()
                .code(HttpCode.OK.getCODE())
                .message(HttpCode.OK.getMESSAGE())
                .data(warehouseInventoryService.listNotifications())
                .build();
    }

    @PostMapping("/production-request")
    ApiResponse<WarehouseNotificationResponse> requestProduction(@RequestBody ProductionRequest request) {
        return ApiResponse.<WarehouseNotificationResponse>builder()
                .code(HttpCode.OK.getCODE())
                .message(HttpCode.OK.getMESSAGE())
                .data(warehouseInventoryService.requestProduction(request.getPastryId()))
                .build();
    }

    @PutMapping("/{id}/read")
    ApiResponse<WarehouseNotificationResponse> acknowledge(@PathVariable String id) {
        return ApiResponse.<WarehouseNotificationResponse>builder()
                .code(HttpCode.OK.getCODE())
                .message(HttpCode.OK.getMESSAGE())
                .data(warehouseInventoryService.acknowledge(id))
                .build();
    }
}
