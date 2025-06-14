package com.boot.service; // 서비스 클래스가 포함된 패키지 선언

import java.io.IOException; // 입출력 예외 처리를 위한 클래스
import java.util.ArrayList; // 리스트 객체 생성을 위한 클래스
import java.util.HashMap;
import java.util.List; // 리스트 타입 사용을 위한 인터페이스
import java.util.Map; // 결과 데이터를 키-값 형태로 다루기 위한 Map 인터페이스

import org.openkoreantext.processor.OpenKoreanTextProcessorJava;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import com.boot.elasticsearch.HangulComposer;
import com.boot.elasticsearch.KeyboardMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient; // Elasticsearch 클라이언트 클래스
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest; // 검색 요청 객체
import co.elastic.clients.elasticsearch.core.SearchResponse; // 검색 응답 객체
import co.elastic.clients.elasticsearch.core.explain.Explanation;
import co.elastic.clients.elasticsearch.core.explain.ExplanationDetail;
import co.elastic.clients.elasticsearch.core.search.Hit; // 검색 결과의 단일 항목 표현
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

	// 페이징처리를 위한 메소드
	public Map<String, Object> searchWithPagination(String keyword, String category, int page, int size,
			boolean skipProcessing) throws IOException {
		log.info("입력받은 keyword => " + keyword);
		String originalKeyword = keyword;

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

		// 6. 점수 기반 추정값 포함해서 결과 생성
		double headlineBoost = 5.0;
		double textrankBoost = 3.0;
		double summaryBoost = 1.0;
		double contentBoost = 0.5;
		double totalBoost = headlineBoost + textrankBoost + summaryBoost + contentBoost;

		List<Map<String, Object>> content = resp.hits().hits().stream().map(hit -> {
			Map<String, Object> doc = new HashMap<>(hit.source());
			double score = hit.score() != null ? hit.score() : 0.0;

			double headlineScore = extractScoreFromExplanation(hit.explanation(), "headline");
			double contentScore = extractScoreFromExplanation(hit.explanation(), "content");

//			double headlineScore = score * (headlineBoost / totalBoost);
//			double contentScore = score * (contentBoost / totalBoost);

			log.info("----------headlineScore => {}", headlineScore);
			log.info("contentScore => {}", contentScore);

			doc.put("score", score);
			doc.put("headlineScore", headlineScore);
			doc.put("contentScore", contentScore);
			return doc;
		}).toList();

		// 7. 최종 응답 리턴
		Map<String, Object> result = new HashMap<>();
		result.put("content", content);
		result.put("totalElements", totalHits);
		result.put("totalPages", totalPages);
		result.put("currentPage", page);
		result.put("originalKeyword", originalKeyword);
		result.put("convertedKeyword", keyword);
		return result;
	}

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

}