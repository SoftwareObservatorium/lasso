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

import { Component, OnInit, AfterViewChecked } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { LassoApiServiceService } from '@app/_service/lasso-api-service.service';
import { first } from 'rxjs/operators';
import { HighlightService } from '@app/_service/highlight.service';

@Component({
  selector: 'app-cache',
  templateUrl: './cache.component.html',
  styleUrls: ['./cache.component.css']
})
export class CacheComponent implements OnInit, AfterViewChecked {

  error = ''
  report
  reportStr

  executionId: string
  reportId: string
  actionId: string
  abstractionId: string
  implementationId: string
  dataSourceId: string
  permId: string

  highlighted: boolean = false;

  constructor(private route: ActivatedRoute,
    private router: Router,
    private lassoApiService: LassoApiServiceService,
    private highlightService: HighlightService) {
    //
  }

  /**
   * Highlight code when ready
   */
  ngAfterViewChecked() {
    if (this.report && !this.highlighted) {
      this.highlightService.highlightAll();
      this.highlighted = true;
    }
  }

  ngOnInit() {
    // get from router
    this.route.paramMap.subscribe(params => {
      this.executionId = params.get('executionId')
      
      //this.reportId = params.get('reportId')

      // split: remove node name from path
      this.reportId = params.get('reportId').split(':')[1]
      this.actionId = params.get('actionId')
      this.abstractionId = params.get('abstractionId')
      this.implementationId = params.get('implementationId')
      this.dataSourceId = params.get('dataSourceId')
      this.permId = params.get('permId')
    });

    // retrieve report
    this.getCachedReport()
  }

  getCachedReport() {
    // execute
    this.lassoApiService.cachedReport(this.executionId, this.reportId, this.actionId, this.abstractionId, this.implementationId, this.dataSourceId, this.permId).pipe(first())
      .subscribe(
          data => {
              console.log(data)

              this.report = data
              this.reportStr = JSON.stringify(this.report)
          },
          error => {
              this.error = error;
          });
  }
}
