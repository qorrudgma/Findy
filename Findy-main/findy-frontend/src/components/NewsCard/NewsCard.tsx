import React from 'react';
import './NewsCard.css';

interface NewsArticle {
  id?: string;
  category: string;
  title: string;
  content: string;
  publishedAt: string;
  tags: string[];
  url: string;
}

interface NewsCardProps {
  article: NewsArticle;
  cardType: 'main-primary' | 'main-secondary' | 'side' | 'normal' | 'list';
  onClick: (article: NewsArticle) => void;
}

const NewsCard: React.FC<NewsCardProps> = ({ article, cardType, onClick }) => {
  const handleClick = () => {
    onClick(article);
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const getCardClassName = () => {
    switch (cardType) {
      case 'main-primary':
        return 'main-news-card primary';
      case 'main-secondary':
        return 'main-news-card secondary';
      case 'side':
        return 'side-news-card';
      case 'normal':
        return 'news-card';
      case 'list':
        return 'list-news-card';
      default:
        return 'news-card';
    }
  };

  const renderMainCard = (isPrimary: boolean) => (
    <div className={getCardClassName()} onClick={handleClick}>
      <div className="main-news-meta">
        <span className="main-news-category">{article.category}</span>
        <span className="main-news-date">{formatDate(article.publishedAt)}</span>
      </div>
      <h3 className="main-news-title">{article.title}</h3>
      <p className="main-news-summary">{article.content}</p>
      <div className="main-news-tags">
        {article.tags && article.tags.map((tag, index) => (
          <span key={index} className="main-news-tag">{tag}</span>
        ))}
      </div>
    </div>
  );

  const renderSideCard = () => (
    <div className={getCardClassName()} onClick={handleClick}>
      <div className="side-news-meta">
        <span className="side-news-category">{article.category}</span>
        <span className="side-news-date">{formatDate(article.publishedAt)}</span>
      </div>
      <h4 className="side-news-title">{article.title}</h4>
      <p className="side-news-summary">{article.content}</p>
      <div className="side-news-tags">
        {article.tags && article.tags.slice(0, 3).map((tag, index) => (
          <span key={index} className="side-news-tag">{tag}</span>
        ))}
      </div>
    </div>
  );

  const renderNormalCard = () => (
    <div className={getCardClassName()} onClick={handleClick}>
      <div className="news-meta">
        <span className="news-category">{article.category}</span>
        <span className="news-date">{formatDate(article.publishedAt)}</span>
      </div>
      <h4 className="news-title">{article.title}</h4>
      <p className="news-summary">{article.content}</p>
      <div className="news-tags">
        {article.tags && article.tags.slice(0, 4).map((tag, index) => (
          <span key={index} className="news-tag">{tag}</span>
        ))}
      </div>
    </div>
  );

  const renderListCard = () => (
    <div className={getCardClassName()} onClick={handleClick}>
      <div className="list-news-content">
        <div className="list-news-main">
          <div className="list-news-meta">
            <span className="list-news-category">{article.category}</span>
            <span className="list-news-date">{formatDate(article.publishedAt)}</span>
          </div>
          <h3 className="list-news-title">{article.title}</h3>
          <p className="list-news-summary">{article.content}</p>
        </div>
        <div className="list-news-tags">
          {article.tags && article.tags.map((tag, index) => (
            <span key={index} className="list-news-tag">{tag}</span>
          ))}
        </div>
      </div>
    </div>
  );

  switch (cardType) {
    case 'main-primary':
      return renderMainCard(true);
    case 'main-secondary':
      return renderMainCard(false);
    case 'side':
      return renderSideCard();
    case 'normal':
      return renderNormalCard();
    case 'list':
      return renderListCard();
    default:
      return renderNormalCard();
  }
};

export default NewsCard; 