package iuh.fit.se.dtos.response;

import iuh.fit.se.entities.enums.PastryStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LowStockPastryResponse {
    String id;
    String name;
    Integer stockQuantity;
    Integer minThreshold;
    PastryStatus status;
}
