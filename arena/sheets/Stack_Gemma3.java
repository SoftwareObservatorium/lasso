package gemma327b;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StackTest {

    @Test
    void testPushAndPop() {
        Stack stack = new Stack();
        Object item = new Object();
        stack.push(item);
        assertEquals(item, stack.pop());
        // Should return null when empty
        assertNull(stack.pop());
    }

    @Test
    void testPeek() {
        Stack stack = new Stack();
        Object item = new Object();
        stack.push(item);
        assertEquals(item, stack.peek());
        stack.pop();
        assertNull(stack.peek());
    }

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

    @Test
    void testEmptyStackPopAndPeek() {
        Stack stack = new Stack();
        assertNull(stack.pop());
        assertNull(stack.peek());
        assertEquals(0, stack.size());
    }

    @Test
    void testPushMultipleItems() {
        Stack stack = new Stack();
        Object item1 = new Object();
        Object item2 = new Object();
        Object item3 = new Object();
        stack.push(item1);
        stack.push(item2);
        stack.push(item3);
        assertEquals(3, stack.size());
        assertEquals(item3, stack.pop());
        assertEquals(item2, stack.pop());
        assertEquals(item1, stack.pop());
        assertNull(stack.pop());
    }

    @Test
    void testPushNull() {
        Stack stack = new Stack();
        stack.push(null);
        assertEquals(1, stack.size());
        assertEquals(null, stack.pop());
    }

    @Test
    void testPeekDoesNotRemove() {
        Stack stack = new Stack();
        Object item = new Object();
        stack.push(item);
        assertEquals(item, stack.peek());
        // Call peek again to ensure it doesn't remove
        assertEquals(item, stack.peek());
        assertEquals(item, stack.pop());
        assertEquals(0, stack.size());
    }
}