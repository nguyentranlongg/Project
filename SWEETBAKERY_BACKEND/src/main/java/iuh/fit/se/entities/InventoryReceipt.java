package iuh.fit.se.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "inventory_receipts")
public class InventoryReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "receipt_code", nullable = false, unique = true, length = 64)
    String receiptCode;

    @Column(name = "supplier_name", length = 255)
    String supplierName = "Bếp nội bộ Sweet Bakery";

    @Column(name = "total_amount")
    Double totalAmount;

    @ManyToOne
    @JoinColumn(name = "created_by")
    User createdBy;

    @Column(name = "import_date")
    LocalDateTime importDate;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    List<InventoryReceiptDetail> items = new ArrayList<>();
}
