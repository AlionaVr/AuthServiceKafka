# Auth + Notification Microservices (Kafka, Native Apache Kafka Client)

## Overview

This project consists of two microservices:

### **AuthService**

* User registration via email
* Verification code generation and validation
* JWT access token issuance
* Publishing verification codes to Kafka (native `kafka-clients`)

### **NotificationService**

* Kafka consumer service
* Receives verification codes from Kafka
* Simulates email delivery by printing messages to the console

Services communicate **asynchronously via Apache Kafka**.

---

## Architecture

```
Client
  |
  v
AuthService  ----->  Kafka  ----->  NotificationService
  |
  v
PostgreSQL
```

---

## Tech Stack

* Java 17
* Spring Boot
* Spring Security
* PostgreSQL
* Apache Kafka (KRaft mode, without ZooKeeper)/Spring Kafka
* Docker

---

## Running the Project

### Build services

Inside each service directory:

```bash
./gradlew build
```

---

### Run with Docker

From the directory containing `docker-compose.yml`:

```bash
docker compose up --build
```

---

### Verification

AuthService:

```
http://localhost:8080
```

NotificationService logs:

```bash
docker logs -f notification-service
```

---

## Kafka

### Topic

```
verification-code
```

### Producer

AuthService publishes JSON messages:

```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

### Consumer

NotificationService consumes messages and prints them to the console.

---

## Security

* Stateless JWT authentication
* Protected endpoint requires a valid access token
* Email verification is required before token issuance

---

## API

### Register

```
POST /api/auth/register
```

### Verify

```
POST /api/auth/verify
```

### Protected endpoint

```
GET /api/protected
```

---