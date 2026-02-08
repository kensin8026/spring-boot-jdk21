# Spring Boot JDK21 프로젝트 작업 요약

## 프로젝트 개요
Spring Boot 3.5.10과 Java 21을 사용한 헥사고날 아키텍처 기반 멀티모듈 데모 애플리케이션

## 주요 작업 내역

### 1. 프로젝트 초기 설정
- **기술 스택**
  - Java 21 (JDK)
  - Spring Boot 3.5.10
  - Gradle 빌드 시스템
  - Spring Modulith 1.3.0
  - Spring Cloud 2024.0.0
  - Spring Batch 5.1.2

- **Steering 문서 생성**
  - `product.md`: 제품 개요
  - `tech.md`: 기술 스택 및 명령어
  - `structure.md`: 프로젝트 구조

### 2. 기본 기능 구현
- **Hello World API**
  - `GET /` - 간단한 텍스트 응답

- **MySQL 8 Docker 연동**
  - Docker 컨테이너로 MySQL 실행
  - Spring Data JDBC 설정
  - H2 인메모리 DB에서 MySQL로 전환

### 3. 헥사고날 아키텍처 적용

#### 패키지 구조
```
com.kiro.jdk21.spring_boot_jdk21/
├── domain/                    # 도메인 레이어
│   ├── User.java
│   └── Team.java
├── application/               # 애플리케이션 레이어
│   ├── port/
│   │   ├── in/               # 인바운드 포트 (Use Cases)
│   │   │   ├── UserUseCase.java
│   │   │   └── TeamUseCase.java
│   │   └── out/              # 아웃바운드 포트
│   │       ├── UserPort.java
│   │       └── TeamPort.java
│   └── service/              # 서비스 구현
│       ├── UserService.java
│       └── TeamService.java
└── adapter/                   # 어댑터 레이어
    ├── in/web/               # REST API
    │   ├── HelloController.java
    │   ├── UserController.java
    │   └── TeamController.java
    └── out/persistence/      # 데이터베이스
        ├── UserEntity.java
        ├── UserJdbcRepository.java
        ├── UserMapper.java
        ├── UserPersistenceAdapter.java
        ├── TeamEntity.java
        ├── TeamJdbcRepository.java
        ├── TeamMapper.java
        └── TeamPersistenceAdapter.java
```

#### 아키텍처 리팩토링
- **Before**: 기능별로 세분화된 12개의 포트 인터페이스
- **After**: 도메인별로 통합된 4개의 포트 인터페이스
  - `UserUseCase`, `TeamUseCase` (인바운드)
  - `UserPort`, `TeamPort` (아웃바운드)
- **효과**: 67% 파일 감소, 의존성 단순화, 가독성 향상

### 4. 도메인 모델 구현

#### User 엔티티
- 필드: id, name, email, teamId, createdAt
- Team과 Many-to-One 관계

#### Team 엔티티
- 필드: id, name, description, createdAt

#### 데이터베이스 스키마
```sql
CREATE TABLE teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    team_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL
);
```

### 5. REST API 구현

#### User API
- `GET /users` - 사용자 목록 조회
- `POST /users` - 사용자 생성
- `PUT /users/{id}` - 사용자 수정
- `DELETE /users/{id}` - 사용자 삭제

#### Team API
- `GET /teams` - 팀 목록 조회
- `POST /teams` - 팀 생성

#### 기타
- `GET /` - Hello World
- `GET /actuator` - Spring Boot Actuator
- `GET /actuator/health` - Health Check

### 6. MapStruct 적용
- **목적**: Entity ↔ Domain 자동 매핑
- **의존성**
  - `org.mapstruct:mapstruct:1.6.3`
  - `org.mapstruct:mapstruct-processor:1.6.3`
  - `lombok-mapstruct-binding:0.2.0`
- **효과**: 보일러플레이트 코드 제거, 타입 안전성, 성능 향상

### 7. Docker 배포

#### Docker Compose 구성
```yaml
services:
  mysql:    # MySQL 8 데이터베이스
  app:      # Spring Boot 애플리케이션
```

#### Docker 이미지 빌드
- Paketo Buildpacks 사용
- Cloud Native Buildpacks 기반
- 자동 JVM 메모리 최적화
- 레이어 캐싱으로 빠른 재빌드

#### 배포 명령어
```bash
# 이미지 빌드
.\gradlew.bat bootBuildImage --imageName=spring-boot-jdk21:latest

# 전체 스택 실행
docker-compose up -d

# 로그 확인
docker logs spring-app

# 중지
docker-compose down
```

### 8. 멀티모듈 구조 전환
- **모듈 구성**
  - `core`: 공유 도메인 및 비즈니스 로직
  - `api`: REST API 서버 (독립 실행)
  - `batch`: Spring Batch 작업 (독립 실행)
- **헥사고날 아키텍처 유지**
  - 포트와 어댑터 패턴
  - 모듈 간 의존성 명확화

### 9. Spring Batch 구현
- **Spring Batch 5.1.2 적용**
  - Job, Step, ItemReader, ItemProcessor, ItemWriter
  - Chunk 기반 처리 (chunk size: 10)
  - 메타데이터 테이블 자동 생성
- **구현된 Job**
  - `userReportJob`: 사용자 리포트 생성
  - `teamReportJob`: 팀 리포트 생성
- **Job 격리**
  - `@ConditionalOnProperty`로 Job별 독립 실행
  - 동시 실행 안전성 보장
- **반복 실행 지원**
  - 타임스탬프 파라미터로 Job Instance 구분
  - 스케줄러 통합 가이드 제공

### 10. MCP 서버 설정
- **CONTEXT7 MCP**: Docker 기반 코드 컨텍스트 분석
- **Playwright MCP**: 브라우저 자동화 테스트

## 초기 데이터

### Teams
1. 개발팀 (Software Development Team)
2. 디자인팀 (UI/UX Design Team)
3. 마케팅팀 (Marketing Team)

### Users
1. 김철수 (kim@example.com) - 개발팀
2. 이영희 (lee@example.com) - 개발팀
3. 박민수 (park@example.com) - 디자인팀
4. 최지은 (choi@example.com) - 디자인팀
5. 정다은 (jung@example.com) - 마케팅팀

## 주요 기술 결정

### 1. 헥사고날 아키텍처
- **선택 이유**: 도메인 로직과 인프라 분리, 테스트 용이성
- **구현 방식**: 포트와 어댑터 패턴
- **리팩토링**: 과도한 세분화 → 도메인별 통합

### 2. MapStruct
- **선택 이유**: 컴파일 타임 매핑, 타입 안전성, 성능
- **대안**: 수동 매핑 (간단한 경우), ModelMapper (런타임)

### 3. Docker 배포
- **선택 이유**: 환경 일관성, 쉬운 배포, 격리
- **빌드팩**: Paketo Buildpacks (Cloud Native)

### 4. Spring Data JDBC
- **선택 이유**: 단순성, 명시적 제어
- **대안**: JPA/Hibernate (복잡한 관계), MyBatis (SQL 제어)

## 테스트 결과

### API 테스트 (Playwright)
- ✅ `GET /` - Hello World 정상
- ✅ `GET /users` - 5명 사용자 조회 성공
- ✅ `GET /teams` - 3개 팀 조회 성공
- ✅ `GET /actuator/health` - Status: UP

### Docker 배포 테스트
- ✅ MySQL 컨테이너 정상 실행
- ✅ Spring Boot 앱 8.4초 만에 시작
- ✅ 모든 API 엔드포인트 정상 작동

## 프로젝트 실행 방법

### 로컬 실행
```bash
# MySQL 시작 (Docker)
docker run -d --name mysql-demo \
  -e MYSQL_ROOT_PASSWORD=root1234 \
  -e MYSQL_DATABASE=testdb \
  -p 3306:3306 mysql:8

# 애플리케이션 실행
.\gradlew.bat bootRun
```

### Docker Compose 실행
```bash
# 전체 스택 시작
docker-compose up -d

# 로그 확인
docker logs -f spring-app

# 중지
docker-compose down
```

### 접속 URL
- 애플리케이션: http://localhost:8080
- Actuator: http://localhost:8080/actuator
- Health Check: http://localhost:8080/actuator/health

## 향후 개선 사항

### 1. 테스트 코드
- 단위 테스트 (JUnit 5)
- 통합 테스트 (Testcontainers)
- API 테스트 (RestAssured)

### 2. 보안
- Spring Security 적용
- JWT 인증/인가
- CORS 설정

### 3. 문서화
- Swagger/OpenAPI 3.0
- API 문서 자동 생성
- 아키텍처 다이어그램

### 4. 모니터링
- Prometheus + Grafana
- 로그 집계 (ELK Stack)
- 분산 추적 (Zipkin)

## 학습 포인트

1. **헥사고날 아키텍처**: 실용적인 적용 방법
2. **멀티모듈 구조**: 도메인 분리와 독립 실행
3. **Spring Batch**: Job, Step, Chunk 기반 처리
4. **동시 실행 안전성**: Job Instance 관리와 메타데이터
5. **포트 설계**: 과도한 세분화 vs 적절한 통합
6. **MapStruct**: 컴파일 타임 매핑의 장점
7. **Docker**: Cloud Native Buildpacks 활용

## 참고 자료

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/3.5.10/)
- [Spring Batch Documentation](https://docs.spring.io/spring-batch/reference/)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [MapStruct Documentation](https://mapstruct.org/)
- [Paketo Buildpacks](https://paketo.io/)
- [Spring Modulith](https://docs.spring.io/spring-modulith/reference/)
