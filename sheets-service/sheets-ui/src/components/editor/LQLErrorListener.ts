import { ANTLRErrorListener, ANTLRInputStream, CommonTokenStream, RecognitionException, Recognizer } from "antlr4ts";
import { LQLLexer } from "../../antlr-lql/LQLLexer";
import { LQLParser } from "../../antlr-lql/LQLParser";

export class LQLError {
    startLineNumber: number;
    endLineNumber: number;
    startColumn: number;
    endColumn: number;
    message: string;

    constructor(startLineNumber: number, endLineNumber: number, startColumn: number, endColumn: number, message: string) {
        this.startLineNumber = startLineNumber;
        this.endLineNumber = endLineNumber;
        this.startColumn = startColumn;
        this.endColumn = endColumn;
        this.message = message;
    }
}

export class LQLErrorListener implements ANTLRErrorListener<any> {

    private errors: LQLError[] = []

    syntaxError(recognizer: Recognizer<any, any>, offendingSymbol: any, line: number, charPositionInLine: number, message: string, e: RecognitionException | undefined): void {
        var endColumn = charPositionInLine + 1;

        if (offendingSymbol) {
            endColumn = charPositionInLine + offendingSymbol.length;
        }
        
        this.errors.push(
            {
                startLineNumber: line,
                endLineNumber: line,
                startColumn: charPositionInLine,
                endColumn: endColumn,
                message: message
            }
        )
    }

    getErrors(): LQLError[] {
        return this.errors;
    }
}

export function parseLqlErrors(lql: string): { errors: LQLError[] } {
    const inputStream = new ANTLRInputStream(lql);
    const lexer = new LQLLexer(inputStream);
    lexer.removeErrorListeners()
    const errorsListener = new LQLErrorListener();
    lexer.addErrorListener(errorsListener);
    const tokenStream = new CommonTokenStream(lexer);
    const parser = new LQLParser(tokenStream);
    parser.removeErrorListeners();
    parser.addErrorListener(errorsListener);
    var ast = parser.parse()
    console.log(ast);
    const errors: LQLError[] = errorsListener.getErrors();

    return { errors };
}