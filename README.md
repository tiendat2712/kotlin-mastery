# Kotlin Journey — From Java Core to Idiomatic Kotlin & Spring Boot

A structured, multi-module **Kotlin/JVM** learning repository designed for Java developers transitioning to **Spring Boot with Kotlin**. Each lesson contains runnable `main()` examples, JVM bytecode/memory analysis, Java-vs-Kotlin comparisons, and active learning exercises.

## Tech Stack

- **Kotlin:** `2.0.21`
- **Gradle:** `8.14` (Kotlin DSL)
- **Coroutines:** `kotlinx-coroutines-core:1.8.1`
- **JVM Target:** `17` (compatible with JDK 17 LTS and JDK 21 LTS)

---

## Project Structure (12 Modules)

| Module | Key Topics | Files (`src/`) |
| :--- | :--- | :--- |
| **`lesson01-basics-and-types`** | `val` vs `var` vs `const val`, JVM memory model (local vars, heap fields, closure capture), `Any`/`Unit`/`Nothing`, String templates, `==` vs `===`, Operator Overloading | `Ex01` – `Ex06` |
| **`lesson02-null-safety-and-scope`** | Nullable types (`?`), Safe calls (`?.`), Elvis (`?:`), Smart casts (`is`), Safe casts (`as?`), Scope functions (`let`, `also`, `apply`, `run`, `with`), `lateinit var`, `by lazy` | `Ex01` – `Ex06`, `CustomerModels.kt` |
| **`lesson03-control-flow-and-loops`** | Nullable `Boolean?`, `if-else` & exhaustive `when` expressions, Ranges (`..`, `..<`, `downTo`, `step`), Labeled `break@` / `continue@` | `Ex01` – `Ex05` |
| **`lesson04-functions-and-extensions`** | Default/Named args, Higher-order functions, Extension functions, Kotlin DSL, `inline`/`noinline`/`crossinline`, Local vs Non-local returns, Closure capture | `Ex01` – `Ex07` |
| **`lesson05-oop-classes-and-objects`** | Properties & backing `field`, Constructors & `init` order, Visibility (`internal`), `open` vs `final` (JDK vs CGLIB proxies), SAM interfaces, Nested vs `inner class` (`this@Outer`) | `Ex01` – `Ex07`, `Car.kt` |
| **`lesson06-special-classes-and-delegation`** | `data class` (shallow `.copy()`, DTO vs JPA `@Entity` trade-offs), `enum class`, `sealed interface`, `object` & `companion object`, `@JvmInline value class`, Delegation (`by`) | `Ex01` – `Ex05` |
| **`lesson07-arrays-and-collections`** | Primitive arrays (`IntArray` vs `Array<Int>`), Read-only (`List`) vs Mutable (`MutableList`), Shallow copy (`toList()`), Mutable aliasing, Shared mutable state & thread-safety | `Ex01` – `Ex06` |
| **`lesson08-collection-pipelines-and-sequences`** | `filter`, `map`, `flatMap`, `associateBy` (in-memory indexing), `groupBy`, `partition`, `sumOf`, `fold`, Eager `Iterable` vs Lazy `Sequence`, Transaction exercises | `Ex01` – `Ex05`, `DomainModels.kt` |
| **`lesson09-generics-exceptions-annotations`** | Variance (`out`/`in`), Type erasure by upper bound, `reified` limits with nested generics & Type Tokens, `require`/`check`/`runCatching`, `.use {}`, `@field:`/`@get:`/`@param:` targets | `Ex01` – `Ex05` |
| **`lesson10-coroutines-basics`** | `suspend` mechanics, Concurrency vs Parallelism, `runBlocking`/`launch`/`async`, Dispatchers & Structured Concurrency, `Flow<T>`, Timeouts, `Mutex`, `flowOn`/`buffer`/`catch`, `StateFlow`/`SharedFlow` | `Ex01` – `Ex06` |
| **`lesson11-java-interop-and-spring-readiness`** | Platform types (`T!`), `-Xjsr305=strict`, `@JvmStatic`/`@JvmOverloads`/`@JvmField`/`@Throws`, `plugin.spring` vs `plugin.jpa`, Pure-Kotlin Spring architecture simulation | `Ex01` – `Ex03`, `LegacyPaymentGateway.java` |
| **`lesson12-final-assignment`** | **Capstone Project: Nova Shop** — End-to-end order management, data normalization, generic pagination, state transitions, and 5 business analytics reports | `Main.kt`, `NovaShopService.kt`, `DomainModels.kt`, `SampleData.kt` |

---

## Recommended Learning Path

1. **Core Backend Foundations (`lesson01` $\rightarrow$ `lesson09`):** Master syntax, null safety, functions/extensions, OOP, immutability, collection pipelines, generics, and annotation use-site targets.
2. **Java Interop & Architecture Patterns (`lesson11`):** Understand Java-Kotlin interoperability and how Kotlin idioms map to layered backend architecture *(note: `Ex03` is a pure-Kotlin simulation, not a real Spring runtime)*.
3. **Hands-on Capstone (`lesson12`):** Implement the **Nova Shop** assignment using idiomatic Kotlin before comparing with the reference solution.
4. **Coroutines & Reactive Streams (`lesson10`):** Learn the fundamentals (`Ex01`–`Ex03`) early; study the advanced concurrency and Flow topics (`Ex04`–`Ex06`) when moving into asynchronous workloads or **Spring WebFlux**.

---

## Getting Started

### Run in IntelliJ IDEA
1. Open the **`Kotlin_Journey`** directory in IntelliJ IDEA.
2. Select **JDK 17** or **JDK 21** as the Project SDK and sync Gradle.
3. Open any `Ex0X...kt` file (or `lesson12-final-assignment/src/Main.kt`) and click the green **Run (`▶`)** icon next to `fun main()`.

### Build via CLI
```powershell
.\gradlew.bat classes
```

---

## Readiness Checklist Before Moving to Spring Boot

- [ ] Clearly distinguish `val` (read-only reference), `List<T>` (read-only interface), shallow copy (`toList()`, `data class.copy()`), and deep immutability.
- [ ] Use `?.`, `?:` (with `return`/`throw`), `as?`, `requireNotNull()`, and scope functions (`let`, `also`, `apply`, `run`, `with`) idiomatically.
- [ ] Understand `inline`/`noinline`/`crossinline`, local vs non-local returns, and mutable closure capture.
- [ ] Know why Kotlin classes are `final` by default, why nested classes are static by default, and how `kotlin("plugin.spring")` (`all-open`) differs from `kotlin("plugin.jpa")` (`no-arg`).
- [ ] Understand why `@field:` / `@get:` / `@param:` use-site targets matter for constructor properties in validation and serialization.

*(Next stage in a real Spring Boot project: Spring IoC container lifecycle, Spring MVC/Web, Jakarta Bean Validation runtime, Spring Data JPA/Hibernate persistence context & `@Transactional` propagation, `jackson-module-kotlin`, and JUnit 5 / MockK testing).*

---

## Study Notes & Official References

- **[`kotlin_core_interview_notes.md`](./kotlin_core_interview_notes.md)** — 20 in-depth Q&A topics covering Kotlin Core, JVM bytecode mechanics, and Spring Boot readiness (in Vietnamese).
- [Kotlin Official Documentation](https://kotlinlang.org/docs/home.html) | [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html) | [Spring Framework Kotlin Support](https://docs.spring.io/spring-framework/reference/languages/kotlin.html)
