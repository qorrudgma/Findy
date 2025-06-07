package com.boot.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.dto.AutocompleteDTO;
import com.boot.service.AutocompleteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AutocompleteController {

    private final AutocompleteService autocompleteService;
    
    /**
     * 자동완성 API 엔드포인트
     * 
     * @param q 검색 쿼리
     * @return 자동완성 제안 목록
     */
    @GetMapping("/api/autocomplete")
    public List<AutocompleteDTO> getAutocompleteSuggestions(@RequestParam("q") String query) {
        log.info("자동완성 API 요청: {}", query);
        return autocompleteService.getSuggestions(query);
    }
} 