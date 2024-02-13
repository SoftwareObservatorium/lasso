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

import { KeyValue } from '@angular/common';
import { AfterViewChecked, ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DataSourceResponse, ScriptInfo, SearchQueryRequest, SearchQueryResponse } from '@app/_model/lsl';
import { HighlightService } from '@app/_service/highlight.service';
import { LassoApiServiceService } from '@app/_service/lasso-api-service.service';
import { NgxEditorModel } from 'ngx-monaco-editor';
import { NgxSmartModalService } from 'ngx-smart-modal';
import { Observable } from 'rxjs/internal/Observable';
import { first, map, tap } from 'rxjs/operators';
import { Templates } from 'src/codeTemplates/templates';

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SearchComponent implements OnInit, AfterViewChecked {

  error = ''

  loadingIndicator: boolean = true
  reorderable: boolean = true

  datasourceResponse: DataSourceResponse

  dataSource: string = "mavenCentral2023"

  actions: string[]
  action: string

  sheetRows
  sheetColumns

  sheetsPageSize = 100

  reportTables

  /**
   * view results only (no form shown)
   */
  executionId: string = null

  //
  searchResults: Observable<any>;

  p: number = 1;
  total: number;
  loading: boolean;
  //

  //
  strategies: string[] = ["concept", "exact2", "class", "class-simple", "class-bare", "class-ext"]
  strategy: string = this.strategies[2]
  compilationUnitTypes: string[] = ["class", "method"]
  compilationUnitType: string = this.compilationUnitTypes[0]

  //
  currentImpl: any

  filterQueries: string = ''

  highlighted: boolean = false;

  constructor(private route: ActivatedRoute,
    private router: Router,
    private lassoApiService: LassoApiServiceService,
    private ref: ChangeDetectorRef,
    public ngxSmartModalService: NgxSmartModalService,
    public highlightService: HighlightService) {
    //
  }

  /**
   * Highlight code when ready
   */
  ngAfterViewChecked() {
    if (!this.highlighted) {
      this.highlightService.highlightAll();
      this.highlighted = true;
    }
  }

  ngOnDestroy() {
    this.action = null
  }

  ngOnInit() {
    // set default dummy value
    this.editorModel.value = this.code

    // load script content from router
    this.route.paramMap.subscribe((params) => {
      let executionId = params.get("executionId");

      // set LSL script from localStorage
      if (executionId) {
        let scriptInfo: ScriptInfo = JSON.parse(
          localStorage.getItem(`currentScript_${executionId}`)
        );

        this.editorModel.value = scriptInfo.content;

        // trigger query
        this.getPageForScript(1, executionId)

        this.executionId = executionId

        //
        this.querySheets()
        this.getReportTables()
      } else {
        // set default dummy value
        this.editorModel.value = this.code;
      }
    });

    // fetch datasources
    this.getDataSources()
  }

  // monaco editor
  editorOptions = {theme: Templates.themeDefault, language: 'lsl', automaticLayout: true};
  code: string= Templates.lql;

  editorModel: NgxEditorModel = {
    value: this.code,
    // set language
    language: 'lsl'
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
  }

  /**
   * get available data sources
   */
  getDataSources() {
    // execute
    this.lassoApiService
      .datasources()
      .pipe(first())
      .subscribe(
        (data) => {
          this.datasourceResponse = data;

          let types = Array.from(Object.keys(this.datasourceResponse.dataSources));

          //this.editorService.init(types);
          console.log(JSON.stringify(this.datasourceResponse))

          // manually check for changes
          this.ref.markForCheck()
        },
        (error) => {
          this.error = error;
        }
      );
  }

  /**
   * Order by score descending
   * 
   * @param akv 
   * @param bkv 
   * @returns 
   */
  orderBy = (akv: KeyValue<string, any>, bkv: KeyValue<string, any>): number => {
    const a = akv.value.score;
    const b = bkv.value.score;

    return a > b ? -1 : (b > a ? 1 : 0);
  };

  setAction(myaction) {
    this.action = myaction

    // trigger query
    this.getPageForScript(1, this.executionId)
  }

  /**
   * query
   */
  getPageForScript(page: number, executionId: string) {
    console.log("triggering query for executionId")

    const perPage = 10;

    this.loading = true;

    let request = new SearchQueryRequest();
    request.forAction = this.action

    // TODO socora strategy
    //request.sortyBy = ["score desc"]

    // get current value
    //request.query = this.editor.getModel(null).getValue()
    request.start = (page - 1) * perPage;
    request.rows = 10

    // set executionId
    request.executionId = executionId;

    // execute
    this.searchResults = this.lassoApiService.queryImplementationsForDataSource(this.dataSource, request).pipe(
      tap(res => {
          this.total = res.total;
          this.p = page;
          this.loading = false;
          this.actions = res.actions
          if(!this.action) {
            this.action = this.actions[this.actions.length - 1] // set to last action
          }
      }),
      map(res => res.implementations)
    );
  }

  /**
   * query
   */
  getPage(page: number) {
    console.log(page)

    if (this.executionId) {
      return this.getPageForScript(page, this.executionId)
    }

    // split filter queries
    let filters = [];
    if(this.filterQueries) {
      try {
        filters = this.filterQueries.split(/\r?\n/)
      } catch (error) {

      }
    }

    const perPage = 10;

    this.loading = true;

    let request = new SearchQueryRequest();

    request.sortyBy = ["score desc"]

    // get current value
    request.query = this.editor.getModel(null).getValue()
    request.start = (page - 1) * perPage;
    request.rows = 10
    request.strategy = this.strategy // default

    //request.filters = ['doctype_s:class', 'type:c']
    request.filters = ['type:c']
    request.filters.push(`doctype_s:${this.compilationUnitType}`)
    filters.forEach(x => request.filters.push(x))

    // execute
    this.searchResults = this.lassoApiService.queryImplementationsForDataSource(this.dataSource, request).pipe(
      tap(res => {
          this.total = res.total;
          this.p = page;
          this.loading = false;
      }),
      map(res => res.implementations)
    );
  }

    /**
   * Add new data source to editor
   *
   * @param dsInfo
   */
  setDataSource(dsInfo) {
    this.dataSource = dsInfo.id
  }

  openSheets() {
    this.querySheets()
    this.ngxSmartModalService.getModal('sheetModal').open()
  }

  doHighlight(event) {
    console.log("do highlight")
    console.log(event)

    this.highlightService.highlightAll()
  }

    /**
   * query
   */
    querySheets() {
      // execute
      this.lassoApiService.actuationSheets(this.executionId).pipe(first())
        .subscribe(
            data => {
                //console.log(data)
  
                this.error = null
  
                this.sheetColumns = []
                this.sheetRows = []
  
                // https://github.com/swimlane/ngx-datatable/issues/1206
                
                // create column
                for(var key in data[0]) {
                  this.sheetColumns.push({
                    name: key//.replace(/[^a-zA-Z0-9]/g,'')
                  })
                }
  
                console.log(JSON.stringify(this.sheetColumns))
  
                // strange, we need to lower case the names and underscore is unsupported .. who knows why
                for(let row of data) {
                  let rowObj = {};
                  for(let name in row) {
                    rowObj[name/*.replace(/[^a-zA-Z0-9]/g,'').toLowerCase()*/] = row[name];
                  }
      
                  this.sheetRows.push(rowObj);
                }

                console.log(JSON.stringify(this.sheetRows))
            },
            error => {
                this.error = error;
            });
    }

    getActuationSheetsFor(candidate) {
      let result = this.sheetColumns.filter(col => col.name.startsWith(candidate) || col.name === 'STATEMENT')

      return result
    }

    getReportTables() {
      // execute
      this.lassoApiService.reportTables(this.executionId).pipe(first())
        .subscribe(
            data => {
                console.log(data)
  
                this.reportTables = data
            },
            error => {
                this.error = error;
            });
    }
}
