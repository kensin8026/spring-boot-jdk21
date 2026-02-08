# Project Structure

## Multi-Module Organization

프로젝트는 3개의 Gradle 모듈로 구성되어 있습니다:

```
spring_boot_jdk21/
├── core/                           # 공유 코어 모듈
│   └── src/main/java/com/kiro/jdk21/core/
│       ├── domain/                 # User, Team 도메인 엔티티
│       ├── application/            # 비즈니스 로직
│       │   ├── port/in/           # Use Case 인터페이스
│       │   ├── port/out/          # Repository 포트
│       │   └── service/           # 서비스 구현
│       └── adapter/out/persistence/ # 데이터베이스 어댑터
│
├── api/                            # REST API 모듈 (독립 실행)
│   └── src/main/java/com/kiro/jdk21/api/
│       ├── adapter/in/web/        # REST 컨트롤러
│       └── ApiApplication.java    # 메인 애플리케이션
│
└── batch/                          # 배치 모듈 (독립 실행)
    └── src/main/java/com/kiro/jdk21/batch/
        ├── config/                # 배치 설정
        └── BatchApplication.java  # 메인 애플리케이션
```

## Package Organization

### Core Module
Base package: `com.kiro.jdk21.core`

- **domain/**: 도메인 엔티티 (User, Team)
- **application/port/in/**: Use Case 인터페이스
- **application/port/out/**: Repository 포트 인터페이스
- **application/service/**: 비즈니스 로직 구현
- **adapter/out/persistence/**: JDBC 리포지토리 어댑터

### API Module
Base package: `com.kiro.jdk21.api`

- **adapter/in/web/**: REST 컨트롤러 (HelloController, UserController, TeamController)
- **ApiApplication**: Spring Boot 메인 클래스

### Batch Module
Base package: `com.kiro.jdk21.batch`

- **config/**: 배치 작업 설정
- **BatchApplication**: Spring Boot 메인 클래스

## Conventions

- **Hexagonal Architecture**: 포트와 어댑터 패턴 적용
- **Module Independence**: API와 Batch는 독립적으로 실행 가능
- **Shared Core**: 도메인과 비즈니스 로직은 Core 모듈에서 공유
- **Configuration**: 각 모듈은 자체 application.properties 보유
- **Lombok**: 보일러플레이트 코드 감소
- **MapStruct**: 엔티티-도메인 객체 매핑

## Build Artifacts

- `build/`: Gradle 빌드 출력 (gitignored)
- `*/build/`: 각 모듈의 빌드 출력
- `.gradle/`: Gradle 캐시 (gitignored)

## Configuration Files

- `build.gradle`: 루트 프로젝트 설정 (공통 의존성 관리)
- `settings.gradle`: 멀티모듈 설정 (core, api, batch)
- `core/build.gradle`: Core 모듈 의존성
- `api/build.gradle`: API 모듈 의존성 및 Spring Boot 플러그인
- `batch/build.gradle`: Batch 모듈 의존성 및 Spring Boot 플러그인
- `docker-compose.yml`: MySQL, API, Batch 컨테이너 설정

## Docker Deployment

각 실행 가능한 모듈은 독립적인 Docker 이미지로 빌드됩니다:
- `api:latest` - REST API 서버
- `batch:latest` - 배치 작업 서버
