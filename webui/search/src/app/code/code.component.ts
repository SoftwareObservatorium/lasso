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
  code: string= 'Base64 {\n  encode(byte[])->java.lang.String\n}';
  sigEditor: any;

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
  rows: number = 100;

  datasource: string | undefined;

  strategies: string[] = ['class-simple'];
  strategy: string = this.strategies[0];

  createHotSettings(): Handsontable.GridSettings {
    let hotSettings: Handsontable.GridSettings = {
      width: '100%',
      height: 'auto',
      colHeaders: true,
      rowHeaders: true,
      stretchH: 'all', // 'none' is default
      contextMenu: true,
      licenseKey: 'non-commercial-and-evaluation'
    }

    return hotSettings;
  }

  getHot(): Handsontable {
    return this.hotRegisterer.getInstance(`hot_${this.selectedSheetIndex}`)
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
      console.log(utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], {header: 1}));
      //this.getHot().loadData())
      this.getHot().updateData(utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], {header: 1}))
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

  onSearch(): void {
    let lslRequest = new LslRequest();

    // visual editor

    let lqlCode = this.code
    let searchFilters = []
    searchFilters = ['type:c']
    searchFilters.push(`doctype_s:class`)
    this.filters.forEach(x => searchFilters.push(x))

    // FIXME if no sequence sheets, directly trigger textual search
    this.router.navigate(
      ['/search'],
      {
        queryParams: { lql: lqlCode, filter: searchFilters, strategy: this.strategy, datasource: this.datasource },
        queryParamsHandling: 'merge' }
      );
  }

  onGenerateSequences(): void {
    console.log(this.getHot().getData());

    // TODO for each
    let mylsl = '';
    this.getHot().getData().forEach(row => {
      mylsl += "row ";
      let first = true;
      row.forEach((col: any) => {
        if(col != null) {
          if(!first) {
            mylsl += ", ";
          } else {
            first = false;
          }
          mylsl += `'${col}'`;
        }
      })

      mylsl += "\n";
    })

    console.log(mylsl);

    //this.lslCode = mylsl + "\n\n" + this.lslCode;
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