package view

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/**
 * BÀI 05: TIMEOUTS (`withTimeoutOrNull`), RESOURCE CLEANUP (`NonCancellable`) & ĐỒNG BỘ HÓA VỚI `Mutex`
 *
 * [PHẦN MỞ RỘNG COROUTINES — Không bắt buộc phải thuộc lòng trước khi bắt đầu học Spring MVC cơ bản,
 * nhưng rất hữu ích khi xử lý tác vụ bất đồng bộ thực tế]
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / TIMEOUT / FINALLY / MUTEX):
 *    - **Giới hạn thời gian thực thi với `withTimeout` và `withTimeoutOrNull`**:
 *      + Khi gọi một dịch vụ bên ngoài (Payment Gateway, AI API, Microservice), bạn không bao giờ muốn Coroutine
 *        đợi vô thời hạn.
 *      + `withTimeout(timeMillis) { ... }`: Tự động hủy Coroutine bên trong và ném `TimeoutCancellationException`
 *        (một lớp con của `CancellationException`) nếu quá thời gian quy định.
 *      + `withTimeoutOrNull(timeMillis) { ... }`: Lịch sự hơn — nếu quá hạn, nó tự hủy khối lệnh bên trong và
 *        trả về `null` (rất tiện kết hợp với toán tử Elvis `?: fallbackValue` mà không cần `try-catch`).
 *
 *    - **Dọn dẹp tài nguyên khi bị hủy với `cancelAndJoin()` & `withContext(NonCancellable)`**:
 *      + `job.cancelAndJoin()` là hàm tiện ích gộp `job.cancel()` (gửi tín hiệu hủy) và `job.join()` (đợi Coroutine
 *        chạy xong khối `finally` rồi mới đi tiếp).
 *      + Khi một Coroutine đang ở trạng thái **Đã bị hủy (Cancelled)**, mọi lệnh gọi `suspend fun` thông thường
 *        (như `delay()`, đóng kết nối mạng bất đồng bộ, gửi log hủy về server) bên trong khối `finally` sẽ lập tức
 *        ném `CancellationException` và không chạy được!
 *      + Trong trường hợp hiếm hoi bắt buộc phải gọi `suspend fun` trong khối `finally`, ta bọc phần đó vào
 *        `withContext(NonCancellable) { ... }` để tạm thời miễn nhiễm với trạng thái hủy trong lúc dọn dẹp.
 *
 *    - **Trạng thái khả biến dùng chung (Shared Mutable State) & `kotlinx.coroutines.sync.Mutex`**:
 *      + Khi nhiều Coroutine chạy song song trên các Dispatcher đa luồng (như `Dispatchers.Default` hoặc `Dispatchers.IO`),
 *        việc cùng đọc/ghi vào một biến chung (`var counter = 0`) sẽ gây ra **Race Condition (Lost Update)** giống hệt
 *        như đa luồng `Thread` trong Java (`lesson14-thread`)!
 *      + Thay vì dùng `synchronized` của Java (vốn khóa cứng OS Thread trong lúc chờ khóa), Coroutines cung cấp
 *        `Mutex` (`mutex.withLock { ... }`): Khi khóa đang bận, Coroutine chỉ **TẠM DỪNG (Suspend)** và nhường OS Thread
 *        cho việc khác!
 */

suspend fun callSlowExternalLoyaltyService(customerId: String, simulatedDelayMs: Long): Int {
    delay(simulatedDelayMs)
    return 1_500 // Điểm thưởng giả lập
}

fun main() = runBlocking {
    println("--- ___ * START EX05: TIMEOUTS, CLEANUP (NONCANCELLABLE) & MUTEX * ___ ---\n")

    // =========================================================================
    // 1. XỬ LÝ QUÁ HẠN AN TOÀN VỚI `withTimeout` VÀ `withTimeoutOrNull`
    // =========================================================================
    println("1. Gioi han thoi gian cho voi `withTimeoutOrNull` va `withTimeout`:")

    // Trường hợp A: Dịch vụ trả lời kịp thời (40ms < giới hạn 100ms)
    val fastPoints: Int = withTimeoutOrNull(100) {
        callSlowExternalLoyaltyService("C001", simulatedDelayMs = 40)
    } ?: 0
    println("  - [Kip thoi 40ms/100ms] Diem thuong lay duoc: $fastPoints")
    check(fastPoints == 1_500)

    // Trường hợp B: Dịch vụ bị treo/chậm (200ms > giới hạn 80ms) -> `withTimeoutOrNull` trả về `null`, dùng Elvis lấy 0
    val slowPointsWithFallback: Int = withTimeoutOrNull(80) {
        callSlowExternalLoyaltyService("C002", simulatedDelayMs = 200)
    } ?: -1
    println("  - [Qua han 200ms/80ms voi withTimeoutOrNull] Ket qua fallback an toan: $slowPointsWithFallback")
    check(slowPointsWithFallback == -1)

    // Trường hợp C: `withTimeout` ném `TimeoutCancellationException` khi quá hạn
    var timeoutExceptionCaught = false
    try {
        withTimeout(60) {
            callSlowExternalLoyaltyService("C003", simulatedDelayMs = 200)
        }
    } catch (e: TimeoutCancellationException) {
        timeoutExceptionCaught = true
        println("  - [Qua han voi withTimeout] Bat duoc ${e::class.simpleName}: ${e.message}")
    }
    check(timeoutExceptionCaught)

    // =========================================================================
    // 2. DỌN DẸP TÀI NGUYÊN KHI HỦY (`cancelAndJoin` & `withContext(NonCancellable)`)
    // =========================================================================
    println("\n2. Don dep tai nguyen trong khoi `finally` voi `cancelAndJoin()` & `NonCancellable`:")
    val auditEvents = mutableListOf<String>()

    val streamingJob = launch {
        try {
            auditEvents.add("OPEN_STREAM")
            println("  - [StreamingJob] Da mo ket noi stream, dang xu ly du lieu...")
            delay(500) // Giả lập tác vụ dài sẽ bị hủy giữa chừng
            auditEvents.add("COMPLETED_NORMALLY")
        } finally {
            // Khi Coroutine đã bị cancel, nếu gọi `delay(20)` trực tiếp ở đây sẽ nổ CancellationException ngay!
            // Bọc trong `withContext(NonCancellable)` cho phép thực hiện nốt thao tác đóng kết nối bất đồng bộ.
            withContext(NonCancellable) {
                delay(20) // Mô phỏng gọi hàm suspend gửi gói tin đóng kết nối (flush/close async)
                auditEvents.add("ASYNC_CLEANUP_DONE")
                println("  - [StreamingJob - finally] Da dong ket noi bat dong bo an toan nho `withContext(NonCancellable)`!")
            }
        }
    }

    delay(50)
    println("  - [Main] Goi `streamingJob.cancelAndJoin()`...")
    streamingJob.cancelAndJoin()
    println("  -> Chuoi su kien ghi nhan duoc: $auditEvents")
    check(auditEvents == listOf("OPEN_STREAM", "ASYNC_CLEANUP_DONE"))

    // =========================================================================
    // 3. BẢO VỆ TRẠNG THÁI DÙNG CHUNG TRÊN MULTI-THREADED DISPATCHER VỚI `Mutex`
    // =========================================================================
    println("\n3. Bao ve bien dem chung tren `Dispatchers.Default` bang `Mutex.withLock`:")
    val mutex = Mutex()
    var safeInventorySold = 0

    // Chạy 500 Coroutines trên Thread Pool đa luồng Dispatchers.Default, mỗi Coroutine tăng biến đếm 2 lần
    coroutineScope {
        repeat(500) {
            launch(Dispatchers.Default) {
                repeat(2) {
                    // `mutex.withLock` đảm bảo tại một thời điểm chỉ có 1 Coroutine vào vùng găng (Critical Section),
                    // các Coroutine khác tạm dừng (suspend) chứ không khóa cứng OS Thread!
                    mutex.withLock {
                        safeInventorySold++
                    }
                }
            }
        }
    }
    println("  -> Ket qua bien dem sau 500 x 2 lan tang co `mutex.withLock`: $safeInventorySold (Ky vong: 1000)")
    check(safeInventorySold == 1_000)

    // =========================================================================
    // 4. GÓC TỰ LUYỆN (ACTIVE LEARNING)
    // =========================================================================
    println("\n4. Goc Tu Luyen (Active Learning):")

    // --- 4a. Dự đoán kết quả (Predict Output) ---
    // [ĐỀ BÀI 4A]: Đoạn code với `withTimeoutOrNull(50)` bên dưới sẽ trả về giá trị gì?
    // A. "DONE"
    // B. "TIMEOUT_FALLBACK"
    // C. Ném ngoại lệ làm dừng chương trình
    val quiz4aResult = withTimeoutOrNull(50) {
        delay(120)
        "DONE"
    } ?: "TIMEOUT_FALLBACK"
    println("  - [4a - Predict Output] Ket qua khi delay(120) ben trong withTimeoutOrNull(50): $quiz4aResult")
    check(quiz4aResult == "TIMEOUT_FALLBACK")

    // --- 4b. Tìm lỗi sai (Find the Bug) ---
    // [ĐỀ BÀI 4B]: Nếu trong khối `finally` của một Coroutine ĐÃ BỊ HỦY, ta gọi thẳng `delay(10)`
    // mà KHÔNG có `withContext(NonCancellable)`, dòng lệnh phía sau `delay(10)` có chạy được không?
    var lineAfterUnprotectedDelayExecuted = false
    val buggyCleanupJob = launch {
        try {
            delay(200)
        } finally {
            try {
                delay(10) // LỖI: Coroutine đã bị hủy nên `delay(10)` ném ngay CancellationException!
                lineAfterUnprotectedDelayExecuted = true
            } catch (_: kotlinx.coroutines.CancellationException) {
                println("  - [4b - Find the Bug] Goi `delay(10)` truc tiep trong `finally` khi da cancel bi nem CancellationException ngay!")
            }
        }
    }
    delay(20)
    buggyCleanupJob.cancelAndJoin()
    check(!lineAfterUnprotectedDelayExecuted)

    // --- 4c. Bài tập nhỏ (Mini Exercise) ---
    // [ĐỀ BÀI 4C]: Hoàn thiện class `SafeFlashSaleStock(initialStock: Int)` dùng `Mutex`
    // để khi 200 khách hàng cùng đặt mua 1 sản phẩm trên `Dispatchers.Default` cho kho chỉ có 50 chiếc,
    // số lượng bán thành công đúng bằng 50 và tồn kho cuối cùng đúng bằng 0 (không bao giờ bị âm kho!).
    val flashSale = SafeFlashSaleStock(initialStock = 50)
    var successOrders = 0
    val orderResultMutex = Mutex()

    coroutineScope {
        repeat(200) {
            launch(Dispatchers.Default) {
                if (flashSale.tryReserveOne()) {
                    orderResultMutex.withLock { successOrders++ }
                }
            }
        }
    }
    println("  - [4c - Mini Exercise] Kho 50 san pham, 200 khach mua cung luc -> Thanh cong: $successOrders, Ton kho con: ${flashSale.remainingStock()}")
    check(successOrders == 50 && flashSale.remainingStock() == 0)
    println("  => [PASS] Goc Tu Luyen Ex05 hoan tat!")

    println("\n--- ___ * END EX05 * ___ ---")
}

// [LỜI GIẢI THAM KHẢO CHO MINI EXERCISE 4C]:
class SafeFlashSaleStock(initialStock: Int) {
    private val mutex = Mutex()
    private var stock = initialStock

    suspend fun tryReserveOne(): Boolean = mutex.withLock {
        if (stock > 0) {
            stock--
            true
        } else {
            false
        }
    }

    suspend fun remainingStock(): Int = mutex.withLock { stock }
}
