package iuh.fit.se.services;

import iuh.fit.se.dtos.response.QrPaymentResponse;
import iuh.fit.se.entities.Order;

public interface VietQrService {
    QrPaymentResponse generate(Order order);
}
