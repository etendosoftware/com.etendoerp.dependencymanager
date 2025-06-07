package com.etendoerp.dependencymanager.process;

import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.BUILD_DEPENDENCY_INFO;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.INVALID;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.NEW_VERSION;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.TEST_PACKAGE;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.UPDATED_ARTIFACT;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.VERSION;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.dependencymanager.data.PackageDependency;
import com.etendoerp.dependencymanager.data.PackageVersion;

/**
 * Comprehensive unit tests for SelectorChangeVersion class
 * Tests cover version compatibility, dependency comparison, and error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SelectorChangeVersion Tests")
class SelectorChangeVersionTest {

  private SelectorChangeVersion selectorChangeVersion;

  @Mock
  private OBDal mockOBDal;

  private MockedStatic<OBDal> obdalMockedStatic;
  private MockedStatic<OBContext> obcontextMockedStatic;
  private MockedStatic<OBMessageUtils> obMessageUtilsMockedStatic;

  /**
   * Initializes the required mocks and static contexts before each test execution.
   * Ensures a clean and controlled environment for every test case.
   */
  @BeforeEach
  void setUp() {
    selectorChangeVersion = new SelectorChangeVersion();

    obdalMockedStatic = mockStatic(OBDal.class);
    obcontextMockedStatic = mockStatic(OBContext.class);
    obMessageUtilsMockedStatic = mockStatic(OBMessageUtils.class);

    obdalMockedStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
    obcontextMockedStatic.when(OBContext::setAdminMode).thenAnswer(invocation -> null);
    obcontextMockedStatic.when(OBContext::restorePreviousMode).thenAnswer(invocation -> null);

    obMessageUtilsMockedStatic.when(() -> OBMessageUtils.messageBD(anyString()))
        .thenReturn("Mocked error message");

  }

  /**
   * Closes all static mocks after each test execution to prevent side effects
   * and ensure proper resource cleanup.
   */
  @AfterEach
  void tearDown() {
    if (obdalMockedStatic != null) obdalMockedStatic.close();
    if (obcontextMockedStatic != null) obcontextMockedStatic.close();
    if (obMessageUtilsMockedStatic != null) obMessageUtilsMockedStatic.close();
  }

  /**
   * Tests that the execute method throws an OBException when invalid JSON is provided.
   *
   * @throws OBException
   *     if the JSON content is invalid.
   */
  @Test
  @DisplayName("Should handle JSON exception properly")
  void testExecuteJSONException() {
    String invalidJson = "{ invalid json }";
    Map<String, Object> parameters = new HashMap<>();

    assertThrows(OBException.class, () ->
            selectorChangeVersion.execute(parameters, invalidJson),
        "Should throw OBException for invalid JSON"
    );

    obcontextMockedStatic.verify(OBContext::restorePreviousMode, times(1));
  }

  /**
   * Parameterized test that verifies the isCoreVersionCompatible method returns the expected result
   * for various version ranges and current core versions.
   *
   * @param currentVersion
   *     the current core version
   * @param requiredStart
   *     the required start version
   * @param requiredEnd
   *     the required end version
   * @param expectedResult
   *     the expected compatibility result
   */
  @ParameterizedTest
  @CsvSource({
      "24.1.0, 24.0.0, 25.0.0, true",   // Within range
      "24.0.0, 24.0.0, 25.0.0, true",   // At lower bound
      "25.0.0, 24.0.0, 25.0.0, true",   // At upper bound
      "23.9.0, 24.0.0, 25.0.0, false",  // Below range
      "25.1.0, 24.0.0, 25.0.0, false",  // Above range
      "24.5.0, 24.0.0, '', true",       // Empty upper bound should be compatible
      "26.0.0, 24.0.0, '', true"        // Empty upper bound allows higher versions
  })
  @DisplayName("Should check core version compatibility correctly")
  void testIsCoreVersionCompatible(String currentVersion, String requiredStart,
      String requiredEnd, boolean expectedResult) {
    boolean result = selectorChangeVersion.isCoreVersionCompatible(
        currentVersion, requiredStart, requiredEnd);

    assertEquals(expectedResult, result,
        String.format("Version %s should %s be compatible with range [%s, %s]",
            currentVersion, expectedResult ? "" : "not", requiredStart, requiredEnd));
  }

  /**
   * Tests that isCoreVersionCompatible returns false when any version string is invalid.
   */
  @Test
  @DisplayName("Should handle invalid version format gracefully")
  void testIsCoreVersionCompatibleInvalidFormat() {
    assertAll("Invalid version format handling",
        () -> assertFalse(selectorChangeVersion.isCoreVersionCompatible(INVALID, VERSION, NEW_VERSION)),
        () -> assertFalse(selectorChangeVersion.isCoreVersionCompatible(VERSION, INVALID, NEW_VERSION)),
        () -> assertFalse(selectorChangeVersion.isCoreVersionCompatible(VERSION, VERSION, INVALID))
    );
  }

  /**
   * Tests that processCoreDependency handles NullPointerException and returns an error in the JSON result.
   *
   * @throws Exception
   *     if reflection or JSON processing fails.
   */
  @Test
  @DisplayName("Should handle null pointer exception in processCoreDependency")
  void testProcessCoreDependencyNullPointerException() throws Exception {
    PackageVersion mockVersionWithNullDeps = mock(PackageVersion.class);
    when(mockVersionWithNullDeps.getETDEPPackageDependencyList()).thenThrow(new NullPointerException("Test NPE"));

    java.lang.reflect.Method method = SelectorChangeVersion.class
        .getDeclaredMethod("processCoreDependency", PackageVersion.class);
    method.setAccessible(true);
    JSONObject result = (JSONObject) method.invoke(selectorChangeVersion, mockVersionWithNullDeps);

    assertAll("NPE handling validation",
        () -> assertTrue(result.has("error"), "Should contain error field"),
        () -> assertTrue(result.getString("error").contains("Null value encountered"))
    );
  }

  /**
   * Tests that buildDependencyInfo correctly builds info for a new dependency.
   *
   * @throws Exception
   *     if reflection or JSON processing fails.
   */
  @Test
  @DisplayName("Should build dependency info correctly for new dependency")
  void testBuildDependencyInfoNewDependency() throws Exception {
    String key = "com.test:new-artifact";
    PackageDependency newDep = createMockDependency(TEST_PACKAGE, "new-artifact", VERSION);

    java.lang.reflect.Method method = SelectorChangeVersion.class
        .getDeclaredMethod(BUILD_DEPENDENCY_INFO, String.class, PackageDependency.class, PackageDependency.class);
    method.setAccessible(true);
    JSONObject result = (JSONObject) method.invoke(selectorChangeVersion, key, null, newDep);

    assertNotNull(result, "Should return dependency info");
    assertEquals(TEST_PACKAGE, result.getString("group"));
    assertEquals("new-artifact", result.getString("artifact"));
    assertEquals("", result.optString("versionV1", ""));
    assertEquals("[New Dependency]", result.getString("status"));
  }

  /**
   * Tests that buildDependencyInfo correctly builds info for an updated dependency.
   *
   * @throws Exception
   *     if reflection or JSON processing fails.
   */
  @Test
  @DisplayName("Should build dependency info correctly for updated dependency")
  void testBuildDependencyInfoUpdatedDependency() throws Exception {
    String key = "com.test:updated-artifact";
    PackageDependency oldDep = createMockDependency(TEST_PACKAGE, UPDATED_ARTIFACT, VERSION);
    PackageDependency newDep = createMockDependency(TEST_PACKAGE, UPDATED_ARTIFACT, NEW_VERSION);

    java.lang.reflect.Method method = SelectorChangeVersion.class
        .getDeclaredMethod(BUILD_DEPENDENCY_INFO, String.class, PackageDependency.class, PackageDependency.class);
    method.setAccessible(true);
    JSONObject result = (JSONObject) method.invoke(selectorChangeVersion, key, oldDep, newDep);

    assertNotNull(result, "Should return dependency info");
    assertEquals(TEST_PACKAGE, result.getString("group"));
    assertEquals(UPDATED_ARTIFACT, result.getString("artifact"));
    assertEquals("[Updated]", result.getString("status"));
  }

  /**
   * Tests that buildDependencyInfo returns null for unchanged dependencies.
   *
   * @throws Exception
   *     if reflection or JSON processing fails.
   */
  @Test
  @DisplayName("Should return null for unchanged dependency")
  void testBuildDependencyInfoUnchangedDependency() throws Exception {
    String key = "com.test:unchanged-artifact";
    PackageDependency sameDep1 = createMockDependency(TEST_PACKAGE, "unchanged-artifact", VERSION);
    PackageDependency sameDep2 = createMockDependency(TEST_PACKAGE, "unchanged-artifact", VERSION);

    java.lang.reflect.Method method = SelectorChangeVersion.class
        .getDeclaredMethod(BUILD_DEPENDENCY_INFO, String.class, PackageDependency.class, PackageDependency.class);
    method.setAccessible(true);
    JSONObject result = (JSONObject) method.invoke(selectorChangeVersion, key, sameDep1, sameDep2);

    assertNull(result, "Should return null for unchanged dependencies");
  }

  /**
   * Creates a mock instance of {@link PackageDependency} with the specified version.
   * This utility method is used to simplify the creation of mock dependencies for testing.
   *
   * @param group
   *     the group ID of the dependency (not used in the mock)
   * @param artifact
   *     the artifact ID of the dependency (not used in the mock)
   * @param version
   *     the version to be returned by {@code getVersion()}
   * @return a mocked {@link PackageDependency} with the specified version
   */
  private PackageDependency createMockDependency(String group, String artifact, String version) {
    PackageDependency dep = mock(PackageDependency.class);
    when(dep.getVersion()).thenReturn(version);
    return dep;
  }
}
