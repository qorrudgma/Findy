# 🐍 Findy Crawler

**Findy 뉴스 검색 엔진을 위한 Python 크롤링 시스템**

## 📁 프로젝트 구조

```
findy-crawler/
├── main.py                 # 메인 실행 파일
├── requirements.txt        # Python 의존성
├── README.md              # 이 파일
├── config/
│   ├── __init__.py
│   └── settings.py        # 설정 파일
├── crawlers/              # 뉴스 크롤러들
│   ├── __init__.py
│   ├── chosun.py         # 조선일보 크롤러
│   ├── donga.py          # 동아일보 크롤러
│   ├── edaily.py         # 이데일리 크롤러
│   ├── hani.py           # 한겨레 크롤러
│   ├── khan.py           # 경향신문 크롤러
│   ├── yna.py            # 연합뉴스 크롤러
│   ├── tfidf.py          # TF-IDF 분석
│   └── textrank.py       # TextRank 분석
├── utils/
│   ├── __init__.py
│   └── db_connector.py   # MongoDB 연결
└── logs/                 # 로그 파일들
```

## 🚀 설치 및 실행

### 1. 의존성 설치
```bash
cd findy-crawler
pip install -r requirements.txt
```

### 2. MongoDB 실행
```bash
# Windows
net start MongoDB

# 또는 MongoDB Compass 사용
```

### 3. 크롤러 실행
```bash
# 모든 크롤러 실행
python main.py

# 개별 크롤러 실행
python crawlers/chosun.py
python crawlers/donga.py
```

## 🔧 설정

`config/settings.py` 파일에서 다음 설정들을 수정할 수 있습니다:

- **MongoDB 연결 정보**
- **크롤링 지연 시간**
- **수집할 기사 수**
- **로그 레벨**

## 📊 데이터 구조

수집된 뉴스 데이터는 다음 형식으로 MongoDB에 저장됩니다:

```json
{
  "headline": "뉴스 제목",
  "url": "기사 URL",
  "content": "기사 본문",
  "time": "발행일시",
  "category": "카테고리",
  "source": "언론사"
}
```

## 🔗 백엔드 연동

이 크롤러로 수집된 데이터는 **Spring Boot 백엔드**에서 읽어와 **React 프론트엔드**로 제공됩니다.

연동 구조:
```
Python Crawler → MongoDB → Spring Boot → React
``` 