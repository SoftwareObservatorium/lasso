import React, { useRef, useState } from 'react';
import { StimulusMatrixRaw } from '../model/models';
import { Box, Container, CssBaseline, Divider, Paper, Stack, styled } from '@mui/material';

import Grid from '@mui/material/Grid2';
import LQLEditor from '../components/editor/LQLEditor';

// TODO remove
const Item = styled(Paper)(({ theme }) => ({
    backgroundColor: '#fff',
    ...theme.typography.body2,
    padding: theme.spacing(1),
    textAlign: 'center',
    color: theme.palette.text.secondary,
    ...theme.applyStyles('dark', {
        backgroundColor: '#1A2027',
    }),
}));

const TestPage = () => {
    // example data
    const [stimulusMatrix, setStimulusMatrix] = useState<StimulusMatrixRaw>({
        abstraction: {
            interfaceSignature: `BoundedQueue {
    BoundedQueue(int)
    enQueue(java.lang.Object)->void
    deQueue()->java.lang.Object
    isEmpty()->boolean
    isFull()->boolean
}`},
        codeModules: [
            {
                className: "demo_examples.BoundedQueue",
                artifacts: [""]
            }
        ],
        tests: [
            {
                signature: "",
                body: `
{"cells": {"A1": {}, "B1": "create", "C1": "BoundedQueue", "D1": 10}}
{"cells": {"A2": {}, "B2": "enQueue", "C2": "A1", "D2": "'Hello World!'"}}
{"cells": {"A3": {}, "B3": "isEmpty", "C3": "A1"}}
{"cells": {"A4": {}, "B4": "isFull", "C4": "A1"}}
{"cells": {"A5": "D2", "B5": "deQueue", "C5": "A1"}}
{"cells": {"A6": {}, "B6": "isEmpty", "C6": "A1"}}
`,
                invocations: []
            }
        ]
    });
    const lqlEditorRef = useRef<any>(null)
    const [interfaceSpecification, setInterfaceSpecification] = useState<string>("");

    // get LQL
    const lqlHandler = (lql: string) => {
        console.log("lql handler " + lql)

        setInterfaceSpecification(lql)
    }

    // get LQL editor (monaco)
    const lqlEditorHandler = (editor: any) => {
        //console.log("lql editor handler " + editor)

        lqlEditorRef.current = editor
    }





    return (
        <div>
            <h2>Editor</h2>
            
            <Grid container spacing={2}>
                <Grid size={8} container spacing={2}>
                    <Grid size={8}>
                        <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
                            <Divider>Interface Specification (LQL)</Divider>
                            <LQLEditor editorHandler={lqlEditorHandler} lqlHandler={lqlHandler} defaultLqlCode={stimulusMatrix.abstraction.interfaceSignature} />
                        </Box>
                    </Grid>
                    <Grid size={4}>
                        <Item>CUTs</Item>
                    </Grid>
                    <Grid size={12}>
                        <Item>Sheets</Item>
                    </Grid>
                </Grid>
                <Grid size={4}><Item>Stimulus Matrix</Item></Grid>
            </Grid>
        </div>
    );
}

export default TestPage;
