package org.testng.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

/** This class houses handling all JVM arguments by TestNG */
public final class RuntimeBehavior {

  public static final String TESTNG_THREAD_AFFINITY = "testng.thread.affinity";
  public static final String TESTNG_MODE_DRYRUN = "testng.mode.dryrun";
  private static final String TEST_CLASSPATH = "testng.test.classpath";
  private static final String SKIP_CALLER_CLS_LOADER = "skip.caller.clsLoader";
  public static final String TESTNG_USE_UNSECURED_URL = "testng.dtd.http";
  public static final String SHOW_TESTNG_STACK_FRAMES = "testng.show.stack.frames";
  private static final String MEMORY_FRIENDLY_MODE = "testng.memory.friendly";
  public static final String STRICTLY_HONOUR_PARALLEL_MODE = "testng.strict.parallel";
  public static final String TESTNG_DEFAULT_VERBOSE = "testng.default.verbose";
  public static final String IGNORE_CALLBACK_INVOCATION_SKIPS = "testng.ignore.callback.skip";
  public static final String SYMMETRIC_LISTENER_EXECUTION = "testng.listener.execution.symmetric";
  public static final String PREFERENTIAL_LISTENERS = "testng.preferential.listeners.package";
  public static final String FAVOR_CUSTOM_THREAD_POOL_EXECUTOR =
      "testng.favor.custom.thread-pool.executor";

  private RuntimeBehavior() {}

  public static boolean ignoreCallbackInvocationSkips() {
    return Boolean.getBoolean(IGNORE_CALLBACK_INVOCATION_SKIPS);
  }

  /**
   * @return - <code>true</code> if TestNG is to be using its custom implementation of {@link
   *     java.util.concurrent.ThreadPoolExecutor} for running concurrent tests. Defaults to <code>
   *     false</code>
   */
  public static boolean favourCustomThreadPoolExecutor() {
    return Boolean.getBoolean(FAVOR_CUSTOM_THREAD_POOL_EXECUTOR);
  }

  /**
   * @return - A comma separated list of packages that represent special listeners which users will
   *     expect to be executed after executing the regular listeners. Here special listeners can be
   *     anything that a user feels should be executed ALWAYS at the end.
   */
  public static List<String> getPreferentialListeners() {
    String packages =
        Optional.ofNullable(System.getProperty(PREFERENTIAL_LISTENERS)).orElse("com.intellij.rt.*");
    return Arrays.asList(packages.split(","));
  }

  public static boolean strictParallelism() {
    return Boolean.getBoolean(STRICTLY_HONOUR_PARALLEL_MODE);
  }

  public static boolean showTestNGStackFrames() {
    return Boolean.getBoolean(SHOW_TESTNG_STACK_FRAMES);
  }

  public static boolean useSecuredUrlForDtd() {
    return !Boolean.getBoolean(TESTNG_USE_UNSECURED_URL);
  }

  public static boolean isMemoryFriendlyMode() {
    return Boolean.parseBoolean(System.getProperty(MEMORY_FRIENDLY_MODE, "false"));
  }

  public static String unsecuredUrlDocumentation() {
    return "TestNG by default disables loading DTD from unsecured Urls. "
        + "If you need to explicitly load the DTD from a http url, please do so "
        + "by using the JVM argument [-D"
        + TESTNG_USE_UNSECURED_URL
        + "=true]";
  }

  public static String getDefaultLineSeparator() {
    return System.getProperty("line.separator");
  }

  public static String getCurrentUserHome() {
    return System.getProperty("user.home");
  }

  public static String getDefaultDataProviderThreadCount() {
    return System.getProperty("dataproviderthreadcount", "");
  }

  public static String getDefaultXmlGenerationImpl() {
    return System.getProperty("testng.xml.weaver", "org.testng.xml.DefaultXmlWeaver");
  }

  public static boolean isTestMode() {
    return Boolean.parseBoolean(System.getProperty("testng.testmode"));
  }

  public static boolean shouldSkipUsingCallerClassLoader() {
    return Boolean.parseBoolean(System.getProperty(SKIP_CALLER_CLS_LOADER));
  }

  public static boolean useStrictParameterMatching() {
    return Boolean.parseBoolean(System.getProperty("strictParameterMatch"));
  }

  public static String orderMethodsBasedOn() {
    return System.getProperty("testng.order", Systematiser.Order.INSTANCES.getValue());
  }

  public static String getTestClasspath() {
    return System.getProperty(TEST_CLASSPATH);
  }

  public static boolean useEmailableReporter() {
    return System.getProperty("noEmailableReporter") == null;
  }

  /**
   * @return - returns <code>true</code> if we would like to run in the Dry mode and <code>false
   *     </code> otherwise.
   */
  public static boolean isDryRun() {
    String value = System.getProperty(TESTNG_MODE_DRYRUN, "false");
    return Boolean.parseBoolean(value);
  }

  /**
   * @return - returns the {@link TimeZone} corresponding to the JVM argument <code>
   *     -Dtestng.timezone</code> if it was set. If not set, it returns the default timezone
   *     pertaining to the user property <code>user.timezone</code>
   */
  public static TimeZone getTimeZone() {
    String timeZone = System.getProperty("testng.timezone", "");
    if (timeZone.trim().isEmpty()) {
      return TimeZone.getDefault();
    }
    return TimeZone.getTimeZone(timeZone);
  }

  /**
   * @return - <code>true</code> if we would like to enforce Thread affinity when dealing with the
   *     below two variants of execution models:
   *     <ul>
   *       <li>Ordering priority
   *       <li>Ordering by dependsOnMethods (will not work with dependency on multiple methods)
   *     </ul>
   */
  public static boolean enforceThreadAffinity() {
    return Boolean.parseBoolean(System.getProperty(TESTNG_THREAD_AFFINITY, "false"));
  }

  /**
   * Returns the default verbosity level if not specified at the suite level.
   *
   * @return default XML suite verbosity level, or 1 if property is missing
   */
  public static int getDefaultVerboseLevel() {
    return Integer.getInteger(TESTNG_DEFAULT_VERBOSE, 1);
  }

  /**
   * @return - <code>true</code> if we would like to invoke AfterClass methods symmetrically to
   *     BeforeClass. When true, order is:
   *     <ol>
   *       <li>Class @afterClass methods
   *       <li>Listener onAfterClass methods
   *     </ol>
   *     When false, order is reversed.
   */
  public static boolean useSymmetricListenerExecution() {
    return Boolean.parseBoolean(System.getProperty(SYMMETRIC_LISTENER_EXECUTION, "false"));
  }
}
