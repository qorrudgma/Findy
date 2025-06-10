
package com.boot.elasticsearch;

// 인증서 없는 버전
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ElasticTestRunner implements CommandLineRunner {

	private final ElasticTestService service;

	public ElasticTestRunner(ElasticTestService service) {
		this.service = service;
	}

	@Override
	public void run(String... args) throws Exception {
		service.searchNews();
	}
}
// 인증서 있는 버전
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@Component
//public class ElasticTestRunner implements CommandLineRunner {
//
//	private final ElasticTestService testService;
//	private final MongoTestService mongoTestService;
//
//	public ElasticTestRunner(ElasticTestService testService, MongoTestService mongoTestService) {
//		this.testService = testService;
//	    this.mongoTestService = mongoTestService;
//	}
//
//	@Override
//	public void run(String... args) throws Exception {
//		System.out.println("!@#$!@#$현재 사용하는 java.home: " + System.getProperty("java.home"));
//		testService.testConnection();
//		mongoTestService.testConnection();
//	}
//}