import React, { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faMagnifyingGlass, faBell, faBookmark, faGear } from '@fortawesome/free-solid-svg-icons';
import { useLanguage } from '../../contexts/LanguageContext';
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
  const { t } = useLanguage();

  // URL에서 검색어를 읽어와서 검색창에 표시
  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    const urlQuery = searchParams.get('q');
    if (urlQuery && urlQuery !== searchQuery) {
      setSearchQuery(urlQuery);
    }
  }, [location.search]);

  const categories = [
    { key: '전체', label: t('header.categories.all') },
    { key: '경제', label: t('header.categories.economy') },
    { key: '오피니언', label: t('header.categories.opinion') },
    { key: '사회', label: t('header.categories.society') },
    { key: '건강', label: t('header.categories.health') },
    { key: '연예/문화', label: t('header.categories.entertainment') },
    { key: '스포츠', label: t('header.categories.sports') }
  ];

  // 더미 자동완성 데이터 사전
  // const dummyAutocompleteDict: Record<string, string[]> = {
  //   '강': ['강아지', '강남', '강물', '강원도', '강철', '강하다', '강남역', '강의', '강변', '강원대학교'],
  //   '경': ['경제', '경기도', '경찰', '경복궁', '경영', '경기', '경쟁', '경력', '경상도', '경험'],
  //   '서': ['서울', '서비스', '서양', '서점', '서쪽', '서버', '서류', '서명', '서초', '서민'],
  //   '부': ['부산', '부동산', '부모', '부분', '부부', '부장', '부족', '부정', '부자', '부대'],
  //   '정': ['정치', '정부', '정책', '정신', '정상', '정보', '정수', '정확', '정리', '정류장'],
  //   '대': ['대학', '대한민국', '대통령', '대구', '대기업', '대전', '대형', '대화', '대상', '대회'],
  //   '사': ['사람', '사회', '사건', '사진', '사업', '사랑', '사고', '사무실', '사용', '사이'],
  //   '일': ['일본', '일자리', '일상', '일요일', '일정', '일기', '일반', '일식', '일년', '일부']
  // };

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
      // let dummySuggestions: SearchSuggestion[] = [];
      
      // 한글 첫 글자 기준으로 매칭되는 단어들 찾기
      // const firstChar = query.charAt(0);
      // if (dummyAutocompleteDict[firstChar]) {
      //   // 입력된 검색어로 시작하는 단어들만 필터링
      //   dummySuggestions = dummyAutocompleteDict[firstChar]
      //     .filter(word => word.startsWith(query))
      //     .map(word => ({ query: word }));
      // }
      
      // 기본 더미 데이터 추가
      // if (dummySuggestions.length === 0) {
      //   dummySuggestions = [
      //     { query: `${query} 뉴스` },
      //     { query: `${query} 정치` },
      //     { query: `${query} 경제` },
      //   ];
      // }
      
      // setSuggestions(dummySuggestions);
      setSuggestions([]);
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
      // setPopularSearches(['경제', '정치', '사회', 'AI', '스포츠']);
      setPopularSearches([]);
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

  // 여기서 검색이 뭐로 할지 넘겨줌
  const handleSearch = (query: string) => {
    // 검색어가 있거나 카테고리가 선택된 경우 검색 실행
    if (query.trim() || selectedCategory) {
      let searchUrl = '/search?';
      const params = new URLSearchParams();
      
      if (query.trim()) {
        params.append('q', query.trim());
      }
      if (selectedCategory) {
        params.append('category', selectedCategory);
      }
      
      navigate(`/search?${params.toString()}`);
      setShowSuggestions(false);
      // setSearchQuery(''); // 검색어를 지우지 않음
      
      // 검색 후 상단으로 부드럽게 스크롤
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
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

  // 카테고리 클릭 처리 및 상단 스크롤
  const handleCategoryClick = (categoryKey: string) => {
    if (categoryKey === 'all') {
      navigate('/');
    } else {
      navigate(`/search?category=${encodeURIComponent(categoryKey)}`);
    }
    // 카테고리 클릭 후 상단으로 부드럽게 스크롤
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleCategoryChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newCategory = e.target.value;
    setSelectedCategory(newCategory);
    
    // 카테고리 변경 시 즉시 검색 실행
    if (newCategory || searchQuery.trim()) {
      const params = new URLSearchParams();
      
      if (searchQuery.trim()) {
        params.append('q', searchQuery.trim());
      }
      if (newCategory) {
        params.append('category', newCategory);
      }
      
      navigate(`/search?${params.toString()}`);
      
      // 검색 후 상단으로 부드럽게 스크롤
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const handlePopularSearchClick = (query: string) => {
    setSearchQuery(query);
    handleSearch(query);
  };

  const handleLogoClick = () => {
    navigate('/');
    // 홈으로 이동 후 상단으로 부드럽게 스크롤
    window.scrollTo({ top: 0, behavior: 'smooth' });
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
                    <option value="">{t('header.categories.all')}</option>
                    <option value="경제">{t('header.categories.economy')}</option>
                    <option value="오피니언">{t('header.categories.opinion')}</option>
                    <option value="사회">{t('header.categories.society')}</option>
                    <option value="건강">{t('header.categories.health')}</option>
                    <option value="연예/문화">{t('header.categories.entertainment')}</option>
                    <option value="스포츠">{t('header.categories.sports')}</option>
                  </select>
                  <div className="search-divider"></div>
                  <input 
                    ref={searchInputRef}
                    type="text" 
                    id="searchInput" 
                    className="search-input-inner" 
                    placeholder={t('header.search.placeholder')} 
                    value={searchQuery}
                    onChange={handleInputChange}
                    onKeyDown={handleKeyDown}
                    onFocus={() => searchQuery.length >= 1 && setShowSuggestions(true)}
                    autoComplete="off"
                  />
                <button type="submit" className="search-btn">
                  <FontAwesomeIcon icon={faMagnifyingGlass} />
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
              {/* <button className="action-btn" title="알림">
                <FontAwesomeIcon icon={faBell} />
              </button>
              <button className="action-btn" title="북마크">
                <FontAwesomeIcon icon={faBookmark} />
              </button>
              <button className="action-btn" title="설정">
                <FontAwesomeIcon icon={faGear} />
              </button> */}
            </div>
          </div>
        </div>
        
        {/* 하단 영역: 카테고리 네비게이션 */}
        <nav className="header-nav">
          <div className="category-list">
            {categories.map((category) => {
              const isActive = 
                (location.pathname === '/' && category.key === 'all') ||
                location.search.includes(`category=${encodeURIComponent(category.key)}`) ||
                (location.pathname === '/search' && category.key === 'all' && !location.search.includes('category='));
              
              return (
                <a 
                  href="#" 
                  key={category.key} 
                  className={`category-item ${ isActive ? 'active' : '' }`}
                  data-category={category.key}
                  onClick={(e) => { e.preventDefault(); handleCategoryClick(category.key); }}
                >
                  {category.label}
                </a>
              );
            })}
          </div>
        </nav>
      </div>
    </header>
  );
};

export default Header; 