package bean

import enums.Category
import enums.MemberTier
import enums.OrderStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * HỆ THỐNG DOMAIN MODELS & BÁO CÁO CỦA NOVA SHOP (IDIOMATIC KOTLIN EDITION)
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Trong `D:\Java_Journey\lesson15-final-assignment\src\bean`, bạn phải tạo 4 file `.java` riêng lẻ
 *      (`Customer.java`, `Product.java`, `OrderLine.java`, `Order.java`) với hàng trăm dòng boilerplate
 *      (constructor rỗng, constructor đầy đủ, getter, setter, toString) và các lớp đó đều là **Mutable**
 *      (ai cũng có thể gọi `order.setLines(...)` làm hỏng dữ liệu gốc).
 *    - Trong Kotlin, ta gom toàn bộ Domain Models và các DTO kết quả báo cáo vào một file duy nhất
 *      với `data class` dùng thuộc tính bất biến `val` (Immutable):
 *      + Trình biên dịch tự sinh `equals()`, `hashCode()`, `toString()`, `componentN()` và đặc biệt là `.copy(...)`.
 *      + Khi cần chuẩn hóa dữ liệu hay chuyển trạng thái đơn hàng, ta tạo bản sao mới bằng `.copy(...)`
 *        giúp **bảo vệ tuyệt đối dữ liệu nguồn (Defensive Immutability)** mà không sợ hiệu ứng phụ (Side-effect)!
 *
 * 2. SEALED CLASS CHO KẾT QUẢ XỬ LÝ ĐƠN HÀNG (`OrderOperationResult`):
 *    - Thay vì chỉ trả về boolean hoặc ném lỗi tràn lan, ta dùng `sealed class` để đóng gói kết quả
 *      thao tác nghiệp vụ (`Success` hoặc `Rejected`), giúp tầng giao diện `Main.kt` xử lý bằng `when` vét cạn!
 */

// =========================================================================
// 1. CORE DOMAIN ENTITIES / MODELS
// =========================================================================

data class Customer(
    val id: String,
    val name: String,
    val email: String?, // Nullable String? chính quy — thay vì null ngầm định trong Java!
    val city: String,
    val currentTier: MemberTier,
    val joinedAt: LocalDate,
    val updatedAt: LocalDateTime
)

data class Product(
    val id: String,
    val name: String,
    val category: Category,
    val listPrice: BigDecimal,
    val active: Boolean
)

data class OrderLine(
    val productId: String,
    val quantity: Int,
    val unitPrice: BigDecimal
) {
    /**
     * Tiền dòng hàng = `quantity * unitPrice` (sử dụng Operator Overloading `*` của Kotlin trên BigDecimal).
     */
    val lineAmount: BigDecimal
        get() = unitPrice * quantity.toBigDecimal()
}

data class Order(
    val id: String,
    val customerId: String,
    val createdAt: LocalDateTime,
    val deliveredAt: LocalDateTime?,
    val status: OrderStatus,
    val tierAtPurchase: MemberTier,
    val lines: List<OrderLine>
)

// =========================================================================
// 2. SEARCH, PAGINATION & OPERATION RESULT MODELS
// =========================================================================

/**
 * Dòng kết quả tra cứu đơn hàng (tóm tắt đầy đủ thông tin đơn + khách + tiền phải trả).
 */
data class OrderSummaryView(
    val orderId: String,
    val customerId: String,
    val customerName: String,
    val customerEmail: String?,
    val createdAt: LocalDateTime,
    val status: OrderStatus,
    val tierAtPurchase: MemberTier,
    val subtotalAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val payableAmount: BigDecimal
)

/**
 * Kết quả phân trang Generic `<out T>` dùng chung cho mọi kiểu dữ liệu (Đơn hàng, Khách hàng, Sản phẩm).
 */
data class PageResult<out T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Int,
    val totalPages: Int
)

/**
 * Sealed Class biểu diễn kết quả của một thao tác nghiệp vụ trên đơn hàng (Tạo đơn / Chuyển trạng thái / Kiểm tra đơn).
 */
sealed class OrderOperationResult<out T> {
    data class Success<out T>(val data: T, val message: String) : OrderOperationResult<T>()
    data class Rejected(val orderId: String, val reason: String) : OrderOperationResult<Nothing>()
}

// =========================================================================
// 3. NĂM GÓC NHÌN BÁO CÁO KINH DOANH (5 BUSINESS REPORT VIEWS)
// =========================================================================

/** (3.1) Báo cáo Tổng quan (Overview Report) */
data class OverviewReport(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val countByStatus: Map<OrderStatus, Int>,
    val totalRevenue: BigDecimal,
    val averageDeliveredOrderValue: BigDecimal,
    val distinctPurchasingCustomers: Int,
    val monthlyRevenue: Map<YearMonth, BigDecimal>
)

/** (3.2) Xếp hạng Khách hàng chi tiêu cao nhất */
data class CustomerSpendRank(
    val customerId: String,
    val customerName: String,
    val city: String,
    val deliveredOrderCount: Int,
    val totalSpent: BigDecimal,
    val latestOrderCreatedAt: LocalDateTime
)

data class CustomerReport(
    val topSpendingCustomers: List<CustomerSpendRank>,
    val inactiveCustomersInPeriod: List<Customer>,
    val purchasingCustomerCities: List<String>
)

/** (3.3) Xếp hạng Sản phẩm bán chạy trong từng Category */
data class ProductSalesRank(
    val productId: String,
    val productName: String,
    val category: Category,
    val totalQuantitySold: Int,
    val grossMerchandiseAmount: BigDecimal,
    val distinctOrderCount: Int
)

data class ProductReport(
    val topProductsByCategory: Map<Category, List<ProductSalesRank>>,
    val unsoldActiveProducts: List<Product>
)

/** (3.4) Phân tích chéo Thành phố x Hạng thành viên lúc mua */
data class CityTierStats(
    val city: String,
    val tierAtPurchase: MemberTier,
    val distinctCustomerCount: Int,
    val totalRevenue: BigDecimal
)

/** (3.5) Cảnh báo vận hành: Đơn chờ quá hạn (> 72 giờ tại mốc `AS_OF`) */
data class OverdueOrderAlert(
    val orderId: String,
    val customerId: String,
    val customerName: String,
    val status: OrderStatus,
    val createdAt: LocalDateTime,
    val waitingHours: Long,
    val payableAmount: BigDecimal
)

/** Gói tổng hợp cả 5 báo cáo kinh doanh */
data class BusinessReport(
    val overview: OverviewReport,
    val customerReport: CustomerReport,
    val productReport: ProductReport,
    val cityTierBreakdown: List<CityTierStats>,
    val overdueAlerts: List<OverdueOrderAlert>
)
