package view

import bean.SampleRepository
import java.math.BigDecimal

/**
 * ============================================================================
 * BÀI 01: COLLECTION PIPELINES - `filter`, `map`, `flatMap`, `distinctBy`
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ----------------------------------------------------------------------------
 * Trong Java 8+ (`lesson12-stream-api`), để lọc và biến đổi một `List`, bạn luôn phải
 * đi qua 3 bước rườm rà:
 *   `list.stream() ... .collect(Collectors.toList())`
 *
 * Trong Kotlin:
 *   - Các hàm `filter`, `map`, `mapNotNull`, `flatMap`, `distinct`, `distinctBy` là các
 *     **Extension Functions gắn trực tiếp trên `Iterable<T>` / `Collection<T>`**!
 *   - Bạn gọi thẳng `products.filter { ... }.map { ... }` mà KHÔNG CẦN `.stream()` và
 *     KHÔNG CẦN `.collect(Collectors.toList())`! Kết quả trả về mặc định đã là một
 *     Read-Only `List<R>` mới tinh trên HEAP.
 *   - Đáng chú ý hơn nữa: Hầu hết các hàm pipeline của Kotlin được đánh dấu từ khóa
 *     **`inline`** ở thư viện chuẩn! Nghĩa là ở tầng Bytecode, trình biên dịch Kotlin
 *     chép thẳng nội dung Lambda vào một vòng lặp `for` truyền thống, giúp **không tốn
 *     chi phí khởi tạo đối tượng Lambda (Anonymous Class allocation)** trên HEAP!
 *
 * 2. CÁC HÀM ĐẶC SẢN CỦA KOTLIN MÀ JAVA STREAM THIẾU VẮNG:
 * ----------------------------------------------------------------------------
 * - `filterNot { ... }`          : Ngược với `filter`, đọc xuôi mắt hơn `!condition`.
 * - `filterIndexed { idx, e -> }`: Lọc có kèm chỉ số `index`.
 * - `filterIsInstance<T>()`      : Lọc và ép kiểu an toàn các phần tử thuộc kiểu `T`!
 * - `mapNotNull { ... }`         : Vừa `map` vừa tự động loại bỏ các kết quả `null` chỉ
 *                                  trong 1 lần duyệt duy nhất!
 * - `distinctBy { it.category }` : Loại bỏ trùng lặp theo một thuộc tính bất kỳ mà KHÔNG
 *                                  cần phải override lại `equals()`/`hashCode()` của class!
 *
 * 3. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * Trong tầng `@Service` và `@RestController` của Spring Boot:
 *   - `entities.map { it.toDto() }` thay thế hoàn toàn cho `ModelMapper` / `MapStruct`
 *     phức tạp.
 *   - `mapNotNull` cực kỳ hữu dụng khi parse hoặc làm giàu dữ liệu (data enrichment)
 *     mà bạn muốn bỏ qua các bản ghi lỗi/thiếu thông tin trong 1 bước duy nhất.
 */

fun main() {
    println("--- ___ * START: Ex01FilterMapFlatMap * ___ ---")

    val products = SampleRepository.getProducts()

    // ========================================================================
    // 1. `filter` + `map` trực tiếp (Không cần .stream() và .collect())
    // ========================================================================
    println("\n[1] Tìm tên các sản phẩm còn hàng (stock > 0) và giá >= 500 USD:")
    val premiumInStockNames: List<String> = products
        .filter { it.stock > 0 && it.price >= BigDecimal("500.00") }
        .map { "${it.name} ($${it.price})" }

    println("  Kết quả: $premiumInStockNames")

    // ========================================================================
    // 2. `mapNotNull`: Vừa biến đổi vừa lọc bỏ `null` trong 1 vòng lặp duy nhất
    // ========================================================================
    println("\n[2] `mapNotNull` (Trích xuất cảnh báo nhập kho cho các mặt hàng sắp hết):")
    val restockAlerts: List<String> = products.mapNotNull { product ->
        if (product.stock <= 5) {
            "CẢNH BÁO: '${product.name}' chỉ còn ${product.stock} chiếc!"
        } else {
            null // Sẽ bị mapNotNull tự động loại bỏ khỏi danh sách kết quả
        }
    }
    restockAlerts.forEach { println("  - $it") }

    // ========================================================================
    // 3. `flatMap` + `distinct`: Làm phẳng danh sách lồng nhau (List of Lists)
    // ========================================================================
    println("\n[3] `flatMap` + `distinct` (Lấy toàn bộ các tag duy nhất từ mọi sản phẩm):")
    val allUniqueTags: List<String> = products
        .flatMap { it.tags }
        .distinct()
    println("  Tất cả Tags duy nhất: $allUniqueTags")

    // ========================================================================
    // 4. `distinctBy` & `filterIsInstance<T>()`
    // ========================================================================
    println("\n[4] `distinctBy` (Mỗi danh mục chỉ lấy sản phẩm đại diện đầu tiên):")
    val onePerCategory = products.distinctBy { it.category }
    onePerCategory.forEach { println("  - [${it.category}] ${it.name}") }

    println("\n[5] `filterIsInstance<T>()` (Lọc phần tử theo kiểu dữ liệu trong danh sách hỗn hợp):")
    val mixedEvents: List<Any> = listOf("USER_LOGIN", 404, "ORDER_PAID", 500.25, "USER_LOGOUT")
    val stringEventsOnly: List<String> = mixedEvents.filterIsInstance<String>()
    println("  Chỉ lấy các sự kiện kiểu String: $stringEventsOnly")

    println("\n--- ___ * FINISH: Ex01FilterMapFlatMap * ___ ---")
}
