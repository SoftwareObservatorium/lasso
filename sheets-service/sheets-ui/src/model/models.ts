export class SheetRequest {
    sheets!: SheetSpec[]
    classesUnderTest!: ClassUnderTestSpec[]
    analyzers!: string[]
}

export class SheetSpec {
    signature!: string
    interfaceSpecification!: string
    body!: string
    implementation!: string
}

export class ClassUnderTestSpec {
    className!: string
    artifacts!: string[]
}

export class TestResult {
    executionId!: string
    status!: string
    classUnderTest!: ClassUnderTestSpec

    actuationSheets!: SheetSpec[]
    adaptedActuationSheets!: SheetSpec[]
}

export class SheetResponse {
    executionId!: string
    status!: string

    testResults!: TestResult[]
}

export class User {
    //id: number;
    username!: string;
    password!: string;
    //firstName: string;
    //lastName: string;
    email!: string;

    token?: string;

    // roles
    roles!: string[];
}

// ui models

export class StimulusSheet {
    signature!: string;
    data!: any[][];
}