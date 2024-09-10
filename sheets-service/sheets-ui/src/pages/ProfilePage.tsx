import React, { useEffect, useState } from 'react';
import AuthService from '../services/AuthService';
import { User } from '../model/models';

function ProfilePage() {
  const [currentUser, setCurrentUser] = useState<User>()

  useEffect(() => {
    const user = AuthService.getCurrentUser()

    if (user) {
      setCurrentUser(user)
    }
  }, []);

  return (
    <div className="App">
      <h1>Profile {currentUser?.username}</h1>
      <h2>Token {currentUser?.token}</h2>
    </div>
  );
}

export default ProfilePage;
