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

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { LassoApiServiceService } from '@app/_service/lasso-api-service.service';
import { RecordsRequest } from '@app/_model/lsl';

import { Papa } from 'ngx-papaparse';

import { first } from 'rxjs/operators';

interface LooseObject {
  [key: string]: any
}

@Component({
  selector: 'app-table',
  templateUrl: './table.component.html',
  styleUrls: ['./table.component.css']
})
export class TableComponent implements OnInit {

  executionId: string;
  csvPath: string;

  error;

  // CSV header
  dataHeader: any[];
  dataRows: any[];

  //
  columns: any[];
  rows: any[];

  pageSize = 10;
  p;
  loadingIndicator: boolean = true;
  reorderable: boolean = true;

  reportId: string

  constructor(private route: ActivatedRoute,
    private lassoApiService: LassoApiServiceService,
    private papa: Papa) {
      //
  }

  ngOnInit() {
    // get from router
    this.route.paramMap.subscribe(params => {
      this.executionId = params.get('executionId')
      this.csvPath = params.get('csvPath')
      // fetch

      this.reportId = this.csvPath.split('_')[0]

      console.log(`found ${this.executionId} csv path '${this.csvPath}'`)

      this.getCSVFile()
    });
  }

  /**
   * get csv
   */
  getCSVFile() {
    let recordsRequest = new RecordsRequest()
    recordsRequest.filePatterns = [this.csvPath]

    // execute
    this.lassoApiService.getCSVFile(recordsRequest, this.executionId).pipe(first())
      .subscribe(
                  data => {
                    //console.log(data);
        
                    //
                    this.parseCSV(data);
                  },
                  error => {
                    this.error = error;
          });
  }

  /**
   * Parse CSV and convert to JSON
   */
  parseCSV(csvData: string) {
    //console.log(csvData)
    this.papa.parse(csvData,{
      skipEmptyLines: true,
      complete: (result) => {
          //console.log('Parsed: ', result);

          this.dataHeader = this.getHeader(result);
          this.dataRows = this.getRows(result);

          this.rows = [];
          this.columns = [];

          for(let col of this.dataHeader) {
            this.columns.push({
              name: col.replace(/[^a-zA-Z0-9]/g,'')
            });
          }

          console.log(this.columns)

          for(let row of this.dataRows) {
            let rowObj = {};
            for(let i = 0; i < this.dataHeader.length; i++) {
              rowObj[this.dataHeader[i].replace(/[^a-zA-Z0-9]/g,'').toLowerCase()] = row[i];
            }

            this.rows.push(rowObj);
          }

          //console.log(this.rows)

          //console.log(result)
      }
    });
  }

  getHeader(results) {
    return results.data[0].map(col => {
      // workaround for https://github.com/swimlane/ngx-datatable/issues/1031
      return col.replace('_', '')
    });
  }

  getRows(results) {
    return results.data.slice(1);
  }

  setPageSize(pageSize: number) {
    this.pageSize = pageSize;
  }
}
