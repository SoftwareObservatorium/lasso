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
import { RouterModule, Routes } from '@angular/router';
import { QueryComponent } from './query/query.component';
import { ResultsComponent } from './results/results.component';
import { LoginComponent } from './login/login.component';
import { AuthGuard } from './service/auth.guard';
import { ScriptsComponent } from './scripts/scripts.component';
import { SrmComponent } from './srm/srm.component';
import { ActionsComponent } from './actions/actions.component';
import { HomeComponent } from './home/home.component';
import { WorkspaceComponent } from './workspace/workspace.component';
import { GridComponent } from './grid/grid.component';
import { LogsComponent } from './logs/logs.component';
import { DfsComponent } from './dfs/dfs.component';
import { ProfileComponent } from './profile/profile.component';
import { DatasourcesComponent } from './datasources/datasources.component';
import { CodeComponent } from './code/code.component';
import { DbComponent } from './db/db.component';

const routes: Routes = [
  {path: '', component: HomeComponent},
  {path: 'login', component: LoginComponent },
  {path: 'submit', component: QueryComponent, canActivate: [AuthGuard] },
  {path: 'submit/:executionId', component: QueryComponent, canActivate: [AuthGuard] },
  {path: 'code', component: CodeComponent, canActivate: [AuthGuard] },
  {path: 'scripts', component: ScriptsComponent, canActivate: [AuthGuard] },
  {path: 'search', component: ResultsComponent, canActivate: [AuthGuard] },
  {path: 'results/:executionId', component: ResultsComponent, canActivate: [AuthGuard] },
  {path: 'db/:executionId', component: DbComponent, canActivate: [AuthGuard] },
  {path: 'srm/:executionId', component: SrmComponent, canActivate: [AuthGuard] },
  {path: 'workspace/:executionId', component: WorkspaceComponent, canActivate: [AuthGuard] },

  {path: 'actions', component: ActionsComponent, canActivate: [AuthGuard] },
  {path: 'datasources', component: DatasourcesComponent, canActivate: [AuthGuard] },

  {path: 'grid/overview', component: GridComponent, canActivate: [AuthGuard] },
  {path: 'grid/dfs', component: DfsComponent, canActivate: [AuthGuard] },
  {path: 'grid/dfs/:executionId', component: DfsComponent, canActivate: [AuthGuard] },
  {path: 'grid/logs', component: LogsComponent, canActivate: [AuthGuard] },

  {path: 'user/profile', component: ProfileComponent, canActivate: [AuthGuard] },

  // otherwise redirect to home
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
