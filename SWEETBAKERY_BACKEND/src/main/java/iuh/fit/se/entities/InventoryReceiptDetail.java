package iuh.fit.se.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "inventory_receipt_details")
public class InventoryReceiptDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    InventoryReceipt receipt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pastry_id", nullable = false)
    Pastry product;

    @Column(nullable = false)
    int quantity;

    /** Giá vốn sản xuất nội bộ (đơn giá). Tổng dòng được cộng vào chi phí để tính lợi nhuận gộp. */
    @Column(name = "import_price", nullable = false)
    Double importPrice;
}
