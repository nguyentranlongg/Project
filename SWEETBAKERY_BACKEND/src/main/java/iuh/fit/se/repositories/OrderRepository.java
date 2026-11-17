package iuh.fit.se.repositories;

import iuh.fit.se.entities.Order;
import iuh.fit.se.entities.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
	List<Order> findByCustomer_Id(String userId);

	@Query("SELECT COALESCE(SUM(o.tongTien), 0.0) FROM Order o WHERE o.paymentStatus = :status")
	Double sumTongTienByPaymentStatus(PaymentStatus status);

	List<Order> findByPaymentStatusOrderByNgayDatHangDesc(PaymentStatus status);
}
