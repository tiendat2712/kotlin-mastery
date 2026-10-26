package view

/**
 * ============================================================================
 * BÀI 01: `data class` DEEP DIVE & CẠM BẪY CHẾT NGƯỜI VỚI JPA `@Entity`
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ----------------------------------------------------------------------------
 * Khi bạn thêm từ khóa `data` trước `class OrderDto(...)`, trình biên dịch Kotlin
 * tự động sinh ra 5 nhóm phương thức ở tầng Bytecode (dựa trên CÁC THUỘC TÍNH TRONG
 * PRIMARY CONSTRUCTOR):
 *   1. `equals(other: Any?): Boolean` -> So sánh giá trị từng thuộc tính (Structural Equality `==`).
 *   2. `hashCode(): Int`              -> Băm kết hợp tất cả thuộc tính trong Primary Constructor.
 *   3. `toString(): String`           -> Định dạng đẹp `"OrderDto(id=..., amount=..., status=...)"`.
 *   4. `copy(...): OrderDto`          -> Tạo một bản sao nông (shallow copy) trên HEAP, cho phép
 *                                        đổi một vài thuộc tính bằng Named Arguments mà vẫn giữ
 *                                        tính bất biến (`val` - Immutability).
 *   5. `component1()`, `component2()`, ... `componentN()` -> Phục vụ cú pháp Destructuring
 *                                        (`val (id, amount, status) = order`).
 *
 * LƯU Ý QUAN TRỌNG VỀ PHẠM VI:
 * Các thuộc tính khai báo **bên trong thân class `{ ... }`** (không nằm trong Primary Constructor)
 * sẽ **KHÔNG** được đưa vào `equals()`, `hashCode()`, `toString()`, và `copy()`!
 *
 * 2. SO SÁNH JAVA vs KOTLIN:
 * ----------------------------------------------------------------------------
 * - Trong Java (`lesson03-oop`): Bạn phải tự override `equals()` + `hashCode()` (hoặc dùng
 *   Lombok `@Data`), hoặc dùng Java 16+ `record` (nhưng Java `record` không có hàm `.copy(...)`
 *   với named arguments tiện lợi như Kotlin!).
 *
 * 3. SPRING / JPA TRAP (CHỦ ĐỀ PHỎNG VẤN & PRODUCTION CỰC KỲ QUAN TRỌNG!):
 * ----------------------------------------------------------------------------
 * CÂU HỎI: "Tại sao `data class` là lựa chọn hoàn hảo cho DTO (Request/Response), nhưng lại
 *           ĐƯỢC KHUYẾN CÁO TUYỆT ĐỐI KHÔNG DÙNG cho JPA `@Entity` (Hibernate)?"
 *
 * TRẢ LỜI (3 lý do kỹ thuật cốt tử):
 *   1. `data class` trong Kotlin KHÔNG THỂ đánh dấu `open` (luôn là `final`), trong khi
 *      Hibernate Lazy Loading cần tạo Proxy kế thừa class `@Entity`. Dù có plugin `kotlin("plugin.jpa")`
 *      giúp tạo no-arg constructor, bản thân `data class` vẫn xung đột triết lý với Hibernate.
 *   2. Cạm bẫy `hashCode()` khi lưu vào `HashSet`:
 *      Một JPA Entity thường có `@Id @GeneratedValue var id: Long? = null` và các trường `var`
 *      thay đổi được. `data class` tính `hashCode()` dựa trên TẤT CẢ thuộc tính. Khi Entity mới tạo
 *      (`id = null`) được đưa vào `HashSet`, rồi sau khi `repository.save(entity)`, Hibernate gán
 *      `id = 100L` -> `hashCode()` của object thay đổi ngay khi đang nằm trong `HashSet`, làm hỏng
 *      bucket của `HashSet` (tìm lại chính object đó trong Set sẽ trả về `false`!).
 *   3. Cạm bẫy `LazyInitializationException` & Đệ quy vô tận (`StackOverflowError`):
 *      Nếu Entity `Order` có `@OneToMany(fetch = LAZY) val items: List<OrderItem>` nằm trong
 *      Primary Constructor của `data class`, bất kỳ khi nào bạn gọi `println(order)` (`toString`)
 *      hoặc `order1 == order2` (`equals`), `data class` sẽ tự động chạm vào danh sách `items` ->
 *      kích hoạt truy vấn SQL ngoài ý muốn hoặc nổ `LazyInitializationException` nếu Session đã đóng!
 *
 * GIẢI PHÁP CHUẨN CHO JPA `@Entity` TRONG KOTLIN:
 * Dùng `class` thường (kết hợp plugin `kotlin("plugin.jpa")` và `kotlin("plugin.allopen")`),
 * và viết `equals()`/`hashCode()` chỉ dựa trên khóa định danh (`id` khác null) hoặc Business Key!
 */

// 1. DTO chuẩn mực với `data class` (Bất biến hoàn toàn với `val`)
private data class OrderSummaryDto(
    val orderId: String,
    val totalAmount: Double,
    val status: String
) {
    // Thuộc tính khai báo trong THÂN CLASS -> Bị loại trừ khỏi equals/hashCode/toString/copy!
    var cachedRenderTimestamp: Long = 0L
}

// 2. Mô phỏng cách viết JPA @Entity chuẩn mực trong Spring Data JPA + Kotlin (KHÔNG dùng data class)
private open class CustomerJpaEntity(
    var id: Long? = null,
    var email: String, // Business Key hoặc mutable field
    var fullName: String
) {
    // Chuẩn Hibernate/JPA: Chỉ so sánh dựa trên ID khi ID đã được DB cấp phát (khác null),
    // hoặc kiểm tra cùng tham chiếu bộ nhớ (`this === other`).
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomerJpaEntity) return false
        return id != null && id == other.id
    }

    // Trả về hằng số (class hashCode) để khi `id` chuyển từ `null` -> `101L` sau khi save(),
    // hash bucket trong HashSet<CustomerJpaEntity> KHÔNG BAO GIỜ bị hỏng!
    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "CustomerJpaEntity(id=$id, email='$email', fullName='$fullName')"
}

fun main() {
    println("--- ___ * START: Ex01DataClassDeepDive * ___ ---")

    // ========================================================================
    // 1. Structural Equality (==), toString(), copy() và Destructuring
    // ========================================================================
    println("\n[1] Sức mạnh của `data class` cho DTO / Value Object:")
    val dto1 = OrderSummaryDto("ORD-101", 1500.0, "PENDING").apply {
        cachedRenderTimestamp = 111111L
    }
    val dto2 = OrderSummaryDto("ORD-101", 1500.0, "PENDING").apply {
        cachedRenderTimestamp = 999999L
    }

    println("  dto1.toString()        : $dto1")
    // Dù `cachedRenderTimestamp` khác nhau (111111L vs 999999L), nhưng vì nó nằm trong thân class
    // nên dto1 == dto2 vẫn là TRUE!
    println("  dto1 == dto2           : ${dto1 == dto2} (bỏ qua thuộc tính trong thân class)")

    // Cập nhật trạng thái bất biến bằng `.copy(...)`
    val confirmedDto = dto1.copy(status = "CONFIRMED")
    println("  Sau khi .copy(...)     : $confirmedDto (dto1 gốc vẫn là ${dto1.status})")

    // Destructuring declaration (gọi ngầm component1(), component2(), component3())
    val (id, amount, currentStatus) = confirmedDto
    println("  Destructuring          : id=$id, amount=$amount, status=$currentStatus")

    // ========================================================================
    // 2. Minh họa cách JPA Entity chuẩn giữ an toàn cho HashSet khi ID thay đổi
    // ========================================================================
    println("\n[2] Kiểm chứng JPA Entity chuẩn mực trong HashSet trước và sau khi DB cấp ID:")
    val entity = CustomerJpaEntity(id = null, email = "user@spring.io", fullName = "Spring User")
    val entitySet = hashSetOf(entity)

    println("  Trước khi save (id=null) -> entitySet.contains(entity): ${entitySet.contains(entity)}")

    // Giả lập Hibernate gán ID sau câu lệnh INSERT INTO
    entity.id = 42L
    println("  Sau khi save   (id=42L)  -> entitySet.contains(entity): ${entitySet.contains(entity)}")

    println("\n--- ___ * FINISH: Ex01DataClassDeepDive * ___ ---")
}
