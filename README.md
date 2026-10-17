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

- **Historical deployment**: A container image built with Gradle `bootBuildImage`, published to Amazon ECR, and deployed on AWS App Runner with Amazon RDS PostgreSQL
- **Database**: PostgreSQL with connection pooling
- **State Management**: PostgreSQL persistence, server-side login sessions, and an in-process Caffeine cache

## Historical AWS Deployment

The project owner confirms that the application was containerized using Gradle `bootBuildImage`, pushed to Amazon ECR, and deployed on AWS App Runner, with PostgreSQL hosted on Amazon RDS. The original README records App Runner and RDS, and the screenshots below preserve Docker and App Runner deployment context.

`bootBuildImage` uses Cloud Native Buildpacks through the Docker daemon; a handwritten Dockerfile is not required for this build route. A build command has the form `./gradlew bootBuildImage --imageName=onlineorder:<version>`. The historical image tag, builder version, and complete cloud configuration were not retained in the repository. The empty `docker-compose.yml` is not a working deployment recipe.

The release flow is: bundled React assets and Spring Boot application → container image → Amazon ECR → AWS App Runner. At runtime, the Spring Boot application connects to Amazon RDS PostgreSQL through JDBC. Application configuration accepts database host, port, username, and password through environment variables. The database name in the default JDBC URL is `onlineorder`.

The original RDS network configuration is not recorded, so public database connectivity versus a VPC connector cannot be reconstructed from this checkout. Current service availability and deployment of the later benchmark optimization have not been verified.

### Docker Screenshot

![Docker Containers](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img_7.png)

### AWS Screenshot

The CloudWatch screenshot shows App Runner log groups for the `onlineorder` service. It records historical deployment activity, not a current health check or a complete deployment log.
![AWS Deployment](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img_8.png)

Local benchmark results establish the reported service-layer behavior on an isolated PostgreSQL fixture; they do not establish AWS latency, throughput, availability, or current deployment health.

## Demo boundaries

- The default SQL initializer drops and recreates the six demo tables. Use a disposable database. Set `INIT_DB=never` when retaining an existing schema and data.
- The development runner creates a sample user at startup. This is demo setup, not a production identity lifecycle.
- Cart writes use transactions and invalidate the local cache after successful mutations. Concurrent lost-update prevention and consistency across multiple application instances have not been established.
- Session authentication currently has CSRF protection disabled. The application requires further security work before production use.
- Monetary fields currently use Java `double`. A production payment system would require an explicit monetary precision/rounding policy and corresponding tests.
