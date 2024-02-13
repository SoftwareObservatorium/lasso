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

import { Component } from '@angular/core';
import { DataSourceResponse } from '../model/lsl';
import { LassoApiServiceService } from '../service/lasso-api-service.service';
import { first } from 'rxjs';

@Component({
  selector: 'app-datasources',
  templateUrl: './datasources.component.html',
  styleUrls: ['./datasources.component.css']
})
export class DatasourcesComponent {
  //showUnstable:Boolean = false;

  error = '';
  datasourceResponse: DataSourceResponse;

  constructor(private lassoApiService: LassoApiServiceService) {
    //
  }

  ngOnInit() {
    this.getDataSources()
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
          },
          (error) => {
            this.error = error;
          }
        );
    }

}
