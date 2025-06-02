import requests
import time

from bs4 import BeautifulSoup

# 링크 추출
def fetch_headlines(category,page):
    page = (page*10)+1
    f_url = f"https://www.donga.com/news/{category}?p={page}&prod=news&ymd=&m="
    # f_url = "https://www.donga.com/news/{0}?p={1}&prod=news&ymd=&m=".format(category, page)
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    }

    try:
        response = requests.get(f_url, headers=headers)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, "html.parser")
        news_headlines = []

        articles = soup.select("div.news_body")
        for article in articles:
            link_tag = article.select_one("a")
            if link_tag:
                url = link_tag.get("href")
                if url and not url.startswith("http"):
                    url = "https://www.donga.com" + url
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
        # 모든 h1 태그를 리스트로 받음
        headline_tags = soup.select("h1")

        # 두 번째 h1이 있으면 가져오고, 없으면 첫 번째 또는 기본값 사용
        if len(headline_tags) >= 2:
            headline = headline_tags[1].text.strip()
        elif len(headline_tags) == 1:
            headline = headline_tags[0].text.strip()
        else:
            headline = "제목 없음"

        # 텍스트 내용이 태그들과 있어서 내용만 뽑아내기
        content_tag = soup.select_one("section.news_view")
        if content_tag:
            clean_text = content_tag.get_text(separator=' ', strip=True)

        # 보도시간 추출
        date_items = soup.select('span[aria-hidden="true"]')
        last_time = date_items[-1].text.strip()

        return {
            "headline": headline,
            "content": clean_text,
            # "content": clean_text,
            "url": article_url,
            "source": "hani",
            "time": last_time
        }

    except Exception as e:
        print(f"본문 크롤링 오류: {e}")
        return None

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
# categories = ["Economy", "Opinion", "Society", "Health", "Sports", "Culture", "Entertainment"]
# categories = ["Economy", "Opinion", "Society", "Health"]
categories = ["Economy"]
for category in categories:
    # 반복할 페이지 수
    for i in range(1):
        headlines = fetch_headlines(category, i)

        if headlines:
            for idx, item in enumerate(headlines, start=1):
                print(f"@#@#@#@#@#@#기사 {idx}번 URL: {item['url']}")
                article = fetch_article_content(item['url'])

                if article:
                    # 출력전에 교체
                    converted_category = category_mapping.get(category, category)
                    print(f"카테고리: {category}, 페이지: {i+1}")
                    print(f" 제목: {article['headline']}")
                    print(f" 내용: {article['content']}")
                    print(f" 보도일: {article['time']}")
                    print(f" 출처: {article['source']}")
                    print(f" 링크: {article['url']}\n")
                else:
                    print("기사 본문 크롤링 실패\n")

                time.sleep(0.5)
        else:
            print("1번 크롤러 실패.")