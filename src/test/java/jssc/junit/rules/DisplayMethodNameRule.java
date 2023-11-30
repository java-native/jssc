package jssc.junit.rules;

import org.junit.Rule;
import org.junit.rules.*;
import org.junit.runner.*;

import static jssc.common.ConsoleStyle.*;

/**
 * Adds the method name to the JUnit logs, useful for debugging
 */
public class DisplayMethodNameRule {
    @Rule
    public TestWatcher testWatcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            System.out.println(INFO.colorize(description));
        }
    };
}
