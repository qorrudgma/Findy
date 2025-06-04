from pymongo import MongoClient

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
            print(f" !@#$ url: {article['url']}\n")
            url = article.get('url')
            if not url:
                skipped += 1
                continue

            unique_post_ids.add(url)

            result = collection.update_one(
                {"url": url},
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