package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.examples.StackNonEmptyConstructorExample;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author Marcus Kessel
 */
public class MemberResolutionUtilsTest {

    @Test
    public void testMethod() throws NoSuchMethodException {
        Class<?> clazz = Stack.class;
        Method method = MemberResolutionUtils.resolveMethod(clazz, "push", new Class[]{Object.class});

        System.out.println(method.toString());

        assertEquals(clazz.getDeclaredMethod("push", Object.class), method);
    }

    @Test
    public void testMethod_String2Object() throws NoSuchMethodException {
        Class<?> clazz = Stack.class;
        Method method = MemberResolutionUtils.resolveMethod(clazz, "push", new Class[]{String.class});

        System.out.println(method.toString());

        assertEquals(clazz.getDeclaredMethod("push", Object.class), method);
    }

    @Test
    public void testConstructor_empty() throws NoSuchMethodException {
        Class<?> clazz = Stack.class;
        Constructor constructor = MemberResolutionUtils.resolveConstructor(clazz, null);

        System.out.println(constructor.toString());

        assertEquals(clazz.getDeclaredConstructor(), constructor);
    }

    @Test
    public void testConstructor_non_empty() throws NoSuchMethodException {
        Class<?> clazz = StackNonEmptyConstructorExample.class;
        Constructor constructor = MemberResolutionUtils.resolveConstructor(clazz, new Class[]{int.class});

        System.out.println(constructor.toString());

        assertEquals(clazz.getDeclaredConstructor(int.class), constructor);
    }

    @Test
    public void testConstructor_non_empty_short_int() throws NoSuchMethodException {
        Class<?> clazz = StackNonEmptyConstructorExample.class;
        Constructor constructor = MemberResolutionUtils.resolveConstructor(clazz, new Class[]{short.class});

        System.out.println(constructor.toString());

        assertEquals(clazz.getDeclaredConstructor(int.class), constructor);
    }
}
