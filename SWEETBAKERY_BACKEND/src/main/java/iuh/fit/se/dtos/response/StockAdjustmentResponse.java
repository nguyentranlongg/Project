package iuh.fit.se.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockAdjustmentResponse {
    String id;
    String pastryId;
    String pastryName;
    Integer oldQuantity;
    Integer newQuantity;
    String reason;
    String adjustedByName;
    LocalDateTime adjustedAt;
}
