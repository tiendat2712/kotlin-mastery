package view

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

/**
 * BÀI 03: DISPATCHERS, `withContext`, STRUCTURED CONCURRENCY & COOPERATIVE CANCELLATION
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / THREAD POOLS / STRUCTURED CONCURRENCY):
 *    - **Coroutine Dispatchers (Bộ điều phối Thread Pool)**:
 *      + `Dispatchers.Default`: Được tối ưu cho **tác vụ nặng về CPU** (sắp xếp danh sách lớn, mã hóa,
 *        tính toán báo cáo, parse JSON lớn). Số lượng thread tối đa bằng số nhân CPU (`Runtime.getRuntime().availableProcessors()`, tối thiểu 2).
 *      + `Dispatchers.IO`: Được thiết kế cho **tác vụ chờ I/O** (gọi JDBC Database, đọc/ghi File, gọi Blocking HTTP client).
 *        Có khả năng co giãn đàn hồi lên tới **64 threads** (hoặc nhiều hơn nếu cấu hình) để tránh nghẽn cổ chai khi chờ I/O!
 *      + `withContext(Dispatchers.IO) { ... }`: Chuyển Coroutine hiện tại sang chạy trên Thread Pool của `Dispatchers.IO`,
 *        và ngay khi xong khối lệnh sẽ tự động quay về Dispatcher ban đầu mà không cần tạo Coroutine con mới!
 *
 *    - **Structured Concurrency (Đồng thời có cấu trúc — `coroutineScope` vs `supervisorScope`)**:
 *      + Trong Java cũ (`new Thread()` hoặc `ExecutorService.submit()`), nếu bạn quên quản lý `Future`,
 *        một luồng con có thể chạy ngầm mãi mãi gây rò rỉ tài nguyên (Thread leak), hoặc khi 1 luồng con chết,
 *        luồng khác vẫn chạy vô ích.
 *      + Trong Kotlin Coroutines, mọi Coroutine bắt buộc phải chạy trong một `CoroutineScope` theo cấu trúc Cha - Con:
 *        (1) Cha LUÔN ĐỢI tất cả các con hoàn thành trước khi Cha kết thúc.
 *        (2) Khi Cha bị hủy (`cancel()`), TẤT CẢ các con tự động bị hủy theo.
 *        (3) Với **`coroutineScope { ... }`** (All-or-Nothing): Nếu **1 Coroutine con ném Exception**,
 *            nó lập tức hủy tất cả các Coroutine anh em (siblings) đang chạy và ném lỗi lên Cha!
 *        (4) Với **`supervisorScope { ... }`** (Độc lập): Nếu **1 Coroutine con thất bại**, các Coroutine anh em khác
 *            **VẪN TIẾP TỤC CHẠY BÌNH THƯỜNG**! Rất hữu ích khi gửi thông báo/load widget phụ không quan trọng.
 *
 *    - **Cooperative Cancellation (Hủy bỏ mang tính hợp tác) & Cạm bẫy `CancellationException`**:
 *      + Khi một Coroutine bị `cancel()`, nó ném một ngoại lệ đặc biệt là `CancellationException` tại điểm `suspend` (`delay`, `yield`).
 *      + Nếu vòng lặp tính toán CPU không gọi hàm `suspend` nào, bạn cần gọi `ensureActive()` hoặc kiểm tra `isActive`
 *        để Coroutine biết dừng lại.
 *      + **CẠM BẪY SỐ 1**: Nếu bạn viết `try { delay(100) } catch (e: Exception) { log(e) }` mà quên `if (e is CancellationException) throw e`,
 *        bạn đã vô tình **NUỐT MẤT tín hiệu hủy (`CancellationException`)**, khiến Coroutine không thể dừng đúng lúc!
 *
 * 2. SPRING KOTLIN CORNER:
 *    - Khi gọi Spring Data JPA (Blocking JDBC) hoặc thư viện đọc file từ bên trong một `suspend fun`,
 *      bạn LUÔN bọc lệnh gọi đó trong `withContext(Dispatchers.IO) { jpaRepository.findById(id) }`
 *      để không làm nghẽn Event Loop Thread của Spring WebFlux/Netty!
 */

// 1. Minh họa chuyển đổi ngữ cảnh Thread Pool với `withContext`
suspend fun generateEncryptedReport(orderIds: List<String>): String {
    // Bước 1: Đọc dữ liệu từ Database/Disk -> Chuyển sang Dispatchers.IO
    val rawRecords = withContext(Dispatchers.IO) {
        println("  - [Bước 1 - I/O] Đang đọc DB trên Thread: [${Thread.currentThread().name}]")
        delay(50)
        orderIds.map { "RECORD_$it" }
    }

    // Bước 2: Xử lý tính toán/mã hóa nặng CPU -> Chuyển sang Dispatchers.Default
    return withContext(Dispatchers.Default) {
        println("  - [Bước 2 - CPU] Đang tổng hợp & băm dữ liệu trên Thread: [${Thread.currentThread().name}]")
        rawRecords.joinToString(separator = " | ") { it.reversed() }
    }
}

fun main() = runBlocking {
    println("--- ___ * START EX03: DISPATCHERS & STRUCTURED CONCURRENCY * ___ ---\n")

    // =========================================================================
    // 1. CHUYỂN NGỮ CẢNH AN TOÀN VỚI `withContext(Dispatchers.IO / Default)`
    // =========================================================================
    println("1. Chuyển đổi Thread Pool mượt mà với `withContext`:")
    val report = generateEncryptedReport(listOf("O001", "O002", "O003"))
    println("  -> Kết quả báo cáo: $report")

    // =========================================================================
    // 2. `coroutineScope` (ALL-OR-NOTHING): 1 CON LỖI -> HỦY NGAY ANH EM!
    // =========================================================================
    println("\n2. `coroutineScope` (All-or-Nothing): Khi 1 tác vụ con lỗi, tác vụ anh em tự động bị hủy:")
    try {
        coroutineScope {
            launch {
                try {
                    println("  - [Sibling 1 - Tồn kho] Đang giữ chỗ hàng hóa (chờ 300ms)...")
                    delay(300)
                    println("  - [Sibling 1] Giữ chỗ thành công (Dòng này sẽ KHÔNG bao giờ in ra!)")
                } catch (e: CancellationException) {
                    println("  - [Sibling 1 - Tồn kho] Nhận tín hiệu CancellationException -> Tự động giải phóng hàng giữ chỗ!")
                    throw e // Luôn ném lại CancellationException!
                }
            }

            launch {
                delay(60)
                println("  - [Sibling 2 - Thanh toán] LỖI: Thẻ tín dụng bị từ chối sau 60ms!")
                throw IllegalStateException("PaymentGatewayException: Số dư thẻ không đủ")
            }
        }
    } catch (e: IllegalStateException) {
        println("  -> [Cha bắt được lỗi từ coroutineScope]: ${e.message}")
    }

    // =========================================================================
    // 3. `supervisorScope`: 1 CON LỖI -> ANH EM VẪN CHẠY BÌNH THƯỜNG!
    // =========================================================================
    println("\n3. `supervisorScope`: Tác vụ phụ (Gợi ý quảng cáo) lỗi không làm hỏng tác vụ chính (Chi tiết đơn):")
    supervisorScope {
        val orderDetailDeferred = async {
            delay(80)
            "Đơn hàng O001 - Tổng thanh toán: 504.000 VNĐ"
        }

        val adsRecommendationDeferred = async {
            delay(40)
            throw RuntimeException("RecommendationService đang quá tải!")
        }

        val orderDetail = orderDetailDeferred.await()
        val adsResult = runCatching { adsRecommendationDeferred.await() }
            .onFailure { if (it is CancellationException) throw it }
            .getOrDefault("[Không có gợi ý sản phẩm lúc này]")

        println("  - Dữ liệu chính vẫn thành công: $orderDetail")
        println("  - Dữ liệu phụ (Fallback an toàn): $adsResult")
    }

    // =========================================================================
    // 4. COOPERATIVE CANCELLATION VỚI `ensureActive()`
    // =========================================================================
    println("\n4. Hủy tác vụ tính toán CPU hợp tác với `ensureActive()`:")
    val cpuJob = launch(Dispatchers.Default) {
        var processedBatch = 0
        while (processedBatch < 10) {
            // Kiểm tra xem Coroutine đã bị hủy chưa; nếu rồi sẽ ném CancellationException ngay lập tức!
            ensureActive()
            processedBatch++
            Thread.sleep(20) // Giả lập 20ms tính toán CPU thuần túy
        }
    }
    delay(55)
    cpuJob.cancel()
    cpuJob.join()
    println("  -> Đã hủy cpuJob an toàn nhờ `ensureActive()` (isCancelled = ${cpuJob.isCancelled})")

    println("\n--- ___ * END EX03 * ___ ---")
}
