package view

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

/**
 * BÀI 06: FLOW NÂNG CAO (`flowOn`, `buffer`, `catch`, `onCompletion`) & HOT FLOWS (`StateFlow`, `SharedFlow`)
 *
 * [PHẦN MỞ RỘNG FLOW — Dành cho giai đoạn cần làm việc với Reactive / Streaming / Spring WebFlux;
 * KHÔNG phải điều kiện tiên quyết để bắt đầu học Spring MVC + JPA truyền thống]
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / CONTEXT PRESERVATION / BACKPRESSURE / HOT VS COLD):
 *    - **Bảo toàn ngữ cảnh (Context Preservation) & Toán tử `.flowOn(Dispatcher)`**:
 *      + Mặc định, khối `flow { ... }` chạy trên chính `CoroutineContext` của nơi gọi `.collect { ... }`.
 *      + **Quy tắc nghiêm ngặt của Flow**: Bạn **KHÔNG ĐƯỢC** đổi Dispatcher bên trong `flow { ... }` bằng
 *        `withContext(Dispatchers.Default) { emit(x) }` (Flow sẽ ném `IllegalStateException` ngay lập tức để đảm bảo
 *        Collector luôn biết phần tử được phát ra trên luồng nào).
 *      + Muốn đổi Dispatcher cho phần **Upstream** (các toán tử đứng TRƯỚC nó), hãy dùng `.flowOn(Dispatchers.Default)`!
 *
 *    - **Điều tiết áp lực (Backpressure) và Tăng tốc với `.buffer()`**:
 *      + Mặc định, `flow` chạy tuần tự giữa Producer và Collector: Producer `emit(1)` -> chờ Collector xử lý xong `1` ->
 *        Producer mới tính toán tiếp `emit(2)`.
 *      + Khi gắn `.buffer()`, Kotlin tạo một Coroutine riêng cho Producer đẩy dữ liệu vào kênh đệm (`Channel`),
 *        giúp Producer tiếp tục tính toán phần tử tiếp theo ngay trong lúc Collector đang bận xử lý phần tử trước!
 *
 *    - **Vòng đời (`.onCompletion`) & Bắt lỗi khai báo (`.catch`) — Tính trong suốt ngoại lệ (Exception Transparency)**:
 *      + `.onCompletion { cause -> ... }`: Luôn chạy khi luồng kết thúc (tương tự `finally`). Nếu `cause == null` là
 *        hoàn thành bình thường, khác `null` là kết thúc do lỗi hoặc bị hủy.
 *      + **Phạm vi của `.catch { e -> ... }`**:
 *        * `.catch` **CHỈ bắt lỗi phát sinh từ Upstream** (tức khối `flow { ... }` và các toán tử `.map`, `.filter`
 *          nằm **phía trên** `.catch`).
 *        * `.catch` **KHÔNG bắt lỗi ném ra bên trong `.collect { ... }`** (Downstream)!
 *        * Theo tài liệu chính thức của `Flow.catch`, toán tử này **không bắt các ngoại lệ dùng để hủy luồng (exceptions
 *          that occur to cancel the flow)** — ví dụ khi phía downstream/collector hủy luồng hoặc ném ngoại lệ làm dừng
 *          việc thu thập, ngoại lệ hủy đó sẽ được ném đi tiếp (rethrow) để bảo toàn cơ chế hủy của Coroutine.
 *
 *    - **Phân biệt Cold `Flow` vs Hot `StateFlow` / `SharedFlow`**:
 *      + **Cold `Flow`**: Lười biếng (Lazy), chỉ chạy khi có `.collect`, mỗi Collector nhận một luồng riêng biệt từ đầu (Unicast).
 *      + **Hot `StateFlow<T>`**: Luôn giữ **1 giá trị trạng thái hiện tại (`.value`)**. Khi một Collector mới đăng ký,
 *        nó nhận ngay lập tức giá trị `.value` mới nhất. Rất hữu ích để giữ trạng thái cấu hình/cache/UI state.
 *      + **Hot `SharedFlow<T>`**: Luồng phát sóng sự kiện cho nhiều người nghe cùng lúc (**Multicast Event Bus**),
 *        không bắt buộc phải có giá trị khởi tạo ban đầu.
 */

// 1. Flow tính toán nặng ở Upstream được chuyển sang `Dispatchers.Default` bằng `.flowOn`
fun computeHeavyMetricsFlow(): Flow<String> = flow {
    for (id in 1..2) {
        val producerThread = Thread.currentThread().name
        emit("Metric#$id(producedOn=$producerThread)")
    }
}.flowOn(Dispatchers.Default) // Chỉ tác động lên phần Upstream phía trên `.flowOn`!

// 2. Flow mô phỏng lỗi ở nhịp thứ 3 để kiểm tra `.catch` và `.onCompletion`
fun paymentStatusStream(): Flow<String> = flow {
    emit("STEP_1: Kiem tra han muc the -> OK")
    emit("STEP_2: Khoa tam thoi so tien -> OK")
    throw IllegalStateException("Cong thanh toan quoc te mat ket noi tai buoc 3!")
}

// 3. Ví dụ quản lý trạng thái cấu hình hệ thống với Hot `StateFlow`
class SystemConfigRegistry {
    private val _maintenanceMode = MutableStateFlow(false)
    val maintenanceMode: StateFlow<Boolean> = _maintenanceMode.asStateFlow()

    fun updateMaintenanceMode(enabled: Boolean) {
        _maintenanceMode.value = enabled
    }
}

fun main() = runBlocking {
    println("--- ___ * START EX06: ADVANCED FLOW & HOT FLOWS (STATEFLOW / SHAREDFLOW) * ___ ---\n")

    // =========================================================================
    // 1. CONTEXT PRESERVATION & `.flowOn(Dispatchers.Default)`
    // =========================================================================
    println("1. Doi Dispatcher cho Upstream bang `.flowOn(Dispatchers.Default)`:")
    val metrics = mutableListOf<String>()
    computeHeavyMetricsFlow().collect { item ->
        val collectorThread = Thread.currentThread().name
        println("  - [Collector tren $collectorThread] Nhan duoc: $item")
        metrics.add(item)
    }
    check(metrics.size == 2)

    // =========================================================================
    // 2. SO SÁNH TỐC ĐỘ KHI CÓ VÀ KHÔNG CÓ `.buffer()`
    // =========================================================================
    println("\n2. Tang toc luong xu ly bat dong bo voi `.buffer()`:")
    val slowProducerFlow: Flow<Int> = flow {
        for (i in 1..3) {
            delay(30) // Producer mất 30ms để tạo 1 phần tử
            emit(i)
        }
    }

    // Không có `.buffer()`: Producer và Collector chạy tuần tự trong cùng 1 Coroutine ->
    // Mỗi phần tử mất (30ms chờ emit + 50ms chờ collect) = ~80ms -> 3 phần tử mất khoảng ~240ms.
    val withoutBufferMs = measureTimeMillis {
        slowProducerFlow.collect { delay(50) }
    }

    // Có `.buffer()`: Tách Producer sang 1 Coroutine riêng gửi qua Channel.
    // Lưu ý: Vì đang chạy trên `runBlocking` đơn luồng và chờ bằng `delay`, hiệu quả giảm thời gian ở đây
    // đến từ việc chạy ĐỒNG THỜI (Concurrency — đan xen thời gian chờ giữa Producer và Collector trên cùng 1 thread),
    // KHÔNG phải chạy song song vật lý (Parallelism) trên nhiều nhân CPU! (Số đo ms thực tế là minh họa, phụ thuộc OS scheduler).
    val withBufferMs = measureTimeMillis {
        slowProducerFlow.buffer().collect { delay(50) }
    }
    println("  - Thoi gian khong dung `.buffer()` (minh hoa): ${withoutBufferMs}ms")
    println("  - Thoi gian co dung `.buffer()`    (minh hoa): ${withBufferMs}ms (Nhanh hon nho Producer & Collector chay dong thoi [concurrently], dan xen thoi gian cho!)")

    // =========================================================================
    // 3. XỬ LÝ LỖI UPSTREAM VỚI `.catch` VÀ THEO DÕI VÒNG ĐỜI VỚI `.onCompletion`
    // =========================================================================
    println("\n3. Xu ly loi khai bao voi `.catch` va `.onCompletion`:")
    val collectedSteps = mutableListOf<String>()
    paymentStatusStream()
        .onCompletion { cause ->
            if (cause != null) {
                println("  - [onCompletion - Upstream] Phat hien luong ket thuc do loi: ${cause.message}")
            } else {
                println("  - [onCompletion] Luong hoan tat binh thuong.")
            }
        }
        .catch { ex ->
            // Bắt lỗi từ Upstream và phát (emit) một giá trị dự phòng (Fallback) xuống cho Collector!
            println("  - [catch Operator] Da bat loi Upstream (${ex::class.simpleName}) -> Phat ban tin Fallback!")
            emit("STEP_3_FALLBACK: Chuyen sang che do doi soat thu cong (Offline Queue)")
        }
        .collect { stepMsg ->
            println("    => [Collector nhan]: $stepMsg")
            collectedSteps.add(stepMsg)
        }
    check(collectedSteps.size == 3 && collectedSteps.last().startsWith("STEP_3_FALLBACK"))

    // =========================================================================
    // 4. HOT FLOWS: `StateFlow` (GIỮ TRẠNG THÁI MỚI NHẤT) & `SharedFlow` (EVENT BUS)
    // =========================================================================
    println("\n4. Phan biet Hot `StateFlow` (State Holder) va `SharedFlow` (Event Broadcast):")

    // 4a. StateFlow luôn có `.value` hiện tại và phát ngay giá trị mới nhất cho người đăng ký
    val configRegistry = SystemConfigRegistry()
    println("  - [StateFlow] Gia tri `.value` ban dau cua maintenanceMode: ${configRegistry.maintenanceMode.value}")
    configRegistry.updateMaintenanceMode(true)
    println("  - [StateFlow] Gia tri `.value` sau khi cap nhat          : ${configRegistry.maintenanceMode.value}")
    check(configRegistry.maintenanceMode.value)

    // 4b. SharedFlow phát sự kiện đồng thời cho nhiều subscriber đang lắng nghe
    val orderEventBus = MutableSharedFlow<String>(replay = 1)
    orderEventBus.emit("EVENT_ORDER_CREATED_O001")
    val receivedBySubscriber = mutableListOf<String>()
    val subJob = launch {
        orderEventBus.collect { event ->
            receivedBySubscriber.add(event)
        }
    }
    delay(30)
    subJob.cancel()
    subJob.join()
    println("  - [SharedFlow replay=1] Subscriber moi vao van nhan duoc su kien gan nhat: $receivedBySubscriber")
    check(receivedBySubscriber == listOf("EVENT_ORDER_CREATED_O001"))

    // =========================================================================
    // 5. GÓC TỰ LUYỆN (ACTIVE LEARNING)
    // =========================================================================
    println("\n5. Goc Tu Luyen (Active Learning):")

    // --- 5a. Dự đoán kết quả (Predict Output) ---
    // [ĐỀ BÀI 5A]: Cho luồng phát 1, 2, 3. Trong `.map`, nếu gặp số 2 thì ném `IllegalArgumentException`,
    // sau đó gắn `.catch { emit(-99) }`. Hỏi `.toList()` thu được danh sách gồm những số nào?
    val quiz5aList: List<Int> = flow {
        emit(1)
        emit(2)
        emit(3) // Không bao giờ chạy tới vì số 2 đã ném lỗi làm dừng luồng!
    }.map { num ->
        require(num != 2) { "Khong chap nhan so 2" }
        num * 10
    }.catch {
        emit(-99)
    }.toList()
    println("  - [5a - Predict Output] Ket qua danh sach sau khi gap loi o phan tu thu 2 va fallback: $quiz5aList")
    check(quiz5aList == listOf(10, -99))

    // --- 5b. Tìm lỗi sai (Find the Bug) ---
    // [ĐỀ BÀI 5B]: Vì sao đoạn code dùng `withContext(Dispatchers.Default)` trực tiếp bên trong `flow { ... }`
    // lại bị cấm và ném `IllegalStateException`? (Cách sửa đúng là dùng `.flowOn(Dispatchers.Default)` ở ngoài).
    var contextViolationCaught = false
    val brokenFlow = flow {
        withContext(Dispatchers.Default) {
            emit("SAI_QUY_TAC_CONTEXT_PRESERVATION")
        }
    }
    try {
        brokenFlow.collect {}
    } catch (e: IllegalStateException) {
        contextViolationCaught = true
        println("  - [5b - Find the Bug] Bat dung IllegalStateException do doi withContext ben trong flow {...}!")
    }
    check(contextViolationCaught)

    // --- 5c. Bài tập nhỏ (Mini Exercise) ---
    // [ĐỀ BÀI 5C]: Viết hàm `safeTemperatureStream(readings: List<Int>): Flow<String>`
    // chuyển từng nhiệt độ `t` thành `"TEMP=${t}C"`, nhưng nếu gặp `t > 100` (nhiệt độ lỗi cảm biến)
    // thì ném `IllegalStateException("OVERHEAT")` và dùng `.catch` để phát ra `"SENSOR_FALLBACK_SAFE_MODE"`.
    val sensorOutput = safeTemperatureStream(listOf(25, 30, 150, 28)).toList()
    println("  - [5c - Mini Exercise] Ket qua luong cam bien nhiet do: $sensorOutput")
    check(sensorOutput == listOf("TEMP=25C", "TEMP=30C", "SENSOR_FALLBACK_SAFE_MODE"))
    println("  => [PASS] Goc Tu Luyen Ex06 hoan tat!")

    println("\n--- ___ * END EX06 * ___ ---")
}

// [LỜI GIẢI THAM KHẢO CHO MINI EXERCISE 5C]:
fun safeTemperatureStream(readings: List<Int>): Flow<String> = flow {
    for (temp in readings) {
        if (temp > 100) {
            throw IllegalStateException("OVERHEAT: $temp")
        }
        emit("TEMP=${temp}C")
    }
}.catch {
    emit("SENSOR_FALLBACK_SAFE_MODE")
}
