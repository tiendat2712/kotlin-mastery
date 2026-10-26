package view

/**
 * =====================================================================
 * LESSON 04 - EX07: `inline`, `noinline`, `crossinline`, NON-LOCAL RETURNS & CLOSURE CAPTURE
 * =====================================================================
 *
 * 1. BẢN CHẤT CỦA `inline`, `noinline`, `crossinline` DƯỚI TẦNG BYTECODE:
 *    - **Vấn đề của Higher-Order Function thông thường (Non-inline)**:
 *      + Khi một hàm nhận tham số Lambda (`fun execute(action: () -> Unit)`), dưới tầng JVM Bytecode,
 *        Lambda phải được biểu diễn thành một đối tượng thực thi interface `kotlin.jvm.functions.Function0`
 *        (thông qua class ẩn danh hoặc `invokedynamic`).
 *      + Nếu Lambda không bắt giữ biến bên ngoài (Non-capturing), Kotlin có thể tái sử dụng một Singleton instance;
 *        NHƯNG nếu Lambda **bắt giữ biến cục bộ (Capturing closure)** và nằm trong vòng lặp nóng (hot loop),
 *        JVM sẽ phải cấp phát một đối tượng `FunctionN` mới trên **HEAP** ở mỗi lượt gọi, kèm chi phí gọi hàm ảo (`invoke()`)!
 *    - **Giải pháp `inline fun`**:
 *      + Khi đánh dấu `inline fun`, trình biên dịch Kotlin **sao chép (copy-paste) trực tiếp mã Bytecode** của cả
 *        thân hàm `inline` lẫn thân biểu thức Lambda truyền vào ngay tại vị trí gọi hàm (Call site)!
 *      + Hệ quả: **Không tạo đối tượng `FunctionN` trên Heap, không tốn lời gọi hàm phụ**, và mở khóa 2 tính năng
 *        chỉ `inline` mới có: **Non-local `return`** và **Reified Type Parameters (`reified T`, học ở Lesson 07)**.
 *    - **Khi nào cần `noinline`?**
 *      + Vì Lambda được `inline` bị "trải phẳng" thành các lệnh Bytecode nên nó không còn là một đối tượng Object nữa.
 *      + Nếu một `inline fun` có 2 Lambda, trong đó 1 Lambda cần được **lưu vào biến/thuộc tính, trả về từ hàm,
 *        hoặc truyền tiếp cho một hàm non-inline khác**, bạn phải gắn từ khóa `noinline` cho tham số Lambda đó!
 *    - **Khi nào cần `crossinline`?**
 *      + Khi thân của `inline fun` không gọi Lambda trực tiếp ngay trong luồng hàm đó, mà lại gọi Lambda **bên trong
 *        một ngữ cảnh thực thi khác** (ví dụ: bên trong một `Runnable { ... }`, một `object : Callback`, hoặc một Lambda không inline).
 *      + Khi đó, Lambda không thể thực hiện **Non-local `return`** (vì không thể `return` xuyên qua biên giới của `Runnable`!).
 *      + Từ khóa `crossinline` báo cho trình biên dịch: "Vẫn hãy inline mã Bytecode của Lambda này, nhưng **CẤM**
 *        người gọi viết lệnh `return` không nhãn (non-local return) bên trong Lambda!"
 *    - **ĐÁNH ĐỔI & GIỚI HẠN CỦA `inline` (TRADE-OFFS & LIMITATIONS)**:
 *      1) **KHÔNG đánh dấu `inline` cho hàm bình thường (không có tham số Lambda / `reified`)**:
 *         Trình biên dịch Kotlin sẽ cảnh báo *"Expected performance impact from inlining is insignificant"*, vì
 *         JIT Compiler của JVM đã tự động inline các hàm thường ở mức mã máy cực tốt rồi!
 *      2) **Phình to mã Bytecode (Bytecode Bloat)**: Không nên `inline` một hàm có thân dài hàng chục dòng code,
 *         vì toàn bộ khối lệnh đó sẽ bị nhân bản tại mọi nơi gọi hàm.
 *      3) **Không thể `inline` hàm đệ quy** (vì sẽ nhân bản vô tận lúc compile).
 *      4) **Giới hạn phạm vi truy cập (Visibility Restriction)**: Một hàm `public inline` không được phép gọi trực tiếp
 *         các hàm/thuộc tính `private` của class (trừ khi thành phần nội bộ đó được đánh dấu `@PublishedApi internal`).
 *
 * 2. LOCAL RETURN VS NON-LOCAL RETURN & LAMBDA `{ ... }` VS ANONYMOUS FUNCTION `fun(...)`:
 *    - **Quy tắc cốt lõi của Kotlin**:
 *      > **Lệnh `return` không kèm nhãn (`@label`) LUÔN thoát khỏi hàm gần nhất được khai báo bằng từ khóa `fun`!**
 *    - Áp dụng quy tắc này:
 *      + **Biểu thức Lambda `{ ... }`** KHÔNG có từ khóa `fun`:
 *        * Nếu truyền vào hàm `inline` (như `.forEach { ... }`): lệnh `return` trơn sẽ **thoát thẳng khỏi hàm `fun` bao ngoài** (**Non-local Return**).
 *        * Nếu chỉ muốn thoát khỏi lượt chạy hiện tại của Lambda: phải dùng **Local Return có nhãn** (`return@forEach`).
 *        * Nếu truyền vào hàm KHÔNG phải `inline`: trình biên dịch **CẤM** viết `return` trơn, bắt buộc dùng `return@tenHam`.
 *      + **Hàm ẩn danh (Anonymous Function — `fun(x: Int) { return }`)** CÓ từ khóa `fun`:
 *        * Vì bản thân nó có chữ `fun`, nên lệnh `return` trơn bên trong nó chỉ thoát khỏi **chính hàm ẩn danh đó** (**Local Return**),
 *          không cần gõ `@label`! Rất hữu ích khi có nhiều nhánh `return` cục bộ phức tạp.
 *
 * 3. CLOSURE CAPTURE (ĐẶC BIỆT KHI BẮT GIỮ BIẾN KHẢ BIẾN `var` TRÊN HEAP):
 *    - Trong Java, Lambda chỉ được phép truy cập biến cục bộ của hàm ngoài nếu biến đó là `final` hoặc *effectively final*.
 *    - Trong Kotlin, Lambda có thể **bắt giữ và thay đổi trực tiếp biến cục bộ `var`** của hàm bao ngoài!
 *    - **Cơ chế dưới tầng Bytecode**:
 *      + Khi biến cục bộ `var counter = 0` bị Lambda bắt giữ, trình biên dịch Kotlin tự động tạo một đối tượng hộp
 *        `val counter = Ref.IntRef()` nằm trên **HEAP** (với kiểu đối tượng là `Ref.ObjectRef<T>`).
 *      + Mọi thao tác đọc/ghi `counter` ở cả hàm ngoài và bên trong các Lambda thực chất đều đọc/ghi vào trường `counter.element` trên Heap!
 *    - **Cái bẫy kinh điển (Delayed Callback capturing mutable `var`)**:
 *      + Nếu bạn tạo danh sách các callback trì hoãn (chạy sau) bên trong vòng lặp và cùng bắt giữ một biến `var currentStep`,
 *        thì tới lúc các callback được kích hoạt, tất cả chúng đều nhìn thấy **giá trị cuối cùng** của `currentStep` trên Heap!
 *      + Cách khắc phục: Luôn sao chép ra một biến `val snapshot = currentStep` ở mỗi vòng lặp để mỗi callback bắt giữ một bản sao bất biến riêng!
 * =====================================================================
 */

fun main() {
    println("--- ___ * START: Ex07InlineReturnsAndClosures * ___ ---\n")

    // =================================================================
    // 1. `inline`, `noinline` và `crossinline` trong thực tế
    // =================================================================
    println("1. [inline, noinline, crossinline] Co che sao chep Bytecode va rang buoc ngu canh:")
    val auditStore = mutableListOf<() -> String>()

    val txResult = executeInTransaction(
        txName = "TX-ORDER-2026",
        // Tham số 2 (`inline`): được chèn thẳng Bytecode vào hàm, không tạo Object Function0
        primaryAction = {
            println("      [Primary Action] Dang tru ton kho va ghi nhan thanh toan...")
            250_000L
        },
        // Tham số 3 (`noinline`): vì hàm cần lưu Lambda này vào `auditStore` (như một Object trên Heap),
        // nên tham số này bắt buộc phải là `noinline`!
        deferredAuditHook = { "Audit log saved for TX-ORDER-2026" },
        auditCollector = auditStore
    )
    check(txResult == 250_000L && auditStore.size == 1)
    println("   -> Ket qua Transaction: $txResult VND | Deferred Hook: \"${auditStore.first().invoke()}\"")

    // Minh họa `crossinline`: Lambda được chạy bên trong một Runnable (ngữ cảnh khác)
    val runnableTask = buildSafeAsyncTask(taskName = "SendEmailJob") {
        // return // LỖI BIÊN DỊCH nếu bỏ comment: 'return' is not allowed here (do có `crossinline`!)
        println("      [crossinline Task] Dang thuc thi tac vu ben trong Runnable...")
    }
    runnableTask.run()
    println()

    // =================================================================
    // 2. So sánh 3 cơ chế Return: Non-local `return` vs `return@label` vs Anonymous `fun`
    // =================================================================
    println("2. [Local vs Non-local Return] So sanh 3 cach Return khi duyet danh sach:")
    val sampleCodes = listOf("VALID-1", "SKIP-ME", "ABORT-NOW", "VALID-2")

    val resNonLocal = demoNonLocalReturn(sampleCodes)
    val resLabeled = demoLabeledLocalReturn(sampleCodes)
    val resAnonymousFun = demoAnonymousFunctionReturn(sampleCodes)

    check(resNonLocal == "ABORTED_AT_ABORT-NOW")
    check(resLabeled == listOf("VALID-1", "ABORT-NOW", "VALID-2"))
    check(resAnonymousFun == listOf("VALID-1", "ABORT-NOW", "VALID-2"))

    println("   -> [2a. Non-local `return` trong inline lambda] Thoat luon khoi ham ngoai: $resNonLocal")
    println("   -> [2b. Local `return@forEach` co nhan]         Chi bo qua 1 phan tu:      $resLabeled")
    println("   -> [2c. Anonymous `fun(code) { return }`]       Return cuc bo khong nhan:  $resAnonymousFun\n")

    // =================================================================
    // 3. Closure Capture: Biến cục bộ `var` được bọc vào `Ref.IntRef` trên HEAP & Bẫy Callback
    // =================================================================
    println("3. [Closure Capture & Ref.IntRef Trap] Bat giu bien `var` tren Heap va cach phong tranh bay:")
    demonstrateClosureCaptureAndTrap()
    println()

    // =================================================================
    // 4. GÓC TỰ LUYỆN (ACTIVE LEARNING)
    // =================================================================
    println("4. [Goc Tu Luyen] Kiem chung bai tap Retry Helper voi inline:")
    runEx07ActiveLearning()

    println("\n--- ___ * END: Ex07InlineReturnsAndClosures * ___ ---")
}

/**
 * Minh họa kết hợp `inline` và `noinline` trong cùng một Higher-Order Function:
 * - [primaryAction] được inline hoàn toàn tại nơi gọi.
 * - [deferredAuditHook] cần được thêm vào danh sách [auditCollector] (đòi hỏi phải là một Object thực thụ trên Heap),
 *   nên bắt buộc phải đánh dấu `noinline`!
 */
private inline fun <T> executeInTransaction(
    txName: String,
    primaryAction: () -> T,
    noinline deferredAuditHook: () -> String,
    auditCollector: MutableList<() -> String>
): T {
    println("   -> [START TX: $txName]")
    val result = primaryAction()
    // Nếu không có `noinline` ở `deferredAuditHook`, dòng `.add(...)` dưới đây sẽ báo lỗi biên dịch:
    // "Illegal usage of inline-parameter 'deferredAuditHook'"
    auditCollector.add(deferredAuditHook)
    println("   -> [COMMIT TX: $txName]")
    return result
}

/**
 * Minh họa `crossinline`:
 * Vì [body] không chạy trực tiếp trong `buildSafeAsyncTask`, mà được gói vào bên trong đối tượng `Runnable`
 * (một ngữ cảnh thực thi độc lập), ta phải dùng `crossinline` để cấm người gọi viết lệnh non-local `return`!
 */
private inline fun buildSafeAsyncTask(
    taskName: String,
    crossinline body: () -> Unit
): Runnable {
    return Runnable {
        println("   -> [Runnable Wrapper] Bat dau chay task: $taskName")
        body()
    }
}

// 2a. Non-local Return: chữ `return` trơn bên trong `.forEach` thoát thẳng khỏi hàm `demoNonLocalReturn`!
private fun demoNonLocalReturn(codes: List<String>): String {
    codes.forEach { code ->
        if (code == "ABORT-NOW") {
            return "ABORTED_AT_$code" // Thoát ngay khỏi `demoNonLocalReturn`!
        }
    }
    return "COMPLETED_ALL"
}

// 2b. Local Return có nhãn (`return@forEach`): chỉ kết thúc lượt gọi Lambda hiện tại (giống `continue`)
private fun demoLabeledLocalReturn(codes: List<String>): List<String> {
    val kept = mutableListOf<String>()
    codes.forEach { code ->
        if (code == "SKIP-ME") {
            return@forEach // Chỉ thoát khỏi Lambda của phần tử "SKIP-ME", đi tiếp phần tử sau!
        }
        kept.add(code)
    }
    return kept
}

// 2c. Anonymous Function (`fun(code: String)`): vì có từ khóa `fun`, chữ `return` trơn chỉ thoát khỏi hàm ẩn danh đó!
private fun demoAnonymousFunctionReturn(codes: List<String>): List<String> {
    val kept = mutableListOf<String>()
    codes.forEach(fun(code: String) {
        if (code == "SKIP-ME") {
            return // Quy tắc Kotlin: `return` thoát khỏi `fun` gần nhất -> chính là `fun(code: String)`!
        }
        kept.add(code)
    })
    return kept
}

// 3. Minh họa Closure Capture trên Heap (`Ref.IntRef`) và bẫy kinh điển khi trì hoãn thực thi Lambda
private fun demonstrateClosureCaptureAndTrap() {
    // 3a. Khi Lambda không trì hoãn mà cập nhật biến đếm: `Ref.IntRef` trên Heap hoạt động đúng như mong đợi
    var totalProcessed = 0
    listOf(10, 20, 30).forEach { value ->
        totalProcessed += value // Đọc/ghi vào đối tượng hộp Ref.IntRef trên Heap
    }
    check(totalProcessed == 60)
    println("   -> [Ref.IntRef] Tong cong don qua Closure Capture: totalProcessed = $totalProcessed")

    // 3b. BẪY KINH ĐIỂN: Nhiều callback trì hoãn cùng bắt giữ 1 biến `var` dùng chung trên Heap!
    val buggyCallbacks = mutableListOf<() -> String>()
    var sharedMutableStep = 0
    while (sharedMutableStep < 3) {
        sharedMutableStep++
        // Mọi Lambda trong `buggyCallbacks` đều giữ tham chiếu tới CÙNG 1 hộp `Ref.IntRef(sharedMutableStep)` trên Heap!
        buggyCallbacks.add { "Step=$sharedMutableStep" }
    }
    val buggyOutputs = buggyCallbacks.map { it() }
    check(buggyOutputs == listOf("Step=3", "Step=3", "Step=3"))
    println("   -> [TRAP - Capture chung `var`] Khi goi cac callback sau vong lap: $buggyOutputs (Tat ca deu in Step=3!)")

    // 3c. CÁCH KHẮC PHỤC CHUẨN: Tạo một bản sao bất biến `val snapshot` ở mỗi lượt lặp!
    val safeCallbacks = mutableListOf<() -> String>()
    var stepCursor = 0
    while (stepCursor < 3) {
        stepCursor++
        val snapshotStep = stepCursor // Mỗi lượt lặp tạo một biến `val` riêng biệt!
        safeCallbacks.add { "Step=$snapshotStep" }
    }
    val safeOutputs = safeCallbacks.map { it() }
    check(safeOutputs == listOf("Step=1", "Step=2", "Step=3"))
    println("   -> [FIXED - Capture `val` snapshot] Moi callback giu dung gia tri: $safeOutputs")
}

// =====================================================================
// GÓC TỰ LUYỆN (ACTIVE LEARNING EXERCISES)
// =====================================================================
//
// [BÀI 1 - DỰ ĐOÁN KẾT QUẢ]:
// Hàm `mysteryFlow()` dưới đây sẽ trả về giá trị bao nhiêu?
// ```kotlin
// fun mysteryFlow(): Int {
//     var sum = 0
//     listOf(1, 2, 3, 4).forEach { n ->
//         if (n == 2) return@forEach
//         if (n == 4) return sum
//         sum += n * 10
//     }
//     return 999
// }
// ```
// -> Đáp án:
//    - Với `n = 1`: `sum` tăng lên `10`.
//    - Với `n = 2`: `return@forEach` chỉ bỏ qua lượt `n = 2`.
//    - Với `n = 3`: `sum` tăng thêm `30` -> `sum = 40`.
//    - Với `n = 4`: `return sum` là Non-local Return (do `.forEach` là `inline fun`), thoát thẳng khỏi `mysteryFlow()`
//      và trả về `40` (dòng `return 999` không bao giờ chạy!).
//
// [BÀI 2 - TÌM LỖI BIÊN DỊCH (ĐÃ COMMENT ĐỂ FILE CHẠY ĐƯỢC)]:
// Vì sao hàm `brokenInlineWrapper` dưới đây không biên dịch được?
// ```kotlin
// inline fun brokenInlineWrapper(action: () -> Unit) {
//     val runnable = Runnable {
//         action() // LỖI 1: Can't inline 'action' here: it may contain non-local returns. Add 'crossinline' modifier!
//     }
//     saveToQueue(action) // LỖI 2: Illegal usage of inline-parameter 'action'. Add 'noinline' modifier!
// }
// ```
//
// [BÀI 3 - TỰ VIẾT HÀM `inline` TIỆN ÍCH]:
// - ĐỀ BÀI:
//   Hãy viết một hàm `inline fun <T> withRetryOnFailure(maxAttempts: Int, block: (attempt: Int) -> T): T`
//   thực thi `block(attempt)` từ lần thử `1` đến `maxAttempts`.
//   Nếu `block` thành công thì trả về kết quả ngay; nếu ném `RuntimeException` và chưa hết số lần thử thì thử lại;
//   nếu hết `maxAttempts` mà vẫn lỗi thì ném lại ngoại lệ cuối cùng.

// --- LỜI GIẢI THAM KHẢO CHO BÀI 3 ---
private inline fun <T> withRetryOnFailure(
    maxAttempts: Int,
    block: (attempt: Int) -> T
): T {
    require(maxAttempts >= 1) { "maxAttempts phai >= 1" }
    var lastException: RuntimeException? = null
    for (attempt in 1..maxAttempts) {
        try {
            return block(attempt)
        } catch (ex: RuntimeException) {
            lastException = ex
        }
    }
    throw checkNotNull(lastException)
}

private fun runEx07ActiveLearning() {
    var callCounter = 0
    val response = withRetryOnFailure(maxAttempts = 3) { attempt ->
        callCounter++
        if (attempt < 3) {
            throw IllegalStateException("Tam thoi mat ket noi o lan thu $attempt")
        }
        "SUCCESS_ON_ATTEMPT_$attempt"
    }
    check(callCounter == 3 && response == "SUCCESS_ON_ATTEMPT_3")
    println("   -> [PASS] withRetryOnFailure thanh cong sau $callCounter lan thu: $response")
}
