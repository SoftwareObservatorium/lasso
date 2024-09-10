import React, { useEffect, useState } from 'react';
import logo from './logo.svg';
import './App.css';

import HomePage from './pages/HomePage';
import { Route, Routes } from 'react-router-dom';
import RegisterPage from './pages/RegisterPage';
import LoginPage from './pages/LoginPage';
import ProfilePage from './pages/ProfilePage';
import SheetEditorPage from './pages/SheetEditorPage';
import MenuAppBar from './components/navigation/MenuAppBar';
import { User } from './model/models';
import AuthService from './services/AuthService';
import PrivateRoute from './components/navigation/PrivateRoute';

function App() {
  const [currentUser, setCurrentUser] = useState<User>()

  useEffect(() => {
    const user = AuthService.getCurrentUser()

    if (user) {
      setCurrentUser(user)
    }
  }, []);

  const logOut = () => {
    AuthService.logout();
  };

  const wrapPrivateRoute = (element: any, user: User | undefined, redirect: any) => {
    return (
      <PrivateRoute user={user} redirect={redirect}>
        {element}
      </PrivateRoute>
    );
  };

  return (
    <div className="App">

      <MenuAppBar />

      <div>
        <Routes>
          <Route path="/" element={<HomePage/>} />
          <Route path="/home" element={<HomePage/>} />
          <Route path="/login" element={<LoginPage/>} />
          <Route path="/register" element={<RegisterPage/>} />
          <Route path="/profile" element={wrapPrivateRoute(<ProfilePage />, currentUser, 'profile')} />
          <Route path="/editor" element={wrapPrivateRoute(<SheetEditorPage />, currentUser, 'editor')} />
          
        </Routes>
      </div>

    </div>
  );
}

export default App;
