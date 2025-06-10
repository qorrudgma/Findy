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

	// 자동완성을 위한 데이터 (실제로는 DB에서 불러오는 방식으로 개선 가능)
	private final Map<String, List<String>> autocompleteDict;

	public AutocompleteService() {
		// 데이터 초기화
		autocompleteDict = new HashMap<>();
		initAutocompleteData();
	}

	private void initAutocompleteData() {
		// '강'으로 시작하는 단어들
		List<String> gang = List.of("강아지", "강릉", "강변", "강원도", "강풍", "강하다", "강릉역", "강의", "강호", "강원대학교");
		autocompleteDict.put("강", gang);

		// '경'으로 시작하는 단어들
		List<String> gyeong = List.of("경제", "경로당", "경찰", "경북대", "경복궁", "경로", "경험", "경적", "경찰서", "경기북부");
		autocompleteDict.put("경", gyeong);

		// '서'로 시작하는 단어들
		List<String> seo = List.of("서울", "서브웨이", "서점", "서류", "서버", "서양", "서유기", "서민", "서초", "서해");
		autocompleteDict.put("서", seo);

		// '부'로 시작하는 단어들
		List<String> bu = List.of("부산", "부산역", "부모", "부부", "부서", "부장", "부의", "부정", "부자", "부채");
		autocompleteDict.put("부", bu);

		// '정'으로 시작하는 단어들
		List<String> jeong = List.of("정치", "정보", "정의", "정신", "정상", "정보", "정수", "정확", "정유", "정유장");
		autocompleteDict.put("정", jeong);

		// '대'로 시작하는 단어들
		List<String> dae = List.of("대학교", "대한독립", "대학생", "대안", "대기업", "대전", "대형", "대화", "대상", "대회");
		autocompleteDict.put("대", dae);

		// '사'로 시작하는 단어들
		List<String> sa = List.of("사람", "사회", "사건", "사진", "사업", "사냥", "사고", "사이좋다", "사용", "사이");
		autocompleteDict.put("사", sa);

		// '일'로 시작하는 단어들
		List<String> il = List.of("일반", "일자리", "일상", "일요일", "일정", "일기", "일부", "일단", "일년", "일자");
		autocompleteDict.put("일", il);
	}

	// 사용자의 입력어에 대한 자동완성 결과 반환
	public List<AutocompleteDTO> getSuggestions(String query) {
		if (query == null || query.isEmpty()) {
			return new ArrayList<>();
		}

		log.info("자동완성 요청: {}", query);

		List<AutocompleteDTO> results = new ArrayList<>();

		String firstChar = query.substring(0, 1);
		if (autocompleteDict.containsKey(firstChar)) {
			results = autocompleteDict.get(firstChar).stream().filter(word -> word.startsWith(query))
					.map(AutocompleteDTO::new).collect(Collectors.toList());
		}

		// 결과가 없으면 추천 문구 추가
		if (results.isEmpty()) {
			results.add(new AutocompleteDTO(query + " 뉴스"));
			results.add(new AutocompleteDTO(query + " 정치"));
			results.add(new AutocompleteDTO(query + " 경제"));
		}

		return results;
	}
}
