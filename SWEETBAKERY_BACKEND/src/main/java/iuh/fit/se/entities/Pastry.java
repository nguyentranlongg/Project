package iuh.fit.se.entities;

import iuh.fit.se.entities.enums.PastryStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "pastries")
public class Pastry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    String id;

    @Column(name = "name")
    String name;

    @Column(name = "price")
    Double price;

    @Column(name = "description")
    String description;

    @Column(name = "image_url")
    String imageUrl;

    @Column(name = "stock_quantity")
    private int stockQuantity = 0;

    @Column(name = "min_threshold", nullable = false)
    private int minThreshold = 5;

    /** Số ngày bánh tươi giữ được sau khi ra lò. Dùng để tính hạn của từng mẻ. */
    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays = 2;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PastryStatus status = PastryStatus.DRAFT;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private PastryCategory category;

    @OneToMany(mappedBy = "pastry")
    private List<OrderDetail> orderDetails;
}
