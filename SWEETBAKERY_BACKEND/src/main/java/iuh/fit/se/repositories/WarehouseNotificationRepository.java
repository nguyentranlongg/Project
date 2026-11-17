package iuh.fit.se.repositories;

import iuh.fit.se.entities.WarehouseNotification;
import iuh.fit.se.entities.enums.WarehouseAlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseNotificationRepository extends JpaRepository<WarehouseNotification, String> {
    boolean existsByPastryIdAndTypeAndAcknowledgedIsFalse(String pastryId, WarehouseAlertType type);

    List<WarehouseNotification> findTop50ByOrderByCreatedAtDesc();
}
