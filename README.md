# Order Service Microservice

A Spring Boot microservice for managing orders in an e-commerce application. Built with a clean layered architecture following microservices best practices.

## Technology Stack

- **Language**: Java 17
- **Framework**: Spring Boot 4.0.6
- **Build Tool**: Maven
- **Database**: MySQL
- **ORM**: Spring Data JPA
- **Dependency Injection**: Lombok
- **Validation**: Jakarta Bean Validation

## Project Structure

```
order-service/
├── src/main/java/org/furmani/orderservice/
│   ├── OrderServiceApplication.java          # Main Spring Boot Application
│   ├── controller/
│   │   └── OrderController.java              # REST API Endpoints
│   ├── service/
│   │   ├── OrderService.java                 # Service interface
│   │   └── OrderServiceImpl.java             # Service implementation
│   ├── repository/
│   │   └── OrderRepository.java              # Data Access Layer
│   ├── models/
│   │   ├── BaseModel.java                    # Common auditing fields (id, createdAt, lastUpdatedAt)
│   │   ├── Order.java                        # Order JPA Entity (extends BaseModel)
│   │   └── OrderStatus.java                  # Order Status Enum
│   ├── dto/
│   │   ├── CreateOrderRequest.java           # Create Order DTO
│   │   ├── UpdateOrderStatusRequest.java     # Update Status DTO
│   │   └── OrderResponse.java                # Order Response DTO
│   ├── exception/
│   │   ├── OrderNotFoundException.java       # Custom Exception
│   │   └── GlobalExceptionHandler.java       # Global Exception Handler
│   └── response/
│       └── ApiResponse.java                  # Standard API Response
└── pom.xml                                    # Maven Configuration
```

## Architecture Highlights

### Layered Architecture
- **Controller Layer**: Handles HTTP requests and responses
- **Service Layer**: Encapsulates business logic
- **Repository Layer**: Abstracts database operations
- **Entity Layer**: Defines JPA entities
- **DTO Layer**: Data Transfer Objects for API communication
- **Exception Handling**: Global exception handler with custom exceptions

### Key Features
- ✅ Constructor injection using Lombok's `@RequiredArgsConstructor`
- ✅ Global exception handling with `@ControllerAdvice`
- ✅ Request/Response validation using Jakarta Bean Validation
- ✅ Standard API response structure
- ✅ Proper HTTP status codes
- ✅ CORS support
- ✅ Transaction management
- ✅ Lombok annotations for boilerplate reduction

## Database Setup

### Create Database
```sql
CREATE DATABASE order_service_db;
```

### Configuration
Update `src/main/resources/application.properties` with your MySQL credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/order_service_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

Tables are automatically created by Hibernate with `spring.jpa.hibernate.ddl-auto=update`

## Order Entity

```java
Order {
    Long id,                      // Primary Key (Auto-generated, provided by BaseModel)
    String orderNumber,           // Unique order number (ORD-XXXXXXXX)
    Long customerId,              // Customer ID (renamed from userId)
    Long productId,               // Product ID
    Integer quantity,             // Order quantity
    BigDecimal price,             // Unit price
    BigDecimal totalAmount,       // Total price (calculated)
    OrderStatus orderStatus,      // Current order status
    LocalDateTime createdAt       // Creation timestamp (audited via BaseModel)
}
```

## Order Status Enum

```
PENDING      -> Order placed but not yet confirmed
CONFIRMED    -> Order confirmed and ready for processing
SHIPPED      -> Order has been shipped
DELIVERED    -> Order delivered to customer
CANCELLED    -> Order cancelled
```

## REST API Endpoints

### 1. Create Order
```http
POST /api/orders
Content-Type: application/json

Request Body:
{
    "customerId": 1,
    "productId": 101,
    "quantity": 5,
    "price": 99.99
}

Response (201 Created):
{
    "message": "Order created successfully",
    "status": 201,
    "data": {
        "id": 1,
        "orderNumber": "ORD-A1B2C3D4",
        "customerId": 1,
        "productId": 101,
        "quantity": 5,
        "price": 99.99,
        "totalAmount": 499.95,
        "orderStatus": "PENDING",
        "createdAt": "2026-05-24T10:30:00"
    }
}
```

### 2. Get All Orders
```http
GET /api/orders

Response (200 OK):
{
    "message": "Orders retrieved successfully",
    "status": 200,
    "data": [
        {
            "id": 1,
            "orderNumber": "ORD-A1B2C3D4",
            "customerId": 1,
            "productId": 101,
            "quantity": 5,
            "price": 99.99,
            "totalAmount": 499.95,
            "orderStatus": "PENDING",
            "createdAt": "2026-05-24T10:30:00"
        }
    ]
}
```

### 3. Get Order by ID
```http
GET /api/orders/{id}

Response (200 OK):
{
    "message": "Order retrieved successfully",
    "status": 200,
    "data": {
        "id": 1,
        "orderNumber": "ORD-A1B2C3D4",
        "customerId": 1,
        "productId": 101,
        "quantity": 5,
        "price": 99.99,
        "totalAmount": 499.95,
        "orderStatus": "PENDING",
        "createdAt": "2026-05-24T10:30:00"
    }
}
```

### 4. Get Orders by Customer ID
```http
GET /api/orders/customer/{customerId}

Response (200 OK):
{
    "message": "Orders retrieved successfully for customer",
    "status": 200,
    "data": [
        {
            "id": 1,
            "orderNumber": "ORD-A1B2C3D4",
            "customerId": 1,
            "productId": 101,
            "quantity": 5,
            "price": 99.99,
            "totalAmount": 499.95,
            "orderStatus": "PENDING",
            "createdAt": "2026-05-24T10:30:00"
        }
    ]
}
```

### 5. Update Order Status
```http
PUT /api/orders/{id}/status
Content-Type: application/json

Request Body:
{
    "orderStatus": "CONFIRMED"
}

Response (200 OK):
{
    "message": "Order status updated successfully",
    "status": 200,
    "data": {
        "id": 1,
        "orderNumber": "ORD-A1B2C3D4",
        "customerId": 1,
        "productId": 101,
        "quantity": 5,
        "price": 99.99,
        "totalAmount": 499.95,
        "orderStatus": "CONFIRMED",
        "createdAt": "2026-05-24T10:30:00"
    }
}
```

### 6. Delete/Cancel Order
```http
DELETE /api/orders/{id}

Response (204 No Content):
{
    "message": "Order deleted successfully",
    "status": 204
}
```

## Error Handling

The API includes comprehensive error handling:

### Validation Error (400 Bad Request)
```json
{
    "message": "Validation failed",
    "status": 400,
    "data": {
        "customerId": "Customer ID cannot be null",
        "quantity": "Quantity must be positive"
    }
}
```

### Order Not Found (404 Not Found)
```json
{
    "message": "Order not found with ID: 999",
    "status": 404
}
```

### Internal Server Error (500)
```json
{
    "message": "An unexpected error occurred: [error details]",
    "status": 500
}
```

## Building and Running

### Prerequisites
- Java 17 or higher
- MySQL 8.0 or higher
- Maven 3.6+ (or use Maven wrapper)

### Build
```bash
./mvnw clean package
```

### Run
```bash
./mvnw spring-boot:run
```

The service will start on `http://localhost:8080`

## API Documentation

Once the application is running, you can test the APIs using:

### Using cURL
```bash
# Create an order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "productId": 101,
    "quantity": 5,
    "price": 99.99
  }'

# Get all orders
curl http://localhost:8080/api/orders

# Get order by ID
curl http://localhost:8080/api/orders/1

# Update order status
curl -X PUT http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{
    "orderStatus": "SHIPPED"
  }'

# Delete order
curl -X DELETE http://localhost:8080/api/orders/1
```

### Using Postman or Insomnia
Import the API endpoints mentioned above into your REST client.

## Dependencies

```xml
- spring-boot-starter-web         (REST API support)
- spring-boot-starter-data-jpa    (JPA/Hibernate)
- spring-boot-starter-validation  (Bean Validation)
- mysql-connector-j               (MySQL JDBC driver)
- lombok                          (Boilerplate reduction)
- spring-boot-devtools            (Development utilities)
```

## Best Practices Implemented

1. **Separation of Concerns**: Clean layered architecture
2. **Dependency Injection**: Constructor-based injection using Lombok
3. **Validation**: Request validation using Jakarta Bean Validation
4. **Exception Handling**: Global exception handling with @ControllerAdvice
5. **DTOs**: Separate DTOs from entities for API communication
6. **HTTP Status Codes**: Proper HTTP status codes for each operation
7. **Transaction Management**: Transactional methods in service layer
8. **Security**: CORS configuration for cross-origin requests
9. **Logging**: Configured logging for debugging
10. **Database**: Auto-generated IDs, proper constraints, and timestamps

## Future Enhancements

- Add authentication and authorization (Spring Security)
- Add pagination and filtering
- Add caching (Redis)
- Add messaging queue (RabbitMQ/Kafka)
- Add service-to-service communication (Feign Client)
- Add API documentation (Swagger/OpenAPI)
- Add unit and integration tests
- Add deployment configuration (Docker, Kubernetes)
- Add monitoring and observability (Micrometer, Prometheus)

## License

This project is created for learning purposes.

## Support

For issues or questions, please refer to the Spring Boot documentation:
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Jakarta Validation](https://beanvalidation.org/)

