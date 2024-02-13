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

import { Injectable } from '@angular/core';
import { gql } from 'apollo-angular'

export class ObservationQuery {
    executionId: string
    systemId: string
    type: string
}

export class ObservationRecord {
    x: number
    y: number
    value: string
    type: string

    executionId: string
    systemId: string
    adapter: string
}

@Injectable({
  providedIn: 'root'
})
export class LassoGraphService {

  constructor() { }
    sayHello = gql`
query sayHello($name: String!) {
    sayHello(name: $name) {
        text
    }
}
`

observationRecords = gql`
query observationRecords($q: ObservationQuery!) {
    observationRecords(q: $q) {
        x
        y
        value
        type
        adapter
    }
}
`

getRecordTypes = gql`
query getRecordTypes($q: ObservationQuery!) {
    getRecordTypes(q: $q)
}
`
}