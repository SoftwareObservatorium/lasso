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
import { ActivatedRoute, Router } from '@angular/router';
import { LassoApiServiceService } from '../service/lasso-api-service.service';
import { FileViewRequest, RecordsRequest } from '../model/lsl';
import { first } from 'rxjs/operators';
import { FlatTreeControl } from '@angular/cdk/tree';
import { MatTreeFlatDataSource, MatTreeFlattener } from '@angular/material/tree';

interface FileNode {
  text: string;
  value: string,
  children?: FileNode[];
}

/** Flat node with expandable and level information */
interface ExampleFlatNode {
  expandable: boolean;
  name: string;
  level: number;
  value: string;
}

@Component({
  selector: 'app-dfs',
  templateUrl: './dfs.component.html',
  styleUrls: ['./dfs.component.css']
})
export class DfsComponent {

  executionId: string | null = ''

  error = ''

  files: any

  selectedFiles: any

  filePatterns: string = "/"

  constructor(private route: ActivatedRoute,
    private router: Router,
    private lassoApiService: LassoApiServiceService) {
      //
  }

  private _transformer = (node: FileNode, level: number) => {
    return {
      expandable: !!node.children && node.children.length > 0,
      name: node.text,
      level: level,
      value: node.value
    };
  };

  treeControl = new FlatTreeControl<ExampleFlatNode>(
    node => node.level,
    node => node.expandable,
  );

  treeFlattener = new MatTreeFlattener(
    this._transformer,
    node => node.level,
    node => node.expandable,
    node => node.children,
  );

  dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);

  hasChild = (_: number, node: ExampleFlatNode) => node.expandable;

  ngOnInit() {
    // get from router
    this.route.paramMap.subscribe(params => {
      this.executionId = params.get('executionId')

      if(this.executionId) {
        this.filePatterns = `/workspace/${this.executionId}/`
      }

      this.getFiles()
    });
  }

  patternToArray() {
    return this.filePatterns.split(",")
  }

  /**
   * List files
   */
  getFiles() {
    let request = new FileViewRequest()
    request.filePatterns = this.patternToArray()

    // execute
    this.lassoApiService.dfsFiles(request).pipe(first())
      .subscribe(
          data => {
              let rootNode: FileNode = data.root
              this.dataSource.data = [rootNode]

              console.log(JSON.stringify(rootNode))
          },
          error => {
              this.error = error;
          });
  }

  updateFiles() {
    this.getFiles()
  }

  /**
   * Download file
   */
  downloadFile(file: string) {
    let recordsRequest = new RecordsRequest()
    recordsRequest.filePatterns = [file]

    // execute
    this.lassoApiService.downloadDfsFile(recordsRequest)
  }
}
