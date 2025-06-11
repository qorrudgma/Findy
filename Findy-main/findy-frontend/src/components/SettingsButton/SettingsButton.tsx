import React from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCog } from '@fortawesome/free-solid-svg-icons';
import './SettingsButton.css';

const SettingsButton: React.FC = () => {
  // 나중에 실제 설정 기능으로 대체할 더미 함수
  const handleSettingsClick = () => {
    console.log('설정 버튼 클릭됨 - 여기에 설정 기능 추가 예정');
    // 임시로 알림 표시
    alert('설정 기능은 곧 추가될 예정입니다!');
  };

  return (
    <button 
      className="settings-button"
      onClick={handleSettingsClick}
      aria-label="설정"
      title="설정"
    >
      <FontAwesomeIcon icon={faCog} />
    </button>
  );
};

export default SettingsButton; 