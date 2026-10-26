package view

/**
 * =====================================================================
 * LESSON 04 - EX06: HÀM `infix` & XÂY DỰNG KOTLIN DSL (LAMBDA WITH RECEIVER)
 * =====================================================================
 *
 * 1. BẢN CHẤT CỦA HÀM `infix`:
 *    - Một hàm thành viên (member function) hoặc hàm mở rộng (extension function) có **ĐÚNG 1 tham số**
 *      khi thêm từ khóa `infix` ở đầu sẽ cho phép gọi theo cú pháp trung tố (bỏ dấu chấm `.` và dấu ngoặc tròn `()`):
 *      + Gọi thường: `1.until(10)`, `"key".to("value")`, `amount.shouldBeGreaterThan(0)`
 *      + Gọi `infix`: `1 until 10`, `"key" to "value"`, `amount shouldBeGreaterThan 0`
 *    - Điều kiện của hàm `infix`:
 *      1) Phải là phương thức của class hoặc Extension Function.
 *      2) Phải có **đúng 1 tham số duy nhất**.
 *      3) Tham số đó không được là `vararg` và không có giá trị mặc định.
 *
 * 2. BẢN CHẤT CỦA KOTLIN TYPE-SAFE DSL (LAMBDA WITH RECEIVER `T.() -> Unit`):
 *    - Khi mới nhìn vào file `build.gradle.kts`, Spring Security Kotlin DSL, hay Spring WebFlux Router:
 *      ```kotlin
 *      route("/api/v1/users") {
 *          get("/profile")
 *          post("/create")
 *      }
 *      ```
 *      Nhiều lập trình viên Java thắc mắc: "Đây là file cấu hình đặc biệt hay cú pháp ma thuật gì?"
 *    - **Câu trả lời**: Đó là **code Kotlin thuần túy** được tạo bởi sự kết hợp giữa:
 *      1) **Trailing Lambda** (đưa `{ ... }` ra ngoài ngoặc tròn).
 *      2) **Lambda with Receiver (`block: RouteBuilder.() -> Unit`)**: Thay vì truyền `(RouteBuilder) -> Unit`
 *         (bắt người dùng phải gõ `it.get(...)`), kiểu `RouteBuilder.() -> Unit` biến đối tượng `RouteBuilder`
 *         thành con trỏ **`this`** bên trong khối `{ ... }`!
 *         Nhờ vậy người dùng có thể gọi trực tiếp `get("/profile")` và `post("/create")` bên trong `{ ... }`!
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong Spring Boot 3+ / Spring Security 6+:
 *      `http { authorizeHttpRequests { authorize("/api/public/all", permitAll) } }`
 *      chính là Lambda with Receiver!
 *    - Trong Kotest / AssertJ Kotlin: `response.statusCode shouldBe 200` chính là hàm `infix`!
 * =====================================================================
 */

// =====================================================================
// PHẦN 1: ĐỊNH NGHĨA CÁC HÀM `infix`
// =====================================================================

// Lặp chuỗi `n` lần bằng cú pháp trung tố: `3 timesRepeat "Ha"`
private infix fun Int.timesRepeat(text: String): String = text.repeat(this)

// Mô phỏng thư viện Assertion trong Unit Test (như Kotest `actual shouldBe expected`)
private infix fun <T> T.shouldBe(expected: T) {
    if (this == expected) {
        println("   -> [ASSERT PASS] Gia tri thuc te ($this) khop voi ky vong ($expected)")
    } else {
        throw AssertionError("Ky vong $expected nhung nhan duoc $this")
    }
}

// =====================================================================
// PHẦN 2: XÂY DỰNG MINI SPRING ROUTER DSL BẰNG LAMBDA WITH RECEIVER
// =====================================================================

private data class EndpointDefinition(
    val method: String,
    val fullPath: String,
    val description: String
)

// Class đóng vai trò là "Receiver" (`this`) bên trong khối ngoặc nhọn `{ ... }` của DSL
private class RouteBuilder(private val basePath: String) {
    private val endpoints = mutableListOf<EndpointDefinition>()

    fun get(subPath: String, description: String = "GET Handler") {
        endpoints.add(EndpointDefinition("GET", "$basePath$subPath", description))
    }

    fun post(subPath: String, description: String = "POST Handler") {
        endpoints.add(EndpointDefinition("POST", "$basePath$subPath", description))
    }

    fun delete(subPath: String, description: String = "DELETE Handler") {
        endpoints.add(EndpointDefinition("DELETE", "$basePath$subPath", description))
    }

    fun build(): List<EndpointDefinition> = endpoints.toList()
}

/**
 * Hàm khởi tạo DSL `route(...)`:
 * Chú ý chữ ký của tham số thứ 2: `block: RouteBuilder.() -> Unit` (Lambda with Receiver!).
 * Khi gọi `builder.block()`, mọi lời gọi `get(...)`, `post(...)` bên trong `{ ... }` sẽ gọi thẳng vào `builder`!
 */
private fun route(basePath: String, block: RouteBuilder.() -> Unit): List<EndpointDefinition> {
    val builder = RouteBuilder(basePath)
    builder.block() // Thực thi Lambda với `this` chính là `builder`!
    return builder.build()
}

fun main() {
    println("--- ___ * START: Ex06InfixAndDslPreview * ___ ---\n")

    // =================================================================
    // 1. Sử dụng hàm `infix` tự định nghĩa và hàm `infix` chuẩn
    // =================================================================
    println("1. [Infix Functions] Goi ham khong can dau cham `.` va dau ngoac `()`:")
    val banner = 3 timesRepeat "Kotlin! "
    println("   -> Ket qua `3 timesRepeat \"Kotlin! \"`: $banner")

    // Infix `to` tạo Pair và Infix `shouldBe` trong Unit Test DSL:
    val statusPair = "HTTP_OK" to 200
    statusPair.second shouldBe 200
    println()

    // =================================================================
    // 2. Giải mã `buildString` (Lambda with Receiver `StringBuilder.() -> Unit` có sẵn)
    // =================================================================
    println("2. [Standard Library DSL: buildString] Ben trong `{ ... }`, `this` chinh la `StringBuilder`:")
    val sqlQuery = buildString {
        // `append` và `appendLine` là phương thức của `this` (StringBuilder)
        appendLine("SELECT u.id, u.email")
        appendLine("FROM users u")
        append("WHERE u.is_active = true;")
    }
    println(sqlQuery.prependIndent("   "))
    println()

    // =================================================================
    // 3. Tự tay sử dụng Mini Spring Router DSL vừa xây dựng ở trên!
    // =================================================================
    println("3. [Custom Spring Router DSL] Cach hoat dong cua `route(\"/api/v1/users\") { ... }`:")

    // Nhìn đoạn code dưới đây: giống hệt Spring WebMVC.fn / WebFlux Router DSL và build.gradle.kts!
    val userRoutes: List<EndpointDefinition> = route("/api/v1/users") {
        get("/profile", description = "Lay thong tin ho so nguoi dung hien tai")
        post("/create", description = "Dang ky tai khoan nguoi dung moi")
        delete("/{id}", description = "Xoa tai khoan theo ID")
    }
    check(userRoutes.size == 3 && userRoutes[0].fullPath == "/api/v1/users/profile")

    println("   -> Cac Endpoint da dang ky thanh cong qua Kotlin DSL:")
    for (ep in userRoutes) {
        println("      [${ep.method.padEnd(6)}] ${ep.fullPath.padEnd(25)} -> ${ep.description}")
    }

    println("\n--- ___ * END: Ex06InfixAndDslPreview * ___ ---")
}
