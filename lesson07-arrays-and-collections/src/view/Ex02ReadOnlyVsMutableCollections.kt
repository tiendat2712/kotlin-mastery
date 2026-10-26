package view

/**
 * ============================================================================
 * BÀI 02: HỆ PHÂN CẤP COLLECTION KÉP - READ-ONLY (`List`) vs MUTABLE (`MutableList`)
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ----------------------------------------------------------------------------
 * Trong Java (`lesson11-colection`), interface `java.util.List<T>` chứa luôn các hàm
 * thay đổi dữ liệu: `.add()`, `.remove()`, `.clear()`.
 * Hậu quả trong Java:
 *   - Nếu bạn dùng `List.of(1, 2, 3)` (Java 9+), kiểu trả về vẫn là `java.util.List<Integer>`.
 *   - Lập trình viên vẫn gọi được `list.add(4)` mà trình biên dịch KHÔNG HỀ BÁO LỖI,
 *     để rồi chương trình nổ `UnsupportedOperationException` ngay trên Production lúc Runtime!
 *
 * Thiết kế thiên tài của Kotlin: **Tách biệt giao diện ĐỌC và giao diện GHI ngay từ Compile-time!**
 *   - **Read-Only Interfaces**: `Iterable<T>` -> `Collection<T>` -> `List<T>`, `Set<T>`, `Map<K, V>`
 *     + CHỈ chứa các hàm đọc (`size`, `get`, `contains`, `iterator`).
 *     + Hoàn toàn KHÔNG CÓ hàm `add()`, `remove()`, `clear()`, `put()`!
 *   - **Mutable Interfaces**: `MutableIterable<T>` -> `MutableCollection<T>` -> `MutableList<T>`, `MutableSet<T>`, `MutableMap<K, V>`
 *     + Kế thừa các interface Read-Only tương ứng và bổ sung thêm các hàm thêm/sửa/xóa.
 *
 * Ở tầng Bytecode JVM:
 * Cả `kotlin.collections.List` và `kotlin.collections.MutableList` đều ánh xạ về
 * `java.util.List` của JVM (thường là `java.util.ArrayList` trên HEAP), nhưng trình biên dịch
 * Kotlin đóng vai trò "người gác cổng", chặn mọi lệnh sửa đổi nếu biến có kiểu là `List<T>`!
 *
 * 2. PHÂN BIỆT RẠCH RÒI 4 KHÁI NIỆM: `val` vs `List<T>` vs `toList()` vs DEEP IMMUTABILITY:
 * ----------------------------------------------------------------------------
 * Rất nhiều lập trình viên nhầm tưởng viết `val items: List<T>` là "bất biến hoàn toàn".
 * Thực chất bạn cần phân biệt chính xác 4 tầng bảo vệ sau:
 *   1. **`val` (Read-only Reference)**:
 *      Chỉ khóa biến tham chiếu (trên STACK hoặc field), ngăn không cho gán lại `=` sang
 *      một đối tượng khác (`items = anotherList`). Nó KHÔNG ngăn việc sửa dữ liệu bên trong HEAP!
 *   2. **`List<T>` (Read-only Interface / View)**:
 *      Là một **giao diện chỉ đọc** (thiếu các hàm `add`, `remove`, `clear`), **KHÔNG phải là lời
 *      đảm bảo bất biến (NOT a guarantee of immutability)**! Nếu có một biến `MutableList<T>`
 *      khác đang cùng trỏ tới danh sách đó (aliasing), dữ liệu qua góc nhìn `List<T>` vẫn thay đổi!
 *   3. **`.toList()` (Shallow Defensive Copy)**:
 *      Tạo một **bản sao nông (shallow copy)** của vỏ danh sách tại thời điểm gọi. Nó bảo vệ
 *      danh sách gốc khỏi việc thêm/xóa phần tử (`add`/`remove`) ở danh sách ngoài, nhưng **KHÔNG
 *      sao chép sâu (deep-copy)** các phần tử nằm bên trong!
 *   4. **Deep Immutability (Bất biến sâu thực sự)**:
 *      Đòi hỏi CẢ HAI điều kiện:
 *        (a) Vỏ Collection không thể bị sửa đổi (và không bị lộ tham chiếu `MutableList` ra ngoài), VÀ
 *        (b) Bản thân kiểu phần tử `T` bên trong cũng phải bất biến sâu (ví dụ `String`, `Int`,
 *            hoặc `data class` chỉ chứa các trường `val` thuộc kiểu bất biến).
 *      *(Xem tiếp Bài 06 `Ex06ImmutabilityAliasingAndSharedState.kt` để thực hành sâu về các cạm bẫy này!)*
 *
 * 3. SPRING KOTLIN CORNER (DEFENSIVE COPYING & ENCAPSULATION):
 * ----------------------------------------------------------------------------
 * Trong Domain Model hoặc Stateful Service:
 *   - Bên trong class, bạn giữ `private val _orders = mutableListOf<String>()` để thêm đơn hàng.
 *   - Khi trả ra ngoài cho tầng Controller, nếu bạn chỉ trả về `val orders: List<String> get() = _orders`,
 *     bên ngoài vẫn thấy sự thay đổi ngầm hoặc ép kiểu ngược `(service.orders as MutableList).clear()`!
 *   - Hãy dùng `.toList()` (Defensive Copy) kết hợp với phần tử bất biến (`String`, Value Object bất biến)
 *     để bảo vệ trạng thái nội bộ!
 */

private class OrderCartService {
    // Trạng thái nội bộ trên HEAP có thể thay đổi nhưng bị khóa `private`
    private val _items: MutableList<String> = mutableListOf()

    // Cấp quyền chỉ đọc (Read-only view) kèm Defensive Copy (`.toList()`)
    // Vì phần tử là `String` (bất biến), bản sao này an toàn trước các thao tác bên ngoài.
    val items: List<String>
        get() = _items.toList()

    fun addItem(sku: String) {
        require(sku.isNotBlank()) { "Ma san pham khong duoc trong" }
        _items.add(sku)
    }
}

fun main() {
    println("--- ___ * START: Ex02ReadOnlyVsMutableCollections * ___ ---")

    // ========================================================================
    // 1. Read-Only `List<T>` vs Mutable `MutableList<T>`
    // ========================================================================
    println("\n[1] Su khac biet giua `listOf` (Read-Only Interface) va `mutableListOf` (Mutable):")
    val readOnlyRoles: List<String> = listOf("ROLE_USER", "ROLE_AUDITOR")
    val mutableRoles: MutableList<String> = mutableListOf("ROLE_USER", "ROLE_AUDITOR")

    // readOnlyRoles.add("ROLE_ADMIN") // ERROR lúc Compile: Unresolved reference 'add'!
    mutableRoles.add("ROLE_ADMIN")

    println("  readOnlyRoles : $readOnlyRoles")
    println("  mutableRoles  : $mutableRoles")

    // ========================================================================
    // 2. Phân biệt `val` vs `var` khi kết hợp với Read-Only `List<String>`
    // ========================================================================
    println("\n[2] Phan biet `val` (khoa gan lai `=`) va `List<T>` (giao dien khong co ham add/remove):")
    var rebindableList: List<String> = listOf("Java 17")
    // Toán tử `+` trên Read-only List KHÔNG sửa list cũ mà tạo ra 1 List MỚI trên HEAP
    rebindableList = rebindableList + "Kotlin 2.0" + "Spring Boot 3"
    println("  Danh sach sau khi tao ban sao moi bang toan tu `+`: $rebindableList")

    // ========================================================================
    // 3. Bảo vệ trạng thái nội bộ với Defensive Copy (`.toList()`)
    // ========================================================================
    println("\n[3] Defensive Copy (`.toList()`) bao ve cau truc danh sach trong Service:")
    val cart = OrderCartService()
    cart.addItem("IPHONE-16-PRO")
    cart.addItem("AIRPODS-PRO-2")

    val snapshot = cart.items
    println("  Snapshot lan 1: $snapshot")

    cart.addItem("MACBOOK-PRO-M4")
    println("  Snapshot lan 1 van giu nguyen (nho .toList()): $snapshot")
    println("  Snapshot hien tai cua gio hang               : ${cart.items}")

    println("\n--- ___ * FINISH: Ex02ReadOnlyVsMutableCollections * ___ ---")
}
