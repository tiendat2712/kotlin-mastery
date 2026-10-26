package view

/**
 * =====================================================================
 * LESSON 03 - EX02: `if-else` NHƯ MỘT BIỂU THỨC (EXPRESSION) & TẠI SAO KHÔNG CÓ `?:` TERNARY
 * =====================================================================
 *
 * 1. BẢN CHẤT (STATEMENT VS EXPRESSION):
 *    - Trong Java, `if - else` là một **Câu lệnh (Statement)** — nó chỉ điều hướng luồng chạy chứ tự thân nó
 *      KHÔNG trả về giá trị. Vì vậy để gán giá trị cho một biến `final` theo điều kiện, lập trình viên Java
 *      hoặc phải khai báo biến rỗng trước, hoặc phải dùng toán tử 3 ngôi `condition ? a : b`.
 *    - Trong Kotlin, `if - else` là một **Biểu thức (Expression)** — bản thân toàn bộ khối `if (...) ... else ...`
 *      **TRẢ VỀ MỘT GIÁ TRỊ** và có thể gán trực tiếp vào biến `val` hoặc đặt ngay sau từ khóa `return`!
 *    - Khi một nhánh của `if - else` có nhiều dòng lệnh bọc trong `{ ... }`, **biểu thức ở dòng cuối cùng**
 *      của nhánh đó chính là giá trị trả về của nhánh!
 *
 * 2. TẠI SAO KOTLIN LOẠI BỎ TOÁN TỬ BA NGÔI (`condition ? a : b`) CỦA JAVA?
 *    - Thứ nhất: Cú pháp `val max = if (a > b) a else b` vừa ngắn gọn, vừa đọc xuôi chiều tự nhiên bằng tiếng Anh,
 *      và không bị rối mắt khi lồng nhau như `cond1 ? val1 : cond2 ? val2 : val3` của Java.
 *    - Thứ hai: Dấu `?` và `:` trong Kotlin đã được dành riêng cho nhiệm vụ quan trọng hơn là
 *      **Null Safety** (`?.` Safe Call và `?:` Elvis Operator)!
 *    - Lưu ý quan trọng: Khi dùng `if` làm **Expression** (để gán biến hoặc `return`), nhánh **`else` là BẮT BUỘC**
 *      để trình biên dịch đảm bảo biến nhận được giá trị trong mọi trường hợp!
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong các hàm `@Service` hoặc `@RestController`, ta thường dùng **`return if (...) { ... } else { ... }`**
 *      (gọi là *Lifted Return*) giúp gom điểm thoát của hàm về một chỗ duy nhất, dễ đọc và dễ bảo trì.
 * =====================================================================
 */

fun main() {
    println("--- ___ * START: Ex02IfAsExpression * ___ ---\n")

    // =================================================================
    // 1. Dùng `if - else` trên 1 dòng thay thế toán tử 3 ngôi `? :` của Java
    // =================================================================
    println("1. [Single-line If Expression] Thay the toan tu 3 ngoi (Ternary Operator) cua Java:")
    val loyaltyPoints = 1_250

    // Thay vì Java: String tier = (loyaltyPoints >= 1000) ? "GOLD" : "STANDARD";
    val memberTier: String = if (loyaltyPoints >= 1_000) "GOLD" else "STANDARD"
    val discountRate: Double = if (memberTier == "GOLD") 0.15 else 0.05
    check(memberTier == "GOLD" && discountRate == 0.15)

    println("   -> Diem tich luy: $loyaltyPoints -> Hang the: $memberTier (Chiet khau: ${discountRate * 100}%)\n")

    // =================================================================
    // 2. Dùng `if - else if - else` nhiều dòng (Block Expression)
    // =================================================================
    println("2. [Multi-line Block If Expression] Moi nhanh co the xu ly nhieu buoc va tra ve dong cuoi cung:")
    val orderSubTotal = 850_000L
    val isHolidayPromo = true

    val finalPayableAmount: Long = if (orderSubTotal >= 1_000_000L) {
        println("   -> [Nhanh VIP] Don hang >= 1 trieu: Mien phi ship + Giam ngay 100,000 VND")
        orderSubTotal - 100_000L // <-- Giá trị trả về của nhánh này!
    } else if (isHolidayPromo) {
        val holidayDiscount = 50_000L
        val shipping = 20_000L
        println("   -> [Nhanh Le Hoi] Giam $holidayDiscount VND, phi ship uu dai $shipping VND")
        orderSubTotal - holidayDiscount + shipping // <-- Giá trị trả về của nhánh này!
    } else {
        val standardShipping = 35_000L
        println("   -> [Nhanh Tieu Chuan] Phi ship tieu chuan $standardShipping VND")
        orderSubTotal + standardShipping // <-- Giá trị trả về của nhánh này!
    }
    check(finalPayableAmount == 820_000L)

    println("   => Tong so tien khach phai thanh toan: $finalPayableAmount VND\n")

    // =================================================================
    // 3. Lifted Return (`return if (...)`) trong hàm nghiệp vụ
    // =================================================================
    println("3. [Lifted Return Idiom] Dat `return` ngay truoc `if` trong ham:")
    println("   -> Kiem tra ton kho (stock = 15, req = 5):  ${ evaluateStockStatus(availableStock = 15, requestedQty = 5) }")
    println("   -> Kiem tra ton kho (stock = 2,  req = 5):  ${ evaluateStockStatus(availableStock = 2, requestedQty = 5) }")
    println("   -> Kiem tra ton kho (stock = 0,  req = 1):  ${ evaluateStockStatus(availableStock = 0, requestedQty = 1) }")

    println("\n--- ___ * END: Ex02IfAsExpression * ___ ---")
}

private fun evaluateStockStatus(availableStock: Int, requestedQty: Int): String {
    // Thay vì viết từ khóa `return` lặp lại ở từng nhánh, ta nhấc `return` ra đầu biểu thức `if`:
    return if (availableStock <= 0) {
        "OUT_OF_STOCK (Het hang)"
    } else if (availableStock < requestedQty) {
        "INSUFFICIENT_STOCK (Chi con $availableStock san pham, khong du $requestedQty)"
    } else {
        "READY_TO_SHIP (Du hang giao ngay)"
    }
}
