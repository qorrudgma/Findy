import requests
import time
from bs4 import BeautifulSoup

def fetch_headlines(category,page):
    f_url = f"https://www.hani.co.kr/arti/{category}?page={page}"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    }

    try:
        response = requests.get(f_url, headers=headers)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, "html.parser")
        news_headlines = []

        articles = soup.select("li.ArticleList_item___OGQO")
        for article in articles:
            link_tag = article.select_one("a.BaseArticleCard_link__Q3YFK")
            if link_tag:
                url = link_tag.get("href")
                if url and not url.startswith("http"):
                    url = "https://www.hani.co.kr" + url
                news_headlines.append({"url": url})
        return news_headlines

    except Exception as e:
        print(f"오류 발생: {e}")
        return []

# 기사 내용 추출 함수
def fetch_article_content(article_url):
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    }

    try:
        response = requests.get(article_url, headers=headers)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, "html.parser")

        # 제목 추출
        headline_tag = soup.select_one("h3.ArticleDetailView_title__9kRU_")
        # 텍스트 클래스가 여러개라 여러개를 합치는 작업
        content_tag = soup.select("p.text")
        paragraphs = [tag.get_text(strip=True) for tag in content_tag]
        full_text = " ".join(paragraphs)
        # full_text = "!@#$!@#$".join(paragraphs)
        headline = headline_tag.text.strip() if headline_tag else "제목 없음"

        return {
            "headline": headline,
            "content": full_text,
            "url": article_url
        }

    except Exception as e:
        print(f"본문 크롤링 오류: {e}")
        return None

# 실행 흐름
# categories = ["economy", "opinion", "society", "hanihealth", "sports", "culture"]
categories = ["economy", "opinion", "society", "hanihealth"]
for category in categories:
    for i in range(3):
        headlines = fetch_headlines(category, i+1)

        if headlines:
            print("URL 리스트 가져옴\n")

            for idx, item in enumerate(headlines, start=1):
                print(f"@#@#@#@#@#@#기사 {idx}번 URL: {item['url']}")
                article = fetch_article_content(item['url'])

                if article:
                    print(f"카테고리: {category}, 페이지: {i+1}")
                    print(f"제목: {article['headline']}")
                    print(f"내용: {article['content']}")
                    print(f"링크: {article['url']}\n")
                else:
                    print("기사 본문 크롤링 실패\n")

                time.sleep(0.5)
        else:
            print("1번 크롤러 실패.")