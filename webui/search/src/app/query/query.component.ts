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

def totalRows = 10
def noOfAdapters = 100
// interface in LQL notation
def interfaceSpec = """Base64{encode(byte[])->byte[]}"""
study(name: 'Base64encode') {
    /* select class candidates using interface-driven code search */
    action(name: 'select', type: 'Select') {
        abstraction('Base64') {
            queryForClasses interfaceSpec, 'class-simple'
            rows = totalRows
            excludeClassesByKeywords(['private', 'abstract'])
            excludeTestClasses()
            excludeInternalPkgs()
        }
    }
    /* filter candidates by two tests (test-driven code filtering) */
    action(name: 'filter', type: 'ArenaExecute') { // filter by tests
        containerTimeout = 10 * 60 * 1000L // 10 minutes
        specification = interfaceSpec
        sequences = [
                // parameterised sheet (SSN) with default input parameter values
                // expected values are given in first row (oracle)
                'testEncode': sheet(base64:'Base64', p2:"user:pass".getBytes()) {
                    row  '',    'create', '?base64'
                    row 'dXNlcjpwYXNz'.getBytes(),  'encode',   'A1',     '?p2'
                },
                'testEncode_padding': sheet(base64:'Base64', p2:"Hello World".getBytes()) {
                    row  '',    'create', '?base64'
                    row 'SGVsbG8gV29ybGQ='.getBytes(),  'encode',   'A1',     '?p2'
                }
        ]
        features = ['cc'] // enable code coverage measurement (class scope)
        maxAdaptations = noOfAdapters // how many adaptations to try

        dependsOn 'select'
        includeAbstractions 'Base64'
        profile('myTdsProfile') {
            scope('class') { type = 'class' }
            environment('java11') {
                image = 'maven:3.6.3-openjdk-17' // Java 17
            }
        }

        // match implementations (note no candidates are dropped)
        whenAbstractionsReady() {
            def base64 = abstractions['Base64']
            // define oracle based on expected responses in sequences
            def expectedBehaviour = toOracle(srm(abstraction: base64).sequences)
            // returns a filtered SRM
            def matchesSrm = srm(abstraction: base64)
                    .systems // select all systems
                    .equalTo(expectedBehaviour) // functionally equivalent
        }
    }
    /* rank candidates based on functional correctness */
    action(name:'rank', type:'Rank') {
        // sort by functional similarity (passing tests/total tests) descending
        criteria = ['FunctionalSimilarityReport.score:MAX:1'] // more criteria possible

        dependsOn 'filter'
        includeAbstractions '*'
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

  onSearch(draft: boolean): void {
    let lslRequest = new LslRequest();

    // get current value
    lslRequest.script = this.lslEditor.getModel(null).getValue();

    lslRequest.email = this.currentUser.email;
    lslRequest.type = draft ? "DRAFT" : null; // or draft

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