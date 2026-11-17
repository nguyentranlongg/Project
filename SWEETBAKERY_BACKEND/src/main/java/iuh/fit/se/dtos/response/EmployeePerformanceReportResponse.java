package iuh.fit.se.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmployeePerformanceReportResponse {
    List<StaffPerformanceResponse> staff;
    PerformanceBucketResponse sales;
    PerformanceBucketResponse importStaff;
    PerformanceBucketResponse inventory;
    Double fulfillmentRate;
}
