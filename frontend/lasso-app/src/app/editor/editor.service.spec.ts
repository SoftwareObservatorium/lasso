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

import { TestBed } from "@angular/core/testing";

import { EditorService } from "./editor.service";

describe("EditorService", () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it("should be created", () => {
    const service: EditorService = TestBed.get(EditorService);
    expect(service).toBeTruthy();
  });

  it("Should extract dependensOn", () => {
    const regex = RegExp(
      /action\s*\(\s*name\s*:\s*'\s*.*\s*'\s*,\s*type\s*:\s*'\s*.*\s*'\s*\)\s*(.*\s*{{1}\s*|\s+)[^°]*?(?=dependsOn\s*('\s*'|'.*'|\s*))dependsOn\s*('\s*|\s*)/
    );
    expect(
      regex.test(
        "    action(name:'selectSHA256', type:'Select') { dependsOn ''"
      )
    ).toBeTruthy();
    expect(
      regex.test("    action(name:'selectSHA256', type:'Select') { dependsOn ")
    ).toBeTruthy();
    expect(
      regex.test(
        "    action(name:'selectSHA256', type:'Select') { dependsOn ' "
      )
    ).toBeTruthy();
  });
  it("should extract action header until type:", () => {
    const regex = RegExp(
      /action\s*\(\s*name\s*:\s*'\s*.*\s*'\s*,\s*type\s*:\s*(('\s*'|\s*){0,1})?/
    );
    expect(regex.test("action(name:'selectSHA256', type:")).toBeTruthy();
    expect(regex.test("action(name:'selectSHA256', type:''")).toBeTruthy();
    expect(regex.test("action(name:'selectSHA256', type:'")).toBeTruthy();
  });

  it("should extract includeAbstraction", () => {
    const regex = RegExp(
      /action\s*\(\s*name\s*:\s*'\s*.*\s*'\s*,\s*type\s*:\s*'\s*.*\s*'\s*\)\s*(.*\s*{{1}\s*)[^.]*includeAbstractions\s*(('\s*|\s*){0,1})?/
    );
    expect(
      regex.test(
        "action(name:'selectSHA256', type:'Select') {dependsOn '' includeAbstractions '"
      )
    ).toBeTruthy();
    expect(
      regex.test(
        "action(name:'selectSHA256', type:'Select') {dependsOn '' includeAbstractions ''"
      )
    ).toBeTruthy();
    expect(
      regex.test(
        "action(name:'selectSHA256', type:'Select') {dependsOn '' includeAbstractions "
      )
    ).toBeTruthy();
  });

  it("should extract action header", () => {
    const regex = RegExp(
      /action\s*\(\s*name\s*:\s*'\s*.*\s*'\s*,\s*type\s*:\s*'\s*.*\s*'\s*\)/
    );
    expect(
      regex.test("action(name:'selectSHA256', type:'Select')")
    ).toBeTruthy();
    expect(regex.test("action(name:'', type:'')")).toBeTruthy();
    expect(regex.test("action(name:'selectSHA256', type:'')")).toBeTruthy();
    expect(regex.test("action(name:'', type:'Select')")).toBeTruthy();
  });

  it("should extract Abstraction definition", () => {
    const regex = RegExp(/abstraction\s*\(\s*'\s*\S+\s*'\s*\)/);
    expect(regex.test("abstraction('SHA256')")).toBeTruthy();
    expect(!regex.test("abstraction('')")).toBeTruthy();
    expect(regex.test("abstraction   ('SHA256')")).toBeTruthy();
    expect(regex.test("abstraction(   '  SHA256 ')")).toBeTruthy();
  });
});
