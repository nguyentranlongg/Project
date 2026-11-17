package iuh.fit.se.entities.enums;

public enum PaymentMethod {
    CASH,
    VNPAY,
    VIETQR,
    BANK_TRANSFER;

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return CASH.name();
        }
        String normalized = raw.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        if (normalized.equals(BANK_TRANSFER.name())
                || normalized.equals(VIETQR.name())
                || normalized.contains("QR")
                || normalized.contains("TRANSFER")) {
            return VIETQR.name();
        }
        if (normalized.contains("VNPAY") || normalized.equals("VN_PAY")) {
            return VNPAY.name();
        }
        if (normalized.equals(CASH.name()) || normalized.equals("TIEN_MAT")) {
            return CASH.name();
        }
        return normalized;
    }

    public static boolean isVietQr(String raw) {
        return VIETQR.name().equals(normalize(raw));
    }
}
