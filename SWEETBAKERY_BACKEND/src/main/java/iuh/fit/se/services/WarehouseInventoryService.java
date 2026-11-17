package iuh.fit.se.services;

import iuh.fit.se.dtos.response.PastryCreationResponse;
import iuh.fit.se.dtos.response.WarehouseNotificationResponse;
import iuh.fit.se.entities.Pastry;

import java.time.LocalDateTime;
import java.util.List;

public interface WarehouseInventoryService {
    void consume(Pastry pastry, int quantity);

    void registerProduction(Pastry pastry, int quantity, LocalDateTime producedAt);

    void syncAdjustment(Pastry pastry, int oldQuantity, int newQuantity);

    void expireDueBatches();

    void attachNearestExpiry(List<PastryCreationResponse> pastries);

    WarehouseNotificationResponse requestProduction(String pastryId);

    List<WarehouseNotificationResponse> listNotifications();

    WarehouseNotificationResponse acknowledge(String notificationId);
}
