package legacy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lớp Java truyền thống (Legacy Java Code) đặt chung trong cùng module với Kotlin
 * để minh họa khả năng tương thích 2 chiều 100% giữa Kotlin và Java trên JVM.
 *
 * Chú ý:
 * 1. Các phương thức ở đây KHÔNG gắn annotation @Nullable hay @NonNull, nên khi gọi từ Kotlin,
 *    kiểu trả về sẽ được trình biên dịch Kotlin nhìn nhận là **Platform Type** (`String!`, `List<String!>!`).
 * 2. Giao diện {@link PaymentCallback} là một Java SAM (Single Abstract Method) interface,
 *    cho phép truyền trực tiếp Lambda của Kotlin vào mà không cần từ khóa `object : ...`.
 */
public class LegacyPaymentGateway {

    /**
     * Java SAM (Single Abstract Method) Interface.
     */
    @FunctionalInterface
    public interface PaymentCallback {
        void onCompleted(String transactionId, boolean success, String message);
    }

    private String merchantCode;

    public LegacyPaymentGateway(String merchantCode) {
        this.merchantCode = merchantCode;
    }

    // Java Getter/Setter chuẩn JavaBeans -> Kotlin tự động cho phép truy cập như thuộc tính `gateway.merchantCode`!
    public String getMerchantCode() {
        return merchantCode;
    }

    public void setMerchantCode(String merchantCode) {
        this.merchantCode = merchantCode;
    }

    /**
     * Trả về String không có annotation nullability -> Trong Kotlin đây là Platform Type `String!`.
     * Nếu orderId bắt đầu bằng "MISSING", hàm trả về `null` thật lúc Runtime!
     */
    public String findTransactionToken(String orderId) {
        if (orderId == null || orderId.startsWith("MISSING")) {
            return null;
        }
        return "TOK-" + merchantCode + "-" + orderId.trim().toUpperCase();
    }

    /**
     * Trả về List<String> của Java -> Trong Kotlin có thể nhìn như `List<String>` (chỉ đọc)
     * hoặc `MutableList<String>` (có thể sửa).
     */
    public List<String> getSupportedCurrencies() {
        return new ArrayList<>(Arrays.asList("VND", "USD", "JPY"));
    }

    /**
     * Nhận một Java SAM interface -> Khi gọi từ Kotlin có thể truyền thẳng `{ txId, ok, msg -> ... }`.
     */
    public void executeCharge(String orderId, long amountVnd, PaymentCallback callback) {
        if (amountVnd <= 0) {
            callback.onCompleted("NONE", false, "So tien thanh toan phai lon hon 0");
            return;
        }
        String txId = "TX-" + orderId + "-" + amountVnd;
        callback.onCompleted(txId, true, "Thanh toan thanh cong qua cong " + merchantCode);
    }
}
