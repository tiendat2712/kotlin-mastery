# Kotlin Core Final Assignment — Nova Shop (Idiomatic Kotlin Edition)

Dự án tổng hợp (Capstone Project) này là phiên bản **Idiomatic Kotlin** đối chiếu trực tiếp 1-1 với bài tập lớn **`D:\Java_Journey\lesson15-final-assignment` (Nova Shop)** trong khóa Java Core.

Mục tiêu của module này là giúp bạn thấy rõ cách **cùng một bài toán nghiệp vụ phức tạp** (chuẩn hóa dữ liệu, khử trùng lặp, tìm kiếm đa điều kiện, phân trang Generic, quản lý vòng đời đơn hàng và lập 5 báo cáo kinh doanh) được nâng cấp từ Java Core sang **Kotlin Core chuẩn bị cho Spring Boot**.

---

## Cấu trúc thư mục

```text
lesson12-final-assignment/
├── README.md
└── src/
    ├── Main.kt                      # Kịch bản chạy tự động & kiểm chứng toàn bộ các mốc nghiệp vụ
    ├── enums/
    │   └── DomainEnums.kt           # Category, MemberTier (kèm tỷ lệ giảm), OrderStatus (kèm luật chuyển trạng thái), OrderSortMode
    ├── bean/
    │   └── DomainModels.kt          # Customer, Product, OrderLine, Order & toàn bộ Data Classes kết quả báo cáo + Sealed Class
    ├── common/
    │   ├── KotlinExtensions.kt      # Extension functions chuẩn hóa chuỗi, làm tròn tiền BigDecimal & phân trang Generic
    │   └── SampleData.kt            # Bộ dữ liệu gốc đối chiếu 100% với Java Core (12 khách, 12 SP, 24 đơn lịch sử, 12 đơn lỗi)
    └── service/
        └── NovaShopService.kt       # Trái tim xử lý nghiệp vụ bằng Kotlin Collection Pipelines & Preconditions
```

---

## Bảng đối chiếu nâng cấp từ Java Core sang Idiomatic Kotlin

| Khía cạnh kỹ thuật | Cách làm trong `D:\Java_Journey\lesson15-final-assignment` | Cách nâng cấp trong `D:\Kotlin_Journey\lesson12-final-assignment` |
| :--- | :--- | :--- |
| **Domain Models & DTOs** | 4 file `.java` riêng lẻ với hàng trăm dòng getter/setter/constructor/toString. | Gói gọn trong [`DomainModels.kt`](src/bean/DomainModels.kt) bằng `data class` bất biến (`val`) và `.copy()` an toàn. |
| **Xử lý `null` (Email, Ngày giao)** | Kiểm tra `if (email != null)` thủ công hoặc dễ dính `NullPointerException`. | Kiểu `String?`, `LocalDateTime?` kết hợp Safe-call `?.`, `takeIf`, Elvis `?:` và `nullsLast()`. |
| **Chuẩn hóa mã, tên, email & tiền** | Tạo lớp tiện ích tĩnh `StringUtils.normalize(...)`, `MoneyUtils.round(...)`. | **Extension Functions** trực quan trong [`KotlinExtensions.kt`](src/common/KotlinExtensions.kt): `id.normalizeCode()`, `name.normalizeSpaces()`, `email.normalizeEmail()`, `amount.roundMoney()`. |
| **Tính toán `BigDecimal`** | `price.multiply(BigDecimal.valueOf(qty)).subtract(discount)` dài dòng. | **Operator Overloading** của Kotlin: `unitPrice * quantity.toBigDecimal()`, `subtotal * (BigDecimal.ONE - discountRate)`, cộng dồn bằng `.sumOf { it.lineAmount }`. |
| **Comparator nhiều tiêu chí** | `Comparator.comparing(...).thenComparing(...)` | `compareByDescending<OrderSummaryView> { it.payableAmount }.thenBy { it.createdAt }.thenBy { it.orderId }` hoặc `compareBy(nullsLast(String.CASE_INSENSITIVE_ORDER)) { it.customerEmail }`. |
| **Collection Pipelines (Thay Stream API)** | `.stream().filter(...).collect(Collectors.groupingBy(...))` | Trực tiếp trên Collection: `.groupBy`, `.associateBy`, `.flatMap`, `.sumOf`, `.maxByOrNull`, `.sortedWith`. |
| **Validation & Kết quả xử lý** | `if (...) throw new BusinessException(...)` | Kết hợp `require(...)`, `BusinessException` và `sealed class OrderOperationResult` để biểu diễn kết quả thành công/thất bại an toàn kiểu. |

---

## Các mốc tự kiểm chứng (Milestones) được xác nhận tự động trong `Main.kt`

1. **Chuẩn hóa & loại trùng khách hàng**: Từ 12 bản ghi gốc còn đúng **10 khách hàng**.
   - `C003` giữ bản cập nhật mới hơn (`20/06/2026 10:00`), email `chi@example.com`, hạng `SILVER`.
   - `C002` giữ bản mới hơn (`01/06/2026 09:00`), không bị bản cũ (`01/05/2026 09:00`) xuất hiện sau ghi đè.
   - `C004` và `C010` có `email == null`.
2. **Thống kê Quý III/2026 (`01/07/2026` – `30/09/2026`)**: Có đúng **22 đơn** trong kỳ (`18 DELIVERED`, `2 CANCELLED`, `1 CONFIRMED`, `1 NEW`). `O021` và `O022` nằm ngoài kỳ.
3. **Tính tiền chính xác (`BigDecimal`, làm tròn `HALF_UP` scale 2 ở cấp đơn)**:
   - `O001` (`GOLD` -10%): Tiền hàng = `560000.00`, Phải trả = **`504000.00`**.
   - `O003` (`STANDARD` 0% lúc mua): Phải trả = **`300000.00`** (không bị ảnh hưởng bởi hạng `SILVER` hiện tại của `C003`).
   - `O008` (`SILVER` -5%, 2 dòng `P001` khác giá mua): Tổng SL = `3`, Tiền hàng = `520000.00`, Phải trả = **`494000.00`**, đóng góp **1 đơn phân biệt** cho `P001`.
4. **Sắp xếp đồng hạng**: `O023` và `O024` cùng phải trả `237500.00` và cùng `createdAt` -> tiêu chí phụ mã đơn tăng dần giữ `O023` đứng trước `O024`.
5. **Kiểm tra 12 đơn không hợp lệ (`invalidOrders()`)**: Bắt chính xác từng lỗi độc lập mà không làm bẩn dữ liệu gốc.
