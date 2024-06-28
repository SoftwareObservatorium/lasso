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

import { AfterViewChecked, ChangeDetectorRef, Component, ElementRef, ViewChild } from '@angular/core';
import { first } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';
import { LassoApiServiceService } from '../service/lasso-api-service.service';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { Observable } from 'rxjs';
import { MatTableDataSource } from '@angular/material/table';
import { ChatRequest, ChatResponse, SearchQueryRequest } from '../model/lsl';
import { HighlightService } from '../service/highlight.service';
import { MatTabChangeEvent } from '@angular/material/tabs';
import { OracleDescription, TextualSearch } from '../model/data';
import { Apollo } from 'apollo-angular';
import { LassoGraphService, ObservationQuery } from '../service/graphql.service';

import * as XLSX from 'xlsx';
import { MatChipSelectionChange } from '@angular/material/chips';
import { LassoUtils } from '../utils/lutils';

const EXCEL_EXTENSION = '.xlsx';

@Component({
  selector: 'app-results',
  templateUrl: './results.component.html',
  styleUrls: ['./results.component.css']
})
export class ResultsComponent implements AfterViewChecked {
  @ViewChild(MatPaginator) paginator: MatPaginator;

  obs: Observable<any>;
  dataSource: MatTableDataSource<any> = new MatTableDataSource();

  executionId: string;

  error: string = '';

  isLoading = false;
  totalRows = 0;
  pageSize = 10;
  currentPage = 0;
  pageSizeOptions: number[] = [5, 10, 25, 100];

  lassoDataSource: string | null = 'mavenCentral2023'

  /** Columns displayed in the table. Columns IDs can be added, removed, or reordered. */
  displayedColumns = ['id'];

  actuationSheetFragments = new Map<string, any[]>();

  highlighted: boolean = false;

  // is textual search or script?
  textualSearch: TextualSearch;

  recordTypes: string[];
  observationRecords = new Map<string, any[]>();

  oracle: OracleDescription;

  // selected outputs for oracle filtering
  oracleFilters: Map<string, string>

  chatResponse: ChatResponse | null

  userQuestion: string
  userModel: string = "llama3:latest"
  userModelTemperature: number = 0.7

  constructor(private route: ActivatedRoute,
    private router: Router,
    private changeDetectorRef: ChangeDetectorRef,
    private lassoApiService: LassoApiServiceService,
    public highlightService: HighlightService,
    private apollo: Apollo,
    private lassoGraphService: LassoGraphService) {
    //
  }

  ngOnInit() {
    console.log(this.route.url);

    // is script
    this.route.paramMap.subscribe((params) => {
      this.executionId = params.get("executionId");

      //this.dataSource = new ResultsDataSource(this.lassoApiService, this.executionId);

      this.changeDetectorRef.detectChanges();
      this.dataSource.paginator = this.paginator;
      
      this.obs = this.dataSource.connect();

      console.log("script results")
    });

    // is textual search
    this.route.queryParamMap.subscribe((paramMap) => {
      let lqlCode = paramMap.get('lql')
      let filters = paramMap.getAll('filter')
      let strategy = paramMap.get('strategy')
      let datasource = paramMap.get('datasource')
      this.lassoDataSource = datasource

      if(lqlCode) {
        // do a textual search
        let textualSearchQuery = new TextualSearch()
        textualSearchQuery.lql = lqlCode
        textualSearchQuery.filters = filters
        textualSearchQuery.strategy = strategy
        

        this.textualSearch = textualSearchQuery

        console.log("textual results")
      }
    });

    this.loadData();
  }

  getRecordTypes(implId: string) {
    let oq = new ObservationQuery()
    oq.executionId = this.executionId
    oq.systemId = implId

    this.apollo.watchQuery({
      query: this.lassoGraphService.getRecordTypes,
      variables: {
        q: oq
    }
    }).valueChanges.subscribe(({data,error}:any)=>{
      //console.log("graphql getRecordTypes results " + JSON.stringify(data.getRecordTypes))

      this.recordTypes = data.getRecordTypes

      this.error=error;
      console.log(this.error)
    })
  }

  getRecords(implId: string, recordType: string) {
    if(this.observationRecords.has(this.getActuationKey(implId, recordType))) {
      return
    }

    let oq = new ObservationQuery()
    oq.executionId = this.executionId
    oq.systemId = implId
    oq.type = recordType

    console.log(JSON.stringify(oq))

    this.apollo.watchQuery({
      query: this.lassoGraphService.observationRecords,
      variables: {
        q: oq
    }

    }).valueChanges.subscribe(({data,error}:any)=>{
      //console.log("graphql observationRecords results " + JSON.stringify(data.observationRecords))

      this.highlighted = false

      this.observationRecords.set(this.getActuationKey(implId, recordType), data.observationRecords);

      this.error=error;
      console.log(this.error)
    })
  }

  getActuationSheetFragment(implId: string, type: string) {
    this.lassoApiService.actuationSheetsForSystem(this.executionId, implId, type).pipe(first())
    .subscribe(
        data => {
            this.actuationSheetFragments.set(this.getActuationKey(implId, type), data)
        },
        error => {
            this.error = error;
        });
  }

  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
  }

  /**
   * Highlight code when ready
   */
  ngAfterViewChecked() {
    if (!this.highlighted) {
      this.highlightService.highlightAll();
      this.highlighted = true;

      console.log("highlighting enabled")
    }
  }

  onTabChange(event: MatTabChangeEvent, implId: string) {
    console.log("tab with impl id " + implId)

    //if (!this.highlighted) {
      this.highlightService.highlightAll();
      this.highlighted = true;

      console.log("highlighting enabled")
    //}

    if(event.tab.textLabel === "Observations") {
      this.getRecordTypes(implId)
    }

    if(event.tab.textLabel === "Responses") {
      if(!this.actuationSheetFragments.has(this.getActuationKey(implId, "value"))) {
        this.getActuationSheetFragment(implId, "value")
      }
    }

    if(event.tab.textLabel === "Operations") {
      if(!this.actuationSheetFragments.has(this.getActuationKey(implId, "op"))) {
        this.getActuationSheetFragment(implId, "op")
      }
    }

    if(event.tab.textLabel === "Stimuli") {
      if(!this.actuationSheetFragments.has(this.getActuationKey(implId, "input_value"))) {
        this.getActuationSheetFragment(implId, "input_value")
      }
    }
  }

  getActuationKey(implId: string, type: string) {
    return implId + "_" + type
  }

  loadData() {
    this.isLoading = true;

    const startIndex = this.paginator.pageIndex * this.paginator.pageSize;
    //return data.splice(startIndex, this.paginator.pageSize);
    console.log("triggering query for executionId")

    //const perPage = 10;

    //this.loading = true;

    let request = new SearchQueryRequest();
    //request.forAction = this.action

    // TODO socora strategy
    //request.sortyBy = ["score desc"]

    // get current value
    //request.query = this.editor.getModel(null).getValue()

    if(this.textualSearch) {
      request.query = this.textualSearch.lql
      request.filters = this.textualSearch.filters
      request.strategy = this.textualSearch.strategy
    } else {
      // set executionId
      request.executionId = this.executionId;

      if(this.oracleFilters) {
        console.log(this.oracleFilters)
        const convMap: any = {};
        this.oracleFilters.forEach((val: string, key: string) => {
          convMap[key] = val;
        });
        request.oracleFilters = convMap
      }
    }

    request.start = startIndex;
    request.rows = this.paginator.pageSize

    console.log(JSON.stringify(request));

    // execute
    let searchResults = this.lassoApiService.queryImplementationsForDataSource(this.lassoDataSource, request)
    .pipe(first())
    .subscribe({
      next: (res) => { this.dataSource.data = this.sortImplementationsToArray(res.implementations); this.isLoading = false;
        setTimeout(() => {
          this.paginator.pageIndex = this.currentPage;
          this.paginator.length = res.total;

          //if (!this.highlighted) {
            this.highlightService.highlightAll();
            this.highlighted = true;
      
            console.log("highlighting enabled")
          //}
        });},
      error: (e) => {this.error = e;
        console.log(this.error);
        this.isLoading = false;},
      complete: () => {} 
    });

    //console.log(JSON.stringify(searchResults.subscribe(res => console.log(res.json()))))
  }

  askQuestion() {
    this.isLoading = true;

    let chatRequest = new ChatRequest();
    chatRequest.modelName = this.userModel
    chatRequest.temperature = this.userModelTemperature
    //chatRequest.message = "You are a software engineer. Given is a set of interface signature descriptions similar to Python's notation, starting wit the interface name, a set of methods delimited by newline, including their name, input parameter types as well as output parameter types. Identify if all interface specifications match the same functionality."
    chatRequest.message = this.userQuestion

    let request = new SearchQueryRequest();

    if(this.textualSearch) {
      request.query = this.textualSearch.lql
      request.filters = this.textualSearch.filters
      request.strategy = this.textualSearch.strategy
    } else {
      // set executionId
      request.executionId = this.executionId;

      if(this.oracleFilters) {
        console.log(this.oracleFilters)
        const convMap: any = {};
        this.oracleFilters.forEach((val: string, key: string) => {
          convMap[key] = val;
        });
        request.oracleFilters = convMap
      }
    }

    request.start = 1;
    request.rows = 25 // FIXME limit to token size

    console.log(JSON.stringify(request));

    chatRequest.searchQueryRequest = request;

    // execute
    this.lassoApiService.askImplementationsForDataSource(this.lassoDataSource, chatRequest)
    .pipe(first())
    .subscribe({
      next: (res) => {
        console.log(res);
        this.chatResponse = res;
        this.isLoading = false;

       },
      error: (e) => {this.error = e;
        console.log(this.error);
        this.isLoading = false;},
      complete: () => {} 
    });
  }

  pageChanged(event: PageEvent) {
    console.log({ event });
    this.pageSize = event.pageSize;
    this.currentPage = event.pageIndex;
    this.loadData();
  }

  ngOnDestroy() {
    if (this.dataSource) { 
      this.dataSource.disconnect(); 
    }
  }

  sortImplementationsToArray = (implementations: any): any[] => {
    const jsonArray = Array.from(Object.values(implementations));

    // sort the array by the "score" attribute
    jsonArray.sort((a: any, b: any) => a.score >b.score ? -1 : (b.score > a.score ? 1 : 0));

    for (let [i, elem] of jsonArray.entries()) { 
      let impl: any = elem
    }

    return jsonArray;
  };
  
  getColumnName(col: string) {
    if(col.startsWith("oracle") && col.includes("_")) {
      return "Oracle"
    }

    if(col.includes("_")) {
      return col.substring(col.indexOf("_") + 1, col.length)
    }

    return col
  }

  getColumns(implId: string, type: string) {
    if(!this.actuationSheetFragments.get(this.getActuationKey(implId, type))) {
      return [];
    }

    let sheetColumns = []
    for(let col in this.actuationSheetFragments.get(this.getActuationKey(implId, type))[0]) {
      if(col.startsWith(implId)) {
        sheetColumns.push(col)
      }

      if(col.startsWith("oracle")) {
        sheetColumns.push(col)
      }
    }

    // sort by adapter asc
    sheetColumns.sort()
    sheetColumns.unshift('STATEMENT')

    return sheetColumns;

  }

  filterByOracle(impl: any, adapterId: string) {
    this.oracle = new OracleDescription()
    this.oracle.referenceImpl = adapterId // is already full id with impl id

    let response = this.actuationSheetFragments.get(this.getActuationKey(impl.id, 'value'))
    this.oracle.manual = response
    this.oracleFilters = new Map<string, string>();

    // this.dataSource.filterPredicate = (record,filter) => {
    //   console.log(record.id)

    //   return true;
    // }

    // this.dataSource.filter = "blub"

    //this.dataSource.data = this.dataSource.data.splice(10)

    // scroll to top
    let el = document.getElementById("options");
    el.scrollIntoView();
  }

  formatOracleRow(r: any) {
    return "(" + r['STATEMENT'] + ") " + r[this.oracle.referenceImpl]
    //return JSON.stringify(r)
  }

  onFilterByOracleValues() {
    console.log(this.oracleFilters)

    // load new (filtered) results
    this.loadData()
  }

  getImplementationLabel(impl: string): string {
    return LassoUtils.getColumnLabel(impl)
  }

  onOutputSelectionChange($event: MatChipSelectionChange, o: any, i: number) {
    if($event.selected) {
      this.oracleFilters.set(o['STATEMENT'], o[this.oracle.referenceImpl])
    } else {
      this.oracleFilters.delete(o['STATEMENT'])
    }
  }

  downloadContent(content: string, name: string) {
    let myBlob = new Blob([content])
    const blobUrl = URL.createObjectURL(myBlob);
  
    const link = document.createElement("a");
  
    link.href = blobUrl;
    link.download = name + ".java";
  
    document.body.appendChild(link);

    link.dispatchEvent(
      new MouseEvent('click', { 
        bubbles: true, 
        cancelable: true, 
        view: window 
      })
    );
  
    document.body.removeChild(link);
  }

  findSimilar(impl: any) {
    this.router.navigate(
      ['/search'],
      {
        queryParams: { lql: impl.lql, /*filter: searchFilters,*/ strategy: 'class-simple', datasource: impl.dataSource },
        queryParamsHandling: 'merge' }
      ).then(() => {
        window.location.reload();
      });
  }

  // Open artifact in Nexus
  openArtifact(impl: any) {
    //http://lassohp12.informatik.uni-mannheim.de:8081/#browse/browse:maven-public:net.swisstech%2Fswissarmyknife%2F2.2.1
    //http://lassohp12.informatik.uni-mannheim.de:8081/#browse/browse:maven-public:net%2Fswisstech%2Fswissarmyknife%2F2.2.1

    let g: string = impl.groupId
    g = g.replaceAll(".", "/")

    let artifact = encodeURIComponent(`${g}/${impl.artifactId}/${impl.version}`)

    window.open(`http://lassohp12.informatik.uni-mannheim.de:8081/#browse/browse:maven-public:${artifact}`);
  }

  /**
   * Download table
   * 
   * @param impl 
   * @param tableId 
   */
  downloadTable(impl: any, tableId: string) {
    let elem = document.getElementById(tableId)

    const ws: XLSX.WorkSheet = XLSX.utils.table_to_sheet(elem);
    // generate workbook and add the worksheet
    const workbook: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, ws, 'Responses');
    // save to file
    XLSX.writeFile(workbook, `${tableId}${EXCEL_EXTENSION}`);
  }

  /**
 * View script in editor
 * 
 */
  onOpenScript() {
    this.router.navigate(['/submit', this.executionId]);
  }

  onDatabase() {
    this.router.navigate(['/db', this.executionId]);
  }

  onSrm() {
    this.router.navigate(['/srm', this.executionId]);
  }
}
