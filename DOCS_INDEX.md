# 문서 인덱스

프로젝트의 모든 문서를 목적별로 정리한 가이드입니다.

## 시작하기

### 필수 문서

1. **[README.md](README.md)** - 프로젝트 개요 및 빠른 시작
   - 프로젝트 소개
   - 기술 스택
   - 빠른 시작 가이드
   - 기본 실행 방법

2. **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** - 프로젝트 전체 요약
   - 작업 내역
   - 아키텍처 결정
   - 학습 포인트
   - 참고 자료

## 아키텍처 & 구조

3. **[MULTI_MODULE_GUIDE.md](MULTI_MODULE_GUIDE.md)** - 멀티모듈 구조 가이드
   - 모듈 구성 (core, api, batch)
   - 의존성 관계
   - 빌드 및 실행 방법

## Spring Batch (현재 구현)

### 주요 문서

4. **[SPRING_BATCH_GUIDE.md](SPRING_BATCH_GUIDE.md)** ⭐ - Spring Batch 구현 가이드
   - Spring Batch 5.1.2 아키텍처
   - Job, Step, Chunk 구성
   - 실행 방법
   - 확장 가능성

5. **[BATCH_CONCURRENT_EXECUTION_FINAL.md](BATCH_CONCURRENT_EXECUTION_FINAL.md)** ⭐ - 동시 실행 완전 가이드
   - 서로 다른 Job 동시 실행
   - 동일 Job 반복 실행 (타임스탬프 파라미터)
   - Job Instance 관리
   - 스케줄러 통합 (Cron, Kubernetes, Spring @Scheduled)
   - 프로덕션 권장 사항

## 구 배치 문서 (참고용)

> **⚠️ 주의**: 다음 문서들은 구 CommandLineRunner 기반 구현을 설명합니다.
> 현재는 Spring Batch 5.1.2를 사용하므로 위의 주요 문서를 참조하세요.

6. **[BATCH_JOBS_GUIDE.md](BATCH_JOBS_GUIDE.md)** - 구 배치 Job 가이드
7. **[BATCH_ARCHITECTURE.md](BATCH_ARCHITECTURE.md)** - 구 배치 아키텍처
8. **[BATCH_EXECUTION_EXAMPLES.md](BATCH_EXECUTION_EXAMPLES.md)** - 구 실행 예시

## 문서 읽는 순서

### 처음 시작하는 경우

1. [README.md](README.md) - 프로젝트 이해
2. [MULTI_MODULE_GUIDE.md](MULTI_MODULE_GUIDE.md) - 구조 파악
3. [SPRING_BATCH_GUIDE.md](SPRING_BATCH_GUIDE.md) - 배치 구현 이해

### 배치 작업 개발

1. [SPRING_BATCH_GUIDE.md](SPRING_BATCH_GUIDE.md) - 기본 구현
2. [BATCH_CONCURRENT_EXECUTION_FINAL.md](BATCH_CONCURRENT_EXECUTION_FINAL.md) - 동시 실행 및 스케줄링

### 프로젝트 전체 이해

1. [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) - 전체 작업 내역 및 결정 사항

## 빠른 참조

### 배치 실행 명령어

```bash
# 단일 실행
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob"

# 반복 실행 (타임스탬프 포함)
./gradlew :batch:bootRun --args="--spring.batch.job.name=userReportJob timestamp(long)=$(date +%s%3N)"
```

### API 실행

```bash
# API 서버 실행
./gradlew :api:bootRun

# Docker Compose로 전체 스택 실행
docker-compose up -d
```

## 문서 업데이트 이력

- 2026-02-09: Spring Batch 5.1.2 구현 완료, 동시 실행 가이드 추가
- 2026-02-09: 멀티모듈 구조 전환
- 2026-02-09: 초기 프로젝트 생성

## 기여

문서 개선 제안이나 오류 발견 시 이슈를 등록해주세요.
