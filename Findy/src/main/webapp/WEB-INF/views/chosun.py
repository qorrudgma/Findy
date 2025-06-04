# 필요한 모듈들을 니코니코니~

import time 
 # 대기 시간 제어용
import threading  
# 스레드 간 동기화에 사용
from concurrent.futures import ThreadPoolExecutor  
# 병렬처리를 위한 ThreadPool
from selenium import webdriver  
# 웹 페이지 조작을 위한 Selenium
from selenium.webdriver.chrome.options import Options  
# Chrome 옵션 설정
from bs4 import BeautifulSoup 
 # HTML 파싱 라이브러리
from urllib.parse import urljoin, urlparse  
# URL 병합 및 파싱용
from pymongo import MongoClient 
 # MongoDB와 연동
from datetime import datetime 
 # 현재 시각 기록용

# ======================
# [1] Selenium 드라이버 설정 함수 정의
# ======================
def get_driver():
    options = Options()  # Chrome 실행 옵션 객체 생성
    options.add_argument("--headless=new")  # 브라우저를 백그라운드(화면 없이) 실행
    options.add_argument("--no-sandbox")  # 보안 샌드박스 해제 (리눅스에서 필요)
    options.add_argument("--disable-dev-shm-usage")  # 공유 메모리 사용 제한 해제
    # 사용자 에이전트 설정: 서버에서 봇 차단을 피하기 위해 일반 브라우저처럼 위장
    options.add_argument(
        "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
    )
    return webdriver.Chrome(options=options)  # 설정된 옵션으로 Chrome 드라이버 실행

# ======================
# [2] 메인 페이지에서 뉴스 카테고리 링크 추출
# ======================
def get_category_links():
    url = "https://www.chosun.com/"  # 조선일보 메인 주소
    driver = get_driver()  # 웹드라이버 생성
    category_links = set()  # 중복 방지를 위한 set

    try:
        driver.get(url)  # 메인 페이지 요청
        soup = BeautifulSoup(driver.page_source, "html.parser")  # HTML 파싱

        # a 태그를 모두 검사하여 뉴스 카테고리 관련 링크만 필터링
        for a_tag in soup.select("a"):
            href = a_tag.get("href")
            if href and href.startswith("/") and any(cat in href for cat in [
                "/politics/", "/economy/", "/sports/", "/culture/",
                "/international/", "/national/", "/opinion/",
                "/entertainments/", "/game/", "/health/", "/sisa/"
            ]):
                # 절대경로로 변환 후 중복 제거
                full_url = urljoin(url, href.split("?")[0].split("#")[0])
                category_links.add(full_url)

    except Exception as e:
        print(f"[에러] 카테고리 수집 실패: {e}")  # 예외 발생 시 메시지 출력
    finally:
        driver.quit()  # 드라이버 종료

    return list(category_links)  # set → list 변환 후 반환

# ======================
# [3] 기사 상세 페이지에서 제목, 본문, 날짜 등 추출
# ======================
def extract_article_data(driver, article_url, category_name):
    try:
        driver.get(article_url)  # 기사 페이지 요청
        time.sleep(1)  # 로딩 대기
        soup = BeautifulSoup(driver.page_source, "html.parser")  # HTML 파싱

        # 제목 추출 (og:title 메타 태그 이용)
        title_tag = soup.select_one("meta[property='og:title']")
        title = title_tag["content"] if title_tag else ""

        # 본문 추출 (표준 구조 기준)
        content = ""
        section = soup.select_one("section.article-body[itemprop='articleBody']")
        if section:
            paragraphs = section.find_all("p")
            content = "\n".join(p.get_text(strip=True) for p in paragraphs)

        # 구조가 다를 경우 백업 선택자 사용
        if not content:
            fallback = soup.select_one("div.article-body") or soup.select_one("div#article-body")
            if fallback:
                paragraphs = fallback.find_all("p")
                content = "\n".join(p.get_text(strip=True) for p in paragraphs)

        # 발행일 추출 (ISO 형식 문자열)
        pub_time = soup.select_one("meta[property='article:published_time']")
        published_at = pub_time["content"] if pub_time else None

        # URL에서 post_id 추출 (중복 제거용 키)
        post_id = urlparse(article_url).path.rstrip("/").split("/")[-1]

        return {
            "title": title,
            "link": article_url,
            "content": content,
            "post_id": post_id,
            "published_at": published_at,
            "scraped_at": datetime.now(),  # 수집 시각
            "category": category_name
        }

    except Exception as e:
        print(f"[본문 추출 실패] {article_url} → {e}")
        return None  # 에러 발생 시 None 반환

# ======================
# [4] 한 카테고리 내 기사 여러 페이지 수집
# ======================
def collect_articles_from_category(category_url, max_pages=3):
    category_name = category_url.strip("/").split("/")[-1]  # URL에서 카테고리명 추출
    collected = []  # 결과 저장 리스트
    driver = get_driver()  # 드라이버 생성

    try:
        for page in range(1, max_pages + 1):  # 페이지 반복
            page_url = f"{category_url}?page={page}"
            print(f"[카테고리] {category_name} → {page_url}")

            driver.get(page_url)
            time.sleep(1)
            soup = BeautifulSoup(driver.page_source, "html.parser")

            # 기사 링크 찾기
            links = soup.select("a.story-card__headline")
            if not links:
                print(" 기사 없음, 중단")
                break

            for a_tag in links:
                href = a_tag.get("href")
                if not href:
                    continue
                article_url = urljoin(category_url, href)  # 절대경로
                article_data = extract_article_data(driver, article_url, category_name)
                if article_data:
                    collected.append(article_data)

    except Exception as e:
        print(f"[페이지 오류] {category_url} → {e}")
    finally:
        driver.quit()

    return collected  # 해당 카테고리에서 수집한 기사 리스트 반환

# ======================
# [5] MongoDB에 저장
# ======================
def save_to_mongodb(articles):
    try:
        client = MongoClient("mongodb://localhost:27017/")  # MongoDB 연결
        db = client["newsdata"]  # DB 선택
        collection = db["newsdata"]  # 컬렉션 선택

        total = len(articles)
        unique_post_ids = set()
        inserted = 0
        skipped = 0

        for article in articles:
            post_id = article.get("post_id")
            if not post_id:
                skipped += 1
                continue

            unique_post_ids.add(post_id)
#################여기서가 중복 제거용 로직임 ##########################

            # 중복 여부 확인 후 upsert (없으면 insert, 있으면 update)
            result = collection.update_one(
                {"post_id": post_id},
                {"$set": article},
                upsert=True
            )
            if result.upserted_id or result.modified_count:
                inserted += 1

        # 결과 출력
        print(f"\n 전체 수집 기사 수: {total}건")
        print(f" 고유 post_id 수: {len(unique_post_ids)}건")
        print(f" 저장 또는 갱신: {inserted}건")
        print(f" post_id 누락으로 스킵된 건수: {skipped}건")

    except Exception as e:
        print(f"[MongoDB 오류] {e}")

# ======================
# [6] 메인 함수: 병렬 수집 및 저장 실행 ( 이거시 멀티 쓰레딩)
# ======================
if __name__ == "__main__":
    categories = get_category_links()  # 뉴스 카테고리 링크 수집
    print(f"\n📚 카테고리 수집 완료: {len(categories)}개")

    total_articles = []  # 전체 기사 리스트
    lock = threading.Lock()  # 리스트 접근 시 동기화용 락

    # 각 카테고리 처리 함수 (스레드용)
    def process_category(cat_url):
        articles = collect_articles_from_category(cat_url, max_pages=3)
        print(f"  {cat_url} → {len(articles)}건 수집됨")
        with lock:  # 스레드 간 충돌 방지
            total_articles.extend(articles)

    # 병렬 처리 시작 (최대 5개 스레드)
    with ThreadPoolExecutor(max_workers=5) as executor:
        executor.map(process_category, categories)

    print(f"\n📰 총 수집된 기사 수: {len(total_articles)}")
    save_to_mongodb(total_articles)  # MongoDB 저장