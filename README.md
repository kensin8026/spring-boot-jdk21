# Spring Boot JDK21 Multi-Module Demo

Spring Boot 3.5.10과 Java 21을 사용한 헥사고날 아키텍처 기반 멀티모듈 데모 애플리케이션

## 주요 기능

- **헥사고날 아키텍처**: 포트와 어댑터 패턴으로 도메인 로직 분리
- **멀티모듈 구조**: Core, API, Batch 모듈로 관심사 분리
- **Spring Batch**: Job, Step, Chunk 기반 배치 처리
- **Spring Data JDBC**: 경량 데이터 액세스
- **MapStruct**: 컴파일 타임 객체 매핑
- **Docker**: Cloud Native Buildpacks 기반 컨테이너화

## 기술 스택

- **Java**: JDK 21
- **Spring Boot**: 3.5.10
- **Spring Batch**: 5.1.2
- **Spring Cloud**: 2024.0.0
- **Spring Modulith**: 1.3.0
- **Build Tool**: Gradle with Gradle Wrapper
- **Database**: MySQL 8
- **Container**: Docker, Docker Compose

## 프로젝트 구조

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
        ├── config/                # Spring Batch Job 설정
        └── BatchApplication.java  # 메인 애플리케이션
```

## 빠른 시작

### 사전 요구사항

- JDK 21
- Docker & Docker Compose

### 1. 프로젝트 클론

```bash
git clone <repository-url>
cd spring_boot_jdk21
```

### 2. 빌드

```bash
# Unix/Linux/Mac
./gradlew build

# Windows
.\gradlew.bat build
```

### 3. Docker 이미지 생성

```bash
# API 모듈
./gradlew :api:bootBuildImage --imageName=api:latest

# Batch 모듈
./gradlew :batch:bootBuildImage --imageName=batch:latest
```

### 4. 실행

```bash
# MySQL과 API 서버 시작
docker-compose up -d

# API 접속
curl http://localhost:8080
curl http://localhost:8080/users
curl http://localhost:8080/teams
```

## API 엔드포인트

### User API

- `GET /users` - 사용자 목록 조회
- `POST /users` - 사용자 생성
- `PUT /users/{id}` - 사용자 수정
- `DELETE /users/{id}` - 사용자 삭제

### Team API

- `GET /teams` - 팀 목록 조회
- `POST /teams` - 팀 생성

### Health Check

- `GET /actuator/health` - 헬스 체크

## 배치 작업

### 구현된 Job

- **userReportJob**: 사용자 리포트 생성
- **teamReportJob**: 팀 리포트 생성

### 실행 방법

#### 단일 실행

```bash
# User Report Job
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob"

# Team Report Job
./gradlew :batch:bootRun --args="--spring.batch.job.name=teamReportJob"
```

#### 반복 실행 (타임스탬프 파라미터 필수)

```bash
# Unix/Linux/Mac
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=$(date +%s%3N)"

# Windows PowerShell
.\gradlew.bat :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
```

**중요**: 동일한 Job을 여러 번 실행하려면 타임스탬프 파라미터가 필요합니다. Spring Batch는 `Job 이름 + Job 파라미터`로 Job Instance를 식별하며, 동일한 파라미터로 이미 성공한 Job은 재실행되지 않습니다.

### 동시 실행

서로 다른 Job은 동시에 안전하게 실행할 수 있습니다:

```bash
# 터미널 1
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob"

# 터미널 2 (동시 실행)
./gradlew :batch:bootRun --args="--spring.batch.job.name=teamReportJob"
```

## 문서

- **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)**: 프로젝트 전체 요약
- **[SPRING_BATCH_GUIDE.md](SPRING_BATCH_GUIDE.md)**: Spring Batch 구현 가이드
- **[BATCH_CONCURRENT_EXECUTION_FINAL.md](BATCH_CONCURRENT_EXECUTION_FINAL.md)**: 동시 실행 및 반복 실행 완전 가이드
- **[MULTI_MODULE_GUIDE.md](MULTI_MODULE_GUIDE.md)**: 멀티모듈 구조 가이드

## 아키텍처

### 헥사고날 아키텍처 (포트와 어댑터)

```
┌─────────────────────────────────────────────────────────┐
│                     Adapter Layer                        │
│  ┌──────────────┐                    ┌──────────────┐  │
│  │   REST API   │                    │  Persistence │  │
│  │ (Controller) │                    │    (JDBC)    │  │
│  └──────┬───────┘                    └───────┬──────┘  │
│         │                                    │          │
├─────────┼────────────────────────────────────┼─────────┤
│         │        Application Layer           │          │
│         │  ┌──────────────────────────┐      │          │
│         └─→│   Use Case (Service)     │←─────┘          │
│            │  - UserService           │                 │
│            │  - TeamService           │                 │
│            └──────────┬───────────────┘                 │
│                       │                                  │
├───────────────────────┼─────────────────────────────────┤
│                       │   Domain Layer                   │
│                       │  ┌──────────────┐               │
│                       └─→│   Entities   │               │
│                          │  - User      │               │
│                          │  - Team      │               │
│                          └──────────────┘               │
└─────────────────────────────────────────────────────────┘
```

### 멀티모듈 의존성

```
┌─────────┐     ┌─────────┐
│   API   │────→│  Core   │
└─────────┘     └─────────┘
                     ↑
┌─────────┐          │
│  Batch  │──────────┘
└─────────┘
```

- **Core**: 도메인 로직과 비즈니스 규칙 (공유)
- **API**: REST API 서버 (Core 의존)
- **Batch**: Spring Batch 작업 (Core 의존)

## 개발

### 로컬 실행

```bash
# MySQL 시작
docker run -d --name mysql-demo \
  -e MYSQL_ROOT_PASSWORD=root1234 \
  -e MYSQL_DATABASE=testdb \
  -p 3306:3306 mysql:8

# API 서버 실행
./gradlew :api:bootRun

# Batch Job 실행
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob"
```

### 테스트

```bash
./gradlew test
```

### 빌드

```bash
# 전체 빌드
./gradlew build

# 특정 모듈 빌드
./gradlew :core:build
./gradlew :api:build
./gradlew :batch:build
```

## 라이선스

MIT License

## 참고 자료

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/3.5.10/)
- [Spring Batch Documentation](https://docs.spring.io/spring-batch/reference/)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [MapStruct Documentation](https://mapstruct.org/)
- [Paketo Buildpacks](https://paketo.io/)
