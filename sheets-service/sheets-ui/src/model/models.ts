export class SheetRequest {
    sheets!: SheetSpec[]
    classesUnderTest!: ClassUnderTestSpec[]
    analyzers!: string[]
    adaptationStrategy!: string
    adapterLimit!:number
}

export class SheetGenerationRequest {
    sheets!: SheetSpec[]
    classesUnderTest!: ClassUnderTestSpec[]
    testGenerator!: string
}

export class SheetSpec {
    signature!: string
    interfaceSpecification!: string
    body!: string
    invocations!: string[];
    implementationId!: string
}

export class ClassUnderTestSpec {
    id!: string
    className!: string
    artifacts!: string[]
}

export class TestResult {
    executionId!: string
    status!: string

    executedImplementations!: ClassUnderTestSpec[]
    executedTests!: SheetSpec[]

    actuationSheets!: SheetSpec[]
    adaptedActuationSheets!: SheetSpec[]
    metricSheets!: SheetSpec[]
    oracleSheets!: SheetSpec[]
    srmViews!: SheetSpec[]
}

export class SheetResponse {
    executionId!: string
    status!: string

    testResults!: TestResult[]
}

export class SheetGenerationResponse {
    sheets!: SheetSpec[]
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

export class CodeSearchRequest {
    interfaceSpecification!: string
    dataSource!: string

    limit!: number
}

export class CodeSearchResponse {
    classResults!: ClassUnderTestSpec[]
}

export class CodeGenerationRequest {
    prompt!: string
    model!: string

    sampleSize!: number
}

export class LqlGenerationRequest {
    prompt!: string
    model!: string

    sampleSize!: number
}

export class CodeGenerationResponse {
    classResults!: ClassUnderTestSpec[]
}

export class LqlGenerationResponse {
    lql!: string
}

// ui models

export class StimulusSheet {
    signature!: string;
    data!: any[][];
    invocations!: string[];
}

// experimental
export class FunctionalAbstractionRaw {
    interfaceSignature!: string;
}

export class CodeModuleRaw {
    id!: string
    className!: string
    artifacts!: string[]
}

export class StimulusSheetRaw {
    signature!: string;
    body!: string
    invocations!: string[];
}

export class StimulusMatrixRaw {
    abstraction!: FunctionalAbstractionRaw;
    codeModules!: CodeModuleRaw[];
    tests!: StimulusSheetRaw[];
}