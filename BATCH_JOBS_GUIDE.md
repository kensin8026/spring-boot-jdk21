# Batch Jobs Guide

> **⚠️ 주의**: 이 문서는 구 CommandLineRunner 기반 배치 구현을 설명합니다.
> 
> **현재 구현**: Spring Batch 5.1.2 프레임워크 사용
> 
> **최신 문서**:
> - `SPRING_BATCH_GUIDE.md`: Spring Batch 구현 가이드
> - `BATCH_CONCURRENT_EXECUTION_FINAL.md`: 동시 실행 및 반복 실행 가이드

---

## Overview

배치 모듈은 각 Job별로 독립적인 인스턴스로 실행됩니다. 스케줄링은 외부 서비스(Kubernetes CronJob, AWS EventBridge, Airflow 등)에서 처리합니다.

## Architecture

- **하나의 Docker 이미지**: `batch:latest`
- **여러 Job 구현**: 환경 변수로 실행할 Job 선택
- **독립 실행**: 각 Job은 실행 후 종료 (exit code 0: 성공, 1: 실패)
- **외부 스케줄링**: Kubernetes CronJob, AWS EventBridge 등 사용

## Available Jobs

### 1. User Report Job
- **Job Name**: `userReport`
- **기능**: 모든 사용자 정보를 로그로 출력
- **환경 변수**: `BATCH_JOB_NAME=userReport`

### 2. Team Report Job
- **Job Name**: `teamReport`
- **기능**: 모든 팀 정보를 로그로 출력
- **환경 변수**: `BATCH_JOB_NAME=teamReport`

## Running Jobs

### Docker Compose로 실행

#### User Report Job 실행
```bash
# 일회성 실행 (완료 후 컨테이너 자동 삭제)
docker-compose --profile batch-user-report run --rm batch-user-report

# 백그라운드 실행 (로그 확인 필요)
docker-compose --profile batch-user-report up -d
docker-compose logs batch-user-report
```

#### Team Report Job 실행
```bash
# 일회성 실행
docker-compose --profile batch-team-report run --rm batch-team-report

# 백그라운드 실행
docker-compose --profile batch-team-report up -d
docker-compose logs batch-team-report
```

### Docker로 직접 실행

```bash
# User Report Job
docker run --rm \
  --network spring_boot_jdk21_spring-network \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/testdb \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root1234 \
  -e BATCH_JOB_NAME=userReport \
  batch:latest

# Team Report Job
docker run --rm \
  --network spring_boot_jdk21_spring-network \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/testdb \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root1234 \
  -e BATCH_JOB_NAME=teamReport \
  batch:latest
```

### Kubernetes CronJob 예시

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: user-report-job
spec:
  schedule: "0 2 * * *"  # 매일 오전 2시
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: batch
            image: batch:latest
            env:
            - name: SPRING_DATASOURCE_URL
              value: "jdbc:mysql://mysql:3306/testdb"
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: mysql-secret
                  key: username
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: mysql-secret
                  key: password
            - name: BATCH_JOB_NAME
              value: "userReport"
          restartPolicy: OnFailure
---
apiVersion: batch/v1
kind: CronJob
metadata:
  name: team-report-job
spec:
  schedule: "0 3 * * *"  # 매일 오전 3시
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: batch
            image: batch:latest
            env:
            - name: SPRING_DATASOURCE_URL
              value: "jdbc:mysql://mysql:3306/testdb"
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: mysql-secret
                  key: username
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: mysql-secret
                  key: password
            - name: BATCH_JOB_NAME
              value: "teamReport"
          restartPolicy: OnFailure
```

## Adding New Jobs

### 1. Job 클래스 생성

```java
package com.kiro.jdk21.batch.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "batch.job.name", havingValue = "myNewJob")
public class MyNewJob implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("=== Starting My New Job ===");
        
        try {
            // Job 로직 구현
            
            log.info("=== My New Job Completed Successfully ===");
            System.exit(0);  // 성공
        } catch (Exception e) {
            log.error("=== My New Job Failed ===", e);
            System.exit(1);  // 실패
        }
    }
}
```

### 2. application properties 생성 (선택사항)

`batch/src/main/resources/application-myNewJob.properties`:
```properties
spring.application.name=batch-my-new-job
batch.job.name=myNewJob
```

### 3. Docker Compose 설정 추가 (선택사항)

```yaml
  batch-my-new-job:
    image: batch:latest
    container_name: spring-batch-my-new-job
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/testdb
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root1234
      BATCH_JOB_NAME: myNewJob
    networks:
      - spring-network
    profiles:
      - batch-my-new-job
    restart: "no"
```

### 4. 빌드 및 배포

```bash
# 이미지 재빌드
.\gradlew.bat :batch:bootBuildImage --imageName=batch:latest

# 실행
docker-compose --profile batch-my-new-job run --rm batch-my-new-job
```

## Job Implementation Guidelines

### 1. CommandLineRunner 사용
- Spring Boot 시작 후 자동 실행
- `run()` 메서드에 Job 로직 구현

### 2. Conditional 어노테이션
- `@ConditionalOnProperty`로 Job 선택
- `batch.job.name` 환경 변수로 제어

### 3. Exit Code
- **성공**: `System.exit(0)`
- **실패**: `System.exit(1)`
- 외부 스케줄러가 exit code로 성공/실패 판단

### 4. 로깅
- 시작/종료 로그 필수
- 에러 발생 시 상세 로그 출력
- 외부 모니터링 시스템과 연동 가능

### 5. 트랜잭션
- Core 모듈의 Service 계층 사용
- Service에서 `@Transactional` 처리

## Monitoring

### 로그 확인
```bash
# Docker Compose
docker-compose logs batch-user-report
docker-compose logs batch-team-report

# Docker
docker logs <container-id>

# Kubernetes
kubectl logs <pod-name>
```

### Exit Code 확인
```bash
# Docker
docker inspect <container-id> --format='{{.State.ExitCode}}'

# Kubernetes
kubectl get pods -l job-name=user-report-job
```

## Troubleshooting

### Job이 실행되지 않음
1. 환경 변수 `BATCH_JOB_NAME` 확인
2. Job 클래스의 `@ConditionalOnProperty` 값 확인
3. 로그에서 Spring Boot 시작 확인

### 데이터베이스 연결 실패
1. MySQL 컨테이너 상태 확인
2. 네트워크 설정 확인
3. 데이터베이스 URL, 사용자명, 비밀번호 확인

### Job이 무한 실행됨
1. `System.exit(0)` 또는 `System.exit(1)` 호출 확인
2. 무한 루프나 대기 상태 확인

## Best Practices

1. **멱등성**: 같은 Job을 여러 번 실행해도 결과가 동일하도록 구현
2. **재시도 로직**: 일시적 오류에 대한 재시도 구현
3. **타임아웃**: 장시간 실행되는 Job은 타임아웃 설정
4. **알림**: 실패 시 알림 시스템 연동 (Slack, Email 등)
5. **메트릭**: 실행 시간, 처리 건수 등 메트릭 수집
