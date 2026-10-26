package view

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

/**
 * BÀI 01: OS THREAD VS KOTLIN COROUTINE & BẢN CHẤT CỦA `suspend fun`
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK VS HEAP / CPS & STATE MACHINE):
 *    - **OS Thread (`java.lang.Thread` trong `D:\Java_Journey\lesson14-thread`)**:
 *      + Mỗi Platform Thread trong JVM ánh xạ 1-1 với một Kernel Thread của Hệ điều hành (OS).
 *      + Mỗi Thread chiếm ~1MB vùng nhớ Stack riêng biệt và việc chuyển ngữ cảnh (Context Switch)
 *        giữa các Thread phải đi qua nhân OS (Kernel space) rất tốn kém CPU.
 *      + Khi bạn gọi `Thread.sleep(100)` hoặc chờ I/O đồng bộ, toàn bộ OS Thread đó bị **BLOCK (phong tỏa)**:
 *        nó nằm im giữ nguyên 1MB Stack mà không thể làm việc khác! Tạo 100.000 OS Thread dễ dàng gây `OutOfMemoryError`.
 *
 *    - **Kotlin Coroutine ("Lightweight Thread" ở tầng User-space)**:
 *      + Coroutine KHÔNG gắn chặt vào một OS Thread cố định. Hàng chục nghìn Coroutine có thể chạy
 *        luân phiên trên một nhóm nhỏ (Thread Pool) chỉ gồm vài OS Thread!
 *      + Một Coroutine thực chất chỉ là một đối tượng nhỏ trên **Heap** (vài chục đến vài trăm bytes) lưu trạng thái.
 *      + Khi gặp lệnh `delay(100)` (hoặc chờ kết quả DB/HTTP non-blocking), Coroutine **TẠM DỪNG (Suspend)**:
 *        nó lưu lại vị trí hiện tại vào đối tượng `Continuation` trên Heap rồi **TRẢ LẠI OS THREAD**
 *        để OS Thread đó đi chạy Coroutine khác ngay lập tức! Khi hết 100ms, một OS Thread trong pool
 *        sẽ tiếp tục (Resume) thực thi phần còn lại của Coroutine.
 *
 *    - **Điều gì xảy ra khi biên dịch `suspend fun` ra JVM Bytecode? (CPS - Continuation Passing Style)**:
 *      Khi bạn viết:
 *      ```kotlin
 *      suspend fun fetchUserName(id: String): String
 *      ```
 *      Trình biên dịch Kotlin biến đổi chữ ký hàm trong Bytecode thành:
 *      ```java
 *      public static final Object fetchUserName(String id, Continuation<? super String> $completion)
 *      ```
 *      Bên trong thân hàm, Kotlin sinh ra một **State Machine (Máy trạng thái `switch (label)`)**:
 *      - `label = 0`: Chạy đoạn code trước điểm `delay()` đầu tiên -> Lưu biến cục bộ vào `$completion`,
 *        tăng `label = 1` và trả về cờ `COROUTINE_SUSPENDED` để giải phóng OS Thread!
 *      - `label = 1`: Khi `delay()` xong, hàm được gọi lại với `$completion`, nhảy thẳng tới `case 1:`
 *        để chạy tiếp đoạn code phía sau!
 *
 * 2. SO SÁNH JAVA VS KOTLIN:
 *    - Java (`lesson14-thread`): Dùng `Thread`, `ExecutorService`, `CompletableFuture.supplyAsync(...).thenCompose(...)`
 *      (dễ rơi vào callback hell hoặc khó debug stacktrace) hoặc Virtual Threads (Project Loom từ Java 21+).
 *    - Kotlin: Viết code bất đồng bộ với `suspend fun` trông **tuần tự, dễ đọc y hệt code đồng bộ**,
 *      dùng `try-catch` và vòng lặp `for` bình thường, nhưng bên dưới hoàn toàn Non-blocking!
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong Spring Boot 3+ (đặc biệt là Spring WebFlux hoặc Spring MVC hỗ trợ Coroutines), bạn có thể viết
 *      trực tiếp từ khóa `suspend` trên метод của `@RestController` và `CoroutineCrudRepository`:
 *      ```kotlin
 *      @GetMapping("/users/{id}")
 *      suspend fun getUser(@PathVariable id: String): UserResponse = userService.findById(id)
 *      ```
 *      Spring tự động chuyển đổi sang Reactive `Mono<UserResponse>` bên dưới mà bạn không cần học Reactor API phức tạp!
 */

// Một `suspend fun` điển hình: Có thể tạm dừng ở các điểm gọi suspend khác (`delay`) mà không khóa OS Thread
suspend fun simulateNonBlockingIoTask(taskId: Int): String {
    val beforeThread = Thread.currentThread().name
    delay(80) // Tạm dừng Coroutine 80ms (NON-BLOCKING: Giải phóng OS Thread!)
    val afterThread = Thread.currentThread().name
    return "Task #$taskId hoàn tất (Trước delay: [$beforeThread] -> Sau delay: [$afterThread])"
}

fun main() = runBlocking {
    println("--- ___ * START EX01: OS THREAD VS KOTLIN COROUTINE * ___ ---\n")

    // =========================================================================
    // 1. MINH HỌA `suspend fun` VÀ CONTINUATION STATE MACHINE
    // =========================================================================
    println("1. Thực thi `suspend fun` (Tạm dừng không khóa Thread - Non-blocking):")
    val result = simulateNonBlockingIoTask(1)
    println("  - Kết quả: $result")

    // =========================================================================
    // 2. SO SÁNH TỐC ĐỘ & BỘ NHỚ: CHẠY ĐỒNG THỜI 1.000 TÁC VỤ CHỜ 50ms
    // =========================================================================
    println("\n2. Chạy đồng thời 1.000 Coroutines (mỗi Coroutine chờ `delay(50ms)`):")
    var completedCount = 0
    val coroutineElapsedMs = measureTimeMillis {
        // Khởi chạy 1.000 Coroutine con chạy song song ngay trên cùng 1 Thread của runBlocking!
        // Nếu dùng Thread.sleep(50) tuần tự trên 1 Thread thì sẽ mất 1000 * 50ms = 50.000ms (50 giây)!
        // Nhưng với `delay(50)` (Non-blocking), cả 1.000 Coroutine cùng bắt đầu chờ và hoàn thành chỉ trong ~50-100ms!
        val jobs = List(1_000) { index ->
            launch {
                delay(50)
                completedCount++
                if (index == 0 || index == 999) {
                    println("  - Coroutine #$index chạy trên Thread: [${Thread.currentThread().name}]")
                }
            }
        }
        // Đợi tất cả 1.000 Coroutine hoàn thành
        jobs.forEach { it.join() }
    }
    println("  -> Đã hoàn thành $completedCount / 1.000 Coroutines chỉ trong: ${coroutineElapsedMs}ms (thay vì 50.000ms!)")

    // =========================================================================
    // 3. ĐỐI CHIẾU VỚI `Thread.sleep()` (BLOCKING OS THREAD)
    // =========================================================================
    println("\n3. Đối chiếu với `Thread.sleep(50)` (Blocking OS Thread tuần tự):")
    val blockingElapsedMs = measureTimeMillis {
        repeat(3) { idx ->
            // Thread.sleep khóa cứng OS Thread hiện tại, không ai khác dùng được Thread này trong lúc chờ!
            Thread.sleep(50)
            println("  - [Blocking] Lần ngủ thứ ${idx + 1} trên Thread [${Thread.currentThread().name}]")
        }
    }
    println("  -> Chỉ riêng 3 lần `Thread.sleep(50)` tuần tự đã tốn: ${blockingElapsedMs}ms")

    println("\n--- ___ * END EX01 * ___ ---")
}
