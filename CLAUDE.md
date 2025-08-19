# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Maven Commands
```bash
# Clean and install dependencies
mvn clean install

# Run development server (Jetty)
mvn jetty:run

# Build WAR file for production
mvn clean package

# Run with different port
mvn jetty:run -Djetty.port=8081

# Run with automatic reload for JSP files
mvn jetty:run -Djetty.reload=automatic
```

### Docker Commands
```bash
# Build Docker image
docker build -t deadlock-app .

# Run container
docker run -d -p 8080:8080 --name deadlock-server deadlock-app

# Using Docker Compose
docker-compose up -d
```

### Quick Start Scripts
- **Windows**: `run.bat` - Automated build and run
- **Linux/WSL**: `start-server.sh` - Docker-based setup with environment detection

## Project Architecture

### Technology Stack
- **Backend**: Spring Framework 5.3.23 + Java 11
- **View Layer**: JSP + JSTL
- **Build System**: Maven
- **External APIs**: Steam Web API, Deadlock API
- **HTTP Client**: Apache HttpComponents with connection pooling
- **Logging**: SLF4J + Logback

### Key Architecture Patterns

**MVC Structure**: Controllers handle HTTP requests, Services contain business logic, Models represent data structures

**Service Layer Design**:
- `SteamService`: Steam OpenID authentication, user profile data, caching with TTL
- `DeadlockService`: Game statistics, match history, fallback data generation
- Both services use `PoolingHttpClientConnectionManager` for efficient HTTP connections

**Caching Strategy**: 
- In-memory `ConcurrentHashMap` with TTL-based expiration
- Separate caches for user info, match data, and stats
- Configurable TTL values in `application.properties`

**Error Handling**: Fallback data generation when external APIs are unavailable

### Configuration Management

**Property Files**: `src/main/resources/application.properties`
- Steam API configuration with environment variable fallback
- HTTP client timeouts and connection pool settings
- Cache TTL configurations

**Spring Configuration**: 
- `servlet-context.xml`: MVC configuration, view resolver, static resources
- `root-context.xml`: Service layer component scanning

### Steam Integration Details

**Authentication Flow**:
1. Generate Steam OpenID URL via `SteamService.getOpenIdLoginUrl()`
2. User redirected to Steam for authentication  
3. Steam redirects back with OpenID identity
4. Extract Steam ID from identity URL
5. Fetch user profile data from Steam Web API

**API Key Configuration**: Set `STEAM_API_KEY` environment variable or use default in properties file

### Development Workflow

**Local Development**:
- JSP files auto-reload in development mode
- Static resources served from `/resources/` 
- Session-based user state management

**File Structure**:
- Controllers: `src/main/java/com/example/controller/`
- Services: `src/main/java/com/example/service/`  
- JSP Views: `src/main/webapp/WEB-INF/views/`
- Static Assets: `src/main/webapp/resources/`

**Testing**: JUnit 4 + Spring Test integration (basic setup in pom.xml)

## Important Implementation Notes

**Security Considerations**:
- Steam API key should be set via environment variable in production
- Session timeout configured at server level
- Input validation in service layer methods

**Performance Optimizations**:
- HTTP connection pooling with configurable limits
- Multi-level caching (user info, match data, stats)
- Fallback data generation to maintain user experience

**API Integration**:
- Deadlock API base URL configurable via properties
- User-Agent headers set for API requests
- Graceful degradation when external services are unavailable

**Deployment**:
- WAR file packaging for traditional servlet containers
- Docker containerization with multi-stage build
- Environment-specific configuration via properties