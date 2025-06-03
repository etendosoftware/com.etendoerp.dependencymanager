package com.etendoerp.dependencymanager.process;

import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.DEPENDENCIES;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.MESSAGE;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.MODULE_ETENDO;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.VERSION;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.VERSION_1_1_0;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
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
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.dependencymanager.data.Package;
import com.etendoerp.dependencymanager.util.DependencyManagerConstants;
import com.etendoerp.dependencymanager.util.PackageUtil;

/**
 * Unit tests for SelectLatestCompVersions class.
 * Covers dependency version checking, compatibility validation, and message construction.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SelectLatestCompVersions Tests")
class SelectLatestCompVersionsTest {

  @InjectMocks
  private SelectLatestCompVersions selectLatestCompVersions;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBCriteria<Package> mockCriteria;

  @Mock
  private Package mockPackage;

  private MockedStatic<OBDal> obDalMockedStatic;
  private MockedStatic<OBMessageUtils> obMessageUtilsMockedStatic;
  private MockedStatic<PackageUtil> packageUtilMockedStatic;

  private Map<String, Object> parameters;

  /**
   * Initializes the required mocks and parameters before each test execution.
   * Ensures a clean and isolated environment for every test case.
   */
  @BeforeEach
  void setUp() {
    obDalMockedStatic = mockStatic(OBDal.class);
    obMessageUtilsMockedStatic = mockStatic(OBMessageUtils.class);
    packageUtilMockedStatic = mockStatic(PackageUtil.class);

    obDalMockedStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    parameters = new HashMap<>();
  }

  /**
   * Closes all static mocks after each test to prevent side effects between tests.
   */
  @AfterEach
  void tearDown() {
    if (obDalMockedStatic != null) {
      obDalMockedStatic.close();
    }
    if (obMessageUtilsMockedStatic != null) {
      obMessageUtilsMockedStatic.close();
    }
    if (packageUtilMockedStatic != null) {
      packageUtilMockedStatic.close();
    }
  }

  /**
   * Configures the basic mock behaviors required for dependency version and message tests.
   * Mocks the creation of criteria, criteria chaining, and static message translations
   * to ensure predictable and isolated test execution.
   */
  private void setupBasicMocks() {
    when(mockOBDal.createCriteria(Package.class)).thenReturn(mockCriteria);
    when(mockCriteria.add(any())).thenReturn(mockCriteria);
    when(mockCriteria.setMaxResults(anyInt())).thenReturn(mockCriteria);

    obMessageUtilsMockedStatic.when(() -> OBMessageUtils.messageBD("ETDEP_Dependency_Update_Info"))
        .thenReturn("Dependency Update Information:");
    obMessageUtilsMockedStatic.when(() -> OBMessageUtils.messageBD("ETDEP_Updating_to_Latest"))
        .thenReturn("Updating to latest version");
  }

  /**
   * Tests the execution when a compatible dependency update is available.
   * Verifies that the response contains the correct message and dependency information.
   *
   * @throws Exception
   *     if there is an error during the execution of the test.
   */
  @Test
  @DisplayName("Should handle compatible dependency update successfully")
  void testExecuteCompatibleDependencyUpdate() throws Exception {
    setupBasicMocks();

    String content = createTestContent(MODULE_ETENDO, "test-module", VERSION);
    JSONObject compatibilityInfo = createCompatibilityInfo(true, VERSION_1_1_0, "1.0.0-2.0.0");

    when(mockCriteria.uniqueResult()).thenReturn(mockPackage);
    packageUtilMockedStatic.when(() -> PackageUtil.getCoreCompatibleOrLatestVersion(mockPackage))
        .thenReturn(VERSION_1_1_0);
    packageUtilMockedStatic.when(() -> PackageUtil.checkCoreCompatibility(mockPackage, VERSION_1_1_0))
        .thenReturn(compatibilityInfo);

    JSONObject result = selectLatestCompVersions.execute(parameters, content);

    assertAll("Compatible dependency update assertions",
        () -> assertNotNull(result, "Result should not be null"),
        () -> assertFalse(result.has("true"), "Should not have warning flag for compatible dependency"),
        () -> assertTrue(result.has(MESSAGE), "Should contain message"),
        () -> assertTrue(result.has(DEPENDENCIES), "Should contain dependencies array"),
        () -> {
          JSONArray deps = result.getJSONArray(DEPENDENCIES);
          assertEquals(1, deps.length(), "Should have one dependency");
          assertEquals("com.etendo.test-module", deps.getString(0), "Dependency name should match");
        },
        () -> {
          String message = result.getString(MESSAGE);
          assertTrue(message.contains("com.etendo.test-module"), "Message should contain dependency name");
          assertTrue(message.contains("Updating to latest version"), "Message should indicate update");
          assertTrue(message.contains(VERSION_1_1_0), "Message should contain new version");
        }
    );
  }

  /**
   * Tests the execution when no dependency updates are available.
   * Expects an empty response without message or dependencies.
   *
   * @throws Exception
   *     if there is an error during the execution of the test.
   */
  @Test
  @DisplayName("Should return empty response when no updates available")
  void testExecuteNoUpdatesAvailable() throws Exception {
    setupBasicMocks();

    String content = createTestContent(MODULE_ETENDO, "current-module", VERSION);
    JSONObject compatibilityInfo = createCompatibilityInfo(true, VERSION, "1.0.0-2.0.0");

    when(mockCriteria.uniqueResult()).thenReturn(mockPackage);
    packageUtilMockedStatic.when(() -> PackageUtil.getCoreCompatibleOrLatestVersion(mockPackage))
        .thenReturn(VERSION);
    packageUtilMockedStatic.when(() -> PackageUtil.checkCoreCompatibility(mockPackage, VERSION))
        .thenReturn(compatibilityInfo);

    JSONObject result = selectLatestCompVersions.execute(parameters, content);

    assertAll("No updates assertions",
        () -> assertNotNull(result, "Result should not be null"),
        () -> assertFalse(result.has(MESSAGE), "Should not have message when no updates"),
        () -> assertFalse(result.has(DEPENDENCIES), "Should not have dependencies when no updates"),
        () -> assertEquals(0, result.length(), "Result should be empty when no updates available")
    );
  }

  /**
   * Tests the execution when the package is not found (null).
   * Expects a NullPointerException to be thrown.
   *
   * @throws Exception
   *     if there is an error during the execution of the test.
   */
  @Test
  @DisplayName("Should handle null package gracefully")
  void testExecuteNullPackage() throws Exception {
    setupBasicMocks();

    String content = createTestContent(MODULE_ETENDO, "missing-module", VERSION);

    when(mockCriteria.uniqueResult()).thenReturn(null);

    assertThrows(NullPointerException.class, () -> selectLatestCompVersions.execute(parameters, content), "Should throw NullPointerException when package is not found");
  }

  /**
   * Tests the execution with malformed JSON content.
   * Expects an OBException to be thrown due to invalid JSON.
   */
  @Test
  @DisplayName("Should handle malformed JSON content")
  void testExecuteMalformedJSON() {
    String malformedContent = "{ invalid json }";

    assertThrows(OBException.class, () -> selectLatestCompVersions.execute(parameters, malformedContent), "Should throw OBException for malformed JSON");
  }

  /**
   * Creates a JSON string representing the test content for dependency selection.
   *
   * @param group
   *     the group of the dependency
   * @param artifact
   *     the artifact of the dependency
   * @param version
   *     the version of the dependency
   * @return a JSON string with the specified dependency information
   * @throws JSONException
   *     if there is an error creating the JSON object
   */
  private String createTestContent(String group, String artifact, String version) throws JSONException {
    JSONObject record = new JSONObject();
    record.put(DependencyManagerConstants.GROUP, group);
    record.put(DependencyManagerConstants.ARTIFACT, artifact);
    record.put(DependencyManagerConstants.VERSION, version);

    JSONArray records = new JSONArray();
    records.put(record);

    JSONObject content = new JSONObject();
    content.put("records", records);

    return content.toString();
  }

  /**
   * Creates a JSON object representing compatibility information for a dependency.
   *
   * @param isCompatible
   *     whether the dependency is compatible
   * @param coreVersionRange
   *     the compatible core version range
   * @param currentCoreVersion
   *     the current core version
   * @return a JSON object with compatibility information
   * @throws JSONException
   *     if there is an error creating the JSON object
   */
  private JSONObject createCompatibilityInfo(boolean isCompatible,
      String coreVersionRange, String currentCoreVersion) throws JSONException {
    JSONObject info = new JSONObject();
    info.put(PackageUtil.IS_COMPATIBLE, isCompatible);
    info.put(PackageUtil.CORE_VERSION_RANGE, coreVersionRange);
    info.put(PackageUtil.CURRENT_CORE_VERSION, currentCoreVersion);
    return info;
  }
}
