package view

/**
 * =====================================================================
 * LESSON 02 - EX06: KHỞI TẠO TRỄ — `lateinit var` VS `by lazy`
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Trong Kotlin, nguyên tắc mặc định là mọi thuộc tính Non-Null của class phải được gán giá trị
 *      ngay lúc khởi tạo (trong Constructor hoặc khối `init`).
 *    - Tuy nhiên, thực tế có 2 tình huống bạn CHƯA THỂ (hoặc CHƯA MUỐN) gán giá trị ngay trong Constructor:
 *
 *    A. **`lateinit var` (Late Initialization cho biến khả biến `var`)**:
 *       - Dành cho thuộc tính Non-Null sẽ được một Framework (Spring Dependency Injection, JUnit `@BeforeEach`)
 *         bơm giá trị vào sau khi Constructor đã chạy xong.
 *       - Dưới tầng Bytecode: Biến được lưu dưới dạng tham chiếu đối tượng với giá trị khởi đầu là `null`.
 *         Khi đọc biến, Kotlin kiểm tra nếu nó vẫn là `null` thì ném ngoại lệ cực kỳ rõ nghĩa:
 *         `kotlin.UninitializedPropertyAccessException: lateinit property xxx has not been initialized`
 *         (thay vì `NullPointerException` mù mờ!).
 *       - **TẠI SAO `lateinit` KHÔNG DÙNG ĐƯỢC CHO KIỂU NGUYÊN THỦY (`Int`, `Long`, `Boolean`, `Double`)?**
 *         + Vì dưới Bytecode, Kotlin dùng giá trị `null` làm cờ đánh dấu "biến chưa được khởi tạo".
 *         + Mà kiểu nguyên thủy (`int`, `boolean` trên JVM) không thể mang giá trị `null` (chúng mặc định là `0` hoặc `false`,
 *           không phân biệt được là chưa gán hay người dùng cố tình gán số `0`)!
 *
 *    B. **`val ... by lazy { ... }` (Lazy Delegation cho biến chỉ đọc `val`)**:
 *       - Dành cho thuộc tính bất biến (`val`) có chi phí khởi tạo nặng (đọc file cấu hình lớn, mở kết nối,
 *         tính toán phức tạp) mà ta chỉ muốn tính **ĐÚNG 1 LẦN DUY NHẤT khi nó được gọi tới lần đầu tiên**.
 *       - Mặc định, `by lazy` hoạt động ở chế độ `LazyThreadSafetyMode.SYNCHRONIZED`: hoàn toàn **Thread-Safe**
 *         (dùng khóa đồng bộ Double-Checked Locking dưới Bytecode để đảm bảo nhiều luồng cùng gọi cũng chỉ tính 1 lần!).
 *
 * 2. SO SÁNH `lateinit var` VS `by lazy`:
 *    | Tiêu chí             | `lateinit var`                              | `val ... by lazy { ... }`                |
 *    |----------------------|---------------------------------------------|------------------------------------------|
 *    | Từ khóa đi kèm       | Bắt buộc là `var`                           | Bắt buộc là `val`                        |
 *    | Ai gán giá trị?      | Bên ngoài gán vào sau (`service = ...`)     | Tự tính qua khối Lambda `{ ... }`        |
 *    | Kiểu nguyên thủy     | ❌ Cấm (`Int`, `Boolean`, `Double`...)       | ✅ Hỗ trợ mọi kiểu (kể cả `Int`, `Int?`) |
 *    | Thread-safe mặc định | Không                                       | Có (`LazyThreadSafetyMode.SYNCHRONIZED`) |
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong Spring Boot Test (`@SpringBootTest`, `@WebMvcTest`), ta dùng `@Autowired lateinit var mockMvc: MockMvc`
 *      để không phải khai báo `var mockMvc: MockMvc? = null` rồi dùng `mockMvc!!` ở mọi bài test!
 * =====================================================================
 */

// Mô phỏng một Integration Test Controller trong Spring Boot
private class OrderIntegrationTestRunner {

    // Dùng `lateinit var` cho dependency được khởi tạo trong hàm @BeforeEach
    lateinit var paymentGatewayClient: String

    // ❌ LỖI BIÊN DỊCH nếu mở comment dòng dưới:
    // 'lateinit' modifier is not allowed on properties of primitive types
    // lateinit var retryCount: Int

    // Dùng `by lazy` cho tài nguyên nặng: chỉ nạp vào bộ nhớ khi thực sự có hàm truy cập tới nó!
    val heavyRsaPublicKey: String by lazy {
        println("      [Lazy Init] Đang đọc và giải mã khóa RSA 4096-bit từ đĩa cứng (chỉ chạy ĐÚNG 1 LẦN)...")
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA_KOTLIN_2026"
    }

    fun checkAndRunPayment() {
        // Dùng Reflection `::tenBien.isInitialized` để kiểm tra xem `lateinit var` đã được gán hay chưa
        if (!::paymentGatewayClient.isInitialized) {
            println("   -> [Kiểm tra ::isInitialized] paymentGatewayClient CHƯA được khởi tạo!")
        } else {
            println("   -> [Kiểm tra ::isInitialized] Đã sẵn sàng kết nối tới: $paymentGatewayClient")
        }
    }

    fun setupBeforeEach() {
        paymentGatewayClient = "https://sandbox.vnpay.vn/paymentv2"
    }
}

fun main() {
    println("--- ___ * START: Ex06LateinitAndLazy * ___ ---\n")

    val testRunner = OrderIntegrationTestRunner()

    // =================================================================
    // 1. Trải nghiệm `lateinit var` trước và sau khi khởi tạo
    // =================================================================
    println("1. [lateinit var] Kiểm tra trạng thái khởi tạo và ngoại lệ UninitializedPropertyAccessException:")
    testRunner.checkAndRunPayment()

    try {
        // Cố tình đọc biến `lateinit var` khi chưa gọi setupBeforeEach()
        println("   -> Đang thử đọc biến: ${testRunner.paymentGatewayClient}")
    } catch (ex: UninitializedPropertyAccessException) {
        println("   -> [Bắt ngoại lệ chuẩn xác]: ${ex.message}")
    }

    // Giả lập JUnit @BeforeEach chạy trước test case
    println("   -> Đang chạy setupBeforeEach()...")
    testRunner.setupBeforeEach()
    testRunner.checkAndRunPayment()
    println()

    // =================================================================
    // 2. Trải nghiệm `by lazy` (Chỉ tính toán ở lần truy cập đầu tiên, các lần sau lấy từ Cache)
    // =================================================================
    println("2. [by lazy] Khởi tạo lười biếng (Lazy Initialization) và lưu cache tự động:")
    println("   -> Trước khi truy cập `heavyRsaPublicKey`: khối lazy chưa hề chạy!")

    println("   -> Lần truy cập thứ 1:")
    println("      Giá trị = ${testRunner.heavyRsaPublicKey}")

    println("   -> Lần truy cập thứ 2 (lấy ngay kết quả đã cache, KHÔNG chạy lại khối khởi tạo):")
    println("      Giá trị = ${testRunner.heavyRsaPublicKey}")

    println("\n--- ___ * END: Ex06LateinitAndLazy * ___ ---")
}
