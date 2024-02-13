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
import { LassoApiServiceService } from '@app/_service/lasso-api-service.service';
import { NgxEditorModel } from 'ngx-monaco-editor';
import { ReportRequest } from '@app/_model/lsl';
import { first } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';

import {LocalStorage, SessionStorage, LocalStorageService} from 'ngx-webstorage';
import { Papa } from 'ngx-papaparse';
import { Templates } from 'src/codeTemplates/templates';

@Component({
  selector: 'app-database',
  templateUrl: './database.component.html',
  styleUrls: ['./database.component.css']
})
export class DatabaseComponent implements OnInit {

  @LocalStorage()
  public databaseSqlStatements: Array<String>

  error = ''
  executionId: string
  rows
  columns

  tables

  pageSize = 10
  p
  loadingIndicator: boolean = true
  reorderable: boolean = true

  reportId: string

  constructor(private route: ActivatedRoute,
    private router: Router,
    private lassoApiService: LassoApiServiceService,
    private storage: LocalStorageService,
    private papa: Papa) {
    //
  }

  ngOnInit() {
    // set default dummy value
    this.editorModel.value = this.code

    // get from router
    this.route.paramMap.subscribe(params => {
      this.executionId = params.get('executionId')

      console.log(this.executionId)

      if(this.executionId) {
        this.editorModel.value = `SELECT * from srm.CellValue where executionId = '${this.executionId}'`
      }
    });
  }

  // monaco editor
  editorOptions = {theme: Templates.themeDefault, language: 'sql', automaticLayout: true};
  code: string= `SELECT * from srm.CellValue`;

  editorModel: NgxEditorModel = {
    value: this.code,
    // set language
    language: 'sql'
  };

  editor: any;
  
  /**
   * Called when monaco editor ready
   * 
   * @param editor 
   */
  onInit(editor) {
    this.editor = editor;

    this.editor.getModel(null).onDidChangeContent((event) => {
      // do something https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.imodelcontentchangedevent.html
      //console.log(event);
    });

    // fetch tables
    this.getTables()
  }

  changeSQL(table: string) {
    this.editor.getModel(null).setValue(`SELECT * from ${table}`)
  }

  changeStatement(index) {
    this.editor.getModel(null).setValue(this.databaseSqlStatements.concat().reverse()[index])
  }

  storeStatement(statement: String) {
    // save query for later
    if(!this.databaseSqlStatements) {
      this.databaseSqlStatements = []
    }
    this.databaseSqlStatements.push(statement)

    // do not exceed 10 elements
    if(this.databaseSqlStatements.length > 10) {
      this.databaseSqlStatements = this.databaseSqlStatements.slice(Math.max(this.databaseSqlStatements.length - 10, 0))
    }

    this.databaseSqlStatements = this.databaseSqlStatements
  }

  /**
   * query
   */
  query() {
    let reportRequest = new ReportRequest();

    // get current value
    reportRequest.sql = this.editor.getModel(null).getValue()

    // parse table
    try {
      let tableName = reportRequest.sql.match(/(from|join)\s+(\w+)/g).map(e => e.split(' ')[1])[0]
      console.log(tableName)
      
      this.reportId = tableName
    } catch (error) {
      this.reportId = null
    }

    // save query for later
    this.storeStatement(reportRequest.sql)

    console.log(`SQL ${reportRequest.sql} for ${this.executionId}`)

    // execute
    this.lassoApiService.queryReport(reportRequest, this.executionId).pipe(first())
      .subscribe(
          data => {
              //console.log(data)

              this.error = null

              this.columns = []
              this.rows = []

              // https://github.com/swimlane/ngx-datatable/issues/1206
              
              // create column
              for(var key in data[0]) {
                this.columns.push({
                  name: key.replace(/[^a-zA-Z0-9]/g,'')
                })
              }

              console.log(JSON.stringify(this.columns))

              // strange, we need to lower case the names and underscore is unsupported .. who knows why
              for(let row of data) {
                let rowObj = {};
                for(let name in row) {
                  rowObj[name.replace(/[^a-zA-Z0-9]/g,'').toLowerCase()] = row[name];
                }
    
                this.rows.push(rowObj);
              }
          },
          error => {
              this.error = error;
          });
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

  setPageSize(pageSize: number) {
    this.pageSize = pageSize;
  }

  toCSV() {
    let csv = this.papa.unparse(this.rows, {
      quotes: false, //or array of booleans
      quoteChar: '"',
      escapeChar: '"',
      delimiter: ",",
      header: true,
      newline: "\r\n",
      skipEmptyLines: false, //or 'greedy',
      columns: null //or array of strings
    });

    let blob = new Blob([csv], { type: "text/csv"});
    let url = window.URL.createObjectURL(blob);
    let pwa = window.open(url);
    if (!pwa || pwa.closed || typeof pwa.closed == 'undefined') {
        alert( 'Please disable your Pop-up blocker and try again.');
    }
  }
}
