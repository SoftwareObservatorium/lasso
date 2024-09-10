import React from 'react';
import { useLocation, Navigate } from 'react-router-dom';
import AuthService from '../../services/AuthService';

const PrivateRoute = ({ user: User, children, redirect }: any) => {
  const authenticate = AuthService.isLoggedIn()
  const location = useLocation();
  return authenticate ? (
    children
  ) : (
    <Navigate
      to={`/login?redirect=${encodeURIComponent(redirect || location.pathname)}`}
    />
  );
};

export default PrivateRoute;