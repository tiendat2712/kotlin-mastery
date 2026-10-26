package view

/**
 * =====================================================================
 * LESSON 01 - EX02: BIẾN (VAL, VAR, CONST VAL) & SUY LUẬN KIỂU
 * =====================================================================
 *
 * 1. BẢN CHẤT (STACK VS HEAP / BYTECODE):
 *    - `val` (Value - Read-only reference):
 *      + Tương đương từ khóa `final` trong Java (`Ex01FinalDef.java` ở `D:\Java_Journey`).
 *      + Khóa chặt giá trị nằm tại ô nhớ trên **STACK**:
 *        * Nếu là kiểu nguyên thủy (`Int`, `Double`, `Boolean`): giá trị nằm ngay trên Stack -> bất biến hoàn toàn.
 *        * Nếu là kiểu đối tượng (Object reference): ô nhớ trên Stack lưu **địa chỉ (reference)** trỏ tới đối tượng trên **HEAP**.
 *          `val` chỉ cấm gán lại biến sang đối tượng khác (`=` bị cấm), nhưng nếu đối tượng trên Heap có thuộc tính `var`
 *          hoặc là tập hợp khả biến (`MutableList`), trạng thái bên trong Heap VẪN THAY ĐỔI ĐƯỢC!
 *      + Khi dịch sang Bytecode cho thuộc tính của class: `val` chỉ sinh ra hàm `getXxx()`, KHÔNG sinh ra `setXxx()`.
 *    - `var` (Variable - Mutable reference):
 *      + Biến bình thường trong Java (không có `final`). Có thể gán lại giá trị/địa chỉ mới cùng kiểu.
 *      + Khi dịch sang Bytecode cho thuộc tính của class: sinh cả `getXxx()` và `setXxx()`.
 *    - `const val` (Compile-time Constant):
 *      + Biên dịch thành `public static final` trong Bytecode và được trình biên dịch **inline** (nhúng thẳng giá trị)
 *        vào mọi nơi gọi nó lúc compile-time!
 *      + Chỉ áp dụng cho kiểu nguyên thủy (Primitive) hoặc `String`, và chỉ được khai báo ở Top-level hoặc trong `object` / `companion object`.
 *
 * 2. SO SÁNH JAVA VS KOTLIN:
 *    - Trong Java, mặc định mọi biến là mutable (`var`), muốn bất biến phải gõ thêm `final`.
 *      Thực tế lập trình viên Java thường lười gõ `final` cho biến cục bộ.
 *    - Trong Kotlin, triết lý thiết kế buộc bạn phải chọn ngay từ đầu: `val` hay `var`.
 *      Quy tắc vàng trong Kotlin: **"Luôn dùng `val` mặc định; chỉ đổi sang `var` khi thực sự cần gán lại."**
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Constructor Injection trong `@Service` / `@RestController`:
 *      `class UserService(private val userRepository: UserRepository)` -> ngắn gọn hơn rất nhiều so với
 *      `private final UserRepository userRepository;` + constructor trong Java!
 *    - Dùng `const val` để khai báo hằng số dùng bên trong Annotation (vì Annotation của JVM bắt buộc
 *      tham số phải là compile-time constant):
 *      `@RequestMapping(ApiRoutes.USERS_V1)` hoặc `@Cacheable(CacheNames.PRODUCT_CACHE)`.
 * =====================================================================
 */

// Hằng số biên dịch (Compile-time constants) ở mức Top-level
// Bytecode sinh ra: public static final String DEFAULT_CURRENCY = "VND";
const val DEFAULT_CURRENCY: String = "VND"
const val MAX_RETRY_ATTEMPTS: Int = 3

// Minh họa object gom nhóm các hằng số đường dẫn API cho Spring @RequestMapping
object ApiRoutes {
    const val BASE_API = "/api/v1"
    const val ORDERS_ENDPOINT = "$BASE_API/orders"
}

// Class có thuộc tính `var` (mutable state trên HEAP)
private class ShoppingCart(
    val cartId: String,          // read-only property (chỉ có getter)
    var totalAmount: Long        // mutable property (có cả getter và setter)
)

fun main() {
    println("--- ___ * START: Ex02VariablesValVarConst * ___ ---\n")

    // =================================================================
    // 1. val vs var trên kiểu cơ bản (Stack)
    // =================================================================
    println("1. [val vs var] Tham chiếu chỉ đọc (val) và tham chiếu khả biến (var):")
    val taxRate = 0.08 // Type Inference: Kotlin tự suy luận kiểu là Double
    var currentBalance: Long = 1_000_000L // Khai báo kiểu tường minh (Explicit Type)

    println("   -> Thuế suất cố định (val taxRate): ${taxRate * 100}%")
    println("   -> Số dư ban đầu (var currentBalance): $currentBalance $DEFAULT_CURRENCY")

    // taxRate = 0.10 // ❌ LỖI BIÊN DỊCH (Compile Error): Val cannot be reassigned!
    currentBalance += 500_000L // ✅ Hợp lệ vì currentBalance là `var`
    println("   -> Số dư sau khi nạp thêm: $currentBalance $DEFAULT_CURRENCY\n")

    // =================================================================
    // 2. BẪY KINH ĐIỂN: `val` chỉ khóa tham chiếu trên STACK, không khóa HEAP!
    // (So sánh trực tiếp với Ex01FinalDef.java trong Java_Journey)
    // =================================================================
    println("2. [Stack vs Heap] Bản chất của `val` khi trỏ tới Object trên Heap:")
    val cart = ShoppingCart(cartId = "CART-2026-01", totalAmount = 250_000L)
    println("   -> Trước khi sửa thuộc tính: cartId=${cart.cartId}, totalAmount=${cart.totalAmount}")

    // ❌ KHÔNG THỂ cho `cart` trỏ sang vùng nhớ Heap mới:
    // cart = ShoppingCart("CART-NEW", 0L) // Compile Error: Val cannot be reassigned

    // ✅ NHƯNG CÓ THỂ thay đổi thuộc tính `var totalAmount` của đối tượng nằm trên HEAP:
    cart.totalAmount = 490_000L
    println("   -> Sau khi sửa trạng thái trên Heap: cartId=${cart.cartId}, totalAmount=${cart.totalAmount}")
    println("   => Bài học: Muốn Immutability thực sự (Thread-safe), cả biến tham chiếu lẫn")
    println("      các thuộc tính bên trong class đều phải dùng `val`!\n")

    // =================================================================
    // 3. Khai báo `val` chưa gán ngay (Deferred Initialization)
    // =================================================================
    println("3. [Deferred val] Khai báo `val` trước, gán giá trị duy nhất 1 lần theo nhánh:")
    val orderValue = 1_500_000L
    val shippingFee: Int // Chưa gán ngay nên bắt buộc phải ghi rõ kiểu `: Int`

    if (orderValue >= 1_000_000L) {
        shippingFee = 0
    } else {
        shippingFee = 30_000
    }
    // Trình biên dịch Kotlin kiểm tra chắc chắn mọi nhánh đều gán `shippingFee` đúng 1 lần!
    println("   -> Phí vận chuyển được gán 1 lần duy nhất: $shippingFee $DEFAULT_CURRENCY\n")

    // =================================================================
    // 4. Sự khác biệt giữa `const val` và `val`
    // =================================================================
    println("4. [const val vs val] Hằng số lúc Compile-time vs Giá trị lúc Runtime:")
    // `const val` đã biết giá trị ngay khi dịch code (compile-time).
    // Ngược lại, `val` có thể nhận giá trị được tính toán lúc chạy (runtime):
    val runtimeTimestamp: Long = System.currentTimeMillis()
    println("   -> [Compile-time] MAX_RETRY_ATTEMPTS = $MAX_RETRY_ATTEMPTS")
    println("   -> [Compile-time] Spring Route = ${ApiRoutes.ORDERS_ENDPOINT}")
    println("   -> [Runtime val]  runtimeTimestamp = $runtimeTimestamp")

    println("\n--- ___ * END: Ex02VariablesValVarConst * ___ ---")
}
