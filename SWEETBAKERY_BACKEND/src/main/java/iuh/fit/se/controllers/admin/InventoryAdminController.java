package iuh.fit.se.controllers.admin;

import iuh.fit.se.dtos.request.InventoryReceiptRequest;
import iuh.fit.se.dtos.response.ApiResponse;
import iuh.fit.se.dtos.response.InventoryReceiptResponse;
import iuh.fit.se.entities.enums.HttpCode;
import iuh.fit.se.services.admin.InventoryReceiptService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/api/v1/inventory-receipts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryAdminController {
    InventoryReceiptService inventoryReceiptService;

    @PostMapping
    ApiResponse<InventoryReceiptResponse> create(@RequestBody InventoryReceiptRequest request) {
        return ApiResponse.<InventoryReceiptResponse>builder()
                .code(HttpCode.OK.getCODE())
                .message(HttpCode.OK.getMESSAGE())
                .data(inventoryReceiptService.create(request))
                .build();
    }

    @GetMapping
    ApiResponse<Page<InventoryReceiptResponse>> findAll(
            @PageableDefault(size = 10, sort = "importDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<Page<InventoryReceiptResponse>>builder()
                .code(HttpCode.OK.getCODE())
                .message(HttpCode.OK.getMESSAGE())
                .data(inventoryReceiptService.findAll(pageable))
                .build();
    }
}
