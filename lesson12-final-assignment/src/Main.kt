import bean.BusinessReport
import bean.OrderOperationResult
import bean.ProductSalesRank
import common.BusinessException
import common.SampleData
import common.formatVn
import common.paginate
import enums.Category
import enums.MemberTier
import enums.OrderSortMode
import enums.OrderStatus
import service.NovaShopService
import java.math.BigDecimal
import java.text.Normalizer
import java.time.LocalDateTime

private val COMBINING_DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")

/**
 * Chuyển chuỗi có dấu tiếng Việt sang ASCII không dấu khi hiển thị ra Console Windows
 * để tránh lỗi hiển thị ký tự `?` (mojibake), trong khi vẫn giữ nguyên 100% dữ liệu tiếng Việt
 * có dấu trong `SampleData` và tầng nghiệp vụ `NovaShopService`.
 */
private fun String.toAscii(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(COMBINING_DIACRITICS_REGEX, "")
        .replace('đ', 'd')
        .replace('Đ', 'D')

/**
 * BÀI TẬP LỚN CUỐI KHÓA (CAPSTONE PROJECT): HỆ THỐNG QUẢN LÝ ĐƠN HÀNG & BÁO CÁO NOVA SHOP
 *
 * File `Main.kt` này đóng vai trò là tầng Presentation / Verification Runner:
 * - Tách biệt hoàn toàn việc dựng chuỗi hiển thị (`buildString { ... }` — Idiomatic Kotlin thay cho `new StringBuilder()`)
 *   khỏi tầng xử lý nghiệp vụ `NovaShopService`.
 * - Chạy tự động toàn bộ kịch bản đầu-cuối (End-to-End) và tự động kiểm chứng (Assert) tất cả các mốc
 *   số liệu chuẩn của đề bài `D:\Java_Journey\lesson15-final-assignment`.
 */
fun main() {
    println("--- ___ * START LESSON 12: NOVA SHOP CAPSTONE (IDIOMATIC KOTLIN EDITION) * ___ ---\n")

    // Khởi tạo NovaShopService từ bộ dữ liệu chuẩn SampleData
    val service = NovaShopService(
        rawCustomers = SampleData.customers(),
        rawProducts = SampleData.products(),
        rawOrders = SampleData.orders()
    )

    // =========================================================================
    // PHẦN 1: KIỂM CHỨNG NỀN DỮ LIỆU, CHUẨN HÓA & KHỬ TRÙNG LẶP
    // =========================================================================
    println("================================================================================")
    println("1. KIEM CHUNG CHUAN HOA & KHU TRUNG LAP KHACH HANG (12 BAN GHI GOC -> 10 KHACH)")
    println("================================================================================")
    val customers = service.getAllCustomers()
    println("  - Tong so khach hang sau chuan hoa & loai trung: ${customers.size} (Ky vong: 10)")
    customers.forEach { c ->
        println("    + [${c.id}] ${c.name.toAscii().padEnd(16)} | Email: ${(c.email ?: "<THIEU>").padEnd(20)} | TP: ${c.city.toAscii().padEnd(12)} | Hang: ${c.currentTier}")
    }

    val c002 = service.findCustomerById("c002")!!
    val c003 = service.findCustomerById("C003")!!
    val c004 = service.findCustomerById("C004")!!
    val c010 = service.findCustomerById("C010")!!

    check(customers.size == 10) { "Sai so luong khach hang: ${customers.size}" }
    check(c003.email == "chi@example.com" && c003.currentTier == MemberTier.SILVER) { "Sai du lieu khu trung C003: $c003" }
    check(c002.name == "Trần Bình" && c002.currentTier == MemberTier.SILVER) { "C002 bi ban cu ghi de: $c002" }
    check(c004.email == null && c010.email == null) { "C004 va C010 phai co email == null" }
    println("  => [PASS] Moc 1: Chuan hoa & khu trung 10 khach hang chinh xac 100%!\n")

    // =========================================================================
    // PHẦN 2: KIỂM CHỨNG TÍNH TIỀN ĐƠN HÀNG & TRA CỨU ĐA ĐIỀU KIỆN + 3 KIỂU SORT
    // =========================================================================
    println("================================================================================")
    println("2. KIEM CHUNG TINH TIEN DON LICH SU & TRA CUU VAN HANH (3 CHE DO SAP XEP)")
    println("================================================================================")
    val o001 = service.getOrderSummary("O001")
    val o003 = service.getOrderSummary("O003")
    val o008 = service.getOrderSummary("O008")

    println("  - Don O001 (GOLD -10%): Tien hang = ${o001.subtotalAmount}, Phai tra = ${o001.payableAmount}")
    println("  - Don O003 (STANDARD 0% luc mua, du C003 hien la SILVER): Phai tra = ${o003.payableAmount}")
    println("  - Don O008 (SILVER -5%, 2 dong P001 khac gia): Tien hang = ${o008.subtotalAmount}, Phai tra = ${o008.payableAmount}")

    check(o001.subtotalAmount == BigDecimal("560000.00") && o001.payableAmount == BigDecimal("504000.00"))
    check(o003.payableAmount == BigDecimal("300000.00"))
    check(o008.subtotalAmount == BigDecimal("520000.00") && o008.payableAmount == BigDecimal("494000.00"))

    // 2a. Tra cứu theo kiểu Sort 2: Tiền phải trả giảm dần -> Ngày tạo tăng dần -> Mã đơn tăng dần
    val sortByPayablePage = service.searchOrders(
        fromDate = SampleData.REPORT_FROM,
        toDate = SampleData.REPORT_TO,
        sortMode = OrderSortMode.PAYABLE_DESC_THEN_CREATED_ASC,
        page = 1,
        size = 25
    )
    println("\n  - Tra cuu Quy III (Sort: PAYABLE_DESC_THEN_CREATED_ASC) - Tong: ${sortByPayablePage.totalElements} don:")
    val o023Index = sortByPayablePage.items.indexOfFirst { it.orderId == "O023" }
    val o024Index = sortByPayablePage.items.indexOfFirst { it.orderId == "O024" }
    println("    + Kiem tra cap dong hang O023 & O024 (cung 237500.00, cung ngay tao 26/09/2026 10:00):")
    println("      Vi tri O023 = #$o023Index, Vi tri O024 = #$o024Index (O023 dung truoc O024 nho tieu chi ma don tang dan!)")
    check(o023Index in 0 until o024Index) { "Sai thu tu dong hang giua O023 va O024!" }

    // 2b. Tra cứu theo kiểu Sort 3: Email khách hàng tăng dần, Email thiếu (null) luôn đứng cuối
    val sortByEmailPage = service.searchOrders(
        fromDate = SampleData.REPORT_FROM,
        toDate = SampleData.REPORT_TO,
        sortMode = OrderSortMode.EMAIL_ASC_NULLS_LAST_THEN_CREATED_DESC,
        page = 1,
        size = 5
    )
    val lastPageByEmail = service.searchOrders(
        fromDate = SampleData.REPORT_FROM,
        toDate = SampleData.REPORT_TO,
        sortMode = OrderSortMode.EMAIL_ASC_NULLS_LAST_THEN_CREATED_DESC,
        page = 5,
        size = 5
    )
    println("    + Trang 1 (Sort Email ASC, nullsLast): ${sortByEmailPage.items.map { "${it.orderId}(${it.customerEmail})" }}")
    println("    + Trang cuoi (Chua cac don cua C004 khong co email): ${lastPageByEmail.items.map { "${it.orderId}(${it.customerEmail ?: "NULL"})" }}")
    check(lastPageByEmail.items.all { it.customerEmail == null }) { "Khach thieu email phai nam o cuoi danh sach!" }

    // 2c. Minh họa dùng Generic Pagination `List<T>.paginate()` cho kiểu dữ liệu thứ 2 (`List<Product>`)
    val productPage1 = service.getAllProducts().paginate(page = 1, size = 4)
    println("    + Phan trang Generic tren List<Product> (Trang 1/${productPage1.totalPages}): ${productPage1.items.map { it.id }}")
    println("  => [PASS] Moc 2: Tinh tien, tim kiem da dieu kien, 3 che do Sort & phan trang Generic chinh xac!\n")

    // =========================================================================
    // PHẦN 3: BÁO CÁO KINH DOANH QUÝ III/2026 (01/07/2026 - 30/09/2026, TOP K = 3)
    // =========================================================================
    println("================================================================================")
    println("3. BAO CAO KINH DOANH QUY III/2026 (K = 3, AS_OF = 01/10/2026 00:00)")
    println("================================================================================")
    val q3Report = service.generateBusinessReport(
        fromDate = SampleData.REPORT_FROM,
        toDate = SampleData.REPORT_TO,
        topK = 3,
        asOf = SampleData.AS_OF
    )
    print(formatBusinessReportText(q3Report))

    // Kiểm chứng tự động các con số trong Báo cáo Quý III/2026
    val ov = q3Report.overview
    check(ov.countByStatus[OrderStatus.DELIVERED] == 18)
    check(ov.countByStatus[OrderStatus.CANCELLED] == 2)
    check(ov.countByStatus[OrderStatus.CONFIRMED] == 1)
    check(ov.countByStatus[OrderStatus.NEW] == 1)

    val bookRanks = q3Report.productReport.topProductsByCategory.getValue(Category.BOOK)
    val p001Rank = bookRanks.first { it.productId == "P001" }
    check(p001Rank.totalQuantitySold == 7 && p001Rank.distinctOrderCount == 3) {
        "P001 trong Quy III ban o O001 (2), O008 (1+2=3), O015 (2) -> Tong SL=7, So don phan biet=3 (khong tinh O021 ngoai ky)!"
    }
    println("  => [PASS] Moc 3: Toan bo 5 goc nhin Bao cao Kinh doanh Quy III/2026 khop 100%!\n")

    // =========================================================================
    // PHẦN 4: TẠO ĐƠN MỚI, CHUYỂN TRẠNG THÁI & BẢO VỆ LỊCH SỬ KHI ĐỔI GIÁ/HẠNG
    // =========================================================================
    println("================================================================================")
    println("4. TAO DON MOI, CHUYEN TRANG THAI & CHUNG MINH BAT BIEN LICH SU")
    println("================================================================================")
    val newOrderCreated = service.createNewOrder(
        orderId = " o025 ",
        customerId = " c003 ", // C003 hiện có hạng SILVER (giảm 5%)
        createdAt = LocalDateTime.of(2026, 9, 29, 14, 30),
        requestedItems = listOf("p001" to 2, "P005" to 1) // 2 * 180000 + 1 * 50000 = 410000 -> giảm 5% còn 389500.00
    )
    println("  - Da tao don moi [${newOrderCreated.orderId}]: trang thai=${newOrderCreated.status}, " +
            "hang chup luc mua=${newOrderCreated.tierAtPurchase}, phai tra=${newOrderCreated.payableAmount}")
    check(newOrderCreated.payableAmount == BigDecimal("389500.00"))

    // Chuyển trạng thái hợp lệ: NEW -> CONFIRMED -> DELIVERED
    val confirmedO025 = service.transitionOrderStatus("O025", OrderStatus.CONFIRMED)
    val deliveredO025 = service.transitionOrderStatus(
        orderId = "O025",
        targetStatus = OrderStatus.DELIVERED,
        deliveredAt = LocalDateTime.of(2026, 9, 30, 16, 0)
    )
    println("  - Chuyen trang thai O025: NEW -> ${confirmedO025.status} -> ${deliveredO025.status} (giao luc ${deliveredO025.createdAt.formatVn()})")

    // Đổi giá danh mục P001 lên 999.000 và đổi hạng C003 lên GOLD -> Tiền đơn O025 và O003 đã tạo PHẢI GIỮ NGUYÊN!
    service.updateProductListPrice("P001", BigDecimal("999000"))
    service.updateCustomerTier("C003", MemberTier.GOLD)
    val o025AfterChange = service.getOrderSummary("O025")
    val o003AfterChange = service.getOrderSummary("O003")
    println("  - Sau khi tang gia P001 len 999.000 va nang C003 len GOLD:")
    println("    + Tien phai tra cua O025 van giu nguyen: ${o025AfterChange.payableAmount} (Ky vong: 389500.00)")
    println("    + Tien phai tra cua O003 van giu nguyen: ${o003AfterChange.payableAmount} (Ky vong: 300000.00)")
    check(o025AfterChange.payableAmount == BigDecimal("389500.00"))
    check(o003AfterChange.payableAmount == BigDecimal("300000.00"))

    // =========================================================================
    // PHẦN 5: KIỂM THỬ 12 ĐƠN KHÔNG HỢP LỆ (`SampleData.invalidOrders()`) & CA BIÊN
    // =========================================================================
    println("\n================================================================================")
    println("5. KIEM THU DOC LAP 12 TINH HUONG DON KHONG HOP LE (`SampleData.invalidOrders()`)")
    println("================================================================================")
    val freshServiceForValidation = NovaShopService(
        rawCustomers = SampleData.customers(),
        rawProducts = SampleData.products(),
        rawOrders = SampleData.orders()
    )
    val orderCountBeforeInvalidTests = freshServiceForValidation.getAllOrders().size

    SampleData.invalidOrders().forEachIndexed { idx, invalidOrder ->
        when (val result = freshServiceForValidation.validateCandidateOrderSafely(invalidOrder)) {
            is OrderOperationResult.Rejected ->
                println("  - Ca #${(idx + 1).toString().padStart(2, '0')} [Don ${result.orderId.toAscii().padEnd(7)}] -> TU CHOI DUNG: ${result.reason.toAscii()}")
            is OrderOperationResult.Success ->
                error("LOI: Don khong hop le ${invalidOrder.id} lai vuot qua validation!")
        }
    }
    check(freshServiceForValidation.getAllOrders().size == orderCountBeforeInvalidTests) {
        "Du lieu nguon bi thay doi sau khi kiem tra don loi!"
    }

    // Kiểm tra thêm các ca biên: Chuyển trạng thái sai (DELIVERED -> CANCELLED) & Đơn chờ đúng 72 giờ (không quá hạn)
    println("\n  - Kiem tra ca bien chuyen trang thai sai (DELIVERED -> CANCELLED tren O001):")
    try {
        freshServiceForValidation.transitionOrderStatus("O001", OrderStatus.CANCELLED)
    } catch (e: BusinessException) {
        println("    + Bat dung BusinessException: ${e.message?.toAscii()}")
        println("    + Trang thai O001 van an toan la: ${freshServiceForValidation.findOrderById("O001")?.status}")
    }

    // Kiểm tra đơn O018 (tạo lúc 2026-09-25T10:00) tại mốc đúng 72 giờ (2026-09-28T10:00) -> KHÔNG bị coi là quá hạn!
    val exact72hReport = freshServiceForValidation.generateBusinessReport(
        asOf = LocalDateTime.of(2026, 9, 28, 10, 0)
    )
    println("  - Kiem tra nguong cho dung 72 gio (tai 28/09/2026 10:00 doi voi O018 tao 25/09/2026 10:00):")
    println("    + So don qua han (> 72h): ${exact72hReport.overdueAlerts.size} (Ky vong: 0 vi dung 72h chua vuot qua 72h!)")
    check(exact72hReport.overdueAlerts.isEmpty())

    // =========================================================================
    // PHẦN 6: ĐỐI CHIẾU THUẬT TOÁN SẮP XẾP MẢNG THỦ CÔNG VS COMPARATOR PIPELINE
    // =========================================================================
    println("\n================================================================================")
    println("6. ON TAP MANG & THUAT TOAN SORT: INSERTION SORT TREN MANG VS COMPARATOR")
    println("================================================================================")
    verifyManualArraySortAgainstComparator(q3Report.productReport.topProductsByCategory.values.flatten())

    println("\n--- ___ * END LESSON 12: ALL NOVA SHOP MILESTONES VERIFIED SUCCESSFULLY * ___ ---")
}

/**
 * Dựng văn bản báo cáo bằng `buildString` (tương đương `StringBuilder` trong Java)
 * với định dạng Tiếng Việt không dấu (ASCII-only) phục vụ hiển thị Console.
 */
private fun formatBusinessReportText(report: BusinessReport): String = buildString {
    val ov = report.overview
    appendLine("  [3.1] TONG QUAN KY BAO CAO (${ov.fromDate.formatVn()} - ${ov.toDate.formatVn()}):")
    appendLine("    - So don theo trang thai : ${ov.countByStatus.entries.joinToString { "${it.key}=${it.value}" }}")
    appendLine("    - Tong doanh thu (Don giao): ${ov.totalRevenue} VND")
    appendLine("    - Gia tri TB moi don giao  : ${ov.averageDeliveredOrderValue} VND")
    appendLine("    - So khach mua thuc su     : ${ov.distinctPurchasingCustomers} khach")
    appendLine("    - Doanh thu theo thang     : ${ov.monthlyRevenue.entries.joinToString { "${it.key}: ${it.value}" }}")

    val cr = report.customerReport
    appendLine("\n  [3.2] BAO CAO KHACH HANG:")
    appendLine("    - Top khach hang chi tieu cao nhat:")
    cr.topSpendingCustomers.forEachIndexed { idx, rank ->
        appendLine("      #${idx + 1} [${rank.customerId}] ${rank.customerName.toAscii()} (${rank.city.toAscii()}) | " +
                "Chi tieu: ${rank.totalSpent} VND | So don giao: ${rank.deliveredOrderCount} | Mua gan nhat: ${rank.latestOrderCreatedAt.formatVn()}")
    }
    appendLine("    - Khach khong co don giao trong ky (${cr.inactiveCustomersInPeriod.size}): ${cr.inactiveCustomersInPeriod.map { "${it.id}-${it.name.toAscii()}" }}")
    appendLine("    - Cac thanh pho co khach mua hang : ${cr.purchasingCustomerCities.map { it.toAscii() }}")

    val pr = report.productReport
    appendLine("\n  [3.3] BAO CAO SAN PHAM THEO CATEGORY:")
    pr.topProductsByCategory.forEach { (category, ranks) ->
        appendLine("    * Category [$category]:")
        if (ranks.isEmpty()) {
            appendLine("      (Khong co san pham nao ban duoc)")
        } else {
            ranks.forEachIndexed { idx, p ->
                appendLine("      #${idx + 1} [${p.productId}] ${p.productName.toAscii().padEnd(20)} | " +
                        "Tong SL: ${p.totalQuantitySold} | Tien hang (Gross): ${p.grossMerchandiseAmount} | So don phan biet: ${p.distinctOrderCount}")
            }
        }
    }
    appendLine("    * San pham dang active nhung chua ban duoc trong ky (${pr.unsoldActiveProducts.size}): " +
            "${pr.unsoldActiveProducts.map { "${it.id}-${it.name.toAscii()} (${it.category})" }}")

    appendLine("\n  [3.4] PHAN TICH CHEO THANH PHO x HANG THANH VIEN LUC MUA:")
    report.cityTierBreakdown.forEach { stat ->
        appendLine("    - ${stat.city.toAscii().padEnd(14)} | Hang luc mua: ${stat.tierAtPurchase.name.padEnd(8)} | " +
                "So khach phan biet: ${stat.distinctCustomerCount} | Doanh thu: ${stat.totalRevenue} VND")
    }

    appendLine("\n  [3.5] CANH BAO VAN HANH - DON CHO QUA HAN (> 72 GIO TAI AS_OF):")
    if (report.overdueAlerts.isEmpty()) {
        appendLine("    - Khong co don cho qua han.")
    } else {
        report.overdueAlerts.forEach { alert ->
            appendLine("    - CANH BAO Don [${alert.orderId}] cua khach [${alert.customerId} - ${alert.customerName.toAscii()}] | " +
                    "Trang thai: ${alert.status} | Tao luc: ${alert.createdAt.formatVn()} | Da cho: ${alert.waitingHours} gio | Phai tra: ${alert.payableAmount} VND")
        }
    }
}

/**
 * Chuyển kết quả xếp hạng sản phẩm sang mảng `Array<ProductSalesRank>`, tự cài đặt thuật toán Insertion Sort
 * và đối chiếu thứ tự mã sản phẩm với kết quả của `Comparator` chuẩn.
 */
private fun verifyManualArraySortAgainstComparator(sourceRanks: List<ProductSalesRank>) {
    val shuffled = sourceRanks.reversed()
    val arrayForManualSort: Array<ProductSalesRank> = shuffled.toTypedArray()

    // So sánh 2 phần tử theo đúng tiêu chí: Tổng SL giảm dần -> Tiền hàng giảm dần -> Mã SP tăng dần
    fun shouldAppearBefore(a: ProductSalesRank, b: ProductSalesRank): Boolean {
        if (a.totalQuantitySold != b.totalQuantitySold) return a.totalQuantitySold > b.totalQuantitySold
        val grossCmp = b.grossMerchandiseAmount.compareTo(a.grossMerchandiseAmount)
        if (grossCmp != 0) return grossCmp < 0
        return a.productId < b.productId
    }

    // Cài đặt Insertion Sort trực tiếp trên mảng `Array<ProductSalesRank>`
    for (i in 1 until arrayForManualSort.size) {
        val key = arrayForManualSort[i]
        var j = i - 1
        while (j >= 0 && shouldAppearBefore(key, arrayForManualSort[j])) {
            arrayForManualSort[j + 1] = arrayForManualSort[j]
            j--
        }
        arrayForManualSort[j + 1] = key
    }

    val expectedByComparator = shuffled.sortedWith(
        compareByDescending<ProductSalesRank> { it.totalQuantitySold }
            .thenByDescending { it.grossMerchandiseAmount }
            .thenBy { it.productId }
    )

    val manualIds = arrayForManualSort.map { it.productId }
    val comparatorIds = expectedByComparator.map { it.productId }

    println("  - Thu tu ma SP sau khi chay Insertion Sort tren Array : $manualIds")
    println("  - Thu tu ma SP sau khi chay Kotlin Comparator Pipeline: $comparatorIds")
    check(manualIds == comparatorIds) { "Ket qua Insertion Sort khong khop voi Comparator!" }
    println("  => [PASS] Moc 6: Insertion Sort tren mang khop tuyet doi voi Comparator Pipeline!")
}
