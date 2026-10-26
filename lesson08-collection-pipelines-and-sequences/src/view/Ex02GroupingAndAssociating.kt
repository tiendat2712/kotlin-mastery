package view

import bean.ProductItem
import bean.SampleRepository

/**
 * ============================================================================
 * BÀI 02: GROUPING, ASSOCIATING & PARTITIONING (BIẾN `List` THÀNH `Map` TRÊN RAM)
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ----------------------------------------------------------------------------
 * Trong Java (`lesson12-stream-api`), để chuyển một `List<ProductItem>` thành `Map`:
 *   ```java
 *   Map<String, ProductItem> map = products.stream()
 *       .collect(Collectors.toMap(ProductItem::getId, Function.identity(), (a, b) -> b));
 *   ```
 * Không những dài dòng, `Collectors.toMap` của Java còn nổi tiếng với cái bẫy nổ
 * `IllegalStateException: Duplicate key` nếu bạn quên truyền hàm merge `(a, b) -> b`!
 *
 * Trong Kotlin, việc biến `List` thành `Map` được chia thành 3 nhóm hàm cực kỳ rõ nghĩa
 * (và mặc định nếu trùng key, phần tử xuất hiện sau sẽ ghi đè phần tử trước):
 *   1. `associateBy { it.id }`       -> `Map<Key, ProductItem>` (Key do bạn chọn, Value là chính phần tử đó).
 *   2. `associateWith { ... }`       -> `Map<ProductItem, Value>` (Key là chính phần tử đó, Value do bạn tính).
 *   3. `associate { it.id to it.name }` -> `Map<Key, Value>` tùy ý (tạo đối tượng `Pair` tạm thời).
 *
 * 2. GOM NHÓM (`groupBy` vs `groupingBy`) & CHIA ĐÔI (`partition`):
 * ----------------------------------------------------------------------------
 * - `groupBy { it.category }` -> Tạo ngay `Map<String, List<ProductItem>>` trên HEAP.
 * - `groupingBy { it.category }.eachCount()` -> Gom nhóm **Lazy** để đếm hoặc cộng dồn
 *   mà KHÔNG cần tạo các `List<ProductItem>` trung gian trên HEAP!
 * - `partition { it.stock > 0 }` -> Duyệt danh sách đúng **1 lần duy nhất**, tách thành
 *   `Pair<List<ProductItem>, List<ProductItem>>` để bạn Destructure ngay thành:
 *   `val (inStock, outOfStock) = products.partition { it.stock > 0 }`!
 *
 * 3. SPRING KOTLIN CORNER (HIỂU ĐÚNG VAI TRÒ CỦA `associateBy` & BÀI TOÁN N+1 QUERY):
 * ----------------------------------------------------------------------------
 * Chúng ta cần phân biệt chính xác 2 bài toán hiệu năng:
 *   A. **Tối ưu tra cứu trong bộ nhớ (In-Memory Lookup Optimization)**:
 *      Bản thân `associateBy` là một thao tác **trên RAM (in-memory)** giúp dựng bảng băm
 *      `Map<K, V>`. Nếu bạn ghép 2 danh sách đã nằm trên RAM bằng vòng lặp lồng nhau
 *      `orders.map { o -> products.find { p -> p.id == o.productId } }`, độ phức tạp là
 *      $O(N \times M)$. Chuyển `products` thành `Map` bằng `associateBy { it.id }` giúp
 *      giảm độ phức tạp ghép dữ liệu trên RAM xuống còn $O(N + M)$ (tra cứu mỗi phần tử $O(1)$).
 *
 *   B. **Kết hợp với Batch Query để tránh N+1 Query (Application-level Batching)**:
 *      - Ở tầng **ORM / JPA (trong cùng 1 Database)**, lỗi N+1 Query được giải quyết tận gốc
 *        bằng SQL `JOIN FETCH`, `@EntityGraph`, cấu hình `@BatchSize`, hoặc DTO Projection.
 *      - Tuy nhiên, khi làm việc với **Microservices** (gọi REST/gRPC sang service khác) hoặc
 *        khi chủ động tách 2 câu truy vấn độc lập (Application-level join), `associateBy` phát huy
 *        tác dụng **khi được kết hợp với chiến lược truy vấn theo lô (Batch Query)**:
 *        ```kotlin
 *        val productIds = orders.map { it.productId }.distinct()
 *        // Bước 1 (Batch Query): Chỉ bắn 1 câu SQL `WHERE id IN (...)` hoặc 1 Batch API call
 *        // Bước 2 (In-Memory Indexing): Dùng `.associateBy { it.id }` để lập chỉ mục trên RAM
 *        val productMap: Map<String, ProductItem> =
 *            productRepo.findAllByIdIn(productIds).associateBy { it.id }
 *        // Bước 3 (O(1) Enrich): Ghép dữ liệu trong bộ nhớ với tốc độ O(1)
 *        val enrichedOrders = orders.map { order -> order to productMap[order.productId] }
 *        ```
 */

fun main() {
    println("--- ___ * START: Ex02GroupingAndAssociating * ___ ---")

    val products = SampleRepository.getProducts()

    // ========================================================================
    // 1. `associateBy`: Biến List thành Lookup Map O(1) trên RAM
    // ========================================================================
    println("\n[1] `associateBy { it.id }` (Tao bang tra cuu in-memory O(1) theo ID):")
    val productById: Map<String, ProductItem> = products.associateBy { it.id }
    println("  Tra cuu nhanh ma 'P04': ${productById["P04"]?.name} - Gia: $${productById["P04"]?.price}")
    check(productById["P04"]?.name == "Logitech MX Master 3S")

    // Biến thể 2 tham số: `associateBy(keySelector, valueTransform)` (Không tốn bộ nhớ tạo Pair!)
    val priceById: Map<String, java.math.BigDecimal> = products.associateBy(
        keySelector = { it.id },
        valueTransform = { it.price }
    )
    println("  Bang gia theo ID      : $priceById")
    check(priceById.size == products.size)

    // ========================================================================
    // 2. `groupBy` & `groupingBy { ... }.eachCount()`
    // ========================================================================
    println("\n[2] `groupBy` (Gom nhom san pham theo Category) & `groupingBy.eachCount()`:")
    val productsByCategory: Map<String, List<String>> = products.groupBy(
        keySelector = { it.category },
        valueTransform = { it.name }
    )
    for ((category, names) in productsByCategory) {
        println("  - Danh muc [$category]: $names")
    }

    val countByCategory: Map<String, Int> = products
        .groupingBy { it.category }
        .eachCount()
    println("  So luong moi danh muc (groupingBy.eachCount): $countByCategory")
    check(countByCategory["LAPTOP"] == 2)

    // ========================================================================
    // 3. `partition`: Tách đôi danh sách theo điều kiện trong 1 lần duyệt
    // ========================================================================
    println("\n[3] `partition` + Destructuring (Tach san pham Con hang vs Het hang):")
    val (availableProducts, outOfStockProducts) = products.partition { it.stock > 0 }

    println("  Con hang (${availableProducts.size} SP) : ${availableProducts.map { it.name }}")
    println("  Het hang (${outOfStockProducts.size} SP) : ${outOfStockProducts.map { it.name }}")
    check(availableProducts.size == 4 && outOfStockProducts.size == 2)

    // ========================================================================
    // 4. GÓC TỰ LUYỆN (ACTIVE LEARNING)
    // ========================================================================
    println("\n[4] Goc Tu Luyen (Active Learning):")

    // ------------------------------------------------------------------------
    // Bài 1 (Dự đoán kết quả): Điều gì xảy ra khi `associateBy` gặp Key trùng lặp?
    // ------------------------------------------------------------------------
    // Khác với `Collectors.toMap` trong Java (nổ IllegalStateException nếu trùng key),
    // `associateBy` trong Kotlin sẽ giữ lại phần tử ĐẦU TIÊN hay phần tử CUỐI CÙNG có cùng key?
    val representativeByCategory: Map<String, String> = products.associateBy(
        keySelector = { it.category },
        valueTransform = { it.id }
    )
    // Trong nhóm LAPTOP có P01 (đứng trước) và P02 (đứng sau). Kết quả của key "LAPTOP" là gì?
    println("  [Bai 1 - Du doan] representativeByCategory[\"LAPTOP\"] = ${representativeByCategory["LAPTOP"]} (Phan tu sau ghi de phan tu truoc!)")
    check(representativeByCategory["LAPTOP"] == "P02")

    // ------------------------------------------------------------------------
    // Bài 2 (Tìm lỗi hiệu năng): Dùng `.find { ... }` lồng bên trong `.map { ... }` ($O(N \times M)$)
    // ------------------------------------------------------------------------
    val requestedProductIds = listOf("P06", "P01", "P04")
    /*
    // CÁCH VIẾT KÉM HIỆU QUẢ ($O(N \times M)$):
    // Với mỗi ID trong `requestedProductIds`, `.find` lại phải quét tuần tự từ đầu danh sách `products`.
    val slowResult = requestedProductIds.mapNotNull { reqId ->
        products.find { it.id == reqId }?.name
    }
    */
    // CÁCH TỐI ƯU ($O(N + M)$): Dùng `productById` (Map đã tạo bằng `associateBy` ở mục [1]) để tra cứu O(1):
    val fastResult = requestedProductIds.mapNotNull { reqId -> productById[reqId]?.name }
    println("  [Bai 2 - Toi uu O(N+M)] fastResult = $fastResult")
    check(fastResult == listOf("Dell UltraSharp 27 4K", "MacBook Pro 16 M4", "Logitech MX Master 3S"))

    // ------------------------------------------------------------------------
    // Bài 3 (Tự viết nhỏ):
    // ------------------------------------------------------------------------
    // [ĐỀ BÀI]:
    // Hãy dùng `groupingBy { ... }.fold(0) { acc, elem -> ... }` để tính TỔNG SỐ LƯỢNG TỒN KHO (`stock`)
    // theo từng danh mục (`category`) mà không cần tạo danh sách trung gian như `groupBy`.

    // [LỜI GIẢI THAM KHẢO]:
    val totalStockByCategory: Map<String, Int> = products
        .groupingBy { it.category }
        .fold(0) { accStock, item -> accStock + item.stock }

    println("  [Bai 3 - Tu viet] Tong ton kho theo danh muc = $totalStockByCategory")
    check(totalStockByCategory["LAPTOP"] == 17)
    check(totalStockByCategory["ACCESSORY"] == 40)
    check(totalStockByCategory["MONITOR"] == 18)

    println("\n--- ___ * FINISH: Ex02GroupingAndAssociating * ___ ---")
}
