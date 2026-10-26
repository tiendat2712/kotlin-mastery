package view

/**
 * =====================================================================
 * LESSON 02 - EX03: KIỂM TRA KIỂU (`is`, `!is`), SMART CAST & SAFE CAST (`as?`)
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Trong Java truyền thống (trước Java 16 Pattern Matching), sau khi kiểm tra `if (obj instanceof String)`,
 *      bạn vẫn phải ép kiểu thủ công `String s = (String) obj;` rồi mới gọi được `s.length()`.
 *    - Trong Kotlin, toán tử `is` (tương đương `instanceof`) và `!is` đi kèm cơ chế **Smart Cast**:
 *      + Ngay khi trình biên dịch chứng minh được một biến thỏa mãn kiểu `T` (hoặc khác `null`),
 *        nó **tự động ép kiểu ngầm trong Bytecode** ở mọi dòng code nằm trong phạm vi đó!
 *    - **Khi nào Smart Cast HOẠT ĐỘNG và khi nào KHÔNG?**
 *      + ✅ Hoạt động với: Biến cục bộ `val`, thuộc tính `val` (không có custom getter), và biến cục bộ `var`
 *        nếu không bị thay đổi giữa lúc kiểm tra và lúc sử dụng.
 *      + ❌ KHÔNG hoạt động với: Thuộc tính `var` của một class (`class Box(var data: Any?)`).
 *        **Lý do (Concurrency / Multi-threading)**: Giữa dòng lệnh `if (box.data is String)` và dòng tiếp theo
 *        `box.data.length`, một luồng (Thread) khác trên CPU có thể đã gán `box.data = 123` (kiểu `Int`) trên HEAP!
 *        Vì vậy trình biên dịch Kotlin từ chối Smart Cast để bảo vệ bạn khỏi lỗi `ClassCastException` lúc runtime!
 *
 * 2. ÉP KIỂU AN TOÀN (`as?`) VS ÉP KIỂU KHÔNG AN TOÀN (`as`):
 *    - `obj as String` (Unsafe Cast): Giống `(String) obj` của Java. Nếu `obj` là `Int` hoặc `null`, nổ ngay `ClassCastException` / `NullPointerException`.
 *    - `obj as? String` (Safe Cast): Nếu `obj` đúng là `String`, trả về `String`. Nếu KHÔNG phải (hoặc là `null`),
 *      trả về `null` một cách êm ái mà không ném bất kỳ ngoại lệ nào!
 *      Cực kỳ ăn ý khi kết hợp với Elvis: `val str = anyObj as? String ?: "Default"`.
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong Spring Security, `SecurityContextHolder.getContext().authentication?.principal` trả về kiểu `Any?`.
 *      Lập trình viên dùng `val userDetails = principal as? CustomUserDetails ?: throw AccessDeniedException(...)`
 *      để lấy thông tin người dùng đăng nhập cực kỳ gọn và an toàn!
 * =====================================================================
 */

// Class minh họa tại sao thuộc tính `var` trên Heap không thể Smart Cast trực tiếp
private class MutablePayloadHolder(
    val immutableHeader: Any = "Bearer-Token-XYZ",
    var mutableBody: Any? = "Nội dung phản hồi dạng chuỗi"
)

fun main() {
    println("--- ___ * START: Ex03SmartCastAndSafeCast * ___ ---\n")

    // =================================================================
    // 1. Smart Cast với `is`, `!is` và kiểm tra `!= null`
    // =================================================================
    println("1. [Smart Cast] Tự động ép kiểu sau khi kiểm tra `is`, `!is` hoặc `!= null`:")
    processIncomingEvent("Đơn hàng #9001 đã thanh toán")
    processIncomingEvent(listOf("ITEM-A", "ITEM-B", "ITEM-C"))
    processIncomingEvent(404)
    println()

    // =================================================================
    // 2. Smart Cast ngay trong biểu thức logic ngắn mạch (&& và ||)
    // =================================================================
    println("2. [Short-circuit Smart Cast] Tự động ép kiểu ngay ở vế phải của `&&` và `||`:")
    val possibleToken: Any? = "JWT_SECRET_TOKEN_2026"

    // Ở vế phải của `&&`, trình biên dịch BIẾT CHẮC `possibleToken` đã là `String` nên gọi `.startsWith()` trực tiếp!
    if (possibleToken is String && possibleToken.startsWith("JWT_")) {
        println("   -> Token hợp lệ (độ dài = ${possibleToken.length}): $possibleToken\n")
    }

    // =================================================================
    // 3. Khi nào Smart Cast KHÔNG hoạt động (Thuộc tính `var` trên Heap) & Cách xử lý
    // =================================================================
    println("3. [Smart Cast Limitation] Tại sao thuộc tính `var` của Object không được Smart Cast:")
    val holder = MutablePayloadHolder()

    // 3a. Với thuộc tính `val immutableHeader` -> Smart Cast hoạt động bình thường
    if (holder.immutableHeader is String) {
        println("   -> [val property] Smart Cast thành công: ${holder.immutableHeader.uppercase()}")
    }

    // 3b. Với thuộc tính `var mutableBody` -> Không thể gọi trực tiếp `holder.mutableBody.length`
    // bởi vì một Thread khác có thể thay đổi `holder.mutableBody` bất cứ lúc nào!
    // Giải pháp chuẩn: Chụp (capture) giá trị vào một biến cục bộ `val` trên STACK của luồng hiện tại:
    val localSnapshot = holder.mutableBody
    if (localSnapshot is String) {
        println("   -> [var property -> local val] Snapshot an toàn trên Stack (length = ${localSnapshot.length}): \"$localSnapshot\"\n")
    }

    // =================================================================
    // 4. Safe Cast `as?` kết hợp Elvis `?:`
    // =================================================================
    println("4. [Safe Cast `as?`] Ép kiểu không bao giờ nổ ClassCastException:")
    val rawConfigValue1: Any = 9090 // Kiểu thực tế là Int, KHÔNG phải String
    val rawConfigValue2: Any = "production-cluster"

    // Nếu ép kiểu thất bại, `as?` trả về `null`, sau đó Elvis `?:` cung cấp giá trị mặc định "Default":
    val clusterName1: String = rawConfigValue1 as? String ?: "default-cluster"
    val clusterName2: String = rawConfigValue2 as? String ?: "default-cluster"

    println("   -> Ép kiểu Int (9090) sang String bằng `as?`:              \"$clusterName1\" (An toàn, không crash!)")
    println("   -> Ép kiểu String (\"production-cluster\") bằng `as?`: \"$clusterName2\"")

    println("\n--- ___ * END: Ex03SmartCastAndSafeCast * ___ ---")
}

private fun processIncomingEvent(payload: Any?) {
    // 1. Kiểm tra `!is`: nếu KHÔNG phải String và dùng `return` ở một hàm riêng:
    when {
        payload == null -> {
            println("   -> Payload rỗng (null)")
        }
        payload is String -> {
            // Tại đây `payload` đã được Smart Cast từ `Any?` sang `String` (Non-Null)!
            println("   -> [String Event] Nội dung: \"$payload\" | Độ dài = ${payload.length} ký tự")
        }
        payload is List<*> -> {
            // Tại đây `payload` đã được Smart Cast sang `List<*>`!
            println("   -> [Batch Event]  Danh sách gồm ${payload.size} phần tử: $payload")
        }
        payload !is Number -> {
            println("   -> [Unknown] Không phải kiểu số")
        }
        else -> {
            // Vì đã loại trừ `!is Number` ở nhánh trên, tại nhánh `else` này `payload` được Smart Cast thành `Number`!
            println("   -> [Number Event] Giá trị số nguyên = ${payload.toInt()}")
        }
    }
}
