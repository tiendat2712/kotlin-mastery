package view

import bean.Address
import bean.Customer

/**
 * =====================================================================
 * LESSON 02 - EX01: NULLABLE TYPES (`?`) & TOÁN TỬ SAFE CALL (`?.`)
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Tony Hoare (cha đẻ của tham chiếu `null` năm 1965) từng gọi `null` là **"Sai lầm tỷ đô" (The Billion Dollar Mistake)**
 *      vì trong Java, mọi biến kiểu đối tượng (`String`, `Customer`) đều ngầm định có thể mang giá trị `null`.
 *      Trình biên dịch Java không hề cảnh báo cho tới khi chương trình chạy và sập vì `NullPointerException` (NPE)!
 *    - Kotlin giải quyết triệt để ngay tại **Compile-time** bằng cách tách hệ thống kiểu làm 2 thế giới:
 *      + **Non-Null Type (`String`, `Customer`)**: Tuyệt đối không thể gán `null`. Dưới tầng Bytecode, Kotlin
 *        gắn annotation `@org.jetbrains.annotations.NotNull` và chèn lệnh kiểm tra nhanh `Intrinsics.checkNotNullParameter(...)`
 *        ở đầu các hàm public để chặn đứng `null` truyền từ code Java sang!
 *      + **Nullable Type (`String?`, `Customer?`)**: Cho phép mang giá trị `null`, nhưng trình biên dịch **CẤM**
 *        gọi trực tiếp dấu chấm `.` (`str.length` sẽ báo lỗi biên dịch ngay!).
 *    - **Toán tử Safe Call (`?.`)**:
 *      + Biểu thức `customer?.address?.city?.uppercase()` được dịch ra Bytecode thành chuỗi kiểm tra `null`
 *        ngắn mạch (short-circuit) an toàn trên Stack mà không tạo thêm bất kỳ đối tượng trung gian nào trên Heap
 *        (như `Optional<T>` của Java 8 vốn phải bọc thêm 1 lớp vỏ Object trên Heap!).
 *
 * 2. SO SÁNH JAVA VS KOTLIN:
 *    - Java truyền thống:
 *      ```java
 *      String city = null;
 *      if (customer != null && customer.getAddress() != null && customer.getAddress().getCity() != null) {
 *          city = customer.getAddress().getCity().toUpperCase();
 *      }
 *      ```
 *    - Java 8+ `Optional` (tốn bộ nhớ Heap cho các wrapper `Optional`):
 *      ```java
 *      String city = Optional.ofNullable(customer)
 *          .map(Customer::getAddress)
 *          .map(Address::getCity)
 *          .map(String::toUpperCase)
 *          .orElse(null);
 *      ```
 *    - Kotlin (`?.` — Zero-overhead, không cấp phát đối tượng phụ trên Heap!):
 *      ```kotlin
 *      val city: String? = customer?.address?.city?.uppercase()
 *      ```
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong `@RestController`, các `@RequestParam(required = false)` hoặc các trường tùy chọn trong Request DTO
 *      luôn được khai báo là `String?`, `Int?`.
 *    - Spring Data JPA hỗ trợ trực tiếp kiểu trả về Nullable trong Repository:
 *      `fun findByEmail(email: String): Customer?` (thay thế hoàn toàn `Optional<Customer>` của Java!).
 * =====================================================================
 */

fun main() {
    println("--- ___ * START: Ex01NullableAndSafeCall * ___ ---\n")

    // =================================================================
    // 1. Non-Null Type vs Nullable Type tại thời điểm biên dịch
    // =================================================================
    println("1. [Compile-time Null Safety] Phân tách Non-Null (String) và Nullable (String?):")
    val mandatoryName: String = "Nguyễn Văn An"
    // mandatoryName = null // ❌ LỖI BIÊN DỊCH: Null can not be a value of a non-null type String

    val optionalNickname: String? = null
    println("   -> mandatoryName (Non-Null): \"$mandatoryName\" (độ dài = ${mandatoryName.length})")
    println("   -> optionalNickname (Nullable): $optionalNickname")

    // optionalNickname.length // ❌ LỖI BIÊN DỊCH: Only safe (?.) or non-null asserted (!!.) calls are allowed
    println("   -> Gọi an toàn optionalNickname?.length = ${optionalNickname?.length}\n")

    // =================================================================
    // 2. Safe Call Chaining (`?.`) qua nhiều tầng đối tượng lồng nhau
    // =================================================================
    println("2. [Safe Call Chaining] Đi xuyên qua chuỗi đối tượng bằng `?.`:")

    val fullCustomer: Customer? = Customer(
        id = "CUST-01",
        name = "Trần Thị Bích",
        email = "bich.tran@example.com",
        address = Address(street = "123 Nguyễn Huệ", city = "Hồ Chí Minh")
    )

    val customerWithoutCity: Customer? = Customer(
        id = "CUST-02",
        name = "Lê Hoàng Nam",
        email = null,
        address = Address(street = "45 Lê Lợi", city = null)
    )

    val nullCustomer: Customer? = null

    // Nếu bất kỳ mắt xích nào trong chuỗi là `null`, toàn bộ biểu thức dừng ngay và trả về `null`!
    println("   -> [CUST-01] Thành phố in hoa: ${fullCustomer?.address?.city?.uppercase()}")
    println("   -> [CUST-02] Thành phố in hoa (city là null): ${customerWithoutCity?.address?.city?.uppercase()}")
    println("   -> [NULL]    Thành phố in hoa (customer là null): ${nullCustomer?.address?.city?.uppercase()}\n")

    // =================================================================
    // 3. Kết hợp Safe Call với các hàm kiểm tra chuỗi (`isNullOrBlank()`)
    // =================================================================
    println("3. [isNullOrBlank / isNullOrEmpty] Gọi hàm kiểm tra trực tiếp trên biến Nullable:")
    // Điểm thú vị: `isNullOrBlank()` là Extension Function định nghĩa trên kiểu `CharSequence?` (có dấu ?),
    // nên bạn gọi thẳng bằng dấu chấm `.` mà KHÔNG cần `?.`!
    val blankEmail: String? = "   "
    val missingEmail: String? = customerWithoutCity?.email

    println("   -> blankEmail (\"   \").isNullOrBlank()   = ${blankEmail.isNullOrBlank()}")
    println("   -> missingEmail (null).isNullOrBlank() = ${missingEmail.isNullOrBlank()}")

    println("\n--- ___ * END: Ex01NullableAndSafeCall * ___ ---")
}
