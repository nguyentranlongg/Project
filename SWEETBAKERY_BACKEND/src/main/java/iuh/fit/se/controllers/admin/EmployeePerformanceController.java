package iuh.fit.se.controllers.admin;

import iuh.fit.se.dtos.response.ApiResponse;
import iuh.fit.se.dtos.response.EmployeePerformanceReportResponse;
import iuh.fit.se.entities.enums.HttpCode;
import iuh.fit.se.services.admin.EmployeePerformanceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/api/v1/employee-performance")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployeePerformanceController {
    EmployeePerformanceService employeePerformanceService;

    @GetMapping
    ApiResponse<EmployeePerformanceReportResponse> report() {
        return ApiResponse.<EmployeePerformanceReportResponse>builder()
                .code(HttpCode.OK.getCODE())
                .message(HttpCode.OK.getMESSAGE())
                .data(employeePerformanceService.report())
                .build();
    }
}
