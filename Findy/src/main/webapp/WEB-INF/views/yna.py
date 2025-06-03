import requests
import time
from bs4 import BeautifulSoup

# 뉴스 목록에서 링크 추출
def fetch_headlines(category, page):
    f_url = f"https://www.yna.co.kr/{category}/all/{page}"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    }

    try:
        response = requests.get(f_url, headers=headers)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, "html.parser")
        news_headlines = []

        articles = soup.select("ul.list01")
        for article in articles:
            link_tag = article.select_one("a.tit-news")
            if link_tag:
                url = link_tag.get("href")
                if url and not url.startswith("http"):
                    url = "https:" + url
                news_headlines.append({"url": url})
        return news_headlines

    except Exception as e:
        print(f"[오류] 기사 목록 수집 실패: {e}")
        return []

# 기사 본문 추출
def fetch_article_content(article_url):
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    }

    try:
        response = requests.get(article_url, headers=headers)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, "html.parser")

        # 제목
        headline_tags = soup.select("h1.tit01")

        # 내용
        content_tag = soup.select("div.story-news p")
        paragraphs = [tag.get_text(strip=True) for tag in content_tag[:-1]]
        full_text = " ".join(paragraphs)

        # 시간
        time_tag = soup.select_one("div.article-info span.update-time")
        published_time = time_tag.get_text(strip=True) if time_tag else "시간 없음"
        clean_time = clean_datetime(published_time)

        return {
            "headline": headline_tags,
            "content": full_text,
            "url": article_url,
            "source": "yna",
            "time": clean_time
        }

    except Exception as e:
        print(f"[오류] 본문 수집 실패: {e}")
        return None

# 날짜 포맷 정리
def clean_datetime(text):
    text = text.replace("기사입력 ", "")
    return text.replace(".", "-")

# 실행 흐름
# 카테고리 종류
# categories = ["economy", "opinion", "society", "hanihealth", "sports", "culture"]
# donga 전용 매핑
category_mapping = {
    "Economy": "economy",
    "Opinion": "opinion",
    "Society": "society",
    "Health": "hanihealth",
    "Sports": "sports",
    "Culture": "culture",
    "Entertainment": "culture"
}
categories = ["economy", "opinion", "society", "hanihealth", "sports", "culture"]  # 연합뉴스 카테고리 예시
for category in categories:
    for i in range(1):  # 첫 페이지만
        headlines = fetch_headlines(category, i+1)

        if headlines:
            for idx, item in enumerate(headlines, start=1):
                print(f"@#@#@#@#@#@#기사 {idx}번 URL: {item['url']}")
                article = fetch_article_content(item['url'])

                if article:
                    print(f"카테고리: {category}, 페이지: {i+1}")
                    print(f"제목: {article['headline']}")
                    print(f"내용: {article['content']}")
                    print(f"보도일: {article['time']}")
                    print(f"출처: {article['source']}")
                    print(f"링크: {article['url']}\n")
                else:
                    print("✖ 기사 본문 크롤링 실패\n")

                time.sleep(0.5)
        else:
            print("✖ 기사 목록 크롤링 실패")
