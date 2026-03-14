# Project Context

## Tech Stack
- Java 17
- Spring Boot 3.x
- Gradle (Groovy DSL)
- PostgreSQL

## Architecture
- Layered architecture: Controller → Service → Repository
- Package convention: com.{company}.{project}.{domain}
- DTO/Entity 분리 필수

## Code Style
- 4-space indent
- camelCase for methods/variables, PascalCase for classes
- Record 클래스 적극 활용 (DTO에 Java 17 record 사용)
- 의미 없는 주석 금지, 코드로 의도 표현

## Database Conventions
- snake_case for table/column names
- FK 컬럼에는 반드시 인덱스 추가 (PostgreSQL은 자동 생성 안 함)
- timestamp는 timestamptz 사용
- SELECT * 금지
- N+1 문제 방지: JOIN FETCH 또는 @BatchSize 활용

## Query Optimization Rules
- 10만 건 이상 테이블 쿼리는 반드시 EXPLAIN ANALYZE 확인
- 페이지네이션은 offset 방식 지양, cursor 기반 검토
- 느린 쿼리 기준: 100ms 초과 시 반드시 개선 검토

## Common Commands
```bash
./gradlew clean build       # 전체 빌드
./gradlew bootRun           # 로컬 실행
./gradlew test              # 테스트 실행
./gradlew dependencies      # 의존성 확인
```

## Cloud Cost Guidelines
- 불필요한 eager loading 금지
- 대용량 조회는 페이지네이션 필수
- 커넥션 풀 설정 항상 명시 (HikariCP)
- 외부 API 호출 시 타임아웃/재시도 로직 필수

## Planning Rules (Opus Plan Mode)
- 새 기능 개발 전 반드시 Plan 모드로 설계 먼저
- DB 스키마 변경은 마이그레이션 영향도 검토 후 진행
- 클라우드 연동 작업은 비용 영향 먼저 분석
