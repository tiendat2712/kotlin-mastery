package view

/**
 * ============================================================================
 * BÀI 03: PRIMARY CONSTRUCTOR, `init` BLOCKS & SECONDARY CONSTRUCTORS
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ----------------------------------------------------------------------------
 * Trong Kotlin, Constructor được chia làm 2 loại rõ rệt:
 *
 * A. Primary Constructor (Hàm khởi tạo chính - nằm ngay trên phần header của class):
 *    `class BankAccount(val accountNumber: String, initialBalance: Double)`
 *    Hãy chú ý sự khác biệt cực lớn giữa 2 tham số trên:
 *    - `val accountNumber: String` (có từ khóa `val` hoặc `var`):
 *      Vừa là tham số truyền vào constructor (trên STACK), vừa yêu cầu Kotlin tạo luôn
 *      một **Property** (có backing field trên HEAP + hàm `getAccountNumber()`).
 *    - `initialBalance: Double` (KHÔNG có `val`/`var` - Plain constructor parameter):
 *      Chỉ là một biến cục bộ trên STACK trong lúc chạy constructor! Sau khi đối tượng
 *      khởi tạo xong, `initialBalance` biến mất, không tốn thêm ô nhớ thừa trên HEAP
 *      và chỉ có thể được sử dụng bên trong các khối `init { ... }` hoặc khi gán khởi tạo
 *      cho các thuộc tính khác trong thân class.
 *
 * B. Thứ tự thực thi của `init { ... }` và Property Initializers:
 *    Vì Primary Constructor nằm trên tiêu đề class nên không chứa được các câu lệnh
 *    logic phức tạp. Kotlin cung cấp khối `init { ... }`.
 *    Ở tầng Bytecode, toàn bộ các dòng khởi tạo thuộc tính trong thân class VÀ các khối
 *    `init { ... }` được **gộp chung vào bên trong thân hàm khởi tạo chính `<init>`**
 *    và chạy **TUẦN TỰ TỪ TRÊN XUỐNG DƯỚI** theo đúng thứ tự xuất hiện trong file code!
 *
 * C. Secondary Constructor (`constructor(...) : this(...)`):
 *    Mọi Secondary Constructor bắt buộc phải ủy quyền (delegate) về Primary Constructor
 *    thông qua `: this(...)`.
 *    Thứ tự chạy:
 *      `Primary Constructor + Property Initializers + init blocks` CHẠY TRƯỚC ->
 *      rồi mới chạy thân của `Secondary Constructor`!
 *
 * 2. SO SÁNH JAVA vs KOTLIN:
 * ----------------------------------------------------------------------------
 * - Trong Java (`lesson03-oop`): Bạn thường viết 3-4 constructors nạp chồng (Constructor
 *   Overloading - Telescoping Constructors).
 * - Trong Kotlin: 95% trường hợp bạn KHÔNG CẦN viết Secondary Constructor! Chỉ cần dùng
 *   **Default Parameter Values** ngay trong Primary Constructor là đã thay thế được
 *   tất cả các constructor nạp chồng của Java.
 *
 * 3. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * Trong Spring Boot + Kotlin:
 *   - **Constructor Injection** được viết thẳng vào Primary Constructor mà không cần
 *     `@Autowired`:
 *     ```kotlin
 *     @Service
 *     class TransferService(
 *         private val accountRepo: AccountRepository,
 *         private val auditLogger: AuditLogger
 *     )
 *     ```
 *   - Khối `init { ... }` thường dùng để validate cấu hình ngay khi Spring khởi tạo Bean
 *     (thay thế gọn nhẹ cho `@PostConstruct` trong những trường hợp không phụ thuộc vào
 *     AOP proxy).
 */

private class BankAccount(
    val accountNumber: String,      // Có `val` -> Tạo property + backing field trên HEAP
    val ownerName: String,          // Có `val` -> Tạo property + backing field trên HEAP
    initialBalance: Double = 0.0    // KHÔNG có `val/var` -> Chỉ là tham số tạm trong lúc khởi tạo
) {
    // Bước 1: Khối init thứ nhất - kiểm tra tính hợp lệ của tham số truyền vào
    init {
        println("    [Step 1 - init #1] Kiểm tra tài khoản '$accountNumber' của '$ownerName'")
        require(accountNumber.isNotBlank()) { "Số tài khoản không được rỗng!" }
        require(initialBalance >= 0.0) { "Số dư ban đầu không được âm: $initialBalance" }
    }

    // Bước 2: Khởi tạo thuộc tính trong thân class (sử dụng tham số `initialBalance`)
    var balance: Double = initialBalance
        private set

    val tier: String = if (initialBalance >= 10_000.0) "VIP_GOLD" else "STANDARD"

    // Bước 3: Khối init thứ hai - chạy sau khi `balance` và `tier` đã được khởi tạo ở trên
    init {
        println("    [Step 2 - init #2] Khởi tạo hoàn tất trên HEAP -> balance=$balance, tier=$tier")
    }

    // Bước 4 (Tùy chọn): Secondary Constructor ủy quyền về Primary Constructor qua `: this(...)`
    constructor(csvRow: String) : this(
        accountNumber = csvRow.split(",")[0].trim(),
        ownerName = csvRow.split(",")[1].trim(),
        initialBalance = csvRow.split(",")[2].trim().toDouble()
    ) {
        // Thân của Secondary Constructor LUÔN chạy SAU CÙNG (sau tất cả các khối init)
        println("    [Step 3 - Secondary Constructor] Đã parse thành công từ chuỗi CSV: '$csvRow'")
    }

    override fun toString(): String {
        return "BankAccount(accountNumber='$accountNumber', ownerName='$ownerName', balance=$balance, tier='$tier')"
    }
}

fun main() {
    println("--- ___ * START: Ex03ConstructorsAndInitBlocks * ___ ---")

    // ========================================================================
    // 1. Gọi Primary Constructor trực tiếp
    // ========================================================================
    println("\n[1] Khởi tạo bằng Primary Constructor:")
    val acc1 = BankAccount("ACC-001", "Le Minh", 15_000.0)
    println("  Kết quả acc1 -> $acc1")

    // ========================================================================
    // 2. Gọi Primary Constructor với giá trị mặc định (thay thế Constructor Overloading)
    // ========================================================================
    println("\n[2] Khởi tạo với Default Argument (`initialBalance` mặc định = 0.0):")
    val acc2 = BankAccount(accountNumber = "ACC-002", ownerName = "Tran Hoa")
    println("  Kết quả acc2 -> $acc2")

    // ========================================================================
    // 3. Gọi Secondary Constructor và quan sát thứ tự thực thi
    // ========================================================================
    println("\n[3] Khởi tạo bằng Secondary Constructor (từ dòng CSV):")
    val acc3 = BankAccount("ACC-003, Pham Tuan, 5200.5")
    println("  Kết quả acc3 -> $acc3")

    println("\n--- ___ * FINISH: Ex03ConstructorsAndInitBlocks * ___ ---")
}
