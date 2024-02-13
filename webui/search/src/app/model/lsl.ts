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

export class LslRequest {
    script: string;
    email: string;
    share: boolean;
    type: string;
}

export class LslResponse {
    script: string;
    executionId: string;
    status: string;
}

export class ScriptInfo {
    executionId: string;
    name: string;
    status: string;
    start: Date;
    end: Date;
    content: string;
    owner: string;
}

export class LSLInfoResponse {
    actions: Map<string, ActionInfo>;
}

export class ExecutionStatus {
    executionId: string;
    status: string;
    start: Date;
}


export class ActionInfo {
    state: string;
    type: string;
    description: string;
    distributable: boolean;
    configuration: Map<string, ActionConfigurationInfo>;
}

export class ActionConfigurationInfo {
    type: string;
    description: string;
    optional: boolean;
}

export class RecordsRequest {
    filePatterns: string[];
}

export class RecordsResponse {
    files: string[];
}

export class ImplementationRequest {
    ids: string[];
}

export class ImplementationResponse {
    implementations: Map<string, Object>;
}

export class ReportRequest {
    sql: string;
}

export class FileViewRequest {
    filePatterns: string[];
}

export class FileViewResponse {
    root: any;
}

export class DataSourceResponse {
    dataSources: Map<string, any>;
}

export class SearchQueryRequest {
    query: string
    filters: string[]
    sortyBy: string[]

    oracleFilters: any;

    strategy: string

    start: number
    rows: number

    executionId: string

    forAction: string
}

export class SearchQueryResponse {
    implementations: Map<string, Object>;

    total: number
    rows: number

    actions: string[]
}