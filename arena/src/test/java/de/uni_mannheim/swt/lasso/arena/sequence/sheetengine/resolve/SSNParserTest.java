package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 *
 * @author Marcus Kessel
 */
public class SSNParserTest {

    @Test
    public void testParse_incomplete() throws IOException {
        String signatureLql = "testBoundedQueue()";
        @Language("jsonl")
        String bodyJsonl = """
                {"sheet":"Sheet 1","header":"Row 1","cells":{"A1":{},"B1":"create","C1":"BoundedQueue","D1":10}}
                {"sheet":"Sheet 1","header":"Row 2","cells":{"A2":{},"B2":"enQueue","C2":"A1","D2":"'Hello World!'"}}
                {"sheet":"Sheet 1","header":"Row 3","cells":{"B3":"isEmpty","C3":"A1"}}
                {"sheet":"Sheet 1","header":"Row 4","cells":{"B4":"isFull","C4":"A1"}}
                {"sheet":"Sheet 1","header":"Row 5","cells":{"A5":"D2","B5":"deQueue","C5":"A1"}}
                {"sheet":"Sheet 1","header":"Row 6","cells":{"A6":true,"B6":"isEmpty","C6":"A1"}}
                """;
        String lql = """
                BoundedQueue {
                    BoundedQueue(int)
                    enQueue(java.lang.Object)->void
                    deQueue()->java.lang.Object
                    isEmpty()->boolean
                    isFull()->boolean
                }
                """;

        SSNParser ssnParser = new SSNParser();

        ParsedSheet parsedSheet = ssnParser.parseJsonl(bodyJsonl, signatureLql, lql);
        assertEquals("testBoundedQueue", parsedSheet.getName());
        assertEquals(6, parsedSheet.getRows().size());

        assertEquals(4, parsedSheet.getRows().get(0).getCells().size());
        assertEquals(4, parsedSheet.getRows().get(1).getCells().size());
        assertEquals(2, parsedSheet.getRows().get(2).getCells().size());

        // must be 1!
        assertEquals(1, parsedSheet.getRows().get(2).getInputs().size());
    }

    @Test
    public void testParse_stack() throws IOException {
        String signatureLql = "testStack()";
        String bodyJsonl = "{\"sheet\": \"Sheet 1\", \"header\": \"Row 1\", \"cells\": {\"A1\": null, \"B1\": \"create\", \"C1\": \"Stack\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 2\", \"cells\": {\"A2\": null, \"B2\": \"push\", \"C2\": \"Hello World!\"}}\n" +
                "{\"sheet\": \"Sheet 2\", \"header\": \"Row 1\", \"cells\": {\"A3\": 2, \"B3\": \"size\", \"C3\": null}}";
        String interfaceLql = """
                Stack {
                    push(java.lang.String)->java.lang.String
                    size()->int
                }
                """;

        SSNParser ssnParser = new SSNParser();

        ParsedSheet parsedSheet = ssnParser.parseJsonl(bodyJsonl, signatureLql, interfaceLql);

        assertEquals("testStack", parsedSheet.getName());
        assertEquals(3, parsedSheet.getRows().size());

        assertEquals(3, parsedSheet.getRows().get(0).getCells().size());
        assertEquals(3, parsedSheet.getRows().get(1).getCells().size());
        assertEquals(3, parsedSheet.getRows().get(2).getCells().size());

        assertNotNull(parsedSheet.getSignature());
        assertNotNull(parsedSheet.getInterfaceSpecification());
    }

    @Test
    public void testParse_stack_fail() throws IOException {
        String bodyJsonl = "{\"sheet\": \"Sheet 1\", \"header\": \"Row 1\", \"cells\": {\"A1\": null, \"B1\": \"create\", \"C1\": \"Stack\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 2\", \"cells\": {\"A2\": null, \"B2\": \"push\", \"C2\": \"Hello World!\"}}\n" +
                "{\"sheet\": \"Sheet 2\", \"header\": \"Row 1\", \"cells\": {\"A3\": 2, \"B3\": \"size\", \"C3\": null}}";

        SSNParser ssnParser = new SSNParser();

        assertThrows(NullPointerException.class, () -> {
            ParsedSheet parsedSheet = ssnParser.parseJsonl(bodyJsonl, null, null);
        });
    }
}
