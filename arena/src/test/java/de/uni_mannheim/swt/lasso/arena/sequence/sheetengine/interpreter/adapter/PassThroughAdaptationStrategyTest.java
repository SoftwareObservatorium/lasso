package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.examples.StackNonEmptyConstructorExample;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.LQLUtils;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.MemberResolutionUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author Marcus Kessel
 */
public class PassThroughAdaptationStrategyTest {

    @Test
    public void testMethod() throws NoSuchMethodException, IOException {
        Class<?> clazz = Stack.class;

        String lql = """
                Stack {
                    push(java.lang.String)->java.lang.String
                    size()->int
                }
                """;
        InterfaceSpecification interfaceSpecification = LQLUtils.lqlToMap(lql).get("Stack");

        ClassUnderTest classUnderTest = CutUtils.createExample(clazz);
        CutUtils.initializeCutDirty(classUnderTest);

        PassThroughAdaptationStrategy adaptationStrategy = new PassThroughAdaptationStrategy();
        PassThroughImplementation impl = (PassThroughImplementation) adaptationStrategy.adapt(interfaceSpecification, classUnderTest,-1).get(0);

        assertEquals(clazz.getConstructor(), impl.getInitializer(interfaceSpecification, 0).getAsConstructor());
        assertEquals(clazz.getMethod("push", Object.class), impl.getMethod(interfaceSpecification, 0).getMethod());
        assertEquals(clazz.getMethod("size"), impl.getMethod(interfaceSpecification, 1).getMethod());
    }
}
