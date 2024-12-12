import { useState, useEffect, useCallback } from "react";
import Spreadsheet, { CellBase, Matrix } from "react-spreadsheet";
import "./Sheet.css";
import DoneIcon from '@mui/icons-material/Done';
import ApiIcon from '@mui/icons-material/Api';
import DeleteIcon from '@mui/icons-material/Delete';
import Button from "@mui/material/Button";
import { Avatar, Box, Card, CardActions, CardContent, Divider, IconButton, InputBase, List, ListItem, ListItemAvatar, ListItemText, Paper, TextField, Typography } from "@mui/material";
import React from "react";

import Grid from '@mui/material/Grid';

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

  const [sheetInvocations, setSheetInvocations] = React.useState<string[]>([]);
  const [sheetInvocation, setSheetInvocation] = React.useState<string>();

  useEffect(() => {
    console.log(data);
    changeHandler(sheetId, sheetSignature, data, sheetInvocations)

  }, [sheetSignature, data, sheetInvocations]);

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

  // add sheet invocation
  const addInvocation = () => {
    console.log("invocation "  + sheetInvocation)

    let my = sheetInvocation!;

    setSheetInvocations([...sheetInvocations, my]);
  }

  const handleInvocationChange = (event: any) => {
    setSheetInvocation(event.target.value)
  }

  const removeInvocation = (invocation: string) => {
    let my = sheetInvocations.filter(obj => obj !== invocation);
    setSheetInvocations(my);
  }

  const card = (
    <React.Fragment>
      <CardContent>
        <Typography gutterBottom sx={{ color: 'text.secondary', fontSize: 14 }}>
          Sheet
        </Typography>
        <Typography variant="h5" component="div">
        <TextField onChange={onChangeSheetSignature} value={sheetSignature} id="outlined-basic" label="Sheet Signature" variant="outlined" />
        </Typography>
        <Typography sx={{ color: 'text.secondary', mb: 1.5 }}>Body</Typography>
        <Typography variant="body2">
          <Spreadsheet data={data} onChange={setData} />
        </Typography>

        {!isResult && (
          <Grid container spacing={2} sx={{ justifyContent: 'center' }}>
                  <Grid item xs={12} md={6}>
                    <Typography gutterBottom sx={{ color: 'text.secondary', fontSize: 14 }}>
                    Add Invocations (Parameterization)
                    </Typography>
                    <Paper
                      component="form"
                      sx={{ p: '2px 4px', display: 'flex', alignItems: 'center', width: 400 }}
                    >
                      <InputBase
                        sx={{ ml: 1, flex: 1 }}
                        placeholder="Add Invocation (comma separated list)"
                        value={sheetInvocation}
                        onChange={(event) => handleInvocationChange(event)}
                        inputProps={{ 'aria-label': 'Add Invocation' }}
                      />
                            <Divider sx={{ height: 28, m: 0.5 }} orientation="vertical" />
                      <IconButton type="button" sx={{ p: '10px' }} aria-label="add" onClick={() => addInvocation()}>
                        <DoneIcon />
                      </IconButton>
                    </Paper>
                      <List dense={true}>
                      {sheetInvocations.map(invocation => {
                          return (
                            <ListItem
                            secondaryAction={
                              <IconButton edge="end" aria-label="delete" onClick={() => removeInvocation(invocation)}>
                                <DeleteIcon />
                              </IconButton>
                            }
                          >
                          <ListItemAvatar>
                            <Avatar>
                              <ApiIcon />
                            </Avatar>
                          </ListItemAvatar>
                          <ListItemText
                            primary={invocation}
                          />
                        </ListItem>
                          );
                      })}
                    </List>
                    </Grid></Grid>
        )}
      </CardContent>
      {!isResult && (
        <CardActions>
          <Button size="small" onClick={(event) => addRow()}>Add Row</Button>
          <Button size="small" onClick={(event) => addColumn()}>Add Column</Button>
          <Button size="small" onClick={(event) => removeRow()}>Remove Row</Button>
          <Button size="small" onClick={(event) => removeColumn()}>Remove Column</Button>
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
