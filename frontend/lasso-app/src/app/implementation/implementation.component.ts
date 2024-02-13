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
import { ImplementationRequest } from '@app/_model/lsl';
import { first } from 'rxjs/operators';
import { HighlightService } from '@app/_service/highlight.service';

@Component({
  selector: 'app-implementation',
  templateUrl: './implementation.component.html',
  styleUrls: ['./implementation.component.css']
})
export class ImplementationComponent implements OnInit, AfterViewChecked {

  implementationId = ''
  dataSourceId = ''

  implementation
  error = ''

  json = ''
  jsonRaw = ''

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
    if (this.implementation && !this.highlighted) {
      this.highlightService.highlightAll();
      this.highlighted = true;
    }
  }

  ngOnInit() {
    // get from router
    this.route.paramMap.subscribe(params => {
      this.implementationId = params.get('implementationId')
      this.dataSourceId = params.get('dataSourceId')

      this.getImplementations(this.dataSourceId, this.implementationId)
    });
  }

  getImplementations(dataSourceId: string, implementationId: string) {
    let request = new ImplementationRequest()
    request.ids = [implementationId]
    
    if(dataSourceId) {
      this.lassoApiService.implementationsForDataSource(dataSourceId, request).pipe(first())
      .subscribe(
          data => {
              this.implementation = data.implementations[implementationId]
  
              this.json = JSON.stringify(this.implementation, null, 4)
              this.jsonRaw = JSON.stringify(data.implementationsRaw[implementationId], null, 4)
          },
          error => {
              this.error = error;
          });
    } else {
      // TODO remove
      this.lassoApiService.implementations(request).pipe(first())
      .subscribe(
          data => {
              this.implementation = data.implementations[implementationId]
  
              this.json = JSON.stringify(this.implementation, null, 4)
              this.jsonRaw = JSON.stringify(data.implementationsRaw[implementationId], null, 4)
          },
          error => {
              this.error = error;
          });
    }
  }
}
