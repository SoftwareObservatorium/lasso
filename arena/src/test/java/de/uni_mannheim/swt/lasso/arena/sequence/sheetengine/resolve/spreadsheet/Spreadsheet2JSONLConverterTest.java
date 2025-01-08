package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.spreadsheet;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.SSNParser;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 *
 * @author Marcus Kessel
 */
public class Spreadsheet2JSONLConverterTest {

    @Test
    public void testConvert_BoundedQueue() throws IOException {
        FileInputStream fis = new FileInputStream("sheets/jsonl/BoundedQueue.xlsx");
        String bodyJsonl = Spreadsheet2JSONLConverter.convert(fis);

        System.out.println(bodyJsonl);

        String signatureLql = "testBoundedQueue()";
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
        assertEquals(3, parsedSheet.getRows().get(2).getCells().size());

        // must be 1!
        assertEquals(1, parsedSheet.getRows().get(2).getInputs().size());

        assertEquals(true, parsedSheet.getRows().get(2).getOutput().getNodeValue().isObject());
    }
}
