package iuh.fit.se.repositories;

import iuh.fit.se.entities.PastryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PastryBatchRepository extends JpaRepository<PastryBatch, String> {
    List<PastryBatch> findByQuantityRemainingGreaterThanAndExpiryDateBefore(int quantity, LocalDateTime expiryDate);

    List<PastryBatch> findByPastry_IdAndQuantityRemainingGreaterThanOrderByExpiryDateAsc(String pastryId, int quantity);

    List<PastryBatch> findByQuantityRemainingGreaterThan(int quantity);
}
