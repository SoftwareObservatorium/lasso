package gemma327b;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StackTest {

    @Test
    void testSize() {
        Stack stack = new Stack();
        assertEquals(0, stack.size());
        stack.push(new Object());
        assertEquals(1, stack.size());
        stack.push(new Object());
        assertEquals(2, stack.size());
        stack.pop();
        assertEquals(1, stack.size());
        stack.pop();
        assertEquals(0, stack.size());
    }
}