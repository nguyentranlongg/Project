package iuh.fit.se.services.admin;

import iuh.fit.se.dtos.request.StockAdjustmentRequest;
import iuh.fit.se.dtos.response.LowStockPastryResponse;
import iuh.fit.se.dtos.response.StockAdjustmentResponse;

import java.util.List;

public interface StockAdjustmentService {
    List<LowStockPastryResponse> findLowStock();

    StockAdjustmentResponse adjust(StockAdjustmentRequest request);

    List<StockAdjustmentResponse> findAll();
}
