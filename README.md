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
| Gradle | Dependency management tool | [https://gradle.org/](https://gradle.org/) |
| Java 21 | Programming language | [https://www.oracle.com/java/](https://www.oracle.com/java/) |
| Docker | Application containerization | [https://www.docker.com](https://www.docker.com) |
| JWT | Token-based authentication | [https://github.com/jwtk/jjwt](https://github.com/jwtk/jjwt) |
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

## Security Implementation
- Spring Security-based authentication and authorization
- Current user retrieval using `@AuthenticationPrincipal User`

## System Characteristics
1. RESTful API design pattern
2. Microservice architecture with frontend-backend separation
3. Built on Spring ecosystem
4. Docker containerization for deployment

## Deployment Environment
The system is containerized using Docker with docker-compose for orchestration, ensuring portability and rapid deployment capabilities.

### Docker Containerization
This project uses Docker for containerization, which provides several advantages:

- **Consistent Environment**: Eliminates "works on my machine" issues by ensuring the same environment across development and production
- **Isolated Services**: Each component (Spring Boot application, MySQL database) runs in its own container
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
- EC2 instances for hosting the containerized application
- RDS for managed MySQL database services
- Elastic Load Balancer for traffic distribution
- AWS IAM for access control and security

![AWS Deployment](https://github.com/jjMurphy1012/OnlineOrder/raw/main/image/img_8.png)

This cloud-based approach ensures the application remains robust, secure, and accessible to users at all times while minimizing operational overhead.