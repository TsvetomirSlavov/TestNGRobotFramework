package integration;

import com.coriant.testng.robot.datamodel.annotations.Critical;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class Test2 {

    @Test
    public void pass() {
        assertTrue(true);
    }

    @Test
    @Critical(value = false)
    public void fail() {
        assertFalse(true);
    }
}