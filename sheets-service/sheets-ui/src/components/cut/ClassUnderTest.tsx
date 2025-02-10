import React, { useRef, useState } from 'react';
import './ClassUnderTest.css';
import { Box, Button, ButtonGroup, Divider, IconButton, TextField } from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';

const ClassUnderTest = ({ detectInterfaceHandler, removeHandler, cutHandler, cutId, classUnderTest }: any) => {

  const [className, setClassName] = useState(classUnderTest.className);
  const [artifacts, setArtifacts] = useState(classUnderTest.artifacts[0]);

  const toArtifacts = () => {
    // FIXME comma-separated
    return [artifacts]
  };

  //cutHandler(className, toArtifacts())

  const onChangeClassname = (e: any) => {
    setClassName(e.target.value);

    cutHandler(cutId, className, toArtifacts())
  };

  const onChangeArtifacts = (e: any) => {
    setArtifacts(e.target.value);

    cutHandler(cutId, className, toArtifacts())
  };

  return (
    <Box sx={{ minWidth: 275, padding: 1 }}>
      {/* <h2>Class Under Test</h2> */}
      <TextField onChange={onChangeClassname} value={className} id="outlined-basic" label="Class Name (fully qualified)" variant="outlined" />
      <TextField onChange={onChangeArtifacts} value={artifacts} id="outlined-basic" label="Artifacts" variant="outlined" />

      {classUnderTest.codeUnit ? <TextField fullWidth multiline maxRows={10} value={classUnderTest.codeUnit.content} id="outlined-basic" label="Code" variant="outlined" /> : null}

      <Divider>Actions</Divider>
      <ButtonGroup variant="outlined" aria-label="Basic button group">
        <Button onClick={(event) => detectInterfaceHandler(cutId)}>Detect (Declared) Interface</Button>
      </ButtonGroup>
      <IconButton edge="end" aria-label="delete" onClick={() => removeHandler(cutId)}>
        <DeleteIcon />
      </IconButton>
    </Box>
  )
}

export default ClassUnderTest