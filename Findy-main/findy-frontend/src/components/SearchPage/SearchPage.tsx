import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import NewsCard from '../NewsCard/NewsCard';
import LoadingSpinner from '../LoadingSpinner/LoadingSpinner';
import './SearchPage.css';

interface NewsArticle {
  id?: string;
  category: string;
  title: string;
  content: string;
  publishedAt: string;
  tags: string[];
  url: string;
}

interface SearchResult {
  content: NewsArticle[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
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

      // api 적용시
      // const response = await fetch(`http://localhost:8485/api/search?${params.toString()}`);
      const response = await fetch(`/api/search?${params.toString()}`);
      
      if (response.ok) {
        const data: SearchResult = await response.json();
        setSearchResults(data.content || []);
        setTotalResults(data.totalElements || 0);
        setTotalPages(data.totalPages || 0);
      } else {
        throw new Error('검색 실패');
      }
    } catch (error) {
      console.error('검색 오류:', error);
      // 검색 실패 시 빈 결과 표시
      setSearchResults([]);
      setTotalResults(0);
      setTotalPages(0);
    } finally {
      setIsLoading(false);
    }
  };

  const generateDummyResults = (): SearchResult => {
    // 총 데이터 수
    const totalElements = 57;
    // 페이지당 아이템 수
    const pageSize = 10;
    // 총 페이지 수
    const totalPages = Math.ceil(totalElements / pageSize);
    
    // 현재 페이지에 표시할 데이터 계산
    const startIdx = currentPage * pageSize;
    const endIdx = Math.min(startIdx + pageSize, totalElements);
    
    // 현재 페이지에 표시할 데이터만 생성
    const content = Array.from({ length: endIdx - startIdx }, (_, index) => {
      const globalIndex = startIdx + index;
      
      return {
        id: `${globalIndex + 1}`,
        category: getRandomCategory(globalIndex),
        title: `${query || category || '검색어'} 관련 뉴스 ${globalIndex + 1}: ${getRandomTitle(globalIndex)}`,
        content: getRandomContent(globalIndex, query || category || '검색어'),
        publishedAt: getRandomDate(globalIndex),
        tags: getRandomTags(globalIndex, query || category || '뉴스'),
        url: '#'
      };
    });
    
    return {
      content,
      totalElements,
      totalPages,
      currentPage
    };
  };
  
  const getRandomCategory = (index: number): string => {
    const categories = ['경제', '정치', '사회', '오피니언', '건강', '연예/문화', '스포츠'];
    return categories[index % categories.length];
  };
  
  const getRandomTitle = (index: number): string => {
    const titles = [
      '주요 동향 분석',
      '최신 발표 내용',
      '전문가 의견',
      '시장 전망 보고서',
      '충격적인 발견',
      '새로운 연구 결과',
      '빠르게 변화하는 트렌드'
    ];
    return titles[index % titles.length];
  };
  
  const getRandomContent = (index: number, keyword: string): string => {
    const contents = [
      `${keyword}와 관련된 중요한 발표가 있었습니다. 관련 전문가들은 이번 발표가 향후 정책 방향에 큰 영향을 미칠 것으로 예상한다고 밝혔습니다.`,
      `최근 ${keyword}와 관련된 시장 동향을 분석한 결과, 향후 전망이 긍정적인 것으로 나타났습니다. 업계 관계자들은 지속적인 성장세를 보일 것으로 전망한다고 말했습니다.`,
      `${keyword}와 관련된 이슈가 시민들의 높은 관심을 받고 있습니다. 관련 단체들은 이번 기회에 건설적인 논의가 이루어지기를 바란다고 표명했습니다.`,
      `${keyword}에 관한 새로운 연구결과가 발표되었습니다. 이번 연구는 기존의 이론을 뒤집는 내용을 담고 있어 학계의 큰 반향을 일으키고 있습니다.`,
      `최근 ${keyword} 분야에서 주목할 만한 변화가 감지되고 있습니다. 전문가들은 이러한 변화가 장기적으로 긍정적인 영향을 미칠 것으로 전망하고 있습니다.`,
      `${keyword}와 관련하여 정부가 새로운 정책을 발표했습니다. 이번 정책은 그동안 지적되어온 문제점들을 해결하기 위한 방안을 담고 있습니다.`,
      `글로벌 ${keyword} 시장에서 한국 기업들의 약진이 두드러지고 있습니다. 국내 기업들의 기술력과 경쟁력이 세계적으로 인정받고 있다는 평가입니다.`
    ];
    return contents[index % contents.length];
  };
  
  const getRandomDate = (index: number): string => {
    const day = 25 - (index % 25);
    const month = 1 + (index % 3);
    return `2025-0${month}-${day < 10 ? '0' + day : day}`;
  };
  
  const getRandomTags = (index: number, keyword: string): string[] => {
    const tagSets = [
      [keyword, '정책', '발표'],
      [keyword, '시장', '분석'],
      [keyword, '이슈', '관심'],
      [keyword, '연구', '발견'],
      [keyword, '트렌드', '변화'],
      [keyword, '기업', '산업'],
      [keyword, '글로벌', '경쟁']
    ];
    return tagSets[index % tagSets.length];
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
                    <div className="news-list-item" key={article.id || `${index}-${article.title}`}>
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