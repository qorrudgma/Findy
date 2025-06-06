# Findy Frontend

Findy 검색 엔진의 프론트엔드 애플리케이션입니다.

## 설치 및 실행

### 필수 요구사항
- Node.js (v14 이상)
- npm (v6 이상)

### 설치 방법
1. 저장소 클론
```bash
git clone [repository-url]
cd findy-frontend
```

2. 의존성 설치
```bash
npm install
```

3. 개발 서버 실행
```bash
npm start
```
- 브라우저에서 http://localhost:3000 으로 접속하여 사용할 수 있습니다.

## 기능

### 1. 검색 기능
- 키워드를 입력하여 뉴스 검색
- 카테고리별 필터링

### 2. 자동완성 기능
- 검색창에 입력 시 자동완성 제안 표시
- 방향키(↑/↓)로 제안 선택 가능
- 제안 클릭 시 검색창에 자동 입력
- 엔터키로 검색 실행

### 3. 카테고리 네비게이션
- 뉴스 카테고리별 탐색
- 카테고리 선택 시 관련 뉴스 표시

### 4. 추가 페이지
- 자주 묻는 질문(FAQ)
- 문의하기
- 이용약관
- 개인정보처리방침
- 저작권 정책
- 광고 정책

## 자동완성 API 연동 방법

### 1. 백엔드 API 연결
프론트엔드는 `/api/autocomplete?q=검색어` 엔드포인트를 호출하여 자동완성 데이터를 가져옵니다.

- API 응답 형식:
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

- count는 선택적 필드로, 해당 검색어의 인기도를 나타냅니다.

### 2. 백엔드 API 없이 테스트
백엔드 API 연결에 실패할 경우, 프론트엔드에서 더미 데이터를 제공합니다.
특정 한글 초성('강', '경', '서' 등)으로 시작하는 단어들에 대해 자동완성이 지원됩니다. 