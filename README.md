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
│   │   ├── OrderItem.java                    # Order Item JPA Entity (product line item)
│   │   └── OrderStatus.java                  # Order Status Enum
│   ├── dto/
│   │   ├── CreateOrderRequest.java           # Create Order DTO
│   │   ├── OrderItemRequest.java             # Order Item Request DTO
│   │   ├── OrderItemResponse.java            # Order Item Response DTO (enriched with product details)
│   │   ├── UpdateOrderStatusRequest.java     # Update Status DTO
│   │   ├── OrderResponse.java                # Order Response DTO
│   │   └── ProductDetails.java               # Product Details DTO
│   ├── client/
│   │   └── ProductServiceClient.java         # Product Service REST Client
│   ├── configuration/
│   │   └── RestTemplateConfig.java           # RestTemplate Bean Configuration
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
- ✅ Multiple products per order (OrderItem support)
- ✅ Microservice integration with Product Service via RestTemplate
- ✅ Non-blocking product detail enrichment for individual order retrieval

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
    Long id,                          // Primary Key (Auto-generated, provided by BaseModel)
    String orderNumber,               // Unique order number (ORD-XXXXXXXX)
    Long customerId,                  // Customer ID
    List<OrderItem> items,            // Multiple product items in the order
    BigDecimal totalAmount,           // Total price (calculated as sum of all items)
    OrderStatus orderStatus,          // Current order status
    LocalDateTime createdAt           // Creation timestamp (audited via BaseModel)
}

OrderItem {
    Long id,                          // Primary Key (Auto-generated)
    Long productId,                   // Product ID
    Integer quantity,                 // Item quantity
    BigDecimal price,                 // Unit price
    BigDecimal totalAmount,           // Item total (price * quantity)
    String productName,               // Product name (enriched from Product Service)
    String productDescription,        // Product description (enriched from Product Service)
    Order order                       // Reference to parent Order
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
    "items": [
        {
            "productId": 101,
            "quantity": 2,
            "price": 49.99
        },
        {
            "productId": 202,
            "quantity": 1,
            "price": 99.50
        }
    ]
}

Response (201 Created):
{
    "message": "Order created successfully",
    "status": 201,
    "data": {
        "id": 1,
        "orderNumber": "ORD-A1B2C3D4",
        "customerId": 1,
        "items": [
            {
                "id": 101,
                "productId": 101,
                "quantity": 2,
                "price": 49.99,
                "totalAmount": 99.98,
                "productName": null,
                "productDescription": null
            },
            {
                "id": 102,
                "productId": 202,
                "quantity": 1,
                "price": 99.50,
                "totalAmount": 99.50,
                "productName": null,
                "productDescription": null
            }
        ],
        "totalAmount": 199.48,
        "orderStatus": "PENDING",
        "createdAt": "2026-05-25T10:30:00"
    }
}
```

Note: When creating an order, `productName` and `productDescription` are `null`. They are only populated when calling `GET /api/orders/{id}`.


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
            "items": [
                {
                    "id": 101,
                    "productId": 101,
                    "quantity": 2,
                    "price": 49.99,
                    "totalAmount": 99.98,
                    "productName": null,
                    "productDescription": null
                }
            ],
            "totalAmount": 99.98,
            "orderStatus": "PENDING",
            "createdAt": "2026-05-25T10:30:00"
        }
    ]
}
```

Note: Product details (`productName`, `productDescription`) are `null` in list endpoints.


### 3. Get Order by ID (Enriched with Product Details)
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
        "items": [
            {
                "id": 101,
                "productId": 101,
                "quantity": 2,
                "price": 49.99,
                "totalAmount": 99.98,
                "productName": "Laptop",
                "productDescription": "High-performance laptop for professionals"
            },
            {
                "id": 102,
                "productId": 202,
                "quantity": 1,
                "price": 99.50,
                "totalAmount": 99.50,
                "productName": "Wireless Mouse",
                "productDescription": "Ergonomic wireless mouse with long battery life"
            }
        ],
        "totalAmount": 199.48,
        "orderStatus": "PENDING",
        "createdAt": "2026-05-25T10:30:00"
    }
}
```

**Note**: This endpoint enriches order items with product details from the Product Service. If product details are unavailable, the `productName` and `productDescription` fields will be `null`, but the order is still returned successfully.


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
            "items": [
                {
                    "id": 101,
                    "productId": 101,
                    "quantity": 2,
                    "price": 49.99,
                    "totalAmount": 99.98,
                    "productName": null,
                    "productDescription": null
                }
            ],
            "totalAmount": 99.98,
            "orderStatus": "PENDING",
            "createdAt": "2026-05-25T10:30:00"
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
        "items": [
            {
                "id": 101,
                "productId": 101,
                "quantity": 2,
                "price": 49.99,
                "totalAmount": 99.98,
                "productName": null,
                "productDescription": null
            }
        ],
        "totalAmount": 99.98,
        "orderStatus": "CONFIRMED",
        "createdAt": "2026-05-25T10:30:00"
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

## Microservice Integration

### Product Service Integration

The Order Service integrates with a Product Service to enrich order item details. This integration is seamless and non-blocking — if the Product Service is unavailable, orders are still returned with product details missing.

**Configuration:**
```properties
# application.properties
product.service.base-url=http://localhost:8080/product-service
```

**How it works:**
1. When calling `GET /api/orders/{id}`, the service fetches each order item
2. For each item, it calls the Product Service endpoint: `GET {product.service.base-url}/product/{productId}`
3. If available, the response extracts `productName` and `productDescription` and enriches the order item
4. If the Product Service is unavailable or the product is not found, the order item is returned without product details

**Example Product Service Response (expected format):**
```json
{
    "status": 200,
    "data": {
        "id": 101,
        "productName": "Laptop",
        "productDescription": "High-performance laptop for professionals",
        "price": 49.99
    }
}
```

Or direct format:
```json
{
    "id": 101,
    "productName": "Laptop",
    "productDescription": "High-performance laptop for professionals",
    "price": 49.99
}
```

The client supports both wrapper and direct JSON formats.

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
# Create an order with multiple items
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "items": [
      {
        "productId": 101,
        "quantity": 2,
        "price": 49.99
      },
      {
        "productId": 202,
        "quantity": 1,
        "price": 99.50
      }
    ]
  }'

# Get all orders
curl http://localhost:8080/api/orders

# Get order by ID (with enriched product details)
curl http://localhost:8080/api/orders/1

# Get orders by customer ID
curl http://localhost:8080/api/orders/customer/1

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
- Replace RestTemplate with Feign Client for service-to-service communication
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

