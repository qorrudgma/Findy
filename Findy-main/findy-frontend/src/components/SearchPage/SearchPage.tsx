import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import NewsCard from '../NewsCard/NewsCard';
import LoadingSpinner from '../LoadingSpinner/LoadingSpinner';
import './SearchPage.css';

interface NewsArticle {
  id?: string;
  headline: string;
  content: string;
  preview: string;
  keywords: string[];
  category: string;
  time: string;
  source: string;
  url: string;
}

const SearchPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const [searchResults, setSearchResults] = useState<NewsArticle[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [totalResults, setTotalResults] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const navigate = useNavigate();

  const query = searchParams.get('q') || '';
  const category = searchParams.get('category') || '';
  const source = searchParams.get('source') || '';

  // 주요 언론사 목록 (표시용 한글명)
  const newsSources = [
    '조선일보', '중앙일보', '동아일보',
    '경향신문', '한겨레', '이데일리',
    '연합뉴스'
  ];

  // 한글 언론사명을 영어 코드로 변환하는 매핑
  const sourceCodeMap: { [key: string]: string } = {
    '조선일보': 'chosun',
    '중앙일보': 'joongang', 
    '동아일보': 'donga',
    '한국일보': 'hankook',
    '경향신문': 'khan',
    '한겨레': 'hani',
    '이데일리': 'edaily',
    '연합뉴스': 'yna'
  };

  // 영어 코드를 한글명으로 변환하는 역매핑
  const reverseSourceCodeMap: { [key: string]: string } = {
    'chosun': '조선일보',
    'joongang': '중앙일보',
    'donga': '동아일보', 
    'hankook': '한국일보',
    'khan': '경향신문',
    'hani': '한겨레',
    'edaily': '이데일리',
    'yna': '연합뉴스'
  };

  // 검색어나 카테고리가 변경되면 페이지를 0으로 초기화
  useEffect(() => {
    setCurrentPage(0);
  }, [query, category, source]);

  // 검색 실행 (페이지 변경 포함)
  useEffect(() => {
    if (query || category || source) {
      performSearch();
    }
  }, [query, category, source, currentPage]);

  // 여기서 검색어 받아서 뭐로 할지 적음
  const performSearch = async () => {
    try {
      setIsLoading(true);
      
      const params = new URLSearchParams();
      if (query) params.append('q', query);
      if (category) params.append('category', category);
      params.append('page', currentPage.toString());
      params.append('size', '10');

      let url;
        if (query) {
          // 키워드 검색용 엔드포인트
          url = `http://localhost:8485/search?${params.toString()}`;
        } else {
          // 카테고리 조회용 엔드포인트
          url = `http://localhost:8485/api/search?${params.toString()}`;
        }
      
      
      const response = await fetch(url);
      
      if (response.ok) {
        const data: any = await response.json();
        console.log('API 응답 데이터:', data);
        
        // API 응답 데이터를 NewsArticle 형태로 변환
        if (data.content && data.content.length > 0) {
          const transformedNews = data.content.map((item: any) => ({
            id: item.id || item.url || Math.random().toString(),
            category: item.category || "기타",
            // category: categoryMap[item.category] || item.category, // 영어 카테고리를 한글로 변환
            headline: item.headline || "제목 없음",
            content: item.content || "내용 없음",
            // summary: item.summary || item.content?.substring(0, 100) + '...',
            preview: item.content?.substring(0, 100) + '...',
            time: item.time || "날짜 없음",
            source: item.source || '기타',
            tags: item.tags || [],
            url: item.url || "#"
          }));
          
          setSearchResults(transformedNews);
          setTotalResults(data.totalElements || 0);
          setTotalPages(data.totalPages || 0);
        } else {
          setSearchResults([]);
          setTotalResults(0);
          setTotalPages(0);
        }
      } else {
        console.error('API 응답 오류:', response.status, response.statusText);
        throw new Error(`검색 실패: ${response.status} ${response.statusText}`);
      }
    } catch (error) {
      console.error('검색 오류:', error);
      
      // 백엔드 연결 실패 시 로그만 출력
      if (error instanceof TypeError && error.message.includes('fetch')) {
        console.error('백엔드 서버에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요.');
      }
      
      // 검색 실패 시 빈 결과 표시 (더미 데이터 사용하지 않음)
      setSearchResults([]);
      setTotalResults(0);
      setTotalPages(0);
    } finally {
      setIsLoading(false);
    }
  };
  
  const handleNewsClick = (article: NewsArticle) => {
    if (article.url && article.url !== '#') {
      window.open(article.url, '_blank', 'noopener,noreferrer');
    }
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleSourceClick = (selectedSource: string) => {
    // 한글 언론사명을 영어 코드로 변환
    const sourceCode = sourceCodeMap[selectedSource] || selectedSource;
    let searchUrl = `/search?source=${encodeURIComponent(sourceCode)}`;
    if (query) {
      searchUrl += `&q=${encodeURIComponent(query)}`;
    }
    if (category) {
      searchUrl += `&category=${encodeURIComponent(category)}`;
    }
    navigate(searchUrl);
    // 페이지 상단으로 부드럽게 스크롤
    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
  };

  const getSearchTitle = () => {
    if (source && query) {
      // 영어 코드를 한글명으로 변환하여 표시
      const sourceName = reverseSourceCodeMap[source] || source;
      return `"${query}" (${sourceName})`;
    } else if (source) {
      // 영어 코드를 한글명으로 변환하여 표시
      const sourceName = reverseSourceCodeMap[source] || source;
      return `${sourceName}`;
    } else if (query && category) {
      return `"${query}" (${category})`;
    } else if (query) {
      return `"${query}"`;
    } else if (category) {
      return `${category}`;
    }
    return '검색 결과';
  };

  return (
    <div className="search-page">
      <div className="search-content">
        <div className="search-header">
          <h2 className="search-title">
            {getSearchTitle()} 검색 결과
          </h2>
          {!isLoading && (
            <p className="search-subtitle">
              총 {totalResults.toLocaleString()}개의 뉴스를 찾았습니다.
              {/* {totalPages > 0 && ` (페이지 ${currentPage + 1}/${totalPages})`} */}
            </p>
          )}
          
          {/* 언론사별 카테고리 버튼 */}
          {/* <div className="news-sources-section">
            <h3 className="sources-title">📰 언론사별 뉴스</h3>
            <div className="news-sources-grid">
              {newsSources.map((newsSource, index) => (
                <button
                  key={index}
                  className={`source-btn ${source === sourceCodeMap[newsSource] ? 'active' : ''}`}
                  onClick={() => handleSourceClick(newsSource)}
                >
                  {newsSource}
                </button>
              ))}
            </div>
          </div> */}
        </div>

        {isLoading ? (
          <LoadingSpinner />
        ) : (
          <>
            {searchResults.length === 0 ? (
              <div className="no-results">
                <h3>검색 결과가 없습니다</h3>
                <p>다른 검색어나 카테고리를 시도해보세요.</p>
              </div>
            ) : (
              <div className="search-results">
                <div className="results-list">
                  {searchResults.map((article, index) => (
                    <div className="news-list-item" key={article.id || `${index}-${article.headline}`}>
                      <NewsCard
                        article={article}
                        cardType="list"
                        onClick={handleNewsClick}
                      />
                    </div>
                  ))}
                </div>

                {/* 페이지네이션 */}
                {totalPages > 1 && (
                  <div className="pagination">
                    {/* 맨 첫 페이지 버튼 */}
                    <button
                      className="pagination-btn first-last-btn"
                      disabled={currentPage === 0}
                      onClick={() => handlePageChange(0)}
                      title="첫 페이지"
                    >
                      ≪
                    </button>
                    
                    <button
                      className="pagination-btn"
                      disabled={currentPage === 0}
                      onClick={() => handlePageChange(currentPage - 1)}
                    >
                      이전
                    </button>
                    
                    <div className="pagination-numbers">
                      {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                        // 현재 페이지를 중심으로 앞뒤로 2페이지씩 표시
                        const startPage = Math.max(0, currentPage - 2);
                        const page = startPage + i;
                        
                        // 전체 페이지 수를 넘어가면 표시하지 않음
                        if (page >= totalPages) return null;
                        
                        return (
                          <button
                            key={page}
                            className={`pagination-number ${
                              page === currentPage ? 'active' : ''
                            }`}
                            onClick={() => handlePageChange(page)}
                          >
                            {page + 1}
                          </button>
                        );
                      })}
                    </div>
                    
                    <button
                      className="pagination-btn"
                      disabled={currentPage === totalPages - 1}
                      onClick={() => handlePageChange(currentPage + 1)}
                    >
                      다음
                    </button>
                    
                    {/* 맨 마지막 페이지 버튼 */}
                    <button
                      className="pagination-btn first-last-btn"
                      disabled={currentPage === totalPages - 1}
                      onClick={() => handlePageChange(totalPages - 1)}
                      title="마지막 페이지"
                    >
                      ≫
                    </button>
                  </div>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default SearchPage; 