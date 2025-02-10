import axios from "axios";
import authHeader from "./authheader";
import { ClassUnderTestSpec, CodeGenerationRequest, CodeSearchRequest, LqlGenerationRequest, SheetGenerationRequest, SheetRequest } from "../model/models";

const API_URL = process.env.REACT_APP_SHEETS_URL;

const getProfile = () => {
  return axios.get(API_URL + "auth/me", { headers: authHeader() });
};

const executeSheet = (sheetRequest: SheetRequest) => {
    return axios.post(API_URL + "sheet/execute", sheetRequest);
};

const generateSheets = (sheetRequest: SheetGenerationRequest) => {
  return axios.post(API_URL + "sheet/generate", sheetRequest);
};

const toLQL = (cutRequest: ClassUnderTestSpec) => {
  return axios.post(API_URL + "sheet/lql", cutRequest);
};

const searchCodeModules = (codeSearchRequest: CodeSearchRequest) => {
  return axios.post(API_URL + "cut/search", codeSearchRequest);
};

const generateCodeModules = (codeGenerationRequest: CodeGenerationRequest) => {
  return axios.post(API_URL + "cut/generate", codeGenerationRequest);
};

const generateInterface = (lqlGenerationRequest: LqlGenerationRequest) => {
  return axios.post(API_URL + "lql/generate", lqlGenerationRequest);
};

const retrieveParquet = (executionId: string) => {
  return axios.get(API_URL + "srm/" + executionId + ".parquet", {responseType: 'arraybuffer'});
};

const retrieveParquetUrl = (executionId: string) => {
  return API_URL + "srm/" + executionId + ".parquet";
};

const SheetService = {
    getProfile,
    executeSheet,
    toLQL,
    retrieveParquet,
    searchCodeModules,
    generateCodeModules,
    generateSheets,
    generateInterface,
    retrieveParquetUrl
};

export default SheetService;