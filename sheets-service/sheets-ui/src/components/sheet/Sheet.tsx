import { useState, useEffect, useCallback } from "react";
import Spreadsheet, { CellBase, Matrix } from "react-spreadsheet";
import "./Sheet.css";
import ButtonGroup from "@mui/material/ButtonGroup";
import Button from "@mui/material/Button";
import { TextField } from "@mui/material";

const Sheet = ({ defaultSheetName, sheetData, executeHandler, isResult }: any) => {
  const [sheetName, setSheetName] = useState(defaultSheetName)
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
  }, [data]);

  const onChangeSheetName = (e: any) => {
    setSheetName(e.target.value);
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

  return (
    <div className="Sheet">

      {!isResult && (
        <>
          <h2>Stimulus Sheet editor</h2>
          <ButtonGroup variant="outlined" aria-label="Basic button group">
            <Button onClick={(event) => addRow()}>Add Row</Button>
            <Button onClick={(event) => addColumn()}>Add Column</Button>
            <Button onClick={(event) => removeRow()}>Remove Row</Button>
            <Button onClick={(event) => removeColumn()}>Remove Column</Button>
          </ButtonGroup>

          <br></br>
        </>
      )}

      <TextField onChange={onChangeSheetName} value={sheetName} id="outlined-basic" label="Sheet Name" variant="outlined" />

      <Spreadsheet data={data} onChange={setData} />

      {!isResult && (
        <>
          <br></br>
          <ButtonGroup variant="outlined" aria-label="Basic button group">
            <Button onClick={(event) => executeHandler(sheetName, data)}>Execute Sheet</Button>
          </ButtonGroup>
        </>
      )}

    </div>
  );

}

export default Sheet;
