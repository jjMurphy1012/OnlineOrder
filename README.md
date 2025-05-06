# OnlineOrder
A Spring Boot and React-based ordering system

# Project Introduction

The OnlineOrder system is a food ordering platform that connects customers with restaurants, built on SpringBoot and deployed using Docker. The customer-facing system features restaurant discovery, menu browsing, cart management, and checkout functionality, while the management interface allows restaurants to manage menus, process orders, and track customer data.


![System Image 1](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img.png)

![System Image 2](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img_1.png)

![System Image 5](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img_4.png)

![System Image 6](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img_5.png)

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
    - Processes the order and clears the cart

## Data Model
### Primary Entities
- **CustomerEntity**: User information
- **CartEntity**: Shopping cart data
- **MenuItemEntity**: Menu item information
- **RestaurantEntity**: Restaurant details
- **OrderItemEntity**: Order item information

## Architecture & Design Patterns

This project implements industry-standard software architecture principles and design patterns to ensure maintainability, scalability, and robust security:

### Core Architecture

| Component | Implementation | Benefits |
| --- | --- | --- |
| **Layered Architecture** | Controller → Service → Repository structure | Clear separation of responsibilities, enhanced maintainability |
| **MVC Pattern** | Spring MVC framework | Separation of concerns, improved testability |
| **RESTful API Design** | HTTP methods, resource-based endpoints | Stateless communication, scalability |
| **Microservices Approach** | Decoupled frontend-backend architecture | Independent deployment, technology flexibility |

### Design Patterns

| Pattern | Implementation | Purpose |
| --- | --- | --- |
| **Dependency Injection** | Spring's annotation-based DI | Loose coupling, better testability |
| **Repository Pattern** | Spring Data JDBC repositories | Data access abstraction, simplified queries |
| **Singleton Pattern** | Spring-managed service beans | Resource efficiency, state management |
| **DTO Pattern** | Data Transfer Objects | Separation of domain and presentation layers |

### Security Architecture

- **Authentication**: Session-based security implementation via Spring Security
- **Authorization**: Role-based access control for resource protection
- **Principal Retrieval**: Context-aware user identification using `@AuthenticationPrincipal`

### Infrastructure & Deployment

- **Containerization**: Docker-based application packaging
- **Cloud Deployment**: AWS App Runner for managed container orchestration
- **Database**: PostgreSQL with connection pooling
- **State Management**: Stateless service design with persistent data layer

## Deployment Environment
The system is containerized using Docker with docker-compose for orchestration, ensuring portability and rapid deployment capabilities.

### Docker Containerization
This project uses Docker for containerization, which provides several advantages:

- **Consistent Environment**: Eliminates "works on my machine" issues by ensuring the same environment across development and production
- **Isolated Services**: Each component (Spring Boot application, PostgreSQL database) runs in its own container
- **Easy Scaling**: Container orchestration simplifies horizontal scaling as needed
- **Simplified Deployment**: Single command deployment with docker-compose

![Docker Containers](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img_7.png)

### AWS Deployment
The Online Order System has been successfully deployed to Amazon Web Services (AWS), providing:

- **High Availability**: Leveraging AWS's reliable infrastructure
- **Scalability**: Ability to scale with increasing user demands
- **Security**: Implementation of AWS security best practices
- **Global Reach**: Fast access for users regardless of geographic location

The deployment architecture includes:
- AWS App Runner for hosting and automatically scaling the containerized application
- RDS PostgreSQL as a managed database service
- AWS IAM for access control and security
![AWS Deployment](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img_8.png)

This cloud-based solution ensures the application remains robust, secure, and accessible to users while minimizing operational overhead.
