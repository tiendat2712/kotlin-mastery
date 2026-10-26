package view

import bean.ProductItem
import bean.SampleRepository

/**
 * ============================================================================
 * BÀI 02: GROUPING, ASSOCIATING & PARTITIONING (BIẾN `List` THÀNH `Map` SIÊU TỐC)
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
 * (và mặc định nếu trùng key, phần tử sau sẽ ghi đè phần tử trước một cách an toàn!):
 *   1. `associateBy { it.id }`       -> `Map<Key, ProductItem>` (Key do bạn chọn, Value là chính phần tử đó).
 *   2. `associateWith { ... }`       -> `Map<ProductItem, Value>` (Key là chính phần tử đó, Value do bạn tính).
 *   3. `associate { it.id to it.name }` -> `Map<Key, Value>` tùy ý (nhưng tạo đối tượng `Pair` tạm thời).
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
 * 3. SPRING KOTLIN CORNER (GIẢI QUYẾT TRIỆT ĐỂ LỖI N+1 QUERY TRONG MICROSERVICES!):
 * ----------------------------------------------------------------------------
 * Đây là pattern được dùng **HÀNG NGÀY** trong Spring Boot + Kotlin:
 * Giả sử bạn có danh sách 100 `Order` (mỗi `Order` có `productId`). Thay vì chạy vòng lặp
 * 100 lần gọi `productRepo.findById(order.productId)` (gây ra 100 câu lệnh SQL - lỗi N+1 Query!):
 *   ```kotlin
 *   val productIds = orders.map { it.productId }.distinct()
 *   // Chỉ 1 câu SQL `WHERE id IN (...)` + `.associateBy { it.id }` tạo bảng băm O(1) trên RAM:
 *   val productMap: Map<String, ProductItem> = productRepo.findAllByIdIn(productIds).associateBy { it.id }
 *   // Ghép dữ liệu với tốc độ O(1):
 *   val enrichedOrders = orders.map { order -> order to productMap[order.productId] }
 *   ```
 */

fun main() {
    println("--- ___ * START: Ex02GroupingAndAssociating * ___ ---")

    val products = SampleRepository.getProducts()

    // ========================================================================
    // 1. `associateBy`: Biến List thành Lookup Map O(1) (Vũ khí diệt N+1 Query!)
    // ========================================================================
    println("\n[1] `associateBy { it.id }` (Tạo bảng tra cứu O(1) theo ID):")
    val productById: Map<String, ProductItem> = products.associateBy { it.id }
    println("  Tra cứu nhanh mã 'P04': ${productById["P04"]?.name} - Giá: $${productById["P04"]?.price}")

    // Biến thể 2 tham số: `associateBy(keySelector, valueTransform)` (Không tốn bộ nhớ tạo Pair!)
    val priceById: Map<String, java.math.BigDecimal> = products.associateBy(
        keySelector = { it.id },
        valueTransform = { it.price }
    )
    println("  Bảng giá theo ID      : $priceById")

    // ========================================================================
    // 2. `groupBy` & `groupingBy { ... }.eachCount()`
    // ========================================================================
    println("\n[2] `groupBy` (Gom nhóm sản phẩm theo Category) & `groupingBy.eachCount()`:")
    val productsByCategory: Map<String, List<String>> = products.groupBy(
        keySelector = { it.category },
        valueTransform = { it.name }
    )
    for ((category, names) in productsByCategory) {
        println("  - Danh mục [$category]: $names")
    }

    val countByCategory: Map<String, Int> = products
        .groupingBy { it.category }
        .eachCount()
    println("  Số lượng mỗi danh mục (groupingBy.eachCount): $countByCategory")

    // ========================================================================
    // 3. `partition`: Tách đôi danh sách theo điều kiện trong 1 lần duyệt
    // ========================================================================
    println("\n[3] `partition` + Destructuring (Tách sản phẩm Còn hàng vs Hết hàng):")
    val (availableProducts, outOfStockProducts) = products.partition { it.stock > 0 }

    println("  Còn hàng (${availableProducts.size} SP) : ${availableProducts.map { it.name }}")
    println("  Hết hàng (${outOfStockProducts.size} SP) : ${outOfStockProducts.map { it.name }}")

    println("\n--- ___ * FINISH: Ex02GroupingAndAssociating * ___ ---")
}
