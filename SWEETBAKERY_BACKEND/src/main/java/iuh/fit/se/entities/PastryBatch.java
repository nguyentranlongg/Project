package iuh.fit.se.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "pastry_batches")
public class PastryBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pastry_id", nullable = false)
    Pastry pastry;

    @Column(name = "quantity_remaining", nullable = false)
    int quantityRemaining;

    @Column(name = "produced_at")
    LocalDateTime producedAt;

    @Column(name = "expiry_date")
    LocalDateTime expiryDate;
}
