package com.boot.elasticsearch;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

// 인증서 없는 버전
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

@Service
public class ElasticTestService {

	private final ElasticsearchClient client;

	public ElasticTestService(ElasticsearchClient client) {
		this.client = client;
	}

	public void searchNews() throws IOException {
		System.out.println("뜨나1");
		SearchRequest request = new SearchRequest.Builder().index("newsdata.newsdata") // 색인 이름
//				.query(q -> q.match(m -> m.field("headline").query("경희대"))).build();
//				.query(q -> q.matchPhrase(m -> m.field("headline").query("작년"))).build();
				// 전체 조회
				.size(1000).query(q -> q.matchAll(m -> m)).build();

		SearchResponse<Object> response = client.search(request, Object.class);
		List<Hit<Object>> hits = response.hits().hits();

		for (Hit<Object> hit : hits) {
//			System.out.println("!@#$" + hit.source());
		}
	}
}
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
//        	BooleanResponse response = elasticsearchClient.ping();
//            if (response.value()) {
//                System.out.println("Elasticsearch 연결 성공!");
////                log.info("Elasticsearch 연결 성공!");
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