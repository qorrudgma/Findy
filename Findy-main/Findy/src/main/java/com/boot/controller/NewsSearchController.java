package com.boot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.elasticsearch.ElasticService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
public class NewsSearchController {
	private final ElasticService elasticService;

	public NewsSearchController(ElasticService elasticService) {
		this.elasticService = elasticService;
	}

	@GetMapping("/search")
	public ResponseEntity<?> searchNews(@RequestParam(value = "q", required = false) String keyword,
			@RequestParam(value = "category", required = false) String category,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {
		List<Map<String, Object>> newsList = elasticService.searchWithCategory(keyword, category, page, size);

		// 간단한 형태로 리턴 구조 통일
		Map<String, Object> result = Map.of("content", newsList, "totalElements", newsList.size(), "totalPages", 1,
				"currentPage", page);
		log.info("result => "+result);
		
		return ResponseEntity.ok(result);
	}

}
