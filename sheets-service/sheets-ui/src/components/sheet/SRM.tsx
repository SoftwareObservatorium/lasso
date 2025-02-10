import "./Sheet.css";
import { Box, Card, CardContent, TextField, Typography } from "@mui/material";
import { DataGrid, GridColDef, GridToolbar } from "@mui/x-data-grid";

function loadSheetJsonl(jsonl: any) {
  const rows: any[] = [];
  for (const line of jsonl.trim().split(/[\r\n]+/)) {
    console.log(line);
    const row = JSON.parse(line + "");

    rows.push(row.cells);
  }

  return rows;
}

// just display records
const SRMView = ({ sheet }: any) => {
  const rows = loadSheetJsonl(sheet?.body);
  console.log("rows " + rows.length)

  // find largest row for columns
  let length = -1;
  let sampleRow = {};
  for (let i = 0; i < rows.length; i++) {
    const currentLength = Object.keys(rows[i]).length;

    console.log("length " + currentLength)

    if (currentLength > length) {
      length = currentLength;
      sampleRow = rows[i];
    }
  }

  const columns: GridColDef[] = Object.keys(sampleRow).map(col => { return {field: col, headerName: col, width: 150} });

  return (
    <Box sx={{ minWidth: 275 }}>
      <Card variant="outlined">
      <CardContent>
        <Typography gutterBottom sx={{ color: 'text.secondary', fontSize: 14 }}>
          Actuation Sheet
        </Typography>
        <Typography variant="h5" component="div">
        <TextField value={sheet.signature} id="outlined-basic" label="Sheet Signature" variant="outlined" />
        </Typography>
        <Typography sx={{ color: 'text.secondary', mb: 1.5 }}>Body</Typography>
        <Typography variant="body2">
          <DataGrid slots={{ toolbar: GridToolbar }} rows={rows} columns={columns} getRowId={(row: any) => /* FIXME unique ID required */ Math.floor(Math.random() * 100000000)} />
        </Typography>
      </CardContent>
      </Card>
    </Box>
  );

}

export default SRMView;
