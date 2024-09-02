package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 *
 * @author Marcus Kessel
 */
public class SSNParserTest {

    @Test
    public void testParse_stack() throws IOException {
        String ssnJsonlStr = "{\"sheet\": \"Sheet 1\", \"header\": \"Row 1\", \"cells\": {\"A1\": null, \"B1\": \"create\", \"C1\": \"Stack\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 2\", \"cells\": {\"A2\": null, \"B2\": \"push\", \"C2\": \"Hello World!\"}}\n" +
                "{\"sheet\": \"Sheet 2\", \"header\": \"Row 1\", \"cells\": {\"A3\": 2, \"B3\": \"size\", \"C3\": null}}";

        SSNParser ssnParser = new SSNParser();

        ParsedSheet parsedSheet = ssnParser.parseJsonl(ssnJsonlStr);

        assertEquals("Sheet 1", parsedSheet.getName());
        assertEquals(3, parsedSheet.getRows().size());

        assertEquals(3, parsedSheet.getRows().get(0).getCells().size());
        assertEquals(3, parsedSheet.getRows().get(1).getCells().size());
        assertEquals(3, parsedSheet.getRows().get(2).getCells().size());

    }
}
