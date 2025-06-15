package com.boot.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.dto.ClickRequest;
import com.boot.service.ElasticService;
import com.boot.service.GeminiService; //  Gemini 서비스 import

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
public class NewsSearchController {

	private final ElasticService elasticService;
	private final GeminiService geminiService; // GeminiService ㅎㅇ

	// 생성자에 GeminiService 주입
	public NewsSearchController(ElasticService elasticService, GeminiService geminiService) {
		this.elasticService = elasticService;
		this.geminiService = geminiService;
	}

	@GetMapping("/search")
	public ResponseEntity<?> searchNews(@RequestParam(value = "q", required = false) String keyword,
			@RequestParam(value = "category", required = false) String category,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size,
			@RequestParam(value = "research", defaultValue = "false") boolean isResearch) throws IOException {

		log.info(" 검색 요청: keyword={}, category={}, page={}, size={}, research={}", keyword, category, page, size,
				isResearch);

		// ElasticService에서 뉴스 검색 결과 가져오기
		Map<String, Object> result = elasticService.searchWithPagination(keyword, category, page, size, isResearch);

		// 최종 JSON 응답 반환
		return ResponseEntity.ok(result);
	}

	@GetMapping("/keywordRank")
	public List<String> topKeywords(@RequestParam(value = "size") int size) throws IOException {

		return elasticService.topKeywords(size);
	}

	@PostMapping("/news/click")
	public void recordClick(@RequestBody ClickRequest request) throws IOException {
		log.info("recordClick()");
//		log.info("클릭 요청 수신 => url={}, keywords={}", request.getUrl(), request.getKeywords());
		elasticService.logPopularNewsAndKeywordsByUrlAndKeywords(request.getUrl(), request.getKeywords());
	}
}