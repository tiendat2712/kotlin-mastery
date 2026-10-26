package view

/**
 * =====================================================================
 * LESSON 03 - EX04: RANGES (`..`, `..<`, `downTo`, `step`) & VÒNG LẶP `for`
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Trong Java, vòng lặp truyền thống `for (int i = 0; i < n; i++)` dễ mắc lỗi lệch 1 đơn vị
 *      (Off-by-one error: nhầm `<` với `<=`, hoặc nhầm `i--` khi duyệt ngược).
 *    - Kotlin thay thế cú pháp `for (init; condition; update)` kiểu C/Java bằng
 *      khái niệm **Range (Miền giá trị / Cấp số cộng `IntProgression`)**:
 *      + `1..5` (Closed range): Bao gồm cả điểm đầu (1) và điểm cuối (5) -> `1, 2, 3, 4, 5`.
 *      + `1..<5` (Open-ended range — chuẩn từ Kotlin 1.9+, tương đương `1 until 5`):
 *        Từ 1 tới sát 5 (KHÔNG lấy 5) -> `1, 2, 3, 4`. Rất tiện khi duyệt chỉ số mảng `0..<list.size`!
 *      + `5 downTo 1`: Duyệt lùi từ 5 về 1 -> `5, 4, 3, 2, 1`.
 *      + `1..10 step 2`: Bước nhảy bằng 2 -> `1, 3, 5, 7, 9`.
 *    - **Hiệu năng dưới Bytecode**:
 *      Khi bạn viết `for (i in 1..5)` hoặc `if (age in 18..65)`, trình biên dịch Kotlin **không cần cấp phát**
 *      đối tượng `IntRange` trên Heap mà dịch thẳng thành vòng lặp `int i = 1; while (i <= 5)` hoặc
 *      `18 <= age && age <= 65`!
 *
 * 2. SO SÁNH JAVA VS KOTLIN KHI DUYỆT DANH SÁCH KÈM CHỈ SỐ (INDEX):
 *    - Java: Phải dùng `for (int i = 0; i < list.size(); i++) { var item = list.get(i); }`
 *    - Kotlin: Dùng Destructuring `for ((index, item) in list.withIndex())` vừa có `index`, vừa có `item`
 *      trực quan và tránh lỗi tràn chỉ số mảng!
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Dùng toán tử `in` để kiểm tra điều kiện nghiệp vụ dễ đọc:
 *      `require(request.age in 18..100) { "Tuoi phai nam trong khoang 18 den 100" }`
 *      `if (order.status in setOf("PAID", "SHIPPED")) { ... }`
 * =====================================================================
 */

fun main() {
    println("--- ___ * START: Ex04RangesAndForLoops * ___ ---\n")

    // =================================================================
    // 1. Các loại Range trong Kotlin: `..`, `..<` (until), `downTo`, `step`
    // =================================================================
    println("1. [Ranges & Progressions] Cac cach tao dai so va ky tu:")

    print("   -> Closed Range (1..5):           ")
    for (i in 1..5) print("$i ")
    println()

    print("   -> Open-ended Range (1..<5):      ")
    for (i in 1..<5) print("$i ")
    println("(tuong duong `1 until 5`)")

    print("   -> Duyet nguoc (10 downTo 2 step 2): ")
    for (i in 10 downTo 2 step 2) print("$i ")
    println()

    print("   -> Dai ky tu ('a'..'f'):          ")
    for (ch in 'a'..'f') print("$ch ")
    println("\n")

    // =================================================================
    // 2. Kiểm tra thuộc khoảng với `in` và `!in`
    // =================================================================
    println("2. [Membership Check `in` / `!in`] Kiem tra gia tri co nam trong khoang hay khong:")
    val customerAge = 28
    val discountCodeChar = 'K'
    check(customerAge in 18..60 && discountCodeChar !in '0'..'9')

    // Dưới Bytecode: `18 <= customerAge && customerAge <= 60`
    println("   -> Tuoi $customerAge nam trong do tuoi lao dong (18..60)? ${customerAge in 18..60}")
    println("   -> Ky tu '$discountCodeChar' KHONG phai chu so ('0'..'9')? ${discountCodeChar !in '0'..'9'}\n")

    // =================================================================
    // 3. Duyệt Danh sách / Mảng theo 3 cách chuẩn trong Kotlin
    // =================================================================
    println("3. [Iterating Collections] 3 cach duyet List/Array trong Kotlin:")
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

    // Cách 3c: Duyệt lấy đồng thời cả (index, value) bằng `.withIndex()` (Phổ biến nhất!)
    println("   -> [3c. for ((index, item) in list.withIndex())]:")
    for ((index, serviceName) in microservices.withIndex()) {
        println("      Server Node ${index + 1} (index=$index) -> $serviceName")
    }
    println()

    // =================================================================
    // 4. Duyệt qua từng ký tự của một chuỗi String
    // =================================================================
    println("4. [String Iteration] Duyet tung ky tu trong String:")
    val pinCode = "9081"
    print("   -> Ma PIN gom cac chu so: ")
    for (digit in pinCode) {
        print("'$digit' ")
    }
    println("\n\n--- ___ * END: Ex04RangesAndForLoops * ___ ---")
}
