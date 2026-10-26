package view

/**
 * =====================================================================
 * LESSON 01 - EX01: HÀM MAIN, NHẬP XUẤT CƠ BẢN, COMMENTS & BACKTICKS
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Trong Java truyền thống, các phương thức thường phải đặt bên trong một `class`. Để chạy chương trình,
 *      JVM cần tìm điểm vào `public static void main(String[] args)` bên trong một class.
 *    - Trong Kotlin, bạn có thể viết **Top-Level Function** (`fun main()`) nằm trực tiếp
 *      trong file `Ex01MainAndIo.kt` mà không cần tự khai báo `class` bao ngoài.
 *    - Khi biên dịch ra JVM Bytecode, trình biên dịch Kotlin (`kotlinc`) tự động sinh ra
 *      một class tên là `view.Ex01MainAndIoKt` (tên file + hậu tố `Kt`), và biến hàm
 *      `fun main()` thành `public static final void main(String[] args)` để JVM thực thi!
 *    - Kể từ Kotlin 1.3+, tham số `args: Array<String>` là tùy chọn (Parameterless main).
 *
 * 2. SO SÁNH JAVA VS KOTLIN:
 *    - Java:   `System.out.println("Hello");` -> Dài dòng.
 *    - Kotlin: `println("Hello")`             -> Hàm inline trong `kotlin.io`, bên dưới
 *                                                vẫn gọi thẳng `System.out.println()`.
 *    - Java không cho phép đặt tên hàm có dấu cách hay trùng từ khóa (`in`, `is`, `when`).
 *    - Kotlin cho phép bọc định danh trong cặp dấu **Backticks (` `)** để:
 *      (a) Gọi các hàm/biến của thư viện Java có tên trùng với từ khóa của Kotlin
 *          (ví dụ: `System.`in``, Mockito `Mockito.`when`(...)`).
 *      (b) Đặt tên hàm kiểm thử (Unit Test) bằng câu tiếng Anh/tiếng Việt tự nhiên có khoảng trắng!
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Điểm khởi chạy của ứng dụng Spring Boot Kotlin thường dùng top-level `fun main(args: Array<String>)`:
 *      ```kotlin
 *      @SpringBootApplication
 *      class OrderServiceApplication
 *
 *      fun main(args: Array<String>) {
 *          runApplication<OrderServiceApplication>(*args)
 *      }
 *      ```
 *    - Trong JUnit 5 + Spring Boot Test, lập trình viên Kotlin thường dùng dấu backtick để viết thẳng
 *      tên hàm test dễ đọc thay vì phải gắn thêm `@DisplayName("...")`:
 *      `fun `should return 200 OK when user exists`() { ... }`
 * =====================================================================
 */

fun main() {
    println("--- ___ * START: Ex01MainAndIo * ___ ---\n")

    // =================================================================
    // 1. Xuất dữ liệu ra Console: print() vs println()
    // =================================================================
    println("1. [print vs println] Xuat du lieu ra Standard Output:")
    print("   -> Dong nay dung print() nen chua xuong dong... ")
    println("Con println() se in tiep va ket thuc bang ky tu newline (\\n)!")
    println("   -> Dong moi bat dau tai day.\n")

    // =================================================================
    // 2. Các loại Comments và KDoc trong Kotlin
    // =================================================================
    println("2. [Comments & KDoc] Ghi chu trong ma nguon:")
    // Comment 1 dòng (Single-line comment) giống hệt Java.
    /*
       Comment nhiều dòng (Multi-line block comment).
       ĐIỂM ĐẶC BIỆT CỦA KOTLIN SO VỚI JAVA:
       /* Kotlin cho phép lồng (nest) block comment bên trong block comment! */
       Điều này rất tiện khi bạn muốn tạm comment-out cả một đoạn code lớn.
    */
    val sumResult = calculateDiscountedPrice(originalPrice = 200_000L, discountPercent = 15)
    check(sumResult == 170_000L)
    println("   -> Gia sau giam (ham co tai lieu KDoc): $sumResult VND\n")

    // =================================================================
    // 3. Backtick Identifiers (` `) - Đặc sản của Kotlin
    // =================================================================
    println("3. [Backtick Identifiers] Giai cuu tu khoa & Dat ten ham Test:")

    // 3a. Đặt tên biến hoặc gọi thành phần trùng với Hard Keyword của Kotlin (in, is, when, object, val...)
    // Trong Java, `System.in` là hợp lệ vì `in` không phải từ khóa của Java.
    // Nhưng trong Kotlin, `in` là từ khóa (dùng cho vòng lặp `for (x in list)`).
    // Do đó khi tham chiếu tới `System.in` hoặc biến tên `in`, ta bọc trong dấu backtick `` `in` ``:
    val `in` = "Luong du lieu dau vao gia lap (escaped keyword `in`)"
    val `is` = true
    println("   -> Gia tri bien trung tu khoa `in`: ${`in`}")
    println("   -> Gia tri bien trung tu khoa `is`: ${`is`}")
    println("   -> Truy cap chuan InputStream cua JVM: System.`in` = ${System.`in`.javaClass.simpleName}")

    // 3b. Gọi hàm có khoảng trắng (mô phỏng JUnit 5 Test trong Spring Boot)
    println("\n4. [Spring Boot Test Idiom] Ten ham chua khoang trang bang Backticks:")
    `should return 200 OK when user exists`()
    `should throw 404 NotFound when order id is invalid`()

    println("\n--- ___ * END: Ex01MainAndIo * ___ ---")
}

/**
 * Hàm tính giá sau khi áp dụng phần trăm chiết khấu.
 *
 * Trong KDoc (tương đương Javadoc của Java):
 * - Dùng cú pháp Markdown trực tiếp thay vì thẻ HTML rườm rà.
 * - Có thể liên kết tới hàm/class khác bằng cặp ngoặc vuông như [main].
 *
 * @param originalPrice Giá gốc của sản phẩm (đơn vị: VND).
 * @param discountPercent Phần trăm giảm giá (0..100).
 * @return Giá cuối cùng sau khi trừ chiết khấu.
 */
fun calculateDiscountedPrice(originalPrice: Long, discountPercent: Int): Long {
    return originalPrice - (originalPrice * discountPercent / 100)
}

// Mô phỏng hàm kiểm thử JUnit 5 trong dự án Spring Boot Kotlin
fun `should return 200 OK when user exists`() {
    println("   [PASS] Test: `should return 200 OK when user exists`")
}

fun `should throw 404 NotFound when order id is invalid`() {
    println("   [PASS] Test: `should throw 404 NotFound when order id is invalid`")
}
