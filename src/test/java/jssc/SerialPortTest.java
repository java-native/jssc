package jssc;

import jssc.junit.rules.DisplayMethodNameRule;
import org.junit.Test;
import org.slf4j.Logger;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;


public class SerialPortTest extends DisplayMethodNameRule {

    private static final Logger log = getLogger(SerialPortTest.class);


    @Test
    public void expectSettingToBeSetSuccessfully() {
        SerialPort serial = new SerialPort("ttyS0");
        /* 100ms proved to be a reasonable value in the use-case our using
         * project had. */
        serial.setWaitEventsTimeoutMs(100);
    }


    /**
     * Cannot really test this deeply. Just make sure the setter accepts the
     * value.
     */
    @Test
    public void disableFeatureByPassingMinusOne() {
        SerialPort serial = new SerialPort("ttyS0");
        serial.setWaitEventsTimeoutMs(-1);
    }


    /**
     * configuring a zero-length timeout doesn't make any sense. I'd expect
     * this to have the same effect as using no timeout in the 1st place.
     * With the difference, we will have nonsense calls to `poll`.
     *
     * As soon someone really has the need to pass zero, then inverse this
     * test and EXPLAIN CLEARLY by replacing this comment why this is the case.
     */
    @Test
    public void mustNotPassZero() {
        SerialPort serial = new SerialPort("ttyS0");
        try {
            serial.setWaitEventsTimeoutMs(0);
            fail("Where's the exception?");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("0"));
        }
    }


    /**
     * Prefer to tell the user right away whenever nonsense values are passed.
     * Makes bugs to appear early in place of them hiding silent.
     */
    @Test
    public void mustNotPassAnyOtherNegativeValues() {
        /* just try a bunch of illegal values (testing ALL possible cases might
         * take a bit too long) */
        SerialPort serial = new SerialPort("ttyS0");
        for(int badTimeoutMs = -42 ; badTimeoutMs <= -2 ; ++badTimeoutMs ){
            log.debug("setWaitEventsTimeoutMs({})", badTimeoutMs);
            try {
                serial.setWaitEventsTimeoutMs(badTimeoutMs);
                fail("Where's the exception for "+ badTimeoutMs +"?");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains(String.valueOf(badTimeoutMs)));
            }
        }
    }


}
