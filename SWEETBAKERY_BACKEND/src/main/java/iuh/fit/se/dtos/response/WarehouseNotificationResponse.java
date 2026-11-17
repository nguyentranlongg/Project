package iuh.fit.se.dtos.response;

import iuh.fit.se.entities.enums.WarehouseAlertType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WarehouseNotificationResponse {
    String id;
    WarehouseAlertType type;
    String message;
    String pastryId;
    String pastryName;
    boolean acknowledged;
    LocalDateTime createdAt;
}
