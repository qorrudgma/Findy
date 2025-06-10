import React, { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './Header.css';

interface SearchSuggestion {
  query: string;
  count?: number;
}

const Header: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [suggestions, setSuggestions] = useState<SearchSuggestion[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [selectedSuggestionIndex, setSelectedSuggestionIndex] = useState(-1);
  const [popularSearches, setPopularSearches] = useState<string[]>([]);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const suggestionsRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const location = useLocation();

  const categories = [
    '전체', '경제', '오피니언', '사회', '건강', 
    '연예/문화', '스포츠'
  ];

  // 더미 자동완성 데이터 사전
  const dummyAutocompleteDict: Record<string, string[]> = {
    '강': ['강아지', '강남', '강물', '강원도', '강철', '강하다', '강남역', '강의', '강변', '강원대학교'],
    '경': ['경제', '경기도', '경찰', '경복궁', '경영', '경기', '경쟁', '경력', '경상도', '경험'],
    '서': ['서울', '서비스', '서양', '서점', '서쪽', '서버', '서류', '서명', '서초', '서민'],
    '부': ['부산', '부동산', '부모', '부분', '부부', '부장', '부족', '부정', '부자', '부대'],
    '정': ['정치', '정부', '정책', '정신', '정상', '정보', '정수', '정확', '정리', '정류장'],
    '대': ['대학', '대한민국', '대통령', '대구', '대기업', '대전', '대형', '대화', '대상', '대회'],
    '사': ['사람', '사회', '사건', '사진', '사업', '사랑', '사고', '사무실', '사용', '사이'],
    '일': ['일본', '일자리', '일상', '일요일', '일정', '일기', '일반', '일식', '일년', '일부']
  };

  // 자동완성 데이터 로드
  const loadSuggestions = async (query: string) => {
    if (query.length < 1) {
      setSuggestions([]);
      return;
    }

    try {
      // 실제 API 호출 시도
      const response = await fetch(`/api/autocomplete?q=${encodeURIComponent(query)}`);
      if (response.ok) {
        const data = await response.json();
        setSuggestions(data);
      } else {
        throw new Error('API 호출 실패');
      }
    } catch (error) {
      console.error('자동완성 로드 오류:', error);
      
      // 더미 자동완성 데이터 생성
      let dummySuggestions: SearchSuggestion[] = [];
      
      // 한글 첫 글자 기준으로 매칭되는 단어들 찾기
      const firstChar = query.charAt(0);
      if (dummyAutocompleteDict[firstChar]) {
        // 입력된 검색어로 시작하는 단어들만 필터링
        dummySuggestions = dummyAutocompleteDict[firstChar]
          .filter(word => word.startsWith(query))
          .map(word => ({ query: word }));
      }
      
      // 기본 더미 데이터 추가
      if (dummySuggestions.length === 0) {
        dummySuggestions = [
          { query: `${query} 뉴스` },
          { query: `${query} 정치` },
          { query: `${query} 경제` },
        ];
      }
      
      setSuggestions(dummySuggestions);
    }
  };

  // 인기 검색어 로드
  const loadPopularSearches = async () => {
    try {
      const response = await fetch('/api/search/popular');
      if (response.ok) {
        const data = await response.json();
        setPopularSearches(data.slice(0, 5));
      }
    } catch (error) {
      console.error('인기 검색어 로드 오류:', error);
      setPopularSearches(['경제', '정치', '사회', 'AI', '스포츠']);
    }
  };

  useEffect(() => {
    loadPopularSearches();
    
    // 클릭 이벤트 리스너 추가 (외부 클릭 시 자동완성 닫기)
    const handleClickOutside = (event: MouseEvent) => {
      if (
        suggestionsRef.current && 
        !suggestionsRef.current.contains(event.target as Node) &&
        searchInputRef.current && 
        !searchInputRef.current.contains(event.target as Node)
      ) {
        setShowSuggestions(false);
      }
    };
    
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // 검색어가 변경될 때마다 자동완성 데이터 로드
  useEffect(() => {
    const timerId = setTimeout(() => {
      if (searchQuery) {
        loadSuggestions(searchQuery);
      }
    }, 200); // 타이핑 후 200ms 지연

    return () => {
      clearTimeout(timerId);
    };
  }, [searchQuery]);

  // 검색 버튼 클릭
  const handleSearch = (query: string) => {
    // console.log("검색 버튼을 눌렀어요");
    // if (query.trim()) {
    //   const categoryParam = selectedCategory ? `&category=${encodeURIComponent(selectedCategory)}` : '';
    //   navigate(`/search?q=${encodeURIComponent(query.trim())}${categoryParam}`);
    //   setShowSuggestions(false);
    //   setSearchQuery('');
    // }
    // controller
    (async () => {
        try {
          const response = await fetch(`/api/search?keyword=${encodeURIComponent(query)}`);
          if (response.ok) {
            const data = await response.json();
            console.log("받아온 데이터 =>", data);
            // 필요하면 여기서 상태 저장도 가능: setResults(data) 등
          }
        } catch (error) {
          console.error('검색 API 호출 오류:', error);
        }
    })();
    // 화면 바뀌는 곳
    navigate(`/search?keyword=${query}`);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setSearchQuery(value);
    setSelectedSuggestionIndex(-1);
    
    if (value.trim().length >= 1) {
      setShowSuggestions(true);
    } else {
      setShowSuggestions(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (selectedSuggestionIndex >= 0 && suggestions[selectedSuggestionIndex]) {
        handleSearch(suggestions[selectedSuggestionIndex].query);
      } else {
        handleSearch(searchQuery);
      }
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedSuggestionIndex(prev => 
        prev < suggestions.length - 1 ? prev + 1 : -1
      );
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedSuggestionIndex(prev => 
        prev > -1 ? prev - 1 : suggestions.length - 1
      );
    } else if (e.key === 'Escape') {
      setShowSuggestions(false);
      setSelectedSuggestionIndex(-1);
    }
  };

  const handleCategoryClick = (category: string) => {
    if (category === '전체') {
      navigate('/');
    } else {
      navigate(`/search?category=${encodeURIComponent(category)}`);
    }
  };

  const handleCategoryChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setSelectedCategory(e.target.value);
  };

  const handlePopularSearchClick = (query: string) => {
    handleSearch(query);
  };

  const handleLogoClick = () => {
    navigate('/');
  };

  const handleSuggestionClick = (suggestion: string) => {
    setSearchQuery(suggestion);
    // 검색창에 입력만 하고 검색은 실행하지 않음
    setShowSuggestions(false);
    
    // 검색창에 포커스 유지
    if (searchInputRef.current) {
      searchInputRef.current.focus();
    }
  };

  return (
    <header className="fixed-header">
      <div className="header-container">
        {/* 상단 영역: 로고 + 검색창 + 오른쪽 영역 */}
        <div className="header-top">
          {/* 왼쪽 로고 영역 */}
          <div className="header-left">
            <div className="logo-container" onClick={handleLogoClick}>
              <img src="/images/Findy_logo.png" alt="Findy Logo" className="header-logo" />
            </div>
          </div>
          
          {/* 중앙 검색창 영역 */}
          <div className="header-center">
            <div className="header-search">
              <form className="search-form" onSubmit={(e) => { e.preventDefault(); handleSearch(searchQuery); }}>
                <div className="search-input-container">
                  <select 
                    id="categorySelect" 
                    className="category-select-inner"
                    value={selectedCategory}
                    onChange={handleCategoryChange}
                  >
                    <option value="">전체</option>
                    <option value="경제">경제</option>
                    <option value="오피니언">오피니언</option>
                    <option value="사회">사회</option>
                    <option value="건강">건강</option>
                    <option value="연예/문화">연예/문화</option>
                    <option value="스포츠">스포츠</option>
                  </select>
                  <div className="search-divider"></div>
                  <input 
                    ref={searchInputRef}
                    type="text" 
                    id="searchInput" 
                    className="search-input-inner" 
                    placeholder="뉴스, 키워드, 주제를 검색해보세요..." 
                    value={searchQuery}
                    onChange={handleInputChange}
                    onKeyDown={handleKeyDown}
                    onFocus={() => searchQuery.length >= 1 && setShowSuggestions(true)}
                    autoComplete="off"
                  />
                  <button type="submit" className="search-btn">
                    🔍
                  </button>
                </div>
                
                {/* 자동완성 드롭다운 */}
                {showSuggestions && suggestions.length > 0 && (
                  <div className="suggestions-dropdown" ref={suggestionsRef}>
                    {suggestions.map((suggestion, index) => (
                      <div
                        key={index}
                        className={`suggestion-item ${
                          index === selectedSuggestionIndex ? 'selected' : ''
                        }`}
                        onClick={() => handleSuggestionClick(suggestion.query)}
                      >
                        <span className="suggestion-text">{suggestion.query}</span>
                        {suggestion.count && (
                          <span className="suggestion-count">{suggestion.count}</span>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </form>
            </div>
          </div>
          
          {/* 오른쪽 영역 */}
          <div className="header-right">
            <div className="user-actions">
              <button className="action-btn" title="알림">
                🔔
              </button>
              <button className="action-btn" title="북마크">
                📚
              </button>
              <button className="action-btn" title="설정">
                ⚙️
              </button>
            </div>
          </div>
        </div>
        
        {/* 하단 영역: 카테고리 네비게이션 */}
        <nav className="header-nav">
          <div className="category-list">
            {categories.map((category) => (
              <a 
                href="#" 
                key={category} 
                className={`category-item ${
                  (location.pathname === '/' && category === '전체') ||
                  location.search.includes(`category=${encodeURIComponent(category)}`)
                    ? 'active' : ''
                }`}
                data-category={category}
                onClick={(e) => { e.preventDefault(); handleCategoryClick(category); }}
              >
                {category}
              </a>
            ))}
          </div>
        </nav>
      </div>
    </header>
  );
};

export default Header; 