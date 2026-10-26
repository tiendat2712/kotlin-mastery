package view

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

/**
 * BÀI 01: OS THREAD VS KOTLIN COROUTINE, CONCURRENCY VS PARALLELISM & BẢN CHẤT CỦA `suspend fun`
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK VS HEAP / CPS & STATE MACHINE):
 *    - **OS Thread (`java.lang.Thread` trong `D:\Java_Journey\lesson14-thread`)**:
 *      + Mỗi Platform Thread trong JVM ánh xạ 1-1 với một Kernel Thread của Hệ điều hành (OS).
 *      + Một OS Thread thường được cấp phát vùng nhớ Stack riêng (mặc định thường khoảng ~1MB tùy hệ điều hành,
 *        kiến trúc JVM 64-bit và tham số cấu hình `-Xss`), và việc chuyển ngữ cảnh (Context Switch)
 *        giữa các OS Thread phải đi qua nhân OS (Kernel space).
 *      + Khi bạn gọi `Thread.sleep(100)` hoặc chờ I/O đồng bộ, toàn bộ OS Thread đó bị **BLOCK (phong tỏa)**:
 *        nó đứng yên giữ nguyên vùng nhớ Stack mà không thể xử lý tác vụ khác trong lúc chờ.
 *
 *    - **Kotlin Coroutine ("Lightweight Thread" ở tầng User-space)**:
 *      + Coroutine KHÔNG gắn chặt vào một OS Thread cố định. Hàng nghìn Coroutine có thể chạy
 *        luân phiên trên một nhóm nhỏ (Thread Pool) chỉ gồm vài OS Thread.
 *      + Trạng thái của một Coroutine được lưu trong các đối tượng `Continuation` trên **Heap** (dung lượng
 *        thường chỉ vài chục đến vài trăm bytes tùy số lượng biến cục bộ và độ sâu chuỗi gọi hàm).
 *      + Khi gặp điểm tạm dừng thực sự như `delay(100)` (hoặc chờ kết quả DB/HTTP non-blocking), Coroutine **TẠM DỪNG (Suspend)**:
 *        nó lưu lại vị trí hiện tại vào đối tượng `Continuation` trên Heap rồi **TRẢ LẠI OS THREAD**
 *        để OS Thread đó đi chạy Coroutine khác! Khi hết 100ms, một OS Thread trong Dispatcher
 *        sẽ tiếp tục (Resume) thực thi phần còn lại của Coroutine.
 *
 *    - **PHÂN BIỆT RÕ CONCURRENCY (ĐỒNG THỜI) VS PARALLELISM (SONG SONG)**:
 *      + **Concurrency (Đồng thời)**: Nhiều tác vụ cùng được khởi phát và có vòng đời chồng lấp nhau theo thời gian
 *        nhờ đan xen (interleaving) khi một tác vụ tạm dừng chờ I/O. Ngay cả khi chỉ có **1 OS Thread duy nhất**
 *        (như `runBlocking`), 1.000 Coroutine vẫn đạt được **Concurrency** (cùng chờ `delay(50)` một lúc)!
 *      + **Parallelism (Song song vật lý)**: Nhiều tác vụ thực sự chạy tại cùng một thời điểm vật lý trên
 *        **nhiều nhân CPU (Multi-core)** thông qua các Dispatcher đa luồng như `Dispatchers.Default`.
 *
 *    - **HIỂU ĐÚNG VỀ TỪ KHÓA `suspend` (TRÁNH NGỘ NHẬN PHỔ BIẾN!)**:
 *      + **Ngộ nhận**: *"Cứ thêm từ khóa `suspend` vào trước hàm là hàm đó tự động chạy bất đồng bộ (non-blocking)
 *        hoặc tự động chuyển sang chạy trên Background Thread!"* -> **SAI HOÀN TOÀN!**
 *      + **Sự thật**: `suspend` chỉ là một **khả năng ở tầng biên dịch (Compile-time capability)**:
 *        Trình biên dịch biến đổi chữ ký hàm theo chuẩn **CPS (Continuation Passing Style)**, thêm tham số ẩn
 *        `Continuation<? super T> $completion` và tạo **State Machine (`switch (label)`)** để hàm *có quyền*
 *        gọi các điểm tạm dừng (`delay`, `withContext`, non-blocking I/O).
 *      + Nếu bên trong một `suspend fun`, bạn KHÔNG gọi điểm tạm dừng nào hoặc bạn gọi hàm khóa luồng như `Thread.sleep(50)`,
 *        thì hàm đó vẫn chạy **trên chính Thread của người gọi (`main`)** và **VẪN KHÓA CỨNG (BLOCK) Thread đó**!
 *
 * 2. SO SÁNH JAVA VS KOTLIN:
 *    - Java (`lesson14-thread`): Dùng `Thread`, `ExecutorService`, `CompletableFuture` hoặc Virtual Threads (Java 21+).
 *    - Kotlin: Viết code bất đồng bộ với `suspend fun` theo cấu trúc tuần tự dễ đọc, dùng `try-catch` và vòng lặp `for`
 *      bình thường, kết hợp chặt chẽ với Structured Concurrency.
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong Spring WebFlux / Reactive Coroutines, bạn có thể viết trực tiếp `suspend fun` trên `@RestController`
 *      khi làm việc với các thư viện Non-blocking (WebClient, R2DBC `CoroutineCrudRepository`).
 */

// 1. Một `suspend fun` đúng chuẩn Non-blocking: Tạm dừng tại `delay` và trả lại OS Thread
suspend fun simulateNonBlockingIoTask(taskId: Int): String {
    val beforeThread = Thread.currentThread().name
    delay(60) // Điểm tạm dừng (Suspension point): Giải phóng OS Thread hiện tại!
    val afterThread = Thread.currentThread().name
    return "Task #$taskId hoan tat (Truoc delay: [$beforeThread] -> Sau delay: [$afterThread])"
}

// 2. Minh họa phản ví dụ (Anti-pattern): Hàm có từ khóa `suspend` nhưng gọi `Thread.sleep()` bên trong!
// Từ khóa `suspend` KHÔNG tự động đẩy hàm này sang Background Thread và KHÔNG biến `Thread.sleep` thành non-blocking!
suspend fun badBlockingSuspendFunction(taskId: Int): String {
    val currentThread = Thread.currentThread().name
    Thread.sleep(50) // VẪN KHÓA CỨNG (BLOCK) OS Thread đang gọi!
    return "BadBlockingTask #$taskId chay tren [$currentThread] (da khoa cung Thread nay 50ms!)"
}

fun main() = runBlocking {
    println("--- ___ * START EX01: OS THREAD VS KOTLIN COROUTINE * ___ ---\n")

    // =========================================================================
    // 1. MINH HỌA `suspend fun` VÀ CONTINUATION STATE MACHINE
    // =========================================================================
    println("1. Thuc thi `suspend fun` voi diem tam dung `delay` (Non-blocking):")
    val result = simulateNonBlockingIoTask(1)
    println("  - Ket qua: $result")

    // =========================================================================
    // 2. CHỨNG MINH: `suspend` KHÔNG TỰ ĐỘNG CHUYỂN THREAD HAY CHỐNG BLOCKING!
    // =========================================================================
    println("\n2. Chung minh: `suspend fun` goi `Thread.sleep(50)` VAN KHOA CUNG Thread goi:")
    val callerThreadName = Thread.currentThread().name
    val badBlockingMs = measureTimeMillis {
        // Dù chạy trong 2 `launch` đồng thời trên cùng 1 Thread của runBlocking,
        // vì `badBlockingSuspendFunction` dùng `Thread.sleep(50)` (blocking, không nhường thread),
        // chúng phải đợi nhau tuần tự ~100ms thay vì ~50ms!
        val job1 = launch { println("  - " + badBlockingSuspendFunction(1)) }
        val job2 = launch { println("  - " + badBlockingSuspendFunction(2)) }
        job1.join()
        job2.join()
    }
    println("  -> Caller thread la [$callerThreadName]; 2 ham `badBlockingSuspendFunction(50ms)` mat tong cong: ${badBlockingMs}ms (>= 100ms vi bi block!)")
    check(badBlockingMs >= 90)

    // =========================================================================
    // 3. CONCURRENCY (ĐỒNG THỜI) TRÊN 1 THREAD: 1.000 COROUTINES CÙNG CHỜ `delay(50ms)`
    // =========================================================================
    println("\n3. Chay dong thoi (Concurrency) 1.000 Coroutines tren cung 1 Thread voi `delay(50ms)`:")
    var completedCount = 0
    val coroutineElapsedMs = measureTimeMillis {
        // Chú ý thuật ngữ: Đây là CONCURRENCY (đồng thời đan xen khi chờ I/O trên cùng 1 Thread của runBlocking),
        // KHÔNG PHẢI Parallelism (song song vật lý trên nhiều nhân CPU)!
        // Khi Coroutine #0 gặp `delay(50)`, nó tạm dừng và nhường ngay Thread `main` cho Coroutine #1, #2...
        // Nhờ đó cả 1.000 Coroutine cùng bắt đầu đếm ngược 50ms và hoàn thành chỉ trong khoảng ~50-150ms!
        val jobs = List(1_000) { index ->
            launch {
                delay(50)
                completedCount++
                if (index == 0 || index == 999) {
                    println("  - Coroutine #$index chay tren Thread: [${Thread.currentThread().name}]")
                }
            }
        }
        jobs.forEach { it.join() }
    }
    println("  -> Da hoan thanh $completedCount / 1.000 Coroutines chi trong: ${coroutineElapsedMs}ms (nho Concurrency & Suspension!)")
    check(completedCount == 1_000)

    // =========================================================================
    // 4. ĐỐI CHIẾU VỚI `Thread.sleep()` TUẦN TỰ
    // =========================================================================
    println("\n4. Doi chieu voi 3 lan `Thread.sleep(50)` tuan tu:")
    val blockingElapsedMs = measureTimeMillis {
        repeat(3) { idx ->
            Thread.sleep(50)
            println("  - [Blocking] Lan ngu thu ${idx + 1} tren Thread [${Thread.currentThread().name}]")
        }
    }
    println("  -> Chi rieng 3 lan `Thread.sleep(50)` tuan tu da ton: ${blockingElapsedMs}ms")

    println("\n--- ___ * END EX01 * ___ ---")
}
