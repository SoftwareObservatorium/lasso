// examples
export class Examples {
    static MAP = {
        BASE64_ENCODE: {
            label: "Base64 Encode Decode",
            scenario:{
                abstraction: {
                    interfaceSignature: `Base64{
    encode(byte[])->byte[]
    decode(java.lang.String)->byte[]
}`,
                    description: "Base64 encoding and decoding",
},
                codeModules: [
                    {
                        id: crypto.randomUUID(),
                        className: "org.apache.commons.codec.binary.Base64",
                        artifacts: ["commons-codec:commons-codec:1.15"]
                    },
                    {
                        id: crypto.randomUUID(),
                        className: "org.apache.commons.codec.binary.Base64",
                        artifacts: ["commons-codec:commons-codec:1.17"]
                    }
                ],
                tests: [
                    {
                        signature: "testEncode()",
                        body: `
                {"cells": {"A1": {}, "B1": "create", "C1": "Base64"}}
                {"cells": {"A2": {}, "B2": "encode", "C2": "A1", "D2": "\\"Hello World!\\".getBytes()"}}
        `,
                        invocations: []
                    },
                    {
                        signature: "testDecode()",
                        body: `
                {"cells": {"A1": {}, "B1": "create", "C1": "Base64"}}
                {"cells": {"A2": {}, "B2": "decode", "C2": "A1", "D2": "\\"SGVsbG8gV29ybGQh\\""}}
        `,
                        invocations: []
                    }
                ]
            }
        },
        STACK_EMPTY_CONSTRUCTOR: {
            label: "Stack (ArrayList)",
            scenario:{
                abstraction: {
                    interfaceSignature: `Stack {
    push(java.lang.String)->java.lang.String
    size()->int
}`,
                description: "Stack data structure",
},
                codeModules: [
                    {
                        id: crypto.randomUUID(),
                        className: "java.util.Stack",
                        artifacts: [""]
                    },
                    {
                        id: crypto.randomUUID(),
                        className: "java.util.ArrayDeque",
                        artifacts: [""]
                    },
                    {
                        id: crypto.randomUUID(),
                        className: "java.util.LinkedList",
                        artifacts: [""]
                    }
                ],
                tests: [
                    {
                        signature: "test1()",
                        body: `
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Stack"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "create", "C2": "java.lang.String", "D2": "'Hello World!'"}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": {}, "B3": "push", "C3": "A1", "D3": "A2"}}
                {"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": 1, "B4": "size", "C4": "A1"}}
        `,
                        invocations: []
                    }
                ]
            }
        },
        BOUNDED_QUEUE: {
            label: "BoundedQueue (Parameterized)",
            scenario: {
                abstraction: {
                    interfaceSignature: `MyBoundedQueue {
    MyBoundedQueue(int)
    enQueue(java.lang.Object)->void
    deQueue()->java.lang.Object
    isEmpty()->boolean
    isFull()->boolean
}`,
                    description: "Bounded queue data structure",
},
                codeModules: [
                    {
                        id: crypto.randomUUID(),
                        className: "demo_examples.BoundedQueue",
                        artifacts: [""]
                    }
                ],
                tests: [
                    {
                        signature: "test1(p1=int)",
                        body: `
        {"cells": {"A1": {}, "B1": "create", "C1": "MyBoundedQueue", "D1": "?p1"}}
        {"cells": {"A2": {}, "B2": "enQueue", "C2": "A1", "D2": "'Hello World!'"}}
        {"cells": {"A3": {}, "B3": "isEmpty", "C3": "A1"}}
        {"cells": {"A4": {}, "B4": "isFull", "C4": "A1"}}
        {"cells": {"A5": "D2", "B5": "deQueue", "C5": "A1"}}
        {"cells": {"A6": {}, "B6": "isEmpty", "C6": "A1"}}
        `,
                        invocations: ["5"]
                    }
                ]
            }
        },

    };
}