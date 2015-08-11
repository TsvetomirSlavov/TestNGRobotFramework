package integration;

import com.coriant.testng.robot.datamodel.annotations.Critical;
import com.coriant.testng.robot.datamodel.annotations.Documentation;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class Test1 {

    @Test(groups = {"group1"})
    @Critical
    public void pass() {
        assertTrue(true);
    }

    @Test
    @Critical
    @Documentation("Wanna be javadoc.")
    public void fail() {
        kwFail();
    }

    public void kwFail() {
        assertFalse(true);
    }
}
