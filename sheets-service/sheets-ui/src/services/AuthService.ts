import axios from "axios";
import { User } from "../model/models";

const API_URL = process.env.REACT_APP_SHEETS_URL;

// FIXME
const register = (username: string, email: string, password: string) => {
    return axios.post(API_URL + "auth/signup", {
        username,
        email,
        password,
    });
};

const login = (username: string, password: string) => {
    return axios
        .post<User>(API_URL + "auth/signin", {
            username,
            password,
        })
        .then((response) => {
            if (response.data.token) {
                localStorage.setItem("user", JSON.stringify(response.data));
            }

            return response.data;
        });
};

const logout = () => {
    localStorage.removeItem("user");
};

const getCurrentUser = () => {
    return <User>JSON.parse(localStorage.getItem("user") || '{}');
};

const isLoggedIn  = () => {
    if(getCurrentUser()?.username) {
        return true
    }

    return false
};

const AuthService = {
    register,
    login,
    logout,
    getCurrentUser,
    isLoggedIn
};

export default AuthService;