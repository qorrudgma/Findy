import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Header from './components/Header/Header';
import Footer from './components/Footer/Footer';
import HomePage from './components/HomePage/HomePage';
import SearchPage from './components/SearchPage/SearchPage';
import TermsOfService from './components/TermsOfService/TermsOfService';
import CopyrightPolicy from './components/CopyrightPolicy/CopyrightPolicy';
import PrivacyPolicy from './components/PrivacyPolicy/PrivacyPolicy';
import AdPolicy from './components/AdPolicy/AdPolicy';
import FAQ from './components/FAQ/FAQ';
import FAQDetail from './components/FAQDetail/FAQDetail';
import ContactForm from './components/ContactForm/ContactForm';
import './styles/common.css';
import './styles/App.css';

function App() {
  return (
    <Router>
      <div className="App">
        <Header />
        <main className="main-container">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/terms" element={<TermsOfService />} />
            <Route path="/copyright" element={<CopyrightPolicy />} />
            <Route path="/privacy" element={<PrivacyPolicy />} />
            <Route path="/ad-policy" element={<AdPolicy />} />
            <Route path="/faq" element={<FAQ />} />
            <Route path="/faq/detail/:id" element={<FAQDetail />} />
            <Route path="/contact" element={<ContactForm />} />
          </Routes>
        </main>
        <Footer />
      </div>
    </Router>
  );
}

export default App; 