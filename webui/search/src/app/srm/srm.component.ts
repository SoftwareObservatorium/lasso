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

import { AfterViewInit, Component, ViewChild } from '@angular/core';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { ActivatedRoute, Router } from '@angular/router';
import { LassoApiServiceService } from '../service/lasso-api-service.service';
import { ReportRequest } from '../model/lsl';
import { first } from 'rxjs/operators';
import { MatSelectChange } from '@angular/material/select';

@Component({
  selector: 'app-srm',
  templateUrl: './srm.component.html',
  styleUrls: ['./srm.component.css']
})
export class SrmComponent implements AfterViewInit {
  @ViewChild(MatPaginator) paginator: MatPaginator;

  /** Columns displayed in the table. Columns IDs can be added, removed, or reordered. */
  displayedColumns: string[] = [];
  rawData: any[] = [];
  data: any[] = [];

  // monaco editor for signature
  editorOptions = {theme: 'vs-light', language: 'sql'};
  code: string= '';
  sqlEditor: any;

  executionId: string;
  reportId: string;

  error: string;

  totalRows = 0;
  pageSize = 5;
  currentPage = 0;
  pageSizeOptions: number[] = [5, 10, 25, 100];

  isLoading = false;

  tables: any;

  onInit(editor: any) {
    this.sqlEditor = editor;
  }

  constructor(private route: ActivatedRoute,
    private router: Router,
    private lassoApiService: LassoApiServiceService) {
    //
  }

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      this.executionId = params.get("executionId");

      if(this.executionId) {
        this.code = `SELECT * from srm.CellValue where executionId = '${this.executionId}'`
      }
    });

    this.getTables();
  }

  getTables() {
    // execute
    this.lassoApiService.reportTables(this.executionId).pipe(first())
      .subscribe(
          data => {
              console.log(data)

              this.tables = data
          },
          error => {
              this.error = error;
          });
  }

  /**
   * Add action template to editor
   * 
   * @param event 
   * 
   */
  onChangeTable(event: MatSelectChange) {
    if(event.value === 'srm') {
      this.sqlEditor.getModel(null).setValue(`SELECT * from srm.CellValue where executionId = '${this.executionId}'`)
    } else {
      this.sqlEditor.getModel(null).setValue(`SELECT * from ${event.value}`)
    }
  }

  ngAfterViewInit(): void {
    //
  }


  /**
   * query
   */
  query() {
    let reportRequest = new ReportRequest();

    this.isLoading = true;

    // get current value
    reportRequest.sql = this.code;

    // parse table
    try {
      let tableName = reportRequest.sql.match(/(from|join)\s+(\w+)/g).map(e => e.split(' ')[1])[0]
      console.log(tableName)
      
      this.reportId = tableName
    } catch (error) {
      this.reportId = null
    }

    // save query for later
    //this.storeStatement(reportRequest.sql)

    console.log(`SQL ${reportRequest.sql} for ${this.executionId}`)

    // execute
    this.lassoApiService.queryReport(reportRequest, this.executionId).pipe(first())
      .subscribe(
          data => {
              //console.log(data)

              this.error = null

              this.displayedColumns = []
              
              // create column
              for(var key in data[0]) {
                this.displayedColumns.push(key);
              }

              this.rawData = data;

              this.setDataPage();

              this.isLoading = false;

              setTimeout(() => {
                this.paginator.pageIndex = this.currentPage;
                this.paginator.length = this.rawData.length;
              });
          },
          error => {
              this.error = error;
              console.log(error);

              this.isLoading = false;
          });
  }

  pageChanged(event: PageEvent) {
    console.log({ event });
    this.pageSize = event.pageSize;
    this.currentPage = event.pageIndex;

    this.setDataPage();
  }

  setDataPage(): void {
    const startIndex = this.paginator.pageIndex * this.paginator.pageSize;
    this.data = this.rawData.splice(startIndex, this.paginator.pageSize);
  }
}
