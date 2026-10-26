package view

/**
 * ============================================================================
 * BÀI 01: `data class` DEEP DIVE, SHALLOW COPY & CẠM BẪY VỚI JPA `@Entity`
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
 *   4. `copy(...): OrderDto`          -> Tạo một bản sao nông (**SHALLOW COPY**) trên HEAP, cho phép
 *                                        đổi một vài thuộc tính bằng Named Arguments.
 *   5. `component1()`, `component2()`, ... `componentN()` -> Phục vụ cú pháp Destructuring
 *                                        (`val (id, amount, status) = order`).
 *
 * LƯU Ý QUAN TRỌNG VỀ PHẠM VI & TÍNH BẤT BIẾN (IMMUTABILITY):
 * - Các thuộc tính khai báo **bên trong thân class `{ ... }`** (không nằm trong Primary Constructor)
 *   sẽ **KHÔNG** được đưa vào `equals()`, `hashCode()`, `toString()`, và `copy()`!
 * - `data class` với các thuộc tính `val` là một **lựa chọn phổ biến, tiện lợi cho DTO / Value Object**,
 *   nhưng **KHÔNG tự động đảm bảo tính bất biến sâu (Deep Immutability)**!
 *   + Hàm `.copy()` chỉ thực hiện **Shallow Copy (sao chép nông)**: nó tạo object vỏ mới trên HEAP,
 *     nhưng các thuộc tính bên trong chỉ được chép lại **tham chiếu (reference)**.
 *   + Nếu một thuộc tính bên trong là `MutableList<T>` hoặc một đối tượng có `var`, thì cả bản gốc
 *     `original` lẫn bản sao `copy` đều **trỏ chung vào cùng 1 vùng nhớ trên HEAP**! Sửa nội dung
 *     bên trong `copy` sẽ làm biến đổi luôn `original`!
 *
 * 2. SO SÁNH JAVA vs KOTLIN:
 * ----------------------------------------------------------------------------
 * - Trong Java (`lesson03-oop`): Bạn phải tự override `equals()` + `hashCode()` (hoặc dùng
 *   Lombok `@Data`), hoặc dùng Java 16+ `record` (nhưng Java `record` không có hàm `.copy(...)`
 *   với named arguments tiện lợi như Kotlin!).
 *
 * 3. SPRING / JPA TRAP (CHỦ ĐỀ PHỎNG VẤN & PRODUCTION CỰC KỲ QUAN TRỌNG!):
 * ----------------------------------------------------------------------------
 * CÂU HỎI: "Tại sao `data class` rất tiện cho DTO (Request/Response), nhưng lại
 *           không phù hợp cho JPA `@Entity` (Hibernate), và cài đặt `equals`/`hashCode`
 *           cho `@Entity` như thế nào mới chuẩn?"
 *
 * TRẢ LỜI KỸ THUẬT:
 *   A. Tại sao không dùng `data class` cho `@Entity`?
 *      1. `data class` trong Kotlin luôn là `final` (không thể `open`), trong khi Hibernate Lazy
 *         Loading cần tạo Proxy kế thừa class `@Entity`.
 *         *Lưu ý Kotlin 2.0.21*: Plugin `kotlin("plugin.jpa")` (`no-arg`) CHỈ tự sinh no-arg
 *         constructor cho `@Entity`, chứ **KHÔNG tự động biến `@Entity` thành `open`**! Muốn
 *         `@Entity` tự động `open` mà không gõ tay, cần cấu hình thêm
 *         `allOpen { annotation("jakarta.persistence.Entity") }` hoặc viết `open class`.
 *      2. Cạm bẫy `LazyInitializationException` & `StackOverflowError`:
 *         Nếu `@OneToMany(fetch = LAZY)` nằm trong Primary Constructor của `data class`, việc gọi
 *         `toString()`, `equals()`, hay `hashCode()` sẽ tự động chạm vào danh sách LAZY -> kích hoạt
 *         câu lệnh SQL ngầm hoặc nổ `LazyInitializationException` khi Session đã đóng!
 *
 *   B. Cạm bẫy `id.hashCode()` với `@GeneratedValue` và 2 hướng giải quyết chuẩn cho JPA `@Entity`:
 *      - **CẢNH BÁO**: Tuyệt đối KHÔNG viết `override fun hashCode() = id.hashCode()` khi `id` được
 *        Database sinh tự động (`@GeneratedValue var id: Long? = null`). Vì lúc mới khởi tạo (`id = null`),
 *        nếu bỏ Entity vào `HashSet`, mã băm là `0`. Sau khi `entityManager.persist(entity)`, DB cấp
 *        `id = 42L` -> mã băm đổi thành `42` ngay khi đang nằm trong `HashSet`, làm lệch bucket khiến
 *        `set.contains(entity)` trả về `false`!
 *      - **Hướng 1: Dùng Business Key / Natural ID bất biến (`@NaturalId val code: String` hoặc `UUID` tạo từ Client)**:
 *        Nếu thực thể có một khóa nghiệp vụ duy nhất, biết ngay từ lúc khởi tạo và không bao giờ đổi
 *        (ví dụ `val taxCode: String` hoặc `val externalUuid: UUID`), ta dùng khóa đó cho cả
 *        `equals()` và `hashCode()`. Cách này phân tán đều các bucket trong `HashSet`/`HashMap` ($O(1)$).
 *      - **Hướng 2: Dùng Constant `hashCode()` theo lớp kết hợp `id != null && id == other.id` (và lưu ý giới hạn với Hibernate Proxy)**:
 *        + Ý tưởng trong mô phỏng thuần Kotlin: Khi chưa có Business Key và phải dùng `@GeneratedValue` ID,
 *          việc trả về một mã băm hằng số không phụ thuộc vào `id` giúp mã băm KHÔNG ĐỔI trước và sau khi `persist()`,
 *          giữ cho `HashSet` không bị lệch bucket khi `id` được gán giá trị.
 *        + **Hai giới hạn kỹ thuật cần nhớ khi bước sang Hibernate thực tế**:
 *          1. *Đánh đổi hiệu năng bucket*: Mọi thực thể cùng loại rơi vào cùng 1 bucket trong `HashSet` ($O(N)$),
 *             nên tránh nhét hàng nghìn thực thể vào cùng một `HashSet` lớn.
 *          2. *Giới hạn với Hibernate Subclass Proxy*: Trong Hibernate thật, một đối tượng thực thể (`CustomerJpaEntity`)
 *             và đối tượng Lazy Proxy của nó (`CustomerJpaEntity$HibernateProxy$...`) có `this.javaClass` KHÁC NHAU!
 *             Nếu `equals` dùng `other is CustomerJpaEntity` (chấp nhận cả proxy là bằng nhau) nhưng `hashCode()` lại viết
 *             `javaClass.hashCode()` trực tiếp, thì thực thể thật và proxy sẽ bằng nhau theo `equals` nhưng khác `hashCode()`
 *             (vi phạm hợp đồng `equals`/`hashCode`!). Do đó, `javaClass.hashCode()` dưới đây chỉ là minh họa đơn giản trong
 *             repo Kotlin Core (chưa có thư viện Hibernate); khi vào dự án Hibernate thực tế, bạn sẽ cần xử lý mở lớp gốc
 *             của proxy (ví dụ qua `HibernateProxy` / `Hibernate.getClass(this)`).
 */

// 1. DTO tiện lợi với `data class` (chứa các kiểu dữ liệu bất biến `String`, `Double`)
private data class OrderSummaryDto(
    val orderId: String,
    val totalAmount: Double,
    val status: String
) {
    // Thuộc tính khai báo trong THÂN CLASS -> Bị loại trừ khỏi equals/hashCode/toString/copy!
    var cachedRenderTimestamp: Long = 0L
}

// 2. Minh họa `.copy()` chỉ là SHALLOW COPY (Sao chép nông)
private data class OrderDraft(
    val id: String,
    val tags: MutableList<String> // CẢNH BÁO: Dùng MutableList bên trong data class!
)

// 3. Mô phỏng JPA @Entity trong Kotlin Core (Minh họa mã băm không đổi khi id chuyển từ null -> 42L;
// lưu ý trong Hibernate thực tế có Lazy Proxy thì cần xét thêm HibernateProxy thay vì chỉ gọi javaClass trực tiếp)
private open class CustomerJpaEntity(
    var id: Long? = null,
    var email: String,
    var fullName: String
) {
    // Chỉ coi 2 thực thể khác tham chiếu là bằng nhau khi CẢ HAI đã được DB cấp ID (id != null) và trùng ID
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomerJpaEntity) return false
        return id != null && id == other.id
    }

    // Minh họa mã băm cố định trong phạm vi Kotlin Core (khi chưa có Hibernate Subclass Proxy)
    override fun hashCode(): Int = CustomerJpaEntity::class.java.hashCode()

    override fun toString(): String = "CustomerJpaEntity(id=$id, email='$email', fullName='$fullName')"
}

fun main() {
    println("--- ___ * START: Ex01DataClassDeepDive * ___ ---")

    // ========================================================================
    // 1. Structural Equality (==), toString(), copy() và Destructuring
    // ========================================================================
    println("\n[1] Su tien loi cua `data class` cho DTO / Value Object:")
    val dto1 = OrderSummaryDto("ORD-101", 1500.0, "PENDING").apply {
        cachedRenderTimestamp = 111111L
    }
    val dto2 = OrderSummaryDto("ORD-101", 1500.0, "PENDING").apply {
        cachedRenderTimestamp = 999999L
    }

    println("  dto1.toString()        : $dto1")
    // Dù `cachedRenderTimestamp` khác nhau (111111L vs 999999L), nhưng vì nó nằm trong thân class
    // nên dto1 == dto2 vẫn là TRUE!
    println("  dto1 == dto2           : ${dto1 == dto2} (bo qua thuoc tinh trong than class)")
    check(dto1 == dto2)

    // Cập nhật trạng thái bằng `.copy(...)`
    val confirmedDto = dto1.copy(status = "CONFIRMED")
    println("  Sau khi .copy(...)     : $confirmedDto (dto1 goc van la ${dto1.status})")
    check(dto1.status == "PENDING" && confirmedDto.status == "CONFIRMED")

    // Destructuring declaration (gọi ngầm component1(), component2(), component3())
    val (id, amount, currentStatus) = confirmedDto
    println("  Destructuring          : id=$id, amount=$amount, status=$currentStatus")

    // ========================================================================
    // 2. Cạm bẫy Shallow Copy của `.copy()` khi chứa thuộc tính khả biến (Mutable)
    // ========================================================================
    println("\n[2] Cam bay Shallow Copy: `data class` + `val` KHONG tu dong la Deep Immutability!")
    val originalDraft = OrderDraft(id = "ORD-1", tags = mutableListOf("NEW", "RETAIL"))
    val copiedDraft = originalDraft.copy(id = "ORD-2")

    // Cả originalDraft.tags và copiedDraft.tags đều trỏ tới CÙNG 1 danh sách trên HEAP!
    copiedDraft.tags.add("URGENT")

    println("  originalDraft.tags sau khi sua copiedDraft.tags : ${originalDraft.tags}")
    println("  copiedDraft.tags                                : ${copiedDraft.tags}")
    println("  Cung tham chieu tren HEAP (===)                 : ${originalDraft.tags === copiedDraft.tags}")
    check(originalDraft.tags === copiedDraft.tags)
    check(originalDraft.tags.contains("URGENT"))

    // Cách copy sâu (Deep Copy) an toàn nếu buộc phải tách nhánh danh sách:
    val safeCopiedDraft = originalDraft.copy(id = "ORD-3", tags = originalDraft.tags.toMutableList())
    safeCopiedDraft.tags.add("VIP_ONLY")
    check(!originalDraft.tags.contains("VIP_ONLY"))

    // ========================================================================
    // 3. Minh họa cách JPA Entity chuẩn giữ an toàn cho HashSet khi ID thay đổi
    // ========================================================================
    println("\n[3] Kiem chung JPA Entity trong HashSet truoc va sau khi DB cap ID:")
    val entity = CustomerJpaEntity(id = null, email = "user@spring.io", fullName = "Spring User")
    val entitySet = hashSetOf(entity)

    println("  Truoc khi save (id=null) -> entitySet.contains(entity): ${entitySet.contains(entity)}")
    check(entitySet.contains(entity))

    // Giả lập Hibernate gán ID sau câu lệnh INSERT INTO
    entity.id = 42L
    println("  Sau khi save   (id=42L)  -> entitySet.contains(entity): ${entitySet.contains(entity)}")
    check(entitySet.contains(entity))

    // ========================================================================
    // 4. GÓC TỰ LUYỆN (ACTIVE LEARNING)
    // ========================================================================
    println("\n[4] Goc Tu Luyen (Active Learning):")

    // ------------------------------------------------------------------------
    // Bài 1 (Dự đoán kết quả): Thuộc tính trong thân `data class` và Shallow Copy
    // ------------------------------------------------------------------------
    val p1 = OrderSummaryDto("ORD-99", 500.0, "PAID").apply { cachedRenderTimestamp = 10L }
    val p2 = p1.copy() // Không truyền tham số -> giữ nguyên các thuộc tính trong Primary Constructor
    // Hỏi: `p2.cachedRenderTimestamp` bằng `10L` hay `0L`? Và `p1 == p2` là `true` hay `false`?
    println("  [Bai 1 - Du doan] p2.cachedRenderTimestamp = ${p2.cachedRenderTimestamp}, p1 == p2 = ${p1 == p2}")
    check(p2.cachedRenderTimestamp == 0L) // Vì copy() chỉ chép các thuộc tính trong Primary Constructor!
    check(p1 == p2)

    // ------------------------------------------------------------------------
    // Bài 2 (Tìm lỗi): Dùng `id.hashCode()` cho JPA Entity có `@GeneratedValue`
    // ------------------------------------------------------------------------
    class BrokenJpaEntity(var id: Long? = null, var name: String) {
        override fun equals(other: Any?): Boolean =
            this === other || (other is BrokenJpaEntity && this.id != null && this.id == other.id)

        // LỖI NGHIÊM TRỌNG: Khi id đổi từ null -> 100L sau persist(), hashCode() bị thay đổi!
        override fun hashCode(): Int = this.id?.hashCode() ?: 0
    }
    val broken = BrokenJpaEntity(id = null, name = "Broken Item")
    val brokenSet = hashSetOf(broken)
    broken.id = 100L // Giả lập DB cấp ID sau khi nằm trong HashSet
    println("  [Bai 2 - Tim loi] brokenSet.contains(broken) sau khi gan ID = ${brokenSet.contains(broken)} (Mat dau trong HashSet!)")
    check(!brokenSet.contains(broken))

    // ------------------------------------------------------------------------
    // Bài 3 (Tự viết nhỏ):
    // ------------------------------------------------------------------------
    // [ĐỀ BÀI]:
    // Hãy viết hàm `cloneWithExtraTag(draft: OrderDraft, newId: String, extraTag: String): OrderDraft`
    // sử dụng `.copy(...)` nhưng đảm bảo danh sách `tags` của bản sao hoàn toàn độc lập trên HEAP
    // (không làm ảnh hưởng tới `draft.tags` ban đầu).

    // [LỜI GIẢI THAM KHẢO]:
    fun cloneWithExtraTag(draft: OrderDraft, newId: String, extraTag: String): OrderDraft {
        val independentTags = draft.tags.toMutableList().apply { add(extraTag) }
        return draft.copy(id = newId, tags = independentTags)
    }

    val baseDraft = OrderDraft("ORD-BASE", mutableListOf("STANDARD"))
    val clonedDraft = cloneWithExtraTag(baseDraft, "ORD-CLONE", "FLASH_SALE")
    println("  [Bai 3 - Tu viet] baseDraft.tags=${baseDraft.tags} | clonedDraft.tags=${clonedDraft.tags}")
    check(baseDraft.tags == listOf("STANDARD"))
    check(clonedDraft.tags == listOf("STANDARD", "FLASH_SALE"))
    check(baseDraft.tags !== clonedDraft.tags)

    println("\n--- ___ * FINISH: Ex01DataClassDeepDive * ___ ---")
}
