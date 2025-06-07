package com.boot.service;

import com.boot.dto.NewsDTO;
import com.boot.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {
    
    private final NewsRepository newsRepository;
    
    /**
     * 키워드와 카테고리로 뉴스 검색
     */
    public Page<NewsDTO> searchNews(String keyword, String category, int page, int size) {
        log.info("뉴스 검색 - 키워드: {}, 카테고리: {}, 페이지: {}, 크기: {}", keyword, category, page, size);
        
        // 프론트엔드 카테고리를 백엔드 카테고리로 변환
        String actualCategory = convertDisplayCategoryToActual(category);
        
        // 시간 내림차순으로 정렬 (최신순)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "time"));
        
        try {
            Page<NewsDTO> result;
            
            if (keyword != null && !keyword.trim().isEmpty() && actualCategory != null && !actualCategory.trim().isEmpty()) {
                // 키워드 + 카테고리 검색
                result = newsRepository.findByKeywordAndCategory(keyword.trim(), actualCategory.trim(), pageable);
            } else if (keyword != null && !keyword.trim().isEmpty()) {
                // 키워드만 검색
                result = newsRepository.findByKeyword(keyword.trim(), pageable);
            } else if (actualCategory != null && !actualCategory.trim().isEmpty()) {
                // 카테고리만 검색
                result = newsRepository.findByCategory(actualCategory.trim(), pageable);
            } else {
                // 전체 조회 (최신순)
                result = newsRepository.findAllOrderByTimeDesc(pageable);
            }
            
            log.info("검색 결과: {}건", result.getTotalElements());
            return result;
            
        } catch (Exception e) {
            log.error("뉴스 검색 중 오류 발생", e);
            return Page.empty(pageable);
        }
    }
    
    /**
     * 프론트엔드 표시용 카테고리를 실제 저장된 카테고리로 변환
     */
    private String convertDisplayCategoryToActual(String displayCategory) {
        if (displayCategory == null) return null;
        
        Map<String, String> reverseMapping = new HashMap<>();
        reverseMapping.put("경제", "economy");
        reverseMapping.put("정치", "politics");
        reverseMapping.put("사회", "society");
        reverseMapping.put("스포츠", "sports");
        reverseMapping.put("연예/문화", "culture");
        reverseMapping.put("오피니언", "opinion");
        reverseMapping.put("건강", "health");
        
        return reverseMapping.getOrDefault(displayCategory, displayCategory);
    }
    
    /**
     * 인기 검색어 조회 (임시 구현)
     */
    public List<String> getPopularQueries() {
        // TODO: 실제로는 검색 로그를 분석해서 인기 검색어를 반환
        return Arrays.asList(
            "AI 기술", "경제 동향", "스포츠 뉴스", "정치 소식", 
            "문화 예술", "IT 트렌드", "건강 정보", "환경 이슈"
        );
    }
    
    /**
     * 자동완성 검색어 제안
     */
    public List<String> getAutocompleteSuggestions(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // TODO: 실제로는 뉴스 데이터에서 자주 나오는 키워드를 분석해서 제안
        List<String> suggestions = new ArrayList<>();
        String trimmedQuery = query.trim();
        
        suggestions.add(trimmedQuery + " 뉴스");
        suggestions.add(trimmedQuery + " 최신");
        suggestions.add(trimmedQuery + " 정보");
        suggestions.add(trimmedQuery + " 분석");
        suggestions.add(trimmedQuery + " 트렌드");
        
        return suggestions;
    }
    
    /**
     * 전체 뉴스 수 조회
     */
    public long getTotalNewsCount() {
        try {
            return newsRepository.count();
        } catch (Exception e) {
            log.error("전체 뉴스 수 조회 중 오류", e);
            return 0;
        }
    }
    
    /**
     * 카테고리별 뉴스 수 조회
     */
    public Map<String, Long> getNewsByCategory() {
        Map<String, Long> categoryCount = new HashMap<>();
        
        try {
            // 실제 크롤링된 카테고리들 조회
            List<String> actualCategories = Arrays.asList("economy", "politics", "society", "sports", "culture", "opinion", "health");
            Map<String, String> categoryMapping = createCategoryMapping();
            
            for (String actualCategory : actualCategories) {
                long count = newsRepository.countByCategory(actualCategory);
                String displayCategory = categoryMapping.getOrDefault(actualCategory, actualCategory);
                categoryCount.put(displayCategory, count);
            }
            
        } catch (Exception e) {
            log.error("카테고리별 뉴스 수 조회 중 오류", e);
        }
        
        return categoryCount;
    }
    
    /**
     * 카테고리 매핑 생성
     */
    private Map<String, String> createCategoryMapping() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("economy", "경제");
        mapping.put("politics", "정치");
        mapping.put("society", "사회");
        mapping.put("sports", "스포츠");
        mapping.put("culture", "연예/문화");
        mapping.put("opinion", "오피니언");
        mapping.put("health", "건강");
        return mapping;
    }
} 