import React from 'react';
import logo from '../logo.svg';
import '../App.css';

function HomePage() {
  return (
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
        <p>
          Sheet Editor
        </p>
        
      </header>
    </div>
  );
}

export default HomePage;
