package view

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * =====================================================================
 * LESSON 01 - EX06: TOÁN TỬ SỐ HỌC, KOTLIN.MATH & OPERATOR OVERLOADING
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Trong Java, các toán tử `+`, `-`, `*`, `/`, `>`, `<` chỉ dùng được cho kiểu nguyên thủy
 *      (và riêng `+` cho `String`). Với mọi class khác như `BigDecimal` hay `LocalDate`,
 *      bạn buộc phải gọi chuỗi phương thức dài dòng: `a.add(b).subtract(c).multiply(d)`.
 *    - Trong Kotlin, mỗi toán tử ký hiệu đều ánh xạ tới một tên hàm chuẩn được đánh dấu từ khóa `operator`:
 *      + `a + b`  --> dịch thành `a.plus(b)`
 *      + `a - b`  --> dịch thành `a.minus(b)`
 *      + `a * b`  --> dịch thành `a.times(b)`
 *      + `a / b`  --> dịch thành `a.div(b)`
 *      + `a % b`  --> dịch thành `a.rem(b)`
 *      + `a > b`  --> dịch thành `a.compareTo(b) > 0`
 *      + `a[i]`   --> dịch thành `a.get(i)`
 *    - Nhờ cơ chế **Operator Overloading** có kiểm soát này, Kotlin định nghĩa sẵn các hàm extension
 *      `operator fun BigDecimal.plus(...)` giúp tính toán tiền tệ bằng `BigDecimal` tự nhiên như số thường!
 *
 * 2. SO SÁNH JAVA VS KOTLIN (`BigDecimal` trong `Ex02BuilderDemo.java`):
 *    - Java:
 *      ```java
 *      BigDecimal total = price.multiply(qty).subtract(discount).add(tax);
 *      if (total.compareTo(limit) > 0) { ... }
 *      ```
 *    - Kotlin:
 *      ```kotlin
 *      val total = price * qty - discount + tax
 *      if (total > limit) { ... }
 *      ```
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong các hệ thống Fintech / E-Commerce viết bằng Spring Boot Kotlin, ta thường xây dựng
 *      Value Object `Money(val amount: BigDecimal, val currency: String)` và nạp chồng toán tử
 *      `+`, `-`, `*` kèm kiểm tra cùng loại tiền tệ (`require(currency == other.currency)`),
 *      giúp tầng Domain Service cực kỳ biểu cảm và không thể cộng nhầm USD với VND!
 * =====================================================================
 */

// Xây dựng Value Object `Money` nạp chồng toán tử (Operator Overloading) chuẩn Domain-Driven Design
private data class Money(
    val amount: BigDecimal,
    val currency: String = "VND"
) : Comparable<Money> {

    // Nạp chồng toán tử `+` (a + b -> a.plus(b))
    operator fun plus(other: Money): Money {
        require(this.currency == other.currency) {
            "Không thể cộng 2 loại tiền tệ khác nhau: ${this.currency} và ${other.currency}"
        }
        return Money(this.amount + other.amount, this.currency)
    }

    // Nạp chồng toán tử `-` (a - b -> a.minus(b))
    operator fun minus(other: Money): Money {
        require(this.currency == other.currency) {
            "Không thể trừ 2 loại tiền tệ khác nhau: ${this.currency} và ${other.currency}"
        }
        return Money(this.amount - other.amount, this.currency)
    }

    // Nạp chồng toán tử `*` với số lượng nguyên (money * 3)
    operator fun times(quantity: Int): Money {
        return Money(this.amount * quantity.toBigDecimal(), this.currency)
    }

    // Nạp chồng compareTo để tự động dùng được các toán tử `<`, `<=`, `>`, `>=`!
    override operator fun compareTo(other: Money): Int {
        require(this.currency == other.currency) {
            "Không thể so sánh 2 loại tiền tệ khác nhau: ${this.currency} và ${other.currency}"
        }
        return this.amount.compareTo(other.amount)
    }

    override fun toString(): String = "${amount.setScale(0, RoundingMode.HALF_UP)} $currency"
}

fun main() {
    println("--- ___ * START: Ex06OperatorsAndMath * ___ ---\n")

    // =================================================================
    // 1. Toán tử số học cơ bản & Thư viện kotlin.math.*
    // =================================================================
    println("1. [Arithmetic & kotlin.math] Toán tử cơ bản và các hàm toán học:")
    val a = 17
    val b = 5
    println("   -> Phép chia nguyên: $a / $b = ${a / b} | Chia lấy dư (rem): $a % $b = ${a % b}")
    println("   -> Phép chia thực:   $a.toDouble() / $b = ${a.toDouble() / b}")
    println("   -> max(17, 5) = ${max(a, b)} | min(17, 5) = ${min(a, b)} | abs(-42) = ${abs(-42)}")
    println("   -> Lũy thừa 2.0.pow(10) = ${2.0.pow(10)} | Làm tròn 3.65.roundToInt() = ${3.65.roundToInt()}\n")

    // =================================================================
    // 2. Toán tử trên BigDecimal (Giải phóng khỏi .add(), .subtract(), .compareTo() của Java)
    // =================================================================
    println("2. [BigDecimal Operators] Tính toán tài chính trực quan trong Kotlin:")
    val unitPrice = BigDecimal("350000.00")
    val quantity = BigDecimal("3")
    val voucherDiscount = BigDecimal("100000.00")
    val freeShipThreshold = BigDecimal("500000.00")

    // Trong Kotlin: dùng trực tiếp *, -, + trên BigDecimal!
    val finalBill: BigDecimal = unitPrice * quantity - voucherDiscount
    println("   -> Đơn giá: $unitPrice | Số lượng: $quantity | Giảm giá: $voucherDiscount")
    println("   -> Tổng thanh toán (unitPrice * quantity - voucherDiscount) = $finalBill")

    // So sánh lớn hơn / nhỏ hơn bằng `>=` (dưới hậu trường gọi finalBill.compareTo(freeShipThreshold) >= 0)
    val isFreeShipping = finalBill >= freeShipThreshold
    println("   -> Đủ điều kiện Freeship (finalBill >= $freeShipThreshold)? $isFreeShipping\n")

    // =================================================================
    // 3. Tự định nghĩa Operator Overloading cho class Money (Spring Domain Model)
    // =================================================================
    println("3. [Custom Operator Overloading] Sử dụng toán tử +, -, *, > trên Domain Object `Money`:")
    val itemPrice = Money(250_000.toBigDecimal(), "VND")
    val shippingFee = Money(30_000.toBigDecimal(), "VND")
    val coupon = Money(50_000.toBigDecimal(), "VND")

    // Nhìn dòng code nghiệp vụ dưới đây: đọc tự nhiên như công thức toán học!
    val orderTotal: Money = (itemPrice * 2) + shippingFee - coupon
    val vipMilestone = Money(400_000.toBigDecimal(), "VND")

    println("   -> Giá 1 sản phẩm: $itemPrice")
    println("   -> Công thức: (itemPrice * 2) + shippingFee - coupon = $orderTotal")
    println("   -> So sánh trực tiếp (orderTotal > vipMilestone): ${orderTotal > vipMilestone}")

    println("\n--- ___ * END: Ex06OperatorsAndMath * ___ ---")
}
