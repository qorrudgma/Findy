import React, { useState, useEffect } from 'react';
import NewsCard from '../NewsCard/NewsCard';
import LoadingSpinner from '../LoadingSpinner/LoadingSpinner';
import './HomePage.css';

interface NewsArticle {
  id?: string;
  category: string;
  title: string;
  content: string;
  publishedAt: string;
  tags: string[];
  url: string;
}

const HomePage: React.FC = () => {
  const [newsData, setNewsData] = useState<NewsArticle[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [popularSearches, setPopularSearches] = useState<string[]>([]);

  // 더미 뉴스 데이터
  const dummyNews: NewsArticle[] = [
    {
      id: '1',
      category: "해킹",
      title: "이제부터 여긴 정종현 갤러리다",
      content: "하루 한번 정종현을 바라보고 하루 두번 정종현이 있는 쪽으로 절해라",
      publishedAt: "2025-01-22",
      tags: ["믿음", "신앙", "종교", "헛소리"],
      url: "#"
    },
    {
      id: '2',
      category: "경제",
      title: "주요 기업들 실적 호조, 코스피 상승세 지속",
      content: "이번 분기 주요 기업들의 실적이 예상을 웃돌면서 증시가 강세를 보이고 있습니다. 특히 IT 및 바이오 섹터에서 두드러진 성과를 나타내며 투자자들의 관심이 집중되고 있습니다.",
      publishedAt: "2025-01-22",
      tags: ["주식", "실적", "코스피", "IT"],
      url: "#"
    },
    {
      id: '3',
      category: "사회",
      title: "전국 대학교 새 학기 시작, 대면 수업 확대",
      content: "전국 대학교들이 새 학기를 맞아 대면 수업을 크게 확대하기로 결정했습니다. 학생들과 교수진은 정상적인 캠퍼스 생활로의 복귀를 환영하며, 다양한 교육 프로그램들이 재개될 예정입니다.",
      publishedAt: "2025-01-22",
      tags: ["대학교", "새학기", "대면수업", "교육"],
      url: "#"
    },
    {
      id: '4',
      category: "오피니언",
      title: "AI 시대, 우리가 준비해야 할 것들",
      content: "인공지능 기술이 급속도로 발전하면서 우리 사회에 미치는 영향이 점점 커지고 있습니다. 교육부터 일자리까지, 변화에 대비한 준비가 필요합니다.",
      publishedAt: "2025-01-21",
      tags: ["AI", "미래", "준비", "기술"],
      url: "#"
    },
    {
      id: '5',
      category: "건강",
      title: "겨울철 건강관리, 면역력 강화 방법",
      content: "겨울철 건강을 위해서는 규칙적인 운동과 충분한 수면, 균형 잡힌 영양섭취가 중요합니다. 전문가들이 제시하는 면역력 강화 방법을 알아봅시다.",
      publishedAt: "2025-01-21",
      tags: ["건강", "면역력", "겨울", "운동"],
      url: "#"
    },
    {
      id: '6',
      category: "연예/문화",
      title: "K-POP 새로운 글로벌 트렌드 주도",
      content: "한국 아이돌 그룹들이 전 세계 음악 차트를 석권하며 새로운 문화 트렌드를 만들어가고 있습니다. 글로벌 팬들의 열띤 반응이 이어지고 있습니다.",
      publishedAt: "2025-01-21",
      tags: ["K-POP", "한류", "글로벌", "음악"],
      url: "#"
    },
    {
      id: '7',
      category: "스포츠",
      title: "프로야구 시즌 개막, 팬들의 뜨거운 관심",
      content: "2025년 프로야구 시즌이 개막되면서 각 팀의 새로운 전력과 기대주들에게 관심이 집중되고 있습니다. 올해는 특히 치열한 경쟁이 예상됩니다.",
      publishedAt: "2025-01-20",
      tags: ["야구", "시즌개막", "프로야구", "스포츠"],
      url: "#"
    },
    {
      id: '8',
      category: "문화",
      title: "전지적 독자 시점 실사화 개봉 예정",
      content: "2025년 7월 초 인기 웹툰 '전지적 독자 시점'의 실사화 영화가 개봉될 예정입니다. 원작 팬들의 기대가 높아지고 있습니다.",
      publishedAt: "2025-01-20",
      tags: ["문화", "웹툰", "영화", "실사화"],
      url: "#"
    }
  ];

  useEffect(() => {
    loadLatestNews();
    loadPopularSearches();
  }, []);

  // 최신 뉴스 로드
  const loadLatestNews = async () => {
    try {
      setIsLoading(true);

      const response = await fetch("http://localhost:8485/api/search?q=뉴스&page=0&size=10");
      if (response.ok) {
        const rawData = await response.json();

        const mappedData: NewsArticle[] = rawData.map((item: any) => ({
          id: item.id || item.url,  // 없으면 URL을 임시 ID로 사용
          category: item.category,
          title: item.headline,  // 🔁 매핑!
          content: item.content,
          publishedAt: item.time,  // 🔁 매핑!
          tags: item.tags || [],  // 누락 시 빈 배열
          url: item.url,
        }));

        setNewsData(mappedData);
      } else {
        throw new Error('API 호출 실패');
      }
    } catch (error) {
      console.error('뉴스 로드 오류:', error);
      setNewsData(dummyNews);
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
          {popularSearches.length > 0 && (
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
          )}
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
                          key={article.id || article.title} 
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
                          key={article.id || article.title} 
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
                        key={article.id || article.title} 
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