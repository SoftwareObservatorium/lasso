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

import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { MatTabChangeEvent } from '@angular/material/tabs';
import { HotTableRegisterer } from '@handsontable/angular';
import Handsontable from 'handsontable';
import { read, utils, writeFileXLSX } from 'xlsx';

import {MatAccordion} from '@angular/material/expansion';
import { LassoApiServiceService } from '../service/lasso-api-service.service';
import { ActivatedRoute, Router } from '@angular/router';
import { DataSourceResponse, LSLInfoResponse, LslRequest } from '../model/lsl';
import { AuthenticationService } from '../service/authentication.service';
import { User } from '../model/user';
import { first } from 'rxjs/operators';
import { MatSelectChange } from '@angular/material/select';

@Component({
  selector: 'app-code',
  templateUrl: './code.component.html',
  styleUrls: ['./code.component.css']
})
export class CodeComponent implements OnInit, AfterViewInit {

  @ViewChild(MatAccordion) accordion: MatAccordion;

  // monaco editor for signature
  editorOptions = {theme: 'vs-light', language: 'java'};
  code: string= 'Base64 {\n  encode(byte[])->byte[]\n}';
  sigEditor: any;

    // monaco for LSL
    lslEditorOptions = {theme: 'vs-light', language: 'lsl'};
    lslCodeTemplate: string= `dataSource '{{corpus_datasource}}'
def totalRows = {{select_rows}}
def noOfAdapters = {{arena_adapters}}
// interface in LQL notation
def interfaceSpec = """{{select_lql}}"""
def abstractionName = '{{abstraction_name}}'
study(name: 'CodeSearch-TDCS-{{abstraction_name}}') {
    /* select class candidates using interface-driven code search */
    action(name: 'select', type: 'Select') {
        abstraction(abstractionName) {
            queryForClasses interfaceSpec, '{{select_strategy}}'
            rows = totalRows
            excludeClassesByKeywords(['private', 'abstract'])
            excludeTestClasses()
            excludeInternalPkgs()
        }
    }
    /* filter candidates by two tests (test-driven code filtering) */
    action(name: 'filter', type: 'ArenaExecute') { // filter by tests
        containerTimeout = 10 * 60 * 1000L // 10 minutes
        specification = interfaceSpec
        sequences = [
                {{arena_sequences}}
        ]
        maxAdaptations = noOfAdapters // how many adaptations to try

        dependsOn 'select'
        includeAbstractions '*'
        profile('myTdsProfile') {
            scope('class') { type = 'class' }
            environment('java11') {
                image = 'maven:3.6.3-openjdk-17' // Java 17
            }
        }

        // match implementations (note no candidates are dropped)
        whenAbstractionsReady() {
            def myAb = abstractions[abstractionName]
            // define oracle based on expected responses in sequences
            def expectedBehaviour = toOracle(srm(abstraction: myAb).sequences)
            // returns a filtered SRM
            def matchesSrm = srm(abstraction: myAb)
                    .systems // select all systems
                    .equalTo(expectedBehaviour) // functionally equivalent
        }
    }
    /* rank candidates based on functional correctness */
    action(name:'rank', type:'Rank') {
        // sort by functional similarity (passing tests/total tests) descending
        criteria = ['FunctionalSimilarityReport.score:MAX:1'] // more criteria possible

        dependsOn 'filter'
        includeAbstractions '*'
    }
}`

  currentEditorTab: number = 0

  onInit(editor: any) {
    this.sigEditor = editor;
  }

  currentUser: User;
  error: string;

  constructor(
    private lassoApiService: LassoApiServiceService,
    private authenticationService: AuthenticationService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.authenticationService.currentUser.subscribe(
      (x) => (this.currentUser = x)
    );
  }

  lslEditor: any;

  onLSLInit(editor: any) {
    this.lslEditor = editor;
    console.log("lsl editor initialized")
  }

  private hotRegisterer = new HotTableRegisterer();

  sheets: string[] = ['Sheet 1'];
  currentSheetTab: MatTabChangeEvent = null;
  selectedSheetIndex: any = 0;

  filters: string[] = [];
  filter: string = '';
  rows: number = 10;

  datasource: string | undefined;

  strategies: string[] = ['class-simple'];
  strategy: string = this.strategies[0];

  // arena
  arenaAdapters: number = 100

  createHotSettings(): Handsontable.GridSettings {
    let hotSettings: Handsontable.GridSettings = {
      width: '100%',
      height: 'auto',
      colHeaders: true,
      rowHeaders: true,
      stretchH: 'all', // 'none' is default
      contextMenu: true,
      manualColumnResize: true,
      licenseKey: 'non-commercial-and-evaluation'
    }

    return hotSettings;
  }

  getHot(): Handsontable {
    return this.hotRegisterer.getInstance(`hot_${this.selectedSheetIndex}`)
  }

  getHotBySheetIndex(sheetIndex: number): Handsontable {
    return this.hotRegisterer.getInstance(`hot_${sheetIndex}`)
  }

  infoResponse: LSLInfoResponse;

  datasourceResponse: DataSourceResponse;

  ngOnInit(): void {
    // query params
    this.route.queryParamMap.subscribe((paramMap) => {
      let lql = paramMap.get('lql')

      console.log(lql)

      console.log("setting to LQL editor")

      if(lql) {
        this.code = lql
      }
    });

    // available ds
    this.getDataSources();
  }

  ngAfterViewInit(): void {
    //
  }

  sheetChanged(tabChangeEvent: MatTabChangeEvent): void {
    console.log("tab changed")
    console.log(tabChangeEvent.index)
    console.log(this.selectedSheetIndex)

    this.currentSheetTab = tabChangeEvent;
  }

  onAddSheet(): void {
    this.sheets.push(`Sheet ${this.sheets.length + 1}`);
    this.selectedSheetIndex += 1;
  }

  onAddRow(): void {
    this.getHot().alter('insert_row_below', this.getHot().countRows(), 1)
  }

  onAddCol(): void {
    this.getHot().alter('insert_col_end', this.getHot().countCols(), 1)
  }

  readSheet(fileChangeEvent: Event) {(async() => {
      const file = (fileChangeEvent.target as HTMLInputElement).files[0];
      let fileReader = new FileReader();
      fileReader.onload = (e) => {
        //console.log(fileReader.result);
      }
      const ab = await file.arrayBuffer();
      /* parse workbook */
      const wb = read(ab);

      // array of arrays with header:1
      let arrArr = utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], {header: 1})

      // dimensions are wrong for some reasons, set all rows to same length
      // determine max cols (based on row with most columns)
      let maxCols = Math.max(...arrArr.map((arr: any) => arr.length))

      let dimData = []
      for (let [idx, arr] of arrArr.entries()) { 
        let myArr: any = arr
        if(myArr.length < maxCols) {
          console.log("need modify")
          let nArr = new Array(maxCols)
          for(let [cdx, carr] of myArr.entries()) {
            nArr[cdx] = carr
          }

          dimData.push(nArr)
        } else {
          dimData.push(arr)
        }
      }

      //this.getHot().updateData(arrArr)
      this.getHot().loadData(dimData)
    })();
  }

  onExport(): void {
    let ws = utils.json_to_sheet(this.getHot().getData(), {skipHeader: true});
    let wb = utils.book_new();
    utils.book_append_sheet(wb, ws, `Sheet ${this.selectedSheetIndex + 1}`);
    writeFileXLSX(wb, `Sheet_${this.selectedSheetIndex + 1}.xlsx`);
  }

  addFilter(): void {
    this.filters.push(this.filter)
    console.log(this.filters)
  }

  /**
   * Identify if sheets have been added.
   * 
   * @returns 
   */
  isTds(): boolean {
    for (let i = 0; i < this.sheets.length ; i++) {
      console.log(this.getHotBySheetIndex(i).countCols())
      console.log(this.getHotBySheetIndex(i).countEmptyCols())

      if(this.getHotBySheetIndex(i).countCols() != this.getHotBySheetIndex(i).countEmptyCols()) {
        return true
      }
    }

    return false
  }

  onSearch(draft: boolean): void {
    // visual editor

    let lqlCode = this.code
    let searchFilters = []
    searchFilters = ['type:c']
    searchFilters.push(`doctype_s:class`)
    this.filters.forEach(x => searchFilters.push(x))

    let tdcs = this.isTds()
    if(tdcs) {
      console.log("TDCS")
      // generate sequences
      let sequences = this.onGenerateSequences()
      // get abstraction name
      let abstractionName = lqlCode.substring(0, lqlCode.indexOf("{")).trim()

      let templateMap = {
        "corpus_datasource": `${this.datasource}`,
        "abstraction_name": `${abstractionName}`,
        "select_rows" : `${this.rows}`,
        "select_lql" : `${this.code}`,
        "select_strategy" : `${this.strategy}`,
        "arena_adapters": `${this.arenaAdapters}`,
        "arena_sequences" : `${sequences}`
      }

      let lslCode = this.substituteStr(this.lslCodeTemplate, templateMap)
      console.log(lslCode)

      let lslRequest = new LslRequest();
      // get current value
      lslRequest.script = lslCode;

      lslRequest.email = this.currentUser.email;
      lslRequest.type = draft ? "DRAFT" : null; // or draft

      console.log(lslRequest);

      // submit script
      this.lassoApiService
        .execute(lslRequest)
        .pipe(first())
        .subscribe({
          error: (e) => {this.error = e;
            console.log(this.error);},
          complete: () => {this.router.navigate(["/scripts"])} 
      });
    } else {
      console.log("IDCS")
      // interface-driven code search (textual search)
      // if no sequence sheets, directly trigger textual search
      this.router.navigate(
        ['/search'],
        {
          queryParams: { lql: lqlCode, filter: searchFilters, strategy: this.strategy, datasource: this.datasource },
          queryParamsHandling: 'merge' }
        );
    }
  }

  /**
   * Substitute LSL template string
   * 
   * @param templateString 
   * @param values 
   * @returns 
   */
  substituteStr(templateString: string, values: any): string {
    return templateString.replace(/{{(.*?)}}/g, function(match, number) { 
      return typeof values[number] != 'undefined'
        ? values[number] 
        : match
      ;
    });
  }

  /**
   * Determine non-null length 
   * 
   * @param arr 
   * @returns 
   */
  findMaxLength(row: any): number {
    let length = 0
    for (let [i, elem] of row.entries()) { 
      if(elem != null) {
        length = i
      }
    }

    return length + 1
  }

  /**
   * Check if row is empty (no cells defined, all null)
   * 
   * @param arr 
   * @returns 
   */
  rowEmpty(arr: any): boolean {
    for (let [i, elem] of arr.entries()) { 
      if(!(elem == null)) {
        return false
      }
    }

    return true
  }

  /**
   * Generate LSL-based sequence sheet blocks from spreadsheets
   * 
   * @returns 
   */
  onGenerateSequences(): string {
    // sequence sheets
    let mylsl = '';

    // N sheets
    for (let i = 0; i < this.sheets.length ; i++) {
      // start sheet block
      mylsl += `'sheet${i + 1}': sheet() {\n`;

      let sheetData = this.getHotBySheetIndex(i).getData()
      //console.log(sheetData)

      // determine max cols (based on row with most columns)
      let maxCols = Math.max(...sheetData.map((arr: any) => this.findMaxLength(arr)))

      for (let [rdx, row] of sheetData.entries()) { 
        if(this.rowEmpty(row)) {
          continue
        }

        let maxLength = this.findMaxLength(row)

        let rowStr = "";
        let myRow: any = row

        for(let [cdx, cell] of myRow.entries()) {
          if(cdx < maxLength) {
            if(cdx < maxCols) {
              if(cdx > 0) {
                rowStr += ", ";
              }
  
              if(cell) {
                rowStr += `${cell}`;
              } else {
                rowStr += `''`;
              }
            }
          }
        }

        if(rowStr.length > 0) {
          mylsl += "row " + rowStr + "\n";
        }
      }

      // end block
      mylsl += `}`;

      if(i + 1 < this.sheets.length) {
        mylsl += ",";
      }

      mylsl += "\n";
    }

    console.log(mylsl);

    return mylsl
  }

  /**
   * 
   * 
   * @param event 
   * 
   */
  onSetDatasource(event: MatSelectChange) {
    console.log(event.value)

    this.datasource = event.value.id
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
          console.log(types)

          this.datasource = types.find(element => {
            return element != "dummy"
          })
        },
        (error) => {
          this.error = error;
        }
      );
  }
}