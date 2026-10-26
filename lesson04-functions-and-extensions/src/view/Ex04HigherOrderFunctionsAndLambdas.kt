package view

/**
 * =====================================================================
 * LESSON 04 - EX04: HIGHER-ORDER FUNCTIONS, LAMBDA SYNTAX & FUNCTION REFERENCES
 * =====================================================================
 *
 * 1. BẢN CHẤT & SO SÁNH VỚI JAVA (`Ex01FunctionalInterfaceDemo.java` & `Ex02LambdaSyntax.java`):
 *    - Trong Java 8+, hàm không có cú pháp kiểu hàm (Function Type) độc lập mà biểu diễn thông qua
 *      các **Functional Interface** trong gói `java.util.function`:
 *      `Predicate<T>`, `Function<T, R>`, `BiFunction<T, U, R>`, `Consumer<T>`, `Supplier<T>`...
 *      Và khi gọi phải chấm thêm `.test(...)`, `.apply(...)`, `.accept(...)`, `.get()`.
 *    - Trong Kotlin, **Hàm là Công dân hạng nhất (First-Class Citizen)** và có **Function Type (Kiểu hàm)**
 *      trực quan ngay trong ngôn ngữ:
 *      + `(Int) -> Boolean`          (Tương đương `Predicate<Integer>`)
 *      + `(String) -> Int`           (Tương đương `Function<String, Integer>`)
 *      + `(Int, Int) -> Int`         (Tương đương `BinaryOperator<Integer>` / `BiFunction`)
 *      + `(String) -> Unit`          (Tương đương `Consumer<String>`)
 *      + `() -> Connection`          (Tương đương `Supplier<Connection>`)
 *    - Dưới tầng Bytecode: Kotlin biên dịch `(A) -> B` thành interface chuẩn `kotlin.jvm.functions.Function1<A, B>`
 *      (có từ `Function0` đến `Function22`, kết hợp `invokedynamic` trên các bản Kotlin mới).
 *      Để gọi hàm, bạn chỉ cần dùng dấu ngoặc tròn `predicate(item)` tự nhiên như gọi hàm bình thường
 *      (tương đương `predicate.invoke(item)`)!
 *
 * 2. QUY TẮC TRAILING LAMBDA & THAM SỐ NGẦM ĐỊNH `it`:
 *    - **Quy tắc Trailing Lambda (Đặc sản cú pháp quan trọng của Kotlin)**:
 *      Nếu tham số **CUỐI CÙNG** của một hàm là một Lambda, bạn được phép **đưa cặp ngoặc nhọn `{ ... }`
 *      ra BÊN NGOÀI dấu ngoặc tròn `(...)`**! Và nếu hàm chỉ có duy nhất 1 tham số là Lambda đó,
 *      bạn được **bỏ luôn cả cặp ngoặc tròn `()`**!
 *    - **Tham số `it`**: Nếu Lambda chỉ có **ĐÚNG 1 tham số**, bạn không cần khai báo `x ->` mà có thể dùng tên mặc định `it`!
 *    - **Lưu ý Cú pháp**: Biểu thức Lambda trong Kotlin **nằm trong cặp ngoặc nhọn `{ thamSo -> thanHam }`**
 *      (khác với Java dùng `thamSo -> thanHam` ngoài ngoặc nhọn).
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Nhiều API trong Spring Boot Kotlin như `JdbcTemplate.queryForObject(sql) { rs, _ -> ... }`,
 *      `TransactionTemplate.execute { status -> ... }`, hay `WebClient` đều tận dụng Trailing Lambda
 *      và dấu `_` (cho tham số không dùng tới như `rowNum`)!
 * =====================================================================
 */

fun main() {
    println("--- ___ * START: Ex04HigherOrderFunctionsAndLambdas * ___ ---\n")

    // =================================================================
    // 1. Lưu hàm vào biến với Function Type `(A, B) -> R`
    // =================================================================
    println("1. [Function Types] Luu tru Lambda vao bien voi kieu ham `(Int, Int) -> Int`:")
    // Khai báo tường minh kiểu hàm `(Int, Int) -> Int`:
    val addOperation: (Int, Int) -> Int = { a, b -> a + b }
    val multiplyOperation: (Int, Int) -> Int = { a, b -> a * b }
    check(addOperation(15, 25) == 40 && multiplyOperation.invoke(6, 7) == 42)

    println("   -> Goi truc tiep addOperation(15, 25)        = ${addOperation(15, 25)}")
    println("   -> Goi qua .invoke(): multiplyOperation(6, 7) = ${multiplyOperation.invoke(6, 7)}\n")

    // =================================================================
    // 2. Higher-Order Function, Trailing Lambda & Implicit `it`
    // =================================================================
    println("2. [Trailing Lambda & `it`] Su tien hoa tu cu phap day du sang Idiomatic Kotlin:")
    val samplePrices = listOf(50_000, 120_000, 350_000, 80_000, 500_000)

    // Bước 1: Truyền Lambda nằm bên trong ngoặc tròn (giống phong cách Java)
    val step1 = filterItems(samplePrices, { price: Int -> price >= 100_000 })

    // Bước 2: Đưa Lambda cuối cùng ra ngoài ngoặc tròn (Trailing Lambda) + Dùng tham số ngầm định `it`
    val step2 = filterItems(samplePrices) { it >= 100_000 }
    check(step1 == step2 && step2 == listOf(120_000, 350_000, 500_000))

    println("   -> Danh sach gia goc: $samplePrices")
    println("   -> Loc cac muc gia >= 100k bang Trailing Lambda `{ it >= 100_000 }`: $step2 (khop step1: ${step1 == step2})\n")

    // =================================================================
    // 3. Dấu gạch dưới `_` cho tham số Lambda không sử dụng (Unused Lambda Parameter)
    // =================================================================
    println("3. [Unused Parameter `_`] Dat dau `_` cho tham so khong dung trong Lambda:")
    val servicePorts = mapOf("auth-service" to 8081, "order-service" to 8082, "payment-service" to 8083)

    // Khi chỉ cần dùng `port` (value) mà không cần `serviceName` (key), ta thay key bằng `_`:
    print("   -> Danh sach cac cong dang mo: ")
    servicePorts.forEach { (_, port) ->
        print("$port ")
    }
    println("\n")

    // =================================================================
    // 4. Function References (`::tenHam` và `Class::tenHam`)
    // =================================================================
    println("4. [Function References `::`] Truyen tham chieu ham co san vao Higher-Order Function:")
    val numbers = listOf(2, 3, 4, 9, 11, 15, 17, 20)

    // Truyền tham chiếu tới hàm top-level `::isPrimeNumber` vào `filterItems`:
    val primeNumbers = filterItems(numbers, ::isPrimeNumber)
    check(primeNumbers == listOf(2, 3, 11, 17))
    println("   -> Loc so nguyen to bang `::isPrimeNumber`: $primeNumbers")

    // Truyền tham chiếu tới phương thức của class `String::uppercase`:
    val rawRoles = listOf("admin", "manager", "auditor")
    val upperRoles = rawRoles.map(String::uppercase)
    check(upperRoles == listOf("ADMIN", "MANAGER", "AUDITOR"))
    println("   -> Chuyen chu hoa bang `String::uppercase`: $upperRoles")

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
