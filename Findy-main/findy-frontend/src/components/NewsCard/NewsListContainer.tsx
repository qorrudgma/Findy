import React, { useState, useEffect } from 'react';
import ExpandableNewsCard from './ExpandableNewsCard';
import './NewsListContainer.css';

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
  imageUrl?: string;
  img?: string;
}

interface NewsListContainerProps {
  articles: NewsArticle[];
  onArticleClick: (article: NewsArticle) => void;
  onExpandedChange?: (isExpanded: boolean) => void;
}

const NewsListContainer: React.FC<NewsListContainerProps> = ({ articles, onArticleClick, onExpandedChange }) => {
  const [expandedArticleId, setExpandedArticleId] = useState<string | null>(null);

  const handleToggle = (articleId: string) => {
    const newExpandedId = expandedArticleId === articleId ? null : articleId;
    setExpandedArticleId(newExpandedId);
    
    // 토글 시 스크롤을 상단으로 이동
    if (newExpandedId !== null) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
    
    if (onExpandedChange) {
      onExpandedChange(newExpandedId !== null);
    }
  };

  useEffect(() => {
    if (onExpandedChange) {
      onExpandedChange(expandedArticleId !== null);
    }
  }, [expandedArticleId, onExpandedChange]);

  const expandedArticle = expandedArticleId 
    ? articles.find(article => (article.id || `article-${articles.indexOf(article)}`) === expandedArticleId)
    : null;

  return (
    <div className={`news-list-container ${expandedArticleId ? 'has-expanded' : ''}`}>
      {/* 왼쪽 리스트 영역 */}
      <div className="news-list-sidebar">
        {articles.map((article, index) => {
          // id가 없는 경우 인덱스를 사용
          const articleId = article.id || `article-${index}`;
          
          return (
            <ExpandableNewsCard
              key={articleId}
              article={{ ...article, id: articleId }}
              onClick={onArticleClick}
              isExpanded={false} // 리스트에서는 항상 축소된 형태로 표시
              onToggle={handleToggle}
            />
          );
        })}
      </div>

      {/* 오른쪽 확장된 콘텐츠 영역 */}
      {expandedArticle && (
        <div className="expanded-news-content">
          <ExpandableNewsCard
            article={expandedArticle}
            onClick={onArticleClick}
            isExpanded={true}
            onToggle={handleToggle}
          />
        </div>
      )}
    </div>
  );
};

export default NewsListContainer; 