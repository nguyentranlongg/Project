package iuh.fit.se.repositories;

import iuh.fit.se.entities.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, String> {
    List<StockAdjustment> findAllByOrderByAdjustedAtDesc();
}
