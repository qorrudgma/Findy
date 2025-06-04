# 필요한 라이브러리 임포트
# import hashlib
import time
import traceback

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from bs4 import BeautifulSoup
from urllib.parse import urljoin
# from pymongo import MongoClient
from konlpy.tag import Komoran
from sklearn.feature_extraction.text import TfidfVectorizer
from mongo_save import save_to_mongodb
from textrank import textrank_keywords, textrank_summarize

# MongoDB에 기사들을 저장하는 함수 정의
# def save_to_mongodb(articles):
#     try:
#         client = MongoClient("mongodb://localhost:27017/")  # MongoDB 클라이언트 연결
#         db = client["newsdata01"]                             # DB 이름
#         collection = db["newsdata01"]                         # 컬렉션 이름

#         total = len(articles)  # 전체 기사 수
#         unique_post_ids = set()  # 중복 제거용 post_id 집합
#         inserted = 0
#         skipped = 0

#         for article in articles:
#             # print(f" !@#$ url: {article['url']}\n")
#             url = article.get('url')
#             if not url:
#                 skipped += 1
#                 continue

#             unique_post_ids.add(url)

#             result = collection.update_one(
#                 {"url": url},
#                 {"$set": article},
#                 upsert=True
#             )
#             if result.upserted_id or result.modified_count:
#                 inserted += 1

#         print(f"\n📢 MongoDB 저장 완료 | 총 시도: {total}, 삽입: {inserted}, 중복 스킵: {skipped}")
#     except Exception as e:
#         print(f"❌ MongoDB 저장 실패: {e}")
#         traceback.print_exc()


# 일반 기사에서 본문과 날짜를 추출하는 함수
def extract_article_content(article_url):
    try:
        driver.get(article_url)
        time.sleep(1.5)
        soup = BeautifulSoup(driver.page_source, "html.parser")

        # 본문 추출
        content_tag = soup.select_one("#news_body_area")
        content = content_tag.get_text(strip=True) if content_tag else "[본문 없음]"

        # 날짜 추출
        date_tag = soup.select_one("div.dates ul li p")
        published_at = "Unknown"

        if date_tag:
            raw_text = date_tag.get_text(strip=True)
            published_at = raw_text  # ✅ 날짜 텍스트 실제 저장
        else:
            print("❗ 날짜 태그 없음")

        return content, published_at

    except Exception as e:
        print(f"❌ 본문/날짜 추출 실패: {e}")
        traceback.print_exc()
        return "[본문 찾기 실패]", "Unknown"



# Selenium 설정
options = Options()
options.add_argument("--headless")  # 메뉴로에서 화면 비어있는 방식
options.add_argument("--disable-gpu")
options.add_argument("--window-size=1920,1080")
driver = webdriver.Chrome(options=options)

# 기억해야 할 headers (requests에서 용도)
headers = {
    "User-Agent": "Mozilla/5.0"
}

# 카테고리 URL 정의
category_urls = {
    "경제": "https://www.edaily.co.kr/article/economy",
    "오피니언": "https://www.edaily.co.kr/opinion",
    "사회": "https://www.edaily.co.kr/article/society",
    "스포츠": "https://www.edaily.co.kr/article/sports",
    "문화/연예": "https://www.edaily.co.kr/article/entertainment",
    "문화/연예": "https://www.edaily.co.kr/article/culture",
    # "정치": "https://www.edaily.co.kr/article/politics",
    # "증권": "https://www.edaily.co.kr/article/stock",
    # "부동산": "https://www.edaily.co.kr/article/estate",
    # "포토": "https://www.edaily.co.kr/photo/"
}

# 총 기사 목록
all_articles = []

# 카테고리별 수집
for category, url in category_urls.items():
    print(f"\n▶ 카테고리: {category} ({url})")
    try:
        driver.get(url)
        time.sleep(2)
        soup = BeautifulSoup(driver.page_source, "html.parser")
        visited = set()

        # 오피니언: newsbox / 기타: newsbox_04
        box_selector = "div.newsbox" if category == "오피니언" else "div.newsbox_04"
        articles = soup.select(box_selector)

        for article in articles:
            a_tag = article.select_one("a")
            if not a_tag or not a_tag.get("href"):
                continue

            full_url = urljoin("https://www.edaily.co.kr", a_tag.get("href"))
            if full_url in visited:
                continue
            visited.add(full_url)

            if category == "오피니언":
                li_tags = article.select("ul.newsbox_texts li")
                title = li_tags[0].get_text(strip=True) if len(li_tags) > 0 else "[제목 없음]"
                summary = li_tags[1].get_text(strip=True) if len(li_tags) > 1 else "[요약 없음]"
            else:
                title_tag = article.select_one("ul.newsbox_texts li")
                title = title_tag.get_text(strip=True) if title_tag else "[제목 없음]"
                summary = a_tag.get_text(strip=True)

            content, published_at = extract_article_content(full_url)
            # content, published_at = extract_article_content(full_url)
            # post_id = hashlib.sha1(full_url.encode("utf-8")).hexdigest()

            doc = {
                # "id": post_id,
                "headline": title,
                # "summary": summary,
                "content": summary,  # 분문 대신 요약 드림
                "url": full_url,
                "category": category,
                "time": published_at,
                "source": "edaily"
            }

            all_articles.append(doc)
            print(f"📰 {title}")
            time.sleep(0.2)

    except Exception as e:
        print(f"❌ [{category}] 수집 중 오류: {e}")
        traceback.print_exc()

# 바로 프로세스 종료하고 모든 것 저장
driver.quit()
save_to_mongodb(all_articles)