package view

/**
 * BÀI 01: GENERICS, CONSTRAINTS VÀ VARIANCE (out vs in) TRONG KOTLIN
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE / TYPE ERASURE):
 *    - Cả Java và Kotlin trên JVM đều chịu ảnh hưởng của **Type Erasure (Xóa kiểu lúc Runtime)**:
 *      + Khi biên dịch ra bytecode, tham số kiểu `<T>` sẽ bị xóa (erase) về **Upper Bound hiệu lực (Effective Upper Bound)** của nó:
 *        * Nếu khai báo `<T>` không có ràng buộc trên (mặc định trong Kotlin là `<T : Any?>`), `T` bị xóa thành `java.lang.Object`.
 *        * Nếu khai báo có Upper Bound như `fun <T : Number> calc(x: T)` hoặc `fun <T : Animal> findHeaviestAnimal(...)`,
 *          thì trong chữ ký phương thức Bytecode, `T` được xóa về chính Upper Bound đó (`java.lang.Number` hoặc `view.Animal`),
 *          KHÔNG PHẢI lúc nào cũng xóa về `java.lang.Object`!
 *      + Trình biên dịch tự động chèn lệnh ép kiểu `CHECKCAST` tại các điểm đọc dữ liệu cụ thể.
 *    - Ở tầng Compile-time, hệ thống kiểu của Kotlin khắc phục điểm yếu dễ gây nhầm lẫn nhất
 *      của Java Generics (`lesson11-colection/src/generic/Ex01WildCardBasicDemo.java`):
 *      + Trong Java: `List<Dog>` KHÔNG PHẢI là kiểu con của `List<Animal>` (Invariant).
 *        Để truyền `List<Dog>` vào hàm nhận `List<Animal>`, Java phải dùng Use-site Wildcard:
 *        `List<? extends Animal>` (Producer - chỉ đọc) hoặc `List<? super Dog>` (Consumer - chỉ ghi) -> Quy tắc PECS.
 *      + Trong Kotlin: Hỗ trợ cả **Declaration-site Variance** (khai báo ngay trên class/interface)
 *        lẫn **Use-site Variance** (Type Projection tại tham số hàm) với 2 từ khóa trực quan:
 *        * `out T` (Covariant - Tương đương `? extends T`): Chỉ xuất T ra ở vị trí trả về (Producer).
 *          Nhờ `interface List<out E>` trong Kotlin là bất biến (read-only), `List<Dog>` TỰ ĐỘNG
 *          là kiểu con của `List<Animal>` mà không cần viết wildcard rườm rà!
 *        * `in T` (Contravariant - Tương đương `? super T`): Chỉ nhận T vào ở vị trí tham số (Consumer).
 *          Ví dụ: `Comparable<in T>`, `Comparator<in T>`. Một bộ so sánh `Animal` đương nhiên so sánh được `Dog`!
 *        * `*` (Star Projection - Tương đương `?` trong Java): Khi không quan tâm kiểu cụ thể là gì,
 *          an toàn hơn Raw Type của Java vì không cho phép ghi phần tử sai kiểu vào tập hợp.
 *
 * 2. SO SÁNH JAVA VS KOTLIN:
 *    - Java (`lesson11-colection/src/generic`):
 *      + Upper bound: `<T extends Comparable<T>>` (erase về `Comparable`)
 *      + Multiple bounds: `<T extends Animal & Comparable<T>>` (erase về bound đầu tiên là `Animal`)
 *      + Wildcards: `? extends T` (Producer), `? super T` (Consumer), `?` (Unbounded)
 *    - Kotlin:
 *      + Upper bound: `<T : Comparable<T>>` (Mặc định nếu không ghi gì là `<T : Any?>` - cho phép cả null;
 *        nếu muốn cấm null thì viết `<T : Any>`).
 *      + Multiple bounds: dùng mệnh đề `where T : Animal, T : Comparable<T>` rõ ràng, dễ đọc.
 *      + Variance: `out T` (Producer), `in T` (Consumer), `*` (Star projection).
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong Spring Boot, bạn thường viết các wrapper generic như `ApiResponse<out T>` hoặc
 *      `PageResponse<out T>` cho REST API. Việc đánh dấu `out T` giúp bạn có thể trả về
 *      `ApiResponse<Nothing>` khi lỗi (vì `Nothing` là kiểu con của mọi kiểu `T`), nên một
 *      `ApiResponse<Nothing>` tự động tương thích với `ApiResponse<UserResponse>`!
 */

// 1. Generic Class với `out T` (Declaration-site Covariance)
// Nhờ `val items: List<T>` chỉ có getter (chỉ xuất dữ liệu ra, không có hàm nhận T vào),
// ta có thể đánh dấu `out T` để `PageResponse<VipCustomer>` là kiểu con của `PageResponse<Customer>`!
data class PageResponse<out T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

// 2. Hierarchy minh họa Variance (Animal -> Dog -> Becgie) giống Ex01WildCardBasicDemo.java
open class Animal(val name: String, val weightKg: Double) : Comparable<Animal> {
    override fun compareTo(other: Animal): Int = this.weightKg.compareTo(other.weightKg)
    override fun toString(): String = "${this::class.simpleName}(name='$name', weight=${weightKg}kg)"
}

open class Dog(name: String, weightKg: Double, val breed: String) : Animal(name, weightKg)
class Becgie(name: String, weightKg: Double) : Dog(name, weightKg, "Becgie Duc")
class Cat(name: String, weightKg: Double) : Animal(name, weightKg)

// 3. Declaration-site Variance: Producer (`out`) vs Consumer (`in`)
// Producer: Chỉ tạo/trả về T (giống Supplier<T> trong Java)
interface AnimalShelterProducer<out T : Animal> {
    fun adoptOne(): T
}

// Consumer: Chỉ nhận T vào để xử lý (giống Consumer<T> / Comparator<T> trong Java)
interface VetClinicConsumer<in T : Animal> {
    fun examine(patient: T): String
}

// 4. Generic Function: Phân trang bất kỳ List<T> nào (Không có upper bound -> T erase về Object)
fun <T> paginate(source: List<T>, page: Int, size: Int): PageResponse<T> {
    require(page >= 1) { "So trang phai >= 1 (nhan duoc: $page)" }
    require(size >= 1) { "Kich thuoc trang phai >= 1 (nhan duoc: $size)" }

    val totalElements = source.size.toLong()
    val totalPages = if (source.isEmpty()) 0 else ((source.size + size - 1) / size)
    val fromIndex = (page - 1) * size

    val pageItems = if (fromIndex >= source.size) {
        emptyList()
    } else {
        source.subList(fromIndex, minOf(fromIndex + size, source.size))
    }

    return PageResponse(
        items = pageItems,
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages
    )
}

// 5. Upper Bound (`<T : Comparable<T>>`) và Multiple Constraints (`where`)
// Chú ý về Type Erasure: Vì `T` có ràng buộc `T : Animal`, trong Bytecode kiểu trả về của hàm này
// được erase về `view.Animal` chứ KHÔNG phải `java.lang.Object`!
fun <T> findHeaviestAnimal(animals: List<T>): T?
        where T : Animal,
              T : Comparable<Animal> {
    if (animals.isEmpty()) return null
    var heaviest = animals.first()
    for (item in animals) {
        if (item > heaviest) { // Gọi compareTo() thông qua toán tử `>` của Kotlin!
            heaviest = item
        }
    }
    return heaviest
}

// 6. Use-site Variance (Type Projection): Khi làm việc với MutableList<T> (vốn là Invariant)
// MutableList<T> vừa có get() (out) vừa có add() (in) nên mặc định là Invariant (bất biến kiểu).
// Để copy từ danh sách con sang danh sách cha giống Collections.copy(dest, src) trong Java:
// - `source`: chỉ đọc -> dùng `MutableList<out T>` (tương đương `List<? extends T>`)
// - `destination`: chỉ ghi -> dùng `MutableList<in T>` (tương đương `List<? super T>`)
fun <T> copyAnimals(source: MutableList<out T>, destination: MutableList<in T>) {
    for (item in source) {
        destination.add(item)
    }
}

// 7. Star Projection (`*`): Khi chỉ cần kiểm tra thông tin chung mà không cần biết kiểu T cụ thể
fun printCollectionInfo(list: List<*>) {
    // Với `List<*>`, phần tử đọc ra được coi là `Any?` (an toàn kiểu 100%)
    val firstPreview = if (list.isNotEmpty()) list.first().toString() else "Rong"
    println("  -> Danh sach co ${list.size} phan tu, phan tu dau tien: $firstPreview")
}

fun main() {
    println("--- ___ * START EX01: GENERICS, CONSTRAINTS & VARIANCE * ___ ---\n")

    // =========================================================================
    // 1. GENERIC CLASS & GENERIC FUNCTION (PageResponse<T>)
    // =========================================================================
    println("1. Generic Class & Generic Function (Phan trang du lieu):")
    val products = listOf("Laptop ThinkPad", "Ban phim co Keychron", "Chuot Logitech", "Man hinh Dell", "Tai nghe Sony")
    val page1 = paginate(products, page = 1, size = 2)
    val page3 = paginate(products, page = 3, size = 2)
    println("  - Trang 1: $page1")
    println("  - Trang 3: $page3")
    check(page1.items.size == 2 && page1.totalPages == 3)

    // =========================================================================
    // 2. UPPER BOUNDS & MULTIPLE CONSTRAINTS (`where`)
    // =========================================================================
    println("\n2. Upper Bounds & Multiple Constraints (`where`):")
    val dogs: List<Dog> = listOf(
        Dog("Milu", 12.5, "Corgi"),
        Becgie("Rex", 34.0),
        Dog("Kiki", 8.2, "Poodle")
    )
    val heaviestDog: Dog? = findHeaviestAnimal(dogs)
    println("  - Chu cho nang nhat dan: $heaviestDog")
    check(heaviestDog?.name == "Rex")

    // =========================================================================
    // 3. DECLARATION-SITE VARIANCE (`out` Producer & `in` Consumer)
    // =========================================================================
    println("\n3. Declaration-site Variance (`out` Producer vs `in` Consumer):")

    // 3a. Nhờ `List<out E>` và `PageResponse<out T>`, PageResponse<Dog> là kiểu con của PageResponse<Animal>!
    val dogPage: PageResponse<Dog> = paginate(dogs, page = 1, size = 2)
    val animalPage: PageResponse<Animal> = dogPage // Hợp lệ 100% không cần wildcard!
    println("  - Gan PageResponse<Dog> cho PageResponse<Animal> thanh cong: ${animalPage.items}")

    // 3b. Producer (`out`): Trại cứu hộ Becgie có thể đóng vai trò là Trại cứu hộ Animal nói chung
    val becgieShelter = object : AnimalShelterProducer<Becgie> {
        override fun adoptOne(): Becgie = Becgie("Max", 36.5)
    }
    val generalAnimalShelter: AnimalShelterProducer<Animal> = becgieShelter // `out` cho phép ép lên kiểu cha (Upcast)
    println("  - Nhan nuoi tu AnimalShelterProducer<Animal>: ${generalAnimalShelter.adoptOne()}")

    // 3c. Consumer (`in`): Bác sĩ thú y khám được mọi Animal thì chắc chắn khám được cho Becgie!
    val generalVet = object : VetClinicConsumer<Animal> {
        override fun examine(patient: Animal): String =
            "Da kham tong quat cho ${patient.name} (${patient.weightKg}kg) -> Suc khoe TOT!"
    }
    val becgieSpecialistVet: VetClinicConsumer<Becgie> = generalVet // `in` cho phép ép xuống kiểu con (Contravariant)
    println("  - Kham cho Becgie bang VetClinicConsumer<Animal>: ${becgieSpecialistVet.examine(Becgie("Rocky", 38.0))}")

    // =========================================================================
    // 4. USE-SITE VARIANCE (PECS trong Kotlin với MutableList) & STAR PROJECTION (`*`)
    // =========================================================================
    println("\n4. Use-site Variance (`out` / `in` tren MutableList) & Star Projection (`*`):")
    val sourceBecgies: MutableList<Becgie> = mutableListOf(Becgie("Alpha", 32.0), Becgie("Bravo", 35.0))
    val destAnimals: MutableList<Animal> = mutableListOf(Cat("Mimi", 4.2))

    // Copy từ MutableList<Becgie> (out Dog) sang MutableList<Animal> (in Dog)
    copyAnimals<Dog>(source = sourceBecgies, destination = destAnimals)
    println("  - Danh sach destAnimals sau khi copy tu sourceBecgies: $destAnimals")
    check(destAnimals.size == 3)

    println("  - Kiem tra Star Projection `List<*>`:")
    printCollectionInfo(sourceBecgies)
    printCollectionInfo(products)

    println("\n--- ___ * END EX01 * ___ ---")
}
