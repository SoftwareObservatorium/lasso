import React, { useEffect, useState } from 'react';
import Sheet from '../components/sheet/Sheet';
import LQLEditor from '../components/editor/LQLEditor';
import { Alert, Box, CircularProgress, TextField } from '@mui/material';
import { CellBase, Matrix } from 'react-spreadsheet';
import ClassUnderTest from '../components/cut/ClassUnderTest';
import { ClassUnderTestSpec, SheetRequest, SheetResponse, SheetSpec } from '../model/models';
import SheetService from '../services/SheetService';


const lqlCode =
  `Stack {
    push(java.lang.String)->java.lang.String
    size()->int
}`

function loadSheet() {
  // FIXME load remotely
  const jsonl = `
{"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Stack"}}
{"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "$eval", "C2": "Arrays.toString(new char[]{'a', 'b'})"}}
{"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": {}, "B3": "push", "C3": "A1", "D3": "A2"}}
{"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": 1, "B4": "size", "C4": "A1"}}
`

  return loadSheetJsonl(jsonl)
}

function loadSheetJsonl(jsonl: any) {
  let sheetData: any[][] = []
  for (const line of jsonl.trim().split(/[\r\n]+/)) {
    console.log(line);
    const row = JSON.parse(line + "")
    //sheetData.push({value: row.})

    const cols: any[] = Object.keys(row.cells).sort().map(key => {
      console.log(`Property: ${key}, Value: ${row.cells[key]}`);

      return { value: row.cells[key], readOnly: false, className: "text-danger" }
    });

    sheetData.push(cols)
  }

  // make certain rows larger if necessary
  const maxLength = findLargestRow(sheetData)
  sheetData = sheetData.map((row) => {
    const nextRow = [...row];
    if (nextRow.length < maxLength) {
      let diff = maxLength - nextRow.length
      nextRow.length += diff;

      console.log(nextRow.length)
    }
    return nextRow;
  })

  return sheetData
}

function findLargestRow(sheetData: any[][]): number {
  let length = -1
  for (let i = 0; i < sheetData.length; i++) {
    const currentLength = sheetData[i].length

    if (currentLength > length) {
      length = currentLength
    }
  }

  return length
}

function toSheetJSONL(data: Matrix<CellBase<any>>) {
  // FIXME indices to spreadsheet coordinates
  const rows = []
  for (let i = 0; i < data.length; i++) {
    let cols: any = {}
    for (let j = 0; j < data[i].length; j++) {
      console.log(`${columnIndexToLabel(j)}${rowIndexToLabel(i)}`);

      let p: string = `${columnIndexToLabel(j)}${rowIndexToLabel(i)}`
      if (data[i][j]?.value) {
        cols[p] = data[i][j]?.value
      }
    }

    rows.push({ sheet: "Sheet 1", header: `Row ${i + 1}`, cells: cols })
  }

  let jsonl = rows.map((row) => { return JSON.stringify(row) }).join('\n')

  console.log(jsonl)

  return jsonl
}

// copied from react-spreadsheet
function columnIndexToLabel(column: number): string {
  let label = "";
  let index = column;
  while (index >= 0) {
    label = String.fromCharCode(65 + (index % 26)) + label;
    index = Math.floor(index / 26) - 1;
  }
  return label;
}

function rowIndexToLabel(row: number) {
  return row + 1;
}


function SheetEditorPage() {
  // load
  const sheetData = loadSheet()

  const [classUnderTestSpec, setClassUnderTestSpec] = useState<ClassUnderTestSpec>(new ClassUnderTestSpec());
  const [interfaceSpecification, setInterfaceSpecification] = useState<string>("");

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const [sheetResponse, setSheetResponse] = useState<SheetResponse>();
  const [sheetResponseUpdate, setSheetResponseUpdate] = useState(0);

  // execute sheet
  const executeHandler = (sheetName: string, sheetData: Matrix<CellBase<any>>) => {
    console.log("executed sheet '" + sheetName + "' data: " + sheetData)
    console.log("lql handler " + interfaceSpecification)
    console.log("cut handler " + JSON.stringify(classUnderTestSpec))

    const request = new SheetRequest()
    request.classUnderTest = classUnderTestSpec
    request.sheets = []

    const sheet = new SheetSpec()
    sheet.name = sheetName
    sheet.interfaceSpecification = interfaceSpecification
    const bodyJsonl = toSheetJSONL(sheetData)
    sheet.body = bodyJsonl

    request.sheets.push(sheet)

    console.log(JSON.stringify(request))

    setMessage("");
    setLoading(true);

    const valid: boolean = true

    if (valid) {
      SheetService.executeSheet(request).then(
        (response) => {
          //navigate("/profile");
          //window.location.reload();

          // FIXME show results
          console.log(`response ${JSON.stringify(response.data)}`)

          setSheetResponse(response.data)

          // ugly hack to re-render actuation sheets
          setSheetResponseUpdate(sheetResponseUpdate+1)

          setLoading(false);
        },
        (error) => {
          const resMessage =
            (error.response &&
              error.response.data &&
              error.response.data.message) ||
            error.message ||
            error.toString();

          setLoading(false);
          setMessage(resMessage);
        }
      );
    } else {
      setLoading(false);
    }
  }

  // get LQL
  const lqlHandler = (lql: string) => {
    console.log("lql handler " + lql)

    setInterfaceSpecification(lql)
  }

  // get CUT
  const cutHandler = (className: string, artifacts: string[]) => {
    console.log("cut handler " + className + " " + typeof (artifacts))

    classUnderTestSpec.className = className
    classUnderTestSpec.artifacts = artifacts
  }

  const parseActuationSheet = (sheet: SheetSpec) => {
    const matrix = loadSheetJsonl(sheet?.body)

    return matrix
  }

  const parseAdaptedActuationSheet = (sheet: SheetSpec) => {
    const matrix = loadSheetJsonl(sheet?.body)

    return matrix
  }

  useEffect(() => {
    console.log("render")
  }, []);

  return (
    <div className="sheet">

      <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
        <LQLEditor lqlHandler={lqlHandler} defaultLqlCode={lqlCode} />
      </Box>

      <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
        <ClassUnderTest cutHandler={cutHandler} />
      </Box>

      <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
        {loading && (
          <CircularProgress />
        )}
        <Sheet defaultSheetName="test1" sheetData={sheetData} executeHandler={executeHandler} />
      </Box>

      {message && (
        <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
          <Alert severity="error">{message}</Alert>
        </Box>
      )}


      {sheetResponse && (
        <>
          <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
            <Alert severity="success">Actuation Sheet</Alert>
            {sheetResponse.actuationSheets.map((sheet) => (
              <Sheet key={sheetResponseUpdate} isResult={true} defaultSheetName={sheet.name} sheetData={() => parseActuationSheet(sheet)} executeHandler={() => console.log("not implemented")} />
            ))
            }
          </Box>
          <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
            <Alert severity="success">Adapted Actuation Sheet</Alert>
            {sheetResponse.adaptedActuationSheets.map((sheet) => (
              <Sheet key={sheetResponseUpdate} isResult={true} defaultSheetName={sheet.name} sheetData={() => parseAdaptedActuationSheet(sheet)} executeHandler={() => console.log("not implemented")} />
            ))
            }
          </Box>
        </>
      )}

    </div>
  );
}

export default SheetEditorPage;
