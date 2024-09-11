import React from 'react';
import Markdown from '../components/markdown/Markdown';

function getPath(absolutePath: string) {
  return process.env.PUBLIC_URL + absolutePath
}

function DocumentationPage() {
  return (
    <div className="App">
      <h1>Documentation</h1>

      <Markdown mdpath={getPath('/doc/lql_syntax.md')} />

      <Markdown mdpath={getPath('/doc/ssn_syntax.md')} />
    </div>
  );
}

export default DocumentationPage;
