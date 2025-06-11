import React, { useState, useEffect } from 'react';
import NewsCard from '../NewsCard/NewsCard';
import LoadingSpinner from '../LoadingSpinner/LoadingSpinner';
import './HomePage.css';

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

const HomePage: React.FC = () => {
  const [newsData, setNewsData] = useState<NewsArticle[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [popularSearches, setPopularSearches] = useState<string[]>([]);



  useEffect(() => {
    loadLatestNews();
    loadPopularSearches();
  }, []);

  // 최신 뉴스 로드
  // + 랜덤 추출 10건
  const loadLatestNews = async () => {
    try {
      setIsLoading(true);

      const response = await fetch("http://localhost:8485/api/search?page=0&size=100");

      if (response.ok) {
        const rawData = await response.json();
        const rawContent = Array.isArray(rawData.content) ? rawData.content : [];
        
        const mappedData: NewsArticle[] = rawContent.map((item: any) => ({
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

        const randomizedTop10 = [...mappedData]
            .sort(() => Math.random() - 0.5)
            .slice(0, 10);

        setNewsData(randomizedTop10);
      } else {
        console.error(" API 호출 실패 (status code):", response.status);
        throw new Error('API 호출 실패');
      }
    } catch (error) {
      console.error(' 뉴스 로드 오류:', error);
      // setNewsData(dummyNews);  // 더미 데이터로 대체
    } finally {
      setIsLoading(false);
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

  const handleNewsClick = (article: NewsArticle) => {
    if (article.url && article.url !== '#') {
      window.open(article.url, '_blank', 'noopener,noreferrer');
    }
  };

  if (isLoading) {
    return <LoadingSpinner />;
  }

  return (
    <main className="home-page">
      <div className="main-content">
        <div className="content-header">
          <h2 className="content-title">최신 뉴스</h2>
          <p className="content-subtitle">
            AI 기반 검색으로 정확하고 빠른 뉴스를 만나보세요
          </p>
          
          {/* 인기 검색어 표시 */}
          {/* {popularSearches.length > 0 && (
            <div className="popular-searches-home">
              <span className="popular-label">🔥 인기 검색어:</span>
              {popularSearches.map((search, index) => (
                <button
                  key={index}
                  className="popular-tag-home"
                  onClick={() => {
                    window.location.href = `/search?q=${encodeURIComponent(search)}`;
                  }}
                >
                  {search}
                </button>
              ))}
            </div>
          )} */}
        </div>

        <div className="news-wrapper">
          {/* 왼쪽 여백 공간 */}
          <div className="side-space left-space">
          {/* <img src="/images/jjh1.jpg" alt="Findy Logo" className="side-space-image" /> */}
            <p>광고 또는 추가 콘텐츠 영역</p>
          </div>
          
          {/* 뉴스 그리드 */}
          <div className="news-grid">
            {/* 뉴스 그리드 */}
            {newsData.length === 0 ? (
              <div className="no-results">
                <h3>뉴스가 없습니다</h3>
                <p>잠시 후 다시 시도해주세요.</p>
              </div>
            ) : (
              <>
                {/* 메인 뉴스 섹션 */}
                <div className="main-news-section">
                  {/* 중앙 메인 뉴스 */}
                  <div className="center-news-container">
                    {newsData[0] && (
                      <NewsCard 
                        article={newsData[0]} 
                        cardType="main-primary" 
                        onClick={handleNewsClick}
                      />
                    )}
                    {newsData[1] && (
                      <NewsCard 
                        article={newsData[1]} 
                        cardType="main-secondary" 
                        onClick={handleNewsClick}
                      />
                    )}
                  </div>

                  {/* 왼쪽 사이드 뉴스 */}
                  {newsData.length > 2 && (
                    <div className="side-news-container left">
                      {newsData.slice(2, 5).map((article) => (
                        <NewsCard 
                          key={article.id || article.headline} 
                          article={article} 
                          cardType="side" 
                          onClick={handleNewsClick}
                        />
                      ))}
                    </div>
                  )}

                  {/* 오른쪽 사이드 뉴스 */}
                  {newsData.length > 5 && (
                    <div className="side-news-container right">
                      {newsData.slice(5, 8).map((article) => (
                        <NewsCard 
                          key={article.id || article.headline} 
                          article={article} 
                          cardType="side" 
                          onClick={handleNewsClick}
                        />
                      ))}
                    </div>
                  )}
                </div>

                {/* 추가 뉴스 그리드 */}
                {newsData.length > 8 && (
                  <div className="additional-news-grid">
                    {newsData.slice(8).map((article) => (
                      <NewsCard 
                        key={article.id || article.headline} 
                        article={article} 
                        cardType="normal" 
                        onClick={handleNewsClick}
                      />
                    ))}
                  </div>
                )}
              </>
            )}
          </div>
          
          {/* 오른쪽 여백 공간 */}
          <div className="side-space right-space">
          {/* <img src="/images/jjh2.jpg" alt="Findy Logo" className="side-space-image" /> */}
            <p>광고 또는 추가 콘텐츠 영역</p>
          </div>
        </div>
      </div>
    </main>
  );
};

export default HomePage; 