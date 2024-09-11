import axios from "axios";
import authHeader from "./authheader";
import { ClassUnderTestSpec, SheetRequest } from "../model/models";

const API_URL = "http://localhost:8877/api/v1/";

const getProfile = () => {
  return axios.get(API_URL + "auth/me", { headers: authHeader() });
};

const executeSheet = (sheetRequest: SheetRequest) => {
    return axios.post(API_URL + "sheet/execute", sheetRequest);
};

const toLQL = (cutRequest: ClassUnderTestSpec) => {
  return axios.post(API_URL + "sheet/lql", cutRequest);
};

const SheetService = {
    getProfile,
    executeSheet,
    toLQL
};

export default SheetService;