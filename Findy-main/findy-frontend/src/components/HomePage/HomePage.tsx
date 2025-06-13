import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import NewsCard from '../NewsCard/NewsCard';
import LoadingSpinner from '../LoadingSpinner/LoadingSpinner';
import { useLanguage } from '../../contexts/LanguageContext';
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
  img?: string; // MongoDB의 이미지 필드
  imageUrl?: string; // 기존 호환성을 위한 필드
}

const HomePage: React.FC = () => {
  const [newsData, setNewsData] = useState<NewsArticle[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [popularSearches, setPopularSearches] = useState<string[]>([]);
  const navigate = useNavigate();
  const { t } = useLanguage();

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

  useEffect(() => {
    loadLatestNews();
    loadPopularSearches();
  }, []);

  // 최신 뉴스 로드
  // + 랜덤 추출 10건
  const loadLatestNews = async () => {
    try {
      setIsLoading(true);

      const response = await fetch("http://localhost:8485/api/search?page=0&size=10");

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
          url: item.url || "#",
          img: item.img || null, // MongoDB의 img 필드 추가
          keywords: item.keywords || [] // keywords 필드도 추가
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

  const handleSourceClick = (source: string) => {
    // 한글 언론사명을 영어 코드로 변환
    const sourceCode = sourceCodeMap[source] || source;
    navigate(`/search?source=${encodeURIComponent(sourceCode)}`);
    // 페이지 상단으로 부드럽게 스크롤
    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
  };

  if (isLoading) {
    return <LoadingSpinner />;
  }

  return (
    <main className="home-page">
      <div className="main-content">
        <div className="content-header">
          <h2 className="content-title">{t('home.latestNews')}</h2>
          <p className="content-subtitle">
            {t('home.subtitle')}
          </p>
          
          {/* 언론사별 카테고리 버튼 */}
          {/* <div className="news-sources-section">
            <h2 className="sources-title">📰 언론사별 뉴스</h2>
            <div className="news-sources-grid">
              {newsSources.map((source, index) => (
                <button
                  key={index}
                  className="source-btn"
                  onClick={() => handleSourceClick(source)}
                >
                  {source}
                </button>
              ))}
            </div>
          </div> */}
        </div>

        <div className="news-wrapper">
          {newsData.length === 0 ? (
            <div className="no-results">
              <h3>{t('home.noNews')}</h3>
              <p>{t('home.tryLater')}</p>
            </div>
          ) : (
            <>
              {/* 상단 뉴스 레이아웃: 왼쪽 메인 + 오른쪽 사이드 */}
              <div className="top-news-layout">
                {/* 왼쪽 메인 뉴스카드 */}
                {newsData[0] && (
                  <NewsCard 
                    article={newsData[0]} 
                    cardType="main-large" 
                    onClick={handleNewsClick}
                  />
                )}

                {/* 오른쪽 작은 뉴스카드들 */}
                <div className="side-news-container">
                  {newsData.slice(1, 7).map((article) => (
                    <NewsCard 
                      key={article.id || article.headline}
                      article={article} 
                      cardType="side-small" 
                      onClick={handleNewsClick}
                    />
                  ))}
                </div>
              </div>

              {/* 하단 뉴스 그리드 (4개) */}
              {newsData.length > 7 && (
                <div className="bottom-news-section">
                  <h3 className="bottom-news-title">{t('home.moreNews')}</h3>
                  <div className="bottom-news-grid">
                    {newsData.slice(7, 11).map((article) => (
                      <NewsCard 
                        key={article.id || article.headline}
                        article={article} 
                        cardType="normal" 
                        onClick={handleNewsClick}
                      />
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </main>
  );
};

export default HomePage; 