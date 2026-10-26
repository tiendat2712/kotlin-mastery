package view

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

/**
 * BÀI 04: `Flow<T>` — LUỒNG DỮ LIỆU BẤT ĐỒNG BỘ LẠNH (COLD ASYNCHRONOUS STREAM)
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE / SO SÁNH 3 CẤP ĐỘ TẬP HỢP):
 *    Hãy đặt 3 công cụ xử lý tập hợp dữ liệu của Kotlin lên bàn cân:
 *    - **`List<T>` (Đồng bộ + Eager - Tức thời)**:
 *      + Toàn bộ phần tử phải được tính toán xong và nằm sẵn trong bộ nhớ Heap trước khi trả về.
 *      + Không thể tạm dừng (`suspend`) giữa chừng.
 *    - **`Sequence<T>` (Đồng bộ + Lazy - Lười biếng, giống Java `Stream<T>`)**:
 *      + Tính toán từng phần tử một khi có Terminal Operator (`toList()`, `sumOf()`), giúp tiết kiệm bộ nhớ.
 *      + NHƯNG `Sequence` vẫn là **Đồng bộ (Synchronous)**: Bên trong `sequence { yield(...) }` bạn KHÔNG ĐƯỢC
 *        gọi bất kỳ hàm `suspend` tự do nào (như `delay()` hay gọi DB bất đồng bộ).
 *    - **`Flow<T>` (Bất đồng bộ + Lazy / Cold Stream)**:
 *      + Một `suspend fun` bình thường chỉ trả về **1 giá trị duy nhất** bất đồng bộ (tương đương `Mono<T>` trong Project Reactor).
 *      + Khi bạn cần trả về **NHIỀU giá trị phát ra theo thời gian** một cách bất đồng bộ (tương đương `Flux<T>` trong Reactor),
 *        bạn dùng `Flow<T>`!
 *      + **Vì sao gọi là Cold Stream (Luồng lạnh)?**
 *        Đoạn code bên trong `flow { ... }` **HOÀN TOÀN KHÔNG CHẠY** khi bạn mới chỉ gọi hàm tạo `Flow` hay gắn
 *        các toán tử trung gian (`.filter`, `.map`, `.onEach`). Nó chỉ bắt đầu chảy khi có một **Terminal Operator**
 *        như `.collect { ... }` hoặc `.toList()` được gọi! Mỗi lần gọi `.collect` sẽ khởi chạy lại luồng từ đầu.
 *
 * 2. SO SÁNH JAVA VS KOTLIN:
 *    - Java Reactive (`Flux<T>` của Project Reactor / RxJava):
 *      Phải học hàng trăm toán tử đặc thù (`flatMap`, `concatMap`, `zipWith`, `subscribeOn`) và không dùng được
 *      cấu trúc điều khiển chuẩn (`for`, `try-catch`) bên trong.
 *    - Kotlin `Flow<T>`:
 *      Bên trong `flow { ... }`, bạn viết vòng lặp `for`, `if/else`, `try-catch`, gọi `delay()` hay `suspend fun`
 *      bình thường, rồi phát từng phần tử ra bằng `emit(value)`!
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong Spring Boot + R2DBC / MongoDB Reactive / Server-Sent Events (SSE) / Streaming AI Responses:
 *      + Repository: `fun findAllByStatus(status: OrderStatus): Flow<OrderEntity>`
 *      + Controller: `@GetMapping("/orders/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE]) fun streamOrders(): Flow<OrderDto>`
 *      Spring tự động chuyển `Flow<OrderDto>` thành luồng Server-Sent Events đẩy từng bản ghi về trình duyệt ngay khi có!
 */

data class StockTick(val symbol: String, val priceVnd: Long, val sequenceNo: Int)

// Hàm tạo một Cold Flow phát ra giá cổ phiếu theo thời gian thực (mỗi 40ms phát 1 bản ghi)
// Lưu ý: Hàm trả về `Flow<T>` KHÔNG CẦN đánh dấu `suspend` vì bản thân việc tạo Flow chưa thực thi gì cả!
fun observeStockPriceStream(symbol: String): Flow<StockTick> = flow {
    println("  -> [Flow Producer] Bắt đầu mở kết nối luồng giá cho mã '$symbol'...")
    val simulatedPrices = listOf(82_500L, 81_900L, 83_200L, 84_000L, 82_800L)

    for ((index, price) in simulatedPrices.withIndex()) {
        delay(40) // Tạm dừng 40ms không khóa OS Thread (mô phỏng chờ gói tin từ sàn chứng khoán)
        val tick = StockTick(symbol, price, sequenceNo = index + 1)
        println("  -> [Flow Producer] Phát (emit) bản tin #${tick.sequenceNo}: ${tick.priceVnd} VNĐ")
        emit(tick) // Đẩy phần tử xuống cho Collector!
    }
    println("  -> [Flow Producer] Đã đóng luồng giá cho mã '$symbol'.")
}

fun main() = runBlocking {
    println("--- ___ * START EX04: KOTLIN FLOW BASICS (COLD ASYNC STREAM) * ___ ---\n")

    // =========================================================================
    // 1. CHỨNG MINH TÍNH CHẤT "COLD STREAM" (LUỒNG LẠNH) CỦA `Flow<T>`
    // =========================================================================
    println("1. Khởi tạo `Flow<T>` và chuỗi toán tử trung gian (`filter`, `map`, `onEach`):")
    val highPriceAlertFlow: Flow<String> = observeStockPriceStream("FPT")
        .filter { tick ->
            // Chỉ quan tâm các bước giá >= 83.000 VNĐ
            tick.priceVnd >= 83_000L
        }
        .onEach { tick ->
            println("    [Intermediate onEach] Phát hiện đỉnh giá vượt ngưỡng tại nhịp #${tick.sequenceNo}")
        }
        .map { tick ->
            "CẢNH BÁO MUA/BÁN [${tick.symbol} - Nhịp #${tick.sequenceNo}]: Giá đạt ${tick.priceVnd} VNĐ"
        }

    println("  * Lưu ý: Cho đến dòng này, `observeStockPriceStream` CHƯA HỀ CHẠY vì chưa gọi `.collect`!\n")

    // =========================================================================
    // 2. KÍCH HOẠT LUỒNG VỚI TERMINAL OPERATOR `.collect { ... }`
    // =========================================================================
    println("2. Bắt đầu thu thập dữ liệu lần 1 với `.collect { ... }`:")
    highPriceAlertFlow.collect { alertMessage ->
        println("    => [Collector nhận được]: $alertMessage")
    }

    // =========================================================================
    // 3. THU GOM LUỒNG THÀNH DANH SÁCH VỚI `.toList()`
    // =========================================================================
    println("\n3. Vì là Cold Flow, gọi `.toList()` sẽ kích hoạt luồng chạy lại độc lập:")
    val collectedAlerts: List<String> = highPriceAlertFlow.toList()
    println("  -> Tổng số cảnh báo thu được qua `.toList()`: ${collectedAlerts.size} bản tin")

    println("\n--- ___ * END EX04 * ___ ---")
}
