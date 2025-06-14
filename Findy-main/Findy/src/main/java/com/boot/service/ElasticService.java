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

		// 1) keyword가 없을 때
		if (keyword == null || keyword.isBlank()) {
			SearchRequest.Builder requestBuilder = new SearchRequest.Builder().index("newsdata.newsdata")
					.from(page * size).size(size);

			if (category != null && !category.isBlank()) {
				requestBuilder.query(q -> q.term(t -> t.field("category.keyword").value(category)));
			} else {
				requestBuilder.query(q -> q.matchAll(m -> m));
			}

			// 검색 실행
			SearchResponse<Map> resp = client.search(requestBuilder.build(), Map.class);
			long totalHits = resp.hits().total() != null ? resp.hits().total().value() : 0;
			int totalPages = (int) Math.ceil((double) totalHits / size);
			List<Map> content = resp.hits().hits().stream().map(Hit::source).toList();

			return Map.of("content", content, "totalElements", totalHits, "totalPages", totalPages, "currentPage",
					page);
		}

		if (!skipProcessing) {
			// 한글 포함 여부 체크
			boolean containsHangul = keyword.matches(".*[가-힣]+.*");
			// 오직 영어 알파벳만으로 이뤄졌는지
			boolean isEnglishOnly = keyword.matches("^[A-Za-z]+$");

			if (containsHangul) {
				// 이미 한글이 포함된 경우
				log.info("한글 포함 변환 생략 => " + keyword);

			} else if (isEnglishOnly && keyword.length() < 4) {
				// 영어만인데, 4글자 미만인 경우
				log.info("영어 키워드(길이 < 4) 변환 생략 => " + keyword);

			} else {
				// 그 외(혼합어 혹은 영어 4글자 이상)는 한영키 변환 + 합치기
				String converted = keyboardMapper.convertEngToKor(keyword);
				log.info("한영키 변환 => " + converted);

				String patched = hangulComposer.combine(converted);
				log.info("한글 패치 => " + patched);

				keyword = patched;
			}
			log.info("한영키 변환 거친 keyword => " + keyword);
		}
		// 2) keyword가 있을 때
		BoolQuery.Builder boolB = new BoolQuery.Builder();

		// combined 리스트 생성
		CharSequence normalized = OpenKoreanTextProcessorJava.normalize(keyword);
		var tokens = OpenKoreanTextProcessorJava.tokenize(normalized);
		List<String> tokenList = OpenKoreanTextProcessorJava.tokensToJavaStringList(tokens);
		log.info("tokenList => " + tokenList);
		List<String> combined = new ArrayList<>(tokenList);
		// 단어 리스트 출력
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
		log.info("검색에 쓰일 단어 => " + combined);
		for (String term : combined) {
			// 제목 가중치
//			Fuzziness.fromEdits(1)
			boolB.should(s -> s.match(m -> m.field("headline").query(term).fuzziness("1").boost(5.0f)));
			// 키워드 가중치
			boolB.should(s -> s.match(m -> m.field("textrank_keywords").query(term).fuzziness("1").boost(3.0f)));
			// 중요 문단 가중치
			boolB.should(s -> s.match(m -> m.field("summary").query(term).fuzziness("1").boost(1.0f)));
			// 내용 가중치
			boolB.should(s -> s.match(m -> m.field("content").query(term).fuzziness("1").boost(0.5f)));
		}

		// 카테고리 필터
		if (category != null && !category.isBlank()) {
			boolB.filter(f -> f.term(t -> t.field("category.keyword").value(category)));
		}

		// 3) keyword 있을 때의 검색 요청
		SearchRequest request = SearchRequest
				.of(b -> b.index("newsdata.newsdata").from(page * size).size(size).query(q -> q.bool(boolB.build())));

		// 4) 검색 실행 & 결과 포장
		SearchResponse<Map> resp = client.search(request, Map.class);
		long totalHits = resp.hits().total() != null ? resp.hits().total().value() : 0;
		int totalPages = (int) Math.ceil((double) totalHits / size);
		List<Map> content = resp.hits().hits().stream().map(Hit::source).toList();

		Map<String, Object> result = new HashMap<>();

		result.put("content", content); // 내용
		result.put("totalElements", totalHits);
		result.put("totalPages", totalPages);
		result.put("currentPage", page);
		result.put("originalKeyword", originalKeyword); // 입력한 원래 검색어
		result.put("convertedKeyword", keyword); // 최종 변환된 검색어
//		result.put("tokenList", tokenList); // 형태소 분석 결과
//		result.put("combinedTerms", combined); // 조합된 검색어 리스트

//		return Map.of("content", content, "totalElements", totalHits, "totalPages", totalPages, "currentPage", page);
		return result;
	}
}