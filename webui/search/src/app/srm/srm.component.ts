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
import { first } from 'rxjs/operators';
import { LassoUtils } from '../utils/lutils';
import { MatSelectChange } from '@angular/material/select';
import { MatChipSelectionChange } from '@angular/material/chips';
import { OracleDescription } from '../model/data';
import { SrmQueryRequest } from '../model/lsl';

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

  executionId: string;
  tableType: string = "outputs";

  error: string;

  totalRows = 0;
  pageSize = 5;
  currentPage = 0;
  pageSizeOptions: number[] = [5, 10, 25, 100];

  isLoading = false;

  oracle: OracleDescription;
  // selected outputs for oracle filtering
  oracleFilters: Map<string, string>

  actuationSheetFragments = new Map<string, any[]>();

  constructor(private route: ActivatedRoute,
    private router: Router,
    private lassoApiService: LassoApiServiceService) {
    //
  }

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      this.executionId = params.get("executionId");
    });

    // TODO params
    this.route.queryParamMap.subscribe((paramMap) => {
      let tableType: string = paramMap.get('view')
      if(tableType) {
        this.tableType = tableType
      }
    });

    this.loadOutputTable(this.tableType);
  }

  /**
   * Set table type
   * 
   * @param event 
   * 
   */
  onChangeTable(event: MatSelectChange) {
    this.tableType = event.value
  }

  ngAfterViewInit(): void {
    //
  }


  /**
   * load output table
   */
  loadOutputTable(tableType: string) {
    let oracleFilters = null
    if(this.oracleFilters) {
      console.log(this.oracleFilters)
      const convMap: any = {};
      this.oracleFilters.forEach((val: string, key: string) => {
        convMap[key] = val;
      });
      oracleFilters = convMap
    }

    if(tableType === "outputs") {
      if(oracleFilters) {
        let request: SrmQueryRequest = new SrmQueryRequest()
        request.oracleFilters = oracleFilters
        request.type = "value"
        this.lassoApiService.actuationSheetsByOracleFilters(this.executionId, "value", request)
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
      } else {
      // execute
      this.lassoApiService.actuationSheets(this.executionId, "value")
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
    } else if(tableType === "sheets") {

    }
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

  getColumnLabel(col: string): string {
    return LassoUtils.getColumnLabel(col)
  }

  openCode(col: string) {
    if(col.indexOf('_') > -1) {
      let parts = col.split('_')
      
      // TODO set datasource dynamically
      this.router.navigate(
        ['/search'],
        {
          queryParams: { lql: '*:*', filter: [`id:"${parts[0]}"`], strategy: 'class-simple', datasource: 'lasso_quickstart' },
          queryParamsHandling: 'merge' }
        ).then(() => {
          window.location.reload();
        });
    }
  }

  formatOracleRow(r: any) {
    return "(" + r['STATEMENT'] + ") " + r[this.oracle.referenceImpl]
    //return JSON.stringify(r)
  }

  onFilterByOracleValues() {
    console.log(this.oracleFilters)

    // load new (filtered) results
    this.loadOutputTable(this.tableType)
  }

  onOutputSelectionChange($event: MatChipSelectionChange, o: any, i: number) {
    if($event.selected) {
      this.oracleFilters.set(o['STATEMENT'], o[this.oracle.referenceImpl])
    } else {
      this.oracleFilters.delete(o['STATEMENT'])
    }
  }

  getImplementationLabel(impl: string): string {
    return LassoUtils.getColumnLabel(impl)
  }

  filterByOracle(impl: string) {
    this.oracle = new OracleDescription()
    this.oracle.referenceImpl = impl // is already full id with impl id

    let parts = impl.split('_')
    
    this.getActuationSheetFragment(parts[0], 'value')

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

  getActuationSheetFragment(implId: string, type: string) {
    this.lassoApiService.actuationSheetsForSystem(this.executionId, implId, type).pipe(first())
    .subscribe(
        data => {
            this.actuationSheetFragments.set(this.getActuationKey(implId, type), data)

            let response = this.actuationSheetFragments.get(this.getActuationKey(implId, 'value'))
            this.oracle.manual = response
            this.oracleFilters = new Map<string, string>();
        },
        error => {
            this.error = error;
        });
  }

  getActuationKey(implId: string, type: string) {
    return implId + "_" + type
  }
}
