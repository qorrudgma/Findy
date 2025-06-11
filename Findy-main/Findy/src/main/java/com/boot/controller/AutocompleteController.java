package com.boot.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.dto.AutocompleteDTO;
import com.boot.elasticsearch.ElasticTestService;
import com.boot.service.AutocompleteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AutocompleteController {

	private final AutocompleteService autocompleteService;

	private final ElasticTestService elasticTestService;

	/**
	 * 자동완성 API 엔드포인트
	 * 
	 * @param q 검색 쿼리
	 * @return 자동완성 제안 목록
	 */
	// 자동완성
	@GetMapping("/api/autocomplete")
	public List<AutocompleteDTO> getAutocompleteSuggestions(@RequestParam("q") String query) {
//        log.info("자동완성 API 요청: {}", query);
		return autocompleteService.getSuggestions(query);
	}

	@GetMapping("/search")
	public List<Map<String, Object>> getKeword(@RequestParam("q") String keyword) throws IOException {
		log.info("@# keyword => " + keyword);
		List<Map<String, Object>> news_data = elasticTestService.searchNews("keyword");
		log.info("!@# news_data => " + news_data);

		return news_data;
	}
}