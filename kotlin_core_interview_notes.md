# 🎯 Sổ Tay Ôn Tập & Phỏng Vấn: Kotlin Core $\rightarrow$ Spring Boot Kotlin

> Tài liệu này được biên soạn theo cùng phong cách với chuỗi file `java_core_interview_*.sql` trong `D:\Java_Journey`: đi thẳng vào **Trả lời phỏng vấn**, **Bản chất dưới góc độ JVM Bytecode (Stack vs Heap)**, **Đối chiếu Java vs Kotlin**, và **Bẫy thực tế trong Spring Boot Kotlin**.

---

## PHẦN 1: NỀN TẢNG, BIẾN, KIỂU DỮ LIỆU & BỘ NHỚ

### KOTLIN-01 | Phân biệt `val`, `var` và `const val` dưới góc độ bộ nhớ (Stack vs Heap)
- **Trả lời phỏng vấn:**
  - `var` (variable): Biến có thể gán lại tham chiếu mới (mutable reference).
  - `val` (value): Biến chỉ đọc (read-only reference), tương đương từ khóa `final` trong Java — chỉ được gán 1 lần duy nhất. Khi biên dịch ra JVM Bytecode, thuộc tính `val` chỉ sinh hàm **getter**, không sinh hàm **setter**.
  - `const val`: Hằng số biên dịch (compile-time constant), tương đương `public static final` trong Java. Chỉ áp dụng cho kiểu nguyên thủy (`Int`, `Long`, `Double`, `Boolean`...) và `String`, bắt buộc khai báo ở top-level hoặc bên trong `object` / `companion object`.
- **Lưu ý / Bẫy phỏng vấn:**
  - **`val` chỉ khóa tham chiếu ở STACK, không khóa dữ liệu bên trong HEAP!** Nếu bạn khai báo `val list = mutableListOf(1, 2)`, bạn không thể gán `list = mutableListOf(3, 4)`, nhưng vẫn gọi được `list.add(3)` để sửa vùng nhớ trên Heap.
  - Khác với `const val` (giá trị được trình biên dịch inline thẳng vào nơi gọi lúc compile), `val` thông thường được tính toán lúc **runtime** (thậm chí `val` có thể có custom getter `get() = ...` trả về kết quả khác nhau mỗi lần gọi!).

---

### KOTLIN-02 | Sự khác nhau giữa `==` và `===` trong Kotlin (Ngược với thói quen Java!)
- **Trả lời phỏng vấn:**
  - Trong Kotlin, toán tử `==` dùng để **so sánh giá trị (Structural Equality)**: trình biên dịch tự động dịch `a == b` thành `a?.equals(b) ?: (b === null)` (an toàn tuyệt đối ngay cả khi `a` hoặc `b` là `null`).
  - Toán tử `===` dùng để **so sánh địa chỉ ô nhớ (Referential Equality)**: kiểm tra xem hai biến trên Stack có đang trỏ tới cùng một đối tượng trên Heap hay không (tương đương toán tử `==` đối với Object trong Java).
- **Ví dụ đối chiếu:**
  ```kotlin
  val s1 = "Kotlin"
  val s2 = String(charArrayOf('K', 'o', 't', 'l', 'i', 'n')) // Tạo vùng nhớ mới trên Heap

  println(s1 == s2)  // true  (So sánh nội dung chuỗi bằng equals)
  println(s1 === s2) // false (Khác địa chỉ ô nhớ trên Heap)
  ```
- **Lưu ý với kiểu số nguyên thủy:** Với các kiểu như `Int`, `Double` khi không bị nullable (`Int`), ở mức bytecode Kotlin dùng kiểu nguyên thủy (`int`, `double`) nên `a === b` và `a == b` hoạt động giống nhau; nhưng nếu là `Int?` (bị boxing thành `java.lang.Integer`), `===` sẽ bị ảnh hưởng bởi Integer Cache (`-128..127`) giống hệt Java!

---

### KOTLIN-03 | Kotlin có kiểu nguyên thủy (Primitive types) không?
- **Trả lời phỏng vấn:**
  - Ở tầng **cú pháp ngôn ngữ (Source code)**, mọi kiểu trong Kotlin (`Int`, `Long`, `Double`, `Boolean`) đều xuất hiện như những Class/Object (bạn có thể gọi `10.toDouble()`, `(-5).coerceAtLeast(0)`).
  - Tuy nhiên, ở tầng **JVM Bytecode**, trình biên dịch Kotlin tự động tối ưu hóa:
    - Biến **Non-null** (`Int`, `Long`, `Double`, `Boolean`) được dịch thẳng thành **primitive types** của Java (`int`, `long`, `double`, `boolean`) lưu trên Stack/thanh ghi để đạt hiệu năng tối đa.
    - Biến **Nullable** (`Int?`, `Double?`) hoặc tham số **Generic** (`List<Int>`, `Array<Int>`) bắt buộc phải được **Autoboxing** thành lớp Wrapper (`java.lang.Integer`, `java.lang.Double`) trên Heap.
- **Mẹo tối ưu bộ nhớ:** Khi cần mảng số nguyên lớn hiệu năng cao, luôn dùng `IntArray` (`int[]`), `LongArray` (`long[]`) thay vì `Array<Int>` (`Integer[]`).

---

### KOTLIN-04 | Hệ thống kiểu `Any`, `Unit` và `Nothing` trong Kotlin là gì?
- **Trả lời phỏng vấn:**
  - **`Any`**: Là class cha tối cao của mọi kiểu non-null trong Kotlin (tương đương `java.lang.Object` ở Bytecode). Muốn nhận cả giá trị `null` thì phải khai báo `Any?`.
  - **`Unit`**: Tương đương `void` trong Java khi hàm không trả về giá trị có nghĩa. Tuy nhiên, `Unit` là một **Singleton `object` thực sự**, nhờ đó các hàm trả về `Unit` có thể tham gia vào Generics (ví dụ `Consumer<T>` của Java chỉ cần viết thành `(T) -> Unit`, không cần tách riêng `Function<T, R>` và `Consumer<T>`!).
  - **`Nothing`**: Là **Bottom Type** (kiểu con của mọi kiểu dữ liệu trong Kotlin), không có bất kỳ thực thể (instance) nào. Dùng để đánh dấu các biểu thức **không bao giờ trả về bình thường** (như `throw Exception(...)` hoặc vòng lặp vô tận).
- **Ứng dụng thần thánh của `Nothing` trong Spring Boot:**
  Nhờ `throw ...` có kiểu là `Nothing` (con của mọi kiểu `T`), bạn có thể kết hợp với toán tử Elvis `?:`:
  ```kotlin
  val user: User = userRepository.findByIdOrNull(id)
      ?: throw EntityNotFoundException("Không tìm thấy user $id")
  // Trình biên dịch hiểu nhánh phải là Nothing -> biến `user` chắc chắn là User (Non-null)!
  ```

---

## PHẦN 2: NULL SAFETY & SCOPE FUNCTIONS

### KOTLIN-05 | Tổng hợp các công cụ xử lý Null Safety trong Kotlin
- **Bảng tóm tắt:**
  1. **Safe Call (`?.`)**: `user?.address?.city` — Nếu vế trái khác `null` thì gọi tiếp, nếu `null` thì cả biểu thức trả về `null`.
  2. **Elvis Operator (`?:`)**: `val city = user?.address?.city ?: "Hà Nội"` — Nếu vế trái `null` thì lấy giá trị mặc định (hoặc `return` / `throw`) ở vế phải.
  3. **Safe Cast (`as?`)**: `val staff = person as? Staff` — Nếu ép kiểu thất bại sẽ trả về `null` thay vì ném `ClassCastException`.
  4. **Smart Cast (`is` / `!is`)**: Sau khi kiểm tra `if (obj is String)` hoặc `if (x != null)`, trình biên dịch tự động coi `obj` là `String` và `x` là non-null trong phạm vi nhánh đó.
  5. **Not-null Assertion (`!!`)**: Cưỡng ép chuyển `T?` thành `T`, nếu thực tế là `null` sẽ ném ngay `NullPointerException`. **Quy tắc thực chiến:** Hạn chế tối đa `!!` trong code nghiệp vụ; hãy thay bằng `requireNotNull(x) { "Thông báo lỗi rõ ràng" }` hoặc `?: throw ...`.

---

### KOTLIN-06 | Phân biệt 5 Scope Functions: `let`, `also`, `apply`, `run`, `with`
- **Trả lời phỏng vấn:** Cả 5 hàm đều thực thi một khối lệnh trên một đối tượng ngữ cảnh (context object), nhưng chỉ khác nhau ở **2 tiêu chí**:
  1. Cách tham chiếu tới đối tượng bên trong lambda: dùng **`it`** (argument) hay **`this`** (receiver).
  2. Giá trị trả về của hàm: trả về **kết quả của lambda** hay trả về **chính đối tượng ngữ cảnh (context object)**.

| Hàm | Đối tượng trong Lambda | Giá trị trả về | Khi nào dùng trong thực tế (Spring Boot)? |
| :--- | :---: | :---: | :--- |
| **`let`** | `it` | Kết quả Lambda | Kiểm tra khác null (`email?.let { send(it) }`) hoặc biến đổi 1 đối tượng sang DTO (`entity.let(::toDto)`). |
| **`also`** | `it` | Chính đối tượng đó | Thực hiện hành động phụ (side-effect) như **ghi log / audit** giữa chuỗi xử lý: `save(order).also { log.info("Saved ${it.id}") }`. |
| **`apply`** | `this` | Chính đối tượng đó | Khởi tạo và cấu hình các thuộc tính của một đối tượng (Builder-style): `JwtConfig().apply { secret = "..."; ttl = 3600 }`. |
| **`run`** | `this` | Kết quả Lambda | Vừa truy cập các hàm của đối tượng qua `this`, vừa tính toán ra một kết quả cuối cùng. |
| **`with`** | `this` | Kết quả Lambda | Gọi nhiều phương thức trên một đối tượng đã biết chắc chắn khác null (`with(stringBuilder) { append(...); toString() }`). |

---

### KOTLIN-07 | Phân biệt `lateinit var` và `by lazy`
- **Trả lời phỏng vấn:**
  | Tiêu chí | `lateinit var` | `by lazy { ... }` |
  | :--- | :--- | :--- |
  | Từ khóa đi kèm | Chỉ dùng với **`var`** (mutable) | Chỉ dùng với **`val`** (read-only) |
  | Kiểu dữ liệu | Không dùng được với kiểu nguyên thủy (`Int`, `Boolean`, `Double`...) và không dùng với kiểu Nullable (`T?`) | Dùng được với **mọi kiểu** (kể cả nguyên thủy và Nullable) |
  | Cách khởi tạo | Gán từ bên ngoài sau khi tạo object (nếu đọc trước khi gán sẽ nổ `UninitializedPropertyAccessException`) | Tự động tính toán **đúng 1 lần** vào lần đầu tiên thuộc tính được truy cập |
  | Thread-safety | Không tự đồng bộ | Mặc định **Thread-safe** (`LazyThreadSafetyMode.SYNCHRONIZED`) |
  | Ứng dụng | Tiêm dependency bằng `@Autowired` trong class Test, hoặc khởi tạo trong `@BeforeEach` | Khởi tạo tài nguyên nặng (ObjectMapper, Regex phức tạp, kết nối) chỉ khi thực sự cần dùng |

---

## PHẦN 3: HÀM, EXTENSION FUNCTIONS & OOP CHO SPRING BOOT

### KOTLIN-08 | Bản chất của Extension Function là gì? Nó có phá vỡ tính đóng gói (Encapsulation) không?
- **Trả lời phỏng vấn:**
  - Extension Function (`fun String.normalizeCode(): String`) cho phép ta gọi thêm phương thức mới trên một class có sẵn như thể hàm đó nằm trong class đó, nhưng **thực chất không hề chỉnh sửa bytecode của class gốc**.
  - Khi biên dịch ra JVM Bytecode, trình biên dịch biến Extension Function thành một **hàm `public static`** nhận đối tượng gốc làm tham số đầu tiên:
    ```java
    // Kotlin: fun String.normalizeCode(): String = this.trim().uppercase()
    // Bytecode tương đương Java:
    public static String normalizeCode(String $this$normalizeCode) {
        return $this$normalizeCode.trim().toUpperCase();
    }
    ```
  - **Không phá vỡ tính đóng gói:** Bên trong Extension Function, bạn **không thể** truy cập các thuộc tính `private` hay `protected` của class gốc, chỉ truy cập được những gì `public` / `internal`.
  - **Bẫy phỏng vấn:** Vì bản chất là hàm `static`, Extension Function được phân giải tĩnh lúc **compile-time** (theo kiểu khai báo của biến), chứ **không có tính đa hình (override) lúc runtime** như method thông thường!

---

### KOTLIN-09 | Tại sao các class trong Kotlin mặc định là `final`? Ảnh hưởng gì tới Spring Boot?
- **Trả lời phỏng vấn:**
  - Tuân theo triết lý trong cuốn *Effective Java* (Item 19: *"Design and document for inheritance or else prohibit it"*), mọi `class` và `fun` trong Kotlin mặc định là **`final`** để tránh việc kế thừa ngoài ý muốn làm hỏng tính đúng đắn của lớp cha. Muốn cho phép kế thừa bắt buộc phải thêm từ khóa `open`.
  - **Ảnh hưởng tới Spring Boot:**
    - Các annotation như `@Configuration`, `@Service`, `@Transactional`, `@Cacheable`, `@Async` trong Spring hoạt động bằng cách dùng **CGLIB tạo một class Proxy kế thừa (subclass)** từ class của bạn lúc runtime để chèn logic AOP (mở/đóng transaction, quản lý bean singleton).
    - Nếu class hoặc hàm là `final`, CGLIB không thể kế thừa/override được và sẽ báo lỗi lúc khởi động!
  - **Giải pháp chuẩn trong Spring Boot Kotlin:**
    - Khai báo plugin **`kotlin("plugin.spring")`** (bản chất là plugin `all-open`) trong `build.gradle.kts`. Plugin này sẽ tự động thêm từ khóa `open` ở tầng bytecode cho mọi class gắn `@Component`, `@Service`, `@Repository`, `@Configuration`, `@Transactional` mà code Kotlin của bạn vẫn sạch đẹp, không cần gõ chữ `open` thủ công!

---

### KOTLIN-10 | Tại sao dùng `data class` cho DTO thì tuyệt vời nhưng KHÔNG NÊN dùng cho JPA `@Entity`?
- **Trả lời phỏng vấn:**
  - **`data class`** tự động sinh `equals()`, `hashCode()`, `toString()`, `copy()`, và `componentN()` dựa trên tất cả các thuộc tính trong Primary Constructor. Điều này cực kỳ hoàn hảo cho **DTO (Request / Response)** hoặc **Value Object** bất biến.
  - Tuy nhiên, nếu gắn `@Entity` của JPA/Hibernate lên `data class`, bạn sẽ dính **3 quả bom hẹn giờ**:
    1. **Xung đột với Hibernate Proxy:** `data class` bắt buộc là `final` (không thể đánh dấu `open data class`), làm hỏng cơ chế tạo Lazy Loading Proxy của Hibernate.
    2. **Nổ `LazyInitializationException` hoặc vòng lặp vô tận (StackOverflowError):** Hàm `toString()`, `equals()`, `hashCode()` tự sinh sẽ duyệt qua tất cả các trường trong primary constructor. Nếu trong đó có quan hệ `@OneToMany` / `@ManyToOne(fetch = FetchType.LAZY)`, chỉ cần gọi `println(entity)` hoặc đưa vào `Set` là Hibernate sẽ kích hoạt truy vấn ngầm ngoài Session gây lỗi!
    3. **Hỏng `HashSet` / `HashMap` khi lưu DB:** Khi mới khởi tạo Entity (`id = null`), `hashCode()` tính theo `id = null`. Sau khi `repository.save(entity)`, DB gán `id = 100`, làm `hashCode()` thay đổi $\rightarrow$ không thể tìm thấy chính Entity đó trong `Set` nữa!
  - **Best Practice trong Spring Data JPA + Kotlin:**
    - Dùng `class` thường (kết hợp plugin `kotlin("plugin.jpa")` để tự sinh no-arg constructor cho Hibernate).
    - Tự cài đặt `equals()` và `hashCode()` chỉ dựa trên khóa tự nhiên (business key) hoặc kiểm tra `id != null && id == other.id`.

---

### KOTLIN-11 | Khi nào dùng `sealed class` / `sealed interface` thay cho `enum class` hoặc Exception?
- **Trả lời phỏng vấn:**
  - **`enum class`**: Mỗi hằng số trong Enum chỉ có **1 thực thể duy nhất (singleton)** và tất cả các hằng số phải có **cùng cấu trúc thuộc tính**.
  - **`sealed class` / `sealed interface`**: Cho phép định nghĩa một tập hợp các lớp con **đóng (biết trước lúc compile)**, nhưng **mỗi lớp con có thể mang cấu trúc dữ liệu hoàn toàn khác nhau** và tạo được nhiều instance khác nhau!
  - **Ví dụ trong Spring Service:**
    ```kotlin
    sealed interface CheckoutResult {
        data class Success(val orderId: String, val payableAmount: BigDecimal) : CheckoutResult
        data class OutOfStock(val productId: String, val requested: Int, val available: Int) : CheckoutResult
        data object CustomerLocked : CheckoutResult
    }
    ```
    Khi Controller xử lý kết quả bằng `when (result)`, trình biên dịch kiểm tra **vét cạn (exhaustive)** cả 3 trường hợp mà không cần nhánh `else`; nếu sau này thêm trạng thái thứ 4 vào `CheckoutResult`, trình biên dịch sẽ báo lỗi ngay tại mọi chỗ `when` chưa xử lý!

---

## PHẦN 4: COLLECTIONS, SEQUENCES, GENERICS & ANNOTATIONS

### KOTLIN-12 | Phân biệt `Iterable` (Collection pipeline) và `Sequence` (`asSequence()`)
- **Trả lời phỏng vấn:**
  - **Các hàm trên `Iterable` (`list.filter {...}.map {...}`)**: Hoạt động theo cơ chế **Eager (Thực thi tức thì - theo chiều ngang)**. Mỗi bước `filter`, `map` sẽ chạy hết toàn bộ danh sách và **tạo ra một `ArrayList` trung gian mới** trên Heap trước khi chuyển sang bước tiếp theo.
  - **`Sequence` (`list.asSequence().filter {...}.map {...}.toList()`)**: Hoạt động theo cơ chế **Lazy (Thực thi trễ - theo chiều dọc)** giống hệt `Stream<T>` trong Java 8+. Không có bất kỳ bước nào chạy cho tới khi gặp hàm kết thúc (terminal operation như `toList()`, `firstOrNull()`, `sumOf()`), và từng phần tử sẽ đi xuyên suốt qua chuỗi `filter -> map` mà **không tạo danh sách trung gian**.
- **Khi nào nên dùng cái nào?**
  - Với danh sách nhỏ và vừa (vài chục đến vài ngàn phần tử) và ít bước biến đổi (1–2 bước): Dùng thẳng `list.filter().map()` vì code ngắn gọn và chi phí tạo `ArrayList` nhỏ rất rẻ, đôi khi nhanh hơn overhead khởi tạo Sequence.
  - Với danh sách lớn (hàng chục ngàn phần tử trở lên), chuỗi pipeline dài nhiều bước, hoặc có hàm ngắt sớm (**short-circuit** như `.firstOrNull()`, `.take(5)`): Dùng **`.asSequence()`** để tiết kiệm bộ nhớ và dừng ngay khi tìm đủ kết quả.

---

### KOTLIN-13 | Tại sao trong Spring Boot Kotlin phải viết `@field:NotBlank` thay vì `@NotBlank`?
- **Trả lời phỏng vấn:**
  - Trong Kotlin, khi bạn viết một thuộc tính trong Primary Constructor:
    ```kotlin
    data class CreateCustomerRequest(
        @NotBlank val fullName: String
    )
    ```
    Một khai báo `val fullName: String` đó đại diện cho **3 thành phần** ở mức JVM Bytecode:
    1. Tham số của hàm khởi tạo (`constructor parameter` - `@param:`)
    2. Biến thuộc tính ẩn bên trong class (`backing field` - `@field:`)
    3. Hàm lấy giá trị (`getter method` - `@get:`)
  - Theo quy tắc mặc định của Kotlin, nếu một Annotation cho phép gắn lên cả `PARAMETER` và `FIELD` (như `@NotBlank`, `@Email`, `@Min`, `@Valid` của `jakarta.validation`), Kotlin sẽ ưu tiên gắn nó vào **Tham số Constructor (`@param:`)**!
  - Trong khi đó, Hibernate Validator của Spring Boot mặc định quét kiểm tra trên **Field (`@field:`)** hoặc **Getter (`@get:`)** của object. Nếu thiếu `@field:`, validation sẽ bị bỏ qua âm thầm!
  - **Cách viết chuẩn bắt buộc:**
    ```kotlin
    data class CreateCustomerRequest(
        @field:NotBlank(message = "Tên không được để trống")
        val fullName: String,

        @field:Valid
        val address: AddressRequest
    )
    ```

---

### KOTLIN-14 |Từ khóa `reified` đi kèm `inline fun` giải quyết bài toán gì?
- **Trả lời phỏng vấn:**
  - Trên JVM, Generics bị **Type Erasure** (xóa kiểu lúc runtime): bên trong hàm `fun <T> parse(json: String): T`, JVM chỉ biết `T` là `Object`, nên bạn không thể viết `T::class.java` hay `if (item is T)`. Vì vậy ở Java, bạn luôn phải truyền thêm tham số `Class<T> clazz` (ví dụ `objectMapper.readValue(json, User.class)`).
  - Trong Kotlin, khi đánh dấu hàm là **`inline fun <reified T>`**, trình biên dịch sẽ **chép thẳng (inline) mã lệnh của hàm đó vào nơi gọi** và thay thế `T` bằng kiểu thực tế tại chỗ gọi (ví dụ `User`). Nhờ đó, thông tin kiểu `T` được giữ nguyên lúc runtime!
  - Đó là lý do các thư viện như Jackson Kotlin hay Spring `RestClient` / `MockMvc` cho phép gọi cực kỳ thanh lịch:
    ```kotlin
    val user = objectMapper.readValue<UserDto>(jsonString)
    ```

---

## PHẦN 5: COROUTINES & SPRING BOOT KOTLIN CHECKLIST

### KOTLIN-15 | Phân biệt `launch` và `async` trong Kotlin Coroutines
- **Trả lời phỏng vấn:**
  - **`launch`**: Dùng cho tác vụ **"Fire and Forget"** (chạy ngầm và không cần trả về giá trị kết quả). Trả về đối tượng `Job` để quản lý vòng đời (`job.join()`, `job.cancel()`). Nếu có ngoại lệ không bắt bên trong `launch`, nó sẽ truyền thẳng lên làm hủy CoroutineScope cha.
  - **`async`**: Dùng khi cần **tính toán song song và lấy kết quả trả về**. Trả về đối tượng `Deferred<T>` (kế thừa từ `Job`), gọi `.await()` (một `suspend fun`) để đợi và nhận kết quả kiểu `T`.

---

### KOTLIN-16 | Checklist 5 bước khi tạo dự án Spring Boot + Kotlin thực tế
1. **Plugins trong `build.gradle.kts`**:
   - `kotlin("jvm")`
   - `kotlin("plugin.spring")` (tự động mở `open` cho các class Spring `@Service`, `@Configuration`, `@Transactional`)
   - `kotlin("plugin.jpa")` (nếu dùng Spring Data JPA — tự động sinh constructor rỗng cho `@Entity`)
2. **Thư viện bắt buộc cho JSON (Jackson)**:
   - Thêm `com.fasterxml.jackson.module:jackson-module-kotlin` (để Jackson đọc/ghi được `data class` Kotlin không cần constructor rỗng hay `@JsonProperty` thủ công).
3. **Cấu hình trình biên dịch**:
   - Đảm bảo có `-Xjsr305=strict` để trình biên dịch Kotlin nhận diện chặt chẽ các annotation `@Nullable` / `@NonNull` từ hệ sinh thái Spring Framework.
4. **Dependency Injection**:
   - Luôn dùng **Constructor Injection** với `private val`:
     ```kotlin
     @Service
     class OrderService(
         private val orderRepository: OrderRepository,
         private val customerRepository: CustomerRepository
     )
     ```
5. **DTO vs Entity**:
   - Request / Response DTO: Dùng `data class` + `@field:NotBlank` / `@field:Valid` + viết hàm chuyển đổi bằng **Extension Function** (`fun Order.toResponseDto()`).
   - JPA `@Entity`: Dùng `class` thường, không dùng `data class`.
