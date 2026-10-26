package common

import bean.PageResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Ngoại lệ nghiệp vụ chuẩn của Nova Shop (Unchecked Exception kế thừa `RuntimeException`).
 */
class BusinessException(message: String) : RuntimeException(message)

/**
 * BỘ EXTENSION FUNCTIONS CHUẨN HÓA DỮ LIỆU, TIỀN TỆ & PHÂN TRANG GENERIC CỦA NOVA SHOP
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Trong Java Core, bạn phải tạo các lớp `StringUtils`, `MoneyUtils`, `PaginationUtils` với các hàm `public static`.
 *    - Trong Kotlin, các Top-level Extension Functions này cũng được biên dịch thành các phương thức
 *      `public static final` trên lớp `KotlinExtensionsKt`, nhưng cho phép gọi nối chuỗi trực tiếp
 *      trên đối tượng (`rawId.normalizeCode()`, `rawEmail.normalizeEmail()`, `amount.roundMoney()`,
 *      `items.paginate(page = 1, size = 5)`), giúp code nghiệp vụ đọc tự nhiên như tiếng Anh!
 */

private val MULTIPLE_SPACES_REGEX = Regex("\\s+")
private val VN_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val VN_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/**
 * Chuẩn hóa mã định danh (Khách hàng, Sản phẩm, Đơn hàng):
 * Bỏ khoảng trắng 2 đầu và chuyển thành chữ IN HOA (`" c003 "` -> `"C003"`).
 */
fun String.normalizeCode(): String = this.trim().uppercase()

/**
 * Chuẩn hóa tên người, tên sản phẩm, tên thành phố:
 * Bỏ khoảng trắng 2 đầu và gộp nhiều khoảng trắng liên tiếp thành 1 khoảng trắng duy nhất
 * (`"  Nguyễn   Minh An "` -> `"Nguyễn Minh An"`).
 */
fun String.normalizeSpaces(): String = this.trim().replace(MULTIPLE_SPACES_REGEX, " ")

/**
 * Chuẩn hóa Email (Extension trên kiểu Nullable `String?`):
 * - Nếu `null` hoặc chỉ chứa khoảng trắng (`"  "`) -> trả về `null`.
 * - Ngược lại -> bỏ khoảng trắng 2 đầu và chuyển thành chữ thường (`" AN@Example.com "` -> `"an@example.com"`).
 */
fun String?.normalizeEmail(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()

/**
 * Làm tròn tiền tệ theo đúng quy ước Nova Shop: `scale = 2`, `RoundingMode.HALF_UP`.
 */
fun BigDecimal.roundMoney(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)

/**
 * Định dạng ngày `dd/MM/yyyy` và thời điểm `dd/MM/yyyy HH:mm`.
 */
fun LocalDate.formatVn(): String = this.format(VN_DATE_FORMATTER)
fun LocalDateTime.formatVn(): String = this.format(VN_DATE_TIME_FORMATTER)

/**
 * Tiện ích phân trang Generic `<T>` dùng được cho mọi danh sách (`List<OrderSummaryView>`, `List<Customer>`, `List<Product>`...):
 * - Số trang `page` bắt đầu từ 1, kích thước trang `size` > 0.
 * - Nếu trang vượt quá dữ liệu hiện có -> trả về danh sách rỗng nhưng vẫn giữ nguyên `totalElements`.
 */
fun <T> List<T>.paginate(page: Int, size: Int): PageResult<T> {
    if (page < 1) {
        throw BusinessException("Số trang (page) phải >= 1 (nhận được: $page)")
    }
    if (size < 1) {
        throw BusinessException("Kích thước trang (size) phải >= 1 (nhận được: $size)")
    }

    val totalElements = this.size
    val totalPages = if (totalElements == 0) 0 else (totalElements + size - 1) / size
    val fromIndex = (page - 1) * size

    val pageItems = if (fromIndex >= totalElements) {
        emptyList()
    } else {
        this.subList(fromIndex, minOf(fromIndex + size, totalElements))
    }

    return PageResult(
        items = pageItems,
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages
    )
}
