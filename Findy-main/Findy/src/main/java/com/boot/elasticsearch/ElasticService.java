package com.boot.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

// 인증서 없는 버전
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ElasticService {

	private final ElasticsearchClient client;

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

}

//   public void searchNews() throws IOException {
//      System.out.println("뜨나1");
/// /      SearchRequest request = new SearchRequest.Builder().index("newsdata.newsdata") // 색인 이름
/// /            .query(q -> q.match(m -> m.field("headline").query("공정위"))).build();
//
//
//      //모든 데이터 출력
//      SearchRequest request = new SearchRequest.Builder()
//            .index("newsdata.newsdata")
//            .query(q -> q.matchAll(m -> m))  // 전체 문서 검색
//            .size(100)  // 최대 100건까지 받도록 설정
//            .build();
//
//
//      SearchResponse<Object> response = client.search(request, Object.class);
//      List<Hit<Object>> hits = response.hits().hits();
//
//      for (Hit<Object> hit : hits) {
//         System.out.println("!@#$" + hit.source());
//      }
//   }

//인증서 있는 버전
//import co.elastic.clients.elasticsearch.ElasticsearchClient;
//import co.elastic.clients.transport.endpoints.BooleanResponse;
//import lombok.extern.slf4j.Slf4j;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Slf4j
//@Service
//public class ElasticTestService {
//
//    @Autowired
//    private ElasticsearchClient elasticsearchClient;
//
//    public void testConnection() {
//        try {
//           BooleanResponse response = elasticsearchClient.ping();
//            if (response.value()) {
//                System.out.println("Elasticsearch 연결 성공!");
/// /                log.info("Elasticsearch 연결 성공!");
//            } else {
//                System.out.println("연결은 되지만 ping 실패");
////                log.info("연결은 되지만 ping 실패");
//            }
//        } catch (Exception e) {
//            System.out.println("연결 실패!");
////            log.info("연결 실패!");
//            e.printStackTrace();
//        }
//    }
//}