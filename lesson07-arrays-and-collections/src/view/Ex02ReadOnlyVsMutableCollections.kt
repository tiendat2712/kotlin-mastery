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
 * 2. PHÂN BIỆT `val`/`var` (CON TRỎ TRÊN STACK) vs `List`/`MutableList` (DỮ LIỆU TRÊN HEAP):
 * ----------------------------------------------------------------------------
 * Đây là câu hỏi phỏng vấn cực kỳ phổ biến:
 *   - `val items = mutableListOf("A", "B")`:
 *     + `val`: Khóa con trỏ trên STACK (không thể gán `items = mutableListOf("C")`).
 *     + `mutableListOf`: Đối tượng `ArrayList` trên HEAP vẫn cho phép gọi `items.add("C")`!
 *   - `var items = listOf("A", "B")`:
 *     + `listOf`: Không thể sửa phần tử bên trong danh sách hiện tại (`items.add` không tồn tại).
 *     + `var`: Cho phép trỏ biến `items` sang một danh sách hoàn toàn mới trên HEAP (`items = items + "C"`).
 *
 * 3. SPRING KOTLIN CORNER (DEFENSIVE COPYING & ENCAPSULATION):
 * ----------------------------------------------------------------------------
 * Trong Domain Model hoặc Stateful Service:
 *   - Bên trong class, bạn giữ `private val _orders = mutableListOf<String>()` để thêm đơn hàng.
 *   - Khi trả ra ngoài cho tầng Controller, nếu bạn chỉ ép kiểu `val orders: List<String> get() = _orders`,
 *     kẻ xấu vẫn có thể ép kiểu ngược `(service.orders as MutableList).clear()`!
 *   - Để bảo vệ tuyệt đối vùng nhớ HEAP nội bộ, hãy dùng `.toList()` (Defensive Copy) hoặc
 *     trả về Read-Only interface kết hợp bản sao bất biến!
 */

private class OrderCartService {
    // Trạng thái nội bộ trên HEAP có thể thay đổi nhưng bị khóa `private`
    private val _items: MutableList<String> = mutableListOf()

    // Cấp quyền chỉ đọc (Read-only view) kèm Defensive Copy (`.toList()`)
    // để đảm bảo bên ngoài không bao giờ làm hỏng danh sách `_items` bên trong
    val items: List<String>
        get() = _items.toList()

    fun addItem(sku: String) {
        require(sku.isNotBlank()) { "Mã sản phẩm không được trống" }
        _items.add(sku)
    }
}

fun main() {
    println("--- ___ * START: Ex02ReadOnlyVsMutableCollections * ___ ---")

    // ========================================================================
    // 1. Read-Only `List<T>` vs Mutable `MutableList<T>`
    // ========================================================================
    println("\n[1] Sự khác biệt giữa `listOf` (Read-Only) và `mutableListOf` (Mutable):")
    val readOnlyRoles: List<String> = listOf("ROLE_USER", "ROLE_AUDITOR")
    val mutableRoles: MutableList<String> = mutableListOf("ROLE_USER", "ROLE_AUDITOR")

    // readOnlyRoles.add("ROLE_ADMIN") // ERROR lúc Compile: Unresolved reference 'add'!
    mutableRoles.add("ROLE_ADMIN")

    println("  readOnlyRoles : $readOnlyRoles")
    println("  mutableRoles  : $mutableRoles")

    // ========================================================================
    // 2. Phân biệt `val` (khóa tham chiếu STACK) vs `List` (khóa sửa đổi HEAP)
    // ========================================================================
    println("\n[2] Kết hợp chuẩn mực nhất: `val` + `listOf` (Bất biến cả STACK lẫn HEAP):")
    var rebindableList: List<String> = listOf("Java 17")
    // Toán tử `+` trên Read-only List KHÔNG sửa list cũ mà tạo ra 1 List MỚI trên HEAP
    rebindableList = rebindableList + "Kotlin 2.0" + "Spring Boot 3"
    println("  Danh sách sau khi tạo bản sao mới bằng toán tử `+`: $rebindableList")

    // ========================================================================
    // 3. Bảo vệ trạng thái nội bộ với Defensive Copy (`.toList()`)
    // ========================================================================
    println("\n[3] Defensive Copy (`.toList()`) bảo vệ dữ liệu trong Service:")
    val cart = OrderCartService()
    cart.addItem("IPHONE-16-PRO")
    cart.addItem("AIRPODS-PRO-2")

    val snapshot = cart.items
    println("  Snapshot lần 1: $snapshot")

    cart.addItem("MACBOOK-PRO-M4")
    println("  Snapshot lần 1 vẫn giữ nguyên (nhờ .toList()): $snapshot")
    println("  Snapshot hiện tại của giỏ hàng               : ${cart.items}")

    println("\n--- ___ * FINISH: Ex02ReadOnlyVsMutableCollections * ___ ---")
}
