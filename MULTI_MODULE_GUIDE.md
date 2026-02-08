# Multi-Module Architecture Guide

## Overview

프로젝트가 3개의 독립적인 모듈로 구성되어 있습니다:
- **core**: 공유 도메인, 비즈니스 로직, 데이터 접근 계층
- **api**: REST API 서버 (독립 실행 가능)
- **batch**: 배치 작업 모듈 (독립 실행 가능)

## Module Structure

```
spring_boot_jdk21/
├── core/                           # 공유 코어 모듈
│   └── src/main/java/com/kiro/jdk21/core/
│       ├── domain/                 # 도메인 엔티티 (User, Team)
│       ├── application/
│       │   ├── port/in/           # Use Case 인터페이스
│       │   ├── port/out/          # Repository 포트 인터페이스
│       │   └── service/           # 비즈니스 로직 구현
│       ├── adapter/out/persistence/ # 데이터베이스 어댑터
│       └── CoreConfiguration.java  # Core 모듈 설정
│
├── api/                            # REST API 모듈
│   └── src/main/java/com/kiro/jdk21/api/
│       ├── adapter/in/web/        # REST 컨트롤러
│       └── ApiApplication.java    # API 애플리케이션 진입점
│
└── batch/                          # 배치 모듈
    └── src/main/java/com/kiro/jdk21/batch/
        ├── config/                # 배치 설정
        └── BatchApplication.java  # 배치 애플리케이션 진입점
```

## Build Commands

### 전체 프로젝트 빌드
```bash
.\gradlew.bat clean build
```

### 개별 모듈 빌드
```bash
.\gradlew.bat :core:build
.\gradlew.bat :api:build
.\gradlew.bat :batch:build
```

### Docker 이미지 생성
```bash
# API 이미지
.\gradlew.bat :api:bootBuildImage --imageName=api:latest

# Batch 이미지
.\gradlew.bat :batch:bootBuildImage --imageName=batch:latest
```

## Running the Application

### Docker Compose로 실행

#### API 서버 실행 (항상 실행)
```bash
docker-compose up -d
```

#### Batch Job 실행 (필요할 때만)
```bash
# User Report Job 실행 (일회성)
docker-compose --profile batch-user-report run --rm batch-user-report

# Team Report Job 실행 (일회성)
docker-compose --profile batch-team-report run --rm batch-team-report
```

**중요**: Batch Job은 실행 후 자동으로 종료됩니다. 항상 실행되는 서비스가 아닙니다.

#### 로그 확인
```bash
docker-compose logs api
docker-compose logs mysql

# Batch Job 로그 (실행 중일 때만)
docker-compose logs batch-user-report
docker-compose logs batch-team-report
```

#### 중지
```bash
docker-compose down
```

### 로컬에서 실행

#### API 서버
```bash
.\gradlew.bat :api:bootRun
```

#### Batch 작업
```bash
.\gradlew.bat :batch:bootRun
```

## API Endpoints

### Hello World
- **GET** `/` - "Hello World" 메시지 반환

### Users
- **GET** `/users` - 모든 사용자 조회
- **POST** `/users` - 새 사용자 생성
- **PUT** `/users/{id}` - 사용자 정보 수정
- **DELETE** `/users/{id}` - 사용자 삭제

### Teams
- **GET** `/teams` - 모든 팀 조회
- **POST** `/teams` - 새 팀 생성

### Health Check
- **GET** `/actuator/health` - 애플리케이션 상태 확인

## Batch Jobs

배치는 **필요할 때만 실행**되는 독립적인 Job들로 구성됩니다. 스케줄링은 외부 서비스(Kubernetes CronJob, AWS EventBridge 등)에서 처리합니다.

### 실행 방식

#### 1. 환경 변수로 Job 선택
```bash
# User Report Job 실행
BATCH_JOB_NAME=userReport

# Team Report Job 실행
BATCH_JOB_NAME=teamReport
```

#### 2. 실행 후 자동 종료
- Job 완료 시: `exit 0` (성공)
- Job 실패 시: `exit 1` (실패)
- 컨테이너가 자동으로 종료됨

#### 3. 외부 스케줄러가 필요할 때 실행
- **Kubernetes CronJob**: 매일 특정 시간에 실행
- **AWS EventBridge**: 이벤트 기반 실행
- **Airflow**: 복잡한 워크플로우 관리
- **수동 실행**: 필요할 때 직접 실행

### Available Jobs

#### User Report Job
- **Job Name**: `userReport`
- **기능**: 모든 사용자 정보를 로그로 출력
- **실행 예시**:
  ```bash
  docker-compose --profile batch-user-report run --rm batch-user-report
  ```

#### Team Report Job
- **Job Name**: `teamReport`
- **기능**: 모든 팀 정보를 로그로 출력
- **실행 예시**:
  ```bash
  docker-compose --profile batch-team-report run --rm batch-team-report
  ```

### 실행 흐름

```
1. 외부 스케줄러가 Job 실행 명령
   ↓
2. Docker 컨테이너 시작 (BATCH_JOB_NAME 환경 변수 설정)
   ↓
3. Spring Boot 애플리케이션 시작
   ↓
4. @ConditionalOnProperty로 해당 Job만 활성화
   ↓
5. CommandLineRunner.run() 실행
   ↓
6. Job 로직 수행
   ↓
7. System.exit(0 또는 1) 호출
   ↓
8. 컨테이너 종료
   ↓
9. 외부 스케줄러가 exit code 확인 (성공/실패)
```

## Module Dependencies

```
api → core
batch → core
```

- **core**: 다른 모듈에 의존하지 않음 (독립적)
- **api**: core 모듈에 의존
- **batch**: core 모듈에 의존

## Configuration

### Core Module (core/build.gradle)
- Spring Data JDBC
- MySQL Driver
- MapStruct (엔티티-도메인 매핑)

### API Module (api/build.gradle)
- Spring Boot Plugin 적용
- Spring Web
- Spring Boot Actuator
- Core 모듈 의존성

### Batch Module (batch/build.gradle)
- Spring Boot Plugin 적용
- Spring Boot Actuator
- Core 모듈 의존성

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    team_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Teams Table
```sql
CREATE TABLE teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Architecture Benefits

### 1. 독립적인 배포
- API와 Batch를 각각 독립적으로 배포 가능
- 각 모듈은 자체 Docker 이미지로 빌드됨

### 2. 코드 재사용
- Core 모듈의 도메인 로직과 데이터 접근 계층을 API와 Batch에서 공유
- 중복 코드 제거

### 3. 확장성
- 새로운 모듈 추가 용이 (예: admin, worker 등)
- 각 모듈을 독립적으로 스케일링 가능

### 4. 유지보수성
- 명확한 책임 분리
- 모듈 간 의존성이 명확함

## Hexagonal Architecture

프로젝트는 헥사고날 아키텍처(포트와 어댑터 패턴)를 따릅니다:

- **Domain**: 비즈니스 로직의 핵심 (User, Team)
- **Application**: Use Case와 비즈니스 서비스
- **Ports**: 인터페이스 정의 (in: Use Cases, out: Repository)
- **Adapters**: 
  - In: REST 컨트롤러 (api 모듈)
  - Out: JDBC 리포지토리 (core 모듈)

## Testing

### 단위 테스트
```bash
.\gradlew.bat test
```

### 특정 모듈 테스트
```bash
.\gradlew.bat :core:test
.\gradlew.bat :api:test
.\gradlew.bat :batch:test
```

## Troubleshooting

### 빌드 실패 시
1. 기존 빌드 아티팩트 정리: `.\gradlew.bat clean`
2. Gradle 캐시 새로고침: `.\gradlew.bat --refresh-dependencies`

### Docker 이미지 빌드 실패 시
1. Docker Desktop이 실행 중인지 확인
2. 기존 이미지 삭제: `docker rmi api:latest batch:latest`
3. 다시 빌드

### 데이터베이스 연결 실패 시
1. MySQL 컨테이너 상태 확인: `docker-compose ps`
2. MySQL 로그 확인: `docker-compose logs mysql`
3. 컨테이너 재시작: `docker-compose restart mysql`
