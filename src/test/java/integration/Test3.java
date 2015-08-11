package integration;

import com.coriant.testng.robot.datamodel.annotations.Critical;
import com.coriant.testng.robot.datamodel.annotations.Documentation;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class Test3 {

    @Test
    @Documentation("This is test that pass.")
    public void pass() {
        assertTrue(true);
    }

    @Test
    @Critical(value = false)
    public void fail() {
        assertFalse(true);
    }
}