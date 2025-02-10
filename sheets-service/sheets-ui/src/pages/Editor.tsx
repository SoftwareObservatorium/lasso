import React, { useEffect, useRef, useState } from 'react';
import LQLEditor from '../components/editor/LQLEditor';
import { Accordion, AccordionActions, AccordionDetails, AccordionSummary, Alert, Backdrop, Badge, BadgeProps, Box, Button, ButtonGroup, Card, CardActions, CardContent, CircularProgress, Container, Dialog, DialogActions, DialogContent, DialogContentText, DialogProps, DialogTitle, Divider, FormControl, IconButton, InputLabel, Link, List, ListItem, ListItemIcon, ListItemText, MenuItem, Select, SelectChangeEvent, styled, Tabs, TextField, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material';
import { CellBase, Matrix } from 'react-spreadsheet';
import ClassUnderTest from '../components/cut/ClassUnderTest';
import { ClassUnderTestSpec, CodeGenerationRequest, CodeGenerationResponse, CodeModuleRaw, CodeSearchRequest, CodeSearchResponse, LqlGenerationRequest, LqlGenerationResponse, SheetGenerationRequest, SheetGenerationResponse, SheetRequest, SheetResponse, SheetSpec, StimulusMatrixRaw, StimulusSheet, TestResult } from '../model/models';
import SheetService from '../services/SheetService';
import ActuationSheet from '../components/sheet/ActuationSheet';
import Sheet from '../components/sheet/StimulusSheet';
import { Examples } from '../model/examples';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Tab from '@mui/material/Tab';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import GridOnIcon from '@mui/icons-material/GridOn';

import Grid from '@mui/material/Grid2';
import { DataGrid, GridColDef, GridColumnMenu, GridColumnMenuItemProps, GridColumnMenuProps, GridRowsProp, GridToolbar } from '@mui/x-data-grid';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function CustomTabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`simple-tabpanel-${index}`}
      aria-labelledby={`simple-tab-${index}`}
      {...other}
    >
      {value === index && <Box component="section" sx={{ p: 2, border: '1px grey' }}>{children}</Box>}
    </div>
  );
}

function a11yProps(index: number) {
  return {
    id: `simple-tab-${index}`,
    'aria-controls': `simple-tabpanel-${index}`,
  };
}

function loadDefaultSheets(stimulusMatrix: StimulusMatrixRaw) {
  console.log("Load default sheet");

  const sheets: StimulusSheet[] = stimulusMatrix.tests.map(sheetRaw => {
    const sheet: StimulusSheet = new StimulusSheet();
    sheet.signature = sheetRaw.signature;
    sheet.data = loadSheetJsonl(sheetRaw.body);
    sheet.invocations = sheetRaw.invocations;
    return sheet;
  })

  return sheets;
}

function loadSheetJsonl(jsonl: any) {
  const sheetData: any[][] = []
  for (const line of jsonl.trim().split(/[\r\n]+/)) {
    if (!line) {
      continue;
    }
    //console.log(line);
    const row = JSON.parse(line + "")
    //sheetData.push({value: row.})

    const cols: any[] = Object.keys(row.cells).sort().map(key => {
      //console.log(`Property: ${key}, Value: ${row.cells[key]}`);

      let myVal = row.cells[key];
      if (myVal.toString() == "[object Object]") {
        myVal = "";
      }

      //console.log(`myVal: ${myVal}`);

      return { value: myVal, readOnly: false, className: "text-danger" }
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

      //console.log(nextRow.length)
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
      //console.log(`${columnIndexToLabel(j)}${rowIndexToLabel(i)}`);

      let p: string = `${columnIndexToLabel(j)}${rowIndexToLabel(i)}`
      if (data[i][j]?.value) {
        cols[p] = data[i][j]?.value
      }
    }

    rows.push({ sheet: "Sheet 1", header: `Row ${i + 1}`, cells: cols })
  }

  let jsonl = rows.map((row) => { return JSON.stringify(row) }).join('\n')

  //console.log(jsonl)

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


function StimulusMatrixEditorPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  let exampleId: string | null = searchParams.get('example');
  console.log("example id: " + exampleId);
  if (!exampleId) {
    exampleId = "BOUNDED_QUEUE";
  }

  const example = Examples.MAP[exampleId as keyof typeof Examples.MAP];

  //
  const [stimulusMatrix, setStimulusMatrix] = useState<StimulusMatrixRaw>(example.scenario);

  // load
  const [stimulusSheets, setStimulusSheets] = useState<StimulusSheet[]>(() => loadDefaultSheets(stimulusMatrix))

  const [classesUnderTest, setClassesUnderTest] = useState<ClassUnderTestSpec[]>(stimulusMatrix.codeModules);

  const [interfaceSpecification, setInterfaceSpecification] = useState<string>("");

  const [faName, setFaName] = useState<string>(example.label);

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const [sheetResponse, setSheetResponse] = useState<SheetResponse>();
  const [sheetResponseUpdate, setSheetResponseUpdate] = useState(0);

  const [codeAnalyzers, setCodeAnalyzers] = React.useState<string[]>([]);

  const lqlEditorRef = useRef<any>(null)

  const [adaptationStrategy, setAdaptationStrategy] = useState<string>("PassThroughAdaptationStrategy");
  const [srmView, setSrmView] = useState<string>("testcases");
  const [srmAbstractionLevel, setSrmAbstractionLevel] = useState<string>("implementation");
  const [oracleVoting, setOracleVoting] = useState<string>("cluster");

  const [adapterLimit, setAdapterLimit] = useState<number>(1);

  const [codeSearchDataSource, setCodeSearchDataSource] = useState<string>("mavenCentral2023");
  const [codeSearchResultsLimit, setCodeSearchResultsLimit] = useState<number>(10);

  const [codeGenerationModel, setCodeGenerationModel] = useState<string>("llama3.1:latest");
  const [codeGenerationPrompt, setCodeGenerationPrompt] = useState<string>("");

  const [lqlGenerationModel, setLqlGenerationModel] = useState<string>("llama3.1:latest");
  const [lqlGenerationPrompt, setLqlGenerationPrompt] = useState<string>(`generate an interface specification for the functionality of ${faName}. return the interface specification in the format used by the following example:
\`\`\`lql
MyBoundedQueue {
    MyBoundedQueue(int)
    enQueue(java.lang.Object)->void
    deQueue()->java.lang.Object
    isEmpty()->boolean
    isFull()->boolean
}
\`\`\``);

  const [resultTab, setResultTab] = useState(0);

  const handleResultTabChange = (event: React.SyntheticEvent, newValue: string) => {
    setResultTab(parseInt(newValue));
  };

  const handleAdaptationStrategyChange = (event: SelectChangeEvent) => {
    setAdaptationStrategy(event.target.value);
  };

  const handleSrmViewChange = (event: SelectChangeEvent) => {
    setSrmView(event.target.value);
  };

  const handleSrmAbstractionLevelChange = (event: SelectChangeEvent) => {
    setSrmAbstractionLevel(event.target.value);
  };

  const handleOracleVotingChange = (event: SelectChangeEvent) => {
    setOracleVoting(event.target.value);
  };

  const handleAdapterLimitChange = (event: any) => {
    setAdapterLimit(parseInt(event.target.value));
  };

  const handleCodeSearchResultsLimit = (event: any) => {
    setCodeSearchResultsLimit(parseInt(event.target.value));
  };

  const handleCodeSearchDataSource = (event: any) => {
    setCodeSearchDataSource(event.target.value);
  };

  const handleCodeGenerationModel = (event: any) => {
    setCodeGenerationModel(event.target.value);
  };

  const handleCodeGenerationPrompt = (event: any) => {
    setCodeGenerationPrompt(event.target.value);
  };

  const handleLqlGenerationModel = (event: any) => {
    setLqlGenerationModel(event.target.value);
  };

  const handleLqlGenerationPrompt = (event: any) => {
    setLqlGenerationPrompt(event.target.value);
  };

  const handleFaName = (event: any) => {
    setFaName(event.target.value);
  };

  const handleCodeAnalyzer = (
    event: React.MouseEvent<HTMLElement>,
    analyzers: string[],
  ) => {
    setCodeAnalyzers(analyzers);
  };

  const addStimulusSheet = () => {
    const stimulusSheet: StimulusSheet = new StimulusSheet();
    stimulusSheet.signature = `test${stimulusSheets.length + 1}()`;
    stimulusSheet.invocations = [];

    // create same dimensions based on existing
    const sampleSheet = stimulusSheets[0];
    stimulusSheet.data = [...sampleSheet.data];

    setStimulusSheets([...stimulusSheets, stimulusSheet]);
  }

  const addClassUnderTest = () => {
    const classUnderTest: ClassUnderTestSpec = new ClassUnderTestSpec();
    classUnderTest.id = crypto.randomUUID();
    classUnderTest.className = "pkg.MyClass";
    classUnderTest.artifacts = [""];

    setClassesUnderTest([...classesUnderTest, classUnderTest]);
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
    console.log("cut handler " + JSON.stringify(classesUnderTest))

    const request = new SheetRequest()
    request.classesUnderTest = classesUnderTest
    request.sheets = []
    request.analyzers = codeAnalyzers

    console.log("total number of analyzers " + codeAnalyzers.length)

    request.adaptationStrategy = adaptationStrategy;
    request.adapterLimit = adapterLimit;

    console.log("adaptationStrategy " + request.adaptationStrategy);
    console.log("adapterLimit " + request.adapterLimit);

    console.log("total number of sheets " + stimulusSheets.length)

    stimulusSheets.forEach((stimulusSheet) => {
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
          //console.log(`response ${JSON.stringify(response.data)}`)
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
  const detectInterfaceHandler = (cutId: number) => {
    const request = classesUnderTest[cutId]

    console.log(JSON.stringify(request))

    setMessage("");
    setLoading(true);

    const valid: boolean = true

    if (valid) {
      SheetService.toLQL(request).then(
        (response) => {
          setInterfaceSpecification(response.data.interfaceSpecification);
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
    console.log("lql handler " + lql);

    setInterfaceSpecification(lql);

    // generate prompt
    const newPrompt = `implement a java class with the following interface specification, but do not inherit a java interface: \`\`\`${lql}\`\`\`. Only output the java class and nothing else.`;

    setCodeGenerationPrompt(newPrompt);
  }

  // get LQL editor (monaco)
  const lqlEditorHandler = (editor: any) => {
    //console.log("lql editor handler " + editor)

    lqlEditorRef.current = editor
  }

  // get CUT
  const cutHandler = (cutId: number, className: string, artifacts: string[]) => {
    console.log("cut handler " + cutId + " " + className + " " + typeof (artifacts))

    const newArr = [...classesUnderTest];
    const cut = newArr[cutId];
    cut.className = className;
    cut.artifacts = artifacts;

    setClassesUnderTest(newArr);
  }

  const removeClassUnderTest = (cutId: number) => {
    const newArr = [...classesUnderTest];
    newArr.splice(cutId, 1);

    setClassesUnderTest(newArr);
  }

  const removeSheet = (sheetId: number) => {
    console.log("remove sheet " + sheetId);

    //const newArr = [...stimulusSheets];
    //newArr.splice(sheetId, 1);

    const newArr = stimulusSheets.filter((el, i) => i != sheetId);

    console.log("arr " + JSON.stringify(newArr));

    setStimulusSheets(newArr);
  }

  const parseActuationSheet = (sheet: SheetSpec) => {
    const matrix = loadSheetJsonl(sheet?.body)

    return matrix
  }

  const codeSearch = () => {
    const codeSearchRequest = new CodeSearchRequest();
    codeSearchRequest.dataSource = codeSearchDataSource;
    codeSearchRequest.limit = codeSearchResultsLimit;
    codeSearchRequest.interfaceSpecification = interfaceSpecification;

    setMessage("");
    setLoading(true);

    SheetService.searchCodeModules(codeSearchRequest).then(
      (response) => {
        const codeSearchResponse: CodeSearchResponse = response.data;

        //console.log("codesearchresponse " + JSON.stringify(codeSearchResponse));

        const newArr = [...classesUnderTest, ...codeSearchResponse.classResults];
        setClassesUnderTest(newArr);

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
  };

  const codeGeneration = () => {
    const codeGenerationRequest = new CodeGenerationRequest();
    codeGenerationRequest.model = codeGenerationModel;
    codeGenerationRequest.prompt = codeGenerationPrompt;
    //codeGenerationRequest.sampleSize

    setMessage("");
    setLoading(true);

    SheetService.generateCodeModules(codeGenerationRequest).then(
      (response) => {
        const codeGenerationResponse: CodeGenerationResponse = response.data;

        //console.log("generateCode response " + JSON.stringify(codeGenerationResponse));

        const newArr = [...classesUnderTest, ...codeGenerationResponse.classResults];
        setClassesUnderTest(newArr);

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
  };

  const interfaceGeneration = () => {
    const lqlGenerationRequest = new LqlGenerationRequest();
    lqlGenerationRequest.model = lqlGenerationModel;
    lqlGenerationRequest.prompt = lqlGenerationPrompt;
    //lqlGenerationRequest.sampleSize

    setMessage("");
    setLoading(true);

    SheetService.generateInterface(lqlGenerationRequest).then(
      (response) => {
        const lqlGenerationResponse: LqlGenerationResponse = response.data;

        console.log("generate lql response " + JSON.stringify(lqlGenerationResponse));

        const lql = lqlGenerationResponse.lql;
        setInterfaceSpecification(lql);
        // update monaco LQL editor
        lqlEditorRef.current.getModel().setValue(lql);

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
  };

  // execute sheet
  const generateSheets = (testGenerator: string) => {
    //console.log("executed sheet '" + sheetSignature + "' data: " + sheetData)
    console.log("lql handler " + interfaceSpecification)
    console.log("cut handler " + JSON.stringify(classesUnderTest))

    const request = new SheetGenerationRequest()
    request.classesUnderTest = classesUnderTest
    request.sheets = []
    request.testGenerator = testGenerator;

    stimulusSheets.forEach((stimulusSheet) => {
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
      SheetService.generateSheets(request).then(
        (response) => {
          console.log("generate response " + JSON.stringify(response.data));

          const sheetGenerationResponse: SheetGenerationResponse = response.data;
          if (sheetGenerationResponse.sheets) {
            const sheets: StimulusSheet[] = sheetGenerationResponse.sheets.map(sheetRaw => {
              const sheet: StimulusSheet = new StimulusSheet();
              sheet.signature = sheetRaw.signature;
              sheet.data = loadSheetJsonl(sheetRaw.body);
              if (sheetRaw.invocations) {
                sheet.invocations = sheetRaw.invocations;
              } else {
                sheet.invocations = [];
              }
              return sheet;
            });

            // append
            setStimulusSheets([...stimulusSheets, ...sheets]);
          }

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
  };

  const [testDialogOpen, setTestDialogOpen] = React.useState(false);

  const handleClickTestDialogOpen = () => {
    setTestDialogOpen(true);
  };

  const handleTestDialogClose = () => {
    setTestDialogOpen(false);
  };

  const testDescriptionElementRef = React.useRef<HTMLElement>(null);
  React.useEffect(() => {
    if (testDialogOpen) {
      const { current: descriptionElement } = testDescriptionElementRef;
      if (descriptionElement !== null) {
        descriptionElement.focus();
      }
    }
  }, [testDialogOpen]);

  const [implDialogOpen, setImplDialogOpen] = React.useState(false);

  const handleClickImplDialogOpen = () => {
    setImplDialogOpen(true);
  };

  const handleImplDialogClose = () => {
    setImplDialogOpen(false);
  };

  const implDescriptionElementRef = React.useRef<HTMLElement>(null);
  React.useEffect(() => {
    if (implDialogOpen) {
      const { current: descriptionElement } = implDescriptionElementRef;
      if (descriptionElement !== null) {
        descriptionElement.focus();
      }
    }
  }, [implDialogOpen]);

  //
  const [actuationSheetDialogOpen, setActuationSheetDialogOpen] = React.useState(false);
  const [currentActuationSheet, setCurrentActuationSheet] = React.useState<any>();

  const handleClickActuationSheetDialogOpen = (actuationSheet: any) => {
    setCurrentActuationSheet(actuationSheet);
    setActuationSheetDialogOpen(true);
  };

  const handleActuationSheetDialogClose = () => {
    setActuationSheetDialogOpen(false);
  };

  const actuationSheetDescriptionElementRef = React.useRef<HTMLElement>(null);
  React.useEffect(() => {
    if (actuationSheetDialogOpen) {
      const { current: descriptionElement } = actuationSheetDescriptionElementRef;
      if (descriptionElement !== null) {
        descriptionElement.focus();
      }
    }
  }, [actuationSheetDialogOpen]);

  //
  const [executedImplDialogOpen, setExecutedImplDialogOpen] = React.useState(false);
  const [currentExecutedImpl, setCurrentExecutedImpl] = React.useState<any>();

  const handleClickExecutedImplDialogOpen = (implId: any) => {
    if (!sheetResponse) {
      return;
    }

    console.log("searching for impl " + implId);
    const implementation = sheetResponse.testResults[0].executedImplementations.find(impl => impl.id == implId);
    console.log("found impl " + implementation);
    setCurrentExecutedImpl(implementation);
    setExecutedImplDialogOpen(true);
  };

  const handleExecutedImplDialogClose = () => {
    setExecutedImplDialogOpen(false);
  };

  const executedImplDescriptionElementRef = React.useRef<HTMLElement>(null);
  React.useEffect(() => {
    if (executedImplDialogOpen) {
      const { current: descriptionElement } = executedImplDescriptionElementRef;
      if (descriptionElement !== null) {
        descriptionElement.focus();
      }
    }
  }, [executedImplDialogOpen]);


  // rows and columns for the SM
  const getSmRows = () => {
    //
    const tests = stimulusSheets.flatMap(ss => {
      if (!ss.invocations || ss.invocations.length == 0) {
        return {
          test: ss,
          invocation: "()"
        };
      }

      const invocs = ss.invocations.map(inv => {
        return {
          test: ss,
          invocation: "(" + inv + ")"
        };
      });

      return invocs;
    });

    const testRows: GridRowsProp = tests.map(test => {
      const keys = classesUnderTest.map(cut => { return cut.id; });
      const values = classesUnderTest.map(cut => { return test.invocation; });
      const row = Object.fromEntries(keys.map((key, index) => [key, values[index]]));

      row["test"] = test.test.signature;

      return row;
    });

    return testRows;
  };

  const getSmColumns = () => {
    // FIXME field (must be ID)
    const cutColumns: GridColDef[] = classesUnderTest.map(cut => { return { field: cut.id, headerName: cut.className, width: 200, valueFormatter: (value: any) => value,
      sortable: false,
      filterable: false } });

    // first column is test column
    const rowHeader = {
      field: "test", headerName: "Tests", width: 200, renderCell: (params: any) => (
        <Link onClick={handleClickTestDialogOpen}>{params.value!.toString()}</Link>
      )
    };

    return [rowHeader, ...cutColumns];
  };

  const getSrmColumns = () => {
    if (!sheetResponse) {
      return [];
    }

    // FIXME ID duplicates
    let cutColumns: GridColDef[];
    if (srmView == "testcases") {
      cutColumns = sheetResponse.testResults[0].executedImplementations.map(impl => {
        return {
          field: impl.id, headerName: impl.className, width: 200, renderCell: (params: any) => (
            <><Button size="small">{"C" + params.value.cluster}</Button><IconButton onClick={(event) => handleClickActuationSheetDialogOpen(params.value)} color="primary" aria-label="Show Actuation Sheet">
              <GridOnIcon />
            </IconButton></>
          ), valueFormatter: (value: any) => value.columnValue,
          sortable: false,
          filterable: false
        }
      });
    } else {
      cutColumns = sheetResponse.testResults[0].executedImplementations.map(impl => {
        return {
          field: impl.id, headerName: impl.className, width: 200, renderCell: (params: any) => (
            <><Button size="small">{"C" + params.value.cluster}</Button>{params.value.columnValue}</>
          ), valueFormatter: (value: any) => value.columnValue,
          sortable: false,
          filterable: false
        }
      });
    }

    // first column is test column
    const rowHeader = {
      field: "test", headerName: "Tests", width: 200, renderCell: (params: any) => (
        <>{params.value.columnValue}</>
      ), valueFormatter: (value: any) => value.columnValue
    };

    return [rowHeader, ...cutColumns];
  };

  // FIXME duplicate code with srmRows cluster analysis
  const doBehavioralClustering = () => {
    if (!sheetResponse) {
      return [];
    }

    // original stimulus sheets
    let tests: SheetSpec[] = sheetResponse.testResults[0].executedTests;
    // implementations
    const impls: string[] = sheetResponse.testResults[0].executedImplementations.map(impl => {
      return impl.id;
    });

    let selectedActuationSheets = sheetResponse.testResults[0].adaptedActuationSheets;
    if (srmAbstractionLevel == "specification") {
      selectedActuationSheets = sheetResponse.testResults[0].actuationSheets;
    }

    // parsed actuation sheets
    let actuationSheetMap: Map<string, any> = new Map(selectedActuationSheets.map(sheet => {
      return [sheet.signature + "_" + sheet.implementationId, {
        test: sheet,
        data: parseActuationSheet(sheet)
      }];
    }));

    // FIXME oracle voting strategy
    if (oracleVoting) {

    }

    // FIXME
    const clusterColumn = 0;
    // concatenate all sheets
    let largeColumnMap: Map<string, any> = new Map(impls.map(implId => {
      const largeColumn = tests.flatMap(test => {
        const actuationSheet = actuationSheetMap.get(test.signature + "_" + implId);
        return actuationSheet.data.map((r: any[]) => r[clusterColumn].value);
      });

      return [implId, largeColumn];
    }));

    const clustered: { [key: string]: any[] } = {};
    largeColumnMap.forEach((data, implId) => {
      // TODO improve equivalence checks.
      const uniqueKey = JSON.stringify(data);
      if (!clustered[uniqueKey]) {
        clustered[uniqueKey] = [];
      }

      clustered[uniqueKey].push(implId);
    });

    // to array to get numerical cluster names
    const clusterNumbered = Object.keys(clustered).map(key => {
      return clustered[key];
    });

    return clusterNumbered;
  };

  // rows and columns for the SM
  const getSrmRows = () => {
    if (!sheetResponse) {
      return [];
    }

    // original stimulus sheets
    let tests: SheetSpec[] = sheetResponse.testResults[0].executedTests;
    // implementations
    const impls: string[] = sheetResponse.testResults[0].executedImplementations.map(impl => {
      return impl.id;
    });

    let selectedActuationSheets = sheetResponse.testResults[0].adaptedActuationSheets;
    if (srmAbstractionLevel == "specification") {
      selectedActuationSheets = sheetResponse.testResults[0].actuationSheets;
    }

    // parsed actuation sheets
    let actuationSheetMap: Map<string, any> = new Map(selectedActuationSheets.map(sheet => {
      return [sheet.signature + "_" + sheet.implementationId, {
        test: sheet,
        data: parseActuationSheet(sheet)
      }];
    }));

    // FIXME
    const clusterColumn = 0;
    // concatenate all sheets
    let largeColumnMap: Map<string, any> = new Map(impls.map(implId => {
      const largeColumn = tests.flatMap(test => {
        const actuationSheet = actuationSheetMap.get(test.signature + "_" + implId);
        return actuationSheet.data.map((r: any[]) => r[clusterColumn].value);
      });

      return [implId, largeColumn];
    }));

    // FIXME oracle voting strategy
    if (oracleVoting) {

    }

    const clustered: { [key: string]: any[] } = {};
    largeColumnMap.forEach((data, implId) => {
      // TODO improve equivalence checks.
      const uniqueKey = JSON.stringify(data);
      if (!clustered[uniqueKey]) {
        clustered[uniqueKey] = [];
      }

      clustered[uniqueKey].push(implId);
    });

    // to array to get numerical cluster names
    const clusterNumbered = Object.keys(clustered).map(key => {
      return clustered[key];
    });

    // now do it by implId
    const clusterIdMap = new Map(Object.keys(clustered).flatMap(key => {
      const arr = clustered[key];
      return arr.map(implId => {
        return [implId, clusterNumbered.indexOf(arr)];
      });
    }));

    selectedActuationSheets.map(sheet => {
      return [sheet.signature + "_" + sheet.implementationId, {
        test: sheet,
        data: parseActuationSheet(sheet)
      }];
    });

    if (srmView == "testcases") {
      const testRows: GridRowsProp = tests.map(test => {
        const values = impls.map(implId => {
          const actuationSheet = actuationSheetMap.get(test.signature + "_" + implId);
          const impl = sheetResponse.testResults[0].executedImplementations.find(i => i.id == implId);

          return {
            test: actuationSheet,
            implementation: impl,
            columnValue: "View",
            cluster: clusterIdMap.get(implId)
          };
        });
        const row = Object.fromEntries(impls.map((key, index) => [key, values[index]]));

        row["test"] = {
          test: {
            test: test,
            data: []
          },
          implementation: new ClassUnderTestSpec(),
          columnValue: test.signature,
          cluster: -1
        };

        return row;
      });

      return testRows;
    } else if(srmView == "testset") {
      //
      const testSet = new SheetSpec();
      testSet.signature = "All";

      const values = impls.map(implId => {
        const impl = sheetResponse.testResults[0].executedImplementations.find(i => i.id == implId);
        const tests = selectedActuationSheets.filter(test => test.implementationId == implId);

        return {
          test: {
            test: testSet,
            data: []
          },
          implementation: impl,
          columnValue: tests.length + " Tests",
          cluster: clusterIdMap.get(implId)
        };
      });
      const row = Object.fromEntries(impls.map((key, index) => [key, values[index]]));

      row["test"] = {
        test: {
          test: testSet,
          data: []
        },
        implementation: new ClassUnderTestSpec(),
        columnValue: testSet.signature,
        cluster: -1
      };

      return [row];
    } else {
      // whitebox
      let columnNo = -1;
      if (srmView == "whitebox_outputs") {
        columnNo = 0;
      }
      if (srmView == "whitebox_operations") {
        columnNo = 1;
      }

      // filter by row (sheet statement)
      const testStmts = tests.flatMap(test => {
        const testData: any[][] = parseActuationSheet(test);
        //const column = testData.map(row => row[columnNo]);

        return testData.map((stmt, index) => {
          return {
            test: test,
            statementNo: index,
            columnNo: columnNo
          };
        });
      });

      const testRows: GridRowsProp = testStmts.map(testStmt => {
        const values = impls.map(implId => {
          const currentTest = testStmt.test;

          const actuationSheet = actuationSheetMap.get(currentTest.signature + "_" + implId);
          const impl = sheetResponse.testResults[0].executedImplementations.find(i => i.id == implId);

          return {
            test: actuationSheet,
            implementation: impl,
            columnValue: actuationSheet.data[testStmt.statementNo][testStmt.columnNo]?.value,
            cluster: clusterIdMap.get(implId)
          };
        });

        const row = Object.fromEntries(impls.map((key, index) => [key, values[index]]));

        row["test"] = {
          test: testStmt.test,
          implementation: new ClassUnderTestSpec(),
          columnValue: testStmt.test.signature + "..." + (testStmt.statementNo + 1),
          cluster: -1
        };

        return row;
      });

      return testRows;
    }
  };

  const SrmCustomItem = (props: GridColumnMenuItemProps) => {
    const { myCustomHandler, myCustomValue } = props;
    return (
      <MenuItem onClick={myCustomHandler}>
        <ListItemIcon>
          <GridOnIcon fontSize="small" />
        </ListItemIcon>
        <ListItemText>{myCustomValue}</ListItemText>
      </MenuItem>
    );
  }

  const SrmCustomColumnMenu = (props: GridColumnMenuProps) => {
    return (
      <GridColumnMenu
        {...props}
        slots={{
          // Add new item
          columnMenuUserItem: SrmCustomItem,
        }}
        slotProps={{
          columnMenuUserItem: {
            // set `displayOrder` for the new item
            displayOrder: 15,
            // Additional props
            myCustomValue: 'Show Details',
            myCustomHandler: () => handleClickExecutedImplDialogOpen(props.colDef.field),
          },
        }}
      />
    );
  }

  return (
    <div>

      <h2>Stimulus Matrix Editor</h2>

      <Box component="section" sx={{ p: 2, border: '1px grey' }}>
        {loading && (
          <Backdrop
            sx={(theme) => ({ color: '#fff', zIndex: theme.zIndex.drawer + 1 })}
            open={loading}
          >
            <CircularProgress color="inherit" />
          </Backdrop>
        )}</Box>

      <Dialog
        open={testDialogOpen}
        fullScreen={true}
        onClose={handleTestDialogClose}
        scroll={'paper'}
        aria-labelledby="scroll-dialog-title"
        aria-describedby="scroll-dialog-description"
      >
        <DialogTitle id="scroll-dialog-title">Stimulus Sheets</DialogTitle>
        <DialogContent dividers={true}>
          <DialogContentText
            id="scroll-dialog-description"
            ref={testDescriptionElementRef}
            tabIndex={-1}
          >
            Tests
            <Box component="section" sx={{ p: 2, border: '1px grey' }}>
              {stimulusSheets.map((stimulusSheet, index) => (
                <Sheet key={stimulusSheet.signature} sheetId={index} model={stimulusSheet} removeHandler={removeSheet} changeHandler={stimulusSheetChangeHandler} />
              ))}

              <ButtonGroup size="small" aria-label="Small button group">
                <Button size="small" onClick={(event) => addStimulusSheet()}>New</Button>
                <Button size="small" onClick={(event) => generateSheets("random")}>Generate Randomly</Button>
                <Button size="small" onClick={(event) => generateSheets("genai")}>Generate (LLM)</Button>
                <Button size="small" onClick={(event) => generateSheets("mutate")}>Mutate Test Data</Button>
              </ButtonGroup>
            </Box>
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleTestDialogClose}>Done</Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={implDialogOpen}
        fullScreen={true}
        onClose={handleImplDialogClose}
        scroll={'paper'}
        aria-labelledby="scroll-dialog-title"
        aria-describedby="scroll-dialog-description"
      >
        <DialogTitle id="scroll-dialog-title">Implementations</DialogTitle>
        <DialogContent dividers={true}>
          <DialogContentText
            id="scroll-dialog-description"
            ref={implDescriptionElementRef}
            tabIndex={-1}
          >
            <Box component="section" sx={{ p: 2, border: '1px grey' }}>
              {classesUnderTest.map((cut, index) => (
                <ClassUnderTest key={cut.className + "_" + JSON.stringify(cut.artifacts)} detectInterfaceHandler={detectInterfaceHandler} removeHandler={removeClassUnderTest} cutHandler={cutHandler} cutId={index} classUnderTest={cut} />
              ))}
              <Button onClick={(event) => addClassUnderTest()}>Manually Add from Artifact Repository</Button>
            </Box>
            <Accordion>
              <AccordionSummary
                expandIcon={<ExpandMoreIcon />}
                aria-controls="panel2-content"
                id="panel2-header"
              >
                <Typography component="span">Retrieve by Interface-Driven Code Search</Typography>
              </AccordionSummary>
              <AccordionDetails>
                <TextField fullWidth onChange={handleCodeSearchDataSource} value={codeSearchDataSource} id="outlined-basic" label="Data Source" variant="outlined" />
                <TextField fullWidth onChange={handleCodeSearchResultsLimit} value={codeSearchResultsLimit} id="outlined-basic" label="Result Limit" variant="outlined" />

              </AccordionDetails>
              <AccordionActions>
                <Button onClick={(event) => codeSearch()}>Search and Add</Button>
              </AccordionActions>
            </Accordion>
            <Accordion>
              <AccordionSummary
                expandIcon={<ExpandMoreIcon />}
                aria-controls="panel3-content"
                id="panel3-header"
              >
                <Typography component="span">Generate by Code Model (LLM)</Typography>
              </AccordionSummary>
              <AccordionDetails>
                <TextField multiline fullWidth onChange={handleCodeGenerationPrompt} value={codeGenerationPrompt} id="outlined-basic" label="Prompt" variant="outlined" />
                <TextField id="outlined-basic" fullWidth onChange={handleCodeGenerationModel} value={codeGenerationModel} label="Model" variant="outlined" />
              </AccordionDetails>
              <AccordionActions>
                <Button onClick={(event) => codeGeneration()}>Generate and Add</Button>
              </AccordionActions>
            </Accordion>
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleImplDialogClose}>Done</Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={actuationSheetDialogOpen}
        fullScreen={true}
        onClose={handleActuationSheetDialogClose}
        scroll={'paper'}
        aria-labelledby="scroll-dialog-title"
        aria-describedby="scroll-dialog-description"
      >
        <DialogTitle id="scroll-dialog-title">Actuation Sheet</DialogTitle>
        <DialogContent dividers={true}>
          <DialogContentText
            id="scroll-dialog-description"
            ref={actuationSheetDescriptionElementRef}
            tabIndex={-1}
          >
            {currentActuationSheet ? <ActuationSheet sheetSignature={currentActuationSheet?.test?.test?.signature} sheetData={currentActuationSheet.test.data} implementation={currentActuationSheet.implementation} /> : undefined}

          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleActuationSheetDialogClose}>Close</Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={executedImplDialogOpen}
        fullScreen={true}
        onClose={handleExecutedImplDialogClose}
        scroll={'paper'}
        aria-labelledby="scroll-dialog-title"
        aria-describedby="scroll-dialog-description"
      >
        <DialogTitle id="scroll-dialog-title">Executed Implementation {currentExecutedImpl?.className} ({currentExecutedImpl?.id})</DialogTitle>
        <DialogContent dividers={true}>
          <DialogContentText
            id="scroll-dialog-description"
            ref={executedImplDescriptionElementRef}
            tabIndex={-1}
          >
            <TextField fullWidth multiline maxRows={20} value={JSON.stringify(currentExecutedImpl, null, 4)} id="outlined-basic" label="Details" variant="outlined" />
            <TextField fullWidth multiline maxRows={20} value={currentExecutedImpl?.codeUnit?.content} id="outlined-basic" label="Code" variant="outlined" />
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleExecutedImplDialogClose}>Close</Button>
        </DialogActions>
      </Dialog>

      <Grid container spacing={2}>
        <Grid size={3}>
          <Card sx={{ height: 250 }}>
            <CardContent>
              <Typography variant="h5" component="div">
                Functional Abstraction
              </Typography>
              <Typography sx={{ color: 'text.secondary', mb: 1.5 }}>Properties</Typography>
              <TextField id="outlined-basic" fullWidth onChange={handleFaName} value={faName} label="Label" variant="outlined" />
            </CardContent>
            <CardActions>
            </CardActions>
          </Card>
        </Grid>
        <Grid size={6}>
          <Card sx={{ height: 250 }}>
            <CardContent>
              <Typography sx={{ color: 'text.secondary', mb: 1.5 }}>Interface Specification (LQL)</Typography>
              <LQLEditor editorHandler={lqlEditorHandler} lqlHandler={lqlHandler} defaultLqlCode={stimulusMatrix.abstraction.interfaceSignature} />

            </CardContent>
            <CardActions>

            </CardActions>
          </Card>
        </Grid>
        <Grid size={3}>
          <Card sx={{ height: 250, overflow: 'auto' }}>
            <CardContent>
              <Typography sx={{ color: 'text.secondary', mb: 1.5 }}>Actions</Typography>
            </CardContent>
            <CardActions>
              <Accordion>
                <AccordionSummary
                  expandIcon={<ExpandMoreIcon />}
                  aria-controls="panel3-content"
                  id="panel3-header"
                >
                  <Typography component="span">Generate Interface</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Box component="section" sx={{ p: 2, border: '1px grey' }}>
                    <TextField multiline fullWidth onChange={handleLqlGenerationPrompt} value={lqlGenerationPrompt} id="outlined-basic" label="Prompt" variant="outlined" />
                    <TextField id="outlined-basic" fullWidth onChange={handleLqlGenerationModel} value={lqlGenerationModel} label="Model" variant="outlined" />
                  </Box>
                </AccordionDetails>
                <AccordionActions>
                  <Button onClick={(event) => interfaceGeneration()}>Generate Interface</Button>
                </AccordionActions>
              </Accordion>
            </CardActions>
          </Card>
        </Grid>
        <Grid size={3}>
          <Card sx={{ height: 500, overflow: 'auto' }}>
            <CardContent>
              <Typography variant="h5" component="div">
                Tests
              </Typography>
              <Typography sx={{ color: 'text.secondary', mb: 1.5 }}>Total number of sheets: {stimulusSheets.length}</Typography>

            </CardContent>
            <CardActions>
              <ButtonGroup size="small" aria-label="Small button group">
                <Button onClick={(event) => handleClickTestDialogOpen()}>Edit</Button>
              </ButtonGroup>
            </CardActions>
          </Card>
        </Grid>
        <Grid size={6}>
          <div style={{ height: 500, width: '100%' }}>
            <DataGrid slots={{ toolbar: GridToolbar }} rows={getSmRows()} columns={getSmColumns()} getRowId={(row: any) => /* FIXME unique ID required */ Math.floor(Math.random() * 100000000)} />
          </div>
        </Grid>
        <Grid size={3}>
          <Card sx={{ height: 500, overflow: 'auto' }}>
            <CardContent>
              <Typography variant="h5" component="div">
                Implementations
              </Typography>
              <Typography sx={{ color: 'text.secondary', mb: 1.5 }}>Total number of implementations: {classesUnderTest.length}</Typography>
            </CardContent>
            <CardActions>
              <ButtonGroup size="small" aria-label="Small button group">
                <Button onClick={(event) => handleClickImplDialogOpen()}>Edit</Button>
              </ButtonGroup>
            </CardActions>
          </Card>
        </Grid>
        <Grid size={12}>
          <Card sx={{}}>
            <CardContent>
              <Typography variant="h5" component="div">
                Arena Test Driver
              </Typography>
              <Typography sx={{ color: 'text.secondary', mb: 1.5 }}>Settings</Typography>

              <Box
                component="form"
                sx={{ '& > :not(style)': { m: 1, width: '25ch' } }}
                noValidate
                autoComplete="off"
              >

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

                <FormControl sx={{ m: 1, minWidth: 80 }}>
                  <InputLabel id="demo-simple-select-autowidth-label">Adaptation Strategy</InputLabel>
                  <Select
                    labelId="demo-simple-select-autowidth-label"
                    id="demo-simple-select-autowidth"
                    value={adaptationStrategy}
                    onChange={handleAdaptationStrategyChange}
                    autoWidth
                    label="Adaptation Strategy">
                    <MenuItem value={"PassThroughAdaptationStrategy"}>None</MenuItem>
                    <MenuItem value={"DefaultAdaptationStrategy"}>Default</MenuItem>
                  </Select>
                </FormControl>
                <TextField onChange={handleAdapterLimitChange} value={adapterLimit} id="outlined-basic" label="No. of Adapters" variant="outlined" />
                <Button onClick={(event) => executeAllHandler()}>Run Tests</Button>

              </Box>
            </CardContent>
            <CardActions>

            </CardActions>
          </Card>
        </Grid>
      </Grid>

      {message && (
        <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
          <Alert severity="error">{message}</Alert>
        </Box>
      )}

      {sheetResponse?.testResults.map(testResult => (
        <>

          <h2>Arena Execution Results (ID '{sheetResponse.executionId}')</h2>
          <Tabs value={resultTab} onChange={handleResultTabChange} aria-label="basic tabs example">
            <Tab label="Observations (SRM)" {...a11yProps(0)} />
            <Tab label="Measurements" {...a11yProps(1)} />
            <Tab label="SRH (Data Analytics)" {...a11yProps(2)} />
          </Tabs>

          <CustomTabPanel value={resultTab} index={0}>
            <FormControl sx={{ m: 1 }}>
              <InputLabel id="demo-simple-select-autowidth-label">Zoom Level</InputLabel>
              <Select autoWidth
                labelId="demo-simple-select-autowidth-label"
                id="demo-simple-select-autowidth"
                value={srmView}
                onChange={handleSrmViewChange}
                label="Zoom Level">
                <MenuItem value={"testset"}>Test Set</MenuItem>
                <MenuItem value={"testcases"}>Test Case</MenuItem>
                <MenuItem value={"whitebox_outputs"}>Test Statement: Output Observations</MenuItem>
                <MenuItem value={"whitebox_operations"}>Test Statement: Operations</MenuItem>

                {/* <MenuItem value={"whitebox_codecoverage"}>White box: Code Coverage</MenuItem> */}

              </Select>
            </FormControl>
            {/* <FormControl sx={{ m: 1 }}>
              <InputLabel id="demo-simple-select-autowidth-label">Sheet Columns</InputLabel>
              <Select autoWidth
                labelId="demo-simple-select-autowidth-label"
                id="demo-simple-select-autowidth"
                value={srmView}
                onChange={handleSrmViewChange}
                label="Sheet Columns">
                <MenuItem value={"all"}>All</MenuItem>
                <MenuItem value={"outputs"}>Outputs</MenuItem>
                <MenuItem value={"operations"}>Operations</MenuItem>
              </Select>
            </FormControl> */}
            <FormControl sx={{ m: 1 }}>
              <InputLabel id="demo-simple-select-autowidth-label">Oracle Voting</InputLabel>
              <Select autoWidth
                labelId="demo-simple-select-autowidth-label"
                id="demo-simple-select-autowidth"
                value={oracleVoting}
                onChange={handleOracleVotingChange}
                label="Oracle Voting">
                <MenuItem value={"cluster"}>Cluster-based</MenuItem>
                {/* <MenuItem value={"test"}>Test-based</MenuItem> */}
              </Select>
            </FormControl>
            <FormControl sx={{ m: 1 }}>
              <InputLabel id="demo-simple-select-autowidth-label">Abstraction Level</InputLabel>
              <Select autoWidth
                labelId="demo-simple-select-autowidth-label"
                id="demo-simple-select-autowidth"
                value={srmAbstractionLevel}
                onChange={handleSrmAbstractionLevelChange}
                label="Abstraction Level">
                <MenuItem value={"implementation"}>Implementation</MenuItem>
                <MenuItem value={"specification"}>Specification</MenuItem>
              </Select>
            </FormControl>

            <Grid container spacing={2}>
              <Grid size={9}>
              <div style={{ height: 500, width: '100%' }}>
              <DataGrid slots={{ toolbar: GridToolbar, columnMenu: SrmCustomColumnMenu }} rows={getSrmRows()} columns={getSrmColumns()} getRowId={(row: any) => /* FIXME unique ID required */ Math.floor(Math.random() * 100000000)} />
            </div>
              </Grid>
        <Grid size={3}>
          <Card sx={{ height: 250 }}>
            <CardContent>
              <Typography variant="h5" component="div">
                Summary
              </Typography>
              <Typography sx={{ color: 'text.secondary', mb: 1.5 }}>Total Tests: {sheetResponse?.testResults[0].executedTests.length}</Typography>
              <Typography sx={{ color: 'text.secondary', mb: 1.5 }}>Total Implementations: {sheetResponse?.testResults[0].executedImplementations.length}</Typography>
              <Typography sx={{ color: 'text.secondary', mb: 1.5 }}>Total Behavioral Clusters: {doBehavioralClustering().length}</Typography>
            </CardContent>
            <CardActions>
            </CardActions>
          </Card>
        </Grid>
        </Grid>

          </CustomTabPanel>

          <CustomTabPanel value={resultTab} index={1}>
            {testResult.metricSheets.map((sheet) => (
              <>
                <h4>Implementation {sheet.implementationId}</h4>
                <ActuationSheet sheetSignature={sheet.signature} sheetData={parseActuationSheet(sheet)} />
              </>
            ))
            }
          </CustomTabPanel>
          <CustomTabPanel value={resultTab} index={2}>
            <List>
              <ListItem disablePadding>
                <ListItemText><Link target="_blank" href={"srm?executionId=" + sheetResponse.executionId}>Open Parquet Viewer (duckdb-wasm)</Link></ListItemText>
              </ListItem>
              <ListItem disablePadding>
                <ListItemText><Link target="_blank" href={SheetService.retrieveParquetUrl(sheetResponse.executionId)}>Download Parquet</Link></ListItemText>
              </ListItem>
            </List>
          </CustomTabPanel>
        </>
      ))}

    </div>
  );
}

export default StimulusMatrixEditorPage;
