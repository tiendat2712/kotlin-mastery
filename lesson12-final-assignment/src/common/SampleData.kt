package common

import bean.Customer
import bean.Order
import bean.OrderLine
import bean.Product
import enums.Category
import enums.MemberTier
import enums.MemberTier.GOLD
import enums.MemberTier.SILVER
import enums.MemberTier.STANDARD
import enums.OrderStatus
import enums.OrderStatus.CANCELLED
import enums.OrderStatus.CONFIRMED
import enums.OrderStatus.DELIVERED
import enums.OrderStatus.NEW
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Dữ liệu đầu vào chuẩn của Nova Shop — đối chiếu 1-1 với `D:\Java_Journey\lesson15-final-assignment\src\common\SampleData.java`.
 *
 * Mỗi lần gọi hàm trong `object SampleData` đều tạo mới danh sách và các đối tượng bên trong
 * để mọi kịch bản kiểm thử hoàn toàn độc lập và tái lập được 100%.
 */
object SampleData {
    val REPORT_FROM: LocalDate = LocalDate.of(2026, 7, 1)
    val REPORT_TO: LocalDate = LocalDate.of(2026, 9, 30)
    val AS_OF: LocalDateTime = LocalDateTime.of(2026, 10, 1, 0, 0)

    /**
     * 12 bản ghi khách hàng gốc (có khoảng trắng thừa, chữ hoa/thường lộn xộn, email rỗng/null,
     * và 2 bản ghi trùng mã `C003`, `C002`). Sau khi chuẩn hóa & khử trùng sẽ còn đúng **10 khách hàng**.
     */
    fun customers(): List<Customer> = listOf(
        customer("C001", "  Nguyễn   Minh An ", " AN@Example.com ", " Hồ Chí Minh ", GOLD, "2025-01-10", "2026-06-01T09:00"),
        customer("C002", "Trần Bình", "binh@example.com", "Hà Nội", SILVER, "2025-03-12", "2026-06-01T09:00"),
        customer("C003", " Lê  Chi ", "old.chi@example.com", "Đà Nẵng", STANDARD, "2025-05-20", "2026-06-01T09:00"),
        customer("C004", "Phạm Dũng", null, "Hà Nội", STANDARD, "2025-06-15", "2026-06-01T09:00"),
        customer("C005", "Hoàng Hà", "ha@example.com", "Hồ Chí Minh", SILVER, "2025-08-01", "2026-06-01T09:00"),
        customer("C006", "Võ Khánh", "khanh@example.com", "Đà Nẵng", GOLD, "2026-01-10", "2026-06-01T09:00"),
        customer("C007", "Đỗ Linh", "linh@example.com", "Cần Thơ", STANDARD, "2026-02-01", "2026-06-01T09:00"),
        customer("C008", "Bùi Nam", "nam@example.com", "Hà Nội", SILVER, "2026-03-01", "2026-06-01T09:00"),
        customer("C009", "Mai Ngọc", "ngoc@example.com", "Huế", STANDARD, "2026-04-01", "2026-06-01T09:00"),
        customer("C010", "Trịnh Phúc", "  ", "Huế", GOLD, "2026-05-01", "2026-06-01T09:00"),
        customer(" c003 ", " Lê   Chi ", " CHI@EXAMPLE.COM ", " Đà Nẵng ", SILVER, "2025-05-20", "2026-06-20T10:00"),
        customer("C002", "Trần Bình (bản cũ)", "old.binh@example.com", "Hà Nội", STANDARD, "2025-03-12", "2026-05-01T09:00")
    )

    /**
     * 12 sản phẩm trong danh mục (có sản phẩm ngừng kinh doanh `P010` và sản phẩm chưa bán được `P011`, `P012`).
     */
    fun products(): List<Product> = listOf(
        product("P001", "Java Core", Category.BOOK, "180000", true),
        product("P002", "Clean Code", Category.BOOK, "250000.00", true),
        product("P003", "Bàn phím cơ", Category.ELECTRONICS, "800000", true),
        product("P004", "Chuột không dây", Category.ELECTRONICS, "300000", true),
        product("P005", "Sổ tay", Category.STATIONERY, "50000", true),
        product("P006", "Bút gel", Category.STATIONERY, "10000.00", true),
        product("P007", "Bình giữ nhiệt", Category.LIFESTYLE, "200000", true),
        product("P008", "Túi vải", Category.LIFESTYLE, "100000", true),
        product("P009", "Thuật toán căn bản", Category.BOOK, "250000.0", true),
        product("P010", "Tai nghe mẫu cũ", Category.ELECTRONICS, "500000", false),
        product("P011", "Đèn bàn", Category.ELECTRONICS, "350000", true),
        product("P012", "Lịch để bàn", Category.STATIONERY, "50000.00", true)
    )

    /**
     * 24 đơn lịch sử hợp lệ (trong đó có 22 đơn thuộc Quý III/2026 và 2 đơn `O021`, `O022` nằm ngoài kỳ).
     */
    fun orders(): List<Order> = listOf(
        order("O001", "C001", "2026-07-01T00:00", "2026-07-03T10:00", DELIVERED, GOLD,
            line("P001", 2, "180000"), line("P005", 4, "50000")),
        order("O002", "C002", "2026-07-05T09:00", "2026-07-07T09:00", DELIVERED, SILVER,
            line("P003", 1, "750000"), line("P004", 1, "300000")),
        order("O003", "C003", "2026-07-10T10:00", "2026-07-11T10:00", DELIVERED, STANDARD,
            line("P002", 1, "250000"), line("P006", 5, "10000")),
        order("O004", "C004", "2026-07-12T11:00", null, CANCELLED, STANDARD,
            line("P003", 2, "800000")),
        order("O005", "C005", "2026-07-20T12:00", "2026-07-23T12:00", DELIVERED, SILVER,
            line("P007", 2, "200000"), line("P008", 1, "100000")),
        order("O006", "C006", "2026-07-25T08:00", "2026-07-26T08:00", DELIVERED, GOLD,
            line("P010", 1, "450000"), line("P006", 10, "10000.0")),
        order("O007", "C001", "2026-08-01T09:00", "2026-08-03T09:00", DELIVERED, GOLD,
            line("P003", 1, "800000"), line("P002", 1, "250000.00")),
        order("O008", "C002", "2026-08-05T09:00", "2026-08-06T09:00", DELIVERED, SILVER,
            line("P001", 1, "180000"), line("P001", 2, "170000")),
        order("O009", "C003", "2026-08-10T10:00", "2026-08-12T10:00", DELIVERED, SILVER,
            line("P009", 2, "250000.0")),
        order("O010", "C004", "2026-08-12T11:00", "2026-08-14T11:00", DELIVERED, STANDARD,
            line("P004", 1, "300000"), line("P005", 2, "50000")),
        order("O011", "C007", "2026-08-15T08:00", null, CANCELLED, STANDARD,
            line("P008", 3, "100000")),
        order("O012", "C006", "2026-08-20T12:00", "2026-08-22T12:00", DELIVERED, GOLD,
            line("P002", 2, "250000"), line("P009", 2, "250000")),
        order("O013", "C001", "2026-09-01T09:00", "2026-09-02T09:00", DELIVERED, GOLD,
            line("P007", 1, "200000"), line("P006", 10, "10000")),
        order("O014", "C005", "2026-09-05T10:00", "2026-09-07T10:00", DELIVERED, SILVER,
            line("P003", 1, "800000"), line("P004", 2, "300000")),
        order("O015", "C008", "2026-09-10T11:00", "2026-09-12T11:00", DELIVERED, SILVER,
            line("P001", 2, "180000"), line("P008", 2, "100000")),
        order("O016", "C004", "2026-09-15T14:00", "2026-09-17T14:00", DELIVERED, STANDARD,
            line("P005", 10, "45000"), line("P006", 10, "10000")),
        order("O017", "C003", "2026-09-20T15:00", "2026-09-21T15:00", DELIVERED, SILVER,
            line("P007", 1, "200000"), line("P008", 1, "100000")),
        order("O018", "C008", "2026-09-25T10:00", null, CONFIRMED, SILVER,
            line("P003", 1, "800000")),
        order("O019", "C007", "2026-09-30T23:59:59", null, NEW, STANDARD,
            line("P002", 1, "250000")),
        order("O020", "C006", "2026-09-28T10:00", "2026-09-30T10:00", DELIVERED, GOLD,
            line("P004", 1, "300000"), line("P008", 2, "100000")),
        order("O021", "C009", "2026-06-30T23:59:59", "2026-07-02T09:00", DELIVERED, STANDARD,
            line("P001", 1, "180000")),
        order("O022", "C009", "2026-10-01T00:00", null, NEW, STANDARD,
            line("P012", 2, "50000")),
        order("O023", "C002", "2026-09-26T10:00", "2026-09-27T10:00", DELIVERED, SILVER,
            line("P002", 1, "250000")),
        order("O024", "C005", "2026-09-26T10:00", "2026-09-27T10:00", DELIVERED, SILVER,
            line("P009", 1, "250000.00"))
    )

    /**
     * 12 tình huống đơn hàng không hợp lệ để kiểm thử độc lập trên dữ liệu gốc.
     *
     * Lưu ý thú vị về Null-Safety giữa Java vs Kotlin:
     * Trong `SampleData.java` của Java Core, `X009` (`unitPrice = null`), `X010` (`lines = null`)
     * và `X011` (`OrderLine = null`) tồn tại vì Java không chặn được `null` lúc Compile-time!
     * Sang Kotlin, nhờ khai báo kiểu Non-null `unitPrice: BigDecimal` và `lines: List<OrderLine>`,
     * trình biên dịch Kotlin đã **tiêu diệt hoàn toàn lỗi truyền `null` ngay từ lúc gõ code**!
     * Vì vậy, ở đây `X009`, `X010`, `X011` được dùng để kiểm thử các ca biên dữ liệu rỗng/0:
     * - `X009`: Giá bán bằng `0.00` (không dương)
     * - `X010`: Mã đơn hàng chỉ chứa khoảng trắng (`"   "`)
     * - `X011`: Mã sản phẩm trong dòng hàng chỉ chứa khoảng trắng (`"   "`)
     */
    fun invalidOrders(): List<Order> = listOf(
        // X001: Khách hàng C999 không tồn tại
        order("X001", "C999", "2026-09-01T09:00", null, NEW, STANDARD,
            line("P001", 1, "180000")),
        // X002: Sản phẩm P999 không tồn tại
        order("X002", "C001", "2026-09-01T09:00", null, NEW, GOLD,
            line("P999", 1, "100000")),
        // X003: Số lượng bằng 0 (không dương)
        order("X003", "C001", "2026-09-01T09:00", null, NEW, GOLD,
            line("P001", 0, "180000")),
        // X004: Đơn giá âm (-180000)
        order("X004", "C001", "2026-09-01T09:00", null, NEW, GOLD,
            line("P001", 1, "-180000")),
        // X005: Danh sách dòng hàng rỗng
        order("X005", "C001", "2026-09-01T09:00", null, NEW, GOLD),
        // X006: Đơn DELIVERED nhưng ngày giao (2026-08-31) trước ngày tạo (2026-09-01)
        order("X006", "C001", "2026-09-01T09:00", "2026-08-31T09:00", DELIVERED, GOLD,
            line("P001", 1, "180000")),
        // X007: Đơn DELIVERED nhưng thiếu ngày giao (deliveredAt = null)
        order("X007", "C001", "2026-09-01T09:00", null, DELIVERED, GOLD,
            line("P001", 1, "180000")),
        // O001: Trùng mã đơn hàng đã tồn tại trong lịch sử
        order("O001", "C001", "2026-09-01T09:00", null, NEW, GOLD,
            line("P001", 1, "180000")),
        // X009: Đơn giá bằng 0 (không dương)
        order("X009", "C001", "2026-09-01T09:00", null, NEW, GOLD,
            line("P001", 1, "0.00")),
        // X010: Mã đơn hàng rỗng/chỉ chứa khoảng trắng
        order("   ", "C001", "2026-09-01T09:00", null, NEW, GOLD,
            line("P001", 1, "180000")),
        // X011: Mã sản phẩm trong dòng hàng rỗng/chỉ chứa khoảng trắng
        order("X011", "C001", "2026-09-01T09:00", null, NEW, GOLD,
            line("   ", 1, "180000")),
        // X012: Đơn ở trạng thái NEW (chưa giao) nhưng lại có deliveredAt khác null
        order("X012", "C001", "2026-09-01T09:00", "2026-09-02T09:00", NEW, GOLD,
            line("P001", 1, "180000"))
    )

    private fun customer(
        id: String,
        name: String,
        email: String?,
        city: String,
        tier: MemberTier,
        joinedAt: String,
        updatedAt: String
    ): Customer = Customer(
        id = id,
        name = name,
        email = email,
        city = city,
        currentTier = tier,
        joinedAt = LocalDate.parse(joinedAt),
        updatedAt = LocalDateTime.parse(updatedAt)
    )

    private fun product(
        id: String,
        name: String,
        category: Category,
        price: String,
        active: Boolean
    ): Product = Product(
        id = id,
        name = name,
        category = category,
        listPrice = BigDecimal(price),
        active = active
    )

    private fun line(productId: String, quantity: Int, price: String): OrderLine =
        OrderLine(productId = productId, quantity = quantity, unitPrice = BigDecimal(price))

    private fun order(
        id: String,
        customerId: String,
        createdAt: String,
        deliveredAt: String?,
        status: OrderStatus,
        tier: MemberTier,
        vararg lines: OrderLine
    ): Order = Order(
        id = id,
        customerId = customerId,
        createdAt = LocalDateTime.parse(createdAt),
        deliveredAt = deliveredAt?.let(LocalDateTime::parse),
        status = status,
        tierAtPurchase = tier,
        lines = lines.toList()
    )
}
