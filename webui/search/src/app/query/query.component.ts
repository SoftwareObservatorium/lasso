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

import { AfterViewInit, Component, OnInit } from '@angular/core';
import { MatTabChangeEvent } from '@angular/material/tabs';
import { HotTableRegisterer } from '@handsontable/angular';

import { LassoApiServiceService } from '../service/lasso-api-service.service';
import { ActivatedRoute, Router } from '@angular/router';
import { ActionInfo, DataSourceResponse, LSLInfoResponse, LslRequest, ScriptInfo } from '../model/lsl';
import { AuthenticationService } from '../service/authentication.service';
import { User } from '../model/user';
import { first } from 'rxjs/operators';

import { MatSelectChange } from '@angular/material/select';

@Component({
  selector: 'app-query',
  templateUrl: './query.component.html',
  styleUrls: ['./query.component.css']
})
export class QueryComponent implements OnInit, AfterViewInit {

  currentEditorTab: number = 0

  ledges: any = []
  lnodes: any = []

  // monaco for LSL
  lslEditorOptions = {theme: 'vs-light', language: 'lsl'};
  lslCode: string= `dataSource 'lasso_quickstart'
study(name: 'HelloWorld') {

    /* create stimulus matrix */
    action(name: 'create') {
        execute {
            // from JDK classes
            stimulusMatrix('Stack', """Stack {
                    push(java.lang.String)->java.lang.String
                    size()->int }""",
                    [
                            implementation("1", "java.util.Stack"),
                            implementation("2", "java.util.ArrayDeque"),
                            implementation("3", "java.util.LinkedList")
                    ], [
                    test(name: 'testPush()') {
                        row '',    'create', 'Stack'
                        row '',  'push',   'A1',     '"Hello World!"'
                        row '',  'size',   'A1'
                    }])
        }
    }
    /* Execute stimulus matrix and obtain stimulus response matrix */
    action(name: 'filter', type: 'Arena') {
        dependsOn 'create'
        include 'Stack'
        profile('java17Profile') {
            scope('class') { type = 'class' }
            environment('java17') {
                image = 'maven:3.9-eclipse-temurin-17' // docker image (JDK 17)
            }
        }
    }
}`;

  currentUser: User;
  error: string;

  constructor(
    private lassoApiService: LassoApiServiceService,
    private authenticationService: AuthenticationService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.authenticationService.currentUser.subscribe(
      (x) => (this.currentUser = x)
    );
  }

  lslEditor: any;

  onLSLInit(editor: any) {
    this.lslEditor = editor;
    console.log("lsl editor initialized")
  }

  private hotRegisterer = new HotTableRegisterer();

  sheets: string[] = ['Sheet 1'];
  currentSheetTab: MatTabChangeEvent = null;
  selectedSheetIndex: any = 0;

  scriptLabel: string;
  scriptDescription: string;
  scriptPermissionType: string;
  scriptTagsCommaSeparated: string;

  infoResponse: LSLInfoResponse;

  datasourceResponse: DataSourceResponse;

  ngOnInit(): void {
    // load script content from router
    this.route.paramMap.subscribe((params) => {
      let executionId = params.get("executionId");

      console.log(executionId);

      // set LSL script from localStorage
      if (executionId) {
        let scriptInfo: ScriptInfo = JSON.parse(
          localStorage.getItem(`currentScript_${executionId}`)
        );

        this.lslCode = scriptInfo.content;
      }
    });

    // load available actions
    this.getActions();
    // available ds
    this.getDataSources();
  }

  ngAfterViewInit(): void {
    //
  }

  readScript(fileChangeEvent: Event) {(async() => {
    const file = (fileChangeEvent.target as HTMLInputElement).files[0];
    let fileReader = new FileReader();
    fileReader.readAsText(file);
    fileReader.onload = (e) => {
      this.lslCode = fileReader.result as string;
    }
    })();
  }

parseCSV(input: string, options?: { trim?: boolean; skipEmpty?: boolean }): string[] {
  const { trim = true, skipEmpty = false } = options || {};

  return input
    .split(',')
    .map(item => (trim ? item.trim() : item))
    .filter(item => (skipEmpty ? item !== '' : true));
}

  onSearch(draft: boolean): void {
    let lslRequest = new LslRequest();

    // get current value
    lslRequest.script = this.lslEditor.getModel(null).getValue();

    lslRequest.email = this.currentUser.email;
    lslRequest.type = draft ? "DRAFT" : null; // or draft

    lslRequest.label = this.scriptLabel;
    lslRequest.description = this.scriptDescription;
    if(this.scriptPermissionType) {
        lslRequest.permissionType = this.scriptPermissionType;
    } else {
        lslRequest.permissionType = "LIMITED_SHARING";
    }

    // tags
    if(this.scriptTagsCommaSeparated) {
      lslRequest.tags = this.parseCSV(this.scriptTagsCommaSeparated);
    }

    console.log(lslRequest);

    // execute
    this.lassoApiService
      .execute(lslRequest)
      .pipe(first())
      .subscribe({
        error: (e) => {this.error = e;
          console.log(this.error);},
        complete: () => {this.router.navigate(["/scripts"])} 
    });
  }

  onRefreshLSL(): void {
    this.lslCode = "// TODO generate";
  }

  onTabChange(event: MatTabChangeEvent) {
    console.log(event.index)

    this.currentEditorTab = event.index

    // FIXME
    this.lslEditor.layout();

    if(event.index == 1) {
      // fetch latest graph
      let lslRequest = new LslRequest();
  
      // get current value
      lslRequest.script = this.lslEditor.getModel(null).getValue();
  
      lslRequest.email = this.currentUser.email;
      lslRequest.type = "DRAFT"; // draft
  
      console.log(lslRequest);
  
      // execute
      this.lassoApiService
        .graph(lslRequest)
        .pipe(first())
        .subscribe(
            data => {
              this.ledges = data.edges
              this.lnodes = data.nodes

              console.log(JSON.stringify(data))
            },
            error => {
                this.error = error;
            });
    }
  }

  onNodeSelect(event: any) {

  }

  //
    /**
   * Create new study
   */
    newStudy() {
      let selection = this.lslEditor.getSelection();
  
      let content = `dataSource 'mavenCentral2023'

study(name:'myStudy') {
    
}`;
      //this.lslEditor.getModel(null).setValue(content);
      this.lslCode = content;
    }


  /**
   * get available actions
   */
  getActions() {
    // execute
    this.lassoApiService
      .actions()
      .pipe(first())
      .subscribe(
        (data) => {
          this.infoResponse = data;
        },
        (error) => {
          this.error = error;
        }
      );
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

    /**
   * Add new action to editor
   *
   * @param actionInfo
   */
    addAction(actionInfo: ActionInfo | null) {
      let selection = this.lslEditor.getSelection();
  
      let model = this.lslEditor.getModel(null);
  
      let actionName: string = "myAction";
      let actionType: string = "";
      let actionConfig: string = "// configuration\n";
  
      if (actionInfo) {
        actionName = "my" + actionInfo.type;
        actionType = `, type:'${actionInfo.type}'`;
  
        // add configuration
        for (let entry of Object.entries(actionInfo.configuration)) {
          let bla: [string, any] = entry;
          actionConfig += `\n        /* ${bla[1]["description"]}`;
          actionConfig += `\n        * Type ${bla[1]["type"]} (optional = ${bla[1]["optional"]}) */`;
          actionConfig += `\n        ${bla[0]} = null`;
        }
      }
  
      // update editor model
      model.pushEditOperations(
        [],
        [
          {
            range: {
              startLineNumber: selection.startLineNumber,
              startColumn: selection.startColumn - 1,
              endLineNumber: selection.startLineNumber,
              endColumn: selection.startColumn,
            },
            text: `
      action(name:'${actionName}'${actionType}) {
          ${actionConfig}
  
          dependsOn '' // mandatory
          includeAbstractions '*'
          profile('myTdsProfile') {
              scope('class') { type = 'class' }
              environment('java11') {
                  image = 'maven:3.6.3-openjdk-17' // change
              }
          }
            
          whenAbstractionsReady() {
              //
          }
      }`,
          },
        ]
      );
    }

  /**
   * Add new select action to editor
   */
  addSelectAction() {
    let selection = this.lslEditor.getSelection();

    let model = this.lslEditor.getModel(null);

    let actionName: string = "mySelectAction";
    let actionType: string = "Select";
    let actionConfig: string = "// configuration\n";

    // update editor model
    model.pushEditOperations(
      [],
      [
        {
          range: {
            startLineNumber: selection.startLineNumber,
            startColumn: selection.startColumn - 1,
            endLineNumber: selection.startLineNumber,
            endColumn: selection.startColumn,
          },
          text: `
    action(name:'${actionName}',type:'${actionType}') {
        dependsOn ''
        includeAbstractions '*'
        abstraction('Stack') { // interface-driven code search
            queryForClasses """Stack {
              push(java.lang.Object)->java.lang.Object
              pop()->java.lang.Object
              peek()->java.lang.Object
              size()->int}""", 'class-simple'
            rows = totalRows
            excludeClassesByKeywords(['private', 'abstract'])
            excludeTestClasses()
            excludeInternalPkgs()
        }
    }`,
        },
      ]
    );
  }

  /**
   * Add action template to editor
   * 
   * @param event 
   * 
   */
  onAddAction(event: MatSelectChange) {
    if(event.value === 'empty') {
      this.addAction(null)
    } else if(event.value === 'select') {
      this.addSelectAction()
    } else {
      let key: ActionInfo = event.value

      this.addAction(key)
    }
  }

  /**
   * Add new data source to editor
   *
   * @param dsInfo
   */
  addDataSource(dsInfo: any) {
    let selection = this.lslEditor.getSelection();

    let model = this.lslEditor.getModel(null);

    // update editor model
    model.pushEditOperations(
      [],
      [
        {
          range: {
            startLineNumber: 0,
            startColumn: 0,
            endLineNumber: selection.startLineNumber,
            endColumn: selection.startColumn,
          },
          text: `\n// ${dsInfo.description}\ndataSource '${dsInfo.id}'\n`,
        },
      ]
    );
  }

  /**
   * Add action template to editor
   * 
   * @param event 
   * 
   */
  onAddDatasource(event: MatSelectChange) {
    let key: ActionInfo = event.value

    this.addDataSource(key)
  }
}