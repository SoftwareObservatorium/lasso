import { useEffect, useState } from "react";
import Spreadsheet, { CellBase, Matrix } from "react-spreadsheet";
import "./Sheet.css";
import { Box, Card, CardContent, TextField, Typography } from "@mui/material";

// just display records
const ActuationSheet = ({ sheetSignature, sheetData}: any) => {
  return (
    <Box sx={{ minWidth: 275 }}>
      <Card variant="outlined">
      <CardContent>
        <Typography gutterBottom sx={{ color: 'text.secondary', fontSize: 14 }}>
          Actuation Sheet
        </Typography>
        <Typography variant="h5" component="div">
        <TextField value={sheetSignature} id="outlined-basic" label="Sheet Signature" variant="outlined" />
        </Typography>
        <Typography sx={{ color: 'text.secondary', mb: 1.5 }}>Body</Typography>
        <Typography variant="body2">
          <Spreadsheet data={sheetData} />
        </Typography>
      </CardContent>
      </Card>
    </Box>
  );

}

export default ActuationSheet;
