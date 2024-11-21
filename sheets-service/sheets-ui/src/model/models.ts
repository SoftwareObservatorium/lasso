export class SheetRequest {
    sheets!: SheetSpec[]
    classesUnderTest!: ClassUnderTestSpec[]
}

export class SheetSpec {
    name!: string
    interfaceSpecification!: string
    body!: string
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