# Spring Batch 가이드

## 개요

Spring Batch 5.1.2 프레임워크를 사용하여 배치 작업을 구현했습니다. Spring Boot 3.5.10과 완벽하게 호환되며, Job, Step, ItemReader, ItemProcessor, ItemWriter 등의 구성요소를 제공합니다.

## 주요 특징

- **Chunk 기반 처리**: 대용량 데이터를 효율적으로 처리
- **트랜잭션 관리**: Chunk 단위 자동 트랜잭션
- **Job Instance 관리**: Job 이름 + 파라미터로 고유 식별
- **메타데이터 추적**: 실행 이력 및 상태 관리
- **동시 실행 안전성**: 서로 다른 Job 동시 실행 가능

## 아키텍처

### 주요 구성요소

1. **Job**: 배치 작업의 최상위 단위
   - `userReportJob`: 사용자 리포트 생성
   - `teamReportJob`: 팀 리포트 생성

2. **Step**: Job을 구성하는 독립적인 단계
   - `userReportStep`: 사용자 데이터 처리
   - `teamReportStep`: 팀 데이터 처리

3. **ItemReader**: 데이터 읽기
   - `ListItemReader`를 사용하여 UseCase에서 데이터 조회

4. **ItemProcessor**: 데이터 가공/변환
   - 로깅 및 비즈니스 로직 처리

5. **ItemWriter**: 데이터 쓰기
   - 리포트 출력 (로그)

### Chunk 처리

- Chunk size: 10
- 트랜잭션 단위로 데이터 처리
- 대용량 데이터 처리 시 메모리 효율적

## 실행 방법

### 1. Gradle로 실행 (단일 실행)

```bash
# User Report Job 실행
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob"

# Team Report Job 실행
./gradlew :batch:bootRun --args="--spring.batch.job.name=teamReportJob"
```

Windows PowerShell:
```powershell
.\gradlew.bat :batch:bootRun --args="--spring.batch.job.name=userReportJob"
.\gradlew.bat :batch:bootRun --args="--spring.batch.job.name=teamReportJob"
```

### 2. 반복 실행 (타임스탬프 파라미터 필수)

동일한 Job을 여러 번 실행하려면 타임스탬프 파라미터가 필요합니다:

```bash
# Unix/Linux/Mac
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=$(date +%s%3N)"

# Windows PowerShell
.\gradlew.bat :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
```

**이유**: Spring Batch는 `Job 이름 + Job 파라미터`로 Job Instance를 식별합니다. 동일한 파라미터로 이미 성공한 Job은 재실행되지 않습니다.

### 3. JAR 파일로 실행

```bash
# 빌드
./gradlew :batch:bootJar

# 단일 실행
java -jar batch/build/libs/batch.jar --spring.batch.job.name=userReportJob

# 반복 실행 (타임스탬프 포함)
java -jar batch/build/libs/batch.jar \
  --spring.batch.job.name=userReportJob \
  timestamp=$(date +%s%3N)
```

## Spring Batch 메타데이터

Spring Batch는 실행 이력을 데이터베이스에 저장합니다:

- `BATCH_JOB_INSTANCE`: Job 인스턴스 정보 (Job 이름 + 파라미터로 고유 식별)
- `BATCH_JOB_EXECUTION`: Job 실행 이력 (시작/종료 시간, 상태)
- `BATCH_STEP_EXECUTION`: Step 실행 이력 (읽기/쓰기 건수, 커밋 횟수)
- `BATCH_JOB_EXECUTION_PARAMS`: Job 파라미터 (timestamp 등)
- `BATCH_JOB_EXECUTION_CONTEXT`: Job 실행 컨텍스트
- `BATCH_STEP_EXECUTION_CONTEXT`: Step 실행 컨텍스트

### Job Instance 식별 원리

```
Job Instance = Job 이름 + Job 파라미터

예시:
- userReportJob + {} = Instance A
- userReportJob + {} = Instance A (동일! 재실행 안 됨)
- userReportJob + {timestamp=123} = Instance B (다름! 새로 실행)
- userReportJob + {timestamp=456} = Instance C (다름! 새로 실행)
```

### 메타데이터 조회

```sql
-- Job Instance 및 실행 이력 확인
SELECT 
    ji.JOB_INSTANCE_ID,
    ji.JOB_NAME,
    je.JOB_EXECUTION_ID,
    je.STATUS,
    je.START_TIME,
    je.END_TIME,
    GROUP_CONCAT(CONCAT(jp.KEY_NAME, '=', COALESCE(jp.STRING_VAL, jp.LONG_VAL))) as PARAMETERS
FROM BATCH_JOB_INSTANCE ji
JOIN BATCH_JOB_EXECUTION je ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
LEFT JOIN BATCH_JOB_EXECUTION_PARAMS jp ON je.JOB_EXECUTION_ID = jp.JOB_EXECUTION_ID
GROUP BY ji.JOB_INSTANCE_ID, je.JOB_EXECUTION_ID
ORDER BY je.START_TIME DESC;
```

이를 통해:
- **재실행 방지**: 동일 파라미터로 성공한 Job은 재실행 안 됨
- **실행 이력 추적**: 모든 실행 기록 보관
- **실패 시 재시작**: 실패 지점부터 재시작 가능

## 설정

### application.properties

```properties
spring.application.name=batch

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/testdb
spring.datasource.username=root
spring.datasource.password=root1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Spring Batch Configuration
spring.batch.job.enabled=true
spring.batch.jdbc.initialize-schema=always

# Job Configuration (set via command line)
# --spring.batch.job.name=userReportJob or teamReportJob
# 
# 동일 Job 반복 실행 시 타임스탬프 파라미터 필수:
# --spring.batch.job.name=userReportJob timestamp(long)=$(date +%s%3N)
```

## Job 구성 예시

### UserReportJobConfig.java

```java
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = "userReportJob")
@RequiredArgsConstructor
public class UserReportJobConfig {
    
    private final UserUseCase userUseCase;
    
    @Bean
    public Job userReportJob(JobRepository jobRepository, Step userReportStep) {
        return new JobBuilder("userReportJob", jobRepository)
                .start(userReportStep)
                .build();
    }
    
    @Bean
    public Step userReportStep(JobRepository jobRepository, 
                               PlatformTransactionManager transactionManager) {
        return new StepBuilder("userReportStep", jobRepository)
                .<User, User>chunk(10, transactionManager)
                .reader(userItemReader())
                .processor(userItemProcessor())
                .writer(userItemWriter())
                .build();
    }
    
    @Bean
    public ItemReader<User> userItemReader() {
        return new ListItemReader<>(userUseCase.getUsers());
    }
    
    @Bean
    public ItemProcessor<User, User> userItemProcessor() {
        return user -> {
            log.info("Processing User: {} - {} (Team ID: {})", 
                    user.name(), user.email(), user.teamId());
            return user;
        };
    }
    
    @Bean
    public ItemWriter<User> userItemWriter() {
        return chunk -> {
            log.info("Writing {} users to report", chunk.size());
            chunk.getItems().forEach(user -> 
                log.info("User Report: {} - {} (Team ID: {}, Created: {})", 
                        user.name(), user.email(), user.teamId(), user.createdAt())
            );
        };
    }
}
```

### 핵심 포인트

1. **@ConditionalOnProperty**: `spring.batch.job.name` 값에 따라 Job Configuration 활성화
2. **Chunk 처리**: 10개씩 묶어서 트랜잭션 처리
3. **ItemReader**: UseCase에서 데이터 조회
4. **ItemProcessor**: 데이터 가공 및 로깅
5. **ItemWriter**: 최종 결과 출력

## 확장 가능성

### 1. 파일 출력

ItemWriter를 FlatFileItemWriter로 변경하여 CSV/TXT 파일 생성:

```java
@Bean
public FlatFileItemWriter<User> userFileWriter() {
    return new FlatFileItemWriterBuilder<User>()
        .name("userFileWriter")
        .resource(new FileSystemResource("output/users.csv"))
        .delimited()
        .names("id", "name", "email", "teamId")
        .build();
}
```

### 2. 데이터베이스 읽기

JdbcPagingItemReader로 대용량 데이터 페이징 처리:

```java
@Bean
public JdbcPagingItemReader<User> userDatabaseReader(DataSource dataSource) {
    return new JdbcPagingItemReaderBuilder<User>()
        .name("userDatabaseReader")
        .dataSource(dataSource)
        .queryProvider(queryProvider())
        .pageSize(100)
        .rowMapper(new UserRowMapper())
        .build();
}
```

### 3. 병렬 처리

멀티스레드 Step으로 성능 향상:

```java
@Bean
public Step parallelStep(JobRepository jobRepository, 
                        PlatformTransactionManager transactionManager,
                        TaskExecutor taskExecutor) {
    return new StepBuilder("parallelStep", jobRepository)
        .<User, User>chunk(10, transactionManager)
        .reader(reader())
        .processor(processor())
        .writer(writer())
        .taskExecutor(taskExecutor)
        .build();
}

@Bean
public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setThreadNamePrefix("batch-");
    executor.initialize();
    return executor;
}
```

### 4. 조건부 Step 실행

Step 실행 결과에 따라 다음 Step 결정:

```java
@Bean
public Job conditionalJob(JobRepository jobRepository,
                         Step step1, Step step2, Step step3) {
    return new JobBuilder("conditionalJob", jobRepository)
        .start(step1)
        .on("COMPLETED").to(step2)
        .from(step1).on("FAILED").to(step3)
        .end()
        .build();
}
```

### 5. 파티셔닝

데이터를 분할하여 병렬 처리:

```java
@Bean
public Step partitionedStep(JobRepository jobRepository,
                           Step workerStep,
                           Partitioner partitioner) {
    return new StepBuilder("partitionedStep", jobRepository)
        .partitioner("workerStep", partitioner)
        .step(workerStep)
        .gridSize(4)
        .taskExecutor(taskExecutor())
        .build();
}
```

## 모니터링

Spring Boot Actuator를 통한 배치 모니터링:

```bash
# 배치 Job 정보 조회
curl http://localhost:8080/actuator/batch/jobs

# 특정 Job 실행 이력
curl http://localhost:8080/actuator/batch/jobs/userReportJob/executions
```

## 장점

1. **표준화된 배치 처리**: 검증된 패턴과 구조
2. **트랜잭션 관리**: Chunk 단위 자동 트랜잭션
3. **재시작 기능**: 실패 지점부터 재실행
4. **확장성**: 병렬 처리, 파티셔닝 지원
5. **모니터링**: 실행 이력 및 상태 추적
6. **유연성**: 다양한 Reader/Writer 제공
7. **안정성**: Spring 생태계와 완벽한 통합
8. **동시 실행 안전성**: 서로 다른 Job 동시 실행 가능
9. **Job Instance 관리**: 파라미터 기반 고유 식별

## 동시 실행 및 반복 실행

### 서로 다른 Job 동시 실행

✅ **완전히 안전합니다**

```bash
# 터미널 1
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob"

# 터미널 2 (동시 실행)
./gradlew :batch:bootRun --args="--spring.batch.job.name=teamReportJob"
```

- 각 Job은 `@ConditionalOnProperty`로 독립적인 Configuration
- 서로 다른 Step 실행
- Spring Batch 메타데이터 테이블의 낙관적 락으로 보호

### 동일 Job 반복 실행

⚠️ **타임스탬프 파라미터 필수**

```bash
# 첫 실행 (성공)
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob"

# 두 번째 실행 (재실행 안 됨! 동일한 Instance)
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob"

# 올바른 반복 실행 (타임스탬프 포함)
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=$(date +%s%3N)"
```

### 스케줄러 통합

#### Cron (Linux/Mac)
```bash
# 5분마다 실행
*/5 * * * * cd /path/to/project && ./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=$(date +%s%3N)"
```

#### Spring @Scheduled
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

자세한 내용은 `BATCH_CONCURRENT_EXECUTION_FINAL.md`를 참조하세요.

## 버전 정보

- Spring Boot: 3.5.10
- Spring Batch: 5.1.2 (Spring Boot 3.5.10에 포함)
- Java: 21

## 관련 문서

- `BATCH_CONCURRENT_EXECUTION_FINAL.md`: 동시 실행 및 반복 실행 완전 가이드
- `BATCH_JOBS_GUIDE.md`: 배치 Job 실행 가이드 (구 CommandLineRunner 방식)
- `BATCH_ARCHITECTURE.md`: 배치 아키텍처 상세 설명
- `BATCH_EXECUTION_EXAMPLES.md`: 다양한 실행 예시
