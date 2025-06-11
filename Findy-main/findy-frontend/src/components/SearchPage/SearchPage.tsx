import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
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

  const query = searchParams.get('q') || '';
  const category = searchParams.get('category') || '';

  useEffect(() => {
    if (query || category) {
      performSearch();
    }
  }, [query, category, currentPage]);

  const performSearch = async () => {
    try {
      setIsLoading(true);
      
      const params = new URLSearchParams();
      if (query) params.append('q', query);
      if (category) params.append('category', category);
      params.append('page', currentPage.toString());
      params.append('size', '10');

      // 백엔드 서버 주소로 직접 호출
      console.log('API 호출 시작:', `http://localhost:8485/api/search?${params.toString()}`);
      const response = await fetch(`http://localhost:8485/api/search?${params.toString()}`);
      
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

  const getSearchTitle = () => {
    if (query && category) {
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