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

import { Injectable } from "@angular/core";
import { EditorComponent } from "./editor.component";
import { Node, Edge } from "@swimlane/ngx-graph";
import { LassoApiServiceService } from '@app/_service/lasso-api-service.service';

import * as monaco from 'monaco-editor'

@Injectable({
  providedIn: "root",
})
export class EditorService {
  //Monaco editor instance
  public editor: any;
  //Actual editor Component
  public editorComponent: EditorComponent;
  //All existing types
  public types = [];

  constructor() {}

  init(types) {
    this.types = types;
  }

  registerProviders() {
    //#region Error Markers
    this.editor.getModel(null).onDidChangeContent((event) => {
      const actionNameErrors = this.getActionNameErrors();
      const typeErrors = this.getTypeErrors();
      monaco.editor.setModelMarkers(
        this.editor.getModel(null),
        "ActionName",
        actionNameErrors
      );
      monaco.editor.setModelMarkers(
        this.editor.getModel(null),
        "Types",
        typeErrors
      );
    });
    //#endregion

    //#region  Completion/Suggestions
    (<any>window).monaco.languages.registerCompletionItemProvider("lsl", {
      provideCompletionItems: (model, position) => {
        console.log("completion");
        //DependsOnProposals
        this.createNodes();

        let textUntilPosition = model.getValueInRange({
          startLineNumber: 1,
          startColumn: 1,
          endLineNumber: position.lineNumber,
          endColumn: position.column,
        });
        let dependsMatch = textUntilPosition.match(
          /action\s*\(\s*name\s*:\s*'\s*.*\s*'\s*,\s*type\s*:\s*'\s*.*\s*'\s*\)\s*(.*\s*{{1}\s*|\s+)[^°]*?(?=dependsOn\s*('\s*'|'.*'|\s*))dependsOn\s*('\s*|\s*)$/
        );
        let typeMatch = textUntilPosition.match(
          /action\s*\(\s*name\s*:\s*'\s*.*\s*'\s*,\s*type\s*:\s*(('\s*'|\s*){0,1})?$/
        );
        let abstractionMatch = textUntilPosition.match(
          /action\s*\(\s*name\s*:\s*'\s*.*\s*'\s*,\s*type\s*:\s*'\s*.*\s*'\s*\)\s*(.*\s*{{1}\s*)[^.]*includeAbstractions\s*(('\s*|\s*){0,1})?$/g
        );

        console.log(`${dependsMatch} ${typeMatch} ${abstractionMatch}`)

        if (!dependsMatch && !typeMatch && !abstractionMatch) {
          return { suggestions: [] };
        }
        let word = model.getWordUntilPosition(position);
        let range = {
          startLineNumber: position.lineNumber,
          endLineNumber: position.lineNumber,
          startColumn: word.startColumn,
          endColumn: word.endColumn,
        };
        if (dependsMatch && !typeMatch && !abstractionMatch) {
          return {
            suggestions: this.filterActionNames(range, dependsMatch),
          };
        }
        if (typeMatch && !dependsMatch && !abstractionMatch) {
          return {
            suggestions: this.getAllTypes(range, typeMatch),
          };
        }
        if (!typeMatch && !dependsMatch && abstractionMatch) {
          return {
            suggestions: this.getAbstractionsList(range),
          };
        }
      },
    });
    //#endregion

    //#region Hover
    (<any>window).monaco.languages.registerHoverProvider("lsl", {
      provideHover: (model, position) => {
        console.log("hover");
        //DependsOnProposals
        this.createNodes();

        let actualLine: string = model.getLineContent(position.lineNumber);
        if (
          actualLine.match(
            /^(.*\s*{{1}\s*|\s+|^)action\s*\(\s*name\s*:\s*'\s*.*\s*'\s*,\s*type\s*:\s*'\s*.*\s*'\s*\).*$/
          )
        ) {
          //cut everything before and after action definition
          actualLine = actualLine
            .replace(/.*action/, "action")
            .replace(
              actualLine.split(
                /action\s*\(\s*name\s*:\s*'\s*.*\s*'\s*,\s*type\s*:\s*'\s*.*\s*'\s*\)/
              )[1],
              ""
            );
          return {
            contents: [
              {
                value:
                  "```lsl\naction" +
                  actualLine.replace(/action|\s|{/g, "") +
                  "\n```\n",
              },
              {
                value: this.createDependencyList(
                  this.getActionName(actualLine)
                ),
              },
            ],
            range: {
              startLineNumber: position.lineNumber,
              endLineNumber: position.lineNumber,
              startColumn: position.startColumn,
              endColumn: position.endColumn,
            },
          };
        }
      },
    });
    //#endregion
  }

  /**
   * Function to filter all action names except actual actions name. Used to show action name suggestion list
   * @param range
   * @param actualAction The action which 'calls' suggestions at dependsOn
   */
  filterActionNames(range: any, actualAction: any) {
    const matchList = this.editor
      .getModel(null)
      .getValue()
      .match(
        /action\s*\(\s*name\s*:\s*'\s*\S+\s*'\s*,\s*type\s*:\s*'\s*.*\s*'\s*\)/g
      );
    let result = [];
    for (const match of matchList) {
      if (this.getActionName(match) !== this.getActionName(actualAction[0])) {
        result.push({
          label: this.getActionName(match),
          kind: (<any>window).monaco.languages.CompletionItemKind.Function,
          insertText:
            actualAction[0].charAt(actualAction[0].length - 1) !== "'"
              ? "'" + this.getActionName(match) + "'"
              : this.getActionName(match),
          range: range,
        });
      }
    }
    return result;
  }

  /**
   * Returns a list of all possible Types for suggestions
   * @param range
   * @param typeMatch The action header until type definition
   */
  getAllTypes(range: any, typeMatch: any) {
    let result = [];
    const actualType = typeMatch[0].match(/type\s*:\s*(('\s*'|\s*){0,1})?.*$/g);
    for (const type of this.types) {
      result.push({
        label: type,
        kind: (<any>window).monaco.languages.CompletionItemKind.Function,
        insertText:
          actualType[0].charAt(actualType[0].length - 1) !== "'"
            ? "'" + type + "'"
            : type,
        range: range,
      });
    }
    return result;
  }

  /**
   * Replaces all unnecessary symbols, returns plain action name
   * @param match The returned action header until name definition
   */
  getActionName(match: any) {
    if (match) {
      return match
        .match(/action\s*\(\s*name\s*:\s*'\s*.*\s*'\s*,/)
        .toString()
        .replace(/action\s*\(|name:|\s|,|'/g, "");
    }
  }

  /**
   * Returns plain type name
   * @param match Actual action header
   */
  getActionType(match: any) {
    return match.split(",")[1].replace(/\s*type\s*:|\s|\)|{|'/g, "");
  }

  /**
   * Returns plain value of dependsOn key
   * @param match actual dependsOn key
   */
  getDependency(match: any) {
    return match
      .match(/\s+dependsOn\s* (('\s*'|'.*'|\s*){0,1})?/g)[0]
      .replace(/\s*dependsOn\s*|\s|'/g, "");
  }

  /**
   * Creates A list of nodes including relevant information to create dependency graph
   */
  extractDependencies() {
    let textUntilPosition = this.editor.getModel(null).getValueInRange({
      startLineNumber: 1,
      startColumn: 1,
      endLineNumber: this.editor.getModel(null).getLineCount(),
      endColumn:
        this.editor
          .getModel(null)
          .getLineContent(this.editor.getModel(null).getLineCount()).length + 1,
    });
    let match = textUntilPosition.match(
      /action\s*\(\s*name\s*:\s*'\s*.*\s*'\s*,\s*type\s*:\s*'\s*.*\s*'\s*\)\s*(.*\s*{{1}\s*|\s+)[^°]*?(?=dependsOn\s*('\s*'|'.*'|\s*))dependsOn\s*('\s*'|'.*'|\s*)/gm
    );
    const nodes = [];
    for (const res of match) {
      nodes.push({
        name: this.getActionName(res),
        dependsOn:
          this.getDependency(res) !== "" ? this.getDependency(res) : null,
      });
    }
    return nodes;
  }

  /**
   * Creates a Dependency list for action Header Hover
   * @param action actual action
   */
  createDependencyList(action: any) {
    const nodes = this.extractDependencies();
    let dependencies = "Actions depending on " + action + ": ";
    for (let i = 0; i < nodes.length; i++) {
      if (nodes[i].dependsOn) {
        if (nodes[i].dependsOn === action) {
          dependencies += nodes[i].name;
          dependencies += i !== nodes.length - 1 ? ", " : ".";
        }
      }
    }
    if (dependencies !== "Actions depending on " + action + ": ") {
      return dependencies;
    }
    return "No depending Actions.";
  }

  /**
   * Creates Nodes and Edges for Dependency graph
   */
  createNodes() {
    const nodeList = this.extractDependencies();
    const nodes = [];
    const edges = [];
    let incID = 1;
    for (const node of nodeList) {
      const n: Node = {
        id: node.name,
        label: node.name,
      };
      nodes.push(n);
      if (node.dependsOn) {
        const edge: Edge = {
          id: node.name + incID.toString(),
          source: node.name,
          target: node.dependsOn,
          label: "Depends on",
        };
        edges.push(edge);
        incID += 1;
      }
    }
    this.editorComponent.nodes = nodes;
    this.editorComponent.edges = edges;
    this.editorComponent.nodes = [...this.editorComponent.nodes];
    this.editorComponent.edges = [...this.editorComponent.edges];
  }

  /**
   * Returns all Actionnames to check if duplicate names are the case
   */
  getAllActionNames() {
    let actions = [];
    const actionMatches = this.editor
      .getModel(null)
      .findMatches(
        /^(.*\s*{{1}\s*|\s+|^)action\s*\(\s*name\s*:\s*'\s*\S+\s*'\s*,\s*type\s*:\s*'.*'\s*\).*$/gm,
        true,
        true,
        true,
        null,
        true
      );
    let nameMatches = this.editor
      .getModel(null)
      .findMatches(/name\s*:\s*'\s*\S+\s*'\s*,/, true, true, true, null, true);
    if (nameMatches) {
      for (const nameMatch of nameMatches) {
        if (
          actionMatches.filter((actionMatch) => {
            return actionMatch.range.containsRange(nameMatch.range);
          }).length < 1
        ) {
          nameMatches = nameMatches.filter((x) => x !== nameMatch);
        }
      }
    }
    if (actionMatches)
      for (const match of actionMatches) {
        actions.push(this.getActionName(match.matches[0]));
      }
    return [actions, nameMatches];
  }
  /**
   * Returns fitting Objects to mark duplicate action name errors
   */
  getActionNameErrors() {
    let duplicates = [];
    let errors = [];
    if (this.getAllActionNames()[0]) {
      duplicates = this.getAllActionNames()[0].filter(
        (elem, index) => this.getAllActionNames()[0].indexOf(elem) !== index
      );
    }
    for (let i = 0; i < this.getAllActionNames()[1].length; i++) {
      if (
        duplicates.indexOf(
          this.getAllActionNames()[1][i].matches[0].replace(/name:|\s|,|'/g, "")
        ) > -1
      ) {
        errors.push(this.getAllActionNames()[1][i]);
        errors.push({
          startLineNumber: this.getAllActionNames()[1][i].range.startLineNumber,
          startColumn: this.getAllActionNames()[1][i].range.startColumn,
          endLineNumber: this.getAllActionNames()[1][i].range.endLineNumber,
          endColumn: this.getAllActionNames()[1][i].range.endColumn,
          message:
            "The name '" +
            this.getAllActionNames()[1][i].matches[0].replace(
              /name:|\s|,|'/g,
              ""
            ) +
            "' already exists!",
          severity: monaco.MarkerSeverity.Error,
        });
      }
    }
    return errors;
  }
  /**
   * Returns Error objects if an type is not existing
   */
  getTypeErrors() {
    const actionMatches = this.editor
      .getModel(null)
      .findMatches(
        /action\s*\(\s*name\s*:\s*'\s*.*\s*'\s*,\s*type\s*:\s*'\s*.*\s*'\)/g,
        true,
        true,
        true,
        null,
        true
      );
    let typeMatches = this.editor
      .getModel(null)
      .findMatches(
        /type(\s*):\s*'\s*.*\s*'\s*\)/,
        true,
        true,
        true,
        null,
        true
      );
    if (typeMatches) {
      for (const typeMatch of typeMatches) {
        if (
          actionMatches.filter((actionMatch) => {
            return actionMatch.range.containsRange(typeMatch.range);
          }).length < 1
        ) {
          typeMatches = typeMatches.filter((x) => x !== typeMatch);
        }
      }
    }
    let typeErrors = [];
    for (const type of typeMatches) {
      if (
        this.types.indexOf(
          type.matches[0].replace(/\s*type\s*:|\s|'|\)/g, "")
        ) < 0
      ) {
        typeErrors.push({
          startLineNumber: type.range.startLineNumber,
          startColumn: type.range.startColumn,
          endLineNumber: type.range.endLineNumber,
          endColumn: type.range.endColumn,
          message:
            "The type '" +
            type.matches[0].replace(/\s*type\s*:|\s|'|\)/g, "") +
            "' does not exist!",
          severity: monaco.MarkerSeverity.Error,
        });
      }
    }
    return typeErrors;
  }
  /**
   * Returns all abstraction definitions and their position
   */
  getAbstractionsList(range: any) {
    let result = [];
    let abstractionMatches = this.editor
      .getModel(null)
      .getValue()
      .match(/abstraction\s*\(\s*'\s*\S+\s*'\s*\)/g);
    if (abstractionMatches) {
      for (const abstraction of abstractionMatches) {
        result.push({
          label: abstraction.replace(/\s*abstraction\s*|:|\s|'|\)|\(/g, ""),
          kind: (<any>window).monaco.languages.CompletionItemKind.Function,
          insertText: abstraction.replace(
            /\s*abstraction\s*|:|\s|'|\)|\(/g,
            ""
          ),
          range: range,
        });
      }
    }
    return result;
  }
}
