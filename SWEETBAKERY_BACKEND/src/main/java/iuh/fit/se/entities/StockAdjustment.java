package iuh.fit.se.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "stock_adjustments")
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pastry_id", nullable = false)
    Pastry product;

    @Column(name = "old_quantity", nullable = false)
    int oldQuantity;

    @Column(name = "new_quantity", nullable = false)
    int newQuantity;

    @Column(length = 500)
    String reason;

    @ManyToOne
    @JoinColumn(name = "adjusted_by")
    User adjustedBy;

    @Column(name = "adjusted_at")
    LocalDateTime adjustedAt;
}
