# BGAI - AI Gateway and Processing Platform

BGAI is an enterprise-grade AI gateway and processing platform designed to integrate with various large language models (LLMs), provide distributed transaction management, and implement robust billing and authentication mechanisms.

## Project Overview

This platform acts as a central hub for AI processing requests, providing the following core functionality:

- **AI Model Integration**: Seamlessly connects to DeepSeek and other LLMs
- **Gateway & Load Balancing**: Routes requests to appropriate AI services
- **Distributed Transactions**: Uses Seata for reliable distributed transaction management
- **Reactive Programming**: Built with Spring WebFlux for high concurrency
- **Usage Tracking & Billing**: Monitors and bills for AI resource consumption
- **Multi-tenant**: Supports multiple users and organizations

## Technology Stack

- **Java 21**: Core programming language
- **Spring Boot 3.0**: Application framework
- **Spring WebFlux**: Reactive web framework
- **Spring Cloud**: Microservices infrastructure
- **Seata**: Distributed transaction management
- **RocketMQ**: Message queue for asynchronous processing
- **MyBatis-Plus**: ORM and data access
- **Redis**: Caching and rate limiting
- **Nacos**: Service discovery and configuration
- **Docker**: Containerization

## Features

### AI Integration
- Connection to DeepSeek LLM API with failover capabilities
- Support for multi-turn conversations
- File processing and content extraction

### Transaction Management
- Distributed transaction logging
- Branch transaction tracking
- Transaction compensation with SAGA pattern
- Automatic rollback on failure

### Security
- Token-based authentication
- API key management
- Rate limiting
- IP filtering

### Scalability
- Reactive request handling
- Asynchronous processing
- Circuit breaking for fault tolerance
- Dynamic route configuration

## Setup Instructions

### Prerequisites
- JDK 21
- Maven 3.8+
- RocketMQ 5.1+
- Redis 6.0+
- MySQL 8.0+
- Seata 1.7.0
- Nacos 2.2.0+ (for service discovery)

### Configuration

1. **Database Setup**

```sql
-- Run the schema scripts
source src/main/resources/sql/user_schema.sql
```

2. **Environment Variables**

```
NACOS_HOST=<nacos-host>
NACOS_PORT=<nacos-port>
NACOS_NAMESPACE=<nacos-namespace>
NACOS_GROUP=<nacos-group>
SPRING_PROFILES_ACTIVE=<dev|test|prod>
```

3. **Build and Run**

```bash
mvn clean package
java -jar target/bgai-0.0.1-SNAPSHOT.jar
```

4. **Docker Deployment**

```bash
docker build -t bgai:latest .
docker run -p 8080:8080 bgai:latest
```

## API Documentation

### Chat API

#### Request
```
POST /Api/chat
Content-Type: multipart/form-data

Parameters:
- file: (optional) File to be processed
- question: (required) User question/prompt
- apiUrl: (optional) Custom API URL
- apiKey: (optional) Custom API key
- modelName: (optional) Model name to use (default: deepseek-chat)
- multiTurn: (optional) Whether to maintain conversation history (default: false)
```

#### Response
```json
{
  "content": "AI response content",
  "usage": {
    "chatCompletionId": "unique-id",
    "promptTokens": 100,
    "totalTokens": 150,
    "completionTokens": 50,
    "modelType": "deepseek-chat"
  }
}
```

### Transaction Testing API

```
GET /Api/test-transaction?userId=<user-id>
```

## Architecture

```
┌───────────────────────────┐
│       Client Apps          │
└─────────────┬─────────────┘
              │
┌─────────────▼─────────────┐
│     API Gateway Layer      │
│   (Load Balancing/Routing) │
└─────────────┬─────────────┘
              │
┌─────────────▼─────────────┐
│    Authentication &        │
│    Authorization Layer     │
└─────────────┬─────────────┘
              │
┌─────────────▼─────────────┐
│     Service Layer          │
│  (Controllers/WebFlux)     │
└─────────────┬─────────────┘
              │
    ┌─────────▼─────────┐
┌───┴───┐        ┌──────▼───┐
│ Seata │        │  Service │
│  TM   │        │  Logic   │
└───┬───┘        └──────┬───┘
    │                   │
    │     ┌─────────────▼───────────┐
    │     │     Message Queue       │
    │     │      (RocketMQ)         │
    │     └─────────────┬───────────┘
    │                   │
┌───▼───────────────────▼───────────┐
│        Database Layer              │
│      (Master/Slave DBs)            │
└───────────────────────────────────┘
```

## Development Guide

### Project Structure

- `/src/main/java/com/bgpay/bgai/` - Core application code
  - `/config/` - Configuration classes
  - `/controller/` - REST API controllers
  - `/service/` - Business logic services
  - `/entity/` - Data models
  - `/mapper/` - Database access
  - `/interceptor/` - AOP and request interceptors
  - `/exception/` - Exception handling

### Adding a New AI Model Integration

1. Create a new service interface extending `ChatCompletionsService`
2. Implement the service with model-specific logic
3. Add appropriate configuration in `application.yml`
4. Register the new service implementation in the service layer

### Transaction Management

The project uses Seata for distributed transactions. Key components:

1. `SeataTransactionInterceptor`: AOP interceptor for transaction logging
2. `TransactionLogService`: Service for recording transaction events
3. `TransactionLog`: Entity for transaction data

### Extending the Platform

- **New Authentication Provider**: Implement custom filters in `/filter/` directory
- **Custom LLM Integrations**: Add new services in `/service/` directory
- **API Extensions**: Add new controllers in `/controller/` directory

## Troubleshooting

### Common Issues

1. **Seata Transaction Logs Not Recording**
   - Check `application.yml` for proper Seata configuration
   - Verify `@GlobalTransactional` annotations are correctly placed
   - Ensure transaction interceptors are catching all required methods

2. **WebFlux Context Issues**
   - Use `ReactiveRequestContextHolder` to maintain context across asynchronous boundaries
   - Verify WebFilter is correctly registered

3. **Missing User Information**
   - Check token parsing in authentication filters
   - Verify HTTP headers are correctly processed

## License

Copyright © 2024 BGPAY 