package com.boot.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

		// 1. ElasticService에서 뉴스 검색 결과 가져오기
		Map<String, Object> result = elasticService.searchWithPagination(keyword, category, page, size, isResearch);

		// 2. Gemini AI 요약 결과 가져오기 (keyword가 있을 때만 요청)
//		if (keyword != null && !keyword.isBlank()) {
//			String aiSummary = geminiService.getSummary(keyword);
//			log.info(" Gemini 요약 결과: {}", aiSummary);
//
//			// 3. result에 AI 요약 결과 포함시켜서 프론트에 함께 전달
//			result.put("aiSummary", aiSummary); // 👉 프론트에서 사용: data.aiSummary
//		}

		// 4. 최종 JSON 응답 반환
		return ResponseEntity.ok(result);
	}
}