package view

/**
 * ============================================================================
 * BÀI 05: MAPS, `getOrPut` CACHING/GROUPING & BIẾN ĐỔI KEY/VALUE
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ----------------------------------------------------------------------------
 * A. Cặp khóa-giá trị với Infix Function `to`:
 *    Khi bạn viết `mapOf("VN" to "Vietnam", "JP" to "Japan")`:
 *    - Từ khóa `to` không phải là cú pháp đặc biệt của trình biên dịch, mà là một
 *      **Infix Extension Function**: `infix fun <A, B> A.to(that: B): Pair<A, B> = Pair(this, that)`.
 *    - Tương tự `setOf`, `mapOf` và `mutableMapOf` mặc định tạo **`java.util.LinkedHashMap`**
 *      trên HEAP để bảo toàn thứ tự chèn của các Key!
 *
 * B. Truy cập & Cập nhật bằng Toán tử Indexing `[]`:
 *    - Trong Java: `map.get("VN")` và `map.put("VN", "Việt Nam")`.
 *    - Trong Kotlin: `map["VN"]` (trả về kiểu Nullable `V?` vì key có thể không tồn tại!)
 *      và `mutableMap["VN"] = "Việt Nam"`.
 *    - Các hàm truy cập nâng cao:
 *      + `map.getOrDefault("US", "Unknown")`
 *      + `map.getOrElse("US") { computeFallback() }` (Lambda chỉ chạy khi không tìm thấy key - Lazy!)
 *      + `map.getValue("VN")`: Ném `NoSuchElementException` ngay nếu không có key (thay vì trả về `null`).
 *
 * C. Siêu hàm `getOrPut(key) { defaultValue }` trên `MutableMap`:
 *     Tương đương `computeIfAbsent` của Java 8 nhưng ngắn gọn và trực quan hơn rất nhiều:
 *    - Nếu `key` đã có trong Map -> trả về value hiện tại.
 *    - Nếu `key` chưa có -> chạy Lambda tạo value mới, tự động `put(key, newValue)` vào Map,
 *      rồi trả về `newValue` đó!
 *
 * 2. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * - `getOrPut` được dùng thường xuyên khi xây dựng In-Memory Cache cục bộ trong một
 *   Request Context hoặc gom nhóm dữ liệu động.
 * - `mapValues { (k, v) -> ... }` và `mapKeys { (k, v) -> ... }` giúp biến đổi trực tiếp
 *   một `Map` sang `Map` mới mà không cần `.entrySet().stream().collect(Collectors.toMap(...))`
 *   dài dòng như trong Java Stream API!
 */

fun main() {
    println("--- ___ * START: Ex05MapsAndDestructuring * ___ ---")

    // ========================================================================
    // 1. Khởi tạo Map (`mapOf`, `buildMap`) và Truy cập an toàn
    // ========================================================================
    println("\n[1] Khởi tạo Map và các cách truy cập Key:")
    val countryMap: Map<String, String> = mapOf(
        "VN" to "Vietnam",
        "JP" to "Japan",
        "SG" to "Singapore"
    )

    println("  countryMap[\"VN\"]                  : ${countryMap["VN"]}")
    println("  countryMap[\"US\"] (không tồn tại)  : ${countryMap["US"]}")
    println("  countryMap.getOrDefault(\"US\", ..) : ${countryMap.getOrDefault("US", "Not Configured")}")

    // ========================================================================
    // 2. Gom nhóm / Caching siêu tốc với `getOrPut(key) { ... }`
    // ========================================================================
    println("\n[2] Sử dụng `getOrPut` để gom nhóm nhân viên theo phòng ban:")
    val rawRecords = listOf(
        "Backend" to "An",
        "Frontend" to "Binh",
        "Backend" to "Cuong",
        "DevOps" to "Dung",
        "Backend" to "Em"
    )

    val employeesByDept = mutableMapOf<String, MutableList<String>>()
    for ((dept, empName) in rawRecords) {
        // Nếu phòng ban `dept` chưa có trong Map, tự động khởi tạo `mutableListOf()`,
        // lưu vào Map rồi `.add(empName)` ngay trên cùng 1 dòng!
        employeesByDept.getOrPut(dept) { mutableListOf() }.add(empName)
    }
    println("  Kết quả gom nhóm: $employeesByDept")

    // ========================================================================
    // 3. Duyệt Map với Destructuring & Biến đổi Map với `mapValues` / `mapKeys`
    // ========================================================================
    println("\n[3] Duyệt Map với Destructuring `for ((key, value) in map)` & `mapValues`:")
    for ((dept, members) in employeesByDept) {
        println("  - Phòng $dept (${members.size} người): ${members.joinToString(", ")}")
    }

    // Biến đổi Map<String, List<String>> -> Map<String, Int> (đếm quân số từng phòng)
    val headcountByDept: Map<String, Int> = employeesByDept.mapValues { (_, members) -> members.size }
    println("  Sau khi .mapValues (Quân số từng phòng): $headcountByDept")

    println("\n--- ___ * FINISH: Ex05MapsAndDestructuring * ___ ---")
}
