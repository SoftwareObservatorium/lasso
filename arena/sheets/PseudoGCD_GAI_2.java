import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class GreatestCommonDivisorTest {

    @Test
    public void testGreatestCommonDivisor() {
        GreatestCommonDivisor greatestCommonDivisor = new GreatestCommonDivisor();
        assertEquals(1, greatestCommonDivisor.greatestCommonDivisor(2, 3));
    }

    @Test
    public void testGreatestCommonDivisorOfCoprimeNumbers() {
        GreatestCommonDivisor greatestCommonDivisor = new GreatestCommonDivisor();
        assertEquals(7, greatestCommonDivisor.greatestCommonDivisor(21, 35));
    }

    @Test
    public void testGreatestCommonDivisorOfZero() {
        GreatestCommonDivisor greatestCommonDivisor = new GreatestCommonDivisor();
        assertEquals(0, greatestCommonDivisor.greatestCommonDivisor(0, 5));
    }
}