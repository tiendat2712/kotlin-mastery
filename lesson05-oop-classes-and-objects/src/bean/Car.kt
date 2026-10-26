package bean

/**
 * ============================================================================
 * BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ============================================================================
 * Trong `D:\Java_Journey\lesson03-oop\src\bean\Car.java`, bạn phải viết ~75 dòng
 * code gồm:
 *   - 3 thuộc tính `private` (`id`, `color`, `salesPrice`)
 *   - 1 thuộc tính `public static String model`
 *   - 2 constructors (default & full-arg)
 *   - 6 hàm getter/setter thủ công
 *   - 1 hàm `toString()`
 *
 * Trong Kotlin, toàn bộ class `Car` đó rút gọn lại chỉ còn vài dòng nhưng
 * trình biên dịch Kotlin (`kotlinc`) vẫn sinh ra **CÙNG MỘT CẤU TRÚC BYTECODE**
 * chuẩn JavaBean trên JVM:
 *   - `var id: String` -> trình biên dịch tự sinh `private String id` (nằm trên HEAP
 *     mỗi khi khởi tạo đối tượng) + `public final String getId()` + `public final void setId(String)`.
 *   - Tham số có giá trị mặc định (`id: String = ""`, `color: String = ""`, `salesPrice: Double = 0.0`)
 *     giúp ta có sẵn cả Default Constructor `Car()` lẫn Full Constructor `Car(id, color, salesPrice)`
 *     và cả Named Arguments `Car(id = "C1", salesPrice = 550.0)`!
 *   - `companion object` thay thế cho từ khóa `static` của Java: biến `model` được lưu
 *     tại vùng nhớ tĩnh của Class, mọi instance `Car` trên HEAP đều dùng chung.
 *
 * ============================================================================
 * SPRING KOTLIN CORNER:
 * ============================================================================
 * Trong Spring Boot, thuật ngữ "Bean" ban đầu xuất phát từ JavaBean (POJO có
 * getter/setter để IoC Container quản lý trong ApplicationContext).
 * Khi chuyển sang Kotlin:
 *   - Các Request/Response DTO, Properties (`@ConfigurationProperties`), hay Domain Model
 *     đều khai báo properties ngay trong Primary Constructor như class `Car` dưới đây.
 *   - Thư viện Jackson (parse JSON trong Spring Web) và Spring Data đều nhận diện hoàn hảo
 *     các hàm `getId()`, `setId(...)` do Kotlin tự động sinh ra ở tầng Bytecode!
 */
class Car(
    var id: String = "",
    var color: String = "",
    var salesPrice: Double = 0.0
) {
    companion object {
        // Tương đương `public static String model;` trong Car.java (line 26)
        // @JvmStatic giúp cả code Java lẫn Kotlin đều gọi được `Car.model` trực tiếp
        @JvmStatic
        var model: String = "Unknown"
    }

    override fun toString(): String {
        return "Car [id=$id, model=$model, color=$color, salesPrice=$salesPrice]"
    }
}
