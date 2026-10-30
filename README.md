# Day 4 code review exercise: Spring and databases

A small Spring Boot online store (Java 17+, Spring Boot 3.3, in-memory H2 database).
It has bugs of the kind covered in the Day 4 notes. Seven automated checks in
`src/test/java/com/shop/Day4ChecksTest.java` fail until you fix them.

## 1. Check your setup (once)

In a terminal:

```
java -version     # needs 17 or newer
mvn -v            # Maven; if "command not found": brew install maven
```

Spring itself needs no installation: Maven downloads it the first time you build (a few minutes, needs internet).

## 2. Open and run

- **VS Code / Cursor:** install "Extension Pack for Java" (and "Spring Boot Extension Pack" if you like).
  Open this folder, wait for the Java project to load, then open `Day4ChecksTest.java` and click "Run Test" above the class.
- **IntelliJ:** File > Open > select this folder (it reads `pom.xml`). Right-click `Day4ChecksTest` > Run.
- **Terminal:** `mvn test` from this folder.

On the first run all 7 checks fail. That is expected.

## 3. Do the exercise

1. **Review first (about 45 minutes), fix nothing yet.** Above each problem line, write
   `// REVIEW [Blocker|Major|Minor|Nit] what is wrong -> what breaks in production -> the fix`.
   Review every file under `src/main/java` and `src/main/resources/application.properties`.
   There are more problems than checks.
2. **Then fix**, one part at a time, until all 7 checks pass.
3. **Send me the project folder (zipped)**, or just the files you changed.

| Part | Files | Check |
| --- | --- | --- |
| A | `inventory/InventoryService.java` | 1 |
| B | `order/OrderService.java` (the coupon) | 2 |
| C | `order/OrderService.java` (the payment), `payment/RazorpayGateway.java` | 3 |
| D | `order/OrderHistoryService.java`, `order/OrderRepository.java` | 4 |
| E | `report/ReportRepository.java`, `report/ReportService.java` | 5 |
| F | `order/OrderController.java` | 6 and 7 |
| G | `order/Order.java`, `order/OrderItem.java`, `application.properties` | none: review only |

Suggested order: E, A, D, B, F, C, G.

## Rules

- Do not change `Day4ChecksTest.java` or `RecordingPaymentGateway.java`.
- Keep these as they are, because the checks call them:
  `InventoryService.reserveCart(List<String>)`,
  `OrderService.placeOrder(String customerId, String sku, int qty, String city, String couponCode)`,
  `OrderHistoryService.summaries(String)` and the `OrderSummary` record,
  `ReportRepository.countOrdersInCity(String)`,
  the `Product` constructor, `ProductRepository.findBySku`, the getters on `Order`,
  the JSON fields of `POST /orders` (customerId, sku, qty, city, couponCode),
  and the table and column names.
- You may add classes, beans, DTOs, repository methods and exception handlers.
- The app must still start with no environment variables set: if you move a setting out of
  `application.properties`, give it a default.
