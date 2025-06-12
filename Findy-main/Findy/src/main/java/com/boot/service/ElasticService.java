package com.boot.service; // 서비스 클래스가 포함된 패키지 선언

import java.io.IOException; // 입출력 예외 처리를 위한 클래스
import java.util.ArrayList; // 리스트 객체 생성을 위한 클래스
import java.util.List; // 리스트 타입 사용을 위한 인터페이스
import java.util.Map; // 결과 데이터를 키-값 형태로 다루기 위한 Map 인터페이스

import org.openkoreantext.processor.OpenKoreanTextProcessorJava;
import org.openkoreantext.processor.tokenizer.KoreanTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import com.boot.elasticsearch.HangulComposer;
import com.boot.elasticsearch.KeyboardMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient; // Elasticsearch 클라이언트 클래스
import co.elastic.clients.elasticsearch.core.SearchRequest; // 검색 요청 객체
import co.elastic.clients.elasticsearch.core.SearchResponse; // 검색 응답 객체
import co.elastic.clients.elasticsearch.core.search.Hit; // 검색 결과의 단일 항목 표현
import lombok.extern.slf4j.Slf4j;
import scala.collection.Seq;

@Slf4j
@Service
public class ElasticService {
	@Autowired
	private ElasticsearchOperations elasticsearchOperations;
	@Autowired
	private KeyboardMapper keyboardMapper;
	@Autowired
	private HangulComposer hangulComposer;
	private final ElasticsearchClient client;

	// 생성자를 통해 의존성 주입 (Spring이 클라이언트 객체를 주입)
	public ElasticService(ElasticsearchClient client) {
		this.client = client;
	}

	private List<Map<String, Object>> extractHits(SearchResponse<Map> response) {
		List<Map<String, Object>> results = new ArrayList<>();
		for (Hit<Map> hit : response.hits().hits()) {
			results.add(hit.source());
		}
		return results;
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

	public List<Map<String, Object>> newsSearch(String keyword) {
		log.info("keyword => " + keyword);

		// 한영키
		keyword = keyboardMapper.convertEngToKor(keyword);
		log.info("한영키 변환 keyword => " + keyword);
		// 위에서 변환한거 합치기
		keyword = hangulComposer.combine(keyword);
		log.info("한글 패치 keyword => " + keyword);
		// 분석할수있게 변환
		CharSequence normalized = OpenKoreanTextProcessorJava.normalize(keyword);
		log.info("형태소 분석한 keyword(normalized) => " + normalized);
		// 토큰으로 나누기
		Seq<KoreanTokenizer.KoreanToken> tokens = OpenKoreanTextProcessorJava.tokenize(normalized);
		log.info("tokens(토큰으로 나누기) => " + tokens);
		// 토큰으로 나눈 값 리스트로
		List<String> tokenList = OpenKoreanTextProcessorJava.tokensToJavaStringList(tokens);
		log.info("tokenList(분리된 입력값) => " + tokenList);

		return new ArrayList<>();
	}
}