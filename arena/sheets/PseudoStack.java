package some.example.pkg;

import org.junit.Test;

public class StackTest {

    @Test
    public void test1() throws Throwable {
        Stack strList0 = new Stack();
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
}