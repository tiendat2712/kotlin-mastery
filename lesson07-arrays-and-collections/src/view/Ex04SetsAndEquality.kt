package view

/**
 * ============================================================================
 * BÀI 04: SETS, THỨ TỰ PHẦN TỬ & ĐẠI SỐ TẬP HỢP (`union`, `intersect`, `subtract`)
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ----------------------------------------------------------------------------
 * Trong Java (`lesson11-colection`), khi bạn dùng `new HashSet<>()` hoặc `Set.of(...)`,
 * thứ tự các phần tử hoàn toàn lộn xộn (không giữ thứ tự chèn vào).
 *
 * Trong Kotlin, hàm mặc định `setOf(...)` và `mutableSetOf(...)` khởi tạo một
 * **`java.util.LinkedHashSet`** dưới tầng HEAP!
 *   - Nghĩa là: Vừa đảm bảo tìm kiếm $O(1)$ qua bảng băm (`hashCode()` + `equals()`),
 *     vừa **GIỮ NGUYÊN THỨ TỰ CHÈN VÀO (Insertion Order)** nhờ danh sách liên kết kép!
 *   - Nếu bạn muốn tiết kiệm bộ nhớ hơn nữa và không cần giữ thứ tự: dùng `hashSetOf(...)` (`HashSet`).
 *   - Nếu bạn muốn tự động sắp xếp tăng dần theo `Comparable` ($O(\log N)$): dùng `sortedSetOf(...)` (`TreeSet`).
 *
 * 2. SO SÁNH ĐẠI SỐ TẬP HỢP: JAVA vs KOTLIN:
 * ----------------------------------------------------------------------------
 * Trong Java Core (`lesson11-colection`), để tìm Hợp (Union), Giao (Intersection),
 * Hiệu (Difference) của 2 tập hợp `setA` và `setB`, bạn phải dùng các hàm làm **biến dạng (mutate)**
 * tập hợp gốc:
 *   - `setA.addAll(setB)`    -> làm hỏng `setA` gốc nếu quên tạo bản copy `new HashSet<>(setA)`!
 *   - `setA.retainAll(setB)` -> lấy phần giao nhưng xóa mất dữ liệu của `setA` gốc!
 *   - `setA.removeAll(setB)` -> lấy phần hiệu nhưng xóa mất dữ liệu của `setA` gốc!
 *
 * Trong Kotlin, các phép toán tập hợp là các **Infix Functions bất biến** (trả về một `Set` mới,
 * giữ nguyên 100% `setA` và `setB` gốc) và đọc tự nhiên như tiếng Anh / Toán học:
 *   - Hợp (Union)        : `setA union setB`     (hoặc `setA + setB`)
 *   - Giao (Intersection): `setA intersect setB`
 *   - Hiệu (Difference)  : `setA subtract setB`  (hoặc `setA - setB`)
 *
 * 3. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * Trong Spring Boot, bài toán đồng bộ danh sách liên kết (ví dụ: Cập nhật danh sách `Tag`
 * của một bài viết hoặc danh sách `Role` của một `User` từ Request gửi lên so với DB hiện tại)
 * được giải quyết trong đúng 3 dòng với Set Algebra:
 *   - `val rolesToAdd    = requestedRoles subtract currentDbRoles`
 *   - `val rolesToRemove = currentDbRoles subtract requestedRoles`
 *   - `val rolesKept     = currentDbRoles intersect requestedRoles`
 */

fun main() {
    println("--- ___ * START: Ex04SetsAndEquality * ___ ---")

    // ========================================================================
    // 1. `setOf` (LinkedHashSet) vs `sortedSetOf` (TreeSet)
    // ========================================================================
    println("\n[1] `setOf` giữ nguyên thứ tự chèn (LinkedHashSet) vs `sortedSetOf` (TreeSet):")
    val insertionOrderSet = setOf("ZaloPay", "MoMo", "VNPay", "MoMo", "ZaloPay")
    val alphabeticalSet = sortedSetOf("ZaloPay", "MoMo", "VNPay", "ApplePay")

    println("  setOf (loại trùng + giữ thứ tự chèn) : $insertionOrderSet")
    println("  sortedSetOf (TreeSet sắp xếp A->Z)   : $alphabeticalSet")

    // ========================================================================
    // 2. Đại số tập hợp: `union`, `intersect`, `subtract` (Infix notation)
    // ========================================================================
    println("\n[2] Đại số tập hợp (So sánh với addAll / retainAll / removeAll trong Java):")
    val currentDbRoles = setOf("ROLE_USER", "ROLE_VIEWER", "ROLE_LEGACY")
    val requestedRoles = setOf("ROLE_USER", "ROLE_ADMIN", "ROLE_EDITOR")

    // Hợp của 2 tập hợp (Union)
    val allDistinctRoles = currentDbRoles union requestedRoles
    // Giao của 2 tập hợp (Intersection - những quyền giữ nguyên)
    val unchangedRoles = currentDbRoles intersect requestedRoles
    // Hiệu của 2 tập hợp (Difference - quyền cần thêm mới vs quyền cần thu hồi)
    val rolesToGrant = requestedRoles subtract currentDbRoles
    val rolesToRevoke = currentDbRoles subtract requestedRoles

    println("  Quyền hiện có trong DB                : $currentDbRoles")
    println("  Quyền mới yêu cầu từ giao diện Admin  : $requestedRoles")
    println("  -> Hợp (`union`)                      : $allDistinctRoles")
    println("  -> Giao (`intersect` - giữ nguyên)    : $unchangedRoles")
    println("  -> Cần cấp thêm (`requested subtract db`) : $rolesToGrant")
    println("  -> Cần thu hồi  (`db subtract requested`) : $rolesToRevoke")

    println("\n--- ___ * FINISH: Ex04SetsAndEquality * ___ ---")
}
