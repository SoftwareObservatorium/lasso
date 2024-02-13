import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
    public void test1_static() throws Throwable {
        String in = "hi!";
        String actual = Base64.encode(in);
    }

    @Test
    public void test2_static_fq() throws Throwable {
        String in = "hi!";
        String actual = bla.Base64.encode(in);
    }

    @Test
    public void test3_mixed_static_fq() throws Throwable {
        Base64 cut = new Base64();

        String in = "hi!";
        String actual = Base64.encode(in);
    }
}