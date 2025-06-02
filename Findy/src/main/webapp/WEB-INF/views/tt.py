import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin, urlparse
from pymongo import MongoClient
from datetime import datetime
import time

# ======================
# [1] 메인 페이지에서 카테고리 링크 추출
# ======================
def get_category_links():
    url = "https://www.chosun.com/"
    headers = {"User-Agent": "Mozilla/5.0"}  # 봇 차단 회피용 User-Agent 설정

    try:
        res = requests.get(url, headers=headers)
        res.raise_for_status()
        soup = BeautifulSoup(res.text, "html.parser")

        category_links = set()  # 중복 제거를 위해 set 사용

        # 모든 <a> 태그 중 카테고리 링크만 필터링
        for a_tag in soup.select("a"):
            href = a_tag.get("href")
            if href and href.startswith("/"):
                full_url = urljoin(url, href)  # 상대경로를 절대경로로 변환

                # 주요 카테고리 키워드가 URL에 포함되어 있으면 수집
                if any(cat in href for cat in [
                    "/politics/", "/economy/", "/sports/", "/culture/",
                    "/international/", "/national/", "/opinion/",
                    "/entertainments/", "/game/", "/health/", "/sisa/"
                ]):
                    category_links.add(full_url)

        return list(category_links)  # 리스트로 변환 후 반환

    except Exception as e:
        print(f"[에러] 카테고리 수집 실패: {e}")
        return []

# ======================
# [2] 개별 기사 페이지에서 제목, 본문, ID, 작성일 추출
# ======================
def extract_article_data(article_url):
    headers = {"User-Agent": "Mozilla/5.0"}
    try:
        res = requests.get(article_url, headers=headers)
        res.raise_for_status()
        soup = BeautifulSoup(res.text, "html.parser")

        # 제목: og:title 메타 태그에서 추출
        title_tag = soup.select_one("meta[property='og:title']")
        title = title_tag["content"] if title_tag else ""

        # 본문: section.article-body 내부의 <p> 태그 텍스트들을 모두 이어붙임
        content_section = soup.select_one("section.article-body")
        content = "" #초기값선언
        if content_section:
            # paragraphs = content_section.find_all("p")
            paragraphs = content_section.select("p.article-body__content.article-body__content-text.text--black")
            content = "\n".join(p.get_text(strip=True) for p in paragraphs)

        # 작성일: article:published_time 메타 태그에서 추출
        pub_time = soup.select_one("meta[property='article:published_time']")
        published_at = pub_time["content"] if pub_time else None

        # 게시글 ID: URL의 마지막 segment 추출
        parsed = urlparse(article_url)
        post_id = parsed.path.rstrip("/").split("/")[-1]

        return {
            "title": title,
            "link": article_url,
            "content": content,
            "post_id": post_id,
            "published_at": published_at,
            "scraped_at": datetime.utcnow()
        }

    except Exception as e:
        print(f"[본문 추출 실패] {article_url} → {e}")
        return None


# ======================
# [3] 카테고리 내 여러 페이지를 순회하며 기사 수집
# ======================
def collect_articles_from_category(category_url, max_pages=10):
    headers = {"User-Agent": "Mozilla/5.0"}
    collected = []  # 수집된 기사 리스트

    for page in range(1, max_pages + 1):
        page_url = f"{category_url}?page={page}"
        print(f" [카테고리 수집 중] {page_url}")

        try:
            res = requests.get(page_url, headers=headers)
            if res.status_code == 404:
                break  # 페이지가 없으면 종료

            soup = BeautifulSoup(res.text, "html.parser")
            links = soup.select("a.story-card__headline")

            if not links:
                print(" 기사 없음, 중단")
                break

            for a_tag in links:
                href = a_tag.get("href")
                if not href:
                    continue

                article_url = urljoin(category_url, href)
                article_data = extract_article_data(article_url)
                if article_data:
                    collected.append(article_data)

            time.sleep(0.5)  # 너무 빠르게 요청하지 않도록 딜레이

        except Exception as e:
            print(f"[페이지 오류] {page_url} → {e}")
            continue

    return collected

# ======================
# [4] MongoDB에 저장
# ======================
def save_to_mongodb(articles):
    try:
        client = MongoClient("mongodb://localhost:27017/")
        db = client["newsdata"]  # DB명: newsdata
        collection = db["newsdata"]  # 컬렉션명: newsdata

        count = 0
        for article in articles:
            # post_id가 없거나 빈 문자열인 경우는 건너뜀
            if not article.get("post_id"):
                print(f" post_id 누락: {article.get('title')} - {article.get('link')}")
                continue

            # post_id 기준으로 upsert (있으면 갱신, 없으면 삽입)
            # collection.update_one(
            #     {"post_id": article["post_id"]},
            #     {"$set": article},
            #     upsert=True
            # )

            #테스트용도로 중복허용 
            collection.insert_one(article)

            count += 1

        print(f" MongoDB 저장 완료: {count}건")

    except Exception as e:
        print(f"[MongoDB 오류] {e}")

# ======================
# [5] 전체 실행 흐름
# ======================
if __name__ == "__main__":
    categories = get_category_links()  # 메인 페이지에서 카테고리 링크 수집
    print(f"\n📚 추출된 카테고리 수: {len(categories)}")

    total_saved = 0  # 전체 저장된 기사 수

    for cat_url in categories:
        print(f"\n============================")
        print(f" 카테고리 시작: {cat_url}")
        articles = collect_articles_from_category(cat_url, max_pages=5)
        print(f" 수집된 기사 수: {len(articles)}")

        save_to_mongodb(articles)  # 수집된 기사 저장
        total_saved += len(articles)

    print(f"\n 전체 저장 완료: {total_saved}건")
