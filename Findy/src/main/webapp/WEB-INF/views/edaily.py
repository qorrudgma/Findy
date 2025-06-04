# 필요한 라이브러리 임포트
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from bs4 import BeautifulSoup
from urllib.parse import urljoin
from pymongo import MongoClient
import requests
import hashlib
import time
import traceback

# MongoDB에 기사들을 저장하는 함수 정의
def save_to_mongodb(articles):
    try:
        client = MongoClient("mongodb://localhost:27017/")  # MongoDB 클라이언트 연결
        db = client["newsdata1"]                             # DB 이름
        collection = db["newsdata1"]                         # 컬렉션 이름

        total = len(articles)  # 전체 기사 수
        unique_post_ids = set()  # 중복 제거용 post_id 집합
        inserted = 0
        skipped = 0

        for article in articles:
            post_id = article["post_id"]
            if post_id in unique_post_ids:
                skipped += 1
                continue

            if collection.count_documents({"post_id": post_id}, limit=1) == 0:
                collection.insert_one(article)
                inserted += 1
            else:
                skipped += 1

            unique_post_ids.add(post_id)

        print(f"\n📢 MongoDB 저장 완료 | 총 시도: {total}, 삽입: {inserted}, 중복 스킵: {skipped}")
    except Exception as e:
        print(f"❌ MongoDB 저장 실패: {e}")
        traceback.print_exc()

# 일반 기사에서 본문과 날짜를 추출하는 함수
def extract_article_content(article_url):
    try:
        res = requests.get(article_url, headers=headers)
        soup = BeautifulSoup(res.text, "html.parser")
        content_tag = soup.select_one("#news_body_area")
        content = content_tag.get_text(strip=True) if content_tag else "[분문 없음]"

        date_tag = soup.select_one("div.article_info span.date")
        published_at = date_tag.get_text(strip=True) if date_tag else "Unknown"

        return content, published_at
    except Exception as e:
        return f"[분문 찾기 실패] {e}", "Unknown"

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
    "정치": "https://www.edaily.co.kr/article/politics",
    "사회": "https://www.edaily.co.kr/article/society",
    "증권": "https://www.edaily.co.kr/article/stock",
    "부동산": "https://www.edaily.co.kr/article/estate",
    "문화": "https://www.edaily.co.kr/article/culture",
    "연예": "https://www.edaily.co.kr/article/entertainment",
    "스포츠": "https://www.edaily.co.kr/article/sports",
    "오피니언": "https://www.edaily.co.kr/opinion",
    "포토": "https://www.edaily.co.kr/photo/"
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

        # 포토 카테고리 처리
        if category == "포토":
            photo_section = soup.select_one("div.projects.clearfix.cols-3")
            if not photo_section:
                print("❌ [포토] projects 영역 없음")
                continue

            photo_items = photo_section.select("div.project-cont")
            print(f"🔍 발견된 사진 개수: {len(photo_items)}")

            for idx, item in enumerate(photo_items, start=1):
                try:
                    title_tag = item.select_one("div.overlay h5")
                    img_tag = item.select_one("img")

                    title = title_tag.get_text(strip=True) if title_tag else "[제목 없음]"
                    image_url = urljoin(url, img_tag["src"]) if img_tag and img_tag.get("src") else "[이미지 없음]"

                    date_tag = item.select_one("span.date") or item.select_one("div.date")
                    published_at = date_tag.get_text(strip=True) if date_tag else ""

                    post_id = hashlib.sha1(image_url.encode("utf-8")).hexdigest()

                    doc = {
                        "post_id": post_id,
                        "title": title,
                        "image_url": image_url,
                        "url": url,
                        "published_at": published_at,
                        "source": "edaily.co.kr",
                        "category": category
                    }

                    all_articles.append(doc)
                    print(f"{idx}. 🖼️ {title}")
                    time.sleep(0.2)

                except Exception as inner_e:
                    print(f"❌ [{idx}] 사진 찾기 실패: {inner_e}")
                    traceback.print_exc()

        else:
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
                post_id = hashlib.sha1(full_url.encode("utf-8")).hexdigest()

                doc = {
                    "post_id": post_id,
                    "title": title,
                    "summary": summary,
                    "content": summary,  # 분문 대신 요약 드림
                    "url": full_url,
                    "category": category,
                    "published_at": published_at,
                    "source": "edaily.co.kr"
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