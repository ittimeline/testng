package test.dataprovider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.testng.Assert;
import org.testng.IDataProviderMethod;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.TestNGException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Lists;
import org.testng.internal.RuntimeBehavior;
import org.testng.internal.collections.Pair;
import org.testng.internal.reflect.MethodMatcherException;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlTest;
import test.InvokedMethodNameListener;
import test.SimpleBaseTest;
import test.dataprovider.issue1691.DataProviderDefinitionAtClassLevelAndNoTestMethodUsage;
import test.dataprovider.issue1691.DataProviderDefinitionCompletelyProvidedAtClassLevel;
import test.dataprovider.issue1691.DataProviderDefinitionCompletelyProvidedAtClassLevelAndPartiallyAtMethodLevel;
import test.dataprovider.issue1691.DataProviderDefinitionProvidedPartiallyAtClassLevel;
import test.dataprovider.issue1691.withinheritance.ChildClassHasFullDefinitionOfDataProviderAtClassLevel;
import test.dataprovider.issue1691.withinheritance.ChildClassHasPartialDefinitionOfDataProviderAtClassLevel;
import test.dataprovider.issue1691.withinheritance.ChildClassWithNoDataProviderInformationInTestMethod;
import test.dataprovider.issue1987.BaseClassSample;
import test.dataprovider.issue1987.DataProviderInBaseClassSample;
import test.dataprovider.issue1987.DataProviderInDifferentClassSample;
import test.dataprovider.issue1987.DataProviderInSameClassSample;
import test.dataprovider.issue1987.DataProviderTrackingListener;
import test.dataprovider.issue2504.SampleTestCaseListener;
import test.dataprovider.issue2565.Data;
import test.dataprovider.issue2565.SampleTestUsingConsumer;
import test.dataprovider.issue2565.SampleTestUsingFunction;
import test.dataprovider.issue2565.SampleTestUsingPredicate;
import test.dataprovider.issue2565.SampleTestUsingSupplier;
import test.dataprovider.issue2819.DataProviderListenerForRetryAwareTests;
import test.dataprovider.issue2819.SimpleRetry;
import test.dataprovider.issue2819.TestClassFailingRetrySample;
import test.dataprovider.issue2819.TestClassSample;
import test.dataprovider.issue2819.TestClassUsingDataProviderRetrySample;
import test.dataprovider.issue2819.TestClassWithMultipleRetryImplSample;
import test.dataprovider.issue2888.SkipDataProviderSample;
import test.dataprovider.issue2934.TestCaseSample;
import test.dataprovider.issue2934.TestCaseSample.CoreListener;
import test.dataprovider.issue2934.TestCaseSample.ToggleDataProvider;
import test.dataprovider.issue2980.LoggingListener;

public class DataProviderTest extends SimpleBaseTest {

  @Test(description = "GITHUB-2819")
  public void testDataProviderCanBeRetriedOnFailures() {
    TestNG testng = create(TestClassUsingDataProviderRetrySample.class);
    DataProviderListenerForRetryAwareTests listener = new DataProviderListenerForRetryAwareTests();
    testng.addListener(listener);
    testng.run();
    // Without retrying itself we would have already invoked the listener once.
    assertThat(listener.getBeforeInvocations()).isEqualTo(3);
    assertThat(listener.getFailureInvocations()).isEqualTo(2);
    assertThat(listener.getAfterInvocations()).isEqualTo(1);
  }

  @Test(description = "GITHUB-2819")
  public void testDataProviderCanBeRetriedViaAnnotationTransformer() {
    TestNG testng = create(TestClassSample.class);
    TestClassSample.EnableRetryForDataProvider transformer =
        new TestClassSample.EnableRetryForDataProvider();
    DataProviderListenerForRetryAwareTests listener = new DataProviderListenerForRetryAwareTests();
    testng.addListener(transformer);
    testng.addListener(listener);
    testng.run();
    // Without retrying itself we would have already invoked the listener once.
    assertThat(listener.getBeforeInvocations()).isEqualTo(3);
    assertThat(listener.getFailureInvocations()).isEqualTo(2);
    assertThat(listener.getAfterInvocations()).isEqualTo(1);
  }

  @Test(description = "GITHUB-2819")
  public void testDataProviderRetryInstancesAreUniqueForEachDataDrivenTest() {
    SimpleRetry.clearObjectIds();
    TestNG testng = create(TestClassWithMultipleRetryImplSample.class);
    DataProviderListenerForRetryAwareTests listener = new DataProviderListenerForRetryAwareTests();
    testng.addListener(listener);
    testng.run();
    assertThat(SimpleRetry.getObjectIds()).hasSize(2);
    // Without retrying itself we would have already invoked the listener once.
    assertThat(listener.getBeforeInvocations()).isEqualTo(6);
    assertThat(listener.getFailureInvocations()).isEqualTo(4);
    assertThat(listener.getAfterInvocations()).isEqualTo(2);
  }

  @Test(description = "GITHUB-2819")
  public void testDataProviderRetryAbortsGracefullyWhenNoRetryAtFirstTime() {
    TestNG testng = create(TestClassFailingRetrySample.class);
    DataProviderListenerForRetryAwareTests listener = new DataProviderListenerForRetryAwareTests();
    testng.addListener(listener);
    testng.run();
    assertThat(listener.getBeforeInvocations()).isEqualTo(1);
    assertThat(listener.getFailureInvocations()).isEqualTo(1);
    assertThat(listener.getAfterInvocations()).isEqualTo(0);
  }

  @Test(description = "GITHUB-2800")
  public void testDataProviderFromAbstractClassWhenCoupledWithFactories() {
    InvokedMethodNameListener listener = run(test.dataprovider.issue2800.TestClassGenerator.class);
    assertThat(listener.getSucceedMethodNames()).containsExactly("hi", "hi");
  }

  @Test(description = "GITHUB-1691")
  public void testDataProviderInfoIgnored() {
    InvokedMethodNameListener listener =
        run(DataProviderDefinitionAtClassLevelAndNoTestMethodUsage.class);
    assertThat(listener.getSucceedMethodNames())
        .containsExactly(
            "verifyHangoutPlaces(Hakuna Matata,Bangalore)", "verifyHangoutPlaces(Gem Inn,Chennai)");
    Throwable throwable = listener.getResult("regularTestMethod").getThrowable();
    assertThat(throwable).isInstanceOf(MethodMatcherException.class);
  }

  @Test(description = "GITHUB-1691", dataProvider = "getClasses")
  public void testDataProviderWhenProvidedAtClassLevel(Class<?> cls) {
    InvokedMethodNameListener listener = run(cls);
    assertThat(listener.getSucceedMethodNames())
        .containsExactly(
            "verifyHangoutPlaces(Hakuna Matata,Bangalore)", "verifyHangoutPlaces(Gem Inn,Chennai)");
  }

  @DataProvider
  public Object[][] getClasses() {
    return new Object[][] {
      // No inheritance involved
      {DataProviderDefinitionProvidedPartiallyAtClassLevel.class},
      {DataProviderDefinitionCompletelyProvidedAtClassLevel.class},
      {DataProviderDefinitionCompletelyProvidedAtClassLevelAndPartiallyAtMethodLevel.class},

      // Involves Inheritance
      {ChildClassHasPartialDefinitionOfDataProviderAtClassLevel.class},
      {ChildClassHasFullDefinitionOfDataProviderAtClassLevel.class},
      {ChildClassWithNoDataProviderInformationInTestMethod.class},
    };
  }

  @Test(description = "GITHUB-1139")
  public void oneDimDataProviderShouldWork() {
    InvokedMethodNameListener listener = run(OneDimDataProviderSample.class);

    assertThat(listener.getSucceedMethodNames())
        .containsExactly(
            "testArray(foo)", "testArray(bar)",
            "testIterator(foo)", "testIterator(bar)",
            "testStaticArray(foo)", "testStaticArray(bar)",
            "testStaticIterator(foo)", "testStaticIterator(bar)");
  }

  @Test
  public void booleanTest() {
    InvokedMethodNameListener listener = run(BooleanDataProviderSample.class);

    assertThat(listener.getSucceedMethodNames()).containsExactly("doStuff(true)", "doStuff(false)");
  }

  @Test
  public void classTest() {
    InvokedMethodNameListener listener = run(ClassDataProviderSample.class);

    assertThat(listener.getSucceedMethodNames()).containsExactly("f(a)", "f(b)", "g(a)", "g(b)");
  }

  @Test
  public void configurationAndDataProvidersTest() {
    InvokedMethodNameListener listener = run(ConfigurationAndDataProvidersSample.class);

    assertThat(listener.getSucceedMethodNames())
        .containsExactly("setUpSuite", "setUpTest", "setUpClass", "setUp", "verifyNames(Test)");
  }

  @Test
  public void dataProviderAsTest() {
    InvokedMethodNameListener listener = run(DataProviderAsTestSample.class);

    assertThat(listener.getSucceedMethodNames()).containsExactly("f");
  }

  @Test
  public void emptyDataProviderTest() {
    InvokedMethodNameListener listener = run(EmptyDataProviderSample.class);

    assertThat(listener.getFailedMethodNames()).isEmpty();
    assertThat(listener.getSkippedMethodNames()).isEmpty();
    assertThat(listener.getSucceedMethodNames()).isEmpty();
  }

  @Test(description = "TESTNG-576: Prefer DataProvider explicit name")
  public void should_prefer_dataProvider_explicit_name() {
    InvokedMethodNameListener listener = run(ExplicitDataProviderNameSample.class);

    assertThat(listener.getSucceedMethodNames())
        .containsExactly("should_find_exactly_one_data_provider(true)");
  }

  /**
   * Make sure that if a test method fails in the middle of a data provider, the rest of the data
   * set is still run.
   */
  @Test
  public void allMethodsShouldBeInvoked() {
    InvokedMethodNameListener listener = run(FailedDataProviderSample.class);

    assertThat(listener.getSucceedMethodNames()).containsExactly("f(1)", "f(3)");
    assertThat(listener.getFailedMethodNames()).containsExactly("f(2)");
  }

  @Test
  public void failedDataProviderShouldCauseSkip() {
    InvokedMethodNameListener listener = run(DependentSample.class);

    assertThat(listener.getSucceedMethodNames()).containsExactly("method1(ok)");
    assertThat(listener.getSkippedMethodNames()).containsExactly("method2");
    assertThat(listener.getFailedMethodNames()).containsExactly("method1(not ok)");
  }

  @Test
  public void inheritedDataProviderTest() {
    InvokedMethodNameListener listener = run(AnnotatedInheritedDataProviderSample.class);

    assertThat(listener.getSucceedMethodNames()).containsExactly("f(a)");
  }

  @Test
  public void instanceDataProviderTest() {
    InvokedMethodNameListener listener = run(InstanceDataProviderSampleFactory.class);

    assertThat(listener.getSucceedMethodNames())
        .hasSize(2)
        .are(new RegexCondition("f\\(-?\\d+\\)"));
  }

  @Test(enabled = false, description = "java 1.4 tests no more supported")
  public void jdk4IteratorTest() {
    TestNG tng = create(Jdk14IteratorSample.class);
    // tng.setAnnotationFinder(new JDK14AnnotationFinder());

    InvokedMethodNameListener listener = new InvokedMethodNameListener();
    tng.addListener(listener);

    tng.run();

    assertThat(listener.getSucceedMethodNames())
        .containsExactly("verifyNames(Cedric,36)", "verifyNames(Anne Marie,37)");
  }

  @Test
  public void methodTest() {
    MethodSample.m_test2 = 0;
    MethodSample.m_test3 = 0;

    InvokedMethodNameListener listener = run(MethodSample.class);

    assertThat(listener.getSucceedMethodNames())
        .containsExactly("test1(Cedric)", "test1(Alois)", "test2(Cedric)", "test3(Cedric)");
    Assert.assertEquals(MethodSample.m_test2, 1);
    Assert.assertEquals(MethodSample.m_test3, 1);
  }

  @Test
  public void constructorTest() {
    ConstructorSample.all = new ArrayList<>(2);

    InvokedMethodNameListener listener = run(ConstructorSample.class);

    assertThat(listener.getSucceedMethodNames()).containsExactly("test", "test");
    assertThat(ConstructorSample.all).containsExactlyInAnyOrder("Cedric", "Alois");
  }

  @Test
  public void constructorOrMethodTest() {
    InvokedMethodNameListener listener = run(ConstructorOrMethodSample.class);

    assertThat(listener.getSucceedMethodNames())
        .containsExactlyInAnyOrder(
            "test1", "test1",
            "test2(Cedric1)", "test2(Alois1)",
            "test2(Cedric0)", "test2(Alois0)");
  }

  @Test
  public void classInjectionTest() {
    InvokedMethodNameListener listener = run(ClassSample.class);

    assertThat(listener.getSucceedMethodNames())
        .containsExactlyInAnyOrder(
            "test1", "test1",
            "test2(Cedric1)", "test2(Alois1)",
            "test2(Cedric0)", "test2(Alois0)");
  }

  @Test
  public void iTestNGMethodTest() {
    InvokedMethodNameListener listener = run(ITestNGMethodSample.class);

    assertThat(listener.getSucceedMethodNames()).containsExactly("test1(Cedric)", "test1(Alois)");
  }

  @Test
  public void shouldNotThrowConcurrentModificationException() {
    InvokedMethodNameListener listener = run(ParallelDataProvider2Sample.class);

    assertThat(listener.getFailedMethodNames()).isEmpty();
    assertThat(listener.getSucceedMethodNames())
        .are(new RegexCondition("checkCME\\(\\d+\\)|null", true));
    // TODO null is not an expected value
    // .hasSize(2_000); TODO it is supposed to work
  }

  @Test(description = "GITHUB-2565", dataProvider = "2565")
  public void testForFunctionalInterfacesInLazyLoadingDataProviders(
      Class<?> cls, List<String> expected) {
    Data.INSTANCE.clear();
    run(cls);
    List<String> actualList = Data.INSTANCE.getData();
    assertThat(actualList).isEqualTo(expected);
  }

  @DataProvider(name = "2565")
  public Object[][] getTestDataFor2565() {
    return new Object[][] {
      {SampleTestUsingSupplier.class, Arrays.asList("Optimus_Prime", "Megatron")},
      {SampleTestUsingPredicate.class, Collections.singletonList("IronHide")},
      {SampleTestUsingFunction.class, Collections.singletonList("Bumble_Bee")},
      {SampleTestUsingConsumer.class, Collections.singletonList("StarScream")}
    };
  }

  public static class RegexCondition extends Condition<String> {

    private final String regex;
    private final boolean acceptNull;

    public RegexCondition(String regex) {
      this(regex, false);
    }

    public RegexCondition(String regex, boolean acceptNull) {
      this.regex = regex;
      this.acceptNull = acceptNull;
    }

    @Override
    public boolean matches(String value) {
      if (value == null) {
        if (acceptNull) {
          value = "null";
        } else {
          return false;
        }
      }
      return value.matches(regex);
    }
  }

  @Test
  public void parallelDataProviderSample() {
    InvokedMethodNameListener listener = run(ParallelDataProviderSample.class);

    assertThat(listener.getSucceedMethodNames())
        .hasSize(4)
        .are(
            new RegexCondition(
                "verifyData1\\(org\\.testng\\.TestRunner@\\p{XDigit}+,("
                    + "Cedric,36"
                    + "|"
                    + "Anne,37"
                    + "|"
                    + "A,36"
                    + "|"
                    + "B,37"
                    + ")\\)"));
  }

  @Test
  public void staticDataProviderTest() {
    InvokedMethodNameListener listener = run(StaticDataProviderSampleSample.class);

    assertThat(listener.getSucceedMethodNames())
        .containsExactly(
            "verifyConstructorInjection(Cedric)",
            "verifyExternal(Cedric)",
            "verifyFieldInjection(Cedric)",
            "verifyStatic(Cedric)");
  }

  @Test
  public void staticDataProviderSampleWithoutGuiceTest() {
    InvokedMethodNameListener listener = run(StaticDataProviderSampleWithoutGuiceSample.class);

    assertThat(listener.getSucceedMethodNames())
        .containsExactly("verifyExternal(Cedric)", "verifyStatic(Cedric)");
  }

  @Test
  public void testInstanceFactoryTest() {
    TestInstanceSample.m_instanceCount = 0;
    InvokedMethodNameListener listener = run(TestInstanceFactory.class);

    assertThat(TestInstanceSample.m_instanceCount).isEqualTo(2);
    assertThat(listener.getSucceedMethodNames())
        .containsExactly(
            "f(42)", "f(43)",
            "f(42)", "f(43)");
  }

  @Test
  public void testNG411Test() {
    InvokedMethodNameListener listener = run(TestNG411Sample.class);

    assertThat(listener.getSucceedMethodNames())
        .hasSize(1)
        .are(
            new RegexCondition(
                "checkMinTest_injection\\(1,2,org\\.testng\\.TestRunner@\\p{XDigit}+\\)"));
    assertThat(listener.getFailedBeforeInvocationMethodNames())
        .containsExactly("checkMaxTest", "checkMinTest");
  }

  @Test
  public void unnamedDataProviderTest() {
    InvokedMethodNameListener listener = run(UnnamedDataProviderSample.class);

    assertThat(listener.getSucceedMethodNames()).containsExactly("doStuff(true)", "doStuff(false)");
  }

  @Test
  public void varArgsDataProviderTest() {
    InvokedMethodNameListener listener = run(VarArgsDataProviderSample.class);

    assertThat(listener.getSucceedMethodNames())
        .containsExactly("testWithTwoEntriesInTestToolWindow([a,b,c])");
  }

  @Test
  public void createDataTest() {
    InvokedMethodNameListener listener = run(CreateDataTest.class);

    assertThat(listener.getSucceedMethodNames()).containsExactly("testMyTest(MyObject{})");
  }

  @Test
  public void testExceptions() {
    InvokedMethodNameListener listener = run(DataProviderIntegrationSample.class);
    Throwable exception = listener.getResult("theTest").getThrowable();
    assertThat(exception).isInstanceOf(MethodMatcherException.class);
  }

  @Test
  public void mixedVarArgsDataProviderTest() {
    InvokedMethodNameListener listener = run(GitHub513Sample.class);

    assertThat(listener.getSucceedMethodNames()).containsExactly("test(a,b,[c,d])");
  }

  @Test(description = "GITHUB1509")
  public void testDataProvidersThatReturnNull() {
    TestListenerAdapter tla = new TestListenerAdapter();
    TestNG tng = create(Github1509TestClassSample.class);
    tng.addListener(tla);
    tng.run();
    assertThat(tla.getFailedTests()).size().isEqualTo(1);
    ITestResult result = tla.getFailedTests().get(0);
    String className = Github1509TestClassSample.class.getName() + ".getData()";
    String msg =
        "Data Provider public java.lang.Object[][] " + className + " returned a null value";
    assertThat(result.getThrowable().getMessage()).contains(msg);
  }

  @Test
  public void ensureDataProviderNotInvokedMultipleTimesForRetriedTests() {
    TestNG testng = create(test.dataprovider.issue2884.TestClassSample.class);
    testng.run();
    assertThat(test.dataprovider.issue2884.TestClassSample.dataProviderInvocationCount.get())
        .isEqualTo(1);
  }

  @Test
  public void retryWithDataProvider() {
    TestNG testng = create(DataProviderRetryTest.class);
    TestListenerAdapter tla = new TestListenerAdapter();
    testng.addListener(tla);
    testng.run();
    assertThat(tla.getFailedTests()).size().isEqualTo(1);
    assertThat(tla.getSkippedTests()).size().isEqualTo(2);
  }

  @Test(description = "GITHUB-217", expectedExceptions = TestNGException.class)
  public void ensureTestNGThrowsExceptionWhenAllTestsAreSkipped() {
    TestNG testng = create(test.dataprovider.issue217.TestClassSample.class);
    testng.toggleFailureIfAllTestsWereSkipped(true);
    testng.run();
  }

  @Test(description = "GITHUB-217")
  public void ensureTestNGFailsDueToDataProviderFailure() {
    TestNG testng = create(test.dataprovider.issue217.TestClassSample.class);
    testng.propagateDataProviderFailureAsTestFailure();
    testng.run();
    assertThat(testng.getStatus()).isEqualTo(1);
  }

  @Test(description = "GITHUB-217")
  public void ensureTestNGFailsDueToDataProviderFailure2() {
    TestNG testng = create(test.dataprovider.issue217.AnotherTestClassSample.class);
    testng.run();
    assertThat(testng.getStatus()).isEqualTo(1);
  }

  @Test(description = "GITHUB-2888")
  public void ensureTestNGSkipExceptionWillSkipTestWithDataProvider() {
    TestNG testng = create(SkipDataProviderSample.class);
    testng.propagateDataProviderFailureAsTestFailure();
    testng.run();
    assertThat(testng.getStatus()).isEqualTo(2);
  }

  @Test(description = "GITHUB-2255")
  public void ensureDataProviderValuesAreVisibleToConfigMethods() {
    TestNG testNG = create(test.dataprovider.issue2255.TestClassSample.class);
    testNG.run();
    assertThat(test.dataprovider.issue2255.TestClassSample.data).containsExactly(100, 200);
  }

  @Test(dataProvider = "testData", description = "GITHUB-1987")
  public void extractDataProviderInfoWhenDpResidesInSameClass(
      Class<?> clazz, boolean performInstanceCheck, Class<?> dataProviderClass) {
    TestNG testng = create(clazz);
    DataProviderTrackingListener listener = new DataProviderTrackingListener();
    testng.addListener(listener);
    testng.run();
    ITestNGMethod method = listener.getResult().getMethod();
    IDataProviderMethod dpm = method.getDataProviderMethod();
    assertThat(dpm).isNotNull();
    if (performInstanceCheck) {
      assertThat(dpm.getInstance()).isEqualTo(method.getInstance());
    }
    assertThat(dpm.getMethod().getName()).isEqualTo("getData");
    assertThat(dpm.getInstance().getClass()).isEqualTo(dataProviderClass);
  }

  @DataProvider(name = "testData")
  public Object[][] getTestData() {
    return new Object[][] {
      {DataProviderInSameClassSample.class, true, DataProviderInSameClassSample.class},
      {DataProviderInBaseClassSample.class, true, DataProviderInBaseClassSample.class},
      {DataProviderInDifferentClassSample.class, false, BaseClassSample.class}
    };
  }

  @Test(description = "GITHUB-2267")
  public void ensureDynamicRetryAnalyzersAreHonouredForDataDrivenTest() {
    TestNG testng = create(test.dataprovider.issue2267.TestClassSample.class);
    TestListenerAdapter tla = new TestListenerAdapter();
    testng.addListener(tla);
    testng.run();
    assertThat(tla.getFailedTests()).size().isEqualTo(1);
    assertThat(tla.getSkippedTests()).size().isEqualTo(1);
  }

  @Test(description = "GITHUB-2327")
  public void ensureDataProviderParametersAreAlwaysAvailableForListeners() {
    TestNG testng = create(test.dataprovider.issue2327.TestClassSample.class);
    TestListenerAdapter tla = new TestListenerAdapter();
    testng.addListener(tla);
    testng.run();

    assertThat(tla.getSkippedTests().size()).isEqualTo(2);
    SoftAssertions assertions = new SoftAssertions();

    for (ITestResult skippedTest : tla.getSkippedTests()) {
      assertions.assertThat(skippedTest.getParameters()).isNotEmpty();
    }
    assertions.assertAll();
  }

  @Test(description = "GITHUB-2504")
  public void ensureParametersCopiedOnConfigFailures() {
    XmlTest xmltest = createXmlTest("2504_suite", "2504_test");
    xmltest.setXmlClasses(
        Collections.singletonList(new XmlClass(test.dataprovider.issue2504.TestClassSample.class)));
    TestNG testNG = create(Collections.singletonList(xmltest.getSuite()));
    SampleTestCaseListener listener = new SampleTestCaseListener();
    testNG.addListener(listener);
    testNG.run();
    assertThat(listener.getParameters()).containsExactlyElementsOf(Arrays.asList(1, 2, 3, 4, 5));
  }

  @Test(description = "GITHUB-2934")
  public void ensureParallelDataProviderWithRetryAnalyserWorks() {
    runTest(true);
  }

  @Test(description = "GITHUB-2934")
  public void ensureSequentialDataProviderWithRetryAnalyserWorks() {
    runTest(false);
  }

  @Test(
      description = "GITHUB-2980",
      expectedExceptions = UnsupportedOperationException.class,
      expectedExceptionsMessageRegExp =
          "Shared thread-pool for data providers is NOT COMPATIBLE with TestNG's custom thread pool executor")
  public void ensureErrorShownWhenUsedWithGraphThreadPoolExecutor() {
    try {
      System.setProperty(RuntimeBehavior.FAVOR_CUSTOM_THREAD_POOL_EXECUTOR, "true");
      runDataProviderTest(true);
    } finally {
      System.setProperty(RuntimeBehavior.FAVOR_CUSTOM_THREAD_POOL_EXECUTOR, "false");
    }
  }

  @Test(description = "GITHUB-2980", dataProvider = "dataProviderForIssue2980")
  public void ensureWeCanShareThreadPoolForDataProviders(
      boolean flag, Pair<List<String>, Integer> pair) {
    LoggingListener listener = runDataProviderTest(flag);
    assertThat(listener.getMethodNames())
        .withFailMessage("Ensuring that the method names along with parameters match.")
        .containsAll(pair.first());
    assertThat(listener.getThreadIds())
        .withFailMessage("Ensuring that the thread ids are correct")
        .hasSize(pair.second());
  }

  @Test(description = "GITHUB-2980", dataProvider = "getSuiteFileNames")
  public void ensureWeCanShareThreadPoolForDataProvidersThroughSuiteFiles(
      String fileName, Pair<List<String>, Integer> pair) {
    TestNG testng = create();
    testng.setTestSuites(Collections.singletonList(fileName));
    LoggingListener listener = new LoggingListener();
    testng.addListener(listener);
    testng.run();
    assertThat(listener.getMethodNames())
        .withFailMessage("Ensuring that the method names along with parameters match.")
        .containsAll(pair.first());
    assertThat(listener.getThreadIds())
        .withFailMessage("Ensuring that the thread ids are correct")
        .hasSize(pair.second());
  }

  @DataProvider
  public Object[][] getSuiteFileNames() {
    return new Object[][] {
      {
        "src/test/resources/2980_with_shared_threadpool_enabled.xml",
        new Pair<>(METHODS_ISSUE_2980, 5)
      },
      {
        "src/test/resources/2980_with_shared_threadpool_disabled.xml",
        new Pair<>(METHODS_ISSUE_2980, 10)
      }
    };
  }

  private LoggingListener runDataProviderTest(boolean flag) {
    TestNG testng = new TestNG();
    testng.setTestClasses(new Class[] {test.dataprovider.issue2980.TestClassSample.class});
    LoggingListener listener = new LoggingListener();
    testng.addListener(listener);
    testng.setDataProviderThreadCount(5);
    testng.shareThreadPoolForDataProviders(flag);
    testng.run();
    return listener;
  }

  private static final List<String> METHODS_ISSUE_2980 =
      Lists.newArrayList(
          "testMethod_[1]",
          "testMethod_[2]",
          "testMethod_[3]",
          "testMethod_[4]",
          "testMethod_[5]",
          "anotherTestMethod_[A]",
          "anotherTestMethod_[B]",
          "anotherTestMethod_[C]",
          "anotherTestMethod_[D]",
          "anotherTestMethod_[E]");

  @DataProvider
  public Object[][] dataProviderForIssue2980() {
    return new Object[][] {
      {true, new Pair<>(METHODS_ISSUE_2980, 5)},
      {false, new Pair<>(METHODS_ISSUE_2980, 10)}
    };
  }

  private static void runTest(boolean isParallel) {
    TestNG testng = create(TestCaseSample.class);
    CoreListener listener = new CoreListener();
    ToggleDataProvider transformer = new ToggleDataProvider(isParallel);
    testng.addListener(listener);
    testng.addListener(transformer);
    testng.setVerbose(2);
    testng.run();
    SoftAssertions softly = new SoftAssertions();

    softly
        .assertThat(listener.totalTests())
        .withFailMessage("We should have had 9 test results in total.")
        .isEqualTo(9);

    // Assert passed tests
    softly
        .assertThat(listener.getPassedTests())
        .withFailMessage("We should have had ONLY 1 passed test.")
        .hasSize(1);
    listener
        .getPassedTests()
        .forEach(
            each ->
                softly
                    .assertThat(each.wasRetried())
                    .withFailMessage(
                        "Passed test " + stringify(each) + " should NOT have been retried")
                    .isFalse());

    // Assert failed tests
    assertThat(listener.getFailedTests())
        .withFailMessage("We should have had 2 failed tests.")
        .hasSize(2);
    listener
        .getFailedTests()
        .forEach(
            each ->
                softly
                    .assertThat(each.wasRetried())
                    .withFailMessage(
                        "Failed test " + stringify(each) + " should NOT have been retried")
                    .isFalse());

    // Assert skipped tests
    assertThat(listener.getSkippedTests())
        .withFailMessage("We should have had 6 skipped tests due to retries.")
        .hasSize(6);
    listener
        .getSkippedTests()
        .forEach(
            each ->
                softly
                    .assertThat(each.wasRetried())
                    .withFailMessage(
                        "Skipped test " + stringify(each) + " should have been retried")
                    .isTrue());
    softly.assertAll();
  }

  private static String stringify(ITestResult itr) {
    return "(" + Arrays.toString(itr.getParameters()) + ")";
  }
}
