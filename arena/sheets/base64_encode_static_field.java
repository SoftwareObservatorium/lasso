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
    public void test1_static_field() throws Throwable {
        Base64 cut = new Base64();

        String in = File.pathSeparator;
        String actual = cut.encode(in);
    }

    @Test
    public void test2_static_field_fq() throws Throwable {
        Base64 cut = new Base64();

        String in = File.pathSeparator;
        String actual = cut.encode(in);
    }
}