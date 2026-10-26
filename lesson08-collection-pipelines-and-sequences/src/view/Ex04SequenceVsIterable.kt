package view

import bean.SampleRepository

/**
 * ============================================================================
 * BÀI 04: `Iterable` (EAGER) vs `Sequence` (LAZY - TƯƠNG ĐƯƠNG JAVA `Stream<T>`)
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ----------------------------------------------------------------------------
 * Đây là câu hỏi phỏng vấn nâng cao phân loại Senior Kotlin Developer:
 * "Sự khác biệt giữa việc gọi `.filter { ... }.map { ... }.first()` trực tiếp trên `List`
 *  so với việc gọi `.asSequence().filter { ... }.map { ... }.first()` là gì?"
 *
 * A. `Iterable` / `List` (EAGER EVALUATION - Xử lý theo chiều NGANG / Horizontal):
 *    - Bước 1: `list.filter { ... }` duyệt qua **TOÀN BỘ N phần tử** của `list`, và cấp phát
 *      một **`ArrayList` trung gian thứ nhất** trên HEAP chứa các phần tử thỏa mãn.
 *    - Bước 2: `.map { ... }` lại duyệt qua toàn bộ `ArrayList` trung gian thứ nhất đó, và
 *      cấp phát tiếp một **`ArrayList` trung gian thứ hai** trên HEAP!
 *    - Bước 3: `.first()` mới lấy phần tử đầu tiên của `ArrayList` thứ hai (bỏ phí công sức
 *      tính toán của tất cả các phần tử phía sau!).
 *
 * B. `Sequence<T>` (LAZY EVALUATION - Xử lý theo chiều DỌC / Vertical, giống hệt Java `Stream<T>`):
 *    - Khi bạn gọi `.asSequence()`, các hàm trung gian (`filter`, `map`) **CHƯA HỀ CHẠY**
 *      và **KHÔNG TẠO BẤT KỲ LIST TRUNG GIAN NÀO TRÊN HEAP**!
 *    - Khi gặp Terminal Operation (như `.first()` hoặc `.toList()`), `Sequence` kéo từng
 *      phần tử một đi xuyên qua toàn bộ ống dẫn:
 *        `Phần tử #1 -> filter -> (nếu qua) -> map -> (nếu gặp .first() thì DỪNG NGAY LẬP TỨC - Short-Circuit!)`
 *
 * 2. KHI NÀO DÙNG `List` TRỰC TIẾP, KHI NÀO DÙNG `asSequence()`?
 * ----------------------------------------------------------------------------
 * - **Dùng `List` trực tiếp (Mặc định trong 85% trường hợp)**:
 *   Khi danh sách nhỏ/vừa (dưới vài nghìn phần tử) và chỉ có 1-2 bước `filter`/`map`.
 *   Nhờ hàm `inline` không tạo đối tượng Lambda, `List` cực kỳ nhanh và đơn giản!
 * - **Dùng `.asSequence()` khi**:
 *   1. Danh sách lớn (hàng chục nghìn -> hàng triệu phần tử) đi qua chuỗi 3-4 bước biến đổi
 *      (giúp tránh cấp phát hàng loạt `ArrayList` trung gian làm đầy bộ nhớ HEAP).
 *   2. Chuỗi pipeline kết thúc bằng phép toán **Short-circuit** (`first()`, `firstOrNull()`,
 *      `take(5)`, `any { ... }`) sau khi đã `map` tốn kém!
 *
 * 3. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * Khi xử lý Batch Job, đọc CSV/Excel nhiều dòng hoặc tổng hợp báo cáo lớn trong bộ nhớ
 * của Spring Boot Service, chuyển sang `.asSequence()` trước khi chạy chuỗi `filter` -> `map` -> `take`
 * giúp giảm đáng kể áp lực Garbage Collection (GC Pause) trên Production.
 */

fun main() {
    println("--- ___ * START: Ex04SequenceVsIterable * ___ ---")

    val products = SampleRepository.getProducts()

    // ========================================================================
    // 1. EAGER Evaluation (`Iterable` / `List`): Chạy hết từng bước theo chiều ngang
    // ========================================================================
    println("\n[1] EAGER (`List`): Duyệt hết toàn bộ danh sách ở mỗi bước + tạo List trung gian:")
    val eagerFirstResult = products
        .filter {
            println("    [Eager filter] Kiểm tra ${it.id} (${it.name})")
            it.price.toDouble() > 500.0
        }
        .map {
            println("    [Eager map]    Biến đổi ${it.id} -> ${it.name.uppercase()}")
            it.name.uppercase()
        }
        .first()
    println("  => Kết quả Eager `.first()` : $eagerFirstResult")

    // ========================================================================
    // 2. LAZY Evaluation (`Sequence`): Chạy từng phần tử theo chiều dọc + Short-circuit!
    // ========================================================================
    println("\n[2] LAZY (`asSequence()`): Đi theo chiều dọc từng phần tử, tìm thấy `.first()` là DỪNG NGAY!")
    val lazyFirstResult = products
        .asSequence()
        .filter {
            println("    [Lazy filter]  Kiểm tra ${it.id} (${it.name})")
            it.price.toDouble() > 500.0
        }
        .map {
            println("    [Lazy map]     Biến đổi ${it.id} -> ${it.name.uppercase()}")
            it.name.uppercase()
        }
        .first()
    println("  => Kết quả Lazy `.first()`  : $lazyFirstResult (Chỉ chạm đúng phần tử P01 rồi dừng!)")

    // ========================================================================
    // 3. Khởi tạo chuỗi vô hạn với `generateSequence`
    // ========================================================================
    println("\n[3] Tạo chuỗi cấp số nhân Lazy với `generateSequence` + `take(5)`:")
    val retryBackoffDelaysMs = generateSequence(seed = 100L) { previous -> previous * 2 }
        .take(5)
        .toList()
    println("  Chuỗi thời gian chờ Retry (Exponential Backoff ms): $retryBackoffDelaysMs")

    println("\n--- ___ * FINISH: Ex04SequenceVsIterable * ___ ---")
}
