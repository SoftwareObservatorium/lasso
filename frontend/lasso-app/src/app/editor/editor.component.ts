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

import { Component, OnInit } from "@angular/core";
import { Router, ActivatedRoute } from "@angular/router";

import { NgxEditorModel } from "ngx-monaco-editor";

import { first } from "rxjs/operators";

import { User } from "../_model/user";

import { AuthenticationService } from "../_service/authentication.service";
import { LassoApiServiceService } from "../_service/lasso-api-service.service";
import {
  LslRequest,
  LslResponse,
  ScriptInfo,
  LSLInfoResponse,
  DataSourceResponse,
} from "../_model/lsl";
import { Templates } from "src/codeTemplates/templates";
import { EditorService } from "./editor.service";
import { NgxSmartModalService } from "ngx-smart-modal";
import { Subject } from "rxjs";

import * as shape from 'd3-shape';

@Component({
  selector: "app-editor",
  templateUrl: "./editor.component.html",
  styleUrls: ["./editor.component.css"],
})
export class EditorComponent implements OnInit {
  nodes = [];
  edges = [];
  curve = shape.curveLinear;
  currentUser: User;
  infoResponse: LSLInfoResponse;

  datasourceResponse: DataSourceResponse;

  error = "";
  actionNames = [];
  fileData: File = null;
  selectedFile: string = "Choose File";
  scripts: any;
  zoomToFit$: Subject<boolean> = new Subject();
  constructor(
    private lassoApiService: LassoApiServiceService,
    private authenticationService: AuthenticationService,
    private router: Router,
    private route: ActivatedRoute,
    private editorService: EditorService,
    private ngxSmartModalService: NgxSmartModalService
  ) {
    this.authenticationService.currentUser.subscribe(
      (x) => (this.currentUser = x)
    );
  }

  ngOnInit() {
    // load script content from router
    this.route.paramMap.subscribe((params) => {
      let executionId = params.get("executionId");

      // set LSL script from localStorage
      if (executionId) {
        let scriptInfo: ScriptInfo = JSON.parse(
          localStorage.getItem(`currentScript_${executionId}`)
        );

        this.editorModel.value = scriptInfo.content;
      } else {
        // set default dummy value
        this.editorModel.value = this.code;
      }
    });
    this.getScripts();
    // load available actions
    this.getActions();
    // available ds
    this.getDataSources();
  }

  // monaco editor
  editorOptions = { theme: Templates.themeDefault, language: "lsl", automaticLayout: true };
  code: string = Templates.default;

  editorModel: NgxEditorModel = {
    value: this.code,
    // set language
    language: "lsl",
  };

  editor: any;

  /**
   * Called when monaco editor ready
   *
   * @param editor
   */
  onInit(editor) {
    this.editor = editor;
    this.editor.getModel(null).onDidChangeContent((event) => {});

    this.editorService.editorComponent = this;
    this.editorService.editor = editor;
    this.editorService.registerProviders();
  }
  fitGraph() {
    console.log("fit");
    this.zoomToFit$.next(true);
  }
  /**
   * Execute LSL script
   */
  executeScript(type: string) {
    let lslRequest = new LslRequest();

    // get current value
    lslRequest.script = this.editor.getModel(null).getValue();

    lslRequest.email = this.currentUser.email;
    lslRequest.type = type;

    console.log(lslRequest);

    // execute
    this.lassoApiService
      .execute(lslRequest)
      .pipe(first())
      .subscribe(
        (data) => {
          console.log(data);

          this.router.navigate(["/scripts"]);
        },
        (error) => {
          this.error = error;
        }
      );
  }

  /**
   * Blocks until result is available.
   * 
   * @param type 
   * 
   */
  executeScriptAndWait(type: string) {
    let lslRequest = new LslRequest();

    // get current value
    lslRequest.script = this.editor.getModel(null).getValue();

    lslRequest.email = this.currentUser.email;
    lslRequest.type = type;

    console.log(lslRequest);

    // execute
    this.lassoApiService
      .execute(lslRequest)
      .pipe(first())
      .subscribe(
        (data) => {
          console.log(data);

          // poll and block
          data.executionId

          //this.router.navigate(["/scripts"]);
        },
        (error) => {
          this.error = error;
        }
    );


  }

  /**
   * Choose local file
   *
   * @param fileInput
   */
  fileProgress(fileInput: any) {
    this.fileData = <File>fileInput.target.files[0];

    this.selectedFile = this.fileData.name;

    console.log(this.fileData);
  }

  /**
   * Upload script file remotely to load it into the editor
   */
  uploadFile() {
    const formData = new FormData();
    formData.append("file", this.fileData);

    this.lassoApiService
      .uploadFile(formData)
      .pipe(first())
      .subscribe(
        (data) => {
          console.log(data);

          //
          let uploadedFilePath = data.fileDownloadUri;

          this.lassoApiService
            .downloadFile(uploadedFilePath)
            .pipe(first())
            .subscribe(
              (data) => {
                console.log("yay");
                console.log(data);

                //this.editorModel.value = data
                //(<any>window).monaco.editor.getModel().value = data;
                this.editor.getModel(null).setValue(data);
              },
              (error) => {
                console.log("oh no");
                this.error = error;
              }
            );
        },
        (error) => {
          this.error = error;
        }
      );
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

          let types = Array.from(Object.keys(this.infoResponse.actions));

          this.editorService.init(types);
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
  addAction(actionInfo) {
    let selection = this.editor.getSelection();

    let model = this.editor.getModel(null);

    let actionName: string = "myAction";
    let actionType: string = "MyAction";
    let actionConfig: string = "// configuration\n";

    if (actionInfo) {
      actionName = "my" + actionInfo.key;
      actionType = actionInfo.key;

      // add configuration
      for (let entry of Object.entries(actionInfo.value.configuration)) {
        actionConfig += `\n        /* ${entry[1]["description"]}`;
        actionConfig += `\n        * Type ${entry[1]["type"]} (optional = ${entry[1]["optional"]}) */`;
        actionConfig += `\n        ${entry[0]} = null`;
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
    action(name:'${actionName}',type:'${actionType}') {
        ${actionConfig}

        dependsOn '' // mandatory
        includeAbstractions '*'
        profile {
            environment('java8') {
                image = 'maven:3.5.4-jdk-8-alpine'
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
   * Add new data source to editor
   *
   * @param dsInfo
   */
  addDataSource(dsInfo) {
    let selection = this.editor.getSelection();

    let model = this.editor.getModel(null);

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
   * Add new select action to editor
   */
  addSelectAction() {
    let selection = this.editor.getSelection();

    let model = this.editor.getModel(null);

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
        abstraction('SHA256') {
            queryForClasses 'SHA256(digest(byte[]):String;)', 'concept'
            rows = 50
            useAlternatives = false
            filter 'm_static_complexity_td:[5 TO *]'
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
   * Create new study
   */
  newStudy() {
    let selection = this.editor.getSelection();

    let content = `dataSource 'mavenCentral'

study(name:'myStudy') {
    
}`;
    this.editor.getModel(null).setValue(content);
  }

  openModal() {
    this.editorService.createNodes();
    this.ngxSmartModalService.getModal("myModal").open();
    this.fitGraph();
  }

  getScripts() {
    this.lassoApiService
      .scripts()
      .pipe(first())
      .subscribe(
        (data) => {
          this.scripts = data;
        },
        (error) => {
          this.error = error;
        }
      );
  }

  openScript(script: any) {
    this.editor.getModel(null).setValue(script.content);
  }
}
