import React, { useState, useRef, useEffect } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCog, faMoon, faSun } from '@fortawesome/free-solid-svg-icons';
import { useTheme } from '../../contexts/ThemeContext';
import './SettingsButton.css';

const SettingsButton: React.FC = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const { isDarkMode, toggleDarkMode } = useTheme();
  const menuRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);

  // 메뉴 외부 클릭 시 메뉴 닫기
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        menuRef.current && 
        buttonRef.current &&
        !menuRef.current.contains(event.target as Node) &&
        !buttonRef.current.contains(event.target as Node)
      ) {
        setIsMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleSettingsClick = () => {
    setIsMenuOpen(prev => !prev);
  };

  const handleDarkModeToggle = () => {
    toggleDarkMode();
    setIsMenuOpen(false); // 다크모드 변경 후 메뉴 닫기
  };

  return (
    <div className="settings-container">
      <button 
        ref={buttonRef}
        className="settings-button"
        onClick={handleSettingsClick}
        aria-label="설정"
        title="설정"
      >
        <FontAwesomeIcon icon={faCog} />
      </button>
      
      {isMenuOpen && (
        <div ref={menuRef} className="settings-menu">
          <button 
            className="settings-menu-item"
            onClick={handleDarkModeToggle}
          >
            <FontAwesomeIcon 
              icon={isDarkMode ? faSun : faMoon} 
              className="menu-icon"
            />
            <span>{isDarkMode ? '라이트 모드' : '다크 모드'}</span>
          </button>
        </div>
      )}
    </div>
  );
};

export default SettingsButton; 