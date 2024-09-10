export class SheetRequest {
    sheets!: SheetSpec[]
    classUnderTest!: ClassUnderTestSpec
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

export class SheetResponse {
    executionId!: string
    status!: string

    actuationSheets!: SheetSpec[]
    adaptedActuationSheets!: SheetSpec[]
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