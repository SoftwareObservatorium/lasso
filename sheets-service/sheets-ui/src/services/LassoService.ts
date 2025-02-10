import axios from "axios";
import authHeader from "./authheader";
import { ClassUnderTestSpec, CodeGenerationRequest, CodeSearchRequest, LqlGenerationRequest, SheetGenerationRequest, SheetRequest } from "../model/models";

const API_URL = process.env.REACT_APP_LASSO_URL;

const getProfile = () => {
  return axios.get(API_URL + "auth/me", { headers: authHeader() });
};

const retrieveParquet = (executionId: string) => {
  return axios.get(API_URL + "publicapi/v1/lasso/analytics/raw/srm/" + executionId + "_all.parquet", {responseType: 'arraybuffer'});
};

const retrieveParquetUrl = (executionId: string) => {
  return API_URL + "publicapi/v1/lasso/analytics/raw/srm/" + executionId + "_all.parquet";
};

const LassoService = {
    getProfile,
    retrieveParquet,
    retrieveParquetUrl
};

export default LassoService;