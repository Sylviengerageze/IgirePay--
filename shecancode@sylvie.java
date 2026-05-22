Flowchart Design (Idempotency Gateway – “Pay Once” Protocol)
                    +----------------------+
                    | Client Sends Request |
                    | POST /payments       |
                    +----------+-----------+
                               |
                               v
                 +----------------------------+
                 | Check Idempotency-Key      |
                 | in HTTP Headers            |
                 +-------------+--------------+
                               |
                    +----------+----------+
                    |                     |
                  Missing               Present
                    |                     |
                    v                     v
        +-------------------+   +----------------------+
        | Return 400 Error  |   | Check if Key Exists |
        | "Key Required"    |   | in Storage (Map/DB) |
        +-------------------+   +----------+-----------+
                                           |
                              +------------+------------+
                              |                         |
                            Exists                  Not Exists
                              |                         |
                              v                         v
                +--------------------------+   +--------------------------+
                | Return Saved Response    |   | Process Payment          |
                | Immediately              |   | Simulate Transaction     |
                +--------------------------+   +------------+-------------+
                                                            |
                                                            v
                                         +-------------------------------+
                                         | Save Response using           |
                                         | Idempotency-Key               |
                                         +---------------+---------------+
                                                         |
                                                         v
                                         +-------------------------------+
                                         | Return Success Response       |
                                         | Payment Processed Once        |
                                         +-------------------------------+
2. Recommended Project Structure
idempotency-gateway/
│
├── src/main/java/com/shecancode/
│   ├── controller/
│   │      PaymentController.java
│   │
│   ├── service/
│   │      PaymentService.java
│   │
│   ├── model/
│   │      PaymentRequest.java
│   │      PaymentResponse.java
│   │
│   ├── storage/
│   │      IdempotencyStore.java
│   │
│   └── IdempotencyGatewayApplication.java
│
├── src/main/resources/
│      application.properties
│
├── pom.xml
└── README.md
3. Full Java Spring Boot Code
Step 1: pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>com.shecancode</groupId>
    <artifactId>idempotency-gateway</artifactId>
    <version>1.0.0</version>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
    </parent>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Lombok (Optional) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

    </dependencies>

</project>
4. Main Application
IdempotencyGatewayApplication.java
package com.shecancode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IdempotencyGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdempotencyGatewayApplication.class, args);
    }
}
5. Payment Request Model
PaymentRequest.java
package com.shecancode.model;

public class PaymentRequest {

    private String customerName;
    private Double amount;

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
6. Payment Response Model
PaymentResponse.java
package com.shecancode.model;

public class PaymentResponse {

    private String transactionId;
    private String message;
    private Double amount;

    public PaymentResponse() {
    }

    public PaymentResponse(String transactionId, String message, Double amount) {
        this.transactionId = transactionId;
        this.message = message;
        this.amount = amount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
7. In-Memory Storage
IdempotencyStore.java
package com.shecancode.storage;

import com.shecancode.model.PaymentResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdempotencyStore {

    private final Map<String, PaymentResponse> store = new ConcurrentHashMap<>();

    public boolean containsKey(String key) {
        return store.containsKey(key);
    }

    public PaymentResponse get(String key) {
        return store.get(key);
    }

    public void save(String key, PaymentResponse response) {
        store.put(key, response);
    }
}
8. Payment Service
PaymentService.java
package com.shecancode.service;

import com.shecancode.model.PaymentRequest;
import com.shecancode.model.PaymentResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {

    public PaymentResponse processPayment(PaymentRequest request) {

        // Simulate payment processing
        String transactionId = UUID.randomUUID().toString();

        return new PaymentResponse(
                transactionId,
                "Payment Processed Successfully",
                request.getAmount()
        );
    }
}
9. Payment Controller
PaymentController.java
package com.shecancode.controller;

import com.shecancode.model.PaymentRequest;
import com.shecancode.model.PaymentResponse;
import com.shecancode.service.PaymentService;
import com.shecancode.storage.IdempotencyStore;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotencyStore store;

    public PaymentController(PaymentService paymentService,
                             IdempotencyStore store) {
        this.paymentService = paymentService;
        this.store = store;
    }

    @PostMapping
    public ResponseEntity<?> processPayment(
            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey,

            @RequestBody PaymentRequest request) {

        // Validate key
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Idempotency-Key header is required");
        }

        // Duplicate request
        if (store.containsKey(idempotencyKey)) {

            return ResponseEntity.ok(store.get(idempotencyKey));
        }

        // First request
        PaymentResponse response =
                paymentService.processPayment(request);

        // Save response
        store.save(idempotencyKey, response);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }
}
10. application.properties
spring.application.name=idempotency-gateway
server.port=8080