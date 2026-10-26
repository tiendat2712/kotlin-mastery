package view

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

/**
 * BÀI 02: CÁC COROUTINE BUILDERS CỐT LÕI (`runBlocking`, `launch`, `async / await`)
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Một `suspend fun` chỉ có thể được gọi từ một `suspend fun` khác hoặc từ một **Coroutine Builder**.
 *    - **Ba Coroutine Builders quan trọng nhất**:
 *      (1) `runBlocking { ... }`:
 *          - Là cây cầu nối từ thế giới Blocking (như `fun main()` hoặc hàm `@Test` JUnit) sang thế giới Suspending.
 *          - Nó khóa (block) OS Thread đang gọi nó cho đến khi toàn bộ các Coroutine con bên trong hoàn tất.
 *          - **TUYỆT ĐỐI KHÔNG dùng `runBlocking` bên trong code nghiệp vụ của Spring Controller/Service**
 *            vì nó sẽ khóa mất Thread của Server!
 *      (2) `launch { ... } -> Job`:
 *          - Khởi chạy một Coroutine mới theo kiểu **"Fire-and-Forget"** (Bắn và quên — không trả về kết quả dữ liệu,
 *            tương tự `Runnable` / `CompletableFuture<Void>` trong Java).
 *          - Trả về đối tượng `Job` để quản lý vòng đời: `job.join()` (chờ xong), `job.cancel()` (hủy bỏ).
 *      (3) `async { ... } -> Deferred<T>`:
 *          - Khởi chạy một Coroutine mới để **tính toán và trả về một giá trị kiểu `T`**
 *            (tương tự `Callable<T>` / `CompletableFuture<T>` trong Java).
 *          - Trả về `Deferred<T>` (là một lớp con của `Job` có thêm hàm `suspend fun await(): T`).
 *          - Khi gọi `deferred.await()`, Coroutine gọi sẽ tạm dừng (suspend, KHÔNG khóa OS Thread!)
 *            cho đến khi kết quả sẵn sàng.
 *
 * 2. SO SÁNH JAVA VS KOTLIN:
 *    - Java (`CompletableFuture`):
 *      ```java
 *      var profileFuture = CompletableFuture.supplyAsync(() -> fetchProfile(id));
 *      var ordersFuture  = CompletableFuture.supplyAsync(() -> fetchOrders(id));
 *      CompletableFuture.allOf(profileFuture, ordersFuture).join();
 *      return new Dashboard(profileFuture.join(), ordersFuture.join());
 *      ```
 *    - Kotlin (`async` + `await` trong `coroutineScope`):
 *      ```kotlin
 *      coroutineScope {
 *          val profileDeferred = async { fetchProfile(id) }
 *          val ordersDeferred  = async { fetchOrders(id) }
 *          Dashboard(profileDeferred.await(), ordersDeferred.await())
 *      }
 *      ```
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong kiến trúc Microservices / BFF (Backend For Frontend) với Spring Boot Kotlin:
 *      Khi một API `/api/v1/customers/{id}/dashboard` cần gọi đồng thời 2 microservice độc lập
 *      (`CustomerService` mất 150ms và `OrderService` mất 150ms), việc bọc trong `coroutineScope` với
 *      2 lệnh `async` sẽ giúp giảm thời gian phản hồi API từ **~300ms xuống chỉ còn ~150ms**!
 */

data class CustomerProfileDto(val customerId: String, val fullName: String, val tier: String)
data class OrderSummaryDto(val orderId: String, val payableAmount: Long)
data class CustomerDashboardView(val profile: CustomerProfileDto, val recentOrders: List<OrderSummaryDto>)

// Giả lập gọi Microservice 1: Tốn 150ms I/O
suspend fun fetchCustomerProfile(customerId: String): CustomerProfileDto {
    delay(150)
    return CustomerProfileDto(customerId, "Nguyễn Minh An", "GOLD")
}

// Giả lập gọi Microservice 2: Tốn 150ms I/O
suspend fun fetchOrderHistory(customerId: String): List<OrderSummaryDto> {
    delay(150)
    return listOf(
        OrderSummaryDto("O001", 504_000L),
        OrderSummaryDto("O007", 945_000L)
    )
}

// Cách 1: Gọi TUẦN TỰ (Sequential) -> Tổng thời gian ~ 150ms + 150ms = ~300ms
suspend fun loadDashboardSequentially(customerId: String): CustomerDashboardView {
    val profile = fetchCustomerProfile(customerId) // Đợi xong 150ms...
    val orders = fetchOrderHistory(customerId)     // ...mới bắt đầu gọi tiếp 150ms!
    return CustomerDashboardView(profile, orders)
}

// Cách 2: Gọi SONG SONG (Concurrent Decomposition với `async` + `await`) -> Tổng thời gian ~ max(150ms, 150ms) = ~150ms!
suspend fun loadDashboardConcurrently(customerId: String): CustomerDashboardView = coroutineScope {
    // Khởi chạy cả 2 tác vụ NGAY LẬP TỨC cùng lúc
    val profileDeferred: Deferred<CustomerProfileDto> = async { fetchCustomerProfile(customerId) }
    val ordersDeferred: Deferred<List<OrderSummaryDto>> = async { fetchOrderHistory(customerId) }

    // Thu hoạch kết quả bằng `.await()`
    CustomerDashboardView(
        profile = profileDeferred.await(),
        recentOrders = ordersDeferred.await()
    )
}

fun main() = runBlocking {
    println("--- ___ * START EX02: COROUTINE BUILDERS (LAUNCH & ASYNC/AWAIT) * ___ ---\n")

    // =========================================================================
    // 1. `launch` BUILDER VÀ QUẢN LÝ VÒNG ĐỜI VỚI `Job` (`join`, `cancel`)
    // =========================================================================
    println("1. Sử dụng `launch` (Fire-and-Forget) và điều khiển `Job`:")
    val auditLogJob: Job = launch {
        println("  - [AuditLog Job] Bắt đầu ghi log bất đồng bộ...")
        delay(80)
        println("  - [AuditLog Job] Đã ghi xong Audit Log vào hệ thống!")
    }

    val slowReportJob: Job = launch {
        repeat(5) { step ->
            println("  - [SlowReport Job] Đang tổng hợp bước ${step + 1}/5...")
            delay(60)
        }
    }

    // Chờ auditLogJob chạy xong
    auditLogJob.join()
    // Hủy slowReportJob khi nó mới chạy được một phần
    slowReportJob.cancel()
    println("  -> Đã gọi slowReportJob.cancel()! (isActive=${slowReportJob.isActive}, isCancelled=${slowReportJob.isCancelled})")

    // =========================================================================
    // 2. SO SÁNH GỌI TUẦN TỰ VS GỌI SONG SONG BẰNG `async` & `await()`
    // =========================================================================
    println("\n2. So sánh hiệu năng: Tuần tự (Sequential) vs Song song (`async` + `await`):")

    val sequentialMs = measureTimeMillis {
        val dashboard = loadDashboardSequentially("C001")
        println("  - [Tuần tự] Kết quả: ${dashboard.profile.fullName}, số đơn: ${dashboard.recentOrders.size}")
    }
    println("  -> Thời gian chạy TUẦN TỰ: ${sequentialMs}ms (~300ms)")

    val concurrentMs = measureTimeMillis {
        val dashboard = loadDashboardConcurrently("C001")
        println("  - [Song song async/await] Kết quả: ${dashboard.profile.fullName}, số đơn: ${dashboard.recentOrders.size}")
    }
    println("  -> Thời gian chạy SONG SONG (`async`/`await`): ${concurrentMs}ms (~150ms - Nhanh gấp đôi!)")

    println("\n--- ___ * END EX02 * ___ ---")
}
