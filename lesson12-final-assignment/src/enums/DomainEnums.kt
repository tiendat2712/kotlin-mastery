package enums

import java.math.BigDecimal

/**
 * Danh mục sản phẩm của Nova Shop (giống hệt `D:\Java_Journey\lesson15-final-assignment\src\enums\Category.java`).
 */
enum class Category {
    BOOK,
    ELECTRONICS,
    STATIONERY,
    LIFESTYLE
}

/**
 * Hạng thành viên kèm tỷ lệ chiết khấu ngay trong Constructor của Enum
 * (STANDARD giảm 0%, SILVER giảm 5%, GOLD giảm 10%).
 *
 * So với Java Core (nơi `MemberTier` chỉ là enum rỗng rồi phải `switch` ở ngoài),
 * trong Kotlin ta gắn luôn thuộc tính `discountRate` vào từng hằng số enum!
 */
enum class MemberTier(val discountRate: BigDecimal) {
    STANDARD(BigDecimal("0.00")),
    SILVER(BigDecimal("0.05")),
    GOLD(BigDecimal("0.10"))
}

/**
 * Trạng thái đơn hàng kèm phương thức kiểm tra luật chuyển trạng thái hợp lệ (`canTransitionTo`).
 * Luật nghiệp vụ Nova Shop:
 * - Chỉ cho phép: `NEW -> CONFIRMED`, `NEW -> CANCELLED`, `CONFIRMED -> DELIVERED`, `CONFIRMED -> CANCELLED`.
 * - Mọi chuyển đổi khác (kể cả chuyển sang chính trạng thái hiện tại) đều bị từ chối!
 */
enum class OrderStatus {
    NEW,
    CONFIRMED,
    DELIVERED,
    CANCELLED;

    fun canTransitionTo(next: OrderStatus): Boolean = when (this) {
        NEW -> next == CONFIRMED || next == CANCELLED
        CONFIRMED -> next == DELIVERED || next == CANCELLED
        DELIVERED, CANCELLED -> false
    }
}

/**
 * Ba chế độ sắp xếp kết quả tra cứu đơn hàng theo đúng đặc tả đề bài.
 */
enum class OrderSortMode {
    /** Mới nhất trước (`createdAt` giảm dần -> `orderId` tăng dần) */
    NEWEST_FIRST,

    /** Tiền phải trả giảm dần -> Ngày tạo tăng dần -> Mã đơn tăng dần */
    PAYABLE_DESC_THEN_CREATED_ASC,

    /** Email khách hàng tăng dần (không phân biệt hoa/thường, email null luôn đứng cuối) -> Ngày tạo giảm dần -> Mã đơn tăng dần */
    EMAIL_ASC_NULLS_LAST_THEN_CREATED_DESC
}
