import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

public class Base64Test {

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
        Base64 base64_0 = new Base64();
        String in = File.pathSeparator;
        String actual = base64_0.encode(in);
    }
}