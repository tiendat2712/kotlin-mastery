package view

/**
 * ============================================================================
 * BÀI 01: ARRAYS TRONG KOTLIN - PRIMITIVE (`IntArray`) vs BOXED (`Array<Int>`)
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ----------------------------------------------------------------------------
 * Trong Java (`lesson10-arrays`), bạn phân biệt rất rõ:
 *   - `int[]`     : Mảng nguyên thủy (Primitive array) -> các số `int` (4 bytes) nằm
 *                   liền kề nhau (contiguous memory) trên HEAP, cực nhanh và thân thiện CPU Cache.
 *   - `Integer[]` : Mảng đối tượng (Boxed array) -> mảng chỉ lưu các con trỏ tham chiếu,
 *                   mỗi con trỏ lại trỏ tới một đối tượng `Integer` riêng biệt trên HEAP
 *                   (tốn gấp 4-5 lần bộ nhớ và gây áp lực lên Garbage Collector!).
 *
 * Trong Kotlin, vì mọi kiểu dữ liệu đều nhìn giống như Class (`Int`, `Double`), rất nhiều
 * lập trình viên chuyển từ Java sang mắc sai lầm nghiêm trọng khi dùng `Array<Int>`:
 *   - `Array<Int>` (tạo bằng `arrayOf(1, 2, 3)`):
 *     Biên dịch ra Bytecode là **`Integer[]` (BOXED)**!
 *   - `IntArray`, `LongArray`, `DoubleArray`, `ByteArray` (tạo bằng `intArrayOf(1, 2, 3)`):
 *     Biên dịch ra Bytecode là **`int[]`, `long[]`, `double[]`, `byte[]` (PRIMITIVE)**!
 *
 * 2. CẠM BẪY SO SÁNH MẢNG (`==` vs `contentEquals`):
 * ----------------------------------------------------------------------------
 * Khác với `List` (nơi toán tử `==` gọi `equals()` để so sánh từng phần tử), đối với
 * `Array` trên JVM, `arr1 == arr2` vẫn gọi `Object.equals()` (tức là chỉ so sánh địa chỉ
 * ô nhớ trên HEAP giống hệt Java!).
 * Để so sánh giá trị bên trong mảng trong Kotlin:
 *   - Mảng 1 chiều: Dùng `arr1.contentEquals(arr2)` (tương đương `Arrays.equals(a, b)`).
 *   - Mảng đa chiều: Dùng `matrix1.contentDeepEquals(matrix2)` (tương đương `Arrays.deepEquals(a, b)`).
 *   - In mảng ra màn hình: Dùng `arr.contentToString()` / `matrix.contentDeepToString()`.
 *
 * 3. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * - Khi làm việc với dữ liệu nhị phân trong Spring Boot (Upload/Download file PDF, Excel,
 *   mã hóa AES/RSA, đọc `InputStream.readAllBytes()`), hãy LUÔN DÙNG **`ByteArray`** (`byte[]`)
 *   chứ tuyệt đối không dùng `Array<Byte>` (`Byte[]`)!
 * - Lưu ý trong `data class`: Nếu một `data class` có thuộc tính là `ByteArray`, hàm
 *   `equals()`/`hashCode()` tự sinh của `data class` vẫn so sánh `ByteArray` theo địa chỉ vùng nhớ!
 *   Vì vậy, bạn nên override `equals()`/`hashCode()` dùng `contentEquals()`/`contentHashCode()`.
 */

fun main() {
    println("--- ___ * START: Ex01ArraysPrimitiveVsBoxed * ___ ---")

    // ========================================================================
    // 1. Boxed Array (`Array<Int>` -> `Integer[]`) vs Primitive Array (`IntArray` -> `int[]`)
    // ========================================================================
    println("\n[1] Phân biệt Boxed `Array<Int>` và Primitive `IntArray` dưới JVM Bytecode:")
    val boxedArray: Array<Int> = arrayOf(10, 20, 30)
    val primitiveArray: IntArray = intArrayOf(10, 20, 30)

    println("  Kiểu JVM của Array<Int> : ${boxedArray.javaClass.simpleName} (Boxed object array)")
    println("  Kiểu JVM của IntArray   : ${primitiveArray.javaClass.simpleName} (Contiguous primitive array)")

    // Khởi tạo mảng bằng hàm конструкtơ kết hợp Lambda: `IntArray(size) { index -> ... }`
    val squares = IntArray(5) { i -> (i + 1) * (i + 1) }
    println("  Khởi tạo bằng Lambda    : ${squares.contentToString()}")

    // ========================================================================
    // 2. Truy cập phần tử, duyệt mảng (`withIndex`) và Sắp xếp (`sortedArray` vs `sort`)
    // ========================================================================
    println("\n[2] Sắp xếp tạo mảng mới (`sortedArrayDescending`) vs Sắp xếp tại chỗ (`sort`):")
    val scores = intArrayOf(75, 92, 60, 88, 95)

    // `sortedArrayDescending()` trả về một mảng MỚI trên HEAP, giữ nguyên mảng `scores` gốc
    val descCopy = scores.sortedArrayDescending()
    println("  Bản sao sắp xếp giảm dần : ${descCopy.contentToString()}")
    println("  Mảng gốc `scores` vẫn là : ${scores.contentToString()}")

    // `sort()` thay đổi trực tiếp (in-place mutation) trên mảng `scores` gốc
    scores.sort()
    println("  Sau khi gọi scores.sort(): ${scores.contentToString()}")

    // ========================================================================
    // 3. So sánh mảng: `==` vs `contentEquals` & `contentDeepEquals`
    // ========================================================================
    println("\n[3] So sánh mảng 1 chiều và 2 chiều (`contentEquals` vs `contentDeepEquals`):")
    val a1 = intArrayOf(1, 2, 3)
    val a2 = intArrayOf(1, 2, 3)

    println("  a1 == a2 (SAI - so sánh địa chỉ HEAP) : ${a1 == a2}")
    println("  a1.contentEquals(a2) (ĐÚNG)           : ${a1.contentEquals(a2)}")

    val matrix1 = arrayOf(intArrayOf(1, 2), intArrayOf(3, 4))
    val matrix2 = arrayOf(intArrayOf(1, 2), intArrayOf(3, 4))
    println("  Ma trận 2D: ${matrix1.contentDeepToString()} | contentDeepEquals = ${matrix1.contentDeepEquals(matrix2)}")

    println("\n--- ___ * FINISH: Ex01ArraysPrimitiveVsBoxed * ___ ---")
}
