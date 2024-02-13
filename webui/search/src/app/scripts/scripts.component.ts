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
import { MatTable } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { ScriptsDataSource } from './scripts-datasource';
import { LassoApiServiceService } from '../service/lasso-api-service.service';
import { Router } from '@angular/router';
import { ScriptInfo } from '../model/lsl';

import { switchMap, takeUntil } from 'rxjs/operators';
import { Subject, timer } from 'rxjs';

const INTERVAL = 5000;  // poll every 5 seconds

@Component({
  selector: 'app-scripts',
  templateUrl: './scripts.component.html',
  styleUrls: ['./scripts.component.css']
})
export class ScriptsComponent implements AfterViewInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatTable) table!: MatTable<ScriptInfo>;
  dataSource: ScriptsDataSource;

  constructor(private lassoApiService: LassoApiServiceService,
    private router: Router) {
    this.dataSource = new ScriptsDataSource(lassoApiService);
  }

  /** Columns displayed in the table. Columns IDs can be added, removed, or reordered. */
  displayedColumns = ['id', 'name', 'start', 'end', 'status', 'action'];

  //closeTimer$ = new Subject<any>();

  ngOnInit() {
  }

  ngOnDestroy() {
    //this.closeTimer$.next();
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
    this.table.dataSource = this.dataSource;

    timer(0, INTERVAL).subscribe(n => {
      console.log("refreshing scripts")
      // this.dataSource.sort = this.sort;
      // this.dataSource.paginator = this.paginator;
      // this.table.dataSource = new ScriptsDataSource(this.lassoApiService);
      // this.table.renderRows();
    });
  }

  onResults(scriptInfo: ScriptInfo): void {
    console.log(scriptInfo)

    localStorage.setItem(`currentScript_${scriptInfo.executionId}`, JSON.stringify(scriptInfo));

    this.router.navigate(['/results', scriptInfo.executionId]);
  }

  /**
   * View script in editor
   * 
   * @param scriptInfo 
   * 
   */
  onOpenScript(scriptInfo: ScriptInfo) {
    localStorage.setItem(`currentScript_${scriptInfo.executionId}`, JSON.stringify(scriptInfo));

    this.router.navigate(['/submit', scriptInfo.executionId]);
  }

  onDatabase(scriptInfo: ScriptInfo) {
    localStorage.setItem(`currentScript_${scriptInfo.executionId}`, JSON.stringify(scriptInfo));

    this.router.navigate(['/srm', scriptInfo.executionId]);
  }

  onWorkspace(scriptInfo: ScriptInfo) {
    this.router.navigate(['/workspace', scriptInfo.executionId]);
  }

  onDFS(scriptInfo: ScriptInfo) {
    this.router.navigate(['/grid/dfs', scriptInfo.executionId]);
  }
}
