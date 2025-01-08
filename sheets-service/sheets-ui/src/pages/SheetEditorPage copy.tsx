import React, { useEffect, useRef, useState } from 'react';
import LQLEditor from '../components/editor/LQLEditor';
import { Alert, Backdrop, Box, Button, ButtonGroup, CircularProgress, Container, Divider, TextField, ToggleButton, ToggleButtonGroup } from '@mui/material';
import { CellBase, Matrix } from 'react-spreadsheet';
import ClassUnderTest from '../components/cut/ClassUnderTest';
import { ClassUnderTestSpec, SheetRequest, SheetResponse, SheetSpec, StimulusSheet, TestResult } from '../model/models';
import SheetService from '../services/SheetService';
import ActuationSheet from '../components/sheet/ActuationSheet';
import Sheet from '../components/sheet/StimulusSheet';

const lqlCode =
`BoundedQueue {
    BoundedQueue(int)
    enQueue(java.lang.Object)->void
    deQueue()->java.lang.Object
    isEmpty()->boolean
    isFull()->boolean
}`

function loadDefaultSheet() {
  console.log("Load default sheet")

  // FIXME load remotely
  const jsonl = `
{"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "BoundedQueue", "D1": 10}}
{"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "enQueue", "C2": "A1", "D2": "'Hello World!'"}}
{"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": {}, "B3": "isEmpty", "C3": "A1"}}
{"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": {}, "B4": "isFull", "C4": "A1"}}
{"sheet": "Sheet 1", "header": "Row 5", "cells": {"A5": "D2", "B5": "deQueue", "C5": "A1"}}
{"sheet": "Sheet 1", "header": "Row 6", "cells": {"A6": {}, "B6": "isEmpty", "C6": "A1"}}
`

  const sheet: StimulusSheet = new StimulusSheet()
  sheet.signature = "test1()"
  sheet.data = loadSheetJsonl(jsonl)

  return sheet
}

function loadSheetJsonl(jsonl: any) {
  const sheetData: any[][] = []
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
  const matrix = sheetData.map((row) => {
    const nextRow = [...row];
    if (nextRow.length < maxLength) {
      let diff = maxLength - nextRow.length
      nextRow.length += diff;

      console.log(nextRow.length)
    }
    return nextRow;
  })

  return matrix
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
  const [stimulusSheets, setStimulusSheets] = useState<StimulusSheet[]>(() => [loadDefaultSheet()])

  const [classUnderTestSpec, setClassUnderTestSpec] = useState<ClassUnderTestSpec>(new ClassUnderTestSpec());
  const [interfaceSpecification, setInterfaceSpecification] = useState<string>("");

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const [sheetResponse, setSheetResponse] = useState<SheetResponse>();
  const [sheetResponseUpdate, setSheetResponseUpdate] = useState(0);

  const [codeAnalyzers, setCodeAnalyzers] = React.useState<string[]>([]);

  const lqlEditorRef = useRef<any>(null)

  const handleCodeAnalyzer = (
    event: React.MouseEvent<HTMLElement>,
    analyzers: string[],
  ) => {
    setCodeAnalyzers(analyzers);
  };

  const addStimulusSheet = () => {
    const stimulusSheet: StimulusSheet = new StimulusSheet()
    stimulusSheet.signature = `test${stimulusSheets.length + 1}()`

    // create same dimensions based on existing
    const sampleSheet = stimulusSheets[0]
    stimulusSheet.data = [...sampleSheet.data]

    setStimulusSheets([...stimulusSheets, stimulusSheet]);
  }

  const stimulusSheetChangeHandler = (sheetId: number, sheetSignature: string, sheetData: Matrix<CellBase<any>>, sheetInvocations: string[]) => {
    console.log("changed " + sheetId)

    const nStimulusSheets = [...stimulusSheets];
    const stimulusSheet: StimulusSheet = new StimulusSheet()
    stimulusSheet.signature = sheetSignature
    stimulusSheet.data = sheetData
    stimulusSheet.invocations = sheetInvocations
    nStimulusSheets[sheetId] = stimulusSheet

    setStimulusSheets(nStimulusSheets);
  };

  // execute sheet
  const executeAllHandler = () => {
      //console.log("executed sheet '" + sheetSignature + "' data: " + sheetData)
      console.log("lql handler " + interfaceSpecification)
      console.log("cut handler " + JSON.stringify(classUnderTestSpec))
  
      const request = new SheetRequest()
      request.classesUnderTest = [classUnderTestSpec]
      request.sheets = []
      request.analyzers = codeAnalyzers

      console.log("total number of analyzers " + codeAnalyzers.length)

      console.log("total number of sheets " + stimulusSheets.length)

      stimulusSheets.forEach( (stimulusSheet) => {
        console.log("sheet " + stimulusSheet.signature)

        const sheet = new SheetSpec()
        sheet.signature = stimulusSheet.signature
        sheet.interfaceSpecification = interfaceSpecification
        const bodyJsonl = toSheetJSONL(stimulusSheet.data)
        sheet.body = bodyJsonl
        sheet.invocations = stimulusSheet.invocations
    
        request.sheets.push(sheet)
      });
  
      console.log(JSON.stringify(request))
  
      setMessage("");
      setLoading(true);
  
      const valid: boolean = true
  
      if (valid) {
        SheetService.executeSheet(request).then(
          (response) => {
            // FIXME show results
            console.log(`response ${JSON.stringify(response.data)}`)
            //console.log(`response ${JSON.stringify(response.data.testResults)}`)
  
            // ugly hack to re-render actuation sheets
            setSheetResponseUpdate(sheetResponseUpdate + 1)

            setSheetResponse(response.data)
  
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

  // toLQL handler
  const detectInterfaceHandler = (className: string, artifacts: string[]) => {
    console.log("detectInterfaceHandler " + JSON.stringify(classUnderTestSpec))

    const request = classUnderTestSpec

    console.log(JSON.stringify(request))

    setMessage("");
    setLoading(true);

    const valid: boolean = true

    if (valid) {
      SheetService.toLQL(request).then(
        (response) => {
          // update monaco LQL editor
          lqlEditorRef.current.getModel().setValue(response.data.interfaceSpecification);

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

  // get LQL editor (monaco)
  const lqlEditorHandler = (editor: any) => {
    //console.log("lql editor handler " + editor)

    lqlEditorRef.current = editor
  }

  // get CUT
  const cutHandler = (className: string, artifacts: string[]) => {
    //console.log("cut handler " + className + " " + typeof (artifacts))

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

  return (
    <Container maxWidth="xl">

      <h2>Sheet Editor</h2>

      <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
      <Divider>Interface Specification (LQL)</Divider>
        <LQLEditor editorHandler={lqlEditorHandler} lqlHandler={lqlHandler} defaultLqlCode={lqlCode} />
      </Box>

      <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
      <Divider>Class Under Test</Divider>
        <ClassUnderTest detectInterfaceHandler={detectInterfaceHandler} cutHandler={cutHandler} />
      </Box>

      <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
        {loading && (
          <Backdrop
            sx={(theme) => ({ color: '#fff', zIndex: theme.zIndex.drawer + 1 })}
            open={loading}
          >
            <CircularProgress color="inherit" />
          </Backdrop>
        )}

        <Divider>Stimulus Sheets</Divider>
        
        {stimulusSheets.map( (stimulusSheet, index) => (
          <Sheet sheetId={index} defaultSheetSignature={stimulusSheet.signature} sheetData={stimulusSheet.data} changeHandler={stimulusSheetChangeHandler} />
        ))}

        <Divider>Actions</Divider>

        <ToggleButtonGroup
          color="primary"
          value={codeAnalyzers}
          onChange={handleCodeAnalyzer}
          aria-label="Analyzers"
        >
          <ToggleButton value="cc" aria-label="bold">
            Code Coverage
          </ToggleButton>
          <ToggleButton value="mt" aria-label="italic">
            Mutation Testing
          </ToggleButton>
        </ToggleButtonGroup>

        <ButtonGroup variant="outlined" aria-label="Basic button group">
          <Button onClick={(event) => addStimulusSheet()}>Add Sheet</Button>
          {/* <Button onClick={(event) => loadExample()}>Load Example</Button> */}
          <Button onClick={(event) => executeAllHandler()}>Run Tests</Button>
        </ButtonGroup>
      </Box>

      {message && (
        <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
          <Alert severity="error">{message}</Alert>
        </Box>
      )}

    {sheetResponse?.testResults.map(testResult => (
      <>
                  <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
                  <Alert severity="success">Actuation Sheet (based on Interface Specification)</Alert>
                  {testResult.actuationSheets.map((sheet) => (
                    <ActuationSheet sheetSignature={sheet.signature} sheetData={parseActuationSheet(sheet)} />
                  ))
                  }
                </Box>
                <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
                  <Alert severity="success">Adapted Actuation Sheet (based on the Candidate's Class Interface)</Alert>
                  {testResult.adaptedActuationSheets.map((sheet) => (
                    <>
                    <h4>Implementation {sheet.implementation}</h4>
                      <ActuationSheet sheetSignature={sheet.signature} sheetData={parseAdaptedActuationSheet(sheet)} />
                    </>
                  ))
                  }
                </Box>
                <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
                  <Alert severity="success">Metric Sheet</Alert>
                  {testResult.metricSheets.map((sheet) => (
                    <>
                    <h4>Implementation {sheet.implementation}</h4>
                      <ActuationSheet sheetSignature={sheet.signature} sheetData={parseAdaptedActuationSheet(sheet)} />
                    </>
                  ))
                  }
                </Box>
                </>
    ))}

    </Container>
  );
}

export default SheetEditorPage;
