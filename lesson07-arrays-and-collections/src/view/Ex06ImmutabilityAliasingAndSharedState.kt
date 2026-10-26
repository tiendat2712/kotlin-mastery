package view

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * ============================================================================
 * BÀI 06: ALIASING, SHALLOW COPY (`toList()`) & SHARED MUTABLE STATE ĐA LUỒNG
 * ============================================================================
 *
 * Bài học này giải phẫu 3 ngộ nhận nguy hiểm nhất về tính bất biến (Immutability)
 * và an toàn luồng (Thread-safety) khi làm việc với Collection trong Kotlin & Spring Boot:
 *
 * 1. NGỘ NHẬN 1: "Kiểu `val list: List<T>` là bất biến (Immutable)" -> SAI!
 * ----------------------------------------------------------------------------
 * - `List<T>` trong Kotlin chỉ là một **Read-Only Interface (Góc nhìn chỉ đọc)**,
 *   tức là trình biên dịch ẩn đi các hàm `add()`, `remove()`.
 * - Nhưng nếu đối tượng thực sự trên HEAP đằng sau `List<T>` đó đang bị một biến
 *   `MutableList<T>` khác nắm giữ (**Mutable Aliasing**), thì khi biến `MutableList<T>`
 *   thêm/xóa phần tử, `List<T>` cũng bị thay đổi theo ngay lập tức!
 *
 * 2. NGỘ NHẬN 2: "Gọi `.toList()` hoặc `.map { it.copy() }` là đạt Deep Immutability" -> CHƯA ĐÚNG!
 * ----------------------------------------------------------------------------
 * - Hàm `.toList()` tạo ra một danh sách chỉ đọc tách biệt cấu trúc với collection nguồn tại thời điểm gọi
 *   (**Shallow Snapshot / Shallow Copy** — lưu ý trong stdlib Kotlin, `.toList()` không cam kết luôn cấp phát
 *   `ArrayList` mới cho mọi trường hợp vì có tối ưu trả về `EmptyList` khi rỗng hoặc `SingletonList` khi có 1 phần tử).
 * - Nó bảo vệ được **cấu trúc danh sách** (thêm/xóa phần tử ở list gốc sau đó không ảnh hưởng tới snapshot).
 * - NHƯNG nếu phần tử `T` bên trong là một đối tượng khả biến (`class MutablePriceItem(val sku: String, var price: Long)`),
 *   thì cả 2 danh sách vẫn trỏ chung vào cùng các đối tượng `MutablePriceItem` trên HEAP!
 *   Gọi `shallowDefensiveList[0].price = 0L` sẽ làm hỏng luôn `originalList[0].price`!
 * - **Phân biệt rõ "Sao chép phần tử (Defensive Element Copy)" vs "Deep Immutability"**:
 *   + **Sao chép phần tử (`original.map { it.copyItem() }`)**:
 *     * Với model phẳng (`MutablePriceItem` chỉ chứa `String` và `Long`), `copyItem()` giúp tách vùng nhớ phần tử
 *       để việc sửa bản sao không làm hỏng bản gốc (**Defensive Copy cho model phẳng**).
 *     * Tuy nhiên, bản sao tạo ra **vẫn là đối tượng khả biến (mutable)**, chứ KHÔNG biến thành bất biến (immutable)!
 *     * Hơn nữa, không được tổng quát hóa `original.map { it.copy() }` thành công thức Deep Copy cho mọi `data class`:
 *       theo tài liệu chính thức của Kotlin, hàm `.copy()` của `data class` chỉ là **Shallow Copy (sao chép nông)**.
 *       Nếu phần tử bên trong còn chứa một `MutableList` hay object khả biến lồng nhau, `.copy()` mặc định vẫn dùng chung
 *       tham chiếu lồng nhau đó!
 *   + **Đạt Deep Immutability thực sự**:
 *     * Thiết kế bản thân phần tử `T` và toàn bộ các thuộc tính/collection lồng bên trong đều bất biến
 *       (ví dụ `data class ImmutablePriceItem(val sku: String, val price: Long)` chỉ dùng `val` với các kiểu bất biến),
 *       kết hợp với việc không để lộ tham chiếu `MutableList` của container ra ngoài.
 *
 * 3. NGỘ NHẬN 3: "Singleton `object` + Read-Only `Map`/`List` là Thread-Safe" -> SAI!
 * ----------------------------------------------------------------------------
 * - Trong bài 03 của Lesson 06, bạn đã biết `object AppCache` được JVM **khởi tạo** an toàn
 *   đa luồng (Thread-safe initialization). Tương tự, một `@Service` trong Spring Boot mặc định
 *   là một Singleton Bean dùng chung cho hàng trăm HTTP Request Threads cùng lúc.
 * - Tuy nhiên, **khởi tạo Singleton an toàn KHÔNG đồng nghĩa với trạng thái khả biến bên trong
 *   Singleton đó an toàn đa luồng**!
 * - Nếu bên trong `object` hoặc `@Service` bạn để:
 *     + `var totalRequests: Int = 0` (`totalRequests++` không phải là thao tác nguyên tử - non-atomic!)
 *     + `private val _cache = mutableMapOf<String, Int>()` (`LinkedHashMap`/`HashMap` không đồng bộ!)
 *     + Và lộ ra ngoài qua `val cache: Map<String, Int> get() = _cache`
 *   -> Khi nhiều luồng cùng ghi và đọc, chương trình sẽ bị **Lost Update (mất dữ liệu đếm)**,
 *      `ConcurrentModificationException`, hoặc đọc phải trạng thái hỏng!
 * - **Giải pháp chuẩn trong Backend / Spring Boot**:
 *   + Dùng `AtomicInteger` / `AtomicLong` cho bộ đếm đa luồng.
 *   + Dùng `ConcurrentHashMap<K, V>` (với các thao tác nguyên tử như `compute`, `merge`, `putIfAbsent`)
 *     hoặc `AtomicReference<Map<K, V>>` chứa các bản snapshot bất biến!
 */

// Minh họa cho phần 2: Phần tử khả biến (Mutable Element) vs Phần tử bất biến (Immutable Element)
private class MutablePriceItem(val sku: String, var price: Long) {
    fun copyItem(): MutablePriceItem = MutablePriceItem(sku, price)
    override fun toString(): String = "MutablePriceItem(sku='$sku', price=$price)"
}

private data class ImmutablePriceItem(val sku: String, val price: Long)

// Minh họa cho phần 3: So sánh Shared Mutable State KHÔNG an toàn vs An toàn đa luồng (Thread-Safe)
private class UnsafeMetricsRegistry {
    var requestCount: Int = 0
        private set

    private val _endpointHits = mutableMapOf<String, Int>()
    // Lộ trực tiếp Read-Only view của một MutableMap không đồng bộ!
    val endpointHits: Map<String, Int>
        get() = _endpointHits

    fun recordHit(endpoint: String) {
        requestCount++ // Race condition: Read-Modify-Write không nguyên tử!
        _endpointHits[endpoint] = (_endpointHits[endpoint] ?: 0) + 1
    }
}

private class SafeMetricsRegistry {
    private val _requestCount = AtomicInteger(0)
    val requestCount: Int
        get() = _requestCount.get()

    private val _endpointHits = ConcurrentHashMap<String, AtomicInteger>()

    // Trả về một bản chụp (snapshot) độc lập tại thời điểm đọc
    fun snapshotHits(): Map<String, Int> =
        _endpointHits.mapValues { (_, atomicCount) -> atomicCount.get() }

    fun recordHit(endpoint: String) {
        _requestCount.incrementAndGet()
        _endpointHits.computeIfAbsent(endpoint) { AtomicInteger(0) }.incrementAndGet()
    }
}

fun main() {
    println("--- ___ * START: Ex06ImmutabilityAliasingAndSharedState * ___ ---")

    // ========================================================================
    // 1. Mutable Aliasing trên Read-Only `List<T>`
    // ========================================================================
    println("\n[1] Mutable Aliasing: `val readOnlyView: List<String>` KHONG ngan duoc thay doi tu backing list!")
    val backing: MutableList<String> = mutableListOf("A", "B")
    val readOnlyView: List<String> = backing // Cùng trỏ tới 1 ArrayList trên HEAP!

    println("  Truoc khi sua backing -> readOnlyView = $readOnlyView (size=${readOnlyView.size})")
    backing.add("C")
    println("  Sau khi backing.add   -> readOnlyView = $readOnlyView (size=${readOnlyView.size})")
    check(readOnlyView.size == 3 && readOnlyView == listOf("A", "B", "C"))

    // ========================================================================
    // 2. Shallow Copy (`toList()`) với các phần tử khả biến (Mutable Elements)
    // ========================================================================
    println("\n[2] Shallow Copy (`.toList()`) voi phan tu kha bien (`MutablePriceItem`):")
    val originalList = listOf(
        MutablePriceItem("SKU-LAPTOP", 30_000_000L),
        MutablePriceItem("SKU-MOUSE", 500_000L)
    )
    // `.toList()` chỉ tạo vỏ List mới, nhưng các phần tử bên trong vẫn dùng chung tham chiếu!
    val shallowDefensiveList = originalList.toList()

    // Sửa thuộc tính `var price` trên phần tử của bản sao nông:
    shallowDefensiveList[0].price = 0L
    println("  Sau khi gan shallowDefensiveList[0].price = 0L:")
    println("    originalList[0]         : ${originalList[0]} (BI HONG DU LIEU GOC!)")
    println("    shallowDefensiveList[0] : ${shallowDefensiveList[0]}")
    check(originalList[0].price == 0L)
    check(originalList[0] === shallowDefensiveList[0])

    // HƯỚNG XỬ LÝ 1 (Khi buộc phải làm việc với model phẳng khả biến):
    // Sao chép phòng thủ từng phần tử (`copyItem()`). Lưu ý: Bản sao `copiedElementsList[0]` vẫn là
    // đối tượng mutable (chưa phải Deep Immutability), nhưng đã tách rời khỏi `restoredList[0]`!
    // (Nếu model có chứa tập hợp khả biến lồng bên trong, `data class.copy()` mặc định chỉ là shallow copy nên sẽ phải tự copy đệ quy từng tầng).
    val restoredList = listOf(MutablePriceItem("SKU-LAPTOP", 30_000_000L))
    val copiedElementsList = restoredList.map { it.copyItem() }
    copiedElementsList[0].price = 999L
    println("  Sau khi sao chep phan tu cho model phang (`map { it.copyItem() }`):")
    println("    restoredList[0]       : ${restoredList[0]} (Ban goc khong bi anh huong!)")
    println("    copiedElementsList[0] : ${copiedElementsList[0]} (Ban sao van la mutable, nhung doc lap)")
    check(restoredList[0].price == 30_000_000L)

    // HƯỚNG XỬ LÝ 2 (Đạt Deep Immutability thực sự): Thiết kế phần tử bất biến (`ImmutablePriceItem`)
    val trulyImmutableList: List<ImmutablePriceItem> = listOf(
        ImmutablePriceItem("SKU-LAPTOP", 30_000_000L)
    )
    // trulyImmutableList[0].price = 0L // ERROR lúc Compile: Val cannot be reassigned!
    check(trulyImmutableList[0].price == 30_000_000L)

    // ========================================================================
    // 3. Shared Mutable State trong Singleton / Service và Thread-Safety
    // ========================================================================
    println("\n[3] Shared Mutable State da luong: `SafeMetricsRegistry` (AtomicInteger & ConcurrentHashMap):")
    val safeRegistry = SafeMetricsRegistry()
    val threadsCount = 8
    val hitsPerThread = 2_500

    val workers = (1..threadsCount).map {
        Thread {
            repeat(hitsPerThread) {
                safeRegistry.recordHit("/api/v1/orders")
            }
        }
    }
    workers.forEach { it.start() }
    workers.forEach { it.join() }

    val expectedTotal = threadsCount * hitsPerThread
    val snapshot = safeRegistry.snapshotHits()
    println("  Tong request ky vong : $expectedTotal")
    println("  SafeRegistry count   : ${safeRegistry.requestCount}")
    println("  SafeRegistry snapshot: $snapshot")
    check(safeRegistry.requestCount == expectedTotal)
    check(snapshot["/api/v1/orders"] == expectedTotal)

    // ========================================================================
    // 4. GÓC TỰ LUYỆN (ACTIVE LEARNING)
    // ========================================================================
    println("\n[4] Goc Tu Luyen (Active Learning):")

    // ------------------------------------------------------------------------
    // Bài 1 (Dự đoán kết quả): Aliasing vs `.toList()`
    // ------------------------------------------------------------------------
    val source = mutableListOf("KOTLIN", "SPRING")
    val aliasView: List<String> = source
    val copiedView: List<String> = source.toList()
    source.add("POSTGRES")
    // Hỏi: `aliasView.size` và `copiedView.size` lần lượt bằng bao nhiêu?
    println("  [Bai 1 - Du doan] aliasView.size=${aliasView.size}, copiedView.size=${copiedView.size}")
    check(aliasView.size == 3)
    check(copiedView.size == 2)

    // ------------------------------------------------------------------------
    // Bài 2 (Tìm lỗi): Lộ tham chiếu MutableMap qua Read-Only interface trong Singleton
    // ------------------------------------------------------------------------
    /*
    object AppRateLimiterCache {
        private val _limits = mutableMapOf("FREE" to 60, "PRO" to 600)
        // LỖI THIẾT KẾ:
        // 1. Trả thẳng `_limits` khiến caller có thể ép kiểu `(AppRateLimiterCache.limits as MutableMap)["FREE"] = 999999`!
        // 2. Trong môi trường đa luồng (Web Server), một luồng đang duyệt `limits` trong khi luồng khác
        //    gọi hàm cập nhật `_limits` sẽ gây ra `ConcurrentModificationException`!
        val limits: Map<String, Int> get() = _limits
    }
    */
    println("  [Bai 2 - Tim loi] Khong lo truc tiep `_limits` (MutableMap) trong Singleton da luong.")

    // ------------------------------------------------------------------------
    // Bài 3 (Tự viết nhỏ):
    // ------------------------------------------------------------------------
    // [ĐỀ BÀI]:
    // Viết class `CatalogSnapshotHolder` lưu danh sách các mặt hàng `MutablePriceItem`.
    // Hãy cài đặt hàm `exportSafeSnapshot(): List<MutablePriceItem>` sao cho khi bên ngoài
    // nhận danh sách trả về và sửa `.price` của bất kỳ phần tử nào, giá gốc bên trong
    // `CatalogSnapshotHolder` vẫn KHÔNG bị thay đổi.

    // [LỜI GIẢI THAM KHẢO]:
    class CatalogSnapshotHolder(initialItems: List<MutablePriceItem>) {
        // Sao chép sâu ngay từ lúc nhận vào (Defensive Deep Copy in Constructor)
        private val internalItems: List<MutablePriceItem> = initialItems.map { it.copyItem() }

        // Sao chép sâu khi xuất dữ liệu ra ngoài (Defensive Deep Copy on Read)
        fun exportSafeSnapshot(): List<MutablePriceItem> = internalItems.map { it.copyItem() }

        fun findInternalPrice(sku: String): Long? = internalItems.find { it.sku == sku }?.price
    }

    val holder = CatalogSnapshotHolder(listOf(MutablePriceItem("SKU-PHONE", 15_000_000L)))
    val externalList = holder.exportSafeSnapshot()
    externalList[0].price = 1L // Bên ngoài cố tình sửa giá của phần tử trong danh sách nhận được

    val internalPrice = holder.findInternalPrice("SKU-PHONE")
    println("  [Bai 3 - Tu viet] externalList[0].price=${externalList[0].price} | internalPrice=$internalPrice")
    check(internalPrice == 15_000_000L)

    println("\n--- ___ * FINISH: Ex06ImmutabilityAliasingAndSharedState * ___ ---")
}
