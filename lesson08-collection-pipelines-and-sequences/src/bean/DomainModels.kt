package bean

import java.math.BigDecimal

/**
 * ============================================================================
 * BẢN CHẤT (UNDER THE HOOD / DOMAIN MODELS CHO MODULE 08):
 * ============================================================================
 * Trong `D:\Java_Journey\lesson11-colection\src\bean\`, bạn phải tạo từng file
 * `Trader.java`, `Transaction.java` với hàng chục dòng getter/setter/equals/hashCode.
 *
 * Trong Kotlin:
 *   1. Nhiều `data class` có liên quan mật thiết về mặt nghiệp vụ có thể đặt chung
 *      trong một file `DomainModels.kt` giúp dễ tra cứu và quản lý.
 *   2. Nhờ `data class`, các thao tác `.distinct()`, `.toSet()`, hay làm key trong `Map`
 *      trên `Trader`, `Transaction`, `ProductItem` hoạt động chính xác ngay lập tức vì
 *      `equals()` và `hashCode()` đã được trình biên dịch tự động sinh ra!
 *   3. Đối với tiền tệ (`price`), ta sử dụng `BigDecimal` chuẩn mực như trong các dự án
 *      Fintech / E-Commerce với Spring Boot.
 */

data class Trader(
    val name: String,
    val city: String
)

data class Transaction(
    val trader: Trader,
    val year: Int,
    val value: Int
)

data class ProductItem(
    val id: String,
    val name: String,
    val category: String,
    val price: BigDecimal,
    val stock: Int,
    val tags: List<String> = emptyList()
)

object SampleRepository {

    /**
     * Dữ liệu giao dịch chuẩn từ bài tập kinh điển `Ex01TransactionApp.java`
     * (kèm các Trader tại Cambridge và Milan để kiểm thử trọn vẹn 10 câu hỏi).
     */
    fun getTransactions(): List<Transaction> {
        val raoul = Trader("Raoul", "Cambridge")
        val mario = Trader("Mario", "Milan")
        val alan = Trader("Alan", "Cambridge")
        val brian = Trader("Brian", "Cambridge")

        return listOf(
            Transaction(brian, 2011, 300),
            Transaction(raoul, 2012, 1000),
            Transaction(raoul, 2011, 400),
            Transaction(mario, 2012, 710),
            Transaction(mario, 2012, 700),
            Transaction(alan, 2012, 950)
        )
    }

    /**
     * Dữ liệu sản phẩm mẫu phục vụ các bài học Pipeline, Grouping, Associating & Aggregation.
     */
    fun getProducts(): List<ProductItem> = listOf(
        ProductItem(
            id = "P01",
            name = "MacBook Pro 16 M4",
            category = "LAPTOP",
            price = BigDecimal("3499.00"),
            stock = 12,
            tags = listOf("apple", "pro", "dev")
        ),
        ProductItem(
            id = "P02",
            name = "ThinkPad X1 Carbon",
            category = "LAPTOP",
            price = BigDecimal("1899.00"),
            stock = 5,
            tags = listOf("lenovo", "ultrabook", "dev")
        ),
        ProductItem(
            id = "P03",
            name = "Keychron Q1 Pro",
            category = "ACCESSORY",
            price = BigDecimal("199.00"),
            stock = 0,
            tags = listOf("keyboard", "mechanical")
        ),
        ProductItem(
            id = "P04",
            name = "Logitech MX Master 3S",
            category = "ACCESSORY",
            price = BigDecimal("99.00"),
            stock = 40,
            tags = listOf("mouse", "ergonomic", "dev")
        ),
        ProductItem(
            id = "P05",
            name = "LG UltraFine 5K Monitor",
            category = "MONITOR",
            price = BigDecimal("1299.00"),
            stock = 0,
            tags = listOf("display", "5k")
        ),
        ProductItem(
            id = "P06",
            name = "Dell UltraSharp 27 4K",
            category = "MONITOR",
            price = BigDecimal("649.00"),
            stock = 18,
            tags = listOf("display", "4k", "usb-c")
        )
    )
}
