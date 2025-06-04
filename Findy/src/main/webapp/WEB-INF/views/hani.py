import requests
import time

from bs4 import BeautifulSoup
from konlpy.tag import Komoran
from sklearn.feature_extraction.text import TfidfVectorizer
from collections import defaultdict
from mongo_save import save_to_mongodb
from textrank import textrank_keywords, textrank_summarize

def fetch_headlines(category, page):
    f_url = f"https://www.hani.co.kr/arti/{category}?page={page}"
    headers = {"User-Agent": "Mozilla/5.0"}
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
    headers = {"User-Agent": "Mozilla/5.0"}
    try:
        response = requests.get(article_url, headers=headers)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, "html.parser")

        # 제목
        headline_tag = soup.select_one("h3.ArticleDetailView_title__9kRU_")
        headline = headline_tag.text.strip() if headline_tag else "제목 없음"

        # 내용
        content_tag = soup.select("p.text")
        paragraphs = [tag.get_text(strip=True) for tag in content_tag[:-1]]
        full_text = " ".join(paragraphs)

        # 형태소
        komoran = Komoran()
        pos_result = komoran.pos(full_text)
        nouns = [word for word, tag in pos_result if tag in ['NNG', 'NNP'] and len(word) > 1]

        sentences = full_text.split('.')
        first_sentence = sentences[1] if len(sentences) > 1 else ""
        last_sentence = sentences[-1]
        position_weights = defaultdict(float)

        for word, tag in pos_result:
            if len(word) <= 1 or tag not in ['NNG', 'NNP']:
                continue
            if word in headline:
                position_weights[word] += 2.0
            if word in first_sentence:
                position_weights[word] += 1.5
            if word in last_sentence:
                position_weights[word] += 1.0
            if tag == 'NNP':
                position_weights[word] += 1.0

        vectorizer = TfidfVectorizer()
        tfidf_matrix = vectorizer.fit_transform([" ".join(nouns)])
        feature_names = vectorizer.get_feature_names_out()
        tfidf_scores = tfidf_matrix.toarray()[0]

        tfidf_keywords = []
        for word, score in zip(feature_names, tfidf_scores):
            final_score = score + position_weights.get(word, 0)
            tfidf_keywords.append((word, final_score))
        tfidf_keywords = sorted(tfidf_keywords, key=lambda x: x[1], reverse=True)

        textrank_kw = textrank_keywords(nouns)

        # 날짜 정보 파싱 (오류 방지 로직 추가)
        time_str = "알 수 없음"
        date_items = soup.select("li.ArticleDetailView_dateListItem__mRc3d")
        for item in date_items:
            if "등록" in item.text:
                time_tag = item.find("span")
                if time_tag:
                    time_str = time_tag.text.strip()

        sentences = [s.strip() for s in full_text.split('.') if len(s.strip()) > 10]
        summary_sentences = textrank_summarize(sentences, top_k=3)

        return {
            "headline": headline,
            "content": full_text,
            "tfidf_keywords": tfidf_keywords[:10],
            "textrank_keywords": textrank_kw,
            "url": article_url,
            "source": "hani",
            "summary": summary_sentences,
            "time": time_str
        }

    except Exception as e:
        print(f"본문 크롤링 오류: {e}")
        return None

# 실행
categories = ["economy"]
data = []
for category in categories:
    for i in range(1):
        headlines = fetch_headlines(category, i + 1)
        if headlines:
            for idx, item in enumerate(headlines, start=1):
                # print(f"기사 {idx}번 URL: {item['url']}")
                article = fetch_article_content(item['url'])
                if article:
                    data.append(article)
                else:
                    print("기사 본문 크롤링 실패")
                time.sleep(0.5)
        else:
            print("크롤링 실패")

save_to_mongodb(data)