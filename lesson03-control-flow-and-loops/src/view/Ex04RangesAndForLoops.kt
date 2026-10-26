package view

/**
 * =====================================================================
 * LESSON 03 - EX04: RANGES (`..`, `..<`, `downTo`, `step`) & VÒNG LẶP `for`
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Trong Java, vòng lặp kinh điển `for (int i = 0; i < n; i++)` rất dễ mắc lỗi lệch 1 đơn vị
 *      (Off-by-one error: nhầm `<` với `<=`, hoặc nhầm `i--` khi duyệt ngược).
 *    - Kotlin loại bỏ hoàn toàn cú pháp `for (init; condition; update)` kiểu C/Java và thay bằng
 *      khái niệm **Range (Miền giá trị / Cấp số cộng `IntProgression`)**:
 *      + `1..5` (Closed range): Bao gồm cả điểm đầu (1) và điểm cuối (5) -> `1, 2, 3, 4, 5`.
 *      + `1..<5` (Open-ended range — chuẩn mới từ Kotlin 1.9+, tương đương `1 until 5`):
 *        Từ 1 tới sát 5 (KHÔNG lấy 5) -> `1, 2, 3, 4`. Cực chuẩn khi duyệt chỉ số mảng `0..<list.size`!
 *      + `5 downTo 1`: Duyệt lùi từ 5 về 1 -> `5, 4, 3, 2, 1`.
 *      + `1..10 step 2`: Bước nhảy bằng 2 -> `1, 3, 5, 7, 9`.
 *    - **Hiệu năng dưới Bytecode**:
 *      Khi bạn viết `for (i in 1..5)` hoặc `if (age in 18..65)`, trình biên dịch Kotlin **KHÔNG cấp phát**
 *      đối tượng `IntRange` trên Heap mà dịch thẳng thành vòng lặp `int i = 1; while (i <= 5)` hoặc
 *      `18 <= age && age <= 65` trên Stack!
 *
 * 2. SO SÁNH JAVA VS KOTLIN KHI DUYỆT DANH SÁCH KÈM CHỈ SỐ (INDEX):
 *    - Java: Phải dùng `for (int i = 0; i < list.size(); i++) { var item = list.get(i); }`
 *    - Kotlin: Dùng Destructuring `for ((index, item) in list.withIndex())` vừa có `index`, vừa có `item`
 *      trực quan và không bao giờ lo tràn chỉ số mảng!
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Dùng toán tử `in` để kiểm tra điều kiện nghiệp vụ cực kỳ dễ đọc:
 *      `require(request.age in 18..100) { "Tuổi phải nằm trong khoảng 18 đến 100" }`
 *      `if (order.status in setOf("PAID", "SHIPPED")) { ... }`
 * =====================================================================
 */

fun main() {
    println("--- ___ * START: Ex04RangesAndForLoops * ___ ---\n")

    // =================================================================
    // 1. Các loại Range trong Kotlin: `..`, `..<` (until), `downTo`, `step`
    // =================================================================
    println("1. [Ranges & Progressions] Các cách tạo dải số và ký tự:")

    print("   -> Closed Range (1..5):           ")
    for (i in 1..5) print("$i ")
    println()

    print("   -> Open-ended Range (1..<5):      ")
    for (i in 1..<5) print("$i ")
    println("(tương đương `1 until 5`)")

    print("   -> Duyệt ngược (10 downTo 2 step 2): ")
    for (i in 10 downTo 2 step 2) print("$i ")
    println()

    print("   -> Dải ký tự ('a'..'f'):          ")
    for (ch in 'a'..'f') print("$ch ")
    println("\n")

    // =================================================================
    // 2. Kiểm tra thuộc khoảng với `in` và `!in`
    // =================================================================
    println("2. [Membership Check `in` / `!in`] Kiểm tra giá trị có nằm trong khoảng hay không:")
    val customerAge = 28
    val discountCodeChar = 'K'

    // Dưới Bytecode: `18 <= customerAge && customerAge <= 60`
    println("   -> Tuổi $customerAge nằm trong độ tuổi lao động (18..60)? ${customerAge in 18..60}")
    println("   -> Ký tự '$discountCodeChar' KHÔNG phải chữ số ('0'..'9')? ${discountCodeChar !in '0'..'9'}\n")

    // =================================================================
    // 3. Duyệt Danh sách / Mảng theo 3 cách chuẩn trong Kotlin
    // =================================================================
    println("3. [Iterating Collections] 3 cách duyệt List/Array trong Kotlin:")
    val microservices = listOf("api-gateway", "auth-service", "order-service", "payment-service")

    // Cách 3a: Duyệt trực tiếp từng phần tử
    print("   -> [3a. for (item in list)]: ")
    for (service in microservices) {
        print("[$service] ")
    }
    println()

    // Cách 3b: Duyệt qua dải chỉ số hợp lệ `.indices` (tương đương `0..<microservices.size`)
    print("   -> [3b. for (i in list.indices)]: ")
    for (i in microservices.indices) {
        print("#$i=${microservices[i]}  ")
    }
    println()

    // Cách 3c: Duyệt lấy đồng thời cả (index, value) bằng `.withIndex()` (Ưa chuộng nhất!)
    println("   -> [3c. for ((index, item) in list.withIndex())]:")
    for ((index, serviceName) in microservices.withIndex()) {
        println("      Server Node ${index + 1} (index=$index) -> $serviceName")
    }
    println()

    // =================================================================
    // 4. Duyệt qua từng ký tự của một chuỗi String
    // =================================================================
    println("4. [String Iteration] Duyệt từng ký tự trong String:")
    val pinCode = "9081"
    print("   -> Mã PIN gồm các chữ số: ")
    for (digit in pinCode) {
        print("'$digit' ")
    }
    println("\n\n--- ___ * END: Ex04RangesAndForLoops * ___ ---")
}
