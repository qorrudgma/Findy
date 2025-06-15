package com.boot.service; // 서비스 클래스가 포함된 패키지 선언

import java.io.IOException; // 입출력 예외 처리를 위한 클래스
import java.util.ArrayList; // 리스트 객체 생성을 위한 클래스
import java.util.Comparator;
import java.util.HashMap;
import java.util.List; // 리스트 타입 사용을 위한 인터페이스
import java.util.Map; // 결과 데이터를 키-값 형태로 다루기 위한 Map 인터페이스
import java.util.stream.Collectors;

import org.openkoreantext.processor.OpenKoreanTextProcessorJava;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import com.boot.dto.KeywordCountDto;
import com.boot.dto.NewsCountDto;
import com.boot.elasticsearch.HangulComposer;
import com.boot.elasticsearch.KeyboardMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient; // Elasticsearch 클라이언트 클래스
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest; // 검색 요청 객체
import co.elastic.clients.elasticsearch.core.SearchResponse; // 검색 응답 객체
import co.elastic.clients.elasticsearch.core.explain.Explanation;
import co.elastic.clients.elasticsearch.core.explain.ExplanationDetail;
import co.elastic.clients.elasticsearch.core.search.Hit; // 검색 결과의 단일 항목 표현
import co.elastic.clients.json.JsonData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ElasticService {
	private final ElasticsearchClient client;
	private final ElasticsearchOperations operations;
	private final KeyboardMapper keyboardMapper;
	private final HangulComposer hangulComposer;

	public ElasticService(ElasticsearchClient client, ElasticsearchOperations operations, KeyboardMapper keyboardMapper,
			HangulComposer hangulComposer) {
		this.client = client;
		this.operations = operations;
		this.keyboardMapper = keyboardMapper;
		this.hangulComposer = hangulComposer;
	}

	private List<Map<String, Object>> extractHits(SearchResponse<Map> response) {
		List<Map<String, Object>> results = new ArrayList<>();
		for (Hit<Map> hit : response.hits().hits()) {
			results.add(hit.source());
		}
		return results;
	}

	// 검색을 위한 메소드
	public Map<String, Object> searchWithPagination(String keyword, String category, int page, int size,
			boolean skipProcessing) throws IOException {
		log.info("입력받은 keyword => " + keyword);
		String originalKeyword = keyword;
		String convertedKeyword = "";

		// 1. 키워드가 없을 경우 전체 검색
		if (keyword == null || keyword.isBlank()) {
			SearchRequest.Builder requestBuilder = new SearchRequest.Builder().index("newsdata.newsdata")
					.from(page * size).size(size);

			if (category != null && !category.isBlank()) {
				requestBuilder.query(q -> q.term(t -> t.field("category.keyword").value(category)));
			} else {
				requestBuilder.query(q -> q.matchAll(m -> m));
			}

			SearchResponse<Map> resp = client.search(requestBuilder.build(), Map.class);
			long totalHits = resp.hits().total() != null ? resp.hits().total().value() : 0;
			int totalPages = (int) Math.ceil((double) totalHits / size);
			List<Map> content = resp.hits().hits().stream().map(Hit::source).toList();

			return Map.of("content", content, "totalElements", totalHits, "totalPages", totalPages, "currentPage",
					page);
		}

		// 2. 키워드가 존재할 경우 처리
		if (!skipProcessing) {
			boolean containsHangul = keyword.matches(".*[가-힣]+.*");
			boolean isEnglishOnly = keyword.matches("^[A-Za-z]+$");

			if (!containsHangul && (!isEnglishOnly || keyword.length() >= 4)) {
				String converted = keyboardMapper.convertEngToKor(keyword);
				String patched = hangulComposer.combine(converted);
				log.info("한영키 변환 => {}", converted);
				log.info("한글 패치 => {}", patched);
				keyword = patched;
				convertedKeyword = patched;
			}
		}

		// 3. 형태소 분석 + 조합어 생성
		CharSequence normalized = OpenKoreanTextProcessorJava.normalize(keyword);
		var tokens = OpenKoreanTextProcessorJava.tokenize(normalized);
		List<String> tokenList = OpenKoreanTextProcessorJava.tokensToJavaStringList(tokens);
		List<String> combined = new ArrayList<>(tokenList);

		for (int i = 0, n = tokenList.size(); i < n; i++) {
			int len = 0;
			var sb = new StringBuilder();
			for (int j = i; j < n; j++) {
				String tok = tokenList.get(j);
				if (len + tok.length() > 3) {
					break;
				}
				sb.append(tok);
				len += tok.length();
				if (len >= 2) {
					combined.add(sb.toString());
				}
			}
		}

		combined = combined.stream().distinct().toList();
		log.info("검색에 쓰일 단어 => {}", combined);

		// 4. BoolQuery 구성
		BoolQuery.Builder boolB = new BoolQuery.Builder();
		for (String term : combined) {
			boolB.should(s -> s.match(m -> m.field("headline").query(term).fuzziness("0").boost(5.0f)));
			boolB.should(s -> s.match(m -> m.field("textrank_keywords").query(term).fuzziness("1").boost(3.0f)));
			boolB.should(s -> s.match(m -> m.field("summary").query(term).fuzziness("1").boost(1.0f)));
			boolB.should(s -> s.match(m -> m.field("content").query(term).fuzziness("1").boost(0.5f)));
		}

		if (category != null && !category.isBlank()) {
			boolB.filter(f -> f.term(t -> t.field("category.keyword").value(category)));
		}

		// 5. 검색 실행
		SearchRequest request = SearchRequest.of(b -> b.index("newsdata.newsdata").from(page * size).size(size)
				.query(q -> q.bool(boolB.build())).explain(true));

		SearchResponse<Map> resp = client.search(request, Map.class);
		long totalHits = resp.hits().total() != null ? resp.hits().total().value() : 0;
		int totalPages = (int) Math.ceil((double) totalHits / size);

		List<Map<String, Object>> content = resp.hits().hits().stream().map(hit -> {
			Map<String, Object> doc = new HashMap<>(hit.source());
			doc.put("id", hit.id());
			double score = hit.score() != null ? hit.score() : 0.0;

			double headlineScore = extractScoreFromExplanation(hit.explanation(), "headline");
			double contentScore = extractScoreFromExplanation(hit.explanation(), "content");
			// 내용우선순위 일때 그냥 안에 내용 빈도수로 점수로 하는건 좀 무리일 듯 싶어 추가적으로 조건이 들어가야할 듯
//			contentScore = contentScore + headlineScore;

//			log.info("----------headlineScore => {}", headlineScore);
//			log.info("contentScore => {}", contentScore);

			doc.put("score", score);
			doc.put("headlineScore", headlineScore);
			doc.put("contentScore", contentScore);
			return doc;
		}).toList();

		// 변환된 키워드로 결과가 없으면 원본으로 재시도
//		if (totalHits == 0 && !originalKeyword.equals(keyword)) {
//			log.info("변환된 키워드 결과 없음, 원래 키워드로 재검색 시도");
//
//			return searchWithPagination(originalKeyword, category, page, size, true);
//		}

		// 7. 최종 응답 리턴
		Map<String, Object> result = new HashMap<>();
		result.put("content", content);
		result.put("totalElements", totalHits);
		result.put("totalPages", totalPages);
		result.put("currentPage", page);
		result.put("originalKeyword", originalKeyword);
		result.put("convertedKeyword", keyword);

//		log.info(originalKeyword);
//		log.info(keyword);

		// 인기 뉴스/키워드 로그 저장 (Top 30 기준)
		logPopularNewsAndKeywords(content);

		return result;
	}

	// 검색결과 우선순위 위한 메소드
	private double extractScoreFromExplanation(Explanation explanation, String field) {
		if (explanation == null) {
			return 0.0;
		}
		double sum = 0.0;

		if (explanation.description() != null && explanation.description().toLowerCase().contains(field)) {
			sum += explanation.value();
		}

		if (explanation.details() != null) {
			for (ExplanationDetail detail : explanation.details()) {
				sum += extractScoreFromExplanationDetail(detail, field);
			}
		}

		return sum;
	}

	// 검색결과 우선순위 위한 메소드
	private double extractScoreFromExplanationDetail(ExplanationDetail detail, String field) {
		double sum = 0.0;

		if (detail.description() != null && detail.description().toLowerCase().contains(field)) {
			sum += detail.value();
		}

		if (detail.details() != null) {
			for (ExplanationDetail sub : detail.details()) {
				sum += extractScoreFromExplanationDetail(sub, field);
			}
		}

		return sum;
	}

	// 키워드 순위
	public List<String> topKeywords(int size) throws IOException {// terms 집계 두 개 요청 (전체 문서 대상)
		SearchRequest req = new SearchRequest.Builder().index("newsdata.newsdata") // 인덱스명 맞추기
				.size(0)
				// 전체 문서
				.query(q -> q.matchAll(m -> m))
				// 조건 필요하면적기 시간으로 하기엔 시간 이 일정한게 아니라 형식이 보류해둠
//		      .query(q -> q
//		          .range(r -> r.field("time").gte(JsonData.of("now-" + days + "d/d")).lte(JsonData.of("now"))))
				.aggregations("top_keywords", agg -> agg.terms(t -> t.field("textrank_keywords.keyword").size(size)))
				.aggregations("top_tfidf", agg -> agg.terms(t -> t.field("tfidf_keywords.keyword").size(size))).build();

		SearchResponse<Void> res = client.search(req, Void.class);

		// 각 집계
		Map<String, Long> textrankMap = res.aggregations().get("top_keywords").sterms().buckets().array().stream()
				.collect(Collectors.toMap(b -> b.key().stringValue(), b -> b.docCount()));

		Map<String, Long> tfidfMap = res.aggregations().get("top_tfidf").sterms().buckets().array().stream()
				.collect(Collectors.toMap(b -> b.key().stringValue(), b -> b.docCount()));

		// 교집합 만들기
		List<String> common = textrankMap.keySet().stream().filter(tfidfMap::containsKey) // 교집합
				.sorted(Comparator.comparingLong((String k) -> textrankMap.get(k) + tfidfMap.get(k)).reversed())
				.limit(size) // 최종 size개
				.toList();

		return common; // List<String> (키워드만) 반환
	}

	// 인기 뉴스/키워드 로그 저장 (Top 30 기준)
	public void logPopularNewsAndKeywords(List<Map<String, Object>> content) throws IOException {
		List<Map<String, Object>> topNews = content.stream().limit(30).toList();
		String now = java.time.Instant.now().toString();

		for (Map<String, Object> doc : topNews) {
			// 하나 문제 생겨도 강행
			if (!doc.containsKey("id") || !doc.containsKey("url") || doc.get("id") == null || doc.get("url") == null) {
				continue;
			}
			String newsId = doc.get("id").toString();
			String url = doc.get("url").toString();

			// popular_news_logs 인덱스에 저장
			client.index(i -> i.index("popular_news_logs")
					.document(Map.of("news_id", newsId, "url", url, "timestamp", now)));

			// textrank_keywords, tfidf_keywords 필드에서 키워드 추출
			Object textrankObj = doc.get("textrank_keywords");
			Object tfidfObj = doc.get("tfidf_keywords");

			List<String> textrankKeywords = textrankObj instanceof List<?>
					? ((List<?>) textrankObj).stream().map(Object::toString).toList()
					: List.of();
			List<String> tfidfKeywords = tfidfObj instanceof List<?>
					? ((List<?>) tfidfObj).stream().map(Object::toString).toList()
					: List.of();

			// [3] 중복 제거 후 각 키워드별로 popular_keywords_logs 저장
			List<String> combinedKeywords = new ArrayList<>();
			combinedKeywords.addAll(textrankKeywords);
			combinedKeywords.addAll(tfidfKeywords);
			combinedKeywords = combinedKeywords.stream().distinct().toList();

			for (String kw : combinedKeywords) {
				client.index(i -> i.index("popular_keywords_logs").document(Map.of("keyword", kw, "timestamp", now)));
			}
		}
	}

	// 오늘 하루 기준, 인기 키워드 Top size
	public List<KeywordCountDto> getTopPopularKeywordsOfToday(int size) throws IOException {
		String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC) + "T00:00:00Z";

		SearchRequest req = new SearchRequest.Builder().index("popular_keywords_logs").size(0)
				.query(q -> q.range(r -> r.field("timestamp").gte(JsonData.of(today))))
				.aggregations("top_keywords", agg -> agg.terms(t -> t.field("keyword").size(size))).build();

		SearchResponse<Void> res = client.search(req, Void.class);

		return res.aggregations().get("top_keywords").sterms().buckets().array().stream()
				.map(b -> new KeywordCountDto(b.key().stringValue(), b.docCount())).toList();
	}

	// 오늘 하루 기준, 인기 뉴스 Top size
	public List<NewsCountDto> getTopPopularNewsOfToday(int size) throws IOException {
		String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC) + "T00:00:00Z";

		SearchRequest req = new SearchRequest.Builder().index("popular_news_logs").size(0)
				.query(q -> q.range(r -> r.field("timestamp").gte(JsonData.of(today))))
				.aggregations("top_news", agg -> agg.terms(t -> t.field("news_id").size(size))).build();

		SearchResponse<Void> res = client.search(req, Void.class);

		List<NewsCountDto> result = new ArrayList<>();

		for (var bucket : res.aggregations().get("top_news").sterms().buckets().array()) {
			String newsId = bucket.key().stringValue();
			long count = bucket.docCount();

			// news_id 기준으로 URL 하나 가져오기 (최신 1건)
			SearchResponse<Map> sub = client.search(s -> s.index("popular_news_logs").size(1)
					.query(q -> q.term(t -> t.field("news_id").value(newsId)))
					.sort(srt -> srt.field(
							f -> f.field("timestamp").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))),
					Map.class);

			var hits = sub.hits().hits();
			String url = sub.hits().hits().isEmpty() ? "" : sub.hits().hits().get(0).source().get("url").toString();

			result.add(new NewsCountDto(newsId, url, count));
		}

		return result;
	}

}