///
/// LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
/// Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
///
/// This file is part of LASSO.
///
/// LASSO is free software: you can redistribute it and/or modify
/// it under the terms of the GNU General Public License as published by
/// the Free Software Foundation, either version 3 of the License, or
/// (at your option) any later version.
///
/// LASSO is distributed in the hope that it will be useful,
/// but WITHOUT ANY WARRANTY; without even the implied warranty of
/// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/// GNU General Public License for more details.
///
/// You should have received a copy of the GNU General Public License
/// along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
///

import { Injectable } from '@angular/core';

import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from 'src/environments/environment';

import { LslRequest, LslResponse, RecordsRequest, ImplementationRequest, ReportRequest, FileViewRequest, SearchQueryRequest } from '../model/lsl';

@Injectable({
  providedIn: 'root'
})
export class LassoApiServiceService {

  constructor(private http: HttpClient) {
  }

  /**
   * Execute LSL Script
   * 
   * @param lslRequest 
   * 
   */
  execute(lslRequest: LslRequest) {
    return this.http.post<any>(`${environment.apiUrl}/api/v1/lasso/execute`, lslRequest)
        .pipe(map(lslResponse => {
            // do something

            //console.log(lslResponse)

            return lslResponse;
        }));
  }

  /**
   * LSL Script to graph
   * 
   * @param lslRequest 
   * 
   */
  graph(lslRequest: LslRequest) {
    return this.http.post<any>(`${environment.apiUrl}/api/v1/lasso/graph`, lslRequest)
        .pipe(map(graphResponse => {
            // do something

            console.log(graphResponse)

            return graphResponse;
        }));
  }

  /**
   * Poll Status of LSL Script
   * 
   * @param lslRequest 
   * 
   */
  executionStatus(executionId: String) {
    return this.http.get<any>(`${environment.apiUrl}/api/v1/lasso/scripts/${executionId}/status`)
        .pipe(map(result => {
            return result;
        }));
  }

  /**
   * Get available actions and their description.
   * 
   */
  actions() {
    return this.http.get<any>(`${environment.apiUrl}/api/v1/lasso/info`)
        .pipe(map(response => {
            // do something

            //console.log(response)

            return response;
        }));
  }

    /**
   * Get available scripts and their status
   * 
   */
  scripts() {
    return this.http.get<any>(`${environment.apiUrl}/api/v1/lasso/scripts`)
        .pipe(map(response => {
            // do something

            //console.log(response)

            return response;
        }));
  }

  /**
   * Get Listing of Files (Workspace)
   * 
   * @param recordsRequest 
   * 
   */
  // FIXME remove (deprecated)
  records(recordsRequest: RecordsRequest, executionId: String) {
    return this.http.post<any>(`${environment.apiUrl}/api/v1/lasso/scripts/${executionId}/records`, recordsRequest)
        .pipe(map(recordsResponse => {
            // do something

            //console.log(recordsResponse)

            return recordsResponse;
        }));
  }

  /**
   * Get Listing of Files (Workspace)
   * 
   * @param filesRequest 
   * 
   */
  files(filesRequest: FileViewRequest, executionId: String) {
    return this.http.post<any>(`${environment.apiUrl}/api/v1/lasso/scripts/${executionId}/files`, filesRequest)
        .pipe(map(filesResponse => {
            // do something

            //console.log(recordsResponse)

            return filesResponse;
        }));
  }

  /**
   * Get Listing of Files (Workspace)
   * 
   * @param filesRequest 
   * 
   */
  dfsFiles(filesRequest: FileViewRequest/*, executionId: String*/) {
    return this.http.post<any>(`${environment.apiUrl}/api/v1/lasso/dfs/files`, filesRequest)
        .pipe(map(filesResponse => {
            // do something

            //console.log(recordsResponse)

            return filesResponse;
        }));
  }

  downloadDfsFile(recordsRequest: RecordsRequest/*, executionId: String*/) {
    return this.http.post(`${environment.apiUrl}/api/v1/lasso/dfs/file`, recordsRequest,
    {responseType: 'arraybuffer'})
      .subscribe(recordsResponse => {
      this.downloadFileToClient(recordsResponse, "application/octet-stream")
    });
  }

  /**
   * Download files (Workspace)
   * 
   * @param recordsRequest 
   * 
   */
  downloadWorkspaceZIP(recordsRequest: RecordsRequest, executionId: String) {
    return this.http.post(`${environment.apiUrl}/api/v1/lasso/scripts/${executionId}/records/download`, recordsRequest,
      {responseType: 'arraybuffer'}
    )
        .subscribe(recordsResponse => {
          this.downloadFileToClient(recordsResponse, "application/octet-stream")
        });
  }

  downloadWorkspaceFile(recordsRequest: RecordsRequest, executionId: String) {
    return this.http.post(`${environment.apiUrl}/api/v1/lasso/scripts/${executionId}/records/file`, recordsRequest,
    {responseType: 'arraybuffer'})
      .subscribe(recordsResponse => {
      this.downloadFileToClient(recordsResponse, "application/octet-stream")
    });
  }

  /**
   * Get file
   * 
   * @param recordsRequest 
   * 
   */
  getCSVFile(recordsRequest: RecordsRequest, executionId: String) {
    return this.http.post(`${environment.apiUrl}/api/v1/lasso/scripts/${executionId}/records/file`, recordsRequest,
      {responseType: 'text'});
  }

  /**
  * Method is use to download file.
  * @param data - Array Buffer data
  * @param type - type of the document.
  */
  downloadFileToClient(data: any, type: string) {
      let blob = new Blob([data], { type: type});
      let url = window.URL.createObjectURL(blob);
      let pwa = window.open(url);
      if (!pwa || pwa.closed || typeof pwa.closed == 'undefined') {
          alert( 'Please disable your Pop-up blocker and try again.');
      }
  }

  /**
   * Upload file
   * 
   * @param formData 
   * 
   */
  uploadFile(formData: FormData) {
    return this.http.post<any>(`${environment.apiUrl}/api/v1/lasso/file/upload`, formData)
      .pipe(map(fileResponse => {
        // do something

        console.log(fileResponse)

        return fileResponse;
    }));
  }

  downloadFile(uploadedFilePath: string) {
    return this.http.get(`${environment.apiUrl}${uploadedFilePath}`, { responseType: 'text'});
  }

  /**
   * Get available cluster nodes and their description.
   * 
   */
  clusterNodes() {
    return this.http.get<any>(`${environment.apiUrl}/api/v1/lasso/cluster/nodes`)
        .pipe(map(response => {
            // do something

            //console.log(response)

            return response;
        }));
  }

  /**
   * Get available cluster metrics and their description.
   * 
   */
  clusterMetrics() {
    return this.http.get<any>(`${environment.apiUrl}/api/v1/lasso/cluster/metrics`)
        .pipe(map(response => {
            // do something

            //console.log(response)

            return response;
        }));
  }

  /**
   * Get master log (last X lines).
   * 
   */
  masterLog() {
    return this.http.get<any>(`${environment.apiUrl}/api/v1/lasso/cluster/log`)
        .pipe(map(response => {
            // do something

            //console.log(response)

            return response;
        }));
  }

  /**
   * Get implementations
   * 
   * @param request 
   * 
   */
  implementations(request: ImplementationRequest) {
    return this.http.post<any>(`${environment.apiUrl}/api/v1/lasso/datasource/implementations`, request)
        .pipe(map(response => {
            // do something

            //console.log(response)

            return response;
        }));
  }

  /**
   * Get implementations
   * 
   * @param request 
   * @param dataSource
   * 
   */
  implementationsForDataSource(dataSource: string, request: ImplementationRequest) {
    return this.http.post<any>(`${environment.apiUrl}/api/v1/lasso/datasource/${dataSource}/implementations`, request)
        .pipe(map(response => {
            // do something

            //console.log(response)

            return response;
        }));
  }

  /**
   * Query implementations
   * 
   * @param request 
   * @param dataSource
   * 
   */
  queryImplementationsForDataSource(dataSource: string, request: SearchQueryRequest) {
    return this.http.post<any>(`${environment.apiUrl}/api/v1/lasso/datasource/${dataSource}/query`, request)
        .pipe(map(response => {
            // do something

            console.log(response)

            return response;
        }));
  }

    /**
   * Get data sources
   * 
   */
  datasources() {
    return this.http.get<any>(`${environment.apiUrl}/api/v1/lasso/datasource/info`)
        .pipe(map(response => {
            // do something

            //console.log(response)

            return response;
        }));
  }

  /**
   * Query report
   * 
   * @param lslRequest 
   * 
   */
  queryReport(request: ReportRequest, executionId: string) {
    return this.http.post<any>(`${environment.apiUrl}/api/v1/lasso/report/${executionId}`, request)
        .pipe(map(response => {
            // do something

            //console.log(lslResponse)

            return response;
        }));
  }

  /**
   * Get all report ables
   * 
   */
  reportTables(executionId: string) {
    return this.http.get<any>(`${environment.apiUrl}/api/v1/lasso/report/${executionId}`)
        .pipe(map(response => {
            // do something

            //console.log(response)

            return response;
        }));
  }

  /**
   * Get cached report
   * 
   */
  cachedReport(executionId: string, reportId: string, actionId: string, abstractionId: string, implementationId: string, dataSourceId: string, permId: string) {
    return this.http.get<any>(`${environment.apiUrl}/api/v1/lasso/report/${executionId}/${reportId}/${actionId}/${abstractionId}/${implementationId}/${dataSourceId}/${permId}`)
        .pipe(map(response => {
            // do something

            //console.log(response)

            return response;
        }));
  }

  /**
   * Get actuation sheets
   * 
   */
  actuationSheets(executionId: string, type: string = "value") {
    let params = new HttpParams().set('type', type);

    return this.http.get<any>(`${environment.apiUrl}/api/v1/lasso/srm/${executionId}`, { params: params })
        .pipe(map(response => {
            // do something

            console.log(response)

            return response;
        }));
  }

  /**
   * Get actuation sheets
   * 
   */
    actuationSheetsForSystem(executionId: string, systemId: string, type: string = "value") {
      let params = new HttpParams().set('type', type);

      return this.http.get<any>(`${environment.apiUrl}/api/v1/lasso/srm/${executionId}/${systemId}`, { params: params })
          .pipe(map(response => {
              // do something
  
              console.log(response)
  
              return response;
          }));
    }
}