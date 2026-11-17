package iuh.fit.se.entities;

import iuh.fit.se.entities.enums.WarehouseAlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "warehouse_notifications")
public class WarehouseNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    WarehouseAlertType type;

    @Column(length = 500, nullable = false)
    String message;

    @Column(name = "pastry_id")
    String pastryId;

    @Column(name = "pastry_name")
    String pastryName;

    @Column(name = "acknowledged", nullable = false)
    boolean acknowledged;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}
