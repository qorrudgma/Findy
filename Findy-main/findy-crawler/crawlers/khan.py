import requests
import time

from bs4 import BeautifulSoup
from komoran import komoran # 형태소
from tfidf import tf_idf # TF-IDF
from textrank import textrank_keywords, textrank_summarize # TextRank
from mongo_save import save_to_mongodb # MongoDB

# 링크 추출
def fetch_headlines(category,page):
    f_url = f"https://www.khan.co.kr/{category}?page={page}"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    }

    try:
        response = requests.get(f_url, headers=headers)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, "html.parser")
        news_headlines = []

        articles = soup.select("div.list > ul > li")
        for article in articles:
            link_tag = article.select_one("a")
            if link_tag:
                url = link_tag.get("href")
                if url and not url.startswith("http"):
                    url = "https://www.khan.co.kr" + url
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
        headline_tags = soup.select("h1")
        if len(headline_tags) >= 2:
            headline = headline_tags[1].text.strip()
        elif len(headline_tags) == 1:
            headline = headline_tags[0].text.strip()
        else:
            headline = "제목 없음"

        # 내용
        content_tag = soup.select_one("div.art_body")
        content = content_tag.get_text(strip=True) if content_tag else "내용 없음"

        # 보도시간 추출
        date_tag = soup.select_one("div.date p")
        published_time = date_tag.get_text(strip=True) if date_tag else "시간 없음"
        time = clean_datetime(published_time)
        
        # 형태소
        nouns, pos_result = komoran(content)
        # TF-IDF
        tfidf_keywords = tf_idf(headline, content, pos_result, nouns)
        # TextRank
        textrank_kw = textrank_keywords(nouns)
        
        # 중요 내용
        sentences = [s.strip() for s in content.split('.') if len(s.strip()) > 10]
        summary_sentences = textrank_summarize(sentences, top_k=3)

        return {
            "headline": headline,
            "content": content,
            "tfidf_keywords": tfidf_keywords,
            "textrank_keywords": textrank_kw,
            "url": article_url,
            "category": category,
            "source": "khan",
            "summary": summary_sentences,
            "time": time
        }

    except Exception as e:
        print(f"본문 크롤링 오류: {e}")
        return None
    
def clean_datetime(text):
    text = text.replace("입력 ", "")
    time = text.replace(".", "-")
    return time

# 실행 흐름
# khan 전용 매핑
category_mapping = {
    "life/health/articles": "health"
}
# categories = ["economy", "opinion", "national", "life/health/articles", "culture"]
categories = ["economy"]
data = []
for category in categories:
    # 반복할 페이지 수
    for i in range(1):
        headlines = fetch_headlines(category, i+1)

        if headlines:
            for idx, item in enumerate(headlines, start=1):
                # print(f"@#@#@#@#@#@#기사 {idx}번 URL: {item['url']}")
                article = fetch_article_content(item['url'])

                if article:
                    # 출력전에 교체
                    converted_category = category_mapping.get(category, category)

                    data.append(article)
                else:
                    print("기사 본문 크롤링 실패\n")

                time.sleep(0.5)
        else:
            print("1번 크롤러 실패.")

# 디비 저장
save_to_mongodb(data)