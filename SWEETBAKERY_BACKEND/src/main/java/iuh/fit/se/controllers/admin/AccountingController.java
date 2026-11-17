package iuh.fit.se.controllers.admin;

import iuh.fit.se.dtos.response.ApiResponse;
import iuh.fit.se.dtos.response.FinancialSummaryResponse;
import iuh.fit.se.entities.enums.HttpCode;
import iuh.fit.se.services.admin.AccountingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/api/v1/accounting")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountingController {
    AccountingService accountingService;

    @GetMapping("/financial-summary")
    ApiResponse<FinancialSummaryResponse> financialSummary() {
        return ApiResponse.<FinancialSummaryResponse>builder()
                .code(HttpCode.OK.getCODE())
                .message(HttpCode.OK.getMESSAGE())
                .data(accountingService.financialSummary())
                .build();
    }
}
