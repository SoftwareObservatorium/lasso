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
import { Routes, RouterModule } from '@angular/router';

import { HomeComponent } from './home/home.component';
import { EditorComponent } from './editor/editor.component';

import { AuthGuard } from './_service/auth.guard';
import { LoginComponent } from './login/login.component';
import { ScriptsComponent } from './scripts/scripts.component';
import { ActionsComponent } from './actions/actions.component';
import { WorkspaceComponent } from './workspace/workspace.component';
import { ContentComponent } from './content/content.component';
import { TableComponent } from './table/table.component';
import { ClusterComponent } from './cluster/cluster.component';
import { LogComponent } from './log/log.component';
import { ImplementationComponent } from './implementation/implementation.component';
import { DatabaseComponent } from './database/database.component';
import { CacheComponent } from './cache/cache.component';
import { ViewerComponent } from './viewer/viewer.component';
import { SearchComponent } from './search/search.component';

const routes: Routes = [
  // {path:  "", pathMatch:  "full",redirectTo:  "home", canActivate: [AuthGuard]},
  // {path: "home", component: HomeComponent},
  // {path: "editor", component: EditorComponent},
  // {path: "login", component: LoginComponent }

  {path: '', component: HomeComponent, canActivate: [AuthGuard] },
  {path: 'login', component: LoginComponent },
  {path: 'editor', component: EditorComponent, canActivate: [AuthGuard]},
  {path: 'search', component: SearchComponent, canActivate: [AuthGuard]},
  {path: 'search/:executionId', component: SearchComponent, canActivate: [AuthGuard]},
  {path: 'editor/:executionId', component: EditorComponent, canActivate: [AuthGuard]},
  {path: 'scripts', component: ScriptsComponent, canActivate: [AuthGuard]},
  {path: 'actions', component: ActionsComponent, canActivate: [AuthGuard]},
  {path: 'datasource/:dataSourceId/:implementationId', component: ImplementationComponent, canActivate: [AuthGuard]},
  {path: 'database/:executionId', component: DatabaseComponent, canActivate: [AuthGuard]},
  {path: 'cache/:executionId/:reportId/:actionId/:abstractionId/:implementationId/:dataSourceId/:permId', component: CacheComponent, canActivate: [AuthGuard]},
  {path: 'cluster', component: ClusterComponent, canActivate: [AuthGuard]},
  {path: 'log', component: LogComponent, canActivate: [AuthGuard]},
  {path: 'workspace/:executionId', component: WorkspaceComponent, canActivate: [AuthGuard]},
  {path: 'table/:executionId/:csvPath', component: TableComponent, canActivate: [AuthGuard]},
  {path: 'viewer/:executionId/:filePath', component: ViewerComponent, canActivate: [AuthGuard]},
  {path: 'content/:contentId', component: ContentComponent, canActivate: [AuthGuard]},

  // otherwise redirect to home
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
