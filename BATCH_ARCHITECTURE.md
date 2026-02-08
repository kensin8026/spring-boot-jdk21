# Batch Architecture 상세 설명

> **⚠️ 주의**: 이 문서는 구 CommandLineRunner 기반 배치 아키텍처를 설명합니다.
> 
> **현재 구현**: Spring Batch 5.1.2 프레임워크 사용
> 
> **최신 문서**:
> - `SPRING_BATCH_GUIDE.md`: Spring Batch 구현 가이드
> - `BATCH_CONCURRENT_EXECUTION_FINAL.md`: 동시 실행 및 반복 실행 가이드

---

## 핵심 개념: "필요할 때만 실행"

### 기존 방식 (스케줄러 내장) ❌
```
┌─────────────────────────────┐
│   Batch 컨테이너 (항상 실행)   │
│                             │
│  ┌──────────────────────┐  │
│  │  @Scheduled          │  │
│  │  매 60초마다 실행     │  │
│  │                      │  │
│  │  while(true) {       │  │
│  │    sleep(60초)       │  │
│  │    job.run()         │  │
│  │  }                   │  │
│  └──────────────────────┘  │
│                             │
│  메모리 계속 사용 중...       │
└─────────────────────────────┘
```
**문제점:**
- 항상 실행 중이므로 메모리 낭비
- 스케줄 변경 시 재배포 필요
- 여러 Job 관리 복잡

---

### 새로운 방식 (외부 스케줄러) ✅
```
┌──────────────────────────────────────────────────────────┐
│              외부 스케줄러 (Kubernetes CronJob)              │
│                                                            │
│  매일 오전 2시: User Report 실행                            │
│  매주 월요일:   Team Report 실행                            │
│  필요할 때:     수동 실행                                   │
└──────────────────────────────────────────────────────────┘
                        │
                        │ 실행 명령
                        ↓
┌─────────────────────────────────────────────────────────┐
│                  Docker 컨테이너 시작                      │
│                                                           │
│  환경 변수: BATCH_JOB_NAME=userReport                     │
│                                                           │
│  ┌────────────────────────────────────────────────┐     │
│  │  Spring Boot 시작                               │     │
│  │                                                 │     │
│  │  @ConditionalOnProperty 평가                    │     │
│  │  → UserReportJob만 활성화                       │     │
│  │                                                 │     │
│  │  CommandLineRunner.run() 실행                   │     │
│  │  ├─ 데이터 조회                                 │     │
│  │  ├─ 로그 출력                                   │     │
│  │  └─ System.exit(0)  ← 여기서 종료!             │     │
│  └────────────────────────────────────────────────┘     │
│                                                           │
│  컨테이너 종료 (메모리 해제)                               │
└─────────────────────────────────────────────────────────┘
                        │
                        │ exit code 반환
                        ↓
┌─────────────────────────────────────────────────────────┐
│              외부 스케줄러가 결과 확인                      │
│                                                           │
│  exit code 0 → 성공 ✓                                    │
│  exit code 1 → 실패 ✗ (재시도 또는 알림)                  │
└─────────────────────────────────────────────────────────┘
```

**장점:**
- Job 실행 중에만 메모리 사용
- 스케줄 변경 시 외부 설정만 수정
- 각 Job 독립적으로 관리

---

## 실행 시나리오

### 시나리오 1: 매일 오전 2시 User Report

```
02:00:00  Kubernetes CronJob 트리거
          ↓
02:00:01  Docker 컨테이너 시작
          - 이미지: batch:latest
          - 환경 변수: BATCH_JOB_NAME=userReport
          ↓
02:00:05  Spring Boot 시작 완료
          ↓
02:00:06  UserReportJob.run() 실행
          - 데이터베이스 연결
          - 사용자 10명 조회
          - 로그 출력
          ↓
02:00:08  System.exit(0) 호출
          ↓
02:00:09  컨테이너 종료
          - 메모리 해제
          - 리소스 반환
          ↓
02:00:10  Kubernetes가 exit code 확인
          - 0이므로 성공으로 기록
```

**총 실행 시간: 약 10초**
**메모리 사용: 10초 동안만**

---

### 시나리오 2: 동시에 여러 Job 실행

```
┌─────────────────────┐     ┌─────────────────────┐
│  User Report Job    │     │  Team Report Job    │
│                     │     │                     │
│  컨테이너 1          │     │  컨테이너 2          │
│  BATCH_JOB_NAME=    │     │  BATCH_JOB_NAME=    │
│  userReport         │     │  teamReport         │
│                     │     │                     │
│  독립적으로 실행     │     │  독립적으로 실행     │
│  서로 영향 없음      │     │  서로 영향 없음      │
└─────────────────────┘     └─────────────────────┘
```

---

## 코드 동작 원리

### 1. Job 선택 메커니즘

```java
// UserReportJob.java
@Component
@ConditionalOnProperty(name = "batch.job.name", havingValue = "userReport")
public class UserReportJob implements CommandLineRunner {
    // BATCH_JOB_NAME=userReport일 때만 이 Bean이 생성됨
}

// TeamReportJob.java
@Component
@ConditionalOnProperty(name = "batch.job.name", havingValue = "teamReport")
public class TeamReportJob implements CommandLineRunner {
    // BATCH_JOB_NAME=teamReport일 때만 이 Bean이 생성됨
}
```

**실행 시:**
```
환경 변수: BATCH_JOB_NAME=userReport

Spring Boot 시작
↓
@ConditionalOnProperty 평가
↓
UserReportJob: havingValue="userReport" → 일치! → Bean 생성 ✓
TeamReportJob: havingValue="teamReport" → 불일치 → Bean 생성 안 함 ✗
↓
UserReportJob만 실행됨
```

### 2. 자동 종료 메커니즘

```java
@Override
public void run(String... args) {
    log.info("=== Starting User Report Job ===");
    
    try {
        // Job 로직 실행
        List<User> users = userUseCase.getUsers();
        users.forEach(user -> log.info("User: {}", user));
        
        log.info("=== Job Completed Successfully ===");
        System.exit(0);  // ← 성공 시 프로세스 종료
        
    } catch (Exception e) {
        log.error("=== Job Failed ===", e);
        System.exit(1);  // ← 실패 시 프로세스 종료
    }
}
```

**System.exit()의 역할:**
- JVM 프로세스를 완전히 종료
- Docker 컨테이너도 함께 종료
- Exit code를 외부 스케줄러에 전달

---

## 외부 스케줄러 예시

### Kubernetes CronJob

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: user-report-daily
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
            - name: BATCH_JOB_NAME
              value: "userReport"  # ← 이 값으로 Job 선택
          restartPolicy: OnFailure  # 실패 시 재시도
```

**동작:**
1. 매일 오전 2시에 Pod 생성
2. 컨테이너 시작 (BATCH_JOB_NAME=userReport)
3. Job 실행
4. 완료 후 Pod 종료
5. 다음 날 오전 2시까지 대기

### AWS EventBridge + ECS

```json
{
  "ScheduleExpression": "cron(0 2 * * ? *)",
  "Targets": [{
    "EcsParameters": {
      "TaskDefinitionArn": "...",
      "LaunchType": "FARGATE"
    },
    "Input": {
      "containerOverrides": [{
        "environment": [{
          "name": "BATCH_JOB_NAME",
          "value": "userReport"  # ← 이 값으로 Job 선택
        }]
      }]
    }
  }]
}
```

**동작:**
1. 매일 오전 2시에 ECS Task 시작
2. Fargate 컨테이너 실행
3. Job 실행
4. 완료 후 Task 종료
5. 사용한 만큼만 과금

---

## 실제 사용 예시

### 로컬 개발 환경

```bash
# 1. MySQL과 API 서버 시작 (항상 실행)
docker-compose up -d

# 2. 필요할 때 User Report 실행
docker-compose --profile batch-user-report run --rm batch-user-report

# 출력:
# === Starting User Report Job ===
# Total users: 10
# User: 김철수 - kim@example.com
# ...
# === Job Completed Successfully ===
# (자동 종료)

# 3. 필요할 때 Team Report 실행
docker-compose --profile batch-team-report run --rm batch-team-report

# 출력:
# === Starting Team Report Job ===
# Total teams: 3
# Team: 개발팀 - Software Development Team
# ...
# === Job Completed Successfully ===
# (자동 종료)
```

### 프로덕션 환경 (Kubernetes)

```bash
# 1. CronJob 배포
kubectl apply -f user-report-cronjob.yaml
kubectl apply -f team-report-cronjob.yaml

# 2. 자동 실행 (스케줄에 따라)
# - User Report: 매일 오전 2시
# - Team Report: 매주 월요일 오전 3시

# 3. 수동 실행 (필요할 때)
kubectl create job user-report-manual-$(date +%s) \
  --from=cronjob/user-report-daily

# 4. 실행 상태 확인
kubectl get jobs
kubectl logs job/user-report-manual-1234567890

# 5. 완료된 Job 정리 (자동)
# Kubernetes가 완료된 Job을 자동으로 정리
```

---

## 비교: 항상 실행 vs 필요할 때만 실행

### 항상 실행 (기존 방식)
```
시간:  00:00  01:00  02:00  03:00  04:00  05:00
메모리: ████  ████  ████  ████  ████  ████
실행:   Job   Job   Job   Job   Job   Job
비용:   $$$   $$$   $$$   $$$   $$$   $$$

총 비용: $$$$$$ (24시간 × 365일)
```

### 필요할 때만 실행 (새로운 방식)
```
시간:  00:00  01:00  02:00  03:00  04:00  05:00
메모리: ----  ----  ██--  ----  ----  ----
실행:   -     -     Job   -     -     -
비용:   -     -     $     -     -     -

총 비용: $ (실행 시간만 과금)
```

**절감 효과:**
- 메모리: 약 95% 절감
- 비용: 약 95% 절감
- 관리: 외부 스케줄러로 중앙 관리

---

## 요약

### 핵심 포인트

1. **필요할 때만 실행**
   - 스케줄러가 정한 시간에만 실행
   - 또는 수동으로 필요할 때 실행

2. **자동 종료**
   - Job 완료 후 `System.exit(0)` 호출
   - 컨테이너 자동 종료
   - 메모리 즉시 해제

3. **환경 변수로 Job 선택**
   - `BATCH_JOB_NAME=userReport` → UserReportJob 실행
   - `BATCH_JOB_NAME=teamReport` → TeamReportJob 실행

4. **외부 스케줄러 사용**
   - Kubernetes CronJob
   - AWS EventBridge
   - Airflow
   - 수동 실행

5. **독립적 실행**
   - 각 Job은 독립적인 컨테이너
   - 동시 실행 가능
   - 서로 영향 없음

이 구조는 **클라우드 네이티브 배치 처리의 표준 패턴**입니다!
