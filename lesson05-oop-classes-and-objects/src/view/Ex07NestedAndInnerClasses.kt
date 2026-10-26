package view

/**
 * ============================================================================
 * BÀI 07: NESTED CLASS vs INNER CLASS - SỰ ĐẢO NGƯỢC THÔNG MINH SO VỚI JAVA
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE & MEMORY LEAK):
 * ----------------------------------------------------------------------------
 * Trong cuốn *Effective Java (Item 24)*, Joshua Bloch khuyên:
 *   "Favor static member classes over nonstatic"
 *   (Hãy ưu tiên dùng static nested class thay vì non-static inner class!).
 *
 * Tại sao? Hãy nhìn vào sự ĐẢO NGƯỢC MẶC ĐỊNH giữa Java và Kotlin:
 *
 * +------------------------------------+------------------------------------+
 * | JAVA                               | KOTLIN                             |
 * +------------------------------------+------------------------------------+
 * | `class Outer { class Inner {} }`   | `class Outer { class Nested {} }`  |
 * | -> MẶC ĐỊNH là **Non-static**!     | -> MẶC ĐỊNH là **`static`**!       |
 * | Bytecode tự động nhét thêm 1 biến  | Bytecode biên dịch thành           |
 * | ẩn `final Outer this$0;` vào trong | `public static final class Nested` |
 * | `Inner` trên HEAP.                 | Hoàn toàn ĐỘC LẬP với instance của |
 * | Hậu quả: Nếu `Inner` sống lâu hơn  | `Outer`, KHÔNG giữ tham chiếu ẩn   |
 * | `Outer`, Garbage Collector (GC)    | `this$0`, KHÔNG bao giờ gây rò rỉ  |
 * | KHÔNG THỂ dọn dẹp `Outer` ->       | bộ nhớ (Zero Memory Leak risk)!    |
 * | **Memory Leak** hoặc lỗi           |                                    |
 * | Serialization (Jackson/JPA)!       |                                    |
 * +------------------------------------+------------------------------------+
 * | Muốn độc lập phải gõ tường minh:   | Chỉ khi THỰC SỰ cần tham chiếu tới |
 * | `static class Nested {}`           | instance của `Outer`, bạn mới chủ  |
 * | (Lập trình viên rất hay quên gõ!)  | động gõ từ khóa `inner`:           |
 * |                                    | `inner class Inner {}`             |
 * +------------------------------------+------------------------------------+
 *
 * 2. CÁCH TRUY CẬP INSTANCE BAO NGOÀI & CÚ PHÁP `this@Outer`:
 * ----------------------------------------------------------------------------
 * - **Nested Class (`class Nested`)**:
 *   + Khởi tạo trực tiếp KHÔNG cần tạo đối tượng `Outer`: `Outer.Nested()`.
 *   + KHÔNG thể truy cập các thuộc tính/phương thức của đối tượng `Outer`.
 * - **Inner Class (`inner class Inner`)**:
 *   + Bắt buộc phải khởi tạo thông qua một instance của `Outer`: `outerInstance.Inner()`.
 *   + Có chứa tham chiếu ẩn `this$0` trỏ tới `outerInstance` trên HEAP.
 *   + Khi cả `Outer` và `Inner` có thuộc tính hoặc phương thức trùng tên (Shadowing),
 *     ta dùng **Qualified `this`**:
 *       - `this.name`       -> thuộc tính `name` của `Inner`.
 *       - `this@Outer.name` -> thuộc tính `name` của `Outer`.
 *
 * 3. KHI NÀO DÙNG TRONG BACKEND / SPRING BOOT:
 * ----------------------------------------------------------------------------
 * - **Nested Class (`class` / `data class` lồng bên trong)**:
 *   + Gom nhóm các API Request / Response DTO theo từng nghiệp vụ để tránh tạo hàng chục
 *     file nhỏ lẻ trong package `dto`:
 *     ```kotlin
 *     class OrderPayloads {
 *         data class CreateReq(val customerId: String, val amount: Long)
 *         data class SummaryRes(val orderId: String, val status: String)
 *     }
 *     ```
 *     Nhờ mặc định là `static` dưới Bytecode, Jackson dễ dàng serialize/deserialize
 *     `OrderPayloads.CreateReq` mà không bao giờ hỏi đòi instance của `OrderPayloads`!
 *   + Cài đặt Builder Pattern hoặc các helper phục vụ Specification.
 *
 * - **Inner Class (`inner class`)**:
 *   + Khi thiết kế một thành phần con gắn chặt với trạng thái (state) của một đối tượng
 *     Aggregate cụ thể (ví dụ: `BatchJobRunner.StepCursor`, `ShoppingCart.CheckoutSession`
 *     cần đọc/cập nhật trực tiếp trạng thái bên trong `ShoppingCart` đang mở).
 */

// ============================================================================
// MINH HỌA 1: NESTED CLASS (MẶC ĐỊNH LÀ `static` TRÊN JVM)
// ============================================================================
private class OrderPayloads {
    // Khong co instance state nao cua OrderPayloads can duoc giu lai
    data class CreateRequest(
        val customerId: String,
        val totalCents: Long
    )

    data class SummaryResponse(
        val orderId: String,
        val customerId: String,
        val formattedTotal: String
    )
}

// ============================================================================
// MINH HỌA 2: INNER CLASS & QUALIFIED `this@Outer`
// ============================================================================
private class WarehouseAggregate(
    val code: String,
    private var availableStock: Int
) {
    // Inner class: Gắn chặt với một kho hàng (WarehouseAggregate) cụ thể trên HEAP
    inner class ReservationSession(val code: String) {

        fun reserve(quantity: Int): String {
            require(quantity > 0) { "So luong dat giu phai > 0" }
            if (quantity > availableStock) {
                return "REJECTED [warehouse=${this@WarehouseAggregate.code}, session=${this.code}, remain=$availableStock]"
            }
            // Truy cập và cập nhật trực tiếp thuộc tính `private var availableStock` của Outer instance!
            availableStock -= quantity

            // Phân biệt rõ `this.code` (của ReservationSession) và `this@WarehouseAggregate.code` (của WarehouseAggregate)
            return "RESERVED [warehouse=${this@WarehouseAggregate.code}, session=${this.code}, qty=$quantity, remain=$availableStock]"
        }
    }

    fun currentStock(): Int = availableStock
}

// ============================================================================
// PHỤC VỤ GÓC TỰ LUYỆN:
// ============================================================================
private class ServerCluster(val name: String) {
    inner class WorkerNode(val name: String) {
        fun fullIdentifier(): String = "${this@ServerCluster.name}::${this.name}"
    }
}

private class InvoiceBuilder(
    private val invoiceNo: String,
    private val taxPercent: Int
) {
    private val lineAmounts = mutableListOf<Long>()

    // [LỜI GIẢI THAM KHẢO BÀI 3]: Inner class cộng thêm dòng hàng vào `lineAmounts` của `InvoiceBuilder`
    inner class LineAdder(val categoryPrefix: String) {
        fun addLine(amount: Long): String {
            require(amount > 0) { "So tien phai > 0" }
            lineAmounts.add(amount)
            return "[${this@InvoiceBuilder.invoiceNo}/$categoryPrefix] +$amount"
        }
    }

    fun calculateGrandTotal(): Long {
        val subtotal = lineAmounts.sum()
        return subtotal + (subtotal * taxPercent / 100)
    }
}

fun main() {
    println("--- ___ * START: Ex07NestedAndInnerClasses * ___ ---")

    // ========================================================================
    // 1. Nested Class (`static` mặc định - Độc lập, không cần Outer instance)
    // ========================================================================
    println("\n[1] Nested Class (Mac dinh la `static` duoi Bytecode - An toan cho DTO):")
    // Khoi tao truc tiep OrderPayloads.CreateRequest ma KHONG goi OrderPayloads()
    val req = OrderPayloads.CreateRequest(customerId = "CUST-101", totalCents = 250_000L)
    val res = OrderPayloads.SummaryResponse(
        orderId = "ORD-2026-01",
        customerId = req.customerId,
        formattedTotal = "${req.totalCents} VND"
    )
    println("  Request DTO  : $req")
    println("  Response DTO : $res")
    check(res.customerId == "CUST-101")
    check(res.formattedTotal == "250000 VND")

    // ========================================================================
    // 2. Inner Class (`inner` - Giữ tham chiếu tới Outer & dùng `this@Outer`)
    // ========================================================================
    println("\n[2] Inner Class (`inner class`) va Qualified `this@WarehouseAggregate`:")
    val hanoiWarehouse = WarehouseAggregate(code = "WH-HANOI", availableStock = 100)

    // Bắt buộc phải khởi tạo ReservationSession thông qua đối tượng `hanoiWarehouse`
    val session1 = hanoiWarehouse.ReservationSession(code = "SESS-01")
    val log1 = session1.reserve(35)
    val log2 = session1.reserve(80) // Vượt quá số lượng còn lại (65)

    println("  Lan 1 : $log1")
    println("  Lan 2 : $log2")
    println("  Ton kho cuoi cung cua ${hanoiWarehouse.code}: ${hanoiWarehouse.currentStock()}")

    check(log1 == "RESERVED [warehouse=WH-HANOI, session=SESS-01, qty=35, remain=65]")
    check(log2 == "REJECTED [warehouse=WH-HANOI, session=SESS-01, remain=65]")
    check(hanoiWarehouse.currentStock() == 65)

    // ========================================================================
    // 3. GÓC TỰ LUYỆN (ACTIVE LEARNING)
    // ========================================================================
    println("\n[3] Goc Tu Luyen (Active Learning):")

    // ------------------------------------------------------------------------
    // Bài 1 (Dự đoán kết quả): `this.name` vs `this@ServerCluster.name`
    // ------------------------------------------------------------------------
    // Hãy đoán xem `node.fullIdentifier()` trả về chuỗi gì khi cả `ServerCluster`
    // và `WorkerNode` đều có thuộc tính tên là `name`?
    val cluster = ServerCluster(name = "AP-SOUTHEAST-1")
    val node = cluster.WorkerNode(name = "NODE-07")
    val fullId = node.fullIdentifier()
    println("  [Bai 1 - Du doan] fullId = $fullId")
    check(fullId == "AP-SOUTHEAST-1::NODE-07")

    // ------------------------------------------------------------------------
    // Bài 2 (Tìm lỗi): Cố truy cập thuộc tính của Outer từ một Nested Class thường
    // ------------------------------------------------------------------------
    /*
    class PaymentConfig(val apiKey: String) {
        // LỖI BIÊN DỊCH: Vì `WebhookPayload` là Nested Class (mặc định là `static`),
        // nó KHÔNG giữ tham chiếu `this$0` tới `PaymentConfig` nên không thấy `apiKey`!
        data class WebhookPayload(val eventId: String) {
            fun debugInfo(): String = "Event=$eventId, Key=$apiKey" // ERROR: Unresolved reference: apiKey
        }
    }
    */
    // BÀI HỌC KINH NGHIỆM:
    // Đừng nhắm mắt thêm từ khóa `inner` vào DTO (như `WebhookPayload`) chỉ để lấy `apiKey`!
    // Thứ nhất: Kotlin CẤM `inner data class` (`Data class is not allowed to be inner`).
    // Thứ hai: Thêm `inner` vào DTO sẽ khiến Jackson/Serialization gặp lỗi vì không tự khởi tạo
    // được class bao ngoài và làm rò rỉ tham chiếu `PaymentConfig`. Hãy truyền dữ liệu cần thiết
    // qua tham số hàm hoặc tách trách nhiệm ra Service!
    println("  [Bai 2 - Tim loi] Nested class (static) khong the truy cap instance state cua Outer.")

    // ------------------------------------------------------------------------
    // Bài 3 (Tự viết nhỏ):
    // ------------------------------------------------------------------------
    // [ĐỀ BÀI]:
    // Cho class `InvoiceBuilder(invoiceNo, taxPercent)` chứa `lineAmounts: MutableList<Long>`.
    // Hãy viết một `inner class LineAdder(val categoryPrefix: String)` có hàm
    // `addLine(amount: Long): String` thực hiện:
    //   1. Cộng `amount` vào danh sách `lineAmounts` của `InvoiceBuilder`.
    //   2. Trả về chuỗi định dạng `"[<invoiceNo>/<categoryPrefix>] +<amount>"`.
    // Sau đó kiểm tra `calculateGrandTotal()` của `InvoiceBuilder`.

    // [LỜI GIẢI THAM KHẢO]: (Xem cài đặt trong class `InvoiceBuilder` phía trên)
    val builder = InvoiceBuilder(invoiceNo = "INV-900", taxPercent = 10)
    val foodAdder = builder.LineAdder(categoryPrefix = "FOOD")
    val techAdder = builder.LineAdder(categoryPrefix = "TECH")

    val step1 = foodAdder.addLine(200_000L)
    val step2 = techAdder.addLine(800_000L)
    val grandTotal = builder.calculateGrandTotal()

    println("  [Bai 3 - Tu viet] $step1 | $step2 -> GrandTotal (gom 10% thue) = $grandTotal")
    check(step1 == "[INV-900/FOOD] +200000")
    check(step2 == "[INV-900/TECH] +800000")
    check(grandTotal == 1_100_000L)

    println("\n--- ___ * FINISH: Ex07NestedAndInnerClasses * ___ ---")
}
