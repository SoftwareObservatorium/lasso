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

import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HotTableModule } from '@handsontable/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { ResultsComponent } from './results/results.component';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatCardModule } from '@angular/material/card';
import { MatMenuModule } from '@angular/material/menu';
import { QueryComponent } from './query/query.component';

import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { MonacoEditorModule, NgxMonacoEditorConfig } from 'ngx-monaco-editor-v2';
import { JwtInterceptor } from './service/jwt.interceptor';
import { ErrorInterceptor } from './service/error.interceptor';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { LoginComponent } from './login/login.component';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import {MatTabsModule} from '@angular/material/tabs';
import {MatExpansionModule} from '@angular/material/expansion';
import { ScriptsComponent } from './scripts/scripts.component';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatBadgeModule} from '@angular/material/badge';
import { SrmComponent } from './srm/srm.component';
import { ActionsComponent } from './actions/actions.component';
import { HomeComponent } from './home/home.component';
import { HighlightService } from './service/highlight.service';
import { NgxGraphModule } from '@swimlane/ngx-graph';
import { GraphQLModule } from './graphql.module';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTreeModule } from '@angular/material/tree';
import {MatChipsModule} from '@angular/material/chips';
import { WorkspaceComponent } from './workspace/workspace.component';
import { GridComponent } from './grid/grid.component';
import { LogsComponent } from './logs/logs.component';
import { DfsComponent } from './dfs/dfs.component';
import { ProfileComponent } from './profile/profile.component';
import { DatasourcesComponent } from './datasources/datasources.component';
import { CodeComponent } from './code/code.component';
import { DbComponent } from './db/db.component';
import { MarkdownModule } from 'ngx-markdown';

export function configureMonaco() {
  console.log((<any>window).monaco); 

  // register new language
  (<any>window).monaco.languages.register({
    id: 'lsl',
  });

  // taken and modified from https://microsoft.github.io/monaco-editor/monarch.html
  (<any>window).monaco.languages.setMonarchTokensProvider('lsl', {
      defaultToken: '',
      tokenPostfix: '.lsl',

      keywords: [
        'abstract', 'continue', 'for', 'new', 'switch', 'assert', 'default',
        'goto', 'package', 'synchronized', 'boolean', 'do', 'if', 'private',
        'this', 'break', 'double', 'implements', 'protected', 'throw', 'byte',
        'else', 'import', 'public', 'throws', 'case', 'enum', 'instanceof', 'return',
        'transient', 'catch', 'extends', 'int', 'short', 'try', 'char', 'final',
        'interface', 'static', 'void', 'class', 'finally', 'long', 'strictfp',
        'volatile', 'const', 'float', 'native', 'super', 'while', 'true', 'false',
        // LSL keywords
        'study', 'action', 'dataSource', 'abstraction', 'whenAbstractionsReady'
      ],

      operators: [
        '=', '>', '<', '!', '~', '?', ':',
        '==', '<=', '>=', '!=', '&&', '||', '++', '--',
        '+', '-', '*', '/', '&', '|', '^', '%', '<<',
        '>>', '>>>', '+=', '-=', '*=', '/=', '&=', '|=',
        '^=', '%=', '<<=', '>>=', '>>>='
      ],

      // we include these common regular expressions
      symbols: /[=><!~?:&|+\-*\/\^%]+/,
      escapes: /\\(?:[abfnrtv\\"']|x[0-9A-Fa-f]{1,4}|u[0-9A-Fa-f]{4}|U[0-9A-Fa-f]{8})/,
      digits: /\d+(_+\d+)*/,
      octaldigits: /[0-7]+(_+[0-7]+)*/,
      binarydigits: /[0-1]+(_+[0-1]+)*/,
      hexdigits: /[[0-9a-fA-F]+(_+[0-9a-fA-F]+)*/,

      // The main tokenizer for our languages
      tokenizer: {
        root: [
          // identifiers and keywords
          [/[a-zA-Z_$][\w$]*/, {
            cases: {
              '@keywords': { token: 'keyword.$0' },
              '@default': 'identifier'
            }
          }],

          // whitespace
          { include: '@whitespace' },

          // delimiters and operators
          [/[{}()\[\]]/, '@brackets'],
          [/[<>](?!@symbols)/, '@brackets'],
          [/@symbols/, {
            cases: {
              '@operators': 'delimiter',
              '@default': ''
            }
          }],

          // @ annotations.
          [/@\s*[a-zA-Z_\$][\w\$]*/, 'annotation'],

          // numbers
          [/(@digits)[eE]([\-+]?(@digits))?[fFdD]?/, 'number.float'],
          [/(@digits)\.(@digits)([eE][\-+]?(@digits))?[fFdD]?/, 'number.float'],
          [/0[xX](@hexdigits)[Ll]?/, 'number.hex'],
          [/0(@octaldigits)[Ll]?/, 'number.octal'],
          [/0[bB](@binarydigits)[Ll]?/, 'number.binary'],
          [/(@digits)[fFdD]/, 'number.float'],
          [/(@digits)[lL]?/, 'number'],

          // delimiter: after number because of .\d floats
          [/[;,.]/, 'delimiter'],

          // strings
          [/"([^"\\]|\\.)*$/, 'string.invalid'],  // non-teminated string
          [/"/, 'string', '@string'],
          // groovy strings
          [/'/, 'string', '@string_single'],

          // characters
          [/'[^\\']'/, 'string'],
          [/(')(@escapes)(')/, ['string', 'string.escape', 'string']],
          [/'/, 'string.invalid']
        ],

        whitespace: [
          [/[ \t\r\n]+/, ''],
          [/\/\*\*(?!\/)/, 'comment.doc', '@javadoc'],
          [/\/\*/, 'comment', '@comment'],
          [/\/\/.*$/, 'comment'],
        ],

        comment: [
          [/[^\/*]+/, 'comment'],
          // [/\/\*/, 'comment', '@push' ],    // nested comment not allowed :-(
          // [/\/\*/,    'comment.invalid' ],    // this breaks block comments in the shape of /* //*/
          [/\*\//, 'comment', '@pop'],
          [/[\/*]/, 'comment']
        ],
        //Identical copy of comment above, except for the addition of .doc
        javadoc: [
          [/[^\/*]+/, 'comment.doc'],
          // [/\/\*/, 'comment.doc', '@push' ],    // nested comment not allowed :-(
          [/\/\*/, 'comment.doc.invalid'],
          [/\*\//, 'comment.doc', '@pop'],
          [/[\/*]/, 'comment.doc']
        ],

        string: [
          [/[^\\"]+/, 'string'],
          [/@escapes/, 'string.escape'],
          [/\\./, 'string.escape.invalid'],
          [/"/, 'string', '@pop']
        ],

        // groovy strings
        string_single: [
          [/[^\\']+/, 'string'],
          [/@escapes/, 'string.escape'],
          [/\\./, 'string.escape.invalid'],
          [/'/, 'string', '@pop']
        ],
      },
    }
  );

} // here monaco object will be available as window.monaco use this function to extend monaco editor functionalities.

// https://www.npmjs.com/package/ngx-monaco-editor-v2
const monacoConfig: NgxMonacoEditorConfig = {
  defaultOptions: { scrollBeyondLastLine: false }, // pass default options to be used
  onMonacoLoad: configureMonaco
};

@NgModule({
  declarations: [
    AppComponent,
    ResultsComponent,
    QueryComponent,
    CodeComponent,
    LoginComponent,
    ScriptsComponent,
    SrmComponent,
    ActionsComponent,
    HomeComponent,
    WorkspaceComponent,
    GridComponent,
    LogsComponent,
    DfsComponent,
    ProfileComponent,
    DatasourcesComponent,
    DbComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule,
    HotTableModule.forRoot(),
    BrowserAnimationsModule,
    MatToolbarModule,
    MatButtonModule,
    MatSidenavModule,
    MatIconModule,
    MatListModule,
    MatGridListModule,
    MatCardModule,
    MatMenuModule,
    MatExpansionModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatChipsModule,
    MatBadgeModule,
    MonacoEditorModule.forRoot(monacoConfig),
    MatInputModule,
    MatSelectModule,
    MatRadioModule,
    MatTabsModule,
    MatTreeModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    NgxGraphModule,
    GraphQLModule,
    MarkdownModule.forRoot()
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
    HighlightService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
