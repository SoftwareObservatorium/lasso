package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.examples.InvisibleStaticMethodExample;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.examples.StackEmptyConstructorExample;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.examples.StackNonEmptyConstructorExample;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.examples.StaticMethodExample;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
     *     CUT = Stack (empty constructor)
     *     java.lang.String
     * </code>
     *
     */
    @Test
    public void test_Stack_empty_constructor() throws IOException, ClassNotFoundException {
        String ssnJsonlStr = "{\"sheet\": \"Sheet 1\", \"header\": \"Row 1\", \"cells\": {\"A1\": null, \"B1\": \"create\", \"C1\": \"Stack\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 2\", \"cells\": {\"A2\": null, \"B2\": \"create\", \"C2\": \"java.lang.String\", \"D2\": \"'Hello World!'\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 3\", \"cells\": {\"A3\": null, \"B3\": \"push\", \"C3\": \"A1\", \"D3\": \"A2\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 3\", \"cells\": {\"A4\": null, \"B4\": \"size\", \"C4\": \"A1\"}}\n";

        String lql = "Stack {\n" +
                "push(java.lang.String)->java.lang.String\n" +
                "size()->int\n" +
                "}";
        ExecutionListener executionListener = new ExecutionListener();

        Class cutClass = StackEmptyConstructorExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        ExecutedInvocations executedInvocations = testDriver.runSheet(ssnJsonlStr, lql, cutClass, 1, executionListener);
        LOG.debug("executed invocations\n{}", executedInvocations);
        Invocations invocations = executedInvocations.getInvocations();

        assertEquals(4, invocations.getSequence().size());
        assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(0).getTargetClass());
        assertEquals(0, invocations.getInvocation(0).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("java.lang.String"), invocations.getInvocation(1).getTargetClass());
        assertEquals(1, invocations.getInvocation(1).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(2).getTargetClass());
        assertEquals(1, invocations.getInvocation(2).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(3).getTargetClass());
        assertEquals(0, invocations.getInvocation(3).getParameters().size());

        assertEquals(4, executedInvocations.getSequence().size());
    }

    /**
     * Multi-object
     *
     * <code>
     *     CUT = Stack (non-empty constructor)
     *     java.lang.String
     * </code>
     *
     */
    @Test
    public void test_Stack_nonempty_constructor() throws IOException, ClassNotFoundException {
        String ssnJsonlStr = "{\"sheet\": \"Sheet 1\", \"header\": \"Row 1\", \"cells\": {\"A1\": null, \"B1\": \"create\", \"C1\": \"Stack\", \"D1\": 10}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 2\", \"cells\": {\"A2\": null, \"B2\": \"create\", \"C2\": \"java.lang.String\", \"D2\": \"'Hello World!'\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 3\", \"cells\": {\"A3\": null, \"B3\": \"push\", \"C3\": \"A1\", \"D3\": \"A2\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 3\", \"cells\": {\"A4\": null, \"B4\": \"size\", \"C4\": \"A1\"}}\n";

        String lql = "Stack {\n" +
                "Stack(int)\n" +
                "push(java.lang.String)->java.lang.String\n" +
                "size()->int\n" +
                "}";
        ExecutionListener executionListener = new ExecutionListener();

        Class cutClass = StackNonEmptyConstructorExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        ExecutedInvocations executedInvocations = testDriver.runSheet(ssnJsonlStr, lql, cutClass, 1, executionListener);
        LOG.debug("executed invocations\n{}", executedInvocations);
        Invocations invocations = executedInvocations.getInvocations();

        assertEquals(4, invocations.getSequence().size());
        assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(0).getTargetClass());
        assertEquals(1, invocations.getInvocation(0).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("java.lang.String"), invocations.getInvocation(1).getTargetClass());
        assertEquals(1, invocations.getInvocation(1).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(2).getTargetClass());
        assertEquals(1, invocations.getInvocation(2).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(3).getTargetClass());
        assertEquals(0, invocations.getInvocation(3).getParameters().size());

        assertEquals(4, executedInvocations.getSequence().size());
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
    public void test_Stack_code() throws IOException, ClassNotFoundException {
        String ssnJsonlStr = "{\"sheet\": \"Sheet 1\", \"header\": \"Row 1\", \"cells\": {\"A1\": null, \"B1\": \"create\", \"C1\": \"Stack\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 2\", \"cells\": {\"A2\": null, \"B2\": \"$eval\", \"C2\": \"Arrays.toString(new char[]{'a', 'b'})\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 3\", \"cells\": {\"A3\": null, \"B3\": \"push\", \"C3\": \"A1\", \"D3\": \"A2\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 3\", \"cells\": {\"A3\": null, \"B3\": \"size\", \"C3\": \"A1\"}}\n";

        String lql = "Stack {\n" +
                "push(java.lang.String)->java.lang.String\n" +
                "size()->int\n" +
                "}";
        ExecutionListener executionListener = new ExecutionListener();

        Class cutClass = StackEmptyConstructorExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        ExecutedInvocations executedInvocations = testDriver.runSheet(ssnJsonlStr, lql, cutClass, 1, executionListener);
        LOG.debug("executed invocations\n{}", executedInvocations);
        Invocations invocations = executedInvocations.getInvocations();

        assertEquals(4, invocations.getSequence().size());
        assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(0).getTargetClass());
        assertEquals(0, invocations.getInvocation(0).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("java.lang.String"), invocations.getInvocation(1).getTargetClass());
        assertEquals(0, invocations.getInvocation(1).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(2).getTargetClass());
        assertEquals(1, invocations.getInvocation(2).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(3).getTargetClass());
        assertEquals(0, invocations.getInvocation(3).getParameters().size());

        assertEquals(4, executedInvocations.getSequence().size());
    }

    /**
     * A CUT class with a static method.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void test_static() throws IOException, ClassNotFoundException {
        String ssnJsonlStr = "{\"sheet\": \"Sheet 1\", \"header\": \"Row 1\", \"cells\": {\"A1\": null, \"B1\": \"create\", \"C1\": \"SingletonExample\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 2\", \"cells\": {\"A2\": null, \"B2\": \"sum\", \"C2\": \"A1\", \"D2\": 2, \"E2\": 3}}\n";

        String lql = "SingletonExample {\n" +
                "sum(int,int)->int\n" +
                "}";
        ExecutionListener executionListener = new ExecutionListener();

        Class cutClass = StaticMethodExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        ExecutedInvocations executedInvocations = testDriver.runSheet(ssnJsonlStr, lql, cutClass, 1, executionListener);
        LOG.debug("executed invocations\n{}", executedInvocations);
        Invocations invocations = executedInvocations.getInvocations();

        assertEquals(2, invocations.getSequence().size());
        assertEquals(invocations.getEval().resolveClass("SingletonExample"), invocations.getInvocation(0).getTargetClass());
        assertEquals(0, invocations.getInvocation(0).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("SingletonExample"), invocations.getInvocation(1).getTargetClass());

        assertEquals(2, executedInvocations.getSequence().size());
    }

    /**
     * A CUT class with a static method and non-visible constructor.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void test_static_invisible() throws IOException, ClassNotFoundException {
        String ssnJsonlStr = "{\"sheet\": \"Sheet 1\", \"header\": \"Row 1\", \"cells\": {\"A1\": null, \"B1\": \"create\", \"C1\": \"SingletonExample\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 2\", \"cells\": {\"A2\": null, \"B2\": \"sum\", \"C2\": \"A1\", \"D2\": 2, \"E2\": 3}}\n";

        String lql = "SingletonExample {\n" +
                "sum(int,int)->int\n" +
                "}";
        ExecutionListener executionListener = new ExecutionListener();

        Class cutClass = InvisibleStaticMethodExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        ExecutedInvocations executedInvocations = testDriver.runSheet(ssnJsonlStr, lql, cutClass, 1, executionListener);
        LOG.debug("executed invocations\n{}", executedInvocations);
        Invocations invocations = executedInvocations.getInvocations();

        assertEquals(2, invocations.getSequence().size());
        assertEquals(invocations.getEval().resolveClass("SingletonExample"), invocations.getInvocation(0).getTargetClass());
        assertEquals(0, invocations.getInvocation(0).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("SingletonExample"), invocations.getInvocation(1).getTargetClass());

        assertEquals(2, executedInvocations.getSequence().size());
    }
}
