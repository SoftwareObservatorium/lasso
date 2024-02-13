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
import { Router } from '@angular/router';

import { first, switchMap, takeUntil } from 'rxjs/operators';

import { LassoApiServiceService } from '../_service/lasso-api-service.service'

import { ScriptInfo } from '../_model/lsl';
import { Subject, timer } from 'rxjs';

const INTERVAL = 5000;  // poll every 5 seconds

@Component({
  selector: 'app-scripts',
  templateUrl: './scripts.component.html',
  styleUrls: ['./scripts.component.css']
})
export class ScriptsComponent implements OnInit {

  closeTimer$ = new Subject<any>();

  error = ''
  scriptInfos : ScriptInfo[]

  loading = false

  constructor(private lassoApiService: LassoApiServiceService,
    private router: Router) {
      //
  }

  ngOnInit() {
    timer(0, INTERVAL).pipe(
    switchMap(() => {
      this.loading = true
      return this.getScripts()
    }),
    takeUntil(this.closeTimer$)
  ).subscribe({
    next: (res: any) => {
      //this.closeTimer$.next();  // stop polling
      this.scriptInfos = res
      this.loading = false
    },
    error: (error: any) => {
      // handle errors
      // note that any errors would stop the polling here
      this.error = error;
      this.loading = false
    }
  });
  }

  ngOnDestroy() {
    this.closeTimer$.next();
  }

  /**
   * Get script infos
   */
  getScripts() {
    // execute
    return this.lassoApiService.scripts();
    // .pipe(first())
    //   .subscribe(
    //       data => {
    //           this.scriptInfos = data
    //       },
    //       error => {
    //           this.error = error;
    //       });
  }

  /**
   * View records
   * 
   * @param scriptInfo 
   * 
   */
  viewRecords(scriptInfo: ScriptInfo) {
    this.router.navigate(['/workspace', scriptInfo.executionId]);
  }

  /**
   * View script in editor
   * 
   * @param scriptInfo 
   * 
   */
  openScript(scriptInfo: ScriptInfo) {
    localStorage.setItem(`currentScript_${scriptInfo.executionId}`, JSON.stringify(scriptInfo));

    this.router.navigate(['/editor', scriptInfo.executionId]);
  }

    /**
   * Query
   * 
   * @param scriptInfo 
   * 
   */
  openQuery(scriptInfo: ScriptInfo) {
    this.router.navigate(['/database', scriptInfo.executionId]);
  }

  /**
   * View script in editor
   * 
   * @param scriptInfo 
   * 
   */
  openSearchResults(scriptInfo: ScriptInfo) {
    localStorage.setItem(`currentScript_${scriptInfo.executionId}`, JSON.stringify(scriptInfo));

    this.router.navigate(['/search', scriptInfo.executionId]);
  }
}
