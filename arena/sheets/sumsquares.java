import org.junit.Test;
import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Arrays;

public class ProblemTest {

    @Test
    public void testHappyCases() {
        assertEquals(14l, Problem.sumSquares(new ArrayList<Float>(Arrays.asList((float) 1.0f, (float) 2.0f, (float) 3.0f))));
        assertEquals(98l, Problem.sumSquares(new ArrayList<Float>(Arrays.asList((float) 1.0f, (float) 4.0f, (float) 9.0f))));
        assertEquals(84l, Problem.sumSquares(new ArrayList<Float>(Arrays.asList((float) 1.0f, (float) 3.0f, (float) 5.0f, (float) 7.0f))));
    }

    @Test
    public void testEdgeCases() {
        // Complex inputs
        assertEquals(29l, Problem.sumSquares(new ArrayList<Float>(Arrays.asList((float) 1.4f, (float) 4.2f, (float) 0.0f))));
        assertEquals(6l, Problem.sumSquares(new ArrayList<Float>(Arrays.asList((float) -2.4f, (float) 1.0f, (float) 1.0f))));

        // Corner case inputs
        assertEquals(0l, Problem.sumSquares(new ArrayList<>()));
        assertEquals(1l, Problem.sumSquares(new ArrayList<>(Arrays.asList((float) 1.0f))));
    }

    @Test
    public void testDifficultInputs() {
        // Negative numbers
        assertEquals(8l, Problem.sumSquares(new ArrayList<Float>(Arrays.asList((float) -2.5f, (float) 3.0f))));

        // Zero
        assertEquals(0l, Problem.sumSquares(new ArrayList<Float>(Arrays.asList((float) 0.0f))));
    }

    @Test
    void testSumSquaresSingleElementPositive() {
        ArrayList<Float> list = new ArrayList<>(Arrays.asList(2.0f));
        long expected = 4;
        long actual = Problem.sumSquares(list);
        assertEquals(expected, actual);
    }
}
