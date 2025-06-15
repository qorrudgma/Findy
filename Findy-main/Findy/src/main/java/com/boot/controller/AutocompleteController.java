package com.boot.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.service.ElasticService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/autocomplete")
//@RequiredArgsConstructor
public class AutocompleteController {

	private final ElasticService elasticService;

	public AutocompleteController(ElasticService elasticService) {
		this.elasticService = elasticService;
	}

	@GetMapping
	public List<String> autocomplete(@RequestParam("q") String prefix) throws IOException {
		return elasticService.getAutocompleteSuggestions(prefix);
	}

//	private final AutocompleteService autocompleteService;
//
//	/**
//	 * 자동완성 API 엔드포인트
//	 * 
//	 * @param q 검색 쿼리
//	 * @return 자동완성 제안 목록
//	 */
//	// 자동완성
//	@GetMapping("/api/autocomplete")
//	public List<AutocompleteDTO> getAutocompleteSuggestions(@RequestParam("q") String query) {
////        log.info("자동완성 API 요청: {}", query);
//		return autocompleteService.getSuggestions(query);
//	}
}