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
 *      + `Dispatchers.IO`: Được thiết kế cho **tác vụ chờ Blocking I/O** (đọc/ghi File, gọi Blocking Socket/HTTP client).
 *        Có khả năng co giãn đàn hồi lên tới **64 threads** (hoặc bằng số nhân CPU nếu lớn hơn, và có thể cấu hình thêm)
 *        để các tác vụ chờ I/O không làm đói CPU thread pool của `Dispatchers.Default`.
 *      + `withContext(Dispatchers.IO) { ... }`: Chuyển phần thực thi của Coroutine hiện tại sang Thread Pool được chỉ định,
 *        và ngay khi xong khối lệnh sẽ tự động quay về Dispatcher ban đầu mà không cần tạo Coroutine con mới.
 *
 *    - **Structured Concurrency (Đồng thời có cấu trúc — `coroutineScope` vs `supervisorScope`)**:
 *      + Trong Kotlin Coroutines, mọi Coroutine bắt buộc phải chạy trong một `CoroutineScope` theo cấu trúc Cha - Con:
 *        (1) Cha LUÔN ĐỢI tất cả các con hoàn thành trước khi Cha kết thúc.
 *        (2) Khi Cha bị hủy (`cancel()`), TẤT CẢ các con tự động bị hủy theo.
 *        (3) Với **`coroutineScope { ... }`** (All-or-Nothing): Nếu **1 Coroutine con ném Exception**,
 *            nó lập tức hủy tất cả các Coroutine anh em (siblings) đang chạy và ném lỗi lên Cha!
 *        (4) Với **`supervisorScope { ... }`** (Độc lập): Nếu **1 Coroutine con thất bại**, các Coroutine anh em khác
 *            **VẪN TIẾP TỤC CHẠY BÌNH THƯỜNG**! Rất hữu ích khi gửi thông báo hoặc tải widget phụ không quan trọng.
 *
 *    - **Cooperative Cancellation (Hủy bỏ mang tính hợp tác) & Cạm bẫy `CancellationException`**:
 *      + Khi một Coroutine bị `cancel()`, nó ném một ngoại lệ đặc biệt là `CancellationException` tại điểm `suspend` (`delay`, `yield`).
 *      + Nếu vòng lặp tính toán CPU không gọi hàm `suspend` nào, bạn cần gọi `ensureActive()` hoặc kiểm tra `isActive`
 *        để Coroutine biết dừng lại.
 *      + **CẠM BẪY SỐ 1**: Nếu bạn viết `try { delay(100) } catch (e: Exception) { log(e) }` mà quên `if (e is CancellationException) throw e`,
 *        bạn đã vô tình **NUỐT MẤT tín hiệu hủy (`CancellationException`)**, khiến Coroutine không thể dừng đúng lúc!
 *
 * 2. SPRING KOTLIN CORNER — HIỂU ĐÚNG VỀ SPRING DATA JPA (JDBC), DISPATCHERS & `@Transactional`:
 *    - **Tránh ngộ nhận phổ biến**: Nhiều bài viết khuyên *"Cứ gọi Spring Data JPA là bọc trong `withContext(Dispatchers.IO)`"*.
 *      Cách hiểu đó thiếu chính xác trong thực tế kiến trúc Spring Boot vì 4 lý do kỹ thuật sau:
 *      (1) **Trong Spring MVC truyền thống (Servlet / Virtual Threads) + Spring Data JPA (JDBC)**:
 *          Mỗi HTTP request vốn đã chạy trên một Worker Thread riêng (hoặc Virtual Thread trên Java 21+).
 *          Bạn chỉ cần viết hàm bình thường (KHÔNG cần `suspend`) và gọi trực tiếp `jpaRepository.findById(...)`
 *          mà không cần `withContext(Dispatchers.IO)`.
 *      (2) **Dời sang `Dispatchers.IO` KHÔNG biến JDBC thành Non-blocking**:
 *          Bản chất driver JDBC vẫn là Blocking I/O; gọi nó trong `withContext(Dispatchers.IO)` chỉ là chuyển sự chờ đợi
 *          từ Thread hiện tại sang một Thread thuộc `Dispatchers.IO` (hữu ích khi bạn buộc phải gọi thư viện blocking
 *          từ một luồng Event Loop như Spring WebFlux/Netty để tránh khóa Event Loop).
 *      (3) **Cạm bẫy với `@Transactional` (ThreadLocal Transaction Context)**:
 *          Spring Transaction truyền thống (JDBC/JPA `PlatformTransactionManager`) lưu trạng thái Transaction và
 *          Database Connection bên trong **`ThreadLocal`** của Thread bắt đầu giao dịch!
 *          Nếu đang ở trong một hàm `@Transactional` gắn với Thread A mà bạn nhảy sang Thread B bằng
 *          `withContext(Dispatchers.IO)` (hoặc `launch`/`async`), `ThreadLocal` **KHÔNG tự động đi theo sang Thread B**,
 *          dẫn đến lệnh gọi DB ở Thread B chạy ngoài Transaction, mất kết nối hoặc gây lỗi `LazyInitializationException`!
 *      (4) **Trong Spring WebFlux / Reactive Coroutines**:
 *          Để truy cập DB bất đồng bộ thực sự và giữ được Transaction qua các lần chuyển Thread của Coroutine,
 *          hệ sinh thái Spring sử dụng **R2DBC** (`CoroutineCrudRepository`) kết hợp với `ReactiveTransactionManager` /
 *          `TransactionalOperator` (lưu ngữ cảnh giao dịch trong `ReactorContext` thuộc `CoroutineContext` thay vì `ThreadLocal`).
 */

// 1. Minh họa chuyển đổi ngữ cảnh Thread Pool với `withContext`
suspend fun generateEncryptedReport(orderIds: List<String>): String {
    // Bước 1: Đọc dữ liệu từ File/Blocking I/O độc lập -> Chuyển sang Dispatchers.IO
    val rawRecords = withContext(Dispatchers.IO) {
        println("  - [Buoc 1 - I/O] Dang doc du lieu tren Thread: [${Thread.currentThread().name}]")
        delay(50)
        orderIds.map { "RECORD_$it" }
    }

    // Bước 2: Xử lý tính toán/mã hóa nặng CPU -> Chuyển sang Dispatchers.Default
    return withContext(Dispatchers.Default) {
        println("  - [Buoc 2 - CPU] Dang tong hop & bam du lieu tren Thread: [${Thread.currentThread().name}]")
        rawRecords.joinToString(separator = " | ") { it.reversed() }
    }
}

fun main() = runBlocking {
    println("--- ___ * START EX03: DISPATCHERS & STRUCTURED CONCURRENCY * ___ ---\n")

    // =========================================================================
    // 1. CHUYỂN NGỮ CẢNH AN TOÀN VỚI `withContext(Dispatchers.IO / Default)`
    // =========================================================================
    println("1. Chuyen doi Thread Pool muot ma voi `withContext`:")
    val report = generateEncryptedReport(listOf("O001", "O002", "O003"))
    println("  -> Ket qua bao cao: $report")
    check(report.contains("100O_DROCER"))

    // =========================================================================
    // 2. `coroutineScope` (ALL-OR-NOTHING): 1 CON LỖI -> HỦY NGAY ANH EM!
    // =========================================================================
    println("\n2. `coroutineScope` (All-or-Nothing): Khi 1 tac vu con loi, tac vu anh em tu dong bi huy:")
    var sibling1CleanedUp = false
    try {
        coroutineScope {
            launch {
                try {
                    println("  - [Sibling 1 - Ton kho] Dang giu cho hang hoa (cho 300ms)...")
                    delay(300)
                    println("  - [Sibling 1] Giu cho thanh cong (Dong nay se KHONG bao gio in ra!)")
                } catch (e: CancellationException) {
                    sibling1CleanedUp = true
                    println("  - [Sibling 1 - Ton kho] Nhan tin hieu CancellationException -> Tu dong giai phong hang giu cho!")
                    throw e // Luôn ném lại CancellationException!
                }
            }

            launch {
                delay(60)
                println("  - [Sibling 2 - Thanh toan] LOI: The tin dung bi tu choi sau 60ms!")
                throw IllegalStateException("PaymentGatewayException: So du the khong du")
            }
        }
    } catch (e: IllegalStateException) {
        println("  -> [Cha bat duoc loi tu coroutineScope]: ${e.message}")
    }
    check(sibling1CleanedUp)

    // =========================================================================
    // 3. `supervisorScope`: 1 CON LỖI -> ANH EM VẪN CHẠY BÌNH THƯỜNG!
    // =========================================================================
    println("\n3. `supervisorScope`: Tac vu phu (Goi y quang cao) loi khong lam hong tac vu chinh (Chi tiet don):")
    supervisorScope {
        val orderDetailDeferred = async {
            delay(80)
            "Don hang O001 - Tong thanh toan: 504.000 VND"
        }

        val adsRecommendationDeferred = async {
            delay(40)
            throw RuntimeException("RecommendationService dang qua tai!")
        }

        val orderDetail = orderDetailDeferred.await()
        val adsResult = runCatching { adsRecommendationDeferred.await() }
            .onFailure { if (it is CancellationException) throw it }
            .getOrDefault("[Khong co goi y san pham luc nay]")

        println("  - Du lieu chinh van thanh cong: $orderDetail")
        println("  - Du lieu phu (Fallback an toan): $adsResult")
        check(orderDetail.contains("O001") && adsResult.startsWith("[Khong co"))
    }

    // =========================================================================
    // 4. COOPERATIVE CANCELLATION VỚI `ensureActive()`
    // =========================================================================
    println("\n4. Huy tac vu tinh toan CPU hop tac voi `ensureActive()`:")
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
    println("  -> Da huy cpuJob an toan nho `ensureActive()` (isCancelled = ${cpuJob.isCancelled})")
    check(cpuJob.isCancelled)

    println("\n--- ___ * END EX03 * ___ ---")
}
