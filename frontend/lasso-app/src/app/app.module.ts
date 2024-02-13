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

import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

import { MonacoEditorModule, NgxMonacoEditorConfig } from 'ngx-monaco-editor';

import { MarkdownModule } from 'ngx-markdown';

import {NgxPaginationModule} from 'ngx-pagination';

import { NgxDatatableModule } from '@swimlane/ngx-datatable';

import { HeaderComponent } from './header/header.component';
import { FooterComponent } from './footer/footer.component';
import { EditorComponent } from './editor/editor.component';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './login/login.component';
import { ScriptsComponent } from './scripts/scripts.component';

import { JwtInterceptor } from './_service/jwt.interceptor';
import { ErrorInterceptor } from './_service/error.interceptor';
import { ActionsComponent } from './actions/actions.component';
import { WorkspaceComponent } from './workspace/workspace.component';
import { ContentComponent } from './content/content.component';
import { TableComponent } from './table/table.component';
import { ClusterComponent } from './cluster/cluster.component';
import { LogComponent } from './log/log.component';
import { ImplementationComponent } from './implementation/implementation.component';
import { HighlightService } from './_service/highlight.service';
import { DatabaseComponent } from './database/database.component';
import { CacheComponent } from './cache/cache.component';

import {NgxWebstorageModule} from 'ngx-webstorage';

import { TreeviewModule } from 'ngx-treeview';
import { ViewerComponent } from './viewer/viewer.component';

import { NgxSmartModalModule } from "ngx-smart-modal";
import { NgxGraphModule } from "@swimlane/ngx-graph";
import { NgxChartsModule } from "@swimlane/ngx-charts";

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { SearchComponent } from './search/search.component';

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

const monacoConfig: NgxMonacoEditorConfig = {
  baseUrl: 'assets', // configure base path for monaco editor default: './assets'
  defaultOptions: { scrollBeyondLastLine: false }, // pass default options to be used
  onMonacoLoad: configureMonaco
};

@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,
    FooterComponent,
    EditorComponent,
    HomeComponent,
    LoginComponent,
    ScriptsComponent,
    ActionsComponent,
    WorkspaceComponent,
    ContentComponent,
    TableComponent,
    ClusterComponent,
    LogComponent,
    ImplementationComponent,
    DatabaseComponent,
    CacheComponent,
    ViewerComponent,
    SearchComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    ReactiveFormsModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule,
    MonacoEditorModule.forRoot(monacoConfig), // use forRoot() in main app module only.
    MarkdownModule.forRoot(),
    NgxPaginationModule,
    NgxDatatableModule,
    NgxWebstorageModule.forRoot(),
    TreeviewModule.forRoot(),
    NgxGraphModule,
    NgxChartsModule,
    NgxSmartModalModule.forChild()
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
    HighlightService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
