import React, { useRef, useState } from 'react';
import './ClassUnderTest.css';
import { TextField } from '@mui/material';

const ClassUnderTest = ({ cutHandler }: any) => {

  const [className, setClassName] = useState("java.util.Stack");
  const [artifacts, setArtifacts] = useState("");

  const toArtifacts = () => {
    // FIXME comma-separated
    return [artifacts]
  };

  cutHandler(className, toArtifacts())

  const onChangeClassname = (e: any) => {
    setClassName(e.target.value);

    cutHandler(className, toArtifacts())
  };

  const onChangeArtifacts = (e: any) => {
    setArtifacts(e.target.value);

    cutHandler(className, toArtifacts())
  };

  return (
    <>
      <h2>Class Under Test</h2>
      <TextField onChange={onChangeClassname} value={className} id="outlined-basic" label="Class Under Test" variant="outlined" />
      <TextField onChange={onChangeArtifacts} value={artifacts} id="outlined-basic" label="Artifacts" variant="outlined" />
    </>
  )
}

export default ClassUnderTest