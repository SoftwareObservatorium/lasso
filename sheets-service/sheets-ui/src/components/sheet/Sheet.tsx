import { useState, useEffect, useCallback } from "react";
import Spreadsheet, { CellBase, Matrix } from "react-spreadsheet";
import "./Sheet.css";
import ButtonGroup from "@mui/material/ButtonGroup";
import Button from "@mui/material/Button";
import { Box, Card, CardActions, CardContent, TextField, Typography } from "@mui/material";
import React from "react";

const Sheet = ({ sheetId, defaultSheetSignature, sheetData, changeHandler, isResult }: any) => {
  const [sheetSignature, setSheetSignature] = useState(defaultSheetSignature)
  const [data, setData] = useState<Matrix<CellBase<any>>>(
    //    [
    //     [
    //       {
    //         value: "redOnly + text-color",
    //         readOnly: true,
    //         className: "text-danger"
    //       },
    //       { value: "text-color", className: "text-danger" },
    //       { value: "readOnly", readOnly: true },
    //       { value: "readOnly + css", readOnly: true, className: "header-row" },
    //       { value: "css", className: "header-row" },
    //       { value: "no options" }
    //     ],
    //     [
    //       { value: "Strawberry" },
    //       { value: "Cookies" },
    //       { value: "Vanilla" },
    //       { value: "Chocolate" },
    //       { value: "Citrus" },
    //       { value: "Green Apple" }
    //     ]
    //   ]
    sheetData
  );

  useEffect(() => {
    console.log(data);
    changeHandler(sheetId, sheetSignature, data)

  }, [sheetSignature, data]);

  const onChangeSheetSignature = (e: any) => {
    setSheetSignature(e.target.value);
  };

  // copied from react-spreadsheet
  const addColumn = useCallback(
    () =>
      setData((data) =>
        data.map((row) => {
          const nextRow = [...row];
          nextRow.length += 1;
          return nextRow;
        })
      ),
    [setData]
  );

  // copied from react-spreadsheet
  const removeColumn = useCallback(() => {
    setData((data) =>
      data.map((row) => {
        return row.slice(0, row.length - 1);
      })
    );
  }, [setData]);

  // copied from react-spreadsheet
  const addRow = useCallback(
    () =>
      setData((data) => {
        const columns = 6
        return [...data, Array(columns)];
      }),
    [setData]
  );

  // copied from react-spreadsheet
  const removeRow = useCallback(() => {
    setData((data) => {
      return data.slice(0, data.length - 1);
    });
  }, [setData]);

  //   const executeSheet = useCallback(() => {
  //     //
  //     sendSheet(data)
  //   }, [setData]);

  const addInvocation = () => {}

  const card = (
    <React.Fragment>
      <CardContent>
        <Typography gutterBottom sx={{ color: 'text.secondary', fontSize: 14 }}>
          Stimulus Sheet
        </Typography>
        <Typography variant="h5" component="div">
        <TextField onChange={onChangeSheetSignature} value={sheetSignature} id="outlined-basic" label="Sheet Signature" variant="outlined" />
        </Typography>
        <Typography sx={{ color: 'text.secondary', mb: 1.5 }}>Body</Typography>
        <Typography variant="body2">
          <Spreadsheet data={data} onChange={setData} />
        </Typography>
      </CardContent>
      {!isResult && (
        <CardActions>
          <Button size="small" onClick={(event) => addRow()}>Add Row</Button>
          <Button size="small" onClick={(event) => addColumn()}>Add Column</Button>
          <Button size="small" onClick={(event) => removeRow()}>Remove Row</Button>
          <Button size="small" onClick={(event) => removeColumn()}>Remove Column</Button>

          <Button size="small" onClick={(event) => addInvocation()}>Add Invocation (Parameterized Sheet)</Button>
        </CardActions>
      )}

    </React.Fragment>
  );

  return (
    <Box sx={{ minWidth: 275 }}>
      <Card variant="outlined">{card}</Card>
    </Box>
  );

}

export default Sheet;
