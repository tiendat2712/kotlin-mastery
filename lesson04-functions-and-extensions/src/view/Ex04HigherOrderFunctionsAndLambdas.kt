package view

/**
 * =====================================================================
 * LESSON 04 - EX04: HIGHER-ORDER FUNCTIONS, LAMBDA SYNTAX & FUNCTION REFERENCES
 * =====================================================================
 *
 * 1. BẢN CHẤT & SO SÁNH VỚI JAVA (`Ex01FunctionalInterfaceDemo.java` & `Ex02LambdaSyntax.java`):
 *    - Trong Java 8+, hàm KHÔNG có kiểu dữ liệu độc lập! Để truyền một hàm vào phương thức khác,
 *      Java phải mượn các **Functional Interface** rời rạc trong gói `java.util.function`:
 *      `Predicate<T>`, `Function<T, R>`, `BiFunction<T, U, R>`, `Consumer<T>`, `Supplier<T>`...
 *      Và khi gọi phải chấm thêm `.test(...)`, `.apply(...)`, `.accept(...)`, `.get()`.
 *    - Trong Kotlin, **Hàm là Công dân hạng nhất (First-Class Citizen)** và có **Function Type (Kiểu hàm)**
 *      chính thức ngay trong ngôn ngữ:
 *      + `(Int) -> Boolean`          (Thay thế `Predicate<Integer>`)
 *      + `(String) -> Int`           (Thay thế `Function<String, Integer>`)
 *      + `(Int, Int) -> Int`         (Thay thế `BinaryOperator<Integer>` / `BiFunction`)
 *      + `(String) -> Unit`          (Thay thế `Consumer<String>`)
 *      + `() -> Connection`          (Thay thế `Supplier<Connection>`)
 *    - Dưới tầng Bytecode: Kotlin biên dịch `(A) -> B` thành interface chuẩn `kotlin.jvm.functions.Function1<A, B>`
 *      (có từ `Function0` đến `Function22`). Và để gọi hàm, bạn chỉ cần dùng dấu ngoặc tròn `predicate(item)`
 *      trực nhiên như gọi hàm bình thường (tương đương `predicate.invoke(item)`)!
 *
 * 2. QUY TẮC TRAILING LAMBDA & THAM SỐ NGẦM ĐỊNH `it`:
 *    - **Quy tắc Trailing Lambda (Đặc sản cú pháp quan trọng nhất của Kotlin)**:
 *      Nếu tham số **CUỐI CÙNG** của một hàm là một Lambda, bạn được phép **đưa cặp ngoặc nhọn `{ ... }`
 *      ra BÊN NGOÀI dấu ngoặc tròn `(...)`**! Và nếu hàm chỉ có duy nhất 1 tham số là Lambda đó,
 *      bạn được **bỏ luôn cả cặp ngoặc tròn `()`**!
 *    - **Tham số `it`**: Nếu Lambda chỉ có **ĐÚNG 1 tham số**, bạn không cần khai báo `x ->` mà dùng luôn tên mặc định `it`!
 *    - **Lưu ý Cú pháp**: Lambda trong Kotlin **LUÔN nằm trong cặp ngoặc nhọn `{ thamSo -> thanHam }`**
 *      (khác với Java dùng `thamSo -> thanHam` không có ngoặc nhọn).
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Mọi API hiện đại trong Spring Boot Kotlin như `JdbcTemplate.queryForObject(sql) { rs, _ -> ... }`,
 *      `TransactionTemplate.execute { status -> ... }`, hay `WebClient` đều tận dụng Trailing Lambda
 *      và dấu `_` (cho tham số không dùng tới như `rowNum`)!
 * =====================================================================
 */

fun main() {
    println("--- ___ * START: Ex04HigherOrderFunctionsAndLambdas * ___ ---\n")

    // =================================================================
    // 1. Lưu hàm vào biến với Function Type `(A, B) -> R`
    // =================================================================
    println("1. [Function Types] Lưu trữ Lambda vào biến với kiểu hàm `(Int, Int) -> Int`:")
    // Khai báo tường minh kiểu hàm `(Int, Int) -> Int`:
    val addOperation: (Int, Int) -> Int = { a, b -> a + b }
    val multiplyOperation: (Int, Int) -> Int = { a, b -> a * b }

    println("   -> Gọi trực tiếp addOperation(15, 25)     = ${addOperation(15, 25)}")
    println("   -> Gọi qua .invoke(): multiplyOperation(6, 7) = ${multiplyOperation.invoke(6, 7)}\n")

    // =================================================================
    // 2. Higher-Order Function, Trailing Lambda & Implicit `it`
    // =================================================================
    println("2. [Trailing Lambda & `it`] Sự tiến hóa từ cú pháp đầy đủ sang Idiomatic Kotlin:")
    val samplePrices = listOf(50_000, 120_000, 350_000, 80_000, 500_000)

    // Bước 1: Truyền Lambda nằm bên trong ngoặc tròn (giống phong cách Java)
    val step1 = filterItems(samplePrices, { price: Int -> price >= 100_000 })

    // Bước 2: Đưa Lambda cuối cùng ra ngoài ngoặc tròn (Trailing Lambda) + Dùng tham số ngầm định `it`
    val step2 = filterItems(samplePrices) { it >= 100_000 }

    println("   -> Danh sách giá gốc: $samplePrices")
    println("   -> Lọc các mức giá >= 100k bằng Trailing Lambda `{ it >= 100_000 }`: $step2 (khớp step1: ${step1 == step2})\n")

    // =================================================================
    // 3. Dấu gạch dưới `_` cho tham số Lambda không sử dụng (Unused Lambda Parameter)
    // =================================================================
    println("3. [Unused Parameter `_`] Đặt dấu `_` cho tham số không dùng trong Lambda:")
    val servicePorts = mapOf("auth-service" to 8081, "order-service" to 8082, "payment-service" to 8083)

    // Khi chỉ cần dùng `port` (value) mà không cần `serviceName` (key), ta thay key bằng `_`:
    print("   -> Danh sách các cổng đang mở: ")
    servicePorts.forEach { (_, port) ->
        print("$port ")
    }
    println("\n")

    // =================================================================
    // 4. Function References (`::tenHam` và `Class::tenHam`)
    // =================================================================
    println("4. [Function References `::`] Truyền tham chiếu hàm có sẵn vào Higher-Order Function:")
    val numbers = listOf(2, 3, 4, 9, 11, 15, 17, 20)

    // Truyền tham chiếu tới hàm top-level `::isPrimeNumber` vào `filterItems`:
    val primeNumbers = filterItems(numbers, ::isPrimeNumber)
    println("   -> Lọc số nguyên tố bằng `::isPrimeNumber`: $primeNumbers")

    // Truyền tham chiếu tới phương thức của class `String::uppercase`:
    val rawRoles = listOf("admin", "manager", "auditor")
    val upperRoles = rawRoles.map(String::uppercase)
    println("   -> Chuyển chữ hoa bằng `String::uppercase`: $upperRoles")

    println("\n--- ___ * END: Ex04HigherOrderFunctionsAndLambdas * ___ ---")
}

// Higher-Order Function: Hàm nhận một hàm khác `predicate: (Int) -> Boolean` làm tham số
private fun filterItems(items: List<Int>, predicate: (Int) -> Boolean): List<Int> {
    val result = mutableListOf<Int>()
    for (item in items) {
        // Gọi `predicate(item)` tự nhiên như một hàm thông thường (không cần `.test(item)` như Java!)
        if (predicate(item)) {
            result.add(item)
        }
    }
    return result
}

// Hàm kiểm tra số nguyên tố để minh họa Function Reference `::isPrimeNumber`
private fun isPrimeNumber(n: Int): Boolean {
    if (n < 2) return false
    for (i in 2..(n / 2)) {
        if (n % i == 0) return false
    }
    return true
}
