# Batch Job 실행 예시

> **⚠️ 주의**: 이 문서는 구 CommandLineRunner 기반 배치 실행을 설명합니다.
> 
> **현재 구현**: Spring Batch 5.1.2 프레임워크 사용
> 
> **최신 문서**:
> - `SPRING_BATCH_GUIDE.md`: Spring Batch 구현 가이드
> - `BATCH_CONCURRENT_EXECUTION_FINAL.md`: 동시 실행 및 반복 실행 가이드

---

## 개요

배치 Job은 **필요할 때만 실행**되는 독립적인 프로세스입니다. 각 Job은 실행 후 자동으로 종료되며, 외부 스케줄러가 필요한 시점에 실행합니다.

## 핵심 개념

### 1. 하나의 이미지, 여러 Job
```
batch:latest 이미지
├── UserReportJob (BATCH_JOB_NAME=userReport)
├── TeamReportJob (BATCH_JOB_NAME=teamReport)
└── 새로운 Job 추가 가능...
```

### 2. 환경 변수로 Job 선택
```bash
# 이 환경 변수 값에 따라 실행될 Job이 결정됨
BATCH_JOB_NAME=userReport  # UserReportJob 실행
BATCH_JOB_NAME=teamReport  # TeamReportJob 실행
```

### 3. 실행 후 자동 종료
```java
@Component
@ConditionalOnProperty(name = "batch.job.name", havingValue = "userReport")
public class UserReportJob implements CommandLineRunner {
    @Override
    public void run(String... args) {
        // Job 로직 실행
        System.exit(0);  // 완료 후 프로세스 종료
    }
}
```

## 실행 방법

### 방법 1: Docker Compose (로컬 테스트)

#### User Report Job 실행
```bash
# 일회성 실행 (완료 후 컨테이너 자동 삭제)
docker-compose --profile batch-user-report run --rm batch-user-report

# 실행 결과
# === Starting User Report Job ===
# Total users: 10
# User: 김철수 - kim@example.com (Team ID: 1)
# User: 이영희 - lee@example.com (Team ID: 1)
# ...
# === User Report Job Completed Successfully ===
# (컨테이너 자동 종료)
```

#### Team Report Job 실행
```bash
docker-compose --profile batch-team-report run --rm batch-team-report

# 실행 결과
# === Starting Team Report Job ===
# Total teams: 3
# Team: 개발팀 - Software Development Team
# Team: 디자인팀 - UI/UX Design Team
# ...
# === Team Report Job Completed Successfully ===
# (컨테이너 자동 종료)
```

### 방법 2: Docker 직접 실행

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

### 방법 3: Kubernetes CronJob (프로덕션)

#### 매일 오전 2시에 User Report 실행
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
            image: your-registry/batch:latest
            env:
            - name: BATCH_JOB_NAME
              value: "userReport"
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
          restartPolicy: OnFailure
```

#### 매주 월요일 오전 3시에 Team Report 실행
```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: team-report-weekly
spec:
  schedule: "0 3 * * 1"  # 매주 월요일 오전 3시
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: batch
            image: your-registry/batch:latest
            env:
            - name: BATCH_JOB_NAME
              value: "teamReport"
            # ... 데이터베이스 설정
          restartPolicy: OnFailure
```

#### 수동 실행 (필요할 때)
```bash
# Kubernetes Job으로 즉시 실행
kubectl create job user-report-manual-$(date +%s) \
  --from=cronjob/user-report-daily

# 실행 상태 확인
kubectl get jobs
kubectl logs job/user-report-manual-1234567890
```

### 방법 4: AWS EventBridge + ECS (프로덕션)

#### EventBridge Rule 생성
```json
{
  "ScheduleExpression": "cron(0 2 * * ? *)",
  "State": "ENABLED",
  "Targets": [
    {
      "Arn": "arn:aws:ecs:region:account:cluster/my-cluster",
      "RoleArn": "arn:aws:iam::account:role/ecsEventsRole",
      "EcsParameters": {
        "TaskDefinitionArn": "arn:aws:ecs:region:account:task-definition/batch-job",
        "TaskCount": 1,
        "LaunchType": "FARGATE",
        "NetworkConfiguration": {
          "awsvpcConfiguration": {
            "Subnets": ["subnet-xxx"],
            "SecurityGroups": ["sg-xxx"],
            "AssignPublicIp": "ENABLED"
          }
        }
      },
      "Input": "{\"containerOverrides\":[{\"name\":\"batch\",\"environment\":[{\"name\":\"BATCH_JOB_NAME\",\"value\":\"userReport\"}]}]}"
    }
  ]
}
```

### 방법 5: Airflow DAG (복잡한 워크플로우)

```python
from airflow import DAG
from airflow.providers.docker.operators.docker import DockerOperator
from datetime import datetime, timedelta

default_args = {
    'owner': 'data-team',
    'depends_on_past': False,
    'start_date': datetime(2026, 1, 1),
    'email_on_failure': True,
    'email_on_retry': False,
    'retries': 1,
    'retry_delay': timedelta(minutes=5),
}

dag = DAG(
    'daily_reports',
    default_args=default_args,
    description='Daily user and team reports',
    schedule_interval='0 2 * * *',  # 매일 오전 2시
    catchup=False,
)

# User Report Job
user_report = DockerOperator(
    task_id='user_report',
    image='batch:latest',
    api_version='auto',
    auto_remove=True,
    environment={
        'BATCH_JOB_NAME': 'userReport',
        'SPRING_DATASOURCE_URL': 'jdbc:mysql://mysql:3306/testdb',
        'SPRING_DATASOURCE_USERNAME': 'root',
        'SPRING_DATASOURCE_PASSWORD': 'root1234',
    },
    network_mode='spring_boot_jdk21_spring-network',
    dag=dag,
)

# Team Report Job (User Report 완료 후 실행)
team_report = DockerOperator(
    task_id='team_report',
    image='batch:latest',
    api_version='auto',
    auto_remove=True,
    environment={
        'BATCH_JOB_NAME': 'teamReport',
        'SPRING_DATASOURCE_URL': 'jdbc:mysql://mysql:3306/testdb',
        'SPRING_DATASOURCE_USERNAME': 'root',
        'SPRING_DATASOURCE_PASSWORD': 'root1234',
    },
    network_mode='spring_boot_jdk21_spring-network',
    dag=dag,
)

# 실행 순서 정의
user_report >> team_report
```

## 실행 흐름 상세

### 1. Job 시작
```
외부 스케줄러 → Docker 컨테이너 시작
                ↓
                환경 변수 설정:
                - BATCH_JOB_NAME=userReport
                - SPRING_DATASOURCE_URL=...
                - SPRING_DATASOURCE_USERNAME=...
                - SPRING_DATASOURCE_PASSWORD=...
```

### 2. Spring Boot 시작
```
Spring Boot 애플리케이션 시작
↓
@ConditionalOnProperty 평가
↓
BATCH_JOB_NAME=userReport이므로
UserReportJob만 Bean으로 등록
(TeamReportJob은 등록되지 않음)
```

### 3. Job 실행
```
CommandLineRunner.run() 호출
↓
UserReportJob.run() 실행
↓
데이터베이스에서 사용자 조회
↓
로그 출력
↓
System.exit(0) 호출
```

### 4. 종료 및 결과 확인
```
컨테이너 종료 (exit code: 0)
↓
외부 스케줄러가 exit code 확인
↓
0이면 성공, 1이면 실패로 판단
↓
필요시 알림 발송 (Slack, Email 등)
```

## 장점

### 1. 독립적 실행
```bash
# 각 Job을 독립적으로 실행 가능
docker run ... -e BATCH_JOB_NAME=userReport batch:latest
docker run ... -e BATCH_JOB_NAME=teamReport batch:latest

# 동시에 여러 Job 실행 가능 (서로 영향 없음)
```

### 2. 리소스 효율성
```
Job 실행 중: 메모리 사용
Job 종료 후: 메모리 해제
↓
항상 실행되는 스케줄러보다 리소스 효율적
```

### 3. 스케일링 용이
```yaml
# Kubernetes에서 동시 실행 제한
spec:
  concurrencyPolicy: Forbid  # 이전 Job이 완료될 때까지 대기
  # 또는
  concurrencyPolicy: Replace  # 이전 Job 종료하고 새로 시작
```

### 4. 실패 처리
```yaml
# Kubernetes 자동 재시도
spec:
  backoffLimit: 3  # 최대 3번 재시도
  restartPolicy: OnFailure
```

## 모니터링

### Exit Code 확인
```bash
# Docker
docker inspect <container-id> --format='{{.State.ExitCode}}'

# Kubernetes
kubectl get jobs
kubectl describe job user-report-manual-1234567890
```

### 로그 확인
```bash
# Docker Compose
docker-compose logs batch-user-report

# Kubernetes
kubectl logs job/user-report-manual-1234567890

# AWS CloudWatch (ECS)
aws logs tail /ecs/batch-job --follow
```

### 알림 설정 (Kubernetes 예시)
```yaml
# Slack 알림을 위한 Job
apiVersion: batch/v1
kind: Job
metadata:
  name: notify-on-failure
spec:
  template:
    spec:
      containers:
      - name: slack-notifier
        image: curlimages/curl
        command:
        - sh
        - -c
        - |
          curl -X POST https://hooks.slack.com/services/YOUR/WEBHOOK/URL \
            -H 'Content-Type: application/json' \
            -d '{"text":"User Report Job Failed!"}'
      restartPolicy: Never
```

## 요약

**핵심 포인트:**
1. 각 Job은 **필요할 때만** 실행됩니다
2. 실행 후 **자동으로 종료**됩니다
3. **환경 변수**로 어떤 Job을 실행할지 결정합니다
4. **외부 스케줄러**가 언제 실행할지 결정합니다
5. **Exit code**로 성공/실패를 판단합니다

이 구조는 클라우드 네이티브 환경에서 배치 Job을 실행하는 표준 패턴입니다.
