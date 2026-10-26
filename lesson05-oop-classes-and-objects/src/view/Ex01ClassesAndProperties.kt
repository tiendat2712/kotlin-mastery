package view

import bean.Car

/**
 * ============================================================================
 * BÀI 01: CLASSES & PROPERTIES - TẠM BIỆT BOILERPLATE GETTER/SETTER
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ----------------------------------------------------------------------------
 * Trong Java (`lesson03-oop`), một biến khai báo trong class được gọi là **Field** (trường).
 * Để đảm bảo tính Đóng gói (Encapsulation), bạn phải:
 *   - Khai báo field là `private`
 *   - Viết `public getXxx()` và `public setXxx()`
 *
 * Trong Kotlin, khái niệm mặc định không phải là Field trần trụi, mà là **Property**:
 *   `Property = private Backing Field (trên HEAP) + Accessors (Getter / Setter)`
 *
 *   - Khi khai báo `val id: String`:
 *     Bytecode sinh ra: `private final String id;` + `public final String getId()`
 *     (Chỉ đọc - Read-only, KHÔNG có hàm `setId`).
 *   - Khi khai báo `var salesPrice: Double`:
 *     Bytecode sinh ra: `private double salesPrice;` + `public final double getSalesPrice()`
 *     + `public final void setSalesPrice(double)`.
 *
 * QUAN TRỌNG NHẤT:
 * Khi bạn viết `c1.salesPrice = 550.0` trong Kotlin, bạn KHÔNG hề chọc thẳng vào
 * một biến `public` như lập trình hướng thủ tục (FOP)! Ở tầng Bytecode, Kotlin
 * tự động dịch câu lệnh đó thành lời gọi hàm `c1.setSalesPrice(550.0)`!
 * Và khi bạn đọc `println(c1.salesPrice)`, Kotlin dịch thành `c1.getSalesPrice()`.
 *
 * 2. SO SÁNH JAVA vs KOTLIN (Company & Employee trong `Ex01CarDemo.java`):
 * ----------------------------------------------------------------------------
 * Hãy nhớ lại câu hỏi kinh điển ở `D:\Java_Journey\lesson03-oop\src\view\Ex01CarDemo.java`:
 *   "Tại sao phải khai báo thuộc tính là private rồi dùng getter/setter mà không
 *    khai báo thẳng là public cho ngắn gọn?"
 *   -> Vì trong ví dụ `Company` trả lương (`payment`) cho `Employee`:
 *      Bên ngoài chỉ được phép đọc `company.balance` và `employee.income`,
 *      chứ KHÔNG được phép tự ý gán `employee.income = 999999.0`! Việc tăng thu nhập
 *      phải đi qua hàm nghiệp vụ `plusIncome(salary)`.
 *
 * Trong Kotlin:
 *   - Ta vừa có cú pháp ngắn gọn `company.balance` (không cần `.getBalance()`),
 *   - Vừa bảo vệ tuyệt đối dữ liệu trên HEAP bằng `private set` (chỉ cho đọc từ bên ngoài,
 *     cấm sửa trực tiếp, bắt buộc phải qua hàm nghiệp vụ `payment` / `plusIncome`)!
 *
 * 3. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * Trong các dự án Spring Boot + Kotlin thực tế:
 *   - Các Spring Bean (`@Service`, `@RestController`) tiêm phụ thuộc qua Constructor
 *     chỉ cần viết `class OrderService(private val paymentGateway: PaymentGateway)`.
 *   - Các Domain Entity (như `Company`, `Wallet`, `Order`) dùng `var balance: Double private set`
 *     để áp dụng Rich Domain Model (DDD): Controller/Service bên ngoài đọc được `wallet.balance`
 *     nhưng chỉ có thể thay đổi số dư thông qua các domain methods (`credit()`, `debit()`).
 */

private class Company(balance: Double) {
    // `private set`: Bên ngoài gọi `company.balance` (getter) thoải mái,
    // nhưng nếu cố tình viết `company.balance = 0.0` sẽ bị lỗi biên dịch ngay lập tức!
    var balance: Double = balance
        private set

    fun payment(employee: Employee, salary: Double) {
        require(salary > 0) { "Lương thanh toán phải > 0" }
        require(this.balance >= salary) { "Quỹ lương công ty không đủ!" }
        this.balance -= salary
        employee.plusIncome(salary)
    }
}

private class Employee(val name: String) {
    var income: Double = 0.0
        private set

    fun plusIncome(salary: Double) {
        require(salary > 0) { "Khoản thu nhập cộng thêm phải > 0" }
        this.income += salary
    }
}

fun main() {
    println("--- ___ * START: Ex01ClassesAndProperties * ___ ---")

    // ========================================================================
    // 1. Tái hiện Ex01CarDemo.java theo phong cách Kotlin Idiomatic
    // ========================================================================
    println("\n[1] Khởi tạo đối tượng trên HEAP và Property Access Syntax:")

    // Gán thuộc tính thuộc phạm vi Class (Companion Object)
    Car.model = "Audi"

    // Tạo đối tượng c1 bằng Default Constructor (nhờ default parameter values)
    val c1 = Car()
    // Nhìn giống gán biến trực tiếp, nhưng thực chất dưới Bytecode là c1.setId("C1")!
    c1.id = "C1"
    c1.color = "Yellow"
    c1.salesPrice = 550.0

    // Cập nhật lại Car.model -> mọi đối tượng Car đều thấy giá trị mới này
    Car.model = "Audi Update"

    // Tạo c2, c3, c4 bằng Full Constructor hoặc Named Arguments
    val c2 = Car("C2", "White", 800.0)
    val c3 = Car("C3", "Black", 800.0)
    val c4 = Car(id = "C4", color = "Red", salesPrice = 620.0)

    println("  c1 --> $c1")
    println("  c2 --> $c2")
    println("  c3 --> $c3")
    println("  c4 --> $c4")

    // ========================================================================
    // 2. Minh họa tính Đóng gói (Encapsulation): Company & Employee
    // ========================================================================
    println("\n[2] Bài toán Company trả lương cho Employee (Bảo toàn Encapsulation):")
    val company = Company(balance = 5000.0)
    val emp = Employee(name = "Nguyen Van A")

    println("  Trước khi trả lương -> Quỹ công ty: ${company.balance}, Thu nhập ${emp.name}: ${emp.income}")

    // Thực hiện hành vi nghiệp vụ (Domain behavior)
    company.payment(emp, 1200.0)
    company.payment(emp, 300.0)

    // Thử mở comment dòng dưới sẽ thấy trình biên dịch báo lỗi ngay:
    // emp.income = 999999.0 // ERROR: Cannot assign to 'income': the setter is private in 'Employee'

    println("  Sau khi trả lương   -> Quỹ công ty: ${company.balance}, Thu nhập ${emp.name}: ${emp.income}")

    println("\n--- ___ * FINISH: Ex01ClassesAndProperties * ___ ---")
}
