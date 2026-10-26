package view

import java.math.BigDecimal

/**
 * BÀI 03: UNCHECKED EXCEPTIONS, `try-catch` EXPRESSION & PRECONDITION FUNCTIONS
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE / DESIGN PHILOSOPHY):
 *    - **Vì sao Kotlin KHÔNG CÓ Checked Exceptions?**
 *      + Ở tầng JVM Bytecode, thực chất **KHÔNG HỀ TỒN TẠI khái niệm Checked vs Unchecked Exception**!
 *        Lệnh `ATHROW` trong bytecode ném `IOException` hay `RuntimeException` hoàn toàn giống hệt nhau.
 *        Quy tắc bắt buộc `throws IOException` hoặc `try-catch` hoàn toàn là luật riêng của trình biên dịch `javac`.
 *      + Trong thực tế phát triển phần mềm hiện đại (và như bạn đã thấy ở `D:\Java_Journey\lesson06-exception`
 *        cùng `lesson12-stream`), Checked Exception của Java gây ra 2 vấn đề lớn:
 *        (1) Làm "ô nhiễm" chữ ký hàm và gây cản trở Functional Programming: Bạn không thể gọi một hàm
 *            ném `IOException` bên trong `list.stream().map(x -> readConfig(x))` vì `Function<T, R>` không khai báo `throws`!
 *        (2) Dẫn đến anti-pattern bắt lỗi rỗng (`catch (Exception e) {}`) hoặc bọc lại vào `RuntimeException` ở khắp nơi.
 *      + Vì thế, Kotlin (cũng như C#, Scala, Swift) coi **mọi Exception đều là Unchecked**! Bạn chỉ bắt
 *        những ngoại lệ mà bạn thực sự muốn xử lý.
 *
 *    - **`try - catch` là một Biểu thức (Expression)**:
 *      + Giống như `if` và `when`, `try - catch` trong Kotlin trả về một giá trị (là biểu thức cuối cùng
 *        trong nhánh `try` hoặc nhánh `catch`). Khối `finally` không ảnh hưởng đến giá trị trả về.
 *      + Điều này loại bỏ hoàn toàn kiểu khai báo biến tạm `int port = 8080; try { port = ... } catch ...` của Java!
 *
 *    - **Bộ tứ Preconditions chuẩn mực (`require`, `requireNotNull`, `check`, `error`)**:
 *      + Đây là các `inline fun` có sẵn trong Kotlin Standard Library (`Preconditions.kt`), đi kèm với
 *        **Kotlin Contracts** (`contract { returns() implies (...) }`):
 *        * `require(condition) { "Lazy message" }`: Kiểm tra **tham số đầu vào (Argument)**.
 *          Nếu sai -> ném `IllegalArgumentException`. Lambda tạo thông báo lỗi chỉ được thực thi khi điều kiện SAI
 *          (tránh tốn chi phí nối chuỗi String trên Heap khi điều kiện đúng!).
 *        * `requireNotNull(value) { "Lazy message" }`: Kiểm tra khác null -> nếu null ném `IllegalArgumentException`,
 *          nếu khác null thì **Smart-cast** biến từ `T?` sang `T` ngay lập tức!
 *        * `check(condition) { "Lazy message" }` & `checkNotNull(value)`: Kiểm tra **trạng thái nội tại của đối tượng (State)**.
 *          Nếu sai -> ném `IllegalStateException`.
 *        * `error("Message")`: Ném ngay `IllegalStateException("Message")` và có kiểu trả về là `Nothing`
 *          (dùng rất gọn sau toán tử Elvis `?: error(...)` hoặc nhánh `else -> error(...)`).
 *
 * 2. SO SÁNH JAVA VS KOTLIN:
 *    - Java (`lesson06-exception`):
 *      ```java
 *      if (amount.compareTo(BigDecimal.ZERO) <= 0) {
 *          throw new IllegalArgumentException("Số tiền phải dương: " + amount);
 *      }
 *      ```
 *    - Kotlin:
 *      ```kotlin
 *      require(amount > BigDecimal.ZERO) { "So tien phai duong: $amount" }
 *      ```
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong Spring Boot, `@Transactional` mặc định chỉ tự động rollback khi gặp `RuntimeException` (Unchecked)
 *      mà KHÔNG rollback khi gặp Checked Exception (trừ khi cấu hình `rollbackFor = [Exception::class]`).
 *    - Trong Domain Model (DDD) và Service Layer của Spring Boot Kotlin, `require(...)` và `check(...)`
 *      được dùng ở khắp các `init {}` block của Value Object/DTO và các phương thức chuyển trạng thái Entity,
 *      sau đó được `@RestControllerAdvice` bắt `IllegalArgumentException` (trả HTTP 400 Bad Request)
 *      và `IllegalStateException` (trả HTTP 409 Conflict).
 */

// Minh họa Value Object / Domain Entity sử dụng Preconditions trong `init` và các hàm nghiệp vụ
class BankWallet(
    val walletId: String,
    ownerEmail: String?,
    initialBalance: BigDecimal,
    private var status: String = "ACTIVE" // ACTIVE, FROZEN, CLOSED
) {
    val ownerEmail: String
    var balance: BigDecimal = initialBalance
        private set

    init {
        // 1. require: Kiểm tra tính hợp lệ của tham số truyền vào constructor
        require(walletId.isNotBlank()) { "Ma vi (walletId) khong duoc de trong!" }
        require(initialBalance >= BigDecimal.ZERO) { "So du ban dau khong duoc am: $initialBalance" }

        // 2. requireNotNull: Vừa kiểm tra khác null, vừa smart-cast từ `String?` thành `String`!
        val validEmail = requireNotNull(ownerEmail?.trim()?.takeIf { it.isNotEmpty() }) {
            "Email chu vi [$walletId] khong duoc null hoac rong!"
        }
        this.ownerEmail = validEmail.lowercase()
    }

    fun withdraw(amount: BigDecimal) {
        // Kiểm tra tham số đầu vào (Argument -> IllegalArgumentException)
        require(amount > BigDecimal.ZERO) { "So tien rut phai lon hon 0 (nhan duoc: $amount)" }

        // 3. check: Kiểm tra trạng thái hiện tại của đối tượng (State -> IllegalStateException)
        check(status == "ACTIVE") { "Chi duoc rut tien khi vi dang ACTIVE (trang thai hien tai: $status)" }
        check(balance >= amount) { "So du khong du! Hien co: $balance, yeu cau rut: $amount" }

        balance -= amount // Toán tử `-=` trên BigDecimal
        println("  -> [THANH CONG] Da rut $amount tu vi $walletId ($ownerEmail). So du moi: $balance")
    }

    fun freezeWallet() {
        check(status != "CLOSED") { "Khong the dong bang vi da bi dong vinh vien (CLOSED)" }
        status = "FROZEN"
    }

    fun getStatusDescription(): String = when (status) {
        "ACTIVE" -> "Dang hoat dong binh thuong"
        "FROZEN" -> "Tam khoa giao dich"
        "CLOSED" -> "Da dong vinh vien"
        // 4. error(...): Trả về kiểu `Nothing`, ném IllegalStateException cho nhánh không thể xảy ra
        else -> error("Phat hien trang thai vi khong xac dinh trong DB: '$status'")
    }
}

// Hàm sử dụng `try - catch` như một Expression trả về giá trị trực tiếp
fun parseServerPort(rawConfig: String?): Int = try {
    val parsed = rawConfig?.trim()?.toInt() ?: 8080
    require(parsed in 1..65535) { "Port nam ngoai dai hop le 1..65535: $parsed" }
    parsed // Giá trị trả về của nhánh try
} catch (e: NumberFormatException) {
    println("  -> [Canh bao] Cau hinh port '$rawConfig' khong phai so nguyen hop le, dung mac dinh 8080.")
    8080 // Giá trị trả về của nhánh catch
} catch (e: IllegalArgumentException) {
    println("  -> [Canh bao] ${e.message}, dung mac dinh 8080.")
    8080
}

fun main() {
    println("--- ___ * START EX03: EXCEPTIONS & PRECONDITIONS * ___ ---\n")

    // =========================================================================
    // 1. `try - catch` LÀ MỘT BIỂU THỨC (EXPRESSION)
    // =========================================================================
    println("1. Su dung `try - catch` nhu mot Expression:")
    val port1 = parseServerPort("9090")
    val port2 = parseServerPort("abc_xyz")
    val port3 = parseServerPort("99999")
    println("  - Ket qua port1 = $port1, port2 = $port2, port3 = $port3")
    check(port1 == 9090 && port2 == 8080 && port3 == 8080)

    // =========================================================================
    // 2. PRECONDITIONS: `require`, `requireNotNull`, `check`, `error`
    // =========================================================================
    println("\n2. Thuc hanh Preconditions (`require`, `requireNotNull`, `check`, `error`):")
    val wallet = BankWallet(
        walletId = "W-001",
        ownerEmail = "  AN.NGUYEN@EXAMPLE.COM  ",
        initialBalance = BigDecimal("1000000")
    )
    println("  - Khoi tao vi thanh cong cho '${wallet.ownerEmail}', trang thai: ${wallet.getStatusDescription()}")
    wallet.withdraw(BigDecimal("350000"))
    check(wallet.balance == BigDecimal("650000"))

    // Thử nghiệm các tình huống vi phạm quy tắc nghiệp vụ
    println("\n3. Kiem chung thong bao loi tu Preconditions:")

    // 3a. Vi phạm `requireNotNull`
    try {
        BankWallet("W-002", "   ", BigDecimal("500000"))
    } catch (e: IllegalArgumentException) {
        println("  - Bat duoc IllegalArgumentException (requireNotNull): ${e.message}")
    }

    // 3b. Vi phạm `require(amount > 0)`
    try {
        wallet.withdraw(BigDecimal("-50000"))
    } catch (e: IllegalArgumentException) {
        println("  - Bat duoc IllegalArgumentException (require): ${e.message}")
    }

    // 3c. Vi phạm `check(status == "ACTIVE")`
    wallet.freezeWallet()
    try {
        wallet.withdraw(BigDecimal("100000"))
    } catch (e: IllegalStateException) {
        println("  - Bat duoc IllegalStateException (check): ${e.message}")
    }

    println("\n--- ___ * END EX03 * ___ ---")
}
