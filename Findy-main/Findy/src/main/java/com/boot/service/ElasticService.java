package com.boot.service; // 서비스 클래스가 포함된 패키지 선언

import java.io.IOException; // 입출력 예외 처리를 위한 클래스
import java.time.Instant;
import java.util.ArrayList; // 리스트 객체 생성을 위한 클래스
import java.util.Comparator;
import java.util.HashMap;
import java.util.List; // 리스트 타입 사용을 위한 인터페이스
import java.util.Map; // 결과 데이터를 키-값 형태로 다루기 위한 Map 인터페이스
import java.util.stream.Collectors;

import org.openkoreantext.processor.OpenKoreanTextProcessorJava;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.boot.elasticsearch.HangulComposer;
import com.boot.elasticsearch.KeyboardMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient; // Elasticsearch 클라이언트 클래스
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest; // 검색 요청 객체
import co.elastic.clients.elasticsearch.core.SearchResponse; // 검색 응답 객체
import co.elastic.clients.elasticsearch.core.explain.Explanation;
import co.elastic.clients.elasticsearch.core.explain.ExplanationDetail;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit; // 검색 결과의 단일 항목 표현
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.NamedValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ElasticService {
	private final ElasticsearchClient client;
	private final ElasticsearchOperations operations;
	private final KeyboardMapper keyboardMapper;
	private final HangulComposer hangulComposer;
	private final GeminiService geminiService;

	public ElasticService(ElasticsearchClient client, ElasticsearchOperations operations, KeyboardMapper keyboardMapper,
			HangulComposer hangulComposer, GeminiService geminiService) {
		this.client = client;
		this.operations = operations;
		this.keyboardMapper = keyboardMapper;
		this.hangulComposer = hangulComposer;
		this.geminiService = geminiService;
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
			List<Map<String, Object>> content = resp.hits().hits().stream().map(hit -> {
				Map<String, Object> map = new HashMap<>(hit.source());

				// textrank_keywords + tfidf_keywords 병합
				List<String> combined = new ArrayList<>();
				Object textrank = map.get("textrank_keywords");
				Object tfidf = map.get("tfidf_keywords");

				if (textrank instanceof List<?>) {
					combined.addAll(((List<?>) textrank).stream().map(Object::toString).toList());
				}
				if (tfidf instanceof List<?>) {
					combined.addAll(((List<?>) tfidf).stream().map(Object::toString).toList());
				}

				map.put("keywords", combined.stream().distinct().toList());

				return map;
			}).toList();

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

			// 키워드 병합 처리
			List<String> combinedKeywords = new ArrayList<>();
			Object textrank = doc.get("textrank_keywords");
			Object tfidf = doc.get("tfidf_keywords");

			if (textrank instanceof List<?>) {
				combinedKeywords.addAll(((List<?>) textrank).stream().map(Object::toString).toList());
			}

			if (tfidf instanceof List<?>) {
				combinedKeywords.addAll(((List<?>) tfidf).stream().map(Object::toString).toList());
			}

			// 중복 제거 후 저장
			doc.put("keywords", combinedKeywords.stream().distinct().toList());

			return doc;
		}).toList();

		// 변환된 키워드로 결과가 없으면 원본으로 재시도
//		if (totalHits == 0 && !originalKeyword.equals(keyword)) {
//			log.info("변환된 키워드 결과 없음, 원래 키워드로 재검색 시도");
//
//			return searchWithPagination(originalKeyword, category, page, size, true);
//		}
//		Gemini AI 요약 결과 가져오기
//		String aiSummary = Gemini(keyword);

		// 7. 최종 응답 리턴
		Map<String, Object> result = new HashMap<>();
		result.put("content", content);
		result.put("totalElements", totalHits);
		result.put("totalPages", totalPages);
		result.put("currentPage", page);
		result.put("originalKeyword", originalKeyword);
		result.put("convertedKeyword", keyword);
		// result에 AI 요약 결과 포함시켜서 프론트에 함께 전달
//		result.put("aiSummary", aiSummary); // 프론트에서 사용: data.aiSummary

//		log.info(originalKeyword);
//		log.info(keyword);

		return result;
	}

//	Gemini AI 요약 결과 가져오기
	private String Gemini(String keyword) {
		String aiSummary = geminiService.getSummary(keyword);
		log.info(" Gemini 요약 결과: {}", aiSummary);

		return aiSummary;
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

	// 클릭시 뉴스 및 키워드 카운트
	public void logPopularNewsAndKeywordsByUrlAndKeywords(String url, List<String> keywords) throws IOException {
		log.info("url: {}", url);

		String now = Instant.now().toString();

		// 인기 뉴스 저장
		try {
			client.index(i -> i.index("popular_news_logs").document(Map.of("url", url, "timestamp", now)));
			log.info("뉴스 저장 성공: {}", url);
		} catch (Exception e) {
			log.error("뉴스 저장 실패: {}", e.getMessage(), e);
		}

		// 키워드들 저장
//		if (keywords != null) {
		for (String kw : keywords.stream().distinct().toList()) {
			client.index(i -> i.index("popular_keywords_logs").document(Map.of("keyword", kw, "timestamp", now)));
		}
//		}
	}

	// 뉴스 및 키워드 자동 삭제
//	@Scheduled(cron = "0 * * * * *") // 매일 (00:00)
	@Scheduled(cron = "0 0 0 * * *") // 매일 (00:00)
	public void deleteOldClickLogs() throws IOException {
		// popular_keywords_logs 에서 하루 전 로그 삭제
		client.deleteByQuery(r -> r.index("popular_keywords_logs")
				.query(q -> q.range(rq -> rq.field("timestamp").lt(JsonData.of("now/d")))));

		// popular_news_logs 에서 하루 전 로그 삭제
		client.deleteByQuery(r -> r.index("popular_news_logs")
				.query(q -> q.range(rq -> rq.field("timestamp").lt(JsonData.of("now/d")))));

		log.info("뉴스 및 키워드 자동 삭제 완료");
	}

	// Top5 뉴스 url 가져오기
	public List<String> getTop5NewsUrls() throws IOException {
		SearchResponse<Void> response = client.search(
				s -> s.index("popular_news_logs").size(0).aggregations("top_urls",
						agg -> agg.terms(
								t -> t.field("url").size(5).order(List.of(NamedValue.of("_count", SortOrder.Desc))))),
				Void.class);

		return response.aggregations().get("top_urls").sterms().buckets().array().stream()
				.map(bucket -> bucket.key().stringValue()).toList();
	}

	// url로 뉴스 내용 가져오기
	public List<Map<String, Object>> getNewsDetail(List<String> urls) throws IOException {
		List<Map<String, Object>> result = new ArrayList<>();

		for (String url : urls) {
			SearchResponse<Map> resp = client.search(s -> s.index("newsdata.newsdata")
					.query(q -> q.term(t -> t.field("url.keyword").value(url))).size(1), Map.class);

			List<Hit<Map>> hits = resp.hits().hits();
			if (!hits.isEmpty()) {
				Map src = hits.get(0).source();
				Map<String, Object> news = new HashMap<>();
				news.put("url", url);
				news.put("headline", src.get("headline"));
				news.put("source", src.get("source"));
				result.add(news);
			}
		}

		return result;
	}

	// 자동완성
	public List<String> getAutocompleteSuggestions(String prefix) throws IOException {
		SearchResponse<Void> response = client.search(
				s -> s.index("unique_keywords")
						.suggest(sg -> sg.suggesters("suggestion",
								st -> st.prefix(prefix).completion(c -> c.field("keyword_suggest").size(10)))),
				Void.class);

		List<Suggestion<Void>> suggestions = response.suggest().get("suggestion");

		return suggestions.stream().flatMap(suggestion -> suggestion.completion().options().stream())
				.map(CompletionSuggestOption::text).collect(Collectors.toList());
	}

}