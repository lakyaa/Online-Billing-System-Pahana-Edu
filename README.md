# Pahana Edu Online Billing System (Tasks B & C)

This repository contains the implementation for **Task B (System Development)** and **Task C (Testing & Test Automation)** of the CIS6003 Advanced Programming assessment (Online Billing System for Pahana Edu).

## Features (Task B)
- **Login (User Authentication)** with form-based Spring Security.
- **Customer Management**: Add, update, delete, list, search.
- **Item Management**: Add, update, delete, list.
- **Billing**: Calculate bill for a customer based on units consumed and item price; generate invoice view.
- **Reports**: Simple report pages for customers, items, and bills.
- **Help** page.
- **Logout**.
- Architecture uses **MVC + Service + Repository (DAO)**.
- Design patterns used: **MVC**, **DAO/Repository**, **Service Layer**, **Singleton** (via Spring beans), **Factory** (simple service factory example).
- **JPA/Hibernate** with **H2** in-memory DB (can switch to MySQL).
- **REST APIs** for AJAX/clients + **Thymeleaf** UI for user-friendly interfaces.

## Testing (Task C)
- **TDD approach**: Sample tests written first for services and controllers.
- **JUnit 5 + Spring Boot Test + MockMvc**.
- **Automated tests** via **GitHub Actions** workflow (`.github/workflows/maven.yml`).

## Run locally
```bash
mvn spring-boot:run
# App at http://localhost:8080
# H2 console: http://localhost:8080/h2-console (jdbc:h2:mem:pahana; user: sa; no password)
```

## Default users
- `admin` / `password`
- `clerk` / `password`

## Switch to MySQL
Edit `src/main/resources/application.properties` and uncomment the MySQL section. Ensure DB exists and credentials are correct.

## API endpoints (examples)
- `GET /api/customers`
- `POST /api/customers`
- `PUT /api/customers/{id}`
- `DELETE /api/customers/{id}`
- `GET /api/items`
- `POST /api/bills/calculate`

## Notes
- This codebase is intentionally compact for teaching. You can expand reports, validations, and billing rules as required by your brief.
- UML diagrams are not included (Task A is separate).

