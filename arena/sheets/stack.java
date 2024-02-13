package some.example.pkg;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StackTest {

    public static boolean debug = false;

    @BeforeClass
    public static void setupAll() {
    }

    @AfterClass
    public static void teardownAll() {
    }

    @Before
    public void setup() {
    }

    @After
    public void teardown() {
    }

    @Test
    public void test1() throws Throwable {
        java.util.Stack<java.lang.String> strList0 = new java.util.Stack<java.lang.String>();
        String input = "hi!";
        java.lang.String str2 = strList0.push(input);
        java.lang.String str3 = strList0.peek();
        int int4 = strList0.size();
        java.lang.String str5 = strList0.pop();
        int int6 = strList0.size();
        org.junit.Assert.assertEquals("'" + str2 + "' != '" + "hi!" + "'", str2, "hi!");
        org.junit.Assert.assertEquals("'" + str3 + "' != '" + "hi!" + "'", str3, "hi!");
        org.junit.Assert.assertTrue("'" + int4 + "' != '" + 1 + "'", int4 == 1);
        org.junit.Assert.assertEquals("'" + str5 + "' != '" + "hi!" + "'", str5, "hi!");
        org.junit.Assert.assertTrue("'" + int6 + "' != '" + 0 + "'", int6 == 0);
    }

    @Test
    public void test2() throws Throwable {
        java.util.Stack<java.lang.String> strList0 = new java.util.Stack<java.lang.String>();
        String input = "hi!";
        java.lang.String str2 = strList0.push(input);
        java.lang.String str22 = strList0.push(input);
        java.lang.String str3 = strList0.peek();
        int int4 = strList0.size();
        java.lang.String str5 = strList0.pop();
        int int6 = strList0.size();
    }
}