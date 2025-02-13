import { Button, ButtonGroup, CardActions, CardContent, Divider, FormControl, InputLabel, MenuItem, Select, SelectChangeEvent, TextField, Typography } from '@mui/material';
import React, { useRef, useState } from 'react';

import * as duckdb from '@duckdb/duckdb-wasm';
import { DataGrid, GridColDef, GridRowsProp, GridToolbar } from '@mui/x-data-grid';
import { useSearchParams } from 'react-router-dom';
import SheetService from '../services/SheetService';
import LassoService from '../services/LassoService';

import Link from '@mui/material/Link';
import { Editor } from '@monaco-editor/react';

const SrmPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  let executionIdParam: string | null = searchParams.get('executionId');
  console.log("executionId id: " + executionIdParam);
  if(!executionIdParam ) {
    executionIdParam = "none";
  }

  let platformIdParam: string | null = searchParams.get('platformId');
  if(!platformIdParam) {
    platformIdParam = "explorer"
  }

  // inputs
  const [executionId, setExecutionId] = useState(executionIdParam)
  const [platformId, setPlatformId] = useState(platformIdParam)
  const [parquetUrl, setParquetUrl] = useState("")
  const [srmSqlQuery, setSrmSqlQuery] = useState('Select * from tdse_srm.parquet')
  
  // references to duckdb
  const dbRef = useRef<duckdb.AsyncDuckDB>()

  // data grid
  const [rows, setRows] = useState<GridRowsProp>([]);
  const [columns, setColumns] = useState<GridColDef[]>([]);

  const editorRef = useRef<any>(null);
  const monacoRef = useRef<any>(null);
  
  // FIXME initialize duckdb once; initialize a certain parquet once -- separate queries
  const loadParquet = async (executionId: string, platformId: string, sqlQuery: string) => {
    if(editorRef.current) {
      editorRef.current.getModel().setValue(sqlQuery);
    }
    
    if(!dbRef.current) {
      const JSDELIVR_BUNDLES = duckdb.getJsDelivrBundles();

      // Select a bundle based on browser checks
      const bundle = await duckdb.selectBundle(JSDELIVR_BUNDLES);
      
      const worker_url = URL.createObjectURL(
        new Blob([`importScripts("${bundle.mainWorker!}");`], {type: 'text/javascript'})
      );
      
      // Instantiate the asynchronus version of DuckDB-Wasm
      const worker = new Worker(worker_url);
      const logger = new duckdb.ConsoleLogger();
      const db = new duckdb.AsyncDuckDB(logger, worker);
      await db.instantiate(bundle.mainModule, bundle.pthreadWorker);
      URL.revokeObjectURL(worker_url);
  
      // apply config
      const config: duckdb.DuckDBConfig = {
        query: {
            /**
             * By default, int values returned by DuckDb are Int32Array(2).
             * This setting tells DuckDB to cast ints to double instead,
             * so they become JS numbers.
             */
            castBigIntToDouble: true,
        },
      }
  
      db.open(config)
  
      console.log("loaded duckdb")
  
      // register parquet file
      //const res = await fetch(parquetPath);
      let res;
      if(platformId == "lasso") {
        setParquetUrl(LassoService.retrieveParquetUrl(executionId));
        res = (await LassoService.retrieveParquet(executionId)).data;
      } else {
        setParquetUrl(SheetService.retrieveParquetUrl(executionId));
        res = (await SheetService.retrieveParquet(executionId)).data;
      }
  
      //const res = (await SheetService.retrieveParquet(executionId)).data;
      await db.registerFileBuffer('tdse_srm.parquet', new Uint8Array(await res));
  
      console.log("registered parquet file")

      dbRef.current = db;
    }

    // Create a new connection
    const conn = await dbRef.current.connect();

    // Query
    const arrowResult = await conn.query(`
        ${sqlQuery}
    `);

    // Convert arrow table to json
    const result = arrowResult.toArray().map((row) => row.toJSON());

    //console.log(result)
    // const headers = Object.keys(result[0]);
    // const rowsSS: object[][] = result.map(item => Object.values(item));
    // setTableHeaders(headers)
    // setTableRows(rowsSS)

    // react-data-grid https://mui.com/x/react-data-grid/getting-started/
    const columns: GridColDef[] = Object.keys(result[0]).map(col => { return {field: col, headerName: col, width: 150} })
    //columns.push({field: "id", headerName: "ID", width: 150})
    setColumns(columns)
    const rows: GridRowsProp = result
    setRows(rows)

    // Close the connection to release memory
    await conn.close();
  }

  const doLoad = () => {
    loadParquet(executionId, platformId, 'Select * from tdse_srm.parquet');
  }

  const doQuery = () => {
    loadParquet(executionId, platformId, srmSqlQuery);
    setSrmSqlQuery(srmSqlQuery);
  }

  const executeQuery = (query: string) => {
    loadParquet(executionId, platformId, query);
    setSrmSqlQuery(query);
  }

  const doSrmQuery = (observationType: string | undefined) => {
    let sqlQuery;
    if(observationType) {
      sqlQuery = `PIVOT (SELECT SHEETID, X, Y, CONCAT(SYSTEMID,'_',VARIANTID,'_',ADAPTERID) as SYSTEMID, value from tdse_srm.parquet where type = '${observationType}') ON SYSTEMID USING first(VALUE) ORDER BY SHEETID, X, Y`
    } else {
      sqlQuery = `PIVOT (SELECT SHEETID, X, Y, CONCAT(SYSTEMID,'_',VARIANTID,'_',ADAPTERID) as SYSTEMID, value, type from tdse_srm.parquet) ON SYSTEMID USING first(VALUE) ORDER BY SHEETID, X, Y`
    }

    setSrmSqlQuery(sqlQuery)
    loadParquet(executionId, platformId, sqlQuery);
  }

  const handleSrmPathChange = (event: any) => {
    setExecutionId(event.target.value)
  }

  const handlePlatformIdChange = (event: SelectChangeEvent) => {
    setPlatformId(event.target.value);
  };

    // FIXME broken (https://duckdb.org/docs/api/wasm/query)
  const exportParquet = async () => {
    // Create a new connection
    if(dbRef.current) {
      const conn = await dbRef.current.connect();
      // Export Parquet
      await conn.send(`COPY (SELECT * FROM tdse_srm.parquet) TO 'result-snappy.parquet' (FORMAT PARQUET);`);
      const parquet_buffer = await dbRef.current.copyFileToBuffer('result-snappy.parquet');
  
      // Generate a download link
      const url = URL.createObjectURL(new Blob([parquet_buffer]));

      const downloadLink = React.createElement('a', { download: 'result-snappy.parquet', href: url}, 'download');

      await conn.close();

      return downloadLink;
    }

    return null;
  }
  
    function handleEditorDidMount(editor: any, monaco: any) {
      monaco.languages.register({ id: 'sql' });
  
      monacoRef.current = monaco;
      editorRef.current = editor ; 
    }
  
    const onSqlValidate = (sql: string | undefined, ev: monaco.editor.IModelContentChangedEvent) => {  
      if(sql) {
        setSrmSqlQuery(sql);
      }
    }
  
  return (
    <React.Fragment>
      <CardContent>
        <Typography gutterBottom sx={{ color: 'text.secondary', fontSize: 18 }}>
          SRM Analysis
        </Typography>
        <Typography variant="h5" component="div">
        <FormControl sx={{ m: 1, minWidth: 80 }}>
                  <InputLabel id="demo-simple-select-autowidth-label">Platform</InputLabel>
                  <Select
                    labelId="demo-simple-select-autowidth-label"
                    id="demo-simple-select-autowidth"
                    value={platformId}
                    onChange={handlePlatformIdChange}
                    autoWidth
                    label="Platform">
                    <MenuItem value={"explorer"}>Explorer</MenuItem>
                    <MenuItem value={"lasso"}>LASSO Platform</MenuItem>
                  </Select>
                </FormControl>
        <TextField onChange={handleSrmPathChange} value={executionId} id="outlined-basic" label="Execution ID" variant="outlined" />
      <Editor
        height="200px"
        defaultLanguage="sql"
        defaultValue={srmSqlQuery}
        onMount={handleEditorDidMount}
        onChange={onSqlValidate}
      />
        </Typography>
      </CardContent>
      <CardActions>


      </CardActions>

      <ButtonGroup variant="contained" aria-label="Basic button group">
            <Button onClick={(event) => doLoad()}>Load Raw SRM parquet</Button>
            <Button onClick={(event) => doQuery()}>Query SQL</Button>
            <Button onClick={(event) => executeQuery('select SHEETID from tdse_srm.parquet group by SHEETID order by SHEETID')}>Show Tests</Button>
            <Button onClick={(event) => executeQuery('select SHEETID, X, Y from tdse_srm.parquet where X >= 0 and Y >= 0 group by SHEETID, X, Y order by SHEETID, X, Y')}>Show Test Statements</Button>
            
            <Button onClick={(event) => executeQuery("select SYSTEMID from tdse_srm.parquet where SYSTEMID != 'abstraction' and SYSTEMID != 'oracle' group by SYSTEMID")}>Show Compilation Units</Button>
            <Button onClick={(event) => executeQuery("select SYSTEMID, VARIANTID, ADAPTERID from tdse_srm.parquet where SYSTEMID != 'abstraction' and SYSTEMID != 'oracle' group by SYSTEMID, VARIANTID, ADAPTERID")}>Show Executed Implementations</Button>
            
            <Button onClick={(event) => doSrmQuery('value')}>View Outputs</Button>
            <Button onClick={(event) => doSrmQuery('service')}>View Services</Button>
            <Button onClick={(event) => doSrmQuery('input_value')}>View Inputs</Button>
            <Button onClick={(event) => doSrmQuery('op')}>View Operations</Button>
            <Button onClick={(event) => doSrmQuery(undefined)}>View All</Button>
          </ButtonGroup>

          <Divider />

          <ButtonGroup variant="contained" aria-label="Basic button group">
            <Button onClick={(event) => executeQuery("select count(*) as cluster_size, list(SYSTEMID) as cluster_implementations, * EXCLUDE (SYSTEMID) from (PIVOT (SELECT CONCAT(SHEETID,'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',VARIANTID,'_',ADAPTERID) as SYSTEMID, value from tdse_srm.parquet where type = 'value') ON STATEMENT USING first(VALUE) ORDER BY SYSTEMID) as mypiv group by all order by cluster_size DESC")}>Cluster-based Voting</Button>
            <Button onClick={(event) => executeQuery("select count(*) as cluster_size, list(SYSTEMID) as cluster_implementations, * EXCLUDE (SYSTEMID) from (PIVOT (SELECT CONCAT(SHEETID,'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',VARIANTID,'_',ADAPTERID) as SYSTEMID, value from tdse_srm.parquet where type = 'value' and y > 0) ON STATEMENT USING first(VALUE) ORDER BY SYSTEMID) as mypiv group by all order by cluster_size DESC")}>Cluster-based Voting (Ignore Create)</Button>
            

            {/* <Button onClick={(event) => executeQuery("from (PIVOT (SELECT CONCAT(SHEETID,'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',VARIANTID,'_',ADAPTERID) as SYSTEMID, value from tdse_srm.parquet where type = 'value') ON STATEMENT USING first(VALUE) ORDER BY SYSTEMID) select mode(COLUMNS(*))")}>Test-based Voting</Button>
            <Button onClick={(event) => executeQuery("from (PIVOT (SELECT CONCAT(SHEETID,'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',VARIANTID,'_',ADAPTERID) as SYSTEMID, value from tdse_srm.parquet where type = 'value' and y > 0) ON STATEMENT USING first(VALUE) ORDER BY SYSTEMID) select mode(COLUMNS(*))")}>Test-based Voting (Ignore Create)</Button> */}
          </ButtonGroup>

          <Divider />

          <ButtonGroup variant="contained" aria-label="Basic button group">
            {/* <Button onClick={(event) => executeQuery(`
-- pick oracle based on clustering-based voting
select ABSTRACTIONID, SHEETID, X, Y, VALUE as cluster_based_oracle from tdse_srm.parquet where type = 'value' and CONCAT(SYSTEMID,'_',VARIANTID,'_',ADAPTERID) = (
select impl_match from 
(select
    count(*) as cluster_size,
    first(SYSTEMID) as impl_match,
    * EXCLUDE (SYSTEMID)
from (PIVOT (SELECT SHEETID, X, Y, CONCAT(SYSTEMID,'_',VARIANTID,'_',ADAPTERID) as SYSTEMID, value from tdse_srm.parquet where type = 'value') ON SHEETID, X, Y USING first(VALUE) ORDER BY SYSTEMID) as mypiv group by all order by cluster_size DESC limit 1)
) order by SHEETID, X, Y
              `)}>Cluster-based Oracle</Button> */}
            <Button onClick={(event) => executeQuery(`
-- pick oracle based on test-based voting (based on mode; most frequent value per test statement)
Select 
    ABSTRACTIONID,
    SHEETID,
    X,
    Y,
    MODE(VALUE) as test_based_oracle,
    list(DISTINCT VALUE) as distinct_values,
    (select list(CONCAT(SYSTEMID, '_', VARIANTID, '_', ADAPTERID) ORDER BY SYSTEMID, VARIANTID, ADAPTERID) from tdse_srm.parquet where VALUE = test_based_oracle and TYPE = 'value' and ABSTRACTIONID = tbl1.ABSTRACTIONID and SHEETID = tbl1.SHEETID and X = tbl1.X and Y=tbl1.Y) as matches
from tdse_srm.parquet as tbl1 where TYPE = 'value' and SYSTEMID != 'oracle' GROUP BY ABSTRACTIONID, SHEETID, X, Y ORDER BY SHEETID, X, Y
              `)}>Test-based Oracle</Button>
            <Button onClick={(event) => executeQuery(`
-- pick oracle based on test-based voting (based on mode; most frequent value per test statement)
Select 
    ABSTRACTIONID,
    SHEETID,
    X,
    Y,
    MODE(VALUE) as test_based_oracle,
    list(DISTINCT VALUE) as distinct_values,
    (select list(CONCAT(SYSTEMID, '_', VARIANTID, '_', ADAPTERID) ORDER BY SYSTEMID, VARIANTID, ADAPTERID) from tdse_srm.parquet where VALUE = test_based_oracle and TYPE = 'value' and ABSTRACTIONID = tbl1.ABSTRACTIONID and SHEETID = tbl1.SHEETID and X = tbl1.X and Y=tbl1.Y) as matches
from tdse_srm.parquet as tbl1 where TYPE = 'value' and SYSTEMID != 'oracle' and Y > 0 GROUP BY ABSTRACTIONID, SHEETID, X, Y ORDER BY SHEETID, X, Y
              `)}>Test-based Oracle (Ignore Create)</Button>
          </ButtonGroup>

{ parquetUrl.length > 0 &&
      <>
      <Divider />
                <Link target="_blank" href={parquetUrl}>Download Raw Parquet</Link>
                <Link target="_blank" onClick={() => exportParquet()}>Export Current Table to Parquet</Link>
      </>
}

      <Divider />

      <div style={{ height: '500px', width: '100%' }}>
        <DataGrid slots={{ toolbar: GridToolbar }} rows={rows} columns={columns} getRowId={(row: any) => /* FIXME unique ID required */ Math.floor(Math.random() * 100000000)} />
      </div>

    </React.Fragment>
  );
}

export default SrmPage;
