package cn.kmbeast.test;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseTest {
    
    @BeforeEach
    public void setUp() {
        // Test preparation
        System.out.println("Test started...");
    }
    
    /**
     * Verify that the result meets expectations
     * @param actual actual result
     * @param expected expected result
     * @param message error message
     */
    protected void assertEquals(Object actual, Object expected, String message) {
        if (actual == null && expected == null) {
            return;
        }
        if (actual == null || !actual.equals(expected)) {
            throw new AssertionError(message + ", actual value: " + actual + ", expected value: " + expected);
        }
    }
    
    /**
     * Verify that the condition is true
     * @param condition condition
     * @param message error message
     */
    protected void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
    
    /**
     * Verify that the object is not null
     * @param object object
     * @param message error message
     */
    protected void assertNotNull(Object object, String message) {
        if (object == null) {
            throw new AssertionError(message);
        }
    }
}
