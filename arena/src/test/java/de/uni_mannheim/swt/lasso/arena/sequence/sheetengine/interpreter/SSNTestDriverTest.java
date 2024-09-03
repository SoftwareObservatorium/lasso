package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 *
 * @author Marcus Kessel
 */
public class SSNTestDriverTest {

    private static final Logger LOG = LoggerFactory.getLogger(SSNTestDriverTest.class);

    /**
     * Multi-object
     *
     * <code>
     *     CUT = Stack
     *     java.lang.String
     * </code>
     *
     */
    @Test
    public void test_Stack() throws IOException {
        String ssnJsonlStr = "{\"sheet\": \"Sheet 1\", \"header\": \"Row 1\", \"cells\": {\"A1\": null, \"B1\": \"create\", \"C1\": \"Stack\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 2\", \"cells\": {\"A2\": null, \"B2\": \"create\", \"C2\": \"java.lang.String\", \"D2\": \"'Hello World!'\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 3\", \"cells\": {\"A3\": null, \"B3\": \"push\", \"C3\": \"A1\", \"D3\": \"A2\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 3\", \"cells\": {\"A4\": null, \"B4\": \"size\", \"C4\": \"A1\"}}\n";

        String lql = "Stack {\n" +
                "push(java.lang.String)->java.lang.String\n" +
                "size()->int\n" +
                "}";

        SSNTestDriver testDriver = new SSNTestDriver();
        ExecutedInvocations executedInvocations = testDriver.runSheet(ssnJsonlStr, lql, Stack.class, 1);
        LOG.debug("executed invocations\n{}", executedInvocations);
    }

    /**
     * Special commands in SSN
     *
     * <code>
     *     $create - create instance
     *     $eval - evaluate code expression
     * </code>
     *
     * @throws IOException
     */
    @Test
    public void test_Stack_code() throws IOException {
        String ssnJsonlStr = "{\"sheet\": \"Sheet 1\", \"header\": \"Row 1\", \"cells\": {\"A1\": null, \"B1\": \"create\", \"C1\": \"Stack\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 2\", \"cells\": {\"A2\": null, \"B2\": \"$eval\", \"C2\": \"Arrays.toString(new char[]{'a', 'b'})\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 3\", \"cells\": {\"A3\": null, \"B3\": \"push\", \"C3\": \"A1\", \"D3\": \"A2\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 3\", \"cells\": {\"A3\": null, \"B3\": \"size\", \"C3\": \"A1\"}}\n";

        String lql = "Stack {\n" +
                "push(java.lang.String)->java.lang.String\n" +
                "size()->int\n" +
                "}";

        Objects.toString(new char[]{'a', 'b'});

        SSNTestDriver testDriver = new SSNTestDriver();
        ExecutedInvocations executedInvocations = testDriver.runSheet(ssnJsonlStr, lql, Stack.class, 1);
        LOG.debug("executed invocations\n{}", executedInvocations);
    }

    @Test
    public void test_noop_static() throws IOException {
        // FIXME
    }
}
