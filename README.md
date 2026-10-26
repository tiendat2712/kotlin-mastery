# 🚀 Kotlin Journey — Từ Java Core Đến Làm Chủ Kotlin & Spring Boot

Dự án học **Kotlin Core** được thiết kế riêng cho lập trình viên đã có nền tảng **Java Core** (tương ứng với lộ trình tại `D:\Java_Journey`), đóng vai trò là bước đệm vững chắc trước khi bước sang **Spring Boot với Kotlin**.

---

## 📂 Cấu Trúc Dự Án (12 Modules)

Mỗi bài học (`lesson01` $\rightarrow$ `lesson12`) là một subproject Gradle độc lập, giữ nguyên cấu trúc thư mục `src/` ngắn gọn, dễ nhìn giống hệt `D:\Java_Journey`:

| Module | Chủ đề trọng tâm | Đối chiếu từ `D:\Java_Journey` | Ứng dụng trong Spring Boot Kotlin |
| :--- | :--- | :--- | :--- |
| **`lesson01-basics-and-types`** | `fun main`, `val`/`var`/`const val`, Kiểu dữ liệu, `Any`/`Unit`/`Nothing`, Chuỗi, String Template, Multiline String, `==` vs `===`, Operator Overloading | `lesson02-data-structure`, `lesson07-libraries`, `lesson08-string` | Viết câu `@Query` SQL/JPQL nhiều dòng với `"""..."""`, tính toán tiền tệ `BigDecimal` bằng toán tử `+`, `-`, `*`, so sánh giá trị an toàn với `==` |
| **`lesson02-null-safety-and-scope`** | Nullable (`?`), Safe Call (`?.`), Elvis (`?:`), `!!`, Smart Cast (`is`), Safe Cast (`as?`), 5 Scope Functions (`let`, `also`, `apply`, `run`, `with`), `lateinit var`, `by lazy` | Khắc phục `NullPointerException` kinh điển của Java | Xử lý kết quả từ Repository (`findByIdOrNull(id) ?: throw NotFoundException()`), cấu hình Bean/Test với `apply`, `lateinit var` |
| **`lesson03-control-flow-and-loops`** | `Boolean?`, `if-else` Expression, `when` Expression nâng cao, Ranges (`..`, `..<`, `downTo`, `step`), Vòng lặp `for`/`while`, Labeled `break@`/`return@` | `lesson02-data-structure/src/controlling` | Dùng `when` expression để điều hướng trạng thái đơn hàng, phân quyền hoặc ánh xạ HTTP Status Code |
| **`lesson04-functions-and-extensions`** | Single-expression function, Default & Named Arguments, `Pair`/`Triple` & Destructuring, Higher-Order Functions, Trailing Lambda, **Extension Functions**, Infix & Kotlin DSL | `lesson00-design-pattern` (Builder), `lesson04-oop-features` (Overload, Functional Interface) | Thay thế Builder Pattern bằng Named/Default Args; dùng **Extension Functions** (`fun UserEntity.toDto()`) làm Mapper chuẩn trong Spring |
| **`lesson05-oop-classes-and-objects`** | Class, Properties (Getter/Setter ngầm định & custom `field`), Primary/Secondary Constructor, `init`, Visibility (`internal`), `open` vs `final`, Interfaces & `fun interface` | `lesson03-oop`, `lesson04-oop-features` | Hiểu tại sao class trong Kotlin mặc định là `final` và tại sao Spring AOP / `@Transactional` cần plugin `kotlin("plugin.spring")` (`all-open`) |
| **`lesson06-special-classes-and-delegation`** | `data class` (`copy`, `componentN`), `enum class`, `sealed class` & `sealed interface`, `object` (Singleton), `companion object`, `@JvmInline value class`, Delegation (`by`) | `lesson00-design-pattern`, `lesson03-oop` | Dùng `data class` cho Request/Response DTO (và tránh bẫy dùng `data class` cho JPA `@Entity`); dùng `sealed interface` cho kết quả nghiệp vụ |
| **`lesson07-arrays-and-collections`** | `IntArray` vs `Array<Int>`, Phân tách **Read-only (`List`, `Set`, `Map`)** và **Mutable (`MutableList`, `MutableSet`, `MutableMap`)**, Destructuring | `lesson10-arrays`, `lesson11-colection` | Bảo vệ tính bất biến (Immutability) của dữ liệu trả về từ tầng Service/Domain |
| **`lesson08-collection-pipelines-and-sequences`** | `filter`, `map`, `flatMap`, `associateBy`, `groupBy`, `partition`, `sumOf`, `fold`, `sortedWith(compareBy...)`, **`Sequence` (`asSequence()`)**, giải bài tập `TransactionApp` | `lesson11-colection`, `lesson12-stream-api` | Xử lý dữ liệu trực tiếp trên Collection không cần `.stream()...collect()`, gom nhóm và tạo Map tra cứu $O(1)$ tránh lỗi N+1 Query |
| **`lesson09-generics-exceptions-annotations`** | Generics Variance (`out`/`in`), **`inline` + `reified`**, Unchecked Exceptions, `require`/`check`/`runCatching`, `.use {}`, **Annotation Use-site Targets (`@field:`)** | `lesson06-exception`, `lesson11-colection/src/generic`, `lesson13-files` | Bắt buộc dùng `@field:NotBlank`, `@field:Valid` khi validate DTO trong Spring Boot; hiểu cách hoạt động của `restClient.body<UserDto>()` nhờ `reified` |
| **`lesson10-coroutines-basics`** | Coroutines vs OS Threads, `suspend fun`, `runBlocking`, `launch`, `async`/`await`, `Dispatchers.IO`/`Default`, Structured Concurrency, `Flow<T>` | `lesson14-thread` | Lập trình bất đồng bộ hiệu năng cao, gọi song song nhiều External API/Microservices, làm chủ Spring WebFlux & Coroutines |
| **`lesson11-java-interop-and-spring-readiness`** | Gọi Java từ Kotlin (Platform Types `T!`, `-Xjsr305=strict`), `@JvmStatic`, `@JvmOverloads`, `@JvmField`, Mô phỏng kiến trúc Controller $\rightarrow$ Service $\rightarrow$ Repository | Tổng hợp chuyển giao Java $\leftrightarrow$ Kotlin | Nắm trọn bộ cấu hình và quy tắc thiết kế một dự án Spring Boot Kotlin chuẩn thực tế |
| **`lesson12-final-assignment`** | **Capstone Project: Nova Shop (Idiomatic Kotlin Edition)** — Quản lý đơn hàng, chuẩn hóa dữ liệu, tìm kiếm phân trang & 5 báo cáo kinh doanh | `lesson15-final-assignment` (Nova Shop Java) | Vận dụng tổng hợp toàn bộ kiến thức Kotlin Core vào một hệ thống nghiệp vụ hoàn chỉnh trước khi ghép nối Database & Spring Web |

---

## 🛠️ Hướng Dẫn Mở Dự Án & Chạy Bài Học

### 1. Mở bằng IntelliJ IDEA (Khuyên dùng)
1. Mở **IntelliJ IDEA** $\rightarrow$ Chọn **Open** $\rightarrow$ Trỏ tới thư mục **`D:\Kotlin_Journey`**.
2. Đợi vài giây để IntelliJ đồng bộ cấu hình Gradle (`build.gradle.kts`).
3. Mở bất kỳ file `Ex01...kt`, `Ex02...kt` nào trong các thư mục `lessonXX.../src/view/` và bấm nút **Run (`▶`)** màu xanh cạnh hàm `fun main()` để chạy thử và quan sát kết quả trên Console!

### 2. Biên dịch kiểm tra bằng dòng lệnh (Gradle Wrapper)
Từ thư mục `D:\Kotlin_Journey`, bạn có thể biên dịch toàn bộ 12 modules bằng lệnh:
```powershell
.\gradlew.bat classes
```

---

## 📖 Tài Liệu Ôn Tập Kèm Theo
- **[`kotlin_core_interview_notes.md`](./kotlin_core_interview_notes.md)**: Sổ tay hệ thống hóa **25 câu hỏi phỏng vấn & bẫy thực chiến** khi chuyển từ Java sang Kotlin & Spring Boot (phân tích sâu dưới góc độ JVM Bytecode).
