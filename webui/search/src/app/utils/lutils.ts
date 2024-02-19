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

import { Injectable } from "@angular/core"

@Injectable({
    providedIn: 'root'
  })
export class LassoUtils {

    public static getColumnLabel(col: string): string {
        if (col === "oracle_oracle") {
            return "Manual Oracle"
        }

        if(col.indexOf('_') > -1) {
            let parts = col.split('_')
            return parts[0] + " (Adapter " + parts[1] + ")"
        }

        return col
    }
}