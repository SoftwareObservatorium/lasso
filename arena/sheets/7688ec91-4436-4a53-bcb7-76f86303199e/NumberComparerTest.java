import org.junit.Test;
import java.lang.Exception;
import org.apache.poi.ss.util.NumberComparer;
import static org.junit.Assert.*;

public class NumberComparerTest {

    /**
     * JUnit Test Case Example
     *
     * Class Under Test = 'com.XXX.adapter.NumberComparer'
     * Method Signature = 'compare(double,double):int'
     */
    // "normal" behaviour of compare method: first argument is greater than second argument
    @Test
    public void test_example0() throws Exception {
        NumberComparer _cut = new NumberComparer();
        int actual = _cut.compare(0.05, 0.04);
        assertEquals(1, actual);
    }

    // "normal" behaviour of compare method: first argument is smaller than second argument
    @Test
    public void test_example1() throws Exception {
        NumberComparer _cut = new NumberComparer();
        int actual = _cut.compare(0.04, 0.05);
        assertEquals(-1, actual);
    }

    // "normal behaviour of compare method: first argument is equal to the second argument"
    @Test
    public void test_example2() throws Exception {
        NumberComparer _cut = new NumberComparer();
        int actual = _cut.compare(0.05, 0.05);
        assertEquals(0, actual);
    }

    // "A=B" is "TRUE" but "A-B" is not "0"
    // A=0.06-0.01, B=0.05
    @Test
    public void test_example3() throws Exception {
        NumberComparer _cut = new NumberComparer();
        int actual = _cut.compare(0.06 - 0.01, 0.05);
        assertEquals(0, actual);
    }
    // no example found for "A=B" is "FALSE" but "A-B" is "0"
}