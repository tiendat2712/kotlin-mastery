package view

/**
 * ============================================================================
 * BÀI 03: LISTS POWER TOOLS - SAFE ACCESS, SLICING, CHUNKING & DESTRUCTURING
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ----------------------------------------------------------------------------
 * Trong Java (`lesson11-colection`), khi thao tác với `List`, bạn rất dễ gặp:
 *   - `IndexOutOfBoundsException` khi gọi `list.get(5)` hoặc `list.get(0)` trên list rỗng.
 *   - Phải viết nhiều vòng lặp thủ công để chia nhỏ danh sách thành từng lô (batch).
 *
 * Bộ thư viện chuẩn của Kotlin (`kotlin.collections`) trang bị sẵn hàng loạt Extension
 * Functions cực mạnh trên `List<T>`:
 *   - **Khởi tạo thông minh**:
 *     + `listOfNotNull("A", null, "B")`: Tự động lọc bỏ mọi phần tử `null` ngay lúc tạo!
 *     + `buildList { add("A"); if (cond) add("B") }`: Bên trong block là `MutableList` để
 *       bạn thoải mái thêm phần tử theo điều kiện, nhưng kết quả trả ra ngoài lại là
 *       một Read-Only `List` hoàn chỉnh!
 *   - **Truy cập an toàn (Safe Element Access - Tạm biệt `IndexOutOfBoundsException`)**:
 *     + `list.getOrNull(99)` -> trả về `null` thay vì nổ Exception.
 *     + `list.getOrElse(99) { "DEFAULT" }` -> trả về giá trị mặc định nếu vượt quá index.
 *     + `list.firstOrNull()`, `list.lastOrNull()`.
 *   - **Cắt lát & Chia lô (`take`, `drop`, `chunked`, `windowed`)**:
 *     + `list.chunked(3)`: Chia danh sách lớn thành các danh sách con tối đa 3 phần tử.
 *     + `list.windowed(size = 3, step = 1)`: Tạo cửa sổ trượt (Sliding Window) để tính
 *       trung bình động (Moving Average).
 *   - **Destructuring trên List**:
 *     + `val (first, second, third) = list` (gọi ngầm `component1()` .. `component5()`).
 *
 * 2. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * Hàm **`.chunked(batchSize)`** là "cứu tinh" trong các dự án Spring Boot thực tế!
 * Ví dụ: Khi bạn cần truy vấn SQL `WHERE id IN (:ids)` với 10,000 IDs (Oracle giới hạn tối đa
 * 1,000 phần tử trong mệnh đề `IN`, hoặc gọi Batch API bên thứ 3 giới hạn 500 items/request):
 *   ```kotlin
 *   userIds.chunked(500).forEach { batchIds ->
 *       userRepository.findAllByIdIn(batchIds)
 *   }
 *   ```
 * Chỉ 1 hàm `.chunked(500)` thay thế cho 15 dòng tính toán `Math.min(i + batchSize, size)`
 * dễ sai sót với `subList` trong Java!
 */

fun main() {
    println("--- ___ * START: Ex03ListsAndDestructuring * ___ ---")

    // ========================================================================
    // 1. Khởi tạo với `listOfNotNull` và Builder DSL `buildList { ... }`
    // ========================================================================
    println("\n[1] Khoi tao danh sach voi `listOfNotNull` & `buildList`:")
    val optionalPromoCode: String? = null
    val activeTags = listOfNotNull("FLASH_SALE", optionalPromoCode, "FREE_SHIP")
    println("  listOfNotNull : $activeTags (da tu dong bo qua bien null)")

    val isVipCustomer = true
    val menuItems: List<String> = buildList {
        add("Trang chu")
        add("San pham")
        if (isVipCustomer) {
            add("Phong cho VIP")
            add("Uu dai doc quyen")
        }
        add("Dang xuat")
    }
    println("  buildList     : $menuItems")

    // ========================================================================
    // 2. Truy cập phần tử an toàn (`getOrNull`, `getOrElse`, `firstOrNull`)
    // ========================================================================
    println("\n[2] Truy cap an toan khong so IndexOutOfBoundsException:")
    val servers = listOf("srv-hn-01", "srv-hcm-01", "srv-dn-01")
    println("  servers[0]               : ${servers[0]}")
    println("  servers.getOrNull(99)    : ${servers.getOrNull(99)}")
    println("  servers.getOrElse(99)    : ${servers.getOrElse(99) { index -> "srv-backup-for-idx-$index" }}")
    println("  emptyList.firstOrNull()  : ${emptyList<String>().firstOrNull()}")

    // ========================================================================
    // 3. Toán tử `+` / `-`, Slicing (`take`, `drop`), `chunked` & `windowed`
    // ========================================================================
    println("\n[3] Cat lat (`take`, `drop`), Chia lo (`chunked`) & Cua so truot (`windowed`):")
    val orderIds = (1..8).map { "ORD-$it" }
    println("  Danh sach goc (8 don)    : $orderIds")
    println("  take(3) (3 don dau)      : ${orderIds.take(3)}")
    println("  drop(5) (bo 5 don dau)   : ${orderIds.drop(5)}")

    // Chia lô mỗi batch 3 đơn hàng để xử lý Batch Insert / Batch API
    val batches: List<List<String>> = orderIds.chunked(3)
    println("  chunked(3) (Batching)    : $batches")

    // Cửa sổ trượt kích thước 3 để tính trung bình trượt (Moving Average)
    val dailyRevenues = listOf(100.0, 120.0, 140.0, 110.0, 160.0)
    val movingAverages = dailyRevenues.windowed(size = 3, step = 1) { window ->
        window.average()
    }
    println("  windowed(3) Moving Avg   : $movingAverages")

    // ========================================================================
    // 4. Destructuring trên List
    // ========================================================================
    println("\n[4] Destructuring lay nhanh cac phan tu dau tien cua List:")
    val (goldMedal, silverMedal, bronzeMedal) = listOf("Team Kotlin", "Team Java", "Team Go", "Team Rust")
    println("  Huy chuong: Vang='$goldMedal', Bac='$silverMedal', Dong='$bronzeMedal'")

    println("\n--- ___ * FINISH: Ex03ListsAndDestructuring * ___ ---")
}
