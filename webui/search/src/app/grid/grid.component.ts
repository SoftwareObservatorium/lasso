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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { LassoApiServiceService } from '../service/lasso-api-service.service';
import { first } from 'rxjs/operators';

@Component({
  selector: 'app-grid',
  templateUrl: './grid.component.html',
  styleUrls: ['./grid.component.css']
})
export class GridComponent implements OnInit, OnDestroy {


  get now() : string { return Date(); }

  error = ''
  metricsResponse: any
  nodesResponse: any

  constructor(private lassoApiService: LassoApiServiceService) {
    //
  }

  ngOnInit() {
    this.getMetrics()
    this.getNodes()
  }

  getMetrics() {
    // execute
    this.lassoApiService.clusterMetrics().pipe(first())
      .subscribe(
          data => {
              this.metricsResponse = data

              // Object.keys(this.metricsResponse).forEach((key: string) => 
              // {
              //   console.log(`Found metrics '${key}'`)
              // });
          },
          error => {
              this.error = error;
          });
  }

  getNodes() {
    // execute
    this.lassoApiService.clusterNodes().pipe(first())
      .subscribe(
          data => {
              // flatten nodes
              let nodes = new Array();

              data.nodes.forEach((node: any) => {
                nodes.push(this.filterAndFlattenObject(node));
              });

              this.nodesResponse = {"nodes" : nodes};
          },
          error => {
              this.error = error;
          });
  }

  ngOnDestroy(): void {
    
  }

  /**
   * Ignores attributes
   * 
   * @param ob 
   */
  filterAndFlattenObject(ob: any) {
    var toReturn: {[index: string]:any} = {};
    
    for (var i in ob) {
      if (!ob.hasOwnProperty(i)) continue;

      // ignore attributes
      if(i.startsWith("attribute")) {
        continue;
      }
      
      if ((typeof ob[i]) == 'object') {
        var flatObject = this.filterAndFlattenObject(ob[i]);
        for (var x in flatObject) {
          if (!flatObject.hasOwnProperty(x)) continue;
          
          toReturn[i + '.' + x] = flatObject[x];
        }
      } else {
        toReturn[i] = ob[i];
      }
    }
    return toReturn;
  };
}
