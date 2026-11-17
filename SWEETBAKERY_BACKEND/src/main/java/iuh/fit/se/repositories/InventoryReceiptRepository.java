package iuh.fit.se.repositories;

import iuh.fit.se.entities.InventoryReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryReceiptRepository extends JpaRepository<InventoryReceipt, String> {
    boolean existsByReceiptCode(String receiptCode);

    @Query("SELECT COALESCE(SUM(r.totalAmount), 0.0) FROM InventoryReceipt r")
    Double sumTotalAmount();

    List<InventoryReceipt> findAllByOrderByImportDateDesc();
}
