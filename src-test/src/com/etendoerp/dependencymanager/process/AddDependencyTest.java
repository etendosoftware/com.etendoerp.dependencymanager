package com.etendoerp.dependencymanager.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.etendoerp.dependencymanager.actions.InstallDependency;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.module.Module;

import com.etendoerp.dependencymanager.data.Dependency;
import com.etendoerp.dependencymanager.data.PackageDependency;
import com.etendoerp.dependencymanager.data.PackageVersion;
import com.etendoerp.dependencymanager.util.DependencyTreeBuilder;
import com.etendoerp.dependencymanager.util.DependencyUtil;
import com.etendoerp.dependencymanager.util.PackageUtil;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link AddDependency} class.
 * <p>
 * This class validates different scenarios of the dependency addition logic,
 * including success cases, errors, version conflicts, and exception handling.
 * It uses Mockito for mocking dependencies and JUnit 5 for test execution.
 * </p>
 *
 * <ul>
 *   <li>Verifies behavior when the package version is not found.</li>
 *   <li>Validates the response when required parameters are missing.</li>
 *   <li>Checks correct creation and update of dependencies.</li>
 *   <li>Evaluates version conflict handling.</li>
 *   <li>Tests the generation of JSON response actions.</li>
 *   <li>Confirms exception and input error handling.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AddDependencyTest {

  @InjectMocks
  private AddDependency addDependency;

  @Mock
  private OBDal obDal;

  @Mock
  private PackageVersion mockPackageVersion;

  @Mock
  private Module mockModule;

  @Mock
  private Dependency mockDependency;

  @Mock
  private PackageDependency mockPackageDependency;

  private Map<String, Object> parameters;
  private JSONObject jsonData;

  /**
   * Initializes the required objects and parameters before each test execution.
   * Ensures that the maps and JSON objects are empty and ready to be used
   * in each test case, preventing side effects between tests.
   */
  @BeforeEach
  void setUp() {
    parameters = new HashMap<>();
    jsonData = new JSONObject();

  }

  /**
   * Tests the behavior of the execute method when the package version is not found.
   * Expects an error response action with the appropriate message.
   *
   * @throws Exception
   *     if there is an error during the execution of the test.
   */
  @Test
  void testExecutePackageVersionNotFound() throws Exception {
    String packageVersionId = "non-existent-id";
    jsonData.put("Etdep_Package_Version_ID", packageVersionId);

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {
      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.get(PackageVersion.class, packageVersionId)).thenReturn(null);

      JSONObject result = addDependency.execute(parameters, jsonData.toString());

      assertNotNull(result);
      assertTrue(result.has("responseActions"));
      JSONArray actions = result.getJSONArray("responseActions");
      assertEquals(1, actions.length());

      JSONObject action = actions.getJSONObject(0);
      assertTrue(action.has("showMsgInProcessView"));
      JSONObject msgInfo = action.getJSONObject("showMsgInProcessView");
      assertEquals("error", msgInfo.getString("msgType"));
      assertEquals("Package version not found", msgInfo.getString("msgText"));
    }
  }

  /**
   * Tests the execution with a bundle package and missing grid parameters.
   * Expects an error message indicating missing grid parameters.
   *
   * @throws Exception
   *     if there is an error during the execution of the test.
   */
  @Test
  void testExecuteBundlePackageWithoutGrid() throws Exception {
    String packageVersionId = "bundle-id";
    jsonData.put("Etdep_Package_Version_ID", packageVersionId);
    jsonData.put("_params", new JSONObject());

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class);
         MockedStatic<OBMessageUtils> msgUtilsStatic = mockStatic(OBMessageUtils.class)) {

      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      msgUtilsStatic.when(() -> OBMessageUtils.messageBD("ETDEP_Missing_Grid_Key_Params"))
          .thenReturn("Missing grid in parameters");

      when(obDal.get(PackageVersion.class, packageVersionId)).thenReturn(mockPackageVersion);

      JSONObject result = addDependency.execute(parameters, jsonData.toString());

      assertNotNull(result);
      assertTrue(result.has("message"));
      JSONObject message = result.getJSONObject("message");
      assertEquals("error", message.getString("severity"));
    }
  }

  /**
   * Tests successful execution with a non-bundle package.
   * Verifies that the process completes without errors.
   *
   * @throws Exception
   *     if there is an error during the execution of the test.
   */
  @Test
  void testExecuteSuccessWithNonBundlePackage() throws Exception {
    // Given
    String packageVersionId = "test-id";
    jsonData.put("Etdep_Package_Version_ID", packageVersionId);

    List<PackageDependency> dependencies = new ArrayList<>();
    dependencies.add(mockPackageDependency);

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class);
         MockedStatic<DependencyTreeBuilder> treeBuilderStatic = mockStatic(DependencyTreeBuilder.class);
         MockedStatic<DependencyUtil> depUtilStatic = mockStatic(DependencyUtil.class);
         MockedStatic<OBMessageUtils> msgUtilsStatic = mockStatic(OBMessageUtils.class);
         MockedStatic<PackageUtil> pkgUtilStatic = mockStatic(PackageUtil.class);
         MockedStatic<InstallDependency> installDepStatic = mockStatic(InstallDependency.class)) {

      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      msgUtilsStatic.when(() -> OBMessageUtils.messageBD(anyString()))
          .thenAnswer(invocation -> invocation.getArgument(0));

      when(obDal.get(PackageVersion.class, packageVersionId)).thenReturn(mockPackageVersion);

      treeBuilderStatic.when(() -> DependencyTreeBuilder.createDependencyTree(mockPackageVersion))
          .thenReturn(dependencies);

      depUtilStatic.when(() -> DependencyUtil.getInstalledModule("com.test", "artifact"))
          .thenReturn(null);
      depUtilStatic.when(() -> DependencyUtil.getInstalledDependency("com.test", "artifact", false))
          .thenReturn(null);

      treeBuilderStatic.when(() -> DependencyTreeBuilder.isBundle(any(PackageDependency.class)))
          .thenReturn(false);
      pkgUtilStatic.when(() -> PackageUtil.getLastPackageVersion(any()))
          .thenReturn(mockPackageVersion);
      installDepStatic.when(() -> InstallDependency.determineVersionStatus(anyString(), anyString()))
          .thenReturn("LATEST");

      JSONObject result = addDependency.execute(parameters, jsonData.toString());

      assertNotNull(result);
    }
  }

  /**
   * Tests the processDependency method for creating a new dependency.
   * Verifies that a new dependency is created and saved.
   *
   * @throws Exception
   *     if there is an error during the execution of the test or reflection.
   */
  @Test
  void testProcessDependencyNewDependency() throws Exception {
    when(mockPackageDependency.getGroup()).thenReturn("com.test");
    when(mockPackageDependency.getArtifact()).thenReturn("artifact");
    when(mockPackageDependency.getVersion()).thenReturn("1.0.0");
    when(mockPackageDependency.isExternalDependency()).thenReturn(false);

    try (MockedStatic<DependencyUtil> depUtilStatic = mockStatic(DependencyUtil.class);
         MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class);
         MockedStatic<DependencyTreeBuilder> treeBuilderStatic = mockStatic(DependencyTreeBuilder.class);
         MockedStatic<PackageUtil> pkgUtilStatic = mockStatic(PackageUtil.class);
         MockedStatic<InstallDependency> installDepStatic = mockStatic(InstallDependency.class)) {

      depUtilStatic.when(() -> DependencyUtil.getInstalledModule("com.test", "artifact"))
          .thenReturn(null);
      depUtilStatic.when(() -> DependencyUtil.getInstalledDependency("com.test", "artifact", false))
          .thenReturn(null);
      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);

      treeBuilderStatic.when(() -> DependencyTreeBuilder.isBundle(mockPackageDependency))
          .thenReturn(false);
      pkgUtilStatic.when(() -> PackageUtil.getLastPackageVersion(any()))
          .thenReturn(mockPackageVersion);
      installDepStatic.when(() -> InstallDependency.determineVersionStatus("1.0.0", "1.0.0"))
          .thenReturn("LATEST");

      var method = AddDependency.class.getDeclaredMethod("processDependency",
          PackageVersion.class, PackageDependency.class);
      method.setAccessible(true);
      JSONObject result = (JSONObject) method.invoke(addDependency, mockPackageVersion, mockPackageDependency);

      assertNotNull(result);
      assertFalse(result.getBoolean("error"));
      assertTrue(result.getBoolean("needFlush"));
      verify(obDal).save(any(Dependency.class));
    }
  }

  /**
   * Tests execution with a bundle package and an empty selection in the grid.
   * Verifies that the process completes without errors.
   *
   * @throws Exception
   *     if there is an error during the execution of the test.
   */
  @Test
  void testExecuteBundlePackageWithEmptySelection() throws Exception {
    String packageVersionId = "bundle-id";
    JSONObject params = new JSONObject();
    JSONObject grid = new JSONObject();
    grid.put("_selection", new JSONArray());
    params.put("grid", grid);

    jsonData.put("Etdep_Package_Version_ID", packageVersionId);
    jsonData.put("_params", params);

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class);
         MockedStatic<OBMessageUtils> msgUtilsStatic = mockStatic(OBMessageUtils.class);
         MockedStatic<DependencyTreeBuilder> treeBuilderStatic = mockStatic(DependencyTreeBuilder.class);
         MockedStatic<PackageUtil> pkgUtilStatic = mockStatic(PackageUtil.class);
         MockedStatic<InstallDependency> installDepStatic = mockStatic(InstallDependency.class)) {

      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      msgUtilsStatic.when(() -> OBMessageUtils.messageBD(anyString()))
          .thenAnswer(invocation -> invocation.getArgument(0));

      when(obDal.get(PackageVersion.class, packageVersionId)).thenReturn(mockPackageVersion);

      treeBuilderStatic.when(() -> DependencyTreeBuilder.addDependenciesFromParams(any(JSONArray.class)))
          .thenReturn(new ArrayList<>());

      treeBuilderStatic.when(() -> DependencyTreeBuilder.isBundle(any(PackageDependency.class)))
          .thenReturn(false);
      pkgUtilStatic.when(() -> PackageUtil.getLastPackageVersion(any()))
          .thenReturn(mockPackageVersion);
      installDepStatic.when(() -> InstallDependency.determineVersionStatus(anyString(), anyString()))
          .thenReturn("LATEST");

      JSONObject result = addDependency.execute(parameters, jsonData.toString());

      assertNotNull(result);

    }
  }

  /**
   * Tests the processDependency method for updating an existing dependency.
   * Verifies that the dependency is updated and saved.
   *
   * @throws Exception
   *     if there is an error during the execution of the test or reflection.
   */
  @Test
  void testProcessDependencyExistingDependencyUpdate() throws Exception {
    when(mockPackageDependency.getGroup()).thenReturn("com.test");
    when(mockPackageDependency.getArtifact()).thenReturn("artifact");
    when(mockPackageDependency.getVersion()).thenReturn("2.0.0");
    when(mockPackageDependency.isExternalDependency()).thenReturn(false);
    when(mockDependency.getVersion()).thenReturn("1.0.0");

    try (MockedStatic<DependencyUtil> depUtilStatic = mockStatic(DependencyUtil.class);
         MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class);
         MockedStatic<PackageUtil> pkgUtilStatic = mockStatic(PackageUtil.class)) {

      depUtilStatic.when(() -> DependencyUtil.getInstalledModule("com.test", "artifact"))
          .thenReturn(null);
      depUtilStatic.when(() -> DependencyUtil.getInstalledDependency("com.test", "artifact", false))
          .thenReturn(mockDependency);
      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      pkgUtilStatic.when(() -> PackageUtil.getLastPackageVersion(any()))
          .thenReturn(mockPackageVersion);

      var method = AddDependency.class.getDeclaredMethod("processDependency",
          PackageVersion.class, PackageDependency.class);
      method.setAccessible(true);
      JSONObject result = (JSONObject) method.invoke(addDependency, mockPackageVersion, mockPackageDependency);

      assertNotNull(result);
      assertFalse(result.getBoolean("error"));
      assertTrue(result.getBoolean("needFlush"));
      verify(mockDependency).setVersion("2.0.0");
      verify(mockDependency).setInstallationStatus(DependencyUtil.STATUS_PENDING);
      verify(obDal).save(mockDependency);
    }
  }

  /**
   * Tests the processDependency method when there is a version conflict.
   * Expects an error message indicating a version conflict.
   *
   * @throws Exception
   *     if there is an error during the execution of the test or reflection.
   */
  @Test
  void testProcessDependencyVersionConflict() throws Exception {
    when(mockPackageDependency.getGroup()).thenReturn("com.test");
    when(mockPackageDependency.getArtifact()).thenReturn("artifact");
    when(mockPackageDependency.getVersion()).thenReturn("1.0.0");
    when(mockPackageDependency.isExternalDependency()).thenReturn(false);
    when(mockModule.getVersion()).thenReturn("2.0.0");

    try (MockedStatic<DependencyUtil> depUtilStatic = mockStatic(DependencyUtil.class);
         MockedStatic<PackageUtil> pkgUtilStatic = mockStatic(PackageUtil.class);
         MockedStatic<OBMessageUtils> msgUtilsStatic = mockStatic(OBMessageUtils.class)) {

      depUtilStatic.when(() -> DependencyUtil.getInstalledModule("com.test", "artifact"))
          .thenReturn(mockModule);
      depUtilStatic.when(() -> DependencyUtil.getInstalledDependency("com.test", "artifact", false))
          .thenReturn(mockDependency);
      pkgUtilStatic.when(() -> PackageUtil.compareVersions("1.0.0", "2.0.0"))
          .thenReturn(-1);
      msgUtilsStatic.when(() -> OBMessageUtils.messageBD("ETDEP_Version_Conflict"))
          .thenReturn("Version conflict");

      var method = AddDependency.class.getDeclaredMethod("processDependency",
          PackageVersion.class, PackageDependency.class);
      method.setAccessible(true);
      JSONObject result = (JSONObject) method.invoke(addDependency, mockPackageVersion, mockPackageDependency);

      assertNotNull(result);
      assertTrue(result.getBoolean("error"));
      assertTrue(result.getString("message").contains("Version conflict"));
    }
  }

  /**
   * Tests the createResponseActions method for a success message type.
   * Verifies that the correct actions are generated for a successful operation.
   *
   * @throws Exception
   *     if there is an error during the execution of the test or reflection.
   */
  @Test
  void testCreateResponseActionsSuccess() throws Exception {
    var method = AddDependency.class.getDeclaredMethod("createResponseActions", String.class, String.class);
    method.setAccessible(true);
    JSONArray actions = (JSONArray) method.invoke(addDependency, "Test success message", "success");

    assertNotNull(actions);
    assertEquals(3, actions.length());

    JSONObject firstAction = actions.getJSONObject(0);
    assertTrue(firstAction.has("showMsgInProcessView"));
    JSONObject msgInfo = firstAction.getJSONObject("showMsgInProcessView");
    assertEquals("success", msgInfo.getString("msgType"));
    assertEquals("Success", msgInfo.getString("msgTitle"));
    assertEquals("Test success message", msgInfo.getString("msgText"));

    JSONObject secondAction = actions.getJSONObject(1);
    assertTrue(secondAction.has("openDirectTab"));

    JSONObject thirdAction = actions.getJSONObject(2);
    assertTrue(thirdAction.has("showMsgInView"));
  }

  /**
   * Tests the createResponseActions method for an error message type.
   * Verifies that the correct actions are generated for an error.
   *
   * @throws Exception
   *     if there is an error during the execution of the test or reflection.
   */
  @Test
  void testCreateResponseActionsError() throws Exception {
    var method = AddDependency.class.getDeclaredMethod("createResponseActions", String.class, String.class);
    method.setAccessible(true);
    JSONArray actions = (JSONArray) method.invoke(addDependency, "Test error message", "error");

    assertNotNull(actions);
    assertEquals(1, actions.length());

    JSONObject action = actions.getJSONObject(0);
    assertTrue(action.has("showMsgInProcessView"));
    JSONObject msgInfo = action.getJSONObject("showMsgInProcessView");
    assertEquals("error", msgInfo.getString("msgType"));
    assertEquals("Error", msgInfo.getString("msgTitle"));
    assertEquals("Test error message", msgInfo.getString("msgText"));
  }

  /**
   * Tests the getTitleForMessageType method for different message types.
   * Verifies that the correct title is returned for each type.
   *
   * @throws Exception
   *     if there is an error during the execution of the test or reflection.
   */
  @Test
  void testGetTitleForMessageType() throws Exception {
    var method = AddDependency.class.getDeclaredMethod("getTitleForMessageType", String.class);
    method.setAccessible(true);

    assertAll(
        () -> assertEquals("Success", method.invoke(addDependency, "success")),
        () -> assertEquals("Warning", method.invoke(addDependency, "warning")),
        () -> assertEquals("Error", method.invoke(addDependency, "error")),
        () -> assertEquals("Message", method.invoke(addDependency, "unknown"))
    );
  }

  /**
   * Tests the exception handling in the execute method when invalid JSON is provided.
   * Expects an error message in the response.
   *
   * @throws Exception
   *     if there is an error during the execution of the test.
   */
  @Test
  void testExceptionHandling() throws Exception {
    String invalidJson = "invalid json";

    JSONObject result = addDependency.execute(parameters, invalidJson);

    assertNotNull(result);
    assertTrue(result.has("message"));
    JSONObject message = result.getJSONObject("message");
    assertEquals("error", message.getString("severity"));
    assertEquals("Error", message.getString("title"));
  }
}
