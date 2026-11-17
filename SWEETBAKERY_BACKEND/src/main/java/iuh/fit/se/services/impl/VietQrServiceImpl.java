package iuh.fit.se.services.impl;

import iuh.fit.se.dtos.response.QrPaymentResponse;
import iuh.fit.se.entities.Order;
import iuh.fit.se.services.VietQrService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class VietQrServiceImpl implements VietQrService {
    @Value("${vietqr.bank-id}")
    private String bankId;

    @Value("${vietqr.account-number}")
    private String accountNumber;

    @Value("${vietqr.account-name}")
    private String accountName;

    @Value("${vietqr.template:compact2}")
    private String template;

    @Override
    public QrPaymentResponse generate(Order order) {
        long amount = Math.round(order.getTongTien());
        String addInfo = "SWEETBAKERY " + order.getId();
        String paymentUrl = "https://img.vietqr.io/image/"
                + bankId + "-" + accountNumber + "-" + template
                + ".png?amount=" + amount
                + "&addInfo=" + URLEncoder.encode(addInfo, StandardCharsets.UTF_8)
                + "&accountName=" + URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        return QrPaymentResponse.builder()
                .orderId(order.getId())
                .paymentUrl(paymentUrl)
                .amount(amount)
                .accountName(accountName)
                .accountNumber(accountNumber)
                .bankId(bankId)
                .addInfo(addInfo)
                .build();
    }
}
