import React, { useRef } from 'react';
import './LQLEditor.css';
import Editor from '@monaco-editor/react';

const LQLEditor = ({lqlHandler, defaultLqlCode} : any) => {
  const editorRef = useRef<any>(null)

  function handleEditorDidMount(editor: any, monaco: any) {
    editorRef.current = editor
    
    lqlHandler(editorRef.current.getValue())
  }

  return (
    <>
          <h2>LQL editor</h2>
      <Editor
        height="200px"
        defaultLanguage="java"
        defaultValue={defaultLqlCode}
        onMount={handleEditorDidMount}
        onChange={lqlHandler}
      />
    </>
  )
}

export default LQLEditor