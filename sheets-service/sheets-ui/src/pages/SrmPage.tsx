import { Button, ButtonGroup, CardActions, CardContent, Divider, TextField, Typography } from '@mui/material';
import React, { useState } from 'react';

import * as duckdb from '@duckdb/duckdb-wasm';
import { DataGrid, GridColDef, GridRowsProp, GridToolbar } from '@mui/x-data-grid';

const SrmPage = () => {
  // inputs
  const [srmPath, setSrmPath] = useState('./tdse_srm.parquet')
  const [srmSqlQuery, setSrmSqlQuery] = useState('Select * from tdse_srm.parquet')
  
  // table
  // const [tableHeaders, setTableHeaders] = useState<string[]>([]) 
  // const [tableRows, setTableRows] = useState<object[][]>([])
  // data grid
  const [rows, setRows] = useState<GridRowsProp>([]) 
  const [columns, setColumns] = useState<GridColDef[]>([])
  
  // FIXME initialize duckdb once; initialize a certain parquet once -- separate queries
  const loadParquet = async (parquetPath: string, sqlQuery: string) => {
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
    const res = await fetch(parquetPath);
    await db.registerFileBuffer('tdse_srm.parquet', new Uint8Array(await res.arrayBuffer()));

    console.log("registered parquet file")

    // Create a new connection
    const conn = await db.connect();

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
    loadParquet(srmPath, 'Select * from tdse_srm.parquet');
  }

  const doSrmQuery = (observationType: string | undefined) => {
    let sqlQuery;
    if(observationType) {
      sqlQuery = `PIVOT (SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID) as SYSTEMID, value, type from tdse_srm.parquet where type = '${observationType}') ON SYSTEMID USING first(VALUE) ORDER BY STATEMENT`
    } else {
      sqlQuery = `PIVOT (SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID) as SYSTEMID, value, type from tdse_srm.parquet) ON SYSTEMID USING first(VALUE) ORDER BY STATEMENT`
    }

    setSrmSqlQuery(sqlQuery)
    loadParquet(srmPath, sqlQuery);
  }

  const handleSrmPathChange = (event: any) => {
    setSrmPath(event.target.value)
  }

  const handleSrmQueryChange = (event: any) => {
    setSrmSqlQuery(event.target.value)
  }
  
  return (
    <React.Fragment>
      <CardContent>
        <Typography gutterBottom sx={{ color: 'text.secondary', fontSize: 14 }}>
          SRM Analysis
        </Typography>
        <Typography variant="h5" component="div">
        <TextField onChange={handleSrmPathChange} value={srmPath} id="outlined-basic" label="SRM URL Path" variant="outlined" />
        <TextField
          id="outlined-multiline-static"
          label="SQL Query (DuckDB)"
          multiline
          rows={4}
          onChange={handleSrmQueryChange} value={srmSqlQuery}
        />
        </Typography>
      </CardContent>
      <CardActions>
          <ButtonGroup variant="contained" aria-label="Basic button group">
            <Button onClick={(event) => doLoad()}>Load Raw SRM parquet</Button>
            <Button onClick={(event) => doSrmQuery(undefined)}>View All</Button>
            <Button onClick={(event) => doSrmQuery('value')}>View Output</Button>
            <Button onClick={(event) => doSrmQuery('input_value')}>View Inputs</Button>
            <Button onClick={(event) => doSrmQuery('op')}>View Operations</Button>
          </ButtonGroup>
      </CardActions>

      <Divider />

      {/* <p>Loaded {tableRows.length} Rows</p>

      <table>
      <thead>
        <tr>
          {tableHeaders.map(header => <th key={header}>{header}</th>)}
        </tr>
      </thead>
      <tbody>
        {tableRows.map((row, index) => (
          <tr key={index}>
            {row.map((cell, index) => <td key={index}>{cell + ""}</td>)}
          </tr>
        ))}
      </tbody>
      </table> */}

      <Divider/>

      <div style={{ height: '500px', width: '100%' }}>
        <DataGrid slots={{ toolbar: GridToolbar }} rows={rows} columns={columns} getRowId={(row: any) => /* FIXME unique ID required */ Math.floor(Math.random() * 100000000)} />
      </div>

    </React.Fragment>
  );
}

export default SrmPage;
