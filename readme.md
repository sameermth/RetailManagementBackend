# Retail Management Backend

A comprehensive retail management system built with Spring Boot, PostgreSQL, and Gradle.

## Features

- **Authentication & Authorization**: JWT-based security with role-based access control
- **Product Management**: Complete CRUD operations for products, categories, and brands
- **Inventory Management**: Track stock levels, movements, and warehouse management
- **Sales Management**: Process sales, generate invoices, and track payments
- **Customer Management**: Manage customer information, dues, and loyalty programs
- **Supplier Management**: Track suppliers, purchases, and outstanding payments
- **Distributor Management**: Manage distributors, orders, and commissions
- **Expense Management**: Track expenses with category budgets and recurring expenses
- **Reporting**: Generate comprehensive reports in multiple formats (PDF, Excel, CSV)
- **Notifications**: Multi-channel notifications (Email, SMS, In-app)

## Tech Stack

- Java 17
- Spring Boot 3.1.x
- Spring Security with JWT
- Spring Data JPA
- PostgreSQL
- Gradle 9.4
- Lombok
- MapStruct
- OpenAPI (Swagger)
- iText PDF
- Apache POI

## Prerequisites

- JDK 17 or higher
- PostgreSQL 14 or higher
- Gradle 9.4 (or use the wrapper)

## Getting Started

1. Clone the repository
2. Create PostgreSQL database
3. Update application.properties with your database credentials
4. Run `./gradlew build`
5. Run `./gradlew bootRun`

## API Documentation

Once the application is running, access Swagger UI at:
http://localhost:8080/swagger-ui.html

## Project Structure
rc/main/java/com/retailmanagement/ \
├── common/ # Common utilities, configs, exceptions  \
├── modules/ # Business modules \
│ ├── auth/ # Authentication & Authorization \
│ ├── product/ # Product management \
│ ├── inventory/ # Inventory management \
│ ├── sales/ # Sales management \
│ ├── customer/ # Customer management \
│ ├── supplier/ # Supplier management \
│ ├── purchase/ # Purchase management \
│ ├── distributor/ # Distributor management \
│ ├── expense/ # Expense management \
│ ├── report/ # Report generation \
│ └── notification/# Notification system \
└── infrastructure/ # Infrastructure components 

## Build and Deployment

```bash
# Build the project
./gradlew clean build

# Run tests
./gradlew test

# Run with dev profile
./gradlew bootRun

# Create Docker image
docker build -t retail-management .

# Run with Docker Compose
docker-compose up