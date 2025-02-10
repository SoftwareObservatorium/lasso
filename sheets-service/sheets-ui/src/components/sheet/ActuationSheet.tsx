import { useEffect, useState } from "react";
import Spreadsheet, { CellBase, Matrix } from "react-spreadsheet";
import "./Sheet.css";
import { Box, Card, CardContent, Divider, TextField, Typography } from "@mui/material";

// just display records
const ActuationSheet = ({ sheetSignature, sheetData, implementation}: any) => {
  return (
    <Box sx={{ minWidth: 275 }}>
      <Card variant="outlined">
      <CardContent>
        <Typography gutterBottom sx={{ color: 'text.secondary', fontSize: 14 }}>
          Actuation Sheet for {implementation?.className} ({implementation?.id})
        </Typography>
        <Typography variant="h5" component="div">
        <TextField value={sheetSignature} id="outlined-basic" label="Sheet Signature" variant="outlined" />
        </Typography>
        <Typography sx={{ color: 'text.secondary', mb: 1.5 }}>Body</Typography>
        <Typography variant="body2">
          <Spreadsheet data={sheetData} />
        </Typography>
        
        {implementation?.codeUnit ? <Typography variant="body2">
          <Divider>Implementation</Divider><TextField multiline fullWidth value={implementation.codeUnit.content} id="outlined-basic" label="Code" variant="outlined" /></Typography> : null }
        
      </CardContent>
      </Card>
    </Box>
  );

}

export default ActuationSheet;
