import bean.BusinessReport
import bean.OrderOperationResult
import bean.OrderSummaryView
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
import java.time.LocalDate
import java.time.LocalDateTime

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
    println("1. KIỂM CHỨNG CHUẨN HÓA & KHỬ TRÙNG LẶP KHÁCH HÀNG (12 BẢN GHI GỐC -> 10 KHÁCH)")
    println("================================================================================")
    val customers = service.getAllCustomers()
    println("  - Tổng số khách hàng sau chuẩn hóa & loại trùng: ${customers.size} (Kỳ vọng: 10)")
    customers.forEach { c ->
        println("    + [${c.id}] ${c.name.padEnd(16)} | Email: ${(c.email ?: "<THIẾU>").padEnd(20)} | TP: ${c.city.padEnd(12)} | Hạng: ${c.currentTier}")
    }

    val c002 = service.findCustomerById("c002")!!
    val c003 = service.findCustomerById("C003")!!
    val c004 = service.findCustomerById("C004")!!
    val c010 = service.findCustomerById("C010")!!

    check(customers.size == 10) { "Sai số lượng khách hàng: ${customers.size}" }
    check(c003.email == "chi@example.com" && c003.currentTier == MemberTier.SILVER) { "Sai dữ liệu khử trùng C003: $c003" }
    check(c002.name == "Trần Bình" && c002.currentTier == MemberTier.SILVER) { "C002 bị bản cũ ghi đè: $c002" }
    check(c004.email == null && c010.email == null) { "C004 và C010 phải có email == null" }
    println("  => [PASS] Mốc 1: Chuẩn hóa & khử trùng 10 khách hàng chính xác 100%!\n")

    // =========================================================================
    // PHẦN 2: KIỂM CHỨNG TÍNH TIỀN ĐƠN HÀNG & TRA CỨU ĐA ĐIỀU KIỆN + 3 KIỂU SORT
    // =========================================================================
    println("================================================================================")
    println("2. KIỂM CHỨNG TÍNH TIỀN ĐƠN LỊCH SỬ & TRA CỨU VẬN HÀNH (3 CHẾ ĐỘ SẮP XẾP)")
    println("================================================================================")
    val o001 = service.getOrderSummary("O001")
    val o003 = service.getOrderSummary("O003")
    val o008 = service.getOrderSummary("O008")

    println("  - Đơn O001 (GOLD -10%): Tiền hàng = ${o001.subtotalAmount}, Phải trả = ${o001.payableAmount}")
    println("  - Đơn O003 (STANDARD 0% lúc mua, dù C003 hiện là SILVER): Phải trả = ${o003.payableAmount}")
    println("  - Đơn O008 (SILVER -5%, 2 dòng P001 khác giá): Tiền hàng = ${o008.subtotalAmount}, Phải trả = ${o008.payableAmount}")

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
    println("\n  - Tra cứu Quý III (Sort: PAYABLE_DESC_THEN_CREATED_ASC) — Tổng: ${sortByPayablePage.totalElements} đơn:")
    val o023Index = sortByPayablePage.items.indexOfFirst { it.orderId == "O023" }
    val o024Index = sortByPayablePage.items.indexOfFirst { it.orderId == "O024" }
    println("    + Kiểm tra cặp đồng hạng O023 & O024 (cùng 237500.00, cùng ngày tạo 26/09/2026 10:00):")
    println("      Vị trí O023 = #$o023Index, Vị trí O024 = #$o024Index (O023 đứng trước O024 nhờ tiêu chí mã đơn tăng dần!)")
    check(o023Index in 0 until o024Index) { "Sai thứ tự đồng hạng giữa O023 và O024!" }

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
    println("    + Trang cuối (Chứa các đơn của C004 không có email): ${lastPageByEmail.items.map { "${it.orderId}(${it.customerEmail ?: "NULL"})" }}")
    check(lastPageByEmail.items.all { it.customerEmail == null }) { "Khách thiếu email phải nằm ở cuối danh sách!" }

    // 2c. Minh họa dùng Generic Pagination `List<T>.paginate()` cho kiểu dữ liệu thứ 2 (`List<Product>`)
    val productPage1 = service.getAllProducts().paginate(page = 1, size = 4)
    println("    + Phân trang Generic trên List<Product> (Trang 1/${productPage1.totalPages}): ${productPage1.items.map { it.id }}")
    println("  => [PASS] Mốc 2: Tính tiền, tìm kiếm đa điều kiện, 3 chế độ Sort & phân trang Generic chính xác!\n")

    // =========================================================================
    // PHẦN 3: BÁO CÁO KINH DOANH QUÝ III/2026 (01/07/2026 - 30/09/2026, TOP K = 3)
    // =========================================================================
    println("================================================================================")
    println("3. BÁO CÁO KINH DOANH QUÝ III/2026 (K = 3, AS_OF = 01/10/2026 00:00)")
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
        "P001 trong Quý III bán ở O001 (2), O008 (1+2=3), O015 (2) -> Tổng SL=7, Số đơn phân biệt=3 (không tính O021 ngoài kỳ)!"
    }
    println("  => [PASS] Mốc 3: Toàn bộ 5 góc nhìn Báo cáo Kinh doanh Quý III/2026 khớp 100%!\n")

    // =========================================================================
    // PHẦN 4: TẠO ĐƠN MỚI, CHUYỂN TRẠNG THÁI & BẢO VỆ LỊCH SỬ KHI ĐỔI GIÁ/HẠNG
    // =========================================================================
    println("================================================================================")
    println("4. TẠO ĐƠN MỚI, CHUYỂN TRẠNG THÁI & CHỨNG MINH BẤT BIẾN LỊCH SỬ")
    println("================================================================================")
    val newOrderCreated = service.createNewOrder(
        orderId = " o025 ",
        customerId = " c003 ", // C003 hiện có hạng SILVER (giảm 5%)
        createdAt = LocalDateTime.of(2026, 9, 29, 14, 30),
        requestedItems = listOf("p001" to 2, "P005" to 1) // 2 * 180000 + 1 * 50000 = 410000 -> giảm 5% còn 389500.00
    )
    println("  - Đã tạo đơn mới [${newOrderCreated.orderId}]: trạng thái=${newOrderCreated.status}, " +
            "hạng chụp lúc mua=${newOrderCreated.tierAtPurchase}, phải trả=${newOrderCreated.payableAmount}")
    check(newOrderCreated.payableAmount == BigDecimal("389500.00"))

    // Chuyển trạng thái hợp lệ: NEW -> CONFIRMED -> DELIVERED
    val confirmedO025 = service.transitionOrderStatus("O025", OrderStatus.CONFIRMED)
    val deliveredO025 = service.transitionOrderStatus(
        orderId = "O025",
        targetStatus = OrderStatus.DELIVERED,
        deliveredAt = LocalDateTime.of(2026, 9, 30, 16, 0)
    )
    println("  - Chuyển trạng thái O025: NEW -> ${confirmedO025.status} -> ${deliveredO025.status} (giao lúc ${deliveredO025.createdAt.formatVn()})")

    // Đổi giá danh mục P001 lên 999.000 và đổi hạng C003 lên GOLD -> Tiền đơn O025 và O003 đã tạo PHẢI GIỮ NGUYÊN!
    service.updateProductListPrice("P001", BigDecimal("999000"))
    service.updateCustomerTier("C003", MemberTier.GOLD)
    val o025AfterChange = service.getOrderSummary("O025")
    val o003AfterChange = service.getOrderSummary("O003")
    println("  - Sau khi tăng giá P001 lên 999.000 và nâng C003 lên GOLD:")
    println("    + Tiền phải trả của O025 vẫn giữ nguyên: ${o025AfterChange.payableAmount} (Kỳ vọng: 389500.00)")
    println("    + Tiền phải trả của O003 vẫn giữ nguyên: ${o003AfterChange.payableAmount} (Kỳ vọng: 300000.00)")
    check(o025AfterChange.payableAmount == BigDecimal("389500.00"))
    check(o003AfterChange.payableAmount == BigDecimal("300000.00"))

    // =========================================================================
    // PHẦN 5: KIỂM THỬ 12 ĐƠN KHÔNG HỢP LỆ (`SampleData.invalidOrders()`) & CA BIÊN
    // =========================================================================
    println("\n================================================================================")
    println("5. KIỂM THỬ ĐỘC LẬP 12 TÌNH HUỐNG ĐƠN KHÔNG HỢP LỆ (`SampleData.invalidOrders()`)")
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
                println("  - Ca #${(idx + 1).toString().padStart(2, '0')} [Đơn ${result.orderId.padEnd(7)}] -> TỪ CHỐI ĐÚNG: ${result.reason}")
            is OrderOperationResult.Success ->
                error("LỖI: Đơn không hợp lệ ${invalidOrder.id} lại vượt qua validation!")
        }
    }
    check(freshServiceForValidation.getAllOrders().size == orderCountBeforeInvalidTests) {
        "Dữ liệu nguồn bị thay đổi sau khi kiểm tra đơn lỗi!"
    }

    // Kiểm tra thêm các ca biên: Chuyển trạng thái sai (DELIVERED -> CANCELLED) & Đơn chờ đúng 72 giờ (không quá hạn)
    println("\n  - Kiểm tra ca biên chuyển trạng thái sai (DELIVERED -> CANCELLED trên O001):")
    try {
        freshServiceForValidation.transitionOrderStatus("O001", OrderStatus.CANCELLED)
    } catch (e: BusinessException) {
        println("    + Bắt đúng BusinessException: ${e.message}")
        println("    + Trạng thái O001 vẫn an toàn là: ${freshServiceForValidation.findOrderById("O001")?.status}")
    }

    // Kiểm tra đơn O018 (tạo lúc 2026-09-25T10:00) tại mốc đúng 72 giờ (2026-09-28T10:00) -> KHÔNG bị coi là quá hạn!
    val exact72hReport = freshServiceForValidation.generateBusinessReport(
        asOf = LocalDateTime.of(2026, 9, 28, 10, 0)
    )
    println("  - Kiểm tra ngưỡng chờ đúng 72 giờ (tại 28/09/2026 10:00 đối với O018 tạo 25/09/2026 10:00):")
    println("    + Số đơn quá hạn (> 72h): ${exact72hReport.overdueAlerts.size} (Kỳ vọng: 0 vì đúng 72h chưa vượt quá 72h!)")
    check(exact72hReport.overdueAlerts.isEmpty())

    // =========================================================================
    // PHẦN 6: ĐỐI CHIẾU THUẬT TOÁN SẮP XẾP MẢNG THỦ CÔNG VS COMPARATOR PIPELINE
    // =========================================================================
    println("\n================================================================================")
    println("6. ÔN TẬP MẢNG & THUẬT TOÁN SORT: INSERTION SORT TRÊN MẢNG VS COMPARATOR")
    println("================================================================================")
    verifyManualArraySortAgainstComparator(q3Report.productReport.topProductsByCategory.values.flatten())

    println("\n--- ___ * END LESSON 12: ALL NOVA SHOP MILESTONES VERIFIED SUCCESSFULLY * ___ ---")
}

/**
 * Dựng văn bản báo cáo bằng `buildString` (tương đương `StringBuilder` trong Java).
 */
private fun formatBusinessReportText(report: BusinessReport): String = buildString {
    val ov = report.overview
    appendLine("  [3.1] TỔNG QUAN KỲ BÁO CÁO (${ov.fromDate.formatVn()} - ${ov.toDate.formatVn()}):")
    appendLine("    - Số đơn theo trạng thái : ${ov.countByStatus.entries.joinToString { "${it.key}=${it.value}" }}")
    appendLine("    - Tổng doanh thu (Đơn giao): ${ov.totalRevenue} VNĐ")
    appendLine("    - Giá trị TB mỗi đơn giao  : ${ov.averageDeliveredOrderValue} VNĐ")
    appendLine("    - Số khách mua thực sự     : ${ov.distinctPurchasingCustomers} khách")
    appendLine("    - Doanh thu theo tháng     : ${ov.monthlyRevenue.entries.joinToString { "${it.key}: ${it.value}" }}")

    val cr = report.customerReport
    appendLine("\n  [3.2] BÁO CÁO KHÁCH HÀNG:")
    appendLine("    - Top khách hàng chi tiêu cao nhất:")
    cr.topSpendingCustomers.forEachIndexed { idx, rank ->
        appendLine("      #${idx + 1} [${rank.customerId}] ${rank.customerName} (${rank.city}) | " +
                "Chi tiêu: ${rank.totalSpent} VNĐ | Số đơn giao: ${rank.deliveredOrderCount} | Mua gần nhất: ${rank.latestOrderCreatedAt.formatVn()}")
    }
    appendLine("    - Khách không có đơn giao trong kỳ (${cr.inactiveCustomersInPeriod.size}): ${cr.inactiveCustomersInPeriod.map { "${it.id}-${it.name}" }}")
    appendLine("    - Các thành phố có khách mua hàng : ${cr.purchasingCustomerCities}")

    val pr = report.productReport
    appendLine("\n  [3.3] BÁO CÁO SẢN PHẨM THEO CATEGORY:")
    pr.topProductsByCategory.forEach { (category, ranks) ->
        appendLine("    * Category [$category]:")
        if (ranks.isEmpty()) {
            appendLine("      (Không có sản phẩm nào bán được)")
        } else {
            ranks.forEachIndexed { idx, p ->
                appendLine("      #${idx + 1} [${p.productId}] ${p.productName.padEnd(20)} | " +
                        "Tổng SL: ${p.totalQuantitySold} | Tiền hàng (Gross): ${p.grossMerchandiseAmount} | Số đơn phân biệt: ${p.distinctOrderCount}")
            }
        }
    }
    appendLine("    * Sản phẩm đang active nhưng chưa bán được trong kỳ (${pr.unsoldActiveProducts.size}): " +
            "${pr.unsoldActiveProducts.map { "${it.id}-${it.name} (${it.category})" }}")

    appendLine("\n  [3.4] PHÂN TÍCH CHÉO THÀNH PHỐ x HẠNG THÀNH VIÊN LÚC MUA:")
    report.cityTierBreakdown.forEach { stat ->
        appendLine("    - ${stat.city.padEnd(14)} | Hạng lúc mua: ${stat.tierAtPurchase.name.padEnd(8)} | " +
                "Số khách phân biệt: ${stat.distinctCustomerCount} | Doanh thu: ${stat.totalRevenue} VNĐ")
    }

    appendLine("\n  [3.5] CẢNH BÁO VẬN HÀNH — ĐƠN CHỜ QUÁ HẠN (> 72 GIỜ TẠI AS_OF):")
    if (report.overdueAlerts.isEmpty()) {
        appendLine("    - Không có đơn chờ quá hạn.")
    } else {
        report.overdueAlerts.forEach { alert ->
            appendLine("    - CẢNH BÁO Đơn [${alert.orderId}] của khách [${alert.customerId} - ${alert.customerName}] | " +
                    "Trạng thái: ${alert.status} | Tạo lúc: ${alert.createdAt.formatVn()} | Đã chờ: ${alert.waitingHours} giờ | Phải trả: ${alert.payableAmount} VNĐ")
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

    println("  - Thứ tự mã SP sau khi chạy Insertion Sort trên Array : $manualIds")
    println("  - Thứ tự mã SP sau khi chạy Kotlin Comparator Pipeline: $comparatorIds")
    check(manualIds == comparatorIds) { "Kết quả Insertion Sort không khớp với Comparator!" }
    println("  => [PASS] Mốc 6: Insertion Sort trên mảng khớp tuyệt đối với Comparator Pipeline!")
}
