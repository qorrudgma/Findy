package com.boot.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.boot.dto.AutocompleteDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AutocompleteService {

    // 자동완성용 데이터 (실제로는 DB에서 가져오는 것이 좋음)
    private final Map<String, List<String>> autocompleteDict;
    
    public AutocompleteService() {
        // 초기 데이터 설정
        autocompleteDict = new HashMap<>();
        
        // 자동완성 데이터 초기화
        initAutocompleteData();
    }
    
    private void initAutocompleteData() {
        // '강'으로 시작하는 단어들
        List<String> gang = List.of("강아지", "강남", "강물", "강원도", "강철", "강하다", "강남역", "강의", "강변", "강원대학교");
        autocompleteDict.put("강", gang);
        
        // '경'으로 시작하는 단어들
        List<String> gyeong = List.of("경제", "경기도", "경찰", "경복궁", "경영", "경기", "경쟁", "경력", "경상도", "경험");
        autocompleteDict.put("경", gyeong);
        
        // '서'로 시작하는 단어들
        List<String> seo = List.of("서울", "서비스", "서양", "서점", "서쪽", "서버", "서류", "서명", "서초", "서민");
        autocompleteDict.put("서", seo);
        
        // '부'로 시작하는 단어들
        List<String> bu = List.of("부산", "부동산", "부모", "부분", "부부", "부장", "부족", "부정", "부자", "부대");
        autocompleteDict.put("부", bu);
        
        // '정'으로 시작하는 단어들
        List<String> jeong = List.of("정치", "정부", "정책", "정신", "정상", "정보", "정수", "정확", "정리", "정류장");
        autocompleteDict.put("정", jeong);
        
        // '대'로 시작하는 단어들
        List<String> dae = List.of("대학", "대한민국", "대통령", "대구", "대기업", "대전", "대형", "대화", "대상", "대회");
        autocompleteDict.put("대", dae);
        
        // '사'로 시작하는 단어들
        List<String> sa = List.of("사람", "사회", "사건", "사진", "사업", "사랑", "사고", "사무실", "사용", "사이");
        autocompleteDict.put("사", sa);
        
        // '일'로 시작하는 단어들
        List<String> il = List.of("일본", "일자리", "일상", "일요일", "일정", "일기", "일반", "일식", "일년", "일부");
        autocompleteDict.put("일", il);
    }
    
    // 검색어에 맞는 자동완성 결과 반환
    public List<AutocompleteDTO> getSuggestions(String query) {
        if (query == null || query.isEmpty()) {
            return new ArrayList<>();
        }
        
        log.info("자동완성 검색어: {}", query);
        
        List<AutocompleteDTO> results = new ArrayList<>();
        
        // 첫 글자로 매칭되는 데이터가 있는지 확인
        String firstChar = query.substring(0, 1);
        if (autocompleteDict.containsKey(firstChar)) {
            // 해당 글자로 시작하는 단어들 중 쿼리로 시작하는 단어들만 필터링
            results = autocompleteDict.get(firstChar).stream()
                    .filter(word -> word.startsWith(query))
                    .map(word -> new AutocompleteDTO(word))
                    .collect(Collectors.toList());
        }
        
        // 결과가 없거나 부족한 경우 기본 제안 추가
        if (results.isEmpty()) {
            results.add(new AutocompleteDTO(query + " 뉴스"));
            results.add(new AutocompleteDTO(query + " 정치"));
            results.add(new AutocompleteDTO(query + " 경제"));
        }
        
        return results;
    }
} 