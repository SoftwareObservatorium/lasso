import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class GCDTest {

    @Test
    public void testGreatestCommonDivisor_BothNumbersAreZero() {
        // Arrange + Act + Assert
        assertEquals(0, GCD.greatestCommonDivisor(0, 0));
    }

    @Test
    public void testGreatestCommonDivisor_FirstNumberIsOne() {
        // Arrange + Act + Assert
        assertEquals(1, GCD.greatestCommonDivisor(1, 2));
    }

    @Test
    public void testGreatestCommonDivisor_SecondNumberIsOne() {
        // Arrange + Act + Assert
        assertEquals(1, GCD.greatestCommonDivisor(2, 1));
    }

    @Test
    public void testGreatestCommonDivisor_BothNumbersAreIdentical() {
        // Arrange + Act + Assert
        assertEquals(5, GCD.greatestCommonDivisor(15, 25)); // GCD of 15 and 20 is 5
    }

    @Test
    public void testGreatestCommonDivisor_NoCommonFactors() {
        // Arrange + Act + Assert
        assertEquals(1, GCD.greatestCommonDivisor(13, 24));
    }
}