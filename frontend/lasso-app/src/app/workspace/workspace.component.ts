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

import { Component, OnInit, Input } from '@angular/core';

import { ActivatedRoute, Router } from '@angular/router';

import { first } from 'rxjs/operators';

import { LassoApiServiceService } from '../_service/lasso-api-service.service'

import { RecordsRequest, RecordsResponse, FileViewRequest } from '../_model/lsl';
import { TreeviewItem } from 'ngx-treeview';

@Component({
  selector: 'app-workspace',
  templateUrl: './workspace.component.html',
  styleUrls: ['./workspace.component.css']
})
export class WorkspaceComponent implements OnInit {

  executionId = ''

  error = ''

  files

  selectedFiles

  filePatterns: string = "**/*.csv,*.txt"

  config = {
    hasAllCheckBox: false,
    hasFilter: true,
    hasCollapseExpand: true,
    decoupleChildFromParent: false,
    maxHeight: 500
 }

  constructor(private route: ActivatedRoute,
    private router: Router,
    private lassoApiService: LassoApiServiceService) {
      //
  }

  ngOnInit() {
    // get from router
    this.route.paramMap.subscribe(params => {
      this.executionId = params.get('executionId')

      this.getFiles()
    });
  }

  onSelectedChange(event) {
    console.log(event)

    this.selectedFiles = event
  }

  onFilterChange(event) {
    console.log(event)
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
    this.lassoApiService.files(request, this.executionId).pipe(first())
      .subscribe(
          data => {
              this.files = []
              let rootNode: TreeviewItem = new TreeviewItem(data.root);
              this.uncheck(rootNode, false)

              this.files.push(rootNode)

              //console.log(JSON.stringify(data.root))
          },
          error => {
              this.error = error;
          });
  }

  /**
   * Workaround to uncheck und collapse nodes
   * 
   * @param node
   * @param collapsed 
   */
  uncheck(node: TreeviewItem, collapsed: boolean) {
    node.collapsed = collapsed
    node.checked = false

    if(node.children) {
      node.children.forEach(c => this.uncheck(c, true))
    }
  }

  updateFiles() {
    this.getFiles()
  }

  /**
   * Download ZIP
   */
  downloadZIPFile(file: string) {
    let recordsRequest = new RecordsRequest()
    recordsRequest.filePatterns = [file]

    // execute
    this.lassoApiService.downloadWorkspaceZIP(recordsRequest, this.executionId)
  }

    /**
   * Download ZIP
   */
  downloadFilesAsZIP() {
    let recordsRequest = new RecordsRequest()
    recordsRequest.filePatterns = this.selectedFiles

    // execute
    this.lassoApiService.downloadWorkspaceZIP(recordsRequest, this.executionId)
  }

  /**
   * Download file
   */
  downloadFile(file: string) {
    let recordsRequest = new RecordsRequest()
    recordsRequest.filePatterns = [file]

    // execute
    this.lassoApiService.downloadWorkspaceFile(recordsRequest, this.executionId)
  }

  downloadWorkspaceZIP() {
    let recordsRequest = new RecordsRequest()
    recordsRequest.filePatterns = this.patternToArray()

    // execute
    this.lassoApiService.downloadWorkspaceZIP(recordsRequest, this.executionId)
  }

  /**
   * Open table
   */
  openTable(file: string) {
    this.router.navigate(['/table', this.executionId, file]);
  }

    /**
   * Open viewer
   */
  openViewer(file: string) {
    this.router.navigate(['/viewer', this.executionId, file]);
  }

  isCSV(file: string) {
    return file.toLowerCase().endsWith('.csv');
  }

  isPlain(file: string) {
    let str: string = file.toLowerCase()

    return str.endsWith('.java') || str.endsWith('.xml') || str.endsWith('.txt') || str.endsWith('.log');
  }
}
