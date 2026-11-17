package iuh.fit.se.dtos.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryReceiptItemRequest {
    String pastryId;
    Integer quantity;
    /** Giá vốn sản xuất nội bộ trên một sản phẩm. */
    Double importPrice;
}
