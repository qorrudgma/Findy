# Findy 백엔드

Findy 검색 엔진의 백엔드 애플리케이션입니다.

## 설치 및 실행

### 필수 요구사항
- Java 11 이상
- Maven 또는 Gradle
- MongoDB (검색 기능에 필요)

### 설치 방법
1. 저장소 클론
```bash
git clone [repository-url]
cd Findy
```

2. 빌드
```bash
./mvnw clean install
```

3. 실행
```bash
./mvnw spring-boot:run
```
- 서버는 기본적으로 8485 포트에서 실행됩니다.

## API 엔드포인트

### 1. 뉴스 검색 API
- `GET /api/search?q={검색어}&page={페이지}&size={크기}`
  - 검색어와 일치하는 뉴스 기사를 반환합니다.
  - 선택적으로 카테고리 필터링을 위해 `&category={카테고리}` 추가 가능

### 2. 자동완성 API
- `GET /api/autocomplete?q={검색어}`
  - 입력된 검색어에 기반한 자동완성 제안을 반환합니다.
  - 응답 형식:
  ```json
  [
    {
      "query": "검색어 제안1",
      "count": 100
    },
    {
      "query": "검색어 제안2",
      "count": 50
    }
  ]
  ```

### 3. 인기 검색어 API
- `GET /api/search/popular`
  - 현재 인기 있는 검색어 목록을 반환합니다.

## 데이터베이스 구성

### MongoDB 연결
- MongoDB 연결 정보는 `application.properties` 파일에 설정되어 있습니다.
- 기본 연결 문자열: `mongodb://localhost:27017/findy`

## 자동완성 기능 구현

현재 구현된 자동완성 기능은 인메모리 데이터를 사용합니다. 실제 환경에서는 다음과 같이 확장할 수 있습니다:

1. MongoDB를 사용한 자동완성:
   - 검색어 컬렉션을 생성하여 자주 검색되는 키워드 저장
   - 텍스트 인덱스를 사용하여 빠른 검색 제공

2. Elasticsearch를 사용한 자동완성:
   - Elasticsearch의 completion suggester 기능 활용
   - 더 정교한 자동완성 및 오타 수정 기능 제공

3. 현재 구현:
   - 자주 사용되는 한글 초성('강', '경', '서' 등)에 대한 인메모리 데이터 제공
   - 검색어 패턴에 따른 기본 제안 생성 