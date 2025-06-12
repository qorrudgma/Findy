import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import './Footer.css';

/**
 * 푸터 컴포넌트
 * 수정사항:
 * - 모든 링크 클릭 시 상단 스크롤 기능 추가
 * - handleNavigation 함수로 통합 처리
 * - handleLinkClick 함수로 외부 링크 처리
 */
const Footer: React.FC = () => {
  const navigate = useNavigate();

  // 내부 네비게이션 처리 및 상단 스크롤
  const handleNavigation = (path: string) => {
    navigate(path);
    // 페이지 이동 후 상단으로 부드럽게 스크롤
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // 외부 링크 클릭 처리 및 상단 스크롤
  const handleLinkClick = (url: string) => {
    window.open(url, '_blank', 'noopener,noreferrer');
    // 외부 링크 클릭 후 상단으로 부드럽게 스크롤
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <footer className="footer">
      <div className="footer-container">
        <div className="footer-content">
          <div className="footer-section">
            <h3 className="footer-title">Findy 뉴스</h3>
            <p className="footer-description">
              AI 기반 뉴스 검색 엔진으로 정확하고 빠른 뉴스를 제공합니다.
            </p>
            <p className="footer-description">
              신뢰할 수 있는 정보, 스마트한 검색 경험을 만나보세요.
            </p>
            {/*  소셜 미디어 링크  */}
            <div className="footer-social">
                <a href="#" className="social-btn" title="페이스북" onClick={() => handleLinkClick('#')} target="_blank">📘</a>
                <a href="#" className="social-btn" title="트위터" onClick={() => handleLinkClick('#')} target="_blank">🐦</a>
                <a href="#" className="social-btn" title="인스타그램" onClick={() => handleLinkClick('#')} target="_blank">📷</a>
                <a href="#" className="social-btn" title="유튜브" onClick={() => handleLinkClick('#')} target="_blank">📺</a>
            </div>
          </div>
          
          <div className="footer-section">
            <h3 className="footer-subtitle">서비스</h3>
            <ul className="footer-links">
              <li><Link to="/" className="footer-link" onClick={() => handleNavigation('/')}>뉴스 검색</Link></li>
              <li><Link to="/search" className="footer-link" onClick={() => handleNavigation('/search')}>카테고리별 뉴스</Link></li>
              <li><a href="#" className="footer-link" onClick={() => handleLinkClick('#')}>인기 검색어</a></li>
              <li><a href="#" className="footer-link" onClick={() => handleLinkClick('#')}>실시간 뉴스</a></li>
              <li><a href="#" className="footer-link" onClick={() => handleLinkClick('#')}>북마크</a></li>
            </ul>
          </div>
          
          <div className="footer-section">
            <h3 className="footer-subtitle">고객지원</h3>
            <ul className="footer-links">
              <li><Link to="/faq" className="footer-link" onClick={() => handleNavigation('/faq')}>자주 묻는 질문</Link></li>
              <li><a href="#" className="footer-link" onClick={() => handleLinkClick('#')}>고객센터</a></li>
              <li><Link to="/contact" className="footer-link" onClick={() => handleNavigation('/contact')}>개선 제안</Link></li>
              <li><Link to="/contact" className="footer-link" onClick={() => handleNavigation('/contact')}>버그 신고</Link></li>
              <li><Link to="/contact" className="footer-link" onClick={() => handleNavigation('/contact')}>문의하기</Link></li>
            </ul>
          </div>
          
          <div className="footer-section">
            <h3 className="footer-subtitle">정책 및 약관</h3>
            <ul className="footer-links">
              <li><Link to="/terms" className="footer-link" onClick={() => handleNavigation('/terms')}>이용약관</Link></li>
              <li><Link to="/privacy" className="footer-link" onClick={() => handleNavigation('/privacy')}>개인정보처리방침</Link></li>
              <li><Link to="/copyright" className="footer-link" onClick={() => handleNavigation('/copyright')}>저작권 정책</Link></li>
              <li><Link to="/ad-policy" className="footer-link" onClick={() => handleNavigation('/ad-policy')}>뉴스 제공업체</Link></li>
              <li><Link to="/ad-policy" className="footer-link" onClick={() => handleNavigation('/ad-policy')}>광고 정책</Link></li>
            </ul>
          </div>
        </div>
        
        <div className="footer-bottom">
          <p className="copyright">
          &copy; 2025 Findy News. All rights reserved. | 대표: 팀 프로젝트 | 사업자등록번호: 123-45-67890
          </p>
          <p >부산광역시 부산진구 해동로 100, Findy 빌딩 | 대표전화: 010-1234-5678 | 이메일: qorrudgma@naver.com
          </p>
        </div>
      </div>
    </footer>
  );
};

export default Footer; 