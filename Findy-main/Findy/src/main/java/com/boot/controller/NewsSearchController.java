package com.boot.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.service.ElasticService;

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
			@RequestParam(value = "size", defaultValue = "10") int size) throws IOException {
		Map<String, Object> result = elasticService.searchWithPagination(keyword, category, page, size);
		return ResponseEntity.ok(result);
	}
}
