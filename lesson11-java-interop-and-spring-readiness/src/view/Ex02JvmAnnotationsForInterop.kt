package view

import java.io.IOException
import java.lang.reflect.Modifier
import java.math.BigDecimal

/**
 * BÀI 02: CÁC ANNOTATION ĐIỀU KHIỂN BYTECODE CHO JAVA INTEROP (`@JvmStatic`, `@JvmOverloads`, `@JvmField`, `@Throws`)
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE GENERATION):
 *    Khi dự án có cả code Java gọi ngược vào code Kotlin (hoặc khi các thư viện Java như JUnit, Jackson,
 *    Spring, Hibernate quét Bytecode của class Kotlin), bạn cần biết 4 annotation điều khiển trình biên dịch:
 *
 *    - **`@JvmStatic` (trên hàm của `object` hoặc `companion object`)**:
 *      + Mặc định, hàm trong `companion object` chỉ là phương thức instance của lớp tĩnh nội bộ `MyClass.Companion`
 *        (trong Java phải gọi dài dòng: `MoneyUtils.Companion.formatVnd(amount)`).
 *      + Khi gắn `@JvmStatic`, Kotlin sinh thêm một phương thức **`public static` thực sự** ngay trên lớp ngoài
 *        (`MoneyUtils.formatVnd(amount)`)!
 *
 *    - **`@JvmOverloads` (trên hàm hoặc constructor có Default Parameter Values)**:
 *      + Java không có khái niệm Default Arguments (`taxRate: Double = 0.08`). Mặc định Kotlin chỉ sinh 1 hàm
 *        nhận ĐỦ tất cả tham số (Java caller bắt buộc phải truyền đủ mọi tham số).
 *      + Khi gắn `@JvmOverloads`, trình biên dịch Kotlin tự động sinh ra **tất cả các phiên bản nạp chồng (Overloads)**
 *        tương ứng trong Bytecode để code Java gọi thiếu tham số vẫn chạy mượt mà!
 *
 *    - **`@JvmField` (trên thuộc tính `val` / `var`)**:
 *      + Mặc định mọi thuộc tính Kotlin đều được biên dịch thành `private field` + `public getter/setter`.
 *      + Khi gắn `@JvmField`, Kotlin KHÔNG sinh getter/setter nữa mà xuất trực tiếp thành biến trường
 *        `public final` (nếu là `val`) giống hệt hằng số/biến trường trong Java (`InvoiceConfig.DEFAULT_CURRENCY`).
 *        (Lưu ý: Với hằng số kiểu nguyên thủy hoặc `String`, bạn cũng có thể dùng `const val`).
 *
 *    - **`@Throws(IOException::class)` (trên hàm có thể ném Checked Exception khi gọi từ Java)**:
 *      + Vì Kotlin không có Checked Exception, hàm Kotlin ném `IOException` mặc định KHÔNG có mệnh đề `throws IOException`
 *        trong chữ ký Bytecode. Nếu code Java gọi hàm đó và viết `try { ... } catch (IOException e)`, trình biên dịch
 *        `javac` sẽ báo lỗi: *"exception IOException is never thrown in body of corresponding try statement"*!
 *      + Gắn `@Throws(IOException::class)` bảo trình biên dịch Kotlin ghi rõ `throws IOException` vào Bytecode method header!
 *
 * 2. SPRING KOTLIN CORNER:
 *    - Hàm `main` khởi chạy Spring Boot trong `companion object` (nếu viết kiểu cũ): cần `@JvmStatic fun main(args: Array<String>)`
 *      (hoặc viết top-level `fun main(args: Array<String>) { runApplication<MyApp>(*args) }` thì không cần).
 *    - Các phương thức tạo dữ liệu mẫu `@ParameterizedTest` + `@MethodSource` trong JUnit 5 trên `companion object`
 *      thường dùng `@JvmStatic` để JUnit tìm thấy static method dễ dàng.
 */

class InvoiceCalculator @JvmOverloads constructor(
    val merchantId: String,
    val defaultTaxRate: Double = 0.08,
    val currency: String = "VND"
) {
    // @JvmOverloads: Sinh ra 3 hàm `calculateTotal` trong Bytecode:
    // 1. calculateTotal(BigDecimal)
    // 2. calculateTotal(BigDecimal, BigDecimal)
    // 3. calculateTotal(BigDecimal, BigDecimal, boolean)
    @JvmOverloads
    fun calculateTotal(
        subtotal: BigDecimal,
        shippingFee: BigDecimal = BigDecimal.ZERO,
        applyTax: Boolean = true
    ): BigDecimal {
        val taxedAmount = if (applyTax) {
            subtotal.multiply(BigDecimal.valueOf(1.0 + defaultTaxRate))
        } else {
            subtotal
        }
        return taxedAmount.add(shippingFee)
    }

    // @Throws: Ghi `throws java.io.IOException` vào chữ ký Bytecode để Java caller bắt được Checked Exception
    @Throws(IOException::class)
    fun exportInvoiceToFile(invoiceId: String, simulateDiskFull: Boolean) {
        if (simulateDiskFull) {
            throw IOException("O dia day khi xuat hoa don [$invoiceId]!")
        }
        println("  - Da xuat hoa don [$invoiceId] ra file thanh cong.")
    }

    companion object {
        // @JvmField: Xuất thẳng thành `public static final BigDecimal FREE_SHIPPING_THRESHOLD` trong Bytecode
        @JvmField
        val FREE_SHIPPING_THRESHOLD: BigDecimal = BigDecimal("500000")

        // @JvmStatic: Xuất thẳng thành `public static String formatCurrency(BigDecimal)` trên class InvoiceCalculator
        @JvmStatic
        fun formatCurrency(amount: BigDecimal): String = "${amount.setScale(0)} VND"
    }
}

fun main() {
    println("--- ___ * START EX02: JVM ANNOTATIONS FOR INTEROP * ___ ---\n")

    // =========================================================================
    // 1. KIỂM CHỨNG BYTECODE ĐƯỢC SINH RA BỞI `@JvmOverloads`, `@JvmStatic`, `@JvmField`, `@Throws`
    // =========================================================================
    println("1. Soi JVM Bytecode cua class `InvoiceCalculator` qua Java Reflection:")
    val clazz = InvoiceCalculator::class.java

    println("  a) Cac Constructor duoc sinh ra nho `@JvmOverloads constructor` (${clazz.declaredConstructors.size} constructors):")
    clazz.declaredConstructors.forEach { ctor ->
        val params = ctor.parameterTypes.joinToString(", ") { it.simpleName }
        println("     - InvoiceCalculator($params)")
    }

    println("\n  b) Cac ham nap chong `calculateTotal` duoc sinh ra nho `@JvmOverloads`:")
    val calcOverloads = clazz.declaredMethods.filter { it.name == "calculateTotal" }
    calcOverloads.forEach { method ->
        val params = method.parameterTypes.joinToString(", ") { it.simpleName }
        println("     - ${method.returnType.simpleName} calculateTotal($params)")
    }
    check(calcOverloads.size == 3)

    println("\n  c) Kiem tra `@JvmStatic` va `@JvmField` tren lop `InvoiceCalculator`:")
    val staticMethod = clazz.getDeclaredMethod("formatCurrency", BigDecimal::class.java)
    println("     - Ham `formatCurrency` co phai `public static` truc tiep tren InvoiceCalculator? -> ${Modifier.isStatic(staticMethod.modifiers)}")
    check(Modifier.isStatic(staticMethod.modifiers))

    val publicField = clazz.getDeclaredField("FREE_SHIPPING_THRESHOLD")
    println("     - Truong `FREE_SHIPPING_THRESHOLD` co phai `public static final`? -> ${Modifier.isPublic(publicField.modifiers) && Modifier.isStatic(publicField.modifiers)}")
    check(Modifier.isPublic(publicField.modifiers) && Modifier.isStatic(publicField.modifiers))

    println("\n  d) Kiem tra `@Throws(IOException::class)` tren ham `exportInvoiceToFile`:")
    val exportMethod = clazz.getDeclaredMethod("exportInvoiceToFile", String::class.java, Boolean::class.javaPrimitiveType)
    val declaredExceptions = exportMethod.exceptionTypes.map { it.simpleName }
    println("     - Danh sach Checked Exceptions ghi trong Bytecode `throws`: $declaredExceptions")
    check(declaredExceptions.contains("IOException"))

    // =========================================================================
    // 2. CHẠY THỬ NGHIỆM HÀM TÍNH TOÁN & BẮT LỖI IOEXCEPTION
    // =========================================================================
    println("\n2. Chay thu nghiem `InvoiceCalculator`:")
    val calc = InvoiceCalculator("NOVA_SHOP")
    val total = calc.calculateTotal(BigDecimal("400000"), shippingFee = BigDecimal("30000"))
    println("  - Tong tien hoa don (da gom VAT 8% + ship 30k): ${InvoiceCalculator.formatCurrency(total)}")
    println("  - Nguong mien phi van chuyen (@JvmField): ${InvoiceCalculator.formatCurrency(InvoiceCalculator.FREE_SHIPPING_THRESHOLD)}")

    try {
        calc.exportInvoiceToFile("INV-2026-001", simulateDiskFull = true)
    } catch (e: IOException) {
        println("  - Bat duoc IOException: ${e.message}")
    }

    println("\n--- ___ * END EX02 * ___ ---")
}
