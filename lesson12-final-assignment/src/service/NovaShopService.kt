package service

import bean.BusinessReport
import bean.CityTierStats
import bean.Customer
import bean.CustomerReport
import bean.CustomerSpendRank
import bean.Order
import bean.OrderLine
import bean.OrderOperationResult
import bean.OrderSummaryView
import bean.OverdueOrderAlert
import bean.OverviewReport
import bean.PageResult
import bean.Product
import bean.ProductReport
import bean.ProductSalesRank
import common.BusinessException
import common.normalizeCode
import common.normalizeEmail
import common.normalizeSpaces
import common.paginate
import common.roundMoney
import enums.Category
import enums.MemberTier
import enums.OrderSortMode
import enums.OrderStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * CHÍNH SÁCH GIẢM GIÁ ĐA HÌNH (POLYMORPHIC DISCOUNT POLICY)
 *
 * Minh họa vận dụng Đa hình (Polymorphism) theo đúng yêu cầu đề bài Nova Shop:
 * - STANDARD: Giảm 0%
 * - SILVER: Giảm 5%
 * - GOLD: Giảm 10%
 * Tiền phải trả = `subtotal * (1 - discountRate)`, làm tròn 1 lần duy nhất ở cấp đơn (`scale = 2, HALF_UP`).
 */
fun interface DiscountPolicy {
    fun calculatePayable(subtotal: BigDecimal): BigDecimal

    companion object {
        private val standardPolicy = DiscountPolicy { subtotal -> subtotal.roundMoney() }
        private val silverPolicy = DiscountPolicy { subtotal ->
            (subtotal * (BigDecimal.ONE - MemberTier.SILVER.discountRate)).roundMoney()
        }
        private val goldPolicy = DiscountPolicy { subtotal ->
            (subtotal * (BigDecimal.ONE - MemberTier.GOLD.discountRate)).roundMoney()
        }

        fun forTier(tier: MemberTier): DiscountPolicy = when (tier) {
            MemberTier.STANDARD -> standardPolicy
            MemberTier.SILVER -> silverPolicy
            MemberTier.GOLD -> goldPolicy
        }
    }
}

/**
 * DỊCH VỤ TRUNG TÂM XỬ LÝ NGHIỆP VỤ & BÁO CÁO NOVA SHOP (IDIOMATIC KOTLIN EDITION)
 *
 * So sánh với `D:\Java_Journey\lesson15-final-assignment`:
 * - Toàn bộ quá trình chuẩn hóa, khử trùng lặp, tìm kiếm đa điều kiện và 5 báo cáo kinh doanh
 *   được viết bằng **Kotlin Collection Pipelines** (`groupBy`, `associateBy`, `flatMap`, `sumOf`,
 *   `sortedWith(compareBy...)`) cực kỳ ngắn gọn, bất biến (Immutable) và an toàn Null 100%.
 */
class NovaShopService(
    rawCustomers: List<Customer>,
    rawProducts: List<Product>,
    rawOrders: List<Order>
) {
    // Lưu trữ nội bộ theo mã đã chuẩn hóa, giữ thứ tự chèn bằng LinkedHashMap
    private val customersById: MutableMap<String, Customer> = linkedMapOf()
    private val productsById: MutableMap<String, Product> = linkedMapOf()
    private val ordersById: MutableMap<String, Order> = linkedMapOf()

    init {
        loadAndNormalizeCustomers(rawCustomers)
        loadAndNormalizeProducts(rawProducts)
        loadAndValidateHistoricalOrders(rawOrders)
    }

    // =========================================================================
    // PHẦN 1: NẠP, CHUẨN HÓA & KHỬ TRÙNG LẶP DỮ LIỆU
    // =========================================================================

    private fun loadAndNormalizeCustomers(rawCustomers: List<Customer>) {
        // Nhóm theo mã đã chuẩn hóa; nếu trùng mã thì giữ bản có `updatedAt` mới nhất
        // (nếu `updatedAt` bằng nhau thì giữ bản xuất hiện trước nhờ ` candidate.updatedAt.isAfter(kept.updatedAt) `)
        val deduplicated = rawCustomers
            .map { raw ->
                raw.copy(
                    id = raw.id.normalizeCode(),
                    name = raw.name.normalizeSpaces(),
                    email = raw.email.normalizeEmail(),
                    city = raw.city.normalizeSpaces()
                )
            }
            .groupBy { it.id }
            .mapValues { (_, duplicates) ->
                duplicates.reduce { kept, candidate ->
                    if (candidate.updatedAt.isAfter(kept.updatedAt)) candidate else kept
                }
            }

        customersById.clear()
        customersById.putAll(deduplicated)
    }

    private fun loadAndNormalizeProducts(rawProducts: List<Product>) {
        productsById.clear()
        rawProducts.forEach { raw ->
            val normalized = raw.copy(
                id = raw.id.normalizeCode(),
                name = raw.name.normalizeSpaces()
            )
            productsById[normalized.id] = normalized
        }
    }

    private fun loadAndValidateHistoricalOrders(rawOrders: List<Order>) {
        ordersById.clear()
        rawOrders.forEach { raw ->
            val normalized = validateAndNormalizeOrder(raw, isNewOrder = false)
            ordersById[normalized.id] = normalized
        }
    }

    // Các hàm đọc danh sách bất biến (Defensive Copy) phục vụ kiểm chứng
    fun getAllCustomers(): List<Customer> = customersById.values.toList()
    fun getAllProducts(): List<Product> = productsById.values.toList()
    fun getAllOrders(): List<Order> = ordersById.values.toList()

    fun findCustomerById(customerId: String): Customer? = customersById[customerId.normalizeCode()]
    fun findProductById(productId: String): Product? = productsById[productId.normalizeCode()]
    fun findOrderById(orderId: String): Order? = ordersById[orderId.normalizeCode()]

    /**
     * Cho phép cập nhật hạng thành viên hoặc giá danh mục sản phẩm để chứng minh:
     * "Khi danh mục hoặc hạng khách hàng thay đổi sau đó, tiền đơn đã tạo phải giữ nguyên."
     */
    fun updateCustomerTier(customerId: String, newTier: MemberTier): Customer {
        val code = customerId.normalizeCode()
        val existing = customersById[code]
            ?: throw BusinessException("Khong tim thay khach hang: $code")
        val updated = existing.copy(currentTier = newTier, updatedAt = LocalDateTime.now())
        customersById[code] = updated
        return updated
    }

    fun updateProductListPrice(productId: String, newPrice: BigDecimal): Product {
        require(newPrice > BigDecimal.ZERO) { "Gia danh muc moi phai duong: $newPrice" }
        val code = productId.normalizeCode()
        val existing = productsById[code]
            ?: throw BusinessException("Khong tim thay san pham: $code")
        val updated = existing.copy(listPrice = newPrice)
        productsById[code] = updated
        return updated
    }

    // =========================================================================
    // PHẦN 2: TÍNH TIỀN ĐƠN HÀNG & TRA CỨU VẬN HÀNH ĐA ĐIỀU KIỆN
    // =========================================================================

    /**
     * Tính toán tóm tắt hóa đơn (`OrderSummaryView`) cho một đơn hàng.
     * - Dùng `order.tierAtPurchase`, TUYỆT ĐỐI KHÔNG dùng `customer.currentTier`.
     * - Dùng `line.unitPrice` đã lưu trong đơn, TUYỆT ĐỐI KHÔNG dùng `product.listPrice`.
     */
    fun toOrderSummaryView(order: Order): OrderSummaryView {
        val customer = customersById[order.customerId]
            ?: throw BusinessException("Khong tim thay khach hang [${order.customerId}] cua don [${order.id}]")

        val rawSubtotal = order.lines.sumOf { it.lineAmount }
        val subtotal = rawSubtotal.roundMoney()
        val payable = DiscountPolicy.forTier(order.tierAtPurchase).calculatePayable(rawSubtotal)
        val discount = (subtotal - payable).roundMoney()

        return OrderSummaryView(
            orderId = order.id,
            customerId = customer.id,
            customerName = customer.name,
            customerEmail = customer.email,
            createdAt = order.createdAt,
            status = order.status,
            tierAtPurchase = order.tierAtPurchase,
            subtotalAmount = subtotal,
            discountAmount = discount,
            payableAmount = payable
        )
    }

    fun getOrderSummary(orderId: String): OrderSummaryView {
        val order = findOrderById(orderId)
            ?: throw BusinessException("Khong tim thay don hang voi ma: ${orderId.normalizeCode()}")
        return toOrderSummaryView(order)
    }

    /**
     * Tra cứu đơn hàng bằng tổ hợp các điều kiện tùy chọn + 3 chế độ sắp xếp + phân trang Generic.
     */
    fun searchOrders(
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        status: OrderStatus? = null,
        customerKeyword: String? = null,
        minPayable: BigDecimal? = null,
        maxPayable: BigDecimal? = null,
        sortMode: OrderSortMode = OrderSortMode.NEWEST_FIRST,
        page: Int = 1,
        size: Int = 10
    ): PageResult<OrderSummaryView> {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw BusinessException("Khoang ngay tim kiem bi dao nguoc: tu ngay ($fromDate) > den ngay ($toDate)")
        }
        if ((minPayable != null && minPayable < BigDecimal.ZERO) || (maxPayable != null && maxPayable < BigDecimal.ZERO)) {
            throw BusinessException("Khoang tien phai tra khong duoc am (min=$minPayable, max=$maxPayable)")
        }
        if (minPayable != null && maxPayable != null && minPayable > maxPayable) {
            throw BusinessException("Khoang tien tim kiem bi dao nguoc: min ($minPayable) > max ($maxPayable)")
        }

        val normalizedKeyword = customerKeyword?.normalizeSpaces()?.takeIf { it.isNotEmpty() }

        val comparator: Comparator<OrderSummaryView> = when (sortMode) {
            OrderSortMode.NEWEST_FIRST ->
                compareByDescending<OrderSummaryView> { it.createdAt }
                    .thenBy { it.orderId }

            OrderSortMode.PAYABLE_DESC_THEN_CREATED_ASC ->
                compareByDescending<OrderSummaryView> { it.payableAmount }
                    .thenBy { it.createdAt }
                    .thenBy { it.orderId }

            OrderSortMode.EMAIL_ASC_NULLS_LAST_THEN_CREATED_DESC ->
                compareBy<OrderSummaryView, String?>(nullsLast(String.CASE_INSENSITIVE_ORDER)) { it.customerEmail }
                    .thenByDescending { it.createdAt }
                    .thenBy { it.orderId }
        }

        val matchedSummaries = ordersById.values
            .asSequence()
            .map { toOrderSummaryView(it) }
            .filter { view ->
                val createdDate = view.createdAt.toLocalDate()
                (fromDate == null || !createdDate.isBefore(fromDate)) &&
                    (toDate == null || !createdDate.isAfter(toDate)) &&
                    (status == null || view.status == status) &&
                    (normalizedKeyword == null || view.customerName.contains(normalizedKeyword, ignoreCase = true)) &&
                    (minPayable == null || view.payableAmount >= minPayable) &&
                    (maxPayable == null || view.payableAmount <= maxPayable)
            }
            .sortedWith(comparator)
            .toList()

        // Tận dụng Generic Extension Function `List<T>.paginate(page, size)`
        return matchedSummaries.paginate(page = page, size = size)
    }

    // =========================================================================
    // PHẦN 3: TẠO ĐƠN MỚI, CHUYỂN TRẠNG THÁI & VALIDATION NGHIỆP VỤ
    // =========================================================================

    private fun validateAndNormalizeOrder(order: Order, isNewOrder: Boolean): Order {
        val orderId = order.id.normalizeCode()
        if (orderId.isEmpty()) {
            throw BusinessException("Ma don hang khong duoc de trong!")
        }
        if (ordersById.containsKey(orderId)) {
            throw BusinessException("Don [$orderId]: Ma don hang da ton tai trong he thong!")
        }

        val customerId = order.customerId.normalizeCode()
        if (customerId.isEmpty()) {
            throw BusinessException("Don [$orderId]: Ma khach hang khong duoc de trong!")
        }
        val customer = customersById[customerId]
            ?: throw BusinessException("Don [$orderId]: Khach hang [$customerId] khong ton tai!")

        if (order.lines.isEmpty()) {
            throw BusinessException("Don [$orderId]: Danh sach dong hang khong duoc rong!")
        }

        if (isNewOrder && order.status != OrderStatus.NEW) {
            throw BusinessException("Don [$orderId]: Khi tao don moi, trang thai bat buoc phai la NEW (nhan duoc: ${order.status})!")
        }

        // Kiểm tra tính nhất quán giữa trạng thái và thời điểm giao hàng `deliveredAt`
        if (order.status == OrderStatus.DELIVERED) {
            val deliveredAt = order.deliveredAt
                ?: throw BusinessException("Don [$orderId]: Trang thai DELIVERED bat buoc phai co thoi diem giao hang (deliveredAt)!")
            if (deliveredAt.isBefore(order.createdAt)) {
                throw BusinessException(
                    "Don [$orderId]: Thoi diem giao ($deliveredAt) khong duoc truoc thoi diem tao don (${order.createdAt})!"
                )
            }
        } else if (order.deliveredAt != null) {
            throw BusinessException(
                "Don [$orderId]: Trang thai ${order.status} (chua giao) khong duoc phep co thoi diem giao hang (${order.deliveredAt})!"
            )
        }

        val normalizedLines = order.lines.mapIndexed { idx, line ->
            val productId = line.productId.normalizeCode()
            if (productId.isEmpty()) {
                throw BusinessException("Don [$orderId] - Dong #${idx + 1}: Ma san pham khong duoc de trong!")
            }
            val product = productsById[productId]
                ?: throw BusinessException("Don [$orderId] - Dong #${idx + 1}: San pham [$productId] khong ton tai!")

            if (isNewOrder && !product.active) {
                throw BusinessException("Don [$orderId]: San pham [$productId - ${product.name}] da ngung kinh doanh, khong the tao don moi!")
            }
            if (line.quantity <= 0) {
                throw BusinessException("Don [$orderId] - San pham [$productId]: So luong mua phai > 0 (nhan duoc: ${line.quantity})!")
            }
            if (line.unitPrice <= BigDecimal.ZERO) {
                throw BusinessException("Don [$orderId] - San pham [$productId]: Don gia mua phai > 0 (nhan duoc: ${line.unitPrice})!")
            }

            line.copy(productId = productId)
        }

        return order.copy(
            id = orderId,
            customerId = customer.id,
            lines = normalizedLines
        )
    }

    /**
     * Tạo đơn hàng mới từ mã khách hàng và danh sách cặp `(productId, quantity)`.
     * Tự động chụp giá danh mục hiện tại (`product.listPrice`) và hạng hiện tại của khách (`customer.currentTier`).
     */
    fun createNewOrder(
        orderId: String,
        customerId: String,
        createdAt: LocalDateTime,
        requestedItems: List<Pair<String, Int>>
    ): OrderSummaryView {
        val normOrderId = orderId.normalizeCode()
        val normCustomerId = customerId.normalizeCode()
        val customer = customersById[normCustomerId]
            ?: throw BusinessException("Khong the tao don [$normOrderId]: Khach hang [$normCustomerId] khong ton tai!")

        if (requestedItems.isEmpty()) {
            throw BusinessException("Khong the tao don [$normOrderId]: Phai co it nhat 1 dong san pham!")
        }

        val lines = requestedItems.map { (rawProductId, qty) ->
            val prodId = rawProductId.normalizeCode()
            val product = productsById[prodId]
                ?: throw BusinessException("Khong the tao don [$normOrderId]: San pham [$prodId] khong ton tai!")
            if (!product.active) {
                throw BusinessException("Khong the tao don [$normOrderId]: San pham [$prodId - ${product.name}] dang ngung ban!")
            }
            OrderLine(
                productId = product.id,
                quantity = qty,
                unitPrice = product.listPrice // Chụp giá danh mục tại thời điểm mua!
            )
        }

        val candidateOrder = Order(
            id = normOrderId,
            customerId = customer.id,
            createdAt = createdAt,
            deliveredAt = null,
            status = OrderStatus.NEW,
            tierAtPurchase = customer.currentTier, // Chụp hạng thành viên tại thời điểm mua!
            lines = lines
        )

        val validated = validateAndNormalizeOrder(candidateOrder, isNewOrder = true)
        ordersById[validated.id] = validated
        return toOrderSummaryView(validated)
    }

    /**
     * Chuyển trạng thái đơn hàng theo đúng quy tắc máy trạng thái:
     * `NEW -> CONFIRMED -> DELIVERED`, `NEW -> CANCELLED`, `CONFIRMED -> CANCELLED`.
     * Nếu thất bại, dữ liệu gốc trong `ordersById` hoàn toàn không bị thay đổi!
     */
    fun transitionOrderStatus(
        orderId: String,
        targetStatus: OrderStatus,
        deliveredAt: LocalDateTime? = null
    ): OrderSummaryView {
        val code = orderId.normalizeCode()
        val existing = ordersById[code]
            ?: throw BusinessException("Khong tim thay don hang [$code] de chuyen trang thai!")

        if (!existing.status.canTransitionTo(targetStatus)) {
            throw BusinessException(
                "Don [$code]: Khong duoc phep chuyen trang thai tu ${existing.status} sang $targetStatus!"
            )
        }

        val updatedOrder = when (targetStatus) {
            OrderStatus.DELIVERED -> {
                val validDeliveredAt = deliveredAt
                    ?: throw BusinessException("Don [$code]: Khi chuyen sang DELIVERED phai cung cap thoi diem giao hang!")
                if (validDeliveredAt.isBefore(existing.createdAt)) {
                    throw BusinessException(
                        "Don [$code]: Thoi diem giao ($validDeliveredAt) khong duoc truoc thoi diem tao (${existing.createdAt})!"
                    )
                }
                existing.copy(status = OrderStatus.DELIVERED, deliveredAt = validDeliveredAt)
            }
            OrderStatus.CONFIRMED, OrderStatus.CANCELLED -> {
                if (deliveredAt != null) {
                    throw BusinessException("Don [$code]: Trang thai $targetStatus khong duoc co thoi diem giao hang!")
                }
                existing.copy(status = targetStatus, deliveredAt = null)
            }
            OrderStatus.NEW -> throw BusinessException("Don [$code]: Khong the quay lai trang thai NEW!")
        }

        ordersById[code] = updatedOrder
        return toOrderSummaryView(updatedOrder)
    }

    /**
     * Kiểm tra thử một đơn hàng độc lập trên dữ liệu hiện tại mà KHÔNG ghi vào `ordersById`
     * (dùng để kiểm chứng bộ 12 đơn lỗi `SampleData.invalidOrders()`).
     */
    fun validateCandidateOrderSafely(candidate: Order): OrderOperationResult<OrderSummaryView> = try {
        val validated = validateAndNormalizeOrder(candidate, isNewOrder = false)
        OrderOperationResult.Success(
            data = toOrderSummaryView(validated),
            message = "Don [${validated.id}] hop le"
        )
    } catch (e: BusinessException) {
        OrderOperationResult.Rejected(
            orderId = candidate.id.normalizeCode().ifEmpty { "<TRONG>" },
            reason = e.message ?: "Loi nghiep vu khong xac dinh"
        )
    }

    // =========================================================================
    // PHẦN 4: 5 BÁO CÁO KINH DOANH (KOTLIN COLLECTION PIPELINES)
    // =========================================================================

    fun generateBusinessReport(
        fromDate: LocalDate = common.SampleData.REPORT_FROM,
        toDate: LocalDate = common.SampleData.REPORT_TO,
        topK: Int = 3,
        asOf: LocalDateTime = common.SampleData.AS_OF
    ): BusinessReport {
        if (fromDate.isAfter(toDate)) {
            throw BusinessException("Ky bao cao khong hop le: tu ngay ($fromDate) > den ngay ($toDate)")
        }
        if (topK <= 0) {
            throw BusinessException("Tham so xep hang topK phai duong (nhan duoc: $topK)")
        }

        // Lọc các đơn có ngày tạo (`createdAt.toLocalDate()`) nằm trong đoạn [fromDate, toDate]
        val ordersInPeriod: List<Order> = ordersById.values.filter { order ->
            val createdDate = order.createdAt.toLocalDate()
            !createdDate.isBefore(fromDate) && !createdDate.isAfter(toDate)
        }

        // Chỉ các đơn DELIVERED trong kỳ mới được tính doanh thu, chi tiêu, số lượng bán và bảng xếp hạng
        val deliveredInPeriod: List<Order> = ordersInPeriod.filter { it.status == OrderStatus.DELIVERED }

        val overview = buildOverviewReport(fromDate, toDate, ordersInPeriod, deliveredInPeriod)
        val customerReport = buildCustomerReport(deliveredInPeriod, topK)
        val productReport = buildProductReport(deliveredInPeriod, topK)
        val cityTierBreakdown = buildCityTierBreakdown(deliveredInPeriod)
        val overdueAlerts = buildOverdueAlerts(asOf)

        return BusinessReport(
            overview = overview,
            customerReport = customerReport,
            productReport = productReport,
            cityTierBreakdown = cityTierBreakdown,
            overdueAlerts = overdueAlerts
        )
    }

    /** (4.1) Báo cáo Tổng quan */
    private fun buildOverviewReport(
        fromDate: LocalDate,
        toDate: LocalDate,
        ordersInPeriod: List<Order>,
        deliveredInPeriod: List<Order>
    ): OverviewReport {
        // Đếm số đơn theo từng trạng thái (đảm bảo mọi trạng thái đều xuất hiện kể cả khi có số lượng = 0)
        val groupedStatusCount = ordersInPeriod.groupingBy { it.status }.eachCount()
        val countByStatus: Map<OrderStatus, Int> = OrderStatus.entries.associateWith { status ->
            groupedStatusCount[status] ?: 0
        }

        // Tổng doanh thu và giá trị trung bình đơn đã giao
        val totalRevenue: BigDecimal = deliveredInPeriod
            .sumOf { toOrderSummaryView(it).payableAmount }
            .roundMoney()

        val averageOrderValue: BigDecimal = if (deliveredInPeriod.isEmpty()) {
            BigDecimal.ZERO.roundMoney()
        } else {
            totalRevenue.divide(BigDecimal.valueOf(deliveredInPeriod.size.toLong()), 2, RoundingMode.HALF_UP)
        }

        val distinctPurchasingCustomers: Int = deliveredInPeriod
            .map { it.customerId }
            .distinct()
            .size

        // Doanh thu theo từng tháng trong kỳ (tháng không có doanh thu vẫn hiện 0.00)
        val revenueByMonthMap = deliveredInPeriod
            .groupBy { YearMonth.from(it.createdAt) }
            .mapValues { (_, monthOrders) ->
                monthOrders.sumOf { toOrderSummaryView(it).payableAmount }.roundMoney()
            }

        val monthlyRevenue = linkedMapOf<YearMonth, BigDecimal>()
        var cursorMonth = YearMonth.from(fromDate)
        val endMonth = YearMonth.from(toDate)
        while (!cursorMonth.isAfter(endMonth)) {
            monthlyRevenue[cursorMonth] = revenueByMonthMap[cursorMonth] ?: BigDecimal.ZERO.roundMoney()
            cursorMonth = cursorMonth.plusMonths(1)
        }

        return OverviewReport(
            fromDate = fromDate,
            toDate = toDate,
            countByStatus = countByStatus,
            totalRevenue = totalRevenue,
            averageDeliveredOrderValue = averageOrderValue,
            distinctPurchasingCustomers = distinctPurchasingCustomers,
            monthlyRevenue = monthlyRevenue
        )
    }

    /** (4.2) Báo cáo Khách hàng (Top K chi tiêu + Khách không mua trong kỳ + Tập thành phố) */
    private fun buildCustomerReport(deliveredInPeriod: List<Order>, topK: Int): CustomerReport {
        val topSpendingCustomers: List<CustomerSpendRank> = deliveredInPeriod
            .groupBy { it.customerId }
            .map { (customerId, customerOrders) ->
                val customer = customersById.getValue(customerId)
                val totalSpent = customerOrders.sumOf { toOrderSummaryView(it).payableAmount }.roundMoney()
                val latestCreatedAt = customerOrders.maxOf { it.createdAt }
                CustomerSpendRank(
                    customerId = customer.id,
                    customerName = customer.name,
                    city = customer.city,
                    deliveredOrderCount = customerOrders.size,
                    totalSpent = totalSpent,
                    latestOrderCreatedAt = latestCreatedAt
                )
            }
            .sortedWith(
                compareByDescending<CustomerSpendRank> { it.totalSpent }
                    .thenByDescending { it.deliveredOrderCount }
                    .thenBy { it.customerId }
            )
            .take(topK)

        val activeCustomerIdsInPeriod: Set<String> = deliveredInPeriod.map { it.customerId }.toSet()

        val inactiveCustomers: List<Customer> = customersById.values
            .filter { it.id !in activeCustomerIdsInPeriod }
            .sortedBy { it.id }

        val purchasingCities: List<String> = activeCustomerIdsInPeriod
            .map { customersById.getValue(it).city }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)

        return CustomerReport(
            topSpendingCustomers = topSpendingCustomers,
            inactiveCustomersInPeriod = inactiveCustomers,
            purchasingCustomerCities = purchasingCities
        )
    }

    /** (4.3) Báo cáo Sản phẩm (Top K mỗi Category + Sản phẩm active chưa bán được) */
    private fun buildProductReport(deliveredInPeriod: List<Order>, topK: Int): ProductReport {
        // Trải phẳng (flatMap) các dòng hàng kèm theo orderId để đếm chính xác số đơn phân biệt chứa sản phẩm
        data class OrderLineOccurrence(val orderId: String, val line: OrderLine)

        val occurrencesByProductId: Map<String, List<OrderLineOccurrence>> = deliveredInPeriod
            .flatMap { order -> order.lines.map { line -> OrderLineOccurrence(order.id, line) } }
            .groupBy { it.line.productId }

        val allSoldProductRanks: List<ProductSalesRank> = occurrencesByProductId
            .map { (productId, occurrences) ->
                val product = productsById.getValue(productId)
                val totalQty = occurrences.sumOf { it.line.quantity }
                val grossAmount = occurrences.sumOf { it.line.lineAmount }.roundMoney()
                // Một đơn có 2 dòng cùng sản phẩm (như O008 có 2 dòng P001) chỉ được đếm 1 lần!
                val distinctOrders = occurrences.map { it.orderId }.distinct().size

                ProductSalesRank(
                    productId = product.id,
                    productName = product.name,
                    category = product.category,
                    totalQuantitySold = totalQty,
                    grossMerchandiseAmount = grossAmount,
                    distinctOrderCount = distinctOrders
                )
            }

        val ranksByCategory = allSoldProductRanks.groupBy { it.category }

        val topProductsByCategory: Map<Category, List<ProductSalesRank>> = Category.entries.associateWith { category ->
            (ranksByCategory[category] ?: emptyList())
                .sortedWith(
                    compareByDescending<ProductSalesRank> { it.totalQuantitySold }
                        .thenByDescending { it.grossMerchandiseAmount }
                        .thenBy { it.productId }
                )
                .take(topK)
        }

        val soldProductIds: Set<String> = occurrencesByProductId.keys
        val unsoldActiveProducts: List<Product> = productsById.values
            .filter { it.active && it.id !in soldProductIds }
            .sortedWith(compareBy<Product> { it.category }.thenBy { it.id })

        return ProductReport(
            topProductsByCategory = topProductsByCategory,
            unsoldActiveProducts = unsoldActiveProducts
        )
    }

    /** (4.4) Phân tích chéo Thành phố khách hàng x Hạng thành viên LÚC MUA */
    private fun buildCityTierBreakdown(deliveredInPeriod: List<Order>): List<CityTierStats> {
        return deliveredInPeriod
            .groupBy { order ->
                val city = customersById.getValue(order.customerId).city
                city to order.tierAtPurchase
            }
            .map { (cityAndTier, groupOrders) ->
                val (city, tierAtPurchase) = cityAndTier
                val distinctCustomers = groupOrders.map { it.customerId }.distinct().size
                val revenue = groupOrders.sumOf { toOrderSummaryView(it).payableAmount }.roundMoney()
                CityTierStats(
                    city = city,
                    tierAtPurchase = tierAtPurchase,
                    distinctCustomerCount = distinctCustomers,
                    totalRevenue = revenue
                )
            }
            .sortedWith(
                compareBy<CityTierStats, String>(String.CASE_INSENSITIVE_ORDER) { it.city }
                    .thenBy { it.tierAtPurchase.ordinal }
            )
    }

    /** (4.5) Cảnh báo vận hành: Đơn NEW / CONFIRMED chờ quá 72 giờ tại mốc `asOf` (xét toàn bộ lịch sử) */
    private fun buildOverdueAlerts(asOf: LocalDateTime): List<OverdueOrderAlert> {
        val threshold = Duration.ofHours(72)
        return ordersById.values
            .filter { order ->
                (order.status == OrderStatus.NEW || order.status == OrderStatus.CONFIRMED) &&
                    Duration.between(order.createdAt, asOf) > threshold
            }
            .sortedWith(
                compareByDescending<Order> { Duration.between(it.createdAt, asOf) }
                    .thenBy { it.id }
            )
            .map { order ->
                val summary = toOrderSummaryView(order)
                OverdueOrderAlert(
                    orderId = order.id,
                    customerId = order.customerId,
                    customerName = summary.customerName,
                    status = order.status,
                    createdAt = order.createdAt,
                    waitingHours = Duration.between(order.createdAt, asOf).toHours(),
                    payableAmount = summary.payableAmount
                )
            }
    }
}
