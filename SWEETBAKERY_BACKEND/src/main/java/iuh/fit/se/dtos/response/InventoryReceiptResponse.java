package iuh.fit.se.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryReceiptResponse {
    String id;
    String receiptCode;
    String supplierName;
    Double totalAmount;
    String createdByName;
    LocalDateTime importDate;
    List<InventoryReceiptItemResponse> items;
}
