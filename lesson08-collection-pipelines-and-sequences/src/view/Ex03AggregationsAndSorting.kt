package view

import bean.ProductItem
import bean.SampleRepository
import java.math.BigDecimal

/**
 * ============================================================================
 * BÀI 03: AGGREGATIONS (`sumOf`, `maxByOrNull`, `fold`) & MULTI-CRITERIA SORTING
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ----------------------------------------------------------------------------
 * A. Tính tổng Tiền tệ (`BigDecimal`) trong Java vs Kotlin:
 *    - Trong Java (`lesson12-stream-api`), cộng tổng `BigDecimal` bằng Stream là một cực hình:
 *      `products.stream().map(ProductItem::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add)`
 *    - Trong Kotlin, hàm **`sumOf { ... }`** được nạp chồng (overloaded) cho cả `Int`, `Long`,
 *      `Double` VÀ **`BigDecimal`**!
 *      Bạn chỉ cần viết: `products.sumOf { it.price * it.stock.toBigDecimal() }`!
 *
 * B. Tìm Max/Min an toàn (`maxByOrNull`, `minByOrNull`, `maxOfOrNull`):
 *    - `maxByOrNull { it.price }` trả về **phần tử `ProductItem?`** có giá cao nhất (hoặc `null`
 *      nếu danh sách rỗng, không bao giờ nổ `NoSuchElementException`).
 *    - `maxOfOrNull { it.price }` trả về **giá trị `BigDecimal?`** cao nhất.
 *
 * C. `fold` vs `reduceOrNull`:
 *    - `reduceOrNull { acc, elem -> ... }`: Lấy phần tử đầu tiên làm giá trị khởi tạo `acc`.
 *    - `fold(initialValue) { acc, elem -> ... }`: Cho phép truyền giá trị khởi tạo `initialValue`
 *      có kiểu dữ liệu bất kỳ (an toàn cả khi danh sách rỗng).
 *
 * D. Sắp xếp đa tiêu chí (`sortedBy`, `sortedWith(compareBy ... thenByDescending ...)`):
 *    - Khác với `list.sort(...)` của Java (làm biến dạng list gốc), các hàm `sortedBy`,
 *      `sortedWith` của Kotlin trả về một **danh sách mới đã sắp xếp**, giữ nguyên list gốc!
 *
 * 2. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * Trong các hệ thống E-Commerce / ERP viết bằng Spring Boot + Kotlin:
 *   - `sumOf { it.unitPrice * it.quantity.toBigDecimal() }` là chuẩn vàng để tính tổng tiền
 *     đơn hàng (`OrderTotal`), tránh sai số dấu phẩy động của `Double` và ngắn gọn gấp 3 lần Java.
 *   - `compareBy<ProductItem> { ... }.thenByDescending { ... }` kết hợp `nullsLast()` giúp
 *     xây dựng bộ sắp xếp động trên RAM cực kỳ trực quan.
 */

fun main() {
    println("--- ___ * START: Ex03AggregationsAndSorting * ___ ---")

    val products = SampleRepository.getProducts()

    // ========================================================================
    // 1. Quantifiers: `any`, `all`, `none`, `count { ... }`
    // ========================================================================
    println("\n[1] Kiem tra dieu kien luong tu (`any`, `all`, `none`, `count`):")
    val hasOutOfStock = products.any { it.stock == 0 }
    val allPositivePrice = products.all { it.price > BigDecimal.ZERO }
    val noneNegativeStock = products.none { it.stock < 0 }
    val laptopCount = products.count { it.category == "LAPTOP" }

    println("  Co san pham nao het hang khong (`any`)?      : $hasOutOfStock")
    println("  Tat ca san pham deu co gia > 0 (`all`)?      : $allPositivePrice")
    println("  Khong co san pham nao am kho (`none`)?       : $noneNegativeStock")
    println("  So luong san pham thuoc nhom LAPTOP (`count`): $laptopCount")

    // ========================================================================
    // 2. Aggregations: `sumOf` (BigDecimal!), `maxByOrNull`, `minByOrNull`, `fold`
    // ========================================================================
    println("\n[2] Tinh tong gia tri kho hang voi `sumOf` (ho tro truc tiep BigDecimal!):")
    val totalInventoryValue: BigDecimal = products.sumOf { product ->
        product.price.multiply(product.stock.toBigDecimal())
    }
    val mostExpensiveProduct: ProductItem? = products.maxByOrNull { it.price }
    val cheapestProduct: ProductItem? = products.minByOrNull { it.price }

    println("  Tong gia tri ton kho (BigDecimal) : \$$totalInventoryValue")
    println("  San pham dat nhat (`maxByOrNull`) : ${mostExpensiveProduct?.name} (\$${mostExpensiveProduct?.price})")
    println("  San pham re nhat (`minByOrNull`)  : ${cheapestProduct?.name} (\$${cheapestProduct?.price})")

    // Sử dụng `fold` để tính tổng tiền sau khi áp dụng phí bảo hiểm kho cố định 50 USD ban đầu
    val totalWithBaseInsurance = products.fold(BigDecimal("50.00")) { acc, item ->
        acc + item.price
    }
    println("  Tong don gia + \$50 phi goc (`fold`): \$$totalWithBaseInsurance")

    // ========================================================================
    // 3. Multi-criteria Sorting: `sortedWith(compareBy ... thenByDescending ...)`
    // ========================================================================
    println("\n[3] Sap xep da tieu chi (Category ASC -> Price DESC -> ID ASC):")
    val multiSorted = products.sortedWith(
        compareBy<ProductItem> { it.category }
            .thenByDescending { it.price }
            .thenBy { it.id }
    )
    multiSorted.forEach { p ->
        println("  - [${p.category.padEnd(9)}] ${p.name.padEnd(24)} | Price: \$${p.price} | Stock: ${p.stock}")
    }

    println("\n--- ___ * FINISH: Ex03AggregationsAndSorting * ___ ---")
}
