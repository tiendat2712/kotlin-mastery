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
 *      Trình biên dịch Java không cảnh báo cho tới khi chương trình chạy và sập vì `NullPointerException` (NPE)!
 *    - Kotlin giải quyết ngay tại **Compile-time** bằng cách tách hệ thống kiểu làm 2 nhánh rõ rệt:
 *      + **Non-Null Type (`String`, `Customer`)**: Không cho phép gán `null` trong mã Kotlin. Dưới tầng Bytecode, Kotlin
 *        gắn annotation `@org.jetbrains.annotations.NotNull` và chèn lệnh kiểm tra nhanh `Intrinsics.checkNotNullParameter(...)`
 *        ở đầu các hàm public để sớm phát hiện `null` truyền từ code Java sang!
 *      + **Nullable Type (`String?`, `Customer?`)**: Cho phép mang giá trị `null`, nhưng trình biên dịch **không cho phép**
 *        gọi trực tiếp dấu chấm `.` (`str.length` sẽ báo lỗi biên dịch ngay!).
 *    - **Toán tử Safe Call (`?.`)**:
 *      + Biểu thức `customer?.address?.city?.uppercase()` được dịch ra Bytecode thành chuỗi kiểm tra `null`
 *        ngắn mạch (short-circuit) an toàn mà không cần tạo đối tượng bọc trung gian trên Heap
 *        (như `Optional<T>` của Java 8 vốn phải cấp phát thêm wrapper Object trên Heap).
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
 *      nên được khai báo là `String?`, `Int?`.
 *    - Spring Data JPA hỗ trợ trực tiếp kiểu trả về Nullable trong Repository:
 *      `fun findByEmail(email: String): Customer?` (thường được ưu tiên hơn `Optional<Customer>` của Java).
 * =====================================================================
 */

fun main() {
    println("--- ___ * START: Ex01NullableAndSafeCall * ___ ---\n")

    // =================================================================
    // 1. Non-Null Type vs Nullable Type tại thời điểm biên dịch
    // =================================================================
    println("1. [Compile-time Null Safety] Phan tach Non-Null (String) va Nullable (String?):")
    val mandatoryName: String = "Nguyen Van An"
    // mandatoryName = null // LỖI BIÊN DỊCH: Null can not be a value of a non-null type String

    val optionalNickname: String? = null
    println("   -> mandatoryName (Non-Null): \"$mandatoryName\" (do dai = ${mandatoryName.length})")
    println("   -> optionalNickname (Nullable): $optionalNickname")

    // optionalNickname.length // LỖI BIÊN DỊCH: Only safe (?.) or non-null asserted (!!.) calls are allowed
    println("   -> Goi an toan optionalNickname?.length = ${optionalNickname?.length}\n")

    // =================================================================
    // 2. Safe Call Chaining (`?.`) qua nhiều tầng đối tượng lồng nhau
    // =================================================================
    println("2. [Safe Call Chaining] Di xuyen qua chuoi doi tuong bang `?.`:")

    val fullCustomer: Customer? = Customer(
        id = "CUST-01",
        name = "Tran Thi Bich",
        email = "bich.tran@example.com",
        address = Address(street = "123 Nguyen Hue", city = "Ho Chi Minh")
    )

    val customerWithoutCity: Customer? = Customer(
        id = "CUST-02",
        name = "Le Hoang Nam",
        email = null,
        address = Address(street = "45 Le Loi", city = null)
    )

    val nullCustomer: Customer? = null

    // Nếu bất kỳ mắt xích nào trong chuỗi là `null`, toàn bộ biểu thức dừng ngay và trả về `null`!
    val city1 = fullCustomer?.address?.city?.uppercase()
    val city2 = customerWithoutCity?.address?.city?.uppercase()
    val city3 = nullCustomer?.address?.city?.uppercase()
    check(city1 == "HO CHI MINH" && city2 == null && city3 == null)

    println("   -> [CUST-01] Thanh pho in hoa: $city1")
    println("   -> [CUST-02] Thanh pho in hoa (city la null): $city2")
    println("   -> [NULL]    Thanh pho in hoa (customer la null): $city3\n")

    // =================================================================
    // 3. Kết hợp Safe Call với các hàm kiểm tra chuỗi (`isNullOrBlank()`)
    // =================================================================
    println("3. [isNullOrBlank / isNullOrEmpty] Goi ham kiem tra truc tiep tren bien Nullable:")
    // Điểm thú vị: `isNullOrBlank()` là Extension Function định nghĩa trên kiểu `CharSequence?` (có dấu ?),
    // nên bạn gọi thẳng bằng dấu chấm `.` mà KHÔNG cần `?.`!
    val blankEmail: String? = "   "
    val missingEmail: String? = customerWithoutCity?.email
    check(blankEmail.isNullOrBlank() && missingEmail.isNullOrBlank())

    println("   -> blankEmail (\"   \").isNullOrBlank()   = ${blankEmail.isNullOrBlank()}")
    println("   -> missingEmail (null).isNullOrBlank() = ${missingEmail.isNullOrBlank()}")

    println("\n--- ___ * END: Ex01NullableAndSafeCall * ___ ---")
}
