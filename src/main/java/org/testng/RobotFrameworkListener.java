package org.testng;

import com.coriant.testng.robot.datamodel.*;
import com.coriant.testng.robot.datamodel.annotations.Critical;
import com.coriant.testng.robot.datamodel.annotations.Documentation;
import org.robotframework.RobotFramework;
import org.testng.internal.ConstructorOrMethod;
import org.testng.internal.IResultListener;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RobotFrameworkListener implements ISuiteListener, ITestListener, IReporter, IResultListener {
    private static final String FILE_NAME = "testOutput.xml";
    private final Map<String, Test> testMap = new HashMap<>();
    private final Suite mainSuite = new Suite();

    @Override
    public void onStart(ISuite iSuite) {
        mainSuite.setName(iSuite.getName());
        mainSuite.setId("Unused");
        XmlSuite xmlSuite = iSuite.getXmlSuite();
        if (xmlSuite != null) {
            List<XmlTest> tests = xmlSuite.getTests();
            if (tests != null) {
                for (XmlTest test : tests) {
                    Suite suite = new Suite();
                    suite.setName(test.getName());
                    suite.setId(String.valueOf(test.getIndex()));
                    mainSuite.getSuite().add(suite);
                }
            }
        }
    }

    @Override
    public void onFinish(ISuite iSuite) {
        Robot robot = new Robot();
        Status status = new Status();
        SuiteRunState suiteState = iSuite.getSuiteState();
        if (suiteState.isFailed()) {
            status.setStatus("FAIL");
        } else {
            status.setStatus("PASS");
        }
        mainSuite.setStatus(status);

        robot.setSuite(mainSuite);

        jaxbObjectToXML(robot);
        Robot fromFile = jaxbXMLToObject();
        System.out.println(fromFile.toString());
        RobotFramework.main(new String[]{"rebot", "-n", "noncritical", FILE_NAME});
    }

    @Override
    public void onTestStart(ITestResult iTestResult) {
        Suite suite = null;
        ITestNGMethod testNGMethod = iTestResult.getMethod();
        XmlTest xmlTest = testNGMethod.getXmlTest();

        if (xmlTest != null) {
            String xmlTestName = xmlTest.getName();
            if (xmlTestName != null) {
                List<Suite> suiteList = mainSuite.getSuite();
                for (Suite suite1 : suiteList) {
                    if (suite1.getName().equals(xmlTestName)) {
                        suite = suite1;
                        break;
                    }
                }
                if (suite == null) {
                    suite = createSuite(iTestResult, xmlTest, xmlTestName);
                    mainSuite.getSuite().add(suite);
                }
            }
        }

        if (suite != null) {
            processTest(iTestResult, testNGMethod, suite);
        }
    }

    private void processTest(ITestResult iTestResult, ITestNGMethod testNGMethod, Suite suite) {
        ITestClass testClass = testNGMethod.getTestClass();
        if (testClass != null) {
            String name = testClass.getName();
            if (name != null) {
                String[] split = name.split("\\.");
                String testName = split[Math.max(0, split.length - 1)];

                Test test = null;
                for (Test testAux : suite.getTest()) {
                    if (testName.equals(testAux.getName())) {
                        test = testAux;
                        break;
                    }
                }

                if (test == null) {
                    test = createTest(iTestResult, testNGMethod, suite, testName);
                }

                Kw kw = null;
                String methodName = testNGMethod.getMethodName();
                if (methodName != null) {
                    for (Kw kwAux : test.getKw()) {
                        if (kwAux.getName().equals(methodName)) {
                            kw = kwAux;
                            break;
                        }
                    }

                    if (kw == null) {
                        kw = createKw(methodName, testNGMethod, test);
                    }

                    test.getKw().add(kw);
                }
            }
        }
    }

    private Kw createKw(String kwName, ITestNGMethod testNGMethod, Test test) {
        Kw kw = new Kw();
        kw.setName(kwName);
        kw.setType("kw");
        Status status = new Status();
        status.setStarttime(getDate(System.currentTimeMillis()));
        kw.setStatus(status);

        String[] groups = testNGMethod.getGroups();
        Tags tags = new Tags();
        if (groups != null) {
            for (String group : groups) {
                tags.getTag().add(group);
            }
        }
        test.getTags().getTag().addAll(tags.getTag());

        ConstructorOrMethod constructorOrMethod = testNGMethod.getConstructorOrMethod();
        if (constructorOrMethod != null) {
            Method method = constructorOrMethod.getMethod();
            if (method != null) {
                if (method.isAnnotationPresent(Critical.class)) {
                    Critical critical = method.getAnnotation(Critical.class);
                    if (!critical.value()) {
                        kw.getStatus().setCritical("no");
                    }
                }

                if (method.isAnnotationPresent(Documentation.class)) {
                    Documentation documentation = method.getAnnotation(Documentation.class);
                    kw.setDoc(documentation.value());
                }
            }
        }

        return kw;
    }

    private Test createTest(ITestResult iTestResult, ITestNGMethod testNGMethod, Suite suite, String testName) {
        Test test = new Test();
        suite.getTest().add(test);
        test.setId("TestId");
        test.setName(testName);

        Status status = new Status();
        status.setCritical("no");
        status.setStarttime(getDate(iTestResult.getStartMillis()));

        test.setStatus(status);

        String[] groups = testNGMethod.getGroups();
        Tags tags = new Tags();
        if (groups != null) {
            for (String group : groups) {
                tags.getTag().add(group);
            }
        }
        ConstructorOrMethod constructorOrMethod = testNGMethod.getConstructorOrMethod();
        if (constructorOrMethod != null) {
            Method method = constructorOrMethod.getMethod();
            if (method != null) {
                if (method.isAnnotationPresent(Critical.class)) {
                    Critical critical = method.getAnnotation(Critical.class);
                    if (!critical.value()) {
                        tags.getTag().add("noncritical");
                        status.setCritical("no");
                    }
                }
                test.setTags(tags);


                if (method.isAnnotationPresent(Documentation.class)) {
                    Documentation documentation = method.getAnnotation(Documentation.class);
                    test.setDoc(documentation.value());
                }
            }
        }

        return test;
    }

    private Suite createSuite(ITestResult iTestResult, XmlTest xmlTest, String xmlTestName) {
        Suite suite = new Suite();
        suite.setName(xmlTestName);
        suite.setId(String.valueOf(xmlTest.getIndex()));

        Status status = new Status();
        status.setStarttime(getDate(iTestResult.getStartMillis()));

        suite.setStatus(status);

        return suite;
    }

    private String getDate(long timeStamp) {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS");
        return sdfDate.format(new Date(timeStamp));
    }

    @Override
    public void onTestSuccess(ITestResult iTestResult) {
        Suite suite = null;
        ITestNGMethod testNGMethod = iTestResult.getMethod();
        XmlTest xmlTest = testNGMethod.getXmlTest();

        if (xmlTest != null) {
            String xmlTestName = xmlTest.getName();
            if (xmlTestName != null) {
                List<Suite> suiteList = mainSuite.getSuite();
                for (Suite suite1 : suiteList) {
                    if (suite1.getName().equals(xmlTestName)) {
                        suite = suite1;
                        break;
                    }
                }
                if (suite == null) {
                    System.out.println("Suite \"" + xmlTestName + "\" not found");
                    return;
                }
            }
        }

        if (suite != null) {
            ITestClass testClass = testNGMethod.getTestClass();
            if (testClass != null) {
                String name = testClass.getName();
                if (name != null) {
                    String[] split = name.split("\\.");
                    String testName = split[Math.max(0, split.length - 1)];

                    Test test = null;
                    for (Test testAux : suite.getTest()) {
                        if (testName.equals(testAux.getName())) {
                            test = testAux;
                            break;
                        }
                    }

                    if (test == null) {
                        System.out.println("Test \"" + testName + "\" not found");
                        return;
                    }

                    Kw kw = null;
                    String methodName = testNGMethod.getMethodName();
                    for (Kw kwAux : test.getKw()) {
                        if (kwAux.getName().equals(methodName)) {
                            kw = kwAux;
                            break;
                        }
                    }

                    if (kw == null) {
                        System.out.println("Kw \"" + methodName + "\" not found");
                        return;
                    }

                    Status status = kw.getStatus();
                    status.setEndtime(getDate(System.currentTimeMillis()));
                    status.setStatus("PASS");

                    for (Kw kw1 : test.getKw()) {
                        if (kw1.getStatus().getStatus().equals("yes")) {
                            test.getStatus().setCritical("yes");
                        }
                    }

                    test.getStatus().setStatus("PASS");
                    test.getStatus().setEndtime(getDate(iTestResult.getEndMillis()));
                }
            }
        }
    }

    @Override
    public void onTestFailure(ITestResult iTestResult) {
        Suite suite = null;
        ITestNGMethod testNGMethod = iTestResult.getMethod();
        XmlTest xmlTest = testNGMethod.getXmlTest();

        if (xmlTest != null) {
            String xmlTestName = xmlTest.getName();
            if (xmlTestName != null) {
                List<Suite> suiteList = mainSuite.getSuite();
                for (Suite suite1 : suiteList) {
                    if (suite1.getName().equals(xmlTestName)) {
                        suite = suite1;
                        break;
                    }
                }
                if (suite == null) {
                    System.out.println("Suite \"" + xmlTestName + "\" not found");
                    return;
                }
            }
        }
        Throwable throwable = iTestResult.getThrowable();
        if (suite != null) {
            ITestClass testClass = testNGMethod.getTestClass();
            if (testClass != null) {
                String name = testClass.getName();
                if (name != null) {
                    String[] split = name.split("\\.");
                    String testName = split[Math.max(0, split.length - 1)];

                    Test test = null;
                    for (Test testAux : suite.getTest()) {
                        if (testName.equals(testAux.getName())) {
                            test = testAux;
                            break;
                        }
                    }

                    if (test == null) {
                        System.out.println("Test \"" + testName + "\" not found");
                        return;
                    }

                    Kw kw = null;
                    String methodName = testNGMethod.getMethodName();
                    for (Kw kwAux : test.getKw()) {
                        if (kwAux.getName().equals(methodName)) {
                            kw = kwAux;
                            break;
                        }
                    }

                    if (kw == null) {
                        System.out.println("Kw \"" + methodName + "\" not found");
                        return;
                    }

                    Status status = kw.getStatus();
                    status.setEndtime(getDate(System.currentTimeMillis()));
                    status.setStatus("FAIL");

                    Msg msg = new Msg();
                    msg.setLevel("ERROR");
                    msg.setTimestamp(getDate(System.currentTimeMillis()));
                    try {
                        PrintStream printStream = new PrintStream("");
                        throwable.printStackTrace(printStream);
                        msg.setValue(printStream.toString());
                    } catch (FileNotFoundException e) {
                        System.out.println(e.getMessage());
                    }
                    msg.setHtml("yes");

                    kw.getMsg().add(msg);

                    if (kw.getStatus().getCritical().equals("yes")) {
                        test.getStatus().forceStatus("FAIL");
                    } else {
                        test.getStatus().setStatus("PASS");
                    }
                    test.getStatus().setEndtime(getDate(iTestResult.getEndMillis()));
                }
            }
        }
    }

    @Override
    public void onTestSkipped(ITestResult iTestResult) {
        System.out.println("onTestSkipped " + iTestResult);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult iTestResult) {
        System.out.println("onTestFailedButWithinSuccessPercentage " + iTestResult);
    }

    @Override
    public void onStart(ITestContext iTestContext) {
        System.out.println("onStart " + iTestContext.getName());
        List<Suite> suiteList = mainSuite.getSuite();
        if (suiteList != null) {
            for (Suite suite1 : suiteList) {//TODO: replace it by List.get() implementing Object.equals() on data model.
                if (iTestContext.getCurrentXmlTest().getName().equals(suite1.getName())) {
                    Status status = new Status();
                    status.setStarttime(getDate(iTestContext.getStartDate().getTime()));
                    suite1.setStatus(status);
                    break;
                }
            }
        }
    }

    @Override
    public void onFinish(ITestContext iTestContext) {
        System.out.println("onFinish " + iTestContext.getName());
    }

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        System.out.println(xmlSuites);
        System.out.println(suites);
        System.out.println(outputDirectory);
    }

    private static Robot jaxbXMLToObject() {
        try {
            JAXBContext context = JAXBContext.newInstance(Robot.class);
            Unmarshaller un = context.createUnmarshaller();
            return (Robot) un.unmarshal(new File(FILE_NAME));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static void jaxbObjectToXML(Robot robot) {

        try {
            JAXBContext context = JAXBContext.newInstance(Robot.class);
            Marshaller m = context.createMarshaller();
            //for pretty-print XML in JAXB
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            // Write to System.out for debugging
            // m.marshal(robot, System.out);

            // Write to File
            m.marshal(robot, new File(FILE_NAME));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigurationSuccess(ITestResult itr) {
        System.out.println("onConfigurationSuccess " + itr);
    }

    @Override
    public void onConfigurationFailure(ITestResult itr) {
        System.out.println("onConfigurationFailure " + itr);
    }

    @Override
    public void onConfigurationSkip(ITestResult itr) {
        System.out.println("onConfigurationSkip " + itr);
    }
}
