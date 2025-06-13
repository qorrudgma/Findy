package com.boot.service; // 서비스 클래스가 포함된 패키지 선언

import java.io.IOException; // 입출력 예외 처리를 위한 클래스
import java.util.ArrayList; // 리스트 객체 생성을 위한 클래스
import java.util.List; // 리스트 타입 사용을 위한 인터페이스
import java.util.Map; // 결과 데이터를 키-값 형태로 다루기 위한 Map 인터페이스
import java.util.stream.Collectors;

import org.openkoreantext.processor.OpenKoreanTextProcessorJava;
import org.openkoreantext.processor.tokenizer.KoreanTokenizer;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import com.boot.elasticsearch.EnglishDictionary;
import com.boot.elasticsearch.HangulComposer;
import com.boot.elasticsearch.KeyboardMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient; // Elasticsearch 클라이언트 클래스
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest; // 검색 요청 객체
import co.elastic.clients.elasticsearch.core.SearchResponse; // 검색 응답 객체
import co.elastic.clients.elasticsearch.core.search.Hit; // 검색 결과의 단일 항목 표현
import lombok.extern.slf4j.Slf4j;
import scala.collection.Seq;

@Slf4j
@Service
public class ElasticService {
	private final ElasticsearchClient client;
	private final ElasticsearchOperations operations;
	private final KeyboardMapper keyboardMapper;
	private final HangulComposer hangulComposer;
	private final EnglishDictionary englishDictionary;

	public ElasticService(ElasticsearchClient client, ElasticsearchOperations operations, KeyboardMapper keyboardMapper,
			HangulComposer hangulComposer, EnglishDictionary englishDictionary) {
		this.client = client;
		this.operations = operations;
		this.keyboardMapper = keyboardMapper;
		this.hangulComposer = hangulComposer;
		this.englishDictionary = englishDictionary;
	}

	private List<Map<String, Object>> extractHits(SearchResponse<Map> response) {
		List<Map<String, Object>> results = new ArrayList<>();
		for (Hit<Map> hit : response.hits().hits()) {
			results.add(hit.source());
		}
		return results;
	}

	// 페이징처리를 위한 메소드
	public Map<String, Object> searchWithPagination(String keyword, String category, int page, int size) {
		try {
			SearchRequest.Builder requestBuilder = new SearchRequest.Builder().index("newsdata.newsdata")
					.from(page * size).size(size);

			// 검색 조건 구성
			if (keyword != null && !keyword.isEmpty() && category != null && !category.isEmpty()) {
				requestBuilder.query(q -> q.bool(b -> b.must(m1 -> m1.match(m -> m.field("headline").query(keyword)))
						.filter(f -> f.term(t -> t.field("category.keyword").value(category)))));
			} else if (keyword != null && !keyword.isEmpty()) {
				requestBuilder.query(q -> q.match(m -> m.field("headline").query(keyword)));
			} else if (category != null && !category.isEmpty()) {
				requestBuilder.query(q -> q.term(t -> t.field("category.keyword").value(category)));
			} else {
				requestBuilder.query(q -> q.matchAll(m -> m));
			}

			// 실제 검색
			SearchResponse<Map> response = client.search(requestBuilder.build(), Map.class);

			// 전체 개수와 데이터 추출
			long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
			int totalPages = (int) Math.ceil((double) totalHits / size);
			List<Map<String, Object>> results = extractHits(response);

			// 반환할 Map 구성
			return Map.of("content", results, "totalElements", totalHits, "totalPages", totalPages, "currentPage",
					page);
		} catch (IOException e) {
			e.printStackTrace();
			return Map.of("content", List.of(), "totalElements", 0, "totalPages", 0, "currentPage", page);
		}
	}

	public List<Map<String, Object>> searchWithCategory(String keyword, String category, int page, int size) {
		try {
			SearchRequest.Builder requestBuilder = new SearchRequest.Builder().index("newsdata.newsdata")
					.from(page * size).size(size);

			if (keyword != null && !keyword.isEmpty() && category != null && !category.isEmpty()) {
				requestBuilder.query(q -> q.bool(b -> b.must(m1 -> m1.match(m -> m.field("headline").query(keyword)))
						.filter(f -> f.term(t -> t.field("category.keyword").value(category)))));
			} else if (keyword != null && !keyword.isEmpty()) {
				requestBuilder.query(q -> q.match(m -> m.field("headline").query(keyword)));
			} else if (category != null && !category.isEmpty()) {
				requestBuilder.query(q -> q.term(t -> t.field("category.keyword").value(category)));
			} else {
				requestBuilder.query(q -> q.matchAll(m -> m));
			}

			SearchResponse<Map> response = client.search(requestBuilder.build(), Map.class);
			return extractHits(response);

		} catch (IOException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	public List<Map<String, Object>> newsSearch(String keyword) throws ElasticsearchException, IOException {
		log.info("keyword => " + keyword);

		// 영 -> 한 변환 시도
//		String converted = keyboardMapper.convertEngToKor(keyword);
//		log.info("convertEngToKor => " + converted);
//		converted = hangulComposer.combine(converted);

		// 변환 결과에 한글 글자가 하나라도 있으면, 진짜 한영키 미전환으로 간주
//		boolean hasHangul = converted.codePoints().anyMatch(cp -> (cp >= 0xAC00 && cp <= 0xD7A3));
//		if (hasHangul) {
//			// 한영키 미전환 -> 합치기
//			keyword = hangulComposer.combine(converted);
//			log.info("한글 패치 keyword => " + keyword);
//
//		} else if (englishDictionary.exists(keyword)) {
//			// 변환 결과에 한글 없고, 사전에 있는 영단어면 변환 생략
//			log.info("영어 사전 단어 감지, 변환 생략 => " + keyword);
//
//		} else {
//			// 사전에 없고 변환 결과도 한글 아님 -> 일단 변환 생략
//			log.info("변환 대상 아님, 그대로 사용 => " + keyword);
//		}

		// 한글 포함 여부 체크
		boolean containsHangul = keyword.matches(".*[가-힣]+.*");
		// 2) 오직 영어 알파벳만으로 이뤄졌는지
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

		// 분석할수있게 변환
		CharSequence normalized = OpenKoreanTextProcessorJava.normalize(keyword);
		log.info("형태소 분석한 keyword(normalized) => " + normalized);
		// 토큰으로 나누기
		Seq<KoreanTokenizer.KoreanToken> tokens = OpenKoreanTextProcessorJava.tokenize(normalized);
		log.info("tokens(토큰으로 나누기) => " + tokens);
		// 토큰으로 나눈 값 리스트로
		List<String> tokenList = OpenKoreanTextProcessorJava.tokensToJavaStringList(tokens);
		log.info("tokenList(분리된 입력값) => " + tokenList);

		// 단어 만들기
		List<String> combined = new ArrayList<>(tokenList);

		int n = tokenList.size();
		for (int i = 0; i < n; i++) {
			StringBuilder sb = new StringBuilder();
			int currentLength = 0; // sb 문자열의 길이(글자 수)

			// j – i 범위 제한 없이, sb.length() 가 3 초과하면 break
			for (int j = i; j < n; j++) {
				String tok = tokenList.get(j);
				int tokLen = tok.length();

				// 새로 붙였을 때 3글자 초과하면 중단
				if (currentLength + tokLen > 3) {
					break;
				}

				// sb에 토큰 추가
				sb.append(tok);
				currentLength += tokLen;

				// 합친 문자열이 2글자 이상이면 결과에 추가
				if (currentLength >= 2) {
					combined.add(sb.toString());
				}
			}
		}

		// 첫 테스트 시작
//		combined = combined.stream().distinct().collect(Collectors.toList());
//		log.info("combined => " + combined);
//		// Elasticsearch
//		BoolQuery.Builder boolB = new BoolQuery.Builder();
//		for (String term : combined) {
//			boolB.should(s -> s.match(m -> m.field("headline").query(term).fuzziness("AUTO")));
//		}
//
//		/*
//		 * 검색시 가중치 제목: 가장 높음 내용: 가장 낮은 가중치 요약: 여기는 내용과 제목사이 가중치 키워드: 요약과 제목 사이 가중치
//		 * 
//		 * 제목 > 키워드 > 요약 > 내용
//		 */
//		SearchRequest req = SearchRequest
//				.of(b -> b.index("newsdata.newsdata").query(q -> q.bool(boolB.build())).size(10));
//
//		SearchResponse<Map> resp = client.search(req, Map.class);
//		List<Map<String, Object>> list = resp.hits().hits().stream().map(h -> h.source()).collect(Collectors.toList());
//		log.info("list => " + list);
		// 첫 테스트 끝

		// …

		// 1) BoolQuery에 term×field 마다 should(match…boost) 추가
		BoolQuery.Builder boolB = new BoolQuery.Builder();
		for (String term : combined) {
			boolB.should(s -> s.match(m -> m.field("headline") // 제목에 있을 때
					.query(term).fuzziness("AUTO").boost(5.0f) // +5.0 점
			));
			boolB.should(s -> s.match(m -> m.field("textrank_keywords")// 키워드 필드에 있을 때
					.query(term).fuzziness("AUTO").boost(3.0f) // +3.0 점
			));
			boolB.should(s -> s.match(m -> m.field("summary") // 요약에 있을 때
					.query(term).fuzziness("AUTO").boost(2.0f) // +2.0 점
			));
			boolB.should(s -> s.match(m -> m.field("content") // 본문에 있을 때
					.query(term).fuzziness("AUTO").boost(1.0f) // +1.0 점
			));
		}

		// 2) SearchRequest 빌드 (score 내림차순 정렬은 기본)
		SearchRequest req = SearchRequest
				.of(b -> b.index("newsdata.newsdata").query(q -> q.bool(boolB.build())).size(10) // 원하는
																									// 개수
				);

		// 3) 검색 실행
		SearchResponse<Map> resp = client.search(req, Map.class);

		// 4) 결과와 점수 읽기
		List<Map<String, Object>> results = resp.hits().hits().stream()
		// 중간에 각 hit의 source를 로그로 출력
//				.peek(hit -> log.info("결과 => " + hit.source()))
				// SearchHit<Map>에서 Map<String,Object>만 꺼내기
				.map(hit -> hit.source())
				// List<Map<String,Object>>로 수집
				.collect(Collectors.toList());
//		log.info("results =>" + results);

		return results;
	}
}