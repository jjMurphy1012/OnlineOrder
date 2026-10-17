# OnlineOrder
A Spring Boot and React-based ordering system

# Project Introduction

OnlineOrder is a Spring Boot and React ordering demo with restaurant/menu browsing, account registration, session login, and per-customer cart management. The current `/cart/checkout` action clears a cart; it does not process a payment, reserve inventory, or create a persistent fulfilled order. Restaurant administration is not implemented by the controllers in this repository.

## Reproducible verification

The `benchmarks/` directory documents the local PostgreSQL tests and cart-read performance comparison. Measurements use synthetic fixtures and an isolated database; they are not production traffic or AWS service-level results. The benchmark keeps a test-only copy of the original per-item lookup algorithm so the optimized batch read can be compared against it with identical output checks.


![System Image 1](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img.png)

![System Image 2](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img_1.png)

![System Image 5](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img_2.png)

![System Image 6](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img_3.png)

![System Image 7](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img_6.png)

## Future Enhancements

Planned additions include:

1. **Payment Integration** - Secure payment processing supporting multiple payment methods.

2. **Coupon System** - Promotional discounts with coupon code generation and redemption tracking.

3. **VIP Membership** - Tiered membership with benefits like priority ordering, exclusive discounts, and free delivery options.

These enhancements will improve user experience and provide additional value to both customers and restaurant partners.
## Technology Stack
### Backend Technologies

| Technology | Description | Official Website |
| --- | --- | --- |
| Spring Boot | Web application development framework | [https://spring.io/projects/spring-boot](https://spring.io/projects/spring-boot) |
| Spring Data JDBC | Database access framework | [https://spring.io/projects/spring-data-jdbc](https://spring.io/projects/spring-data-jdbc) |
| Spring MVC | Web framework | [https://docs.spring.io/spring-framework/reference/web/webmvc.html](https://docs.spring.io/spring-framework/reference/web/webmvc.html) |
| Spring Security | Authentication and authorization framework | [https://spring.io/projects/spring-security](https://spring.io/projects/spring-security) |
| PostgreSQL | Open-source relational database | [https://www.postgresql.org/](https://www.postgresql.org/) |
| Java 21 | Programming language | [https://www.oracle.com/java/](https://www.oracle.com/java/) |
| Session-based Auth | Server-side session management | [https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html) |
| Docker | Application containerization | [https://www.docker.com](https://www.docker.com) |
### Frontend Technologies

| Technology         | Description                              | Official Website                                  |
|--------------------|------------------------------------------|---------------------------------------------------|
| React              | JavaScript library for building UI       | https://reactjs.org/                              |
| Ant Design (antd)  | UI component library for enterprise apps | https://ant.design/                               |
| React Hooks        | React's built-in state and lifecycle API | https://reactjs.org/docs/hooks-intro.html         |

## Core Functional Modules
### User Management Module
- **Registration**: `/signup` (POST)
    - Accepts user credentials (email, password, name)
    - Creates new user accounts

### Shopping Cart Module
- **Retrieve Cart**: `/cart` (GET)
    - Retrieves current user's cart contents

- **Add to Cart**: `/cart` (POST)
    - Adds menu items to the shopping cart

- **Checkout**: `/cart/checkout` (POST)
    - Clears the current user's cart; payment and order fulfillment are not implemented

## Data Model
### Primary Entities
- **CustomerEntity**: User information
- **CartEntity**: Shopping cart data
- **MenuItemEntity**: Menu item information
- **RestaurantEntity**: Restaurant details
- **OrderItemEntity**: Order item information

## Architecture & Design Patterns

The application uses a Controller → Service → Repository structure within one Spring Boot application:

### Core Architecture

| Component | Implementation | Benefits |
| --- | --- | --- |
| **Layered Architecture** | Controller → Service → Repository structure | Clear separation of responsibilities, enhanced maintainability |
| **MVC Pattern** | Spring MVC framework | Separation of concerns, improved testability |
| **HTTP API Design** | HTTP methods and resource-based endpoints | Client/server separation; authentication uses server-side sessions |
| **Application Structure** | One Spring Boot backend and a bundled React build | A layered monolith, not a microservices deployment |

### Design Patterns

| Pattern | Implementation | Purpose |
| --- | --- | --- |
| **Dependency Injection** | Spring's annotation-based DI | Loose coupling, better testability |
| **Repository Pattern** | Spring Data JDBC repositories | Data access abstraction, simplified queries |
| **Singleton Pattern** | Spring-managed service beans | Resource efficiency, state management |
| **DTO Pattern** | Data Transfer Objects | Separation of domain and presentation layers |

### Security Architecture

- **Authentication**: Session-based security implementation via Spring Security
- **Authorization**: Protected routes require authentication; cart controllers derive the customer from the authenticated principal
- **Principal Retrieval**: Context-aware user identification using `@AuthenticationPrincipal`

### Infrastructure & Deployment

- **Deployment exercise**: Docker and AWS screenshots are retained below as historical project context
- **Database**: PostgreSQL with connection pooling
- **State Management**: PostgreSQL persistence, server-side login sessions, and an in-process Caffeine cache

## Deployment Exercise

The original screenshots show a Docker/AWS deployment exercise. This checkout has no Dockerfile and its `docker-compose.yml` is empty, so it does not currently provide a reproducible Compose deployment. Use the local PostgreSQL benchmark instructions for the configuration tested in this repository.

### Docker Screenshot

![Docker Containers](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img_7.png)

### AWS Screenshot

The original project description identifies AWS App Runner and RDS PostgreSQL as the deployment environment. No current cloud deployment or availability verification is included in the local test results.
![AWS Deployment](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img_8.png)

The screenshots document the original deployment exercise. Local benchmark results do not establish cloud availability, throughput, or current deployment health.

## Demo boundaries

- The default SQL initializer drops and recreates the six demo tables. Use a disposable database. Set `INIT_DB=never` when retaining an existing schema and data.
- The development runner creates a sample user at startup. This is demo setup, not a production identity lifecycle.
- Cart writes use transactions and invalidate the local cache after successful mutations. Concurrent lost-update prevention and consistency across multiple application instances have not been established.
- Session authentication currently has CSRF protection disabled. The application requires further security work before production use.
- Monetary fields currently use Java `double`. A production payment system would require an explicit monetary precision/rounding policy and corresponding tests.
