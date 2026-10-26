package view

/**
 * =====================================================================
 * LESSON 01 - EX02: BIẾN (VAL, VAR, CONST VAL), MÔ HÌNH BỘ NHỚ & TÍNH BẤT BIẾN
 * =====================================================================
 *
 * 1. BẢN CHẤT CỦA `val`, `var` VÀ `const val`:
 *    - `val` (Read-only reference / property — Chỉ cho phép gán 1 lần, KHÔNG đồng nghĩa với "hằng số bất biến"):
 *      + Tương đương từ khóa `final` trong Java (`Ex01FinalDef.java` ở `D:\Java_Journey`).
 *      + `val` chỉ ngăn việc dùng toán tử `=` để gán lại biến hoặc thuộc tính đó.
 *      + Khi dịch sang Bytecode cho thuộc tính của class: `val` chỉ sinh hàm `getXxx()`, KHÔNG sinh `setXxx()`.
 *      + **Cẩn trọng với Custom Getter**: Một thuộc tính `val` khai báo kèm `get() = ...` hoàn toàn có thể trả về
 *        giá trị KHÁC NHAU ở mỗi lần đọc (ví dụ: `val now: Long get() = System.currentTimeMillis()`)!
 *    - `var` (Mutable reference / property — Biến khả biến):
 *      + Có thể gán lại giá trị/tham chiếu mới cùng kiểu. Thuộc tính `var` của class sinh cả `getXxx()` và `setXxx()`.
 *    - `const val` (Compile-time Constant — Hằng số thời điểm biên dịch):
 *      + Biên dịch thành `public static final` và được trình biên dịch **inline** (nhúng thẳng giá trị) vào nơi gọi.
 *      + Chỉ áp dụng cho kiểu nguyên thủy (Primitive) hoặc `String`, và chỉ khai báo ở Top-level hoặc trong `object` / `companion object`.
 *
 * 2. BIẾN NẰM Ở ĐÂU TRÊN BỘ NHỚ (STACK VS HEAP)? — 3 TRƯỜNG HỢP PHẢI PHÂN BIỆT RÕ:
 *    Không phải cứ khai báo `val` / `var` là biến đó nằm trên Stack! Vị trí lưu trữ phụ thuộc vào **nơi khai báo**
 *    và **cách biến đó được sử dụng**:
 *    - **Trường hợp 1 — Biến cục bộ (Local variables) bên trong hàm**:
 *      + Theo mô hình máy ảo JVM kinh điển, biến cục bộ (`val taxRate = 0.08`, `val cart = ShoppingCart(...)`)
 *        sống trong **Stack Frame** của luồng hiện tại (hoặc được JIT Compiler tối ưu hóa vào thanh ghi CPU / Escape Analysis).
 *      + Nếu biến cục bộ mang kiểu đối tượng (`val cart`), ô nhớ trên Stack Frame chỉ giữ **địa chỉ tham chiếu (reference)**
 *        trỏ tới thực thể `ShoppingCart` nằm trên **HEAP**.
 *    - **Trường hợp 2 — Thuộc tính của đối tượng (Object properties / fields)**:
 *      + Các thuộc tính `val cartId: String` hay `var totalAmount: Long` bên trong `class ShoppingCart` có backing field
 *        nằm trực tiếp **bên trong thực thể `ShoppingCart` trên HEAP**, KHÔNG nằm trên Stack!
 *    - **Trường hợp 3 — Biến cục bộ bị Closure / Lambda bắt giữ (Captured local variable)**:
 *      + Nếu một biến cục bộ `var clickCount = 0` trong hàm bị một Lambda bắt giữ và thay đổi giá trị, trình biên dịch
 *        Kotlin sẽ tự động bọc nó vào một đối tượng hộp (`kotlin.jvm.internal.Ref$IntRef`) cấp phát trên **HEAP**
 *        để cả hàm bao ngoài và Lambda cùng trỏ tới!
 *
 * 3. PHÂN BIỆT 3 CẤP ĐỘ: READ-ONLY VS SHALLOW IMMUTABILITY VS DEEP IMMUTABILITY:
 *    - **Read-only (`val` / `List<T>`)**: Chỉ khóa việc gán lại biến (`=`) hoặc ẩn các hàm sửa đổi trên bề mặt interface.
 *    - **Shallow Immutability (Bất biến nông)**: Các thuộc tính cấp 1 của class đều là `val` (với backing field cố định),
 *      nhưng có thuộc tính trỏ tới đối tượng khả biến (`MutableList` hoặc class khác có `var`). Trạng thái bên dưới vẫn đổi được!
 *    - **Deep Immutability (Bất biến sâu)**: Bản thân tham chiếu là `val`, mọi thuộc tính trong class là `val` cố định,
 *      và đệ quy xuống tất cả các đối tượng/tập hợp con bên trong cũng đều bất biến hoàn toàn. Chỉ khi đạt Deep Immutability,
 *      đối tượng mới thực sự an toàn tuyệt đối khi chia sẻ giữa nhiều luồng (Thread-safe).
 *
 * 4. SPRING KOTLIN CORNER:
 *    - Khuyến nghị thực hành: Ưu tiên dùng `val` mặc định; chỉ chuyển sang `var` khi có lý do rõ ràng cần cập nhật trạng thái.
 *    - Constructor Injection trong `@Service`: `class UserService(private val userRepository: UserRepository)`
 *      lưu tham chiếu `final` bên trong Bean trên Heap.
 *    - Dùng `const val` khi truyền hằng số vào Annotation (`@RequestMapping(ApiRoutes.ORDERS_ENDPOINT)`).
 * =====================================================================
 */

// Hằng số biên dịch (Compile-time constants) ở mức Top-level
const val DEFAULT_CURRENCY: String = "VND"
const val MAX_RETRY_ATTEMPTS: Int = 3

// Minh họa object gom nhóm các hằng số đường dẫn API cho Spring @RequestMapping
object ApiRoutes {
    const val BASE_API = "/api/v1"
    const val ORDERS_ENDPOINT = "$BASE_API/orders"
}

// Class có thuộc tính nằm trên HEAP (cùng với thực thể ShoppingCart)
private class ShoppingCart(
    val cartId: String,          // read-only property: backing field nằm trong instance trên HEAP (chỉ có getter)
    var totalAmount: Long,       // mutable property: backing field nằm trong instance trên HEAP (có cả getter & setter)
    val items: MutableList<String> = mutableListOf() // `val` nhưng trỏ tới tập hợp khả biến (chưa đạt Deep Immutability!)
)

// Class minh họa: `val` KHÔNG có nghĩa là bất biến nếu dùng Custom Getter (`get()`)!
private class SequenceGenerator {
    private var internalCounter: Int = 0

    // Dù khai báo là `val`, mỗi lần đọc `nextTicketNumber` sẽ gọi hàm `get()` và trả về một số mới!
    val nextTicketNumber: Int
        get() = ++internalCounter
}

fun main() {
    println("--- ___ * START: Ex02VariablesValVarConst * ___ ---\n")

    // =================================================================
    // 1. val vs var cho biến cục bộ (Local Variables trên Stack Frame)
    // =================================================================
    println("1. [Local val vs var] Bien cuc bo trong Stack Frame cua ham:")
    val taxRate = 0.08 // Type Inference: Kotlin tự suy luận kiểu là Double
    var currentBalance: Long = 1_000_000L // Khai báo kiểu tường minh (Explicit Type)

    println("   -> Thue suat co dinh (val taxRate): ${taxRate * 100}%")
    println("   -> So du ban dau (var currentBalance): $currentBalance $DEFAULT_CURRENCY")

    // taxRate = 0.10 // LỖI BIÊN DỊCH (Compile Error): Val cannot be reassigned!
    currentBalance += 500_000L // Hợp lệ vì currentBalance là `var`
    check(currentBalance == 1_500_000L)
    println("   -> So du sau khi nap them: $currentBalance $DEFAULT_CURRENCY\n")

    // =================================================================
    // 2. MÔ HÌNH BỘ NHỚ: Biến cục bộ (Stack) vs Thuộc tính đối tượng (Heap) & Shallow vs Deep Immutability
    // =================================================================
    println("2. [Memory Model & Immutability] Bien cuc bo (Stack) vs Thuoc tinh doi tuong (Heap):")
    // - Biến cục bộ `cart` nằm ở Stack Frame, giữ địa chỉ trỏ tới đối tượng ShoppingCart trên HEAP.
    // - Các thuộc tính `cart.cartId`, `cart.totalAmount`, `cart.items` đều nằm BÊN TRONG đối tượng ShoppingCart trên HEAP!
    val cart = ShoppingCart(
        cartId = "CART-2026-01",
        totalAmount = 250_000L,
        items = mutableListOf("Ban phim co")
    )
    println("   -> Ban dau: cartId=${cart.cartId}, totalAmount=${cart.totalAmount}, items=${cart.items}")

    // Không thể gán lại biến cục bộ `cart` sang đối tượng khác:
    // cart = ShoppingCart("CART-NEW", 0L) // Compile Error: Val cannot be reassigned

    // Nhưng CÓ THỂ sửa thuộc tính `var totalAmount` trên Heap, hoặc thêm phần tử vào `val items: MutableList`!
    cart.totalAmount = 490_000L
    cart.items.add("Chuot khong day")
    check(cart.totalAmount == 490_000L && cart.items.size == 2)
    println("   -> Sau khi sua tren Heap: cartId=${cart.cartId}, totalAmount=${cart.totalAmount}, items=${cart.items}")
    println("   => Bai hoc: `val` chi cam gan lai tham chieu (`=`). Muon Deep Immutability (an toan da luong),")
    println("      moi thuoc tinh va tap hop con ben trong cung phai bat bien!\n")

    // =================================================================
    // 3. BẪY TƯ DUY: Thuộc tính `val` có Custom Getter (`get()`) trả về giá trị khác nhau mỗi lần đọc!
    // =================================================================
    println("3. [val with Custom Getter] `val` la Read-only (khong co setter), KHONG phai hang so:")
    val ticketGen = SequenceGenerator()
    val firstRead = ticketGen.nextTicketNumber
    val secondRead = ticketGen.nextTicketNumber
    check(firstRead == 1 && secondRead == 2)
    println("   -> Doc `val nextTicketNumber` lan 1: $firstRead")
    println("   -> Doc `val nextTicketNumber` lan 2: $secondRead (Gia tri thay doi du la `val`!)\n")

    // =================================================================
    // 4. Biến cục bộ `var` bị Closure/Lambda capture (Đưa lên Heap qua Ref.IntRef) & `const val`
    // =================================================================
    println("4. [Closure Capture & const val] Bien `var` bi Lambda bat giu va Hang so Compile-time:")
    var capturedCounter = 0 // Vì bị Lambda bên dưới sửa đổi, Kotlin bọc biến này vào `Ref.IntRef` trên HEAP!
    val incrementLambda: () -> Unit = {
        capturedCounter += 10
    }
    incrementLambda()
    incrementLambda()
    check(capturedCounter == 20)
    println("   -> Bien cuc bo `var capturedCounter` sau 2 lan goi Lambda (qua Ref.IntRef tren Heap): $capturedCounter")

    // Khai báo `val` gán trễ theo nhánh (Deferred Initialization) và `const val`:
    val orderValue = 1_500_000L
    val shippingFee: Int = if (orderValue >= 1_000_000L) 0 else 30_000
    println("   -> [Deferred val] Phi van chuyen: $shippingFee $DEFAULT_CURRENCY")
    println("   -> [const val]    Spring Route = ${ApiRoutes.ORDERS_ENDPOINT} | MAX_RETRY = $MAX_RETRY_ATTEMPTS\n")

    // =================================================================
    // 5. GÓC TỰ LUYỆN (ACTIVE LEARNING)
    // =================================================================
    println("5. [Goc Tu Luyen] Kiem chung bai tap Deep Immutability:")
    runActiveLearningVerification()

    println("\n--- ___ * END: Ex02VariablesValVarConst * ___ ---")
}

// =====================================================================
// GÓC TỰ LUYỆN (ACTIVE LEARNING EXERCISES)
// =====================================================================
//
// [BÀI 1 - DỰ ĐOÁN KẾT QUẢ]:
// Hãy đọc đoạn code sau và dự đoán 2 giá trị được in ra màn hình trước khi chạy thử:
// ```kotlin
// class DynamicBox {
//     var factor = 2
//     val computed: Int get() = 10 * factor
// }
// val box = DynamicBox()
// val a = box.computed
// box.factor = 5
// val b = box.computed
// println("a = $a, b = $b") // Dự đoán: a = ?, b = ?
// ```
// -> Giải thích: Dù `computed` là `val` và `box` cũng là `val`, nhưng `computed` tính toán qua `get()`
//    dựa trên `factor` (nằm trên Heap), nên `a = 20` và `b = 50`!
//
// [BÀI 2 - TÌM LỖI BIÊN DỊCH / LOGIC (ĐÃ COMMENT ĐỂ FILE BIÊN DỊCH ĐƯỢC)]:
// Vì sao 2 dòng dưới đây bị trình biên dịch báo lỗi nếu bỏ comment?
// ```kotlin
// // Lỗi 1: `const val` chỉ nhận giá trị biết ngay lúc compile-time, không nhận hàm gọi lúc runtime!
// const val START_TIME = System.currentTimeMillis()
//
// // Lỗi 2: Biến `val` chưa khởi tạo ngay bắt buộc phải khai báo kiểu tường minh (`: String`)
// // và phải được gán ở TẤT CẢ các nhánh `if - else` trước khi đọc!
// val statusLabel
// if (MAX_RETRY_ATTEMPTS > 0) { statusLabel = "ACTIVE" }
// println(statusLabel)
// ```
//
// [BÀI 3 - TỰ VIẾT CLASS BẤT BIẾN SÂU (DEEP IMMUTABILITY)]:
// - ĐỀ BÀI:
//   Cho một thiết kế `MutableOrderSummary` chưa an toàn (dùng `var` và `MutableList` khiến bên ngoài
//   có thể tự ý sửa danh sách mã giảm giá sau khi tạo).
//   Hãy thiết kế `ImmutableOrderSummary` đạt tính bất biến sâu (Deep Immutability):
//   + Mọi thuộc tính đều là `val` kiểu bất biến (`String`, `Long`, `List<String>`).
//   + Khi nhận danh sách `coupons` từ bên ngoài vào khởi tạo, hãy sao chép phòng thủ (`toList()`)
//     để nếu bên ngoài sửa `MutableList` gốc thì dữ liệu bên trong `ImmutableOrderSummary` vẫn không bị ảnh hưởng!
//   + Viết hàm `totalAfterDiscount(discountPerCoupon: Long): Long` trả về số tiền sau khi trừ khuyến mãi (tối thiểu là 0L).

// --- LỜI GIẢI THAM KHẢO CHO BÀI 3 ---
private class ImmutableOrderSummary(
    val orderId: String,
    val subtotal: Long,
     couponsInput: List<String>
) {
    // Sao chép phòng thủ (Defensive copy) sang List chỉ đọc để ngăn bên ngoài thay đổi list gốc trên Heap!
    val coupons: List<String> = couponsInput.toList()

    fun totalAfterDiscount(discountPerCoupon: Long): Long {
        val discount = coupons.size * discountPerCoupon
        return (subtotal - discount).coerceAtLeast(0L)
    }
}

private fun runActiveLearningVerification() {
    val externalMutableCoupons = mutableListOf("VOUCHER-50K", "FREESHIP-50K")
    val safeOrder = ImmutableOrderSummary(
        orderId = "ORD-SAFE-01",
        subtotal = 300_000L,
        couponsInput = externalMutableCoupons
    )

    // Cố tình thay đổi danh sách bên ngoài sau khi đã tạo đối tượng `safeOrder`:
    externalMutableCoupons.add("HACKED-COUPON")

    // Kiểm chứng: `safeOrder.coupons` vẫn chỉ có 2 mã, không bị ảnh hưởng bởi `externalMutableCoupons`!
    check(safeOrder.coupons.size == 2) { "Deep immutability failed: internal list was mutated!" }
    check(safeOrder.totalAfterDiscount(50_000L) == 200_000L)
    println("   -> [PASS] ImmutableOrderSummary bao ve an toan danh sach coupons=${safeOrder.coupons}, total=${safeOrder.totalAfterDiscount(50_000L)} VND")
}
