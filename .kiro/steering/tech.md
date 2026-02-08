# Technology Stack

## Core Technologies

- **Java**: JDK 21 (required)
- **Spring Boot**: 3.5.10
- **Build Tool**: Gradle with Gradle Wrapper
- **Spring Cloud**: 2024.0.0
- **Spring Modulith**: 1.3.0

## Key Dependencies

- **Web**: Spring WebMVC, WebClient (reactive HTTP)
- **Data**: Spring Data JDBC
- **Batch**: Spring Batch (Job, Step, ItemReader, ItemProcessor, ItemWriter)
- **Messaging**: Apache Kafka, Kafka Streams
- **Resilience**: Resilience4J circuit breaker
- **Observability**: Spring Boot Actuator, Spring Modulith Observability
- **Code Generation**: Lombok
- **Testing**: JUnit Platform, Spring Boot Test starters

## Common Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Clean build artifacts
./gradlew clean
```

### Batch Commands
```bash
# Run User Report Job
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob"

# Run Team Report Job
./gradlew :batch:bootRun --args="--spring.batch.job.name=teamReportJob"
```

### Docker Image
```bash
# Build OCI image (uses Paketo buildpacks)
./gradlew bootBuildImage
```

### Development
```bash
# Continuous build
./gradlew build --continuous

# Check dependencies
./gradlew dependencies
```

## Platform Notes

- Use `./gradlew` on Unix/Linux/Mac
- Use `gradlew.bat` on Windows
- Gradle wrapper ensures consistent build environment
