package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.examples.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.GsonMapper;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.ObjectMapperVisitor;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Marcus Kessel
 */
public class SSNTestDriverTest {

    private static final Logger LOG = LoggerFactory.getLogger(SSNTestDriverTest.class);

    @Test
    public void testToLql() {
        Class cutClass = CompositeNodeExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        InterfaceSpecification interfaceSpecification = testDriver.toLQL(cutClass.getCanonicalName(), null);

        LOG.debug("LQL\n{}", interfaceSpecification.toLQL());
    }

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
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Stack"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "create", "C2": "java.lang.String", "D2": "'Hello World!'"}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": {}, "B3": "push", "C3": "A1", "D3": "A2"}}
                {"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": 1, "B4": "size", "C4": "A1"}}
                """;

        String lql = """
                Stack {
                    push(java.lang.String)->java.lang.String
                    size()->int
                }
                """;
        ObjectMapperVisitor visitor = createVisitor();

        Class cutClass = StackEmptyConstructorExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        ExecutedInvocations executedInvocations = testDriver.runSheet(ssnJsonlStr, lql, cutClass, 1, visitor);
        LOG.debug("executed invocations\n{}", executedInvocations);
        visitor.getActuationSheet().debug();
        visitor.getAdaptedActuationSheet().debug();
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
        // test oracle values (first column)
        assertTrue(invocations.getInvocation(0).getExpectedOutput().isUndefined());
        assertTrue(invocations.getInvocation(1).getExpectedOutput().isUndefined());
        assertTrue(invocations.getInvocation(2).getExpectedOutput().isUndefined());
        assertFalse(invocations.getInvocation(3).getExpectedOutput().isUndefined());
        assertEquals("1", invocations.getInvocation(3).getExpectedOutput().getExpression());

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
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Stack", "D1": 10}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "create", "C2": "java.lang.String", "D2": "'Hello World!'"}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": {}, "B3": "push", "C3": "A1", "D3": "A2"}}
                {"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": 1, "B4": "size", "C4": "A1"}}
                """;

        String lql = """
                Stack {
                    Stack(int)
                    push(java.lang.String)->java.lang.String
                    size()->int
                }
                """;
        ObjectMapperVisitor visitor = createVisitor();

        Class cutClass = StackNonEmptyConstructorExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        ExecutedInvocations executedInvocations = testDriver.runSheet(ssnJsonlStr, lql, cutClass, 1, visitor);
        LOG.debug("executed invocations\n{}", executedInvocations);
        visitor.getActuationSheet().debug();
        visitor.getAdaptedActuationSheet().debug();
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
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Stack"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "$eval", "C2": "Arrays.toString(new char[]{'a', 'b'})"}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": {}, "B3": "push", "C3": "A1", "D3": "A2"}}
                {"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": 1, "B4": "size", "C4": "A1"}}
                """;

        String lql = """
                Stack {
                    push(java.lang.String)->java.lang.String
                    size()->int
                }
                """;
        ObjectMapperVisitor visitor = createVisitor();

        Class cutClass = StackEmptyConstructorExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        ExecutedInvocations executedInvocations = testDriver.runSheet(ssnJsonlStr, lql, cutClass, 1, visitor);
        LOG.debug("executed invocations\n{}", executedInvocations);
        visitor.getActuationSheet().debug();
        visitor.getAdaptedActuationSheet().debug();
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
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Singleton"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "sum", "C2": "A1", "D2": 2, "E2": 3}}
                """;

        String lql = """
                Singleton {
                    sum(int,int)->int
                }
                """;
        ObjectMapperVisitor visitor = createVisitor();

        Class cutClass = StaticMethodExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        ExecutedInvocations executedInvocations = testDriver.runSheet(ssnJsonlStr, lql, cutClass, 1, visitor);
        LOG.debug("executed invocations\n{}", executedInvocations);
        visitor.getActuationSheet().debug();
        visitor.getAdaptedActuationSheet().debug();
        Invocations invocations = executedInvocations.getInvocations();

        assertEquals(2, invocations.getSequence().size());
        assertEquals(invocations.getEval().resolveClass("Singleton"), invocations.getInvocation(0).getTargetClass());
        assertEquals(0, invocations.getInvocation(0).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("Singleton"), invocations.getInvocation(1).getTargetClass());

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
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Singleton"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "sum", "C2": "A1", "D2": 2, "E2": 3}}
                """;

        String lql = """
                Singleton {
                    sum(int,int)->int
                }
                """;
        ObjectMapperVisitor visitor = createVisitor();

        Class cutClass = InvisibleStaticMethodExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        ExecutedInvocations executedInvocations = testDriver.runSheet(ssnJsonlStr, lql, cutClass, 1, visitor);
        LOG.debug("executed invocations\n{}", executedInvocations);
        visitor.getActuationSheet().debug();
        visitor.getAdaptedActuationSheet().debug();
        Invocations invocations = executedInvocations.getInvocations();

        assertEquals(2, invocations.getSequence().size());
        assertEquals(invocations.getEval().resolveClass("Singleton"), invocations.getInvocation(0).getTargetClass());
        assertEquals(0, invocations.getInvocation(0).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("Singleton"), invocations.getInvocation(1).getTargetClass());

        assertEquals(2, executedInvocations.getSequence().size());
    }

    @Test
    public void test_composite_node() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Node", "D1": "'node1'"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "create", "C2": "Node", "D2": "'node2'"}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": {}, "B3": "setParent", "C3": "A1", "D3": "A2"}}
                {"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": "A2", "B4": "getParent", "C4": "A1"}}
                """;

        String lql = """
                Node {
                    Node(java.lang.String)
                    setParent(Node)->void
                    getParent()->Node
                }
                """;
        ObjectMapperVisitor visitor = createVisitor();

        Class cutClass = CompositeNodeExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        ExecutedInvocations executedInvocations = testDriver.runSheet(ssnJsonlStr, lql, cutClass, 1, visitor);
        LOG.debug("executed invocations\n{}", executedInvocations);
        visitor.getActuationSheet().debug();
        visitor.getAdaptedActuationSheet().debug();
        Invocations invocations = executedInvocations.getInvocations();

        assertEquals(4, invocations.getSequence().size());
        assertEquals(invocations.getEval().resolveClass("Node"), invocations.getInvocation(0).getTargetClass());
        assertEquals(1, invocations.getInvocation(0).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("Node"), invocations.getInvocation(1).getTargetClass());
        assertEquals(1, invocations.getInvocation(1).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("Node"), invocations.getInvocation(2).getTargetClass());
        assertEquals(1, invocations.getInvocation(2).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("Node"), invocations.getInvocation(3).getTargetClass());
        assertEquals(0, invocations.getInvocation(3).getParameters().size());
        // test oracle values (first column)
        assertTrue(invocations.getInvocation(0).getExpectedOutput().isUndefined());
        assertTrue(invocations.getInvocation(1).getExpectedOutput().isUndefined());
        assertTrue(invocations.getInvocation(2).getExpectedOutput().isUndefined());
        assertFalse(invocations.getInvocation(3).getExpectedOutput().isUndefined());
        assertEquals("A2", invocations.getInvocation(3).getExpectedOutput().getExpression());

        assertEquals(4, executedInvocations.getSequence().size());
    }

    @Test
    public void test_composite_node_self() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Node", "D1": "'node1'"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "setParent", "C2": "A1", "D2": "A1"}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": "A1", "B3": "getParent", "C3": "A1"}}
                """;

        String lql = """
                Node {
                    Node(java.lang.String)
                    setParent(Node)->void
                    getParent()->Node
                }
                """;
        ObjectMapperVisitor visitor = createVisitor();

        Class cutClass = CompositeNodeExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        ExecutedInvocations executedInvocations = testDriver.runSheet(ssnJsonlStr, lql, cutClass, 1, visitor);
        LOG.debug("executed invocations\n{}", executedInvocations);
        visitor.getActuationSheet().debug();
        visitor.getAdaptedActuationSheet().debug();
        Invocations invocations = executedInvocations.getInvocations();

        assertEquals(3, invocations.getSequence().size());
        assertEquals(invocations.getEval().resolveClass("Node"), invocations.getInvocation(0).getTargetClass());
        assertEquals(1, invocations.getInvocation(0).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("Node"), invocations.getInvocation(1).getTargetClass());
        assertEquals(1, invocations.getInvocation(1).getParameters().size());
        assertEquals(invocations.getEval().resolveClass("Node"), invocations.getInvocation(2).getTargetClass());
        assertEquals(0, invocations.getInvocation(2).getParameters().size());
        // test oracle values (first column)
        assertTrue(invocations.getInvocation(0).getExpectedOutput().isUndefined());
        assertTrue(invocations.getInvocation(1).getExpectedOutput().isUndefined());
        assertFalse(invocations.getInvocation(2).getExpectedOutput().isUndefined());
        assertEquals("A1", invocations.getInvocation(2).getExpectedOutput().getExpression());

        assertEquals(3, executedInvocations.getSequence().size());
    }

    private ObjectMapperVisitor createVisitor() {
        ObjectMapperVisitor visitor = new ObjectMapperVisitor(new GsonMapper());
//        InvocationVisitor invocationVisitor = new CompositeInvocationVisitor(
//                Arrays.asList(visitor));

        return visitor;
    }
}
