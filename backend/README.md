# Smart Smoke Sensor Backend

This directory contains the Spring Boot backend for the smart smoke sensor system.

## Tech Stack

- Java 17
- Maven
- Spring Boot 3
- Spring Web
- Spring Data JPA
- MySQL Driver
- Lombok
- Validation

## Package

Base package:

```text
com.chinasoft.smokesensor
```

## Local Build

```bash
mvn clean package
```

## Local Run

Update `src/main/resources/application.yml` with your local MySQL database, username, and password, then run:

```bash
mvn spring-boot:run
```

The default server port is `8080`.
