package iuh.fit.se.dtos.response;

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
public class InventoryReceiptItemResponse {
    String id;
    String pastryId;
    String pastryName;
    Integer quantity;
    Double importPrice;
    Double lineAmount;
}
