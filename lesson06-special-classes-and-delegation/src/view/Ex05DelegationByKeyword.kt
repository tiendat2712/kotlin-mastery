package view

import kotlin.properties.Delegates

/**
 * ============================================================================
 * BÀI 05: DELEGATION PATTERN VỚI TỪ KHÓA `by` (CLASS & PROPERTY DELEGATION)
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / EFFECTIVE JAVA ITEM 18 / BYTECODE):
 * ----------------------------------------------------------------------------
 * Trong *Effective Java (Item 18)*: **"Favor composition over inheritance"**
 * (Ưu tiên thành phần/ủy quyền hơn kế thừa).
 * Tuy nhiên, trong Java, việc viết **Decorator / Wrapper Pattern** rất khổ sở:
 * Nếu interface `UserRepository` có 20 phương thức mà bạn chỉ muốn bọc thêm log vào
 * đúng 1 phương thức `save()`, bạn vẫn phải tự viết tay 19 phương thức còn lại chỉ để
 * gọi `return delegate.findById(id);`!
 *
 * Trong Kotlin, từ khóa **`by`** hỗ trợ Delegation ở cấp độ ngôn ngữ:
 *
 * A. Class Delegation (`class AuditingUserRepository(private val delegate: UserRepository) : UserRepository by delegate`):
 *    - Ở tầng Bytecode, trình biên dịch Kotlin tự động sinh ra toàn bộ các hàm chuyển tiếp
 *      (forwarding methods) cho tất cả các phương thức trong `UserRepository` mà bạn không
 *      ghi đè!
 *    - Bạn chỉ cần `override` duy nhất phương thức nào bạn muốn gắn thêm logic (Decorate).
 *
 * B. Property Delegation (`by lazy`, `by Delegates.observable`, `by Delegates.vetoable`):
 *    Thay vì lặp lại logic trong `get()` và `set()` ở nhiều thuộc tính, bạn ủy quyền việc
 *    đọc/ghi thuộc tính cho một đối tượng Delegate:
 *    - `Delegates.observable(initialValue) { prop, old, new -> ... }`:
 *      Kích hoạt callback SAU KHI giá trị mới đã được gán (dùng để bắn Event, ghi Audit Log).
 *    - `Delegates.vetoable(initialValue) { prop, old, new -> Boolean }`:
 *      Kích hoạt callback TRƯỚC KHI gán. Nếu biểu thức trả về `false`, phép gán bị **từ chối (veto)**
 *      và thuộc tính giữ nguyên giá trị cũ!
 *
 * 2. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * Trong Spring Boot + Kotlin:
 *   - **Class Delegation (`by`)** cực kỳ hữu ích khi bạn muốn tạo một `CachingUserRepository`
 *     hoặc `MeteredPaymentClient` bọc ngoài một Spring Data Repository / Feign Client sẵn có
 *     mà chỉ cần can thiệp vào 1-2 hàm quan trọng.
 */

// 1. Interface với nhiều phương thức
private interface UserRepository {
    fun findById(id: String): String?
    fun findAllActive(): List<String>
    fun countUsers(): Int
    fun save(id: String, username: String)
}

private class InMemoryUserRepository : UserRepository {
    private val storage = linkedMapOf(
        "U01" to "alice_kotlin",
        "U02" to "bob_spring"
    )

    override fun findById(id: String): String? = storage[id]
    override fun findAllActive(): List<String> = storage.values.toList()
    override fun countUsers(): Int = storage.size
    override fun save(id: String, username: String) {
        storage[id] = username
    }
}

// 2. Class Delegation với `by delegate`:
// Tự động chuyển tiếp `findById`, `findAllActive`, `countUsers` sang `delegate` mà không tốn 1 dòng code thừa!
// Chỉ ghi đè duy nhất hàm `save` để bổ sung Audit Log (Decorator Pattern trong 3 dòng!).
private class AuditingUserRepository(
    private val delegate: UserRepository
) : UserRepository by delegate {

    override fun save(id: String, username: String) {
        println("    [AUDIT BEFORE SAVE] Chuan bi luu user id='$id', username='$username'")
        delegate.save(id, username)
        println("    [AUDIT AFTER SAVE]  Luu thanh cong! Tong so user hien tai = ${countUsers()}")
    }
}

// 3. Property Delegation với `Delegates.observable` và `Delegates.vetoable`
private class StockTicker(val symbol: String, initialPrice: Double) {

    // `observable`: Tự động thông báo mỗi khi trạng thái thay đổi
    var tradingStatus: String by Delegates.observable("CLOSED") { property, oldValue, newValue ->
        println("    [Observable '${property.name}'] Trang thai san '$symbol' chuyen tu '$oldValue' -> '$newValue'")
    }

    // `vetoable`: Quyền phủ quyết (chặn các giá trị không hợp lệ trước khi ghi vào bộ nhớ)
    // Giả sử quy định biên độ sàn chứng khoán: Giá mới không được giảm sâu quá 20% so với giá cũ!
    var currentPrice: Double by Delegates.vetoable(initialPrice) { property, oldPrice, newPrice ->
        val minAllowed = oldPrice * 0.80
        val accepted = newPrice >= minAllowed
        if (!accepted) {
            println("    [Vetoable REJECTED] Tu choi cap nhat '${property.name}' tu $oldPrice -> $newPrice (Thap hon gia san $minAllowed)")
        } else {
            println("    [Vetoable ACCEPTED] Chap nhan cap nhat '${property.name}' tu $oldPrice -> $newPrice")
        }
        accepted
    }
}

fun main() {
    println("--- ___ * START: Ex05DelegationByKeyword * ___ ---")

    // ========================================================================
    // 1. Class Delegation (`by delegate` - Decorator Pattern)
    // ========================================================================
    println("\n[1] Class Delegation (`AuditingUserRepository : UserRepository by delegate`):")
    val repo: UserRepository = AuditingUserRepository(InMemoryUserRepository())

    // Các hàm không override vẫn hoạt động hoàn hảo nhờ ủy quyền tự động
    println("  Tim U01 (uy quyen tu dong) : ${repo.findById("U01")}")
    // Hàm `save` chạy qua lớp bọc AuditingUserRepository
    repo.save("U03", "charlie_ddd")
    println("  Danh sach tat ca users     : ${repo.findAllActive()}")

    // ========================================================================
    // 2. Property Delegation (`Delegates.observable` & `Delegates.vetoable`)
    // ========================================================================
    println("\n[2] Property Delegation (`observable` & `vetoable`):")
    val fptStock = StockTicker(symbol = "FPT", initialPrice = 100.0)

    fptStock.tradingStatus = "OPEN_MORNING_SESSION"

    // Cập nhật hợp lệ (giảm 5% -> trong biên độ cho phép)
    fptStock.currentPrice = 95.0
    println("  Gia FPT hien tai: ${fptStock.currentPrice}")

    // Cập nhật không hợp lệ (rơi xuống 50.0 -> giảm quá 20% -> bị phủ quyết!)
    fptStock.currentPrice = 50.0
    println("  Gia FPT sau khi bi Veto giu nguyen o muc: ${fptStock.currentPrice}")

    println("\n--- ___ * FINISH: Ex05DelegationByKeyword * ___ ---")
}
