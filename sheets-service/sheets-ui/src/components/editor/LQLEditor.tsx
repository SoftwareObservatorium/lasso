/// <reference path="../../../node_modules/monaco-editor/monaco.d.ts" />
import React, { useRef } from 'react';
import './LQLEditor.css';
import Editor from '@monaco-editor/react';
import { parseLqlErrors } from './LQLErrorListener';

const LQLEditor = ({ editorHandler, lqlHandler, defaultLqlCode }: any) => {
  const editorRef = useRef<any>(null)
  const monacoRef = useRef<any>(null)

  function handleEditorDidMount(editor: any, monaco: any) {
    monaco.languages.register({ id: 'lql' })

    monacoRef.current = monaco
    editorRef.current = editor

    lqlHandler(editorRef.current.getValue())

    // send ref so that other components can set value
    editorHandler(editor)

  }

  const onLqlValidate = (lql: string | undefined, ev: monaco.editor.IModelContentChangedEvent) => {
    if(lql) {
      const errors = parseLqlErrors(lql)

      monacoRef.current.editor.setModelMarkers(editorRef.current.getModel(), 'owner', errors.errors);

      console.log(JSON.stringify(errors))
    }

    lqlHandler(lql)
  }

  return (
    <>
      <h2>LQL Interface Specification</h2>
      <Editor
        height="200px"
        defaultLanguage="lql"
        defaultValue={defaultLqlCode}
        onMount={handleEditorDidMount}
        onChange={onLqlValidate}
      />
    </>
  )
}

export default LQLEditor