package jssc.junit.rules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.MarkerManager;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Adds the method name to the JUnit logs, useful for debugging
 */
public class DisplayMethodNameRule {
    @Rule
    public TestWatcher methodWatcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            Logger log = LogManager.getLogger(description.getTestClass());
            log.info(MarkerManager.getMarker("MethodName"), description.getMethodName());
        }
    };
}