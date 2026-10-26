# 🎯 Sổ Tay Ôn Tập & Phỏng Vấn: Kotlin Core $\rightarrow$ Spring Boot Kotlin (20 Chủ Đề Trọng Tâm)

> Tài liệu này tổng hợp **20 chủ đề kỹ thuật cốt lõi** (`KOTLIN-01` $\rightarrow$ `KOTLIN-20`), được biên soạn theo phong cách đi thẳng vào **Bản chất ngôn ngữ & JVM Bytecode**, **Đối chiếu Java vs Kotlin**, và **Lưu ý thực tế khi làm Backend / Spring Boot**.
> *(Phạm vi phiên bản đối chiếu trong repo: Kotlin `2.0.21`, `kotlinx.coroutines` `1.8.1`, JVM Target `17`).*

---

## PHẦN 1: BIẾN, TÍNH BẤT BIẾN (IMMUTABILITY), BỘ NHỚ & KIỂU DỮ LIỆU

### KOTLIN-01 | Phân biệt `val`, `var`, `const val` và giới hạn của mô hình Stack vs Heap
- **Bản chất:**
  - `var` (mutable variable/property): Cho phép gán lại giá trị hoặc tham chiếu mới (`=`). Với thuộc tính của class, trình biên dịch sinh cả `getter` và `setter`.
  - `val` (read-only variable/property): Chỉ được gán 1 lần duy nhất (tương đương `final` trong Java đối với biến cục bộ). Với thuộc tính của class, trình biên dịch chỉ sinh `getter`, không sinh `setter`.
  - `const val` (compile-time constant): Hằng số biết trước ngay lúc biên dịch (`public static final` + được trình biên dịch inline giá trị vào nơi gọi). Chỉ áp dụng cho kiểu cơ bản (primitive) và `String`, khai báo ở top-level hoặc trong `object` / `companion object`.
- **Những điểm cần tránh hiểu nhầm:**
  1. **`val` không đồng nghĩa với hằng số bất biến cố định:** Một thuộc tính `val` có thể định nghĩa custom getter (`val currentTime: Long get() = System.currentTimeMillis()`) và trả về giá trị khác nhau ở mỗi lần đọc.
  2. **Không phải mọi biến/tham chiếu đều nằm trên Stack:**
     - **Biến cục bộ (local variable)** trong hàm thường nằm trong khung kích hoạt của hàm (stack frame / thanh ghi CPU — mô hình minh họa của JVM).
     - **Thuộc tính của class (`val`/`var` property)** có backing field nằm ngay bên trong đối tượng đó trên **Heap**, không nằm trên Stack.
     - **Biến cục bộ `var` bị Closure (Non-inline Lambda) capture:**
       + Nếu biến cục bộ `var` được đọc/ghi bên trong một **Lambda được `inline`** (như `listOf(1, 2).forEach { sum += it }`), thân lambda được chép thẳng vào hàm gọi nên bytecode chỉ dùng biến cục bộ trong Stack Frame (`iload`, `iadd`, `istore`), **không** tạo đối tượng hộp trên Heap.
       + Chỉ khi biến cục bộ `var` bị bắt giữ bởi một **Closure phải tồn tại dưới dạng đối tượng** (như truyền vào hàm non-inline, `noinline`, hoặc lưu trữ callback), trình biên dịch Kotlin mới đóng gói nó vào một đối tượng wrapper trên **Heap** (`Ref.IntRef` cho `Int`, `Ref.LongRef` cho `Long`, hoặc `Ref.ObjectRef<T>` cho kiểu tham chiếu đối tượng) để chia sẻ giữa hàm ngoài và closure.

---

### KOTLIN-02 | Phân biệt `val`, Read-Only Interface (`List<T>`), Shallow Copy và Deep Immutability
- **Bản chất 4 khái niệm thường bị đánh đồng:**
  1. **`val`:** Chỉ ngăn việc dùng toán tử `=` để trỏ biến sang đối tượng khác.
  2. **`List<T>`, `Set<T>`, `Map<K, V>` (Read-only interfaces):** Là **giao diện chỉ đọc** (không khai báo các phương thức sửa đổi như `add`, `remove`, `put`). Nó **không đảm bảo** đối tượng bên dưới không thể thay đổi.
  3. **Shallow Copy / Snapshot (`toList()`, `toMap()`, `data class.copy()`):**
     - `.toList()` tạo một danh sách chỉ đọc tách biệt với các thao tác thêm/xóa sau đó trên collection nguồn (lưu ý: stdlib có tối ưu trả về singleton cho danh sách 0 hoặc 1 phần tử nên không cam kết luôn cấp phát `ArrayList` mới), nhưng các phần tử bên trong vẫn **chia sẻ chung tham chiếu** với bản gốc.
     - `.copy()` của `data class` cũng chỉ là **shallow copy**: nếu thuộc tính bên trong là `MutableList` hay object khả biến, bản sao vẫn dùng chung dữ liệu lồng nhau đó. Ngoài ra, sao chép một đối tượng khả biến (`copyItem()`) chỉ tạo ra một **bản sao khả biến độc lập** (defensive copy), không biến đối tượng đó thành immutable.
  4. **Deep Immutability (Bất biến sâu):** Cả container lẫn toàn bộ cây đối tượng lồng bên trong đều không thể bị thay đổi trạng thái sau khi khởi tạo.
- **Ví dụ 2 tình huống thực tế (Xem chi tiết tại `lesson07/Ex06ImmutabilityAliasingAndSharedState.kt`):**
  - **Mutable Aliasing:**
    ```kotlin
    val backing: MutableList<String> = mutableListOf("A", "B")
    val readOnlyView: List<String> = backing // Cùng trỏ tới 1 ArrayList
    backing.add("C")
    println(readOnlyView.size) // 3 -> Dù là `val` + `List<String>`, dữ liệu vẫn đổi!
    ```
  - **Shallow Copy chứa phần tử khả biến (Mutable Elements):**
    Nếu bạn có `val copyList = originalList.toList()` hoặc `val copyDto = dto.copy()`, việc gọi `copyList[0].price = 999` (khi phần tử có `var price`) vẫn làm thay đổi phần tử trong `originalList`!

---

### KOTLIN-03 | Shared Mutable State trong `object` hoặc Singleton Service có tự động Thread-Safe không?
- **Bản chất:**
  - Việc khởi tạo một `object` trong Kotlin là thread-safe (nhờ cơ chế nạp lớp của JVM).
  - Tuy nhiên, nếu bên trong `object` (hoặc một Spring `@Service` vốn mặc định là Singleton) có chứa **trạng thái khả biến dùng chung (shared mutable state)** như `var count = 0`, `mutableListOf()`, `mutableMapOf()`, thì các thao tác đọc/ghi đồng thời từ nhiều luồng **KHÔNG tự động thread-safe** (ngay cả khi bạn phơi bày ra ngoài qua kiểu chỉ đọc `Map<K, V>`).
- **Giải pháp khi cần lưu trạng thái dùng chung đa luồng:**
  - Dùng cấu trúc dữ liệu đồng thời của JDK (`ConcurrentHashMap`, `AtomicInteger`, `AtomicReference`, `CopyOnWriteArrayList`) hoặc cơ chế khóa (`synchronized` cho luồng OS, `Mutex` cho Coroutines).

---

### KOTLIN-04 | Sự khác nhau giữa `==` và `===` trong Kotlin
- **Bản chất:**
  - `==` (**Structural Equality — So sánh giá trị**): Dịch thành `a?.equals(b) ?: (b === null)`. An toàn với `null` và so sánh nội dung thông qua `equals()`.
  - `===` (**Referential Equality — So sánh tham chiếu**): Kiểm tra xem hai tham chiếu có trỏ tới cùng một địa chỉ đối tượng trên Heap hay không (tương đương `==` đối với kiểu tham chiếu trong Java).
- **Lưu ý với kiểu số:**
  - Với `Int`, `Long`, `Boolean` (non-null), Kotlin biên dịch thành kiểu nguyên thủy JVM nên `==` và `===` cho kết quả như nhau (Kotlin 2.0+ khuyến cáo không dùng `===` trên kiểu nguyên thủy/boxed số).
  - Với `Int?` (nullable), giá trị bị boxing thành `java.lang.Integer`, nên `===` phụ thuộc vào vùng nhớ đệm Integer Cache (`-128..127`) của JVM. Vì vậy, luôn dùng `==` khi so sánh giá trị.

---

### KOTLIN-05 | Kiểu nguyên thủy (Primitives) và Mảng trong Kotlin dưới góc nhìn JVM Bytecode
- **Bản chất:**
  - Trên mặt cú pháp Kotlin, mọi kiểu (`Int`, `Double`, `Boolean`) đều hành xử thống nhất như lớp đối tượng.
  - Khi biên dịch ra JVM Bytecode:
    - Kiểu **Non-null** (`Int`, `Long`, `Double`, `Boolean`) được tối ưu thành **primitive** (`int`, `long`, `double`, `boolean`) trừ khi dùng làm tham số kiểu Generic (`List<Int>`).
    - Kiểu **Nullable** (`Int?`, `Double?`) được **boxing** thành `java.lang.Integer`, `java.lang.Double`.
  - Với mảng: `Array<Int>` biên dịch thành `Integer[]` (mảng các đối tượng boxed), còn `IntArray`, `LongArray`, `DoubleArray` biên dịch thành `int[]`, `long[]`, `double[]` nguyên thủy liên tục trong bộ nhớ.

---

### KOTLIN-06 | Hệ thống kiểu `Any`, `Unit` và `Nothing`
- **Bản chất:**
  - **`Any`**: Lớp cha gốc của mọi kiểu non-null trong Kotlin (ánh xạ xuống `java.lang.Object` trên JVM). Kiểu gốc nhận cả `null` là `Any?`.
  - **`Unit`**: Tương đương `void` của Java nhưng là một kiểu dữ liệu thật (singleton `object`), giúp các hàm không trả về giá trị vẫn tương thích mượt mà với Generics (ví dụ `(T) -> Unit`).
  - **`Nothing`**: Là **Bottom Type** (kiểu con của mọi kiểu trong Kotlin) và không có bất kỳ instance nào. Biểu thị một biểu thức không bao giờ trả về bình thường (`throw Exception(...)`, `return`).
- **Ứng dụng thực tế:**
  Nhờ `throw` và `return` có kiểu `Nothing`, bạn có thể viết guard clause ngắn gọn với toán tử Elvis `?:`:
  ```kotlin
  val order = orderRepository.findByIdOrNull(id)
      ?: throw OrderNotFoundException("Khong tim thay don $id")
  // Trình biên dịch suy luận vế phải là Nothing -> `order` tự động mang kiểu non-null `Order`!
  ```

---

## PHẦN 2: NULL SAFETY, SCOPE FUNCTIONS, HÀM & CLOSURE

### KOTLIN-07 | Null Safety, Smart Cast (`is`) và khi nào Smart Cast không hoạt động?
- **Các toán tử cốt lõi:**
  - Safe call `?.`, Elvis `?:`, Safe cast `as?`, và hàm kiểm tra điều kiện `requireNotNull(value) { "Loi..." }` (ưu tiên dùng thay cho `!!` để có thông báo lỗi ngữ cảnh rõ ràng).
- **Khi nào Smart Cast (`if (x != null)` hoặc `if (x is String)`) KHÔNG hoạt động?**
  - Trình biên dịch chỉ cho phép Smart Cast khi nó **đảm bảo được biến không thể bị thay đổi giá trị** giữa thời điểm kiểm tra (`if`) và thời điểm sử dụng:
    - Biến cục bộ `val`, hoặc biến cục bộ `var` không bị sửa đổi trong nhánh đó và không bị thay đổi bởi lambda: **Smart Cast được**.
    - Thuộc tính `var` của class, hoặc thuộc tính `val` có custom getter / `open val`: **KHÔNG Smart Cast được** (vì một luồng khác có thể gán lại `var`, hoặc custom getter có thể trả về kết quả khác ở lần gọi sau). Cách xử lý chuẩn là gán vào một biến cục bộ `val temp = obj.mutableProp` hoặc dùng `obj.mutableProp?.let { ... }`.

---

### KOTLIN-08 | Chọn đúng trong 5 Scope Functions (`let`, `also`, `apply`, `run`, `with`)
| Hàm | Tham chiếu đối tượng | Giá trị trả về | Ngữ cảnh sử dụng phù hợp |
| :--- | :---: | :---: | :--- |
| **`let`** | `it` (có thể đặt tên lại) | Kết quả của Lambda | Chạy khối lệnh khi khác null (`value?.let { ... }`) hoặc ánh xạ đối tượng sang kết quả mới. |
| **`also`** | `it` (có thể đặt tên lại) | Chính đối tượng gốc | Thực hiện tác vụ phụ (ghi log, kiểm tra, debug) mà không làm đứt chuỗi gọi hàm. |
| **`apply`** | `this` (có thể lược bỏ) | Chính đối tượng gốc | Cấu hình các thuộc tính của đối tượng vừa khởi tạo (`Config().apply { ... }`). |
| **`run`** | `this` (có thể lược bỏ) | Kết quả của Lambda | Vừa thao tác trên `this` vừa tính toán trả về một kết quả mới. |
| **`with`** | `this` (có thể lược bỏ) | Kết quả của Lambda | Gom nhóm nhiều lời gọi phương thức trên một đối tượng non-null: `with(builder) { ... }`. |
- **Lưu ý:** Tránh lồng nhiều `let`/`apply`/`run` chồng chéo làm rối con trỏ `this` và `it`.

---

### KOTLIN-09 | Phân biệt `lateinit var` và `by lazy`
- **`lateinit var`**: Dùng cho thuộc tính `var` non-null (không áp dụng cho kiểu nguyên thủy như `Int`, `Boolean`), cho phép trì hoãn việc gán giá trị sau khi constructor chạy xong (thường gặp trong test setup `@BeforeEach` hoặc framework injection). Nếu đọc trước khi gán sẽ ném `UninitializedPropertyAccessException`.
- **`by lazy { ... }`**: Dùng cho thuộc tính `val`, tính toán giá trị vào lần đầu tiên được đọc và lưu kết quả dùng lại. Mặc định `lazy` đồng bộ hóa (`LazyThreadSafetyMode.SYNCHRONIZED`) để an toàn khi nhiều luồng cùng truy cập lần đầu.

---

### KOTLIN-10 | Bản chất của Extension Functions: Phân giải tĩnh (Static Dispatch)
- **Bản chất:**
  - Extension Function (`fun String.toSlug(): String`) biên dịch thành một phương thức `public static` nhận đối tượng mở rộng (receiver) làm tham số đầu tiên.
  - Nó không sửa đổi lớp gốc và không truy cập được thành viên `private`/`protected` của lớp gốc.
- **Lưu ý phỏng vấn:**
  - Vì là phương thức tĩnh ở mức Bytecode, Extension Function được quyết định lúc **compile-time dựa trên kiểu khai báo của biến**, không có tính đa hình động (runtime polymorphism) như phương thức `open`/`override` bên trong class.

---

### KOTLIN-11 | `inline`, `noinline`, `crossinline`, Non-local Return & Closure Capture
- **`inline` function và đánh đổi (trade-offs):**
  - Khi đánh dấu hàm bậc cao (nhận lambda) là `inline`, trình biên dịch chép thân hàm và thân lambda trực tiếp vào nơi gọi, giúp tránh tạo đối tượng `FunctionN` ở mức bytecode (lưu ý với non-inline capturing lambda, dù bytecode tạo đối tượng `FunctionN`, JIT của JVM vẫn có thể tối ưu qua Escape Analysis tùy ngữ cảnh), tránh tạo hộp `Ref.*` khi cập nhật biến cục bộ `var` trong lambda được inline, và cho phép dùng `reified` cũng như non-local `return`.
  - **Giới hạn & đánh đổi:** Không nên dùng `inline` cho hàm thường không nhận lambda (chỉ làm phình kích thước bytecode vô ích); không nên inline hàm có thân quá dài; hàm đệ quy không thể inline.
- **`noinline` và `crossinline`:**
  - `noinline`: Giữ lại một tham số lambda dưới dạng đối tượng `FunctionN` bình thường để có thể lưu vào biến hoặc truyền tiếp cho hàm không inline.
  - `crossinline`: Dùng khi lambda được inline nhưng lại được gọi bên trong một ngữ cảnh thực thi khác (như `Runnable` hoặc object ẩn danh), nơi không cho phép nhảy `return` thoát khỏi hàm ngoài.
- **Local Return vs Non-local Return (Lambda vs Anonymous Function):**
  - Quy tắc cốt lõi của Kotlin: lệnh `return` không nhãn luôn thoát khỏi hàm gần nhất được khai báo bằng từ khóa **`fun`**.
  - Do đó, `return` trần bên trong một `inline` lambda (như `list.forEach { if (...) return }`) là **non-local return** (thoát luôn khỏi hàm `fun` bao ngoài!).
  - Muốn chỉ thoát khỏi bước lặp hiện tại của lambda, dùng **local return có nhãn** (`return@forEach`) hoặc dùng **Anonymous Function** (`list.forEach(fun(item) { if (...) return })`).

---

## PHẦN 3: OOP, SPECIAL CLASSES & BẤT ĐỒNG BỘ TRONG BACKEND

### KOTLIN-12 | Nested Class vs Inner Class (`this@Outer`) — Sự đảo ngược giữa Java và Kotlin
- **Bản chất:**
  - **Trong Java:** Một `class Inner` khai báo bên trong `class Outer` mặc định là **non-static** (luôn ngầm giữ một tham chiếu ẩn `this$0` trỏ tới đối tượng `Outer`). Nếu quên từ khóa `static`, việc giữ đối tượng con lâu dài rất dễ gây rò rỉ bộ nhớ (memory leak) đối tượng `Outer`.
  - **Trong Kotlin (đảo ngược lại để an toàn mặc định):**
    - `class Outer { class Nested }`: Mặc định tương đương **`static class`** của Java! Nó **không** giữ tham chiếu tới `Outer` và không truy cập được thuộc tính instance của `Outer` (rất phù hợp để gom nhóm các class DTO Request/Response hoặc Builder).
    - `class Outer { inner class Inner }`: Chỉ khi thêm từ khóa `inner`, lớp con mới giữ tham chiếu tới `Outer` và dùng được `this@Outer`.

---

### KOTLIN-13 | `final` mặc định, `kotlin("plugin.spring")`, `kotlin("plugin.jpa")` và cơ chế Proxy
- **Bản chất trên Kotlin `2.0.21`:**
  - Mọi `class` và phương thức trong Kotlin mặc định là `final`.
  - Trong Spring, các tính năng AOP (`@Transactional`, `@Async`, `@Cacheable`) và `@Configuration` hoạt động qua **Proxy**:
    - Spring hỗ trợ cả **JDK Dynamic Proxy** (dựa trên interface) và **CGLIB Proxy** (tạo lớp con kế thừa lớp gốc; đây là chế độ mặc định trong Spring Boot với `proxyTargetClass = true`).
    - Khi tạo CGLIB subclass proxy, lớp và phương thức đích không được là `final`.
- **Phân biệt rõ 2 compiler plugins:**
  1. **`kotlin("plugin.spring")` (cấu hình sẵn của `all-open`):** Tự động mở `open` ở mức bytecode cho các class/method có gắn `@Component`, `@Service`, `@Repository`, `@Controller`, `@Configuration`, `@Transactional`, `@Async`, `@Cacheable`...
  2. **`kotlin("plugin.jpa")` (cấu hình sẵn của `no-arg`):** Tự động sinh một **constructor không tham số (no-arg constructor)** ở mức bytecode cho các class gắn `@Entity`, `@Embeddable`, `@MappedSuperclass` để JPA/Hibernate khởi tạo qua Reflection.
     *(Lưu ý: `plugin.jpa` chỉ sinh no-arg constructor, không tự mở `open` cho `@Entity`; nếu muốn Hibernate tạo lazy-loading proxy cho `@Entity` mà không tự gõ `open`, cần cấu hình thêm `allOpen { annotation("jakarta.persistence.Entity") }` trong `build.gradle.kts` hoặc khai báo `open` trên entity).*

---

### KOTLIN-14 | `data class` cho DTO và những lưu ý quan trọng với JPA `@Entity`
- **Với DTO (Request / Response):**
  - `data class` với các thuộc tính `val` là lựa chọn rất phổ biến nhờ tự sinh `equals`, `hashCode`, `toString`, `copy`, `componentN`. Tuy nhiên, cần nhớ đây chỉ là **shallow immutability** (nếu bên trong chứa `MutableList` hay mảng `Array`, dữ liệu bên trong vẫn sửa được).
- **Vì sao không nên dùng `data class` cho JPA `@Entity`?**
  1. `data class` không thể là `open`, gây cản trở cơ chế tạo proxy lazy-loading của Hibernate.
  2. `toString()`, `equals()`, `hashCode()` tự sinh của `data class` truy cập toàn bộ thuộc tính trong primary constructor — nếu có quan hệ `@OneToMany` / `@ManyToOne(fetch = LAZY)`, nó dễ kích hoạt truy vấn ngoài ý muốn (`LazyInitializationException`) hoặc đệ quy hai chiều (`StackOverflowError`).
- **Cảnh báo khi viết `equals`/`hashCode` cho JPA `@Entity` có ID tự sinh (`@GeneratedValue`):**
  - Không áp dụng máy móc `override fun hashCode() = id.hashCode()` khi `var id: Long? = null` được DB cấp phát sau khi `persist()`! Khi `id` đổi từ `null` sang số cụ thể, `hashCode` bị thay đổi lúc đối tượng đang nằm trong `HashSet`/`HashMap`, làm hỏng cấu trúc bảng băm.
  - **Cách tiếp cận an toàn & lưu ý về Hibernate Proxy:**
    - Dùng khóa tự nhiên bất biến (`@NaturalId val businessCode: String` hoặc `UUID` gán ngay từ đầu).
    - Hoặc kết hợp `equals` kiểm tra `id != null && id == other.id` với mã băm hằng số theo lớp thực thể (đánh đổi: các entity cùng lớp dùng chung 1 bucket băm, nên tránh nhét hàng chục nghìn bản ghi vào cùng một `HashSet`). **Lưu ý giới hạn với Hibernate Subclass Proxy:** Trong Hibernate thực tế, thực thể thật và đối tượng Lazy Proxy của nó có `javaClass` khác nhau; nếu `equals` coi chúng bằng nhau nhưng `hashCode()` chỉ gọi `javaClass.hashCode()` trực tiếp thì sẽ vi phạm hợp đồng `equals`/`hashCode`. Do đó, ở giai đoạn làm việc với Hibernate thật, cần dùng chiến lược equality có xét proxy (như `HibernateProxy` / `Hibernate.getClass(this)`).

---

### KOTLIN-15 | Khi nào dùng `enum class`, `sealed interface/class` và `@JvmInline value class`?
- **`enum class`**: Tập hợp hữu hạn các hằng số cố định, mỗi hằng số chỉ có duy nhất 1 instance.
- **`sealed class` / `sealed interface`**: Tập hợp đóng các kiểu con biết trước lúc biên dịch, trong đó mỗi kiểu con có thể là `data class` (mang dữ liệu riêng cho từng nhánh như `Success(val data: T)`, `ValidationError(val errors: List<String>)`) hoặc `data object`. Giúp biểu thức `when` kiểm tra vét cạn mà không cần `else`.
- **`@JvmInline value class`**: Bọc một giá trị đơn (như `CustomerId(val value: String)`) để trình biên dịch kiểm tra an toàn kiểu (không truyền nhầm `ProductId` vào chỗ `CustomerId`), trong khi lúc chạy thường được mở hộp (unbox) về kiểu cơ bản `String` để giảm cấp phát đối tượng trên Heap.

---

## PHẦN 4: COLLECTIONS, GENERICS, ANNOTATIONS & COROUTINES

### KOTLIN-16 | `Iterable` vs `Sequence` và hiểu đúng về `associateBy` với bài toán N+1 Query
- **`Iterable` (Eager) vs `Sequence` (Lazy):**
  - Các phép toán trực tiếp trên `Iterable` (`list.filter {...}.map {...}`) thực thi tức thời theo từng bước và tạo danh sách trung gian sau mỗi bước (phù hợp với danh sách nhỏ/vừa hoặc ít bước).
  - `Sequence` (`list.asSequence().filter {...}.map {...}.toList()`) thực thi lười biếng (lazy) từng phần tử xuyên qua toàn bộ chuỗi và không tạo danh sách trung gian (hữu ích khi chuỗi nhiều bước trên tập dữ liệu lớn hoặc có toán tử dừng sớm như `firstOrNull()`, `take(k)`).
- **Hiểu đúng về `associateBy` và N+1 Query:**
  - Bản thân `associateBy { it.id }` chỉ là phép biến đổi **trong bộ nhớ (in-memory)** giúp gom `List` thành `Map` để giảm chi phí tìm kiếm ghép nối trên RAM từ $O(N \times M)$ xuống $O(N + M)$.
  - Ở tầng Database/ORM, `associateBy` **không tự động chặn N+1 query** nếu bạn không chủ động thay đổi chiến lược truy vấn (ví dụ: gom danh sách ID rồi gọi 1 câu truy vấn lô `WHERE id IN (...)` trước khi dùng `associateBy`, hoặc sử dụng `JOIN FETCH`, `@EntityGraph`, `@BatchSize`, DTO Projection của JPA).

---

### KOTLIN-17 | Generics Variance (`out`/`in`), Type Erasure theo Upper Bound & Giới hạn của `reified`
- **Type Erasure theo Upper Bound:**
  - Trên JVM, tham số kiểu `<T>` không phải lúc nào cũng xóa thành `java.lang.Object`. Nó được xóa về **Upper Bound hiệu lực** của nó: nếu không khai báo ràng buộc (`<T>`), nó xóa về `Object` (`Any?`); nhưng nếu khai báo `<T : Number>` hoặc `<T : Animal>`, ở bytecode chữ ký sẽ dùng `Number` hoặc `Animal`.
- **Giới hạn của `inline fun <reified T>` với Nested Generic Types:**
  - `reified` giúp giữ lại kiểu `T` tại vị trí gọi hàm được inline. Tuy nhiên, nếu `T` là một kiểu generic lồng nhau như `List<String>`, thì `value is T` ở runtime chỉ kiểm tra được `value is List<*>`, **không tự động kiểm tra mọi phần tử bên trong có phải là `String` hay không** (vì bản thân đối tượng `ArrayList` trên JVM vẫn bị xóa thông tin kiểu phần tử).
- **Phân biệt `T::class` với Type Token (`TypeReference<T>` / `typeOf<T>()`):**
  - Khi các thư viện JSON như Jackson cung cấp hàm `mapper.readValue<List<UserDto>>(json)`, chúng không chỉ dùng `T::class.java` (vì `T::class.java` chỉ ra `List.class`, mất thông tin `UserDto`). Thay vào đó, hàm `inline reified` tạo một lớp ẩn danh kế thừa `TypeReference<T>()` tại điểm gọi để lưu metadata kiểu đầy đủ `List<UserDto>` vào bytecode của lớp ẩn danh đó, rồi Jackson vẫn dùng reflection/metadata ở runtime để đọc và khởi tạo từng `UserDto`. Vì vậy, `reified` giúp code gọn và giữ được metadata kiểu tại call-site, chứ không xóa bỏ hoàn toàn reflection của các framework JSON/ORM.

---

### KOTLIN-18 | Annotation Use-Site Targets (`@field:`, `@get:`, `@param:`) trên Primary Constructor
- **Bản chất:**
  - Một khai báo thuộc tính trong primary constructor (`val email: String`) có thể tương ứng với 3 vị trí trong bytecode: tham số hàm tạo (`@param:`), biến trường ẩn (`@field:`), và hàm đọc (`@get:`).
  - Trong Kotlin `2.0.21` (với cấu hình mặc định hiện tại), khi một annotation cho phép gắn lên nhiều vị trí (như `@NotBlank`, `@Email`, `@Valid` của Jakarta Validation cho phép cả `PARAMETER`, `FIELD`, `METHOD`), Kotlin chọn mục tiêu mặc định theo thứ tự ưu tiên: **`param` > `property` > `field`**.
  - Do đó, nếu viết `@NotBlank val email: String` mà không chỉ định target, annotation sẽ nằm ở **tham số constructor (`@param:`)**. Với các bộ kiểm tra quét trên **Field (`@field:`)** hoặc **Getter (`@get:`)**, ràng buộc đó sẽ không được kích hoạt trừ khi bạn viết rõ `@field:NotBlank`, `@field:Valid`.
  - *(Lưu ý phiên bản: Các phiên bản Kotlin tương lai có lộ trình thay đổi cơ chế mặc định qua cờ `-Xannotation-default-target=param-property`, nhưng việc ghi tường minh `@field:` hoặc `@get:` vẫn là thói quen rõ ràng và an toàn nhất).*

---

### KOTLIN-19 | Hiểu đúng về `suspend fun`, Cancellation, Concurrency vs Parallelism và quản lý vòng đời Coroutines
- **`suspend` không tự động làm code non-blocking, không tự đổi sang background thread, và không tự kiểm tra hủy (cancellation):**
  - Đánh dấu một hàm là `suspend` chỉ cho phép trình biên dịch biến đổi hàm đó theo cơ chế CPS (Continuation Passing Style) để nó **có khả năng tạm dừng** tại các điểm treo (suspension points).
  - Nếu bên trong `suspend fun` bạn gọi một hàm chặn luồng như `Thread.sleep(...)` hoặc đọc I/O đồng bộ mà không chuyển dispatcher, nó vẫn chạy ngay trên luồng của người gọi và **chặn cứng luồng đó**.
  - Tương tự, `suspend` không tự động kiểm tra trạng thái hủy của Coroutine: chỉ những hàm tạm dừng có hỗ trợ hủy (như `delay()`, `yield()`, các hàm dùng `suspendCancellableCoroutine`, hoặc hàm chủ động gọi `ensureActive()`) mới kiểm tra và ném `CancellationException` khi Coroutine đã bị hủy. Với `Flow.catch`, toán tử này chỉ bắt ngoại lệ từ upstream và không bắt các ngoại lệ phát sinh nhằm hủy luồng (`Flow` cancellation).
- **Concurrency (Đồng thời) vs Parallelism (Song song):**
  - Chạy 1.000 coroutines cùng lúc hoặc dùng `Flow.buffer()` trên **1 thread duy nhất** (như `runBlocking` đơn luồng) là **Concurrency**: các coroutine đan xen nhau sử dụng CPU khi coroutine khác đang tạm dừng chờ (`delay`).
  - **Parallelism** là khi nhiều tác vụ thực sự chạy cùng một thời điểm vật lý trên **nhiều nhân CPU** (như khi chạy trên thread pool đa luồng `Dispatchers.Default`).
- **Vòng đời của `launch` (`Job`):**
  - Không nên hiểu `launch` là "bắn ra rồi bỏ mặc". `launch` dùng cho tác vụ không trả về giá trị kết quả (trả về `Job` thay vì `Deferred<T>` của `async`), nhưng nó vẫn **chịu sự quản lý chặt chẽ của `CoroutineScope` cha (Structured Concurrency)**: scope cha sẽ đợi `Job` hoàn tất trước khi đóng, và nếu `Job` ném ngoại lệ không bắt trong `coroutineScope`, nó sẽ hủy cả các coroutine anh em.

---

### KOTLIN-20 | Spring MVC + JPA (Blocking) vs Reactive/Coroutines, Dispatchers & Transactions
- **Tránh quy tắc máy móc *"Luôn bọc mọi lời gọi JPA trong `withContext(Dispatchers.IO)`"*:**
  1. **Trong ứng dụng Spring MVC truyền thống (hoặc dùng Virtual Threads trên JDK 21+) + Spring Data JPA (JDBC):**
     - Mỗi HTTP request vốn đã chạy trên một worker thread riêng; bạn viết hàm bình thường (không cần `suspend`) và gọi thẳng JPA Repository.
     - Việc bọc lời gọi JDBC vào `withContext(Dispatchers.IO)` **không biến JDBC thành non-blocking**, mà chỉ đẩy việc chờ đợi từ thread hiện tại sang một thread khác trong pool `Dispatchers.IO`.
  2. **Rủi ro với `@Transactional` truyền thống (Thread-bound Transaction):**
     - Quản lý giao dịch JDBC truyền thống của Spring (`PlatformTransactionManager`) lưu trữ kết nối và trạng thái giao dịch trong **`ThreadLocal`** của luồng hiện tại.
     - Nếu đang ở trong một hàm `@Transactional` gắn với luồng A mà bạn dùng `withContext(Dispatchers.IO)` hay `async` nhảy sang luồng B, ngữ cảnh giao dịch trong `ThreadLocal` **không tự động đi theo sang luồng B**, dẫn đến việc truy vấn chạy ngoài transaction hoặc lỗi đóng kết nối!
  3. **Khi nào dùng Coroutines / `Flow` trong Spring Boot?**
     - Khi xây dựng hệ thống trên nền **Spring WebFlux**, gọi song song nhiều HTTP API bất đồng bộ bằng `WebClient`, hoặc truy cập DB qua trình điều khiển bất đồng bộ **R2DBC** (sử dụng `ReactorContext` và `TransactionalOperator` thay cho `ThreadLocal`).
