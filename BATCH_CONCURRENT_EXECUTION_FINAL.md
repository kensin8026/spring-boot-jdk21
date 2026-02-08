# Spring Batch 동시 실행 최종 가이드

## 핵심 질문에 대한 답변

### 1. teamReportJob과 userReportJob 동시 실행 가능한가?

**✅ 예, 완전히 안전합니다.**

- 각 Job은 `@ConditionalOnProperty`로 독립적인 Configuration
- 서로 다른 Step 실행
- 데이터베이스 락 경합 없음
- Spring Batch 메타데이터 테이블의 낙관적 락으로 보호

```bash
# 터미널 1
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=1"

# 터미널 2 (동시 실행)
./gradlew :batch:bootRun --args="--spring.batch.job.name=teamReportJob timestamp(long)=1"
```

### 2. userReportJob을 5분마다 실행하면?

**⚠️ 주의: 파라미터 없이 실행하면 동일한 Instance로 인식되어 재실행되지 않습니다.**

Spring Batch는 **Job 이름 + Job 파라미터**로 Job Instance를 식별합니다:
- 동일한 파라미터 = 동일한 Instance = 이미 성공했으면 재실행 안 됨
- 다른 파라미터 = 다른 Instance = 새로 실행됨

## 해결 방법

### 방법 1: 타임스탬프 파라미터 전달 (권장)

매 실행마다 고유한 타임스탬프를 전달:

```bash
# Unix/Linux/Mac
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=$(date +%s%3N)"

# Windows PowerShell
.\gradlew.bat :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"

# Windows CMD
for /f %i in ('powershell -Command "[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()"') do gradlew.bat :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=%i"
```

### 방법 2: Cron/스케줄러에서 타임스탬프 자동 생성

#### Linux Cron
```cron
# 5분마다 실행
*/5 * * * * cd /path/to/project && ./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=$(date +%s%3N)"
```

#### Windows Task Scheduler (PowerShell 스크립트)
```powershell
# run-batch.ps1
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
& .\gradlew.bat :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=$timestamp"
```

#### Spring @Scheduled (애플리케이션 내부)
```java
@Component
@RequiredArgsConstructor
public class BatchScheduler {
    
    private final JobLauncher jobLauncher;
    private final Job userReportJob;
    
    @Scheduled(cron = "0 */5 * * * *") // 5분마다
    public void runUserReportJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        
        jobLauncher.run(userReportJob, params);
    }
}
```

### 방법 3: Kubernetes CronJob

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: user-report-job
spec:
  schedule: "*/5 * * * *"  # 5분마다
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: batch
            image: batch:latest
            command:
            - /bin/sh
            - -c
            - |
              TIMESTAMP=$(date +%s%3N)
              java -jar /app/batch.jar \
                --spring.batch.job.name=userReportJob \
                timestamp(long)=$TIMESTAMP
          restartPolicy: OnFailure
```

## 동시 실행 시나리오 정리

### ✅ 안전한 시나리오

1. **서로 다른 Job 동시 실행**
```bash
# 동시 실행 가능
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob"
./gradlew :batch:bootRun --args="--spring.batch.job.name=teamReportJob"
```

2. **동일 Job + 다른 파라미터**
```bash
# 동시 실행 가능
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=1"
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=2"
```

3. **5분마다 실행 (타임스탬프 포함)**
```bash
# 매번 새로운 Instance로 실행됨
*/5 * * * * ./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=$(date +%s%3N)"
```

### ❌ 문제가 되는 시나리오

1. **동일 Job + 동일 파라미터 (또는 파라미터 없음)**
```bash
# 첫 실행: 성공
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob"

# 두 번째 실행: 재실행 안 됨 (이미 성공한 Instance)
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob"
```

2. **5분마다 실행 (파라미터 없음)**
```bash
# 첫 실행: 성공
# 5분 후: 재실행 안 됨 (동일한 Instance)
*/5 * * * * ./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob"
```

## Spring Batch Instance 식별 원리

```
Job Instance = Job Name + Job Parameters

예시:
- userReportJob + {} = Instance A
- userReportJob + {} = Instance A (동일!)
- userReportJob + {timestamp=123} = Instance B (다름!)
- userReportJob + {timestamp=456} = Instance C (다름!)
- teamReportJob + {} = Instance D (Job 이름이 다름!)
```

## 메타데이터 테이블 확인

```sql
-- 현재 실행 중인 Job
SELECT * FROM BATCH_JOB_EXECUTION 
WHERE STATUS = 'STARTED';

-- Job Instance 확인 (파라미터 포함)
SELECT 
    ji.JOB_INSTANCE_ID,
    ji.JOB_NAME,
    je.JOB_EXECUTION_ID,
    je.STATUS,
    je.START_TIME,
    GROUP_CONCAT(CONCAT(jp.KEY_NAME, '=', jp.STRING_VAL, jp.LONG_VAL)) as PARAMETERS
FROM BATCH_JOB_INSTANCE ji
JOIN BATCH_JOB_EXECUTION je ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
LEFT JOIN BATCH_JOB_EXECUTION_PARAMS jp ON je.JOB_EXECUTION_ID = jp.JOB_EXECUTION_ID
GROUP BY ji.JOB_INSTANCE_ID, je.JOB_EXECUTION_ID
ORDER BY je.START_TIME DESC;
```

## 프로덕션 권장 사항

1. **항상 타임스탬프 파라미터 사용**
   - 반복 실행 보장
   - 실행 이력 추적 용이

2. **스케줄러 사용 시 자동 파라미터 생성**
   - Cron, Task Scheduler, Kubernetes CronJob
   - Spring @Scheduled

3. **모니터링 설정**
   - Spring Boot Actuator
   - 메타데이터 테이블 모니터링
   - 실패 알림 설정

4. **동시 실행 제한 (선택사항)**
   - 리소스 보호를 위해 최대 동시 실행 수 제한
   - Kubernetes: `concurrencyPolicy: Forbid`
   - Spring: Custom JobLauncher with Semaphore

## 결론

**✅ teamReportJob과 userReportJob 동시 실행**: 완전히 안전

**⚠️ userReportJob 5분마다 실행**: 타임스탬프 파라미터 필수
- 파라미터 없음 = 첫 실행 후 재실행 안 됨
- 타임스탬프 포함 = 매번 새로운 Instance로 실행됨

**권장 실행 방법**:
```bash
# 타임스탬프 자동 생성
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=$(date +%s%3N)"
```
