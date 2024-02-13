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
import { ActivatedRoute } from '@angular/router';
import { LassoApiServiceService } from '@app/_service/lasso-api-service.service';
import { RecordsRequest } from '@app/_model/lsl';
import { first } from 'rxjs/operators';
import { NgxEditorModel } from 'ngx-monaco-editor';

import * as monaco from 'monaco-editor'

@Component({
  selector: 'app-viewer',
  templateUrl: './viewer.component.html',
  styleUrls: ['./viewer.component.css']
})
export class ViewerComponent implements OnInit {

  constructor(private route: ActivatedRoute,
    private lassoApiService: LassoApiServiceService) {
      //
  }

  executionId: string;
  filePath: string;

  fileSource: string = "";

  error

    // monaco editor
    editorOptions = {theme: 'vs-dark', language: undefined, automaticLayout: true};
    code: string= "SELECT * from SelectReport";
  
    editorModel: NgxEditorModel = {
      value: this.fileSource,
      // set language
      language: undefined
    };
  
    editor: any;

  ngOnInit() {
    // get from router
    this.route.paramMap.subscribe(params => {
      this.executionId = params.get('executionId')
      this.filePath = params.get('filePath')

      console.log(this.filePath)

      // fetch

      console.log(`found ${this.executionId} csv path '${this.filePath}'`)

      this.getFile()
    });
  }

    /**
   * Called when monaco editor ready
   * 
   * @param editor 
   */
  onInit(editor) {
    this.editor = editor;

    this.editor.getModel(null).onDidChangeContent((event) => {
      // do something https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.imodelcontentchangedevent.html
      //console.log(event);
    });

    //this.editor.getModel(null).setValue(this.fileSource);
    this.updateEditorModel()
  }

  /**
   * get file
   */
  getFile() {
    let recordsRequest = new RecordsRequest()
    recordsRequest.filePatterns = [this.filePath]

    // execute
    this.lassoApiService.getCSVFile(recordsRequest, this.executionId).pipe(first())
      .subscribe(
                  data => {
                    //console.log(data);
        
                    //
                    this.fileSource = data

                    //this.editor.getModel(null).setValue(this.fileSource);
                    this.updateEditorModel()
                  },
                  error => {
                    this.error = error;
          });
  }

  /**
   * Update editor model and set language according to filename.
   * 
   */
  updateEditorModel() {
    let model = monaco.editor.getModel(monaco.Uri.file(this.filePath))
    if(model) {
      model.setValue(this.fileSource)
      this.editor.setModel(model)
    } else {
      let model = monaco.editor.createModel(
      this.fileSource,
      undefined, // language
      monaco.Uri.file(this.filePath) // uri
    )

      this.editor.setModel(model)
    }
  }

}
