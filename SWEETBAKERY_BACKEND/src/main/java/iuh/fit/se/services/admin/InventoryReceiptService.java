package iuh.fit.se.services.admin;

import iuh.fit.se.dtos.request.InventoryReceiptRequest;
import iuh.fit.se.dtos.response.InventoryReceiptResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryReceiptService {
    InventoryReceiptResponse create(InventoryReceiptRequest request);

    Page<InventoryReceiptResponse> findAll(Pageable pageable);
}
