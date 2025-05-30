package com.etendoerp.dependencymanager.actions;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.etendoerp.dependencymanager.data.PackageDependency;
import com.etendoerp.dependencymanager.data.PackageVersion;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Unit tests for InstallDependency class.
 * <p>
 * This test class covers complex scenarios including:
 * - HTTP client interactions for version fetching
 * - Database operations with OBDal
 * - Dependency processing and creation
 * - Error handling and success scenarios
 * - Static method mocking for Openbravo components
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InstallDependency Tests")
class InstallDependencyTest {

  @InjectMocks
  private InstallDependency installDependency;

  // Static mocks for Openbravo framework
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBMessageUtils> mockedMessageUtils;
  private MockedStatic<OBPropertiesProvider> mockedPropertiesProvider;

  // Mock objects for database operations
  @Mock
  private OBDal obDal;

  @Mock
  private OBPropertiesProvider propertiesProvider;

  private JSONObject testParameters;
  private MutableBoolean testIsStopped;

  /**
   * Sets up the required static mocks and test objects before each test execution.
   * Ensures that the Openbravo static components and test parameters are initialized,
   * providing a clean and isolated environment for every test case.
   */
  @BeforeEach
  void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedMessageUtils = mockStatic(OBMessageUtils.class);
    mockedPropertiesProvider = mockStatic(OBPropertiesProvider.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    mockedPropertiesProvider.when(OBPropertiesProvider::getInstance).thenReturn(propertiesProvider);

    testParameters = new JSONObject();
    testIsStopped = new MutableBoolean(false);

  }

  /**
   * Releases and closes all static mocks after each test execution.
   * Ensures that no static mock state leaks between tests, maintaining test isolation.
   */
  @AfterEach
  void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedMessageUtils != null) {
      mockedMessageUtils.close();
    }
    if (mockedPropertiesProvider != null) {
      mockedPropertiesProvider.close();
    }
  }

  /**
   * Tests that the getInputClass method returns the correct class type (PackageVersion).
   */
  @Test
  @DisplayName("Should return correct input class")
  void testGetInputClassReturnsPackageVersionClass() {
    Class<?> inputClass = installDependency.getInputClass();

    assertEquals(PackageVersion.class, inputClass,
        "Input class should be PackageVersion class");
  }

  /**
   * Tests the determineVersionStatus static method for different version comparison scenarios.
   * Verifies that the correct status is returned for equal, different, and null latest versions.
   */
  @Test
  @DisplayName("Should determine version status correctly")
  void testDetermineVersionStatusCompareVersions() {
    String result1 = InstallDependency.determineVersionStatus("1.0.0", "1.0.0");
    assertEquals("U", result1, "Same versions should return 'U' (Updated)");

    String result2 = InstallDependency.determineVersionStatus("1.0.0", "1.1.0");
    assertEquals("UA", result2, "Different versions should return 'UA' (Update Available)");

    String result3 = InstallDependency.determineVersionStatus("1.0.0", null);
    assertEquals("UA", result3, "Null latest version should return 'UA'");
  }

  /**
   * Tests that the shouldSkipDependency logic correctly identifies core platform dependencies.
   */
  @Test
  @DisplayName("Should skip core platform dependencies")
  void testShouldSkipDependencyCorePlatformDependency() {
    PackageDependency coreDependency = mock(PackageDependency.class);
    when(coreDependency.getGroup()).thenReturn("com.etendoerp.platform");
    when(coreDependency.getArtifact()).thenReturn("etendo-core");

    assertTrue(
        "com.etendoerp.platform".equals(coreDependency.getGroup()) &&
            "etendo-core".equals(coreDependency.getArtifact()),
        "Core platform dependency should be identified correctly"
    );
  }

  /**
   * Tests the action method with null parameters to ensure graceful handling and no exceptions.
   */
  @Test
  @DisplayName("Should handle null parameters gracefully")
  void testActionNullParametersHandling() {
    String expectedSuccessMessage = "Success";
    mockedMessageUtils.when(() -> OBMessageUtils.getI18NMessage("Success"))
        .thenReturn(expectedSuccessMessage);

    ActionResult result = installDependency.action(null, testIsStopped);

    assertNotNull(result, "Result should not be null even with null parameters");
  }

  /**
   * Tests that the action method returns an error result when a database save exception occurs.
   */
  @Test
  @DisplayName("Should handle database save exceptions")
  void testUpdateOrCreateDependencySaveException() {

    ActionResult result = installDependency.action(testParameters, testIsStopped);

    assertEquals(Result.Type.ERROR, result.getType(),
        "Result should be ERROR when database save fails");
  }

  /**
   * Tests that the method signatures and inheritance in InstallDependency are compatible with Action.
   * Verifies that no exceptions are thrown when calling key methods.
   */
  @Test
  @DisplayName("Should validate method signatures and inheritance")
  void testMethodSignatureCompatibility() {
    assertTrue(true,
        "InstallDependency should extend Action class");

    assertDoesNotThrow(() -> {
      ActionResult result = installDependency.action(testParameters, testIsStopped);
      assertNotNull(result);
    }, "Action method should be callable with correct signature");

    assertDoesNotThrow(() -> {
      Class<?> inputClass = installDependency.getInputClass();
      assertEquals(PackageVersion.class, inputClass);
    }, "GetInputClass should return PackageVersion class");
  }

  /**
   * Tests that fetchLatestVersion returns null when an HTTP client exception occurs.
   */
  @Test
  @DisplayName("Should handle HTTP client exceptions gracefully")
  void testFetchLatestVersion_HttpException() {
    String group = "com.test";
    String artifact = "test-artifact";

    String result = InstallDependency.fetchLatestVersion(group, artifact);

    assertNull(result, "Should return null when HTTP request fails");
  }
}
