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
            /*String nl = System.getProperty("line.separator");
            System.out.println(String.format(nl +
                            "-------------------------------------------------------" + nl +
                            "METHOD: %s" + nl +
                            "-------------------------------------------------------",
                    description.getMethodName()));*/
            System.out.println(INFO.colorize(description));
        }
    };
}
