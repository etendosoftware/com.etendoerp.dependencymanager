package com.etendoerp.dependencymanager.process;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.dependencymanager.data.Dependency;
import com.etendoerp.dependencymanager.data.Package;
import com.etendoerp.dependencymanager.data.PackageDependency;
import com.etendoerp.dependencymanager.data.PackageVersion;
import com.etendoerp.dependencymanager.util.PackageUtil;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeVersion Process Tests")
class ChangeVersionTest {

  @InjectMocks
  private ChangeVersion changeVersion;

  @Mock
  private SelectorChangeVersion mockSelector;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Dependency mockDependency;

  @Mock
  private PackageVersion mockPackageVersion;

  @Mock
  private Package mockPackage;

  @Mock
  private PackageVersion mockLatestVersion;

  private MockedStatic<OBDal> mockedStaticOBDal;
  private MockedStatic<OBContext> mockedStaticOBContext;
  private MockedStatic<OBMessageUtils> mockedStaticOBMessageUtils;
  private MockedStatic<PackageUtil> mockedStaticPackageUtil;

  @BeforeEach
  void setUp() {
    mockedStaticOBDal = mockStatic(OBDal.class);
    mockedStaticOBContext = mockStatic(OBContext.class);
    mockedStaticOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedStaticPackageUtil = mockStatic(PackageUtil.class);

    mockedStaticOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedStaticOBMessageUtils.when(() -> OBMessageUtils.messageBD(anyString())).thenReturn("Test message");

    changeVersion.selector = mockSelector;
  }

  @AfterEach
  void tearDown() {
    if (mockedStaticOBDal != null) mockedStaticOBDal.close();
    if (mockedStaticOBContext != null) mockedStaticOBContext.close();
    if (mockedStaticOBMessageUtils != null) mockedStaticOBMessageUtils.close();
    if (mockedStaticPackageUtil != null) mockedStaticPackageUtil.close();
  }

  @Nested
  @DisplayName("Successful Version Change Tests")
  class SuccessfulVersionChangeTests {

    @Test
    @DisplayName("Should successfully change to new package version")
    void shouldSuccessfullyChangeToNewPackageVersion() throws Exception {
      String dependencyId = "test-dependency-id";
      String newVersionId = "test-version-id";
      String content = createValidJsonContent(dependencyId, newVersionId, "false");
      Map<String, Object> parameters = new HashMap<>();

      setupSuccessfulVersionChangeMocks(dependencyId, newVersionId);

      JSONObject result = changeVersion.doExecute(parameters, content);

      assertAll("Version change should be successful",
          () -> assertNotNull(result, "Result should not be null"),
          () -> verify(mockDependency).setVersion("2.0.0"),
          () -> verify(mockDependency).setInstallationStatus("PENDING"),
          () -> verify(mockOBDal).save(mockDependency),
          () -> verify(mockOBDal).flush(),
          () -> {
            mockedStaticOBContext.verify(() -> OBContext.setAdminMode(true));
            mockedStaticOBContext.verify(OBContext::restorePreviousMode);
          }
      );
    }

    @Test
    @DisplayName("Should successfully change to external dependency version")
    void shouldSuccessfullyChangeToExternalDependencyVersion() throws Exception {
      String dependencyId = "test-dependency-id";
      String content = createExternalDependencyJsonContent(dependencyId, "3.0.0-EXTERNAL");
      Map<String, Object> parameters = new HashMap<>();

      setupExternalDependencyMocks(dependencyId);

      JSONObject result = changeVersion.doExecute(parameters, content);

      assertAll("External dependency change should be successful",
          () -> assertNotNull(result, "Result should not be null"),
          () -> verify(mockDependency).setVersion("3.0.0-EXTERNAL"),
          () -> verify(mockDependency).setInstallationStatus("PENDING"),
          () -> verify(mockDependency).setFormat("J"),
          () -> verify(mockOBDal).save(mockDependency),
          () -> verify(mockOBDal).flush()
      );
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should throw exception when dependency not found")
    void shouldThrowExceptionWhenDependencyNotFound() throws Exception {
      String dependencyId = "non-existent-dependency";
      String content = createValidJsonContent(dependencyId, "test-version-id", "false");
      Map<String, Object> parameters = new HashMap<>();

      when(mockOBDal.get(Dependency.class, dependencyId)).thenReturn(null);
      mockedStaticOBMessageUtils.when(() -> OBMessageUtils.messageBD("ETDEP_Package_Version_Not_Found_ID"))
          .thenReturn("Dependency not found: ");

      JSONObject result = changeVersion.doExecute(parameters, content);

      assertNotNull(result, "Result should not be null even on error");
      verify(mockOBDal, never()).save(any());
      mockedStaticOBContext.verify(OBContext::restorePreviousMode);
    }

    @Test
    @DisplayName("Should throw exception when package version not found")
    void shouldThrowExceptionWhenPackageVersionNotFound() throws Exception {
      String dependencyId = "test-dependency-id";
      String newVersionId = "non-existent-version";
      String content = createValidJsonContent(dependencyId, newVersionId, "false");
      Map<String, Object> parameters = new HashMap<>();

      when(mockOBDal.get(Dependency.class, dependencyId)).thenReturn(mockDependency);
      when(mockOBDal.get(PackageVersion.class, newVersionId)).thenReturn(null);
      mockedStaticOBMessageUtils.when(() -> OBMessageUtils.messageBD("ETDEP_Dependency_Not_Found_ID"))
          .thenReturn("Version not found: ");

      JSONObject result = changeVersion.doExecute(parameters, content);

      assertNotNull(result, "Result should not be null even on error");
      verify(mockOBDal, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when external version is empty")
    void shouldThrowExceptionWhenExternalVersionIsEmpty() throws Exception {
      String dependencyId = "test-dependency-id";
      String content = createExternalDependencyJsonContent(dependencyId, "");
      Map<String, Object> parameters = new HashMap<>();

      when(mockOBDal.get(Dependency.class, dependencyId)).thenReturn(mockDependency);
      mockedStaticOBMessageUtils.when(() -> OBMessageUtils.messageBD("ETDEP_External_Version_Empty"))
          .thenReturn("External version is empty");

      JSONObject result = changeVersion.doExecute(parameters, content);

      assertNotNull(result, "Result should not be null even on error");
      verify(mockOBDal, never()).save(any());
    }

    @Test
    @DisplayName("Should handle JSON parsing errors gracefully")
    void shouldHandleJSONParsingErrorsGracefully() {
      String invalidJsonContent = "{ invalid json }";
      Map<String, Object> parameters = new HashMap<>();

      mockedStaticOBMessageUtils.when(() -> OBMessageUtils.messageBD("ETDEP_Error_Updating_Dependency_Version"))
          .thenReturn("Error updating dependency version");

      JSONObject result = changeVersion.doExecute(parameters, invalidJsonContent);

      assertNotNull(result, "Result should not be null even on JSON error");
      mockedStaticOBContext.verify(OBContext::restorePreviousMode);
    }
  }

  @Nested
  @DisplayName("Dependency Processing Tests")
  class DependencyProcessingTests {

    @Test
    @DisplayName("Should build dependency info correctly for new dependency")
    void shouldBuildDependencyInfoCorrectlyForNewDependency() throws Exception {
      Map<String, PackageDependency> currentDeps = new HashMap<>();
      Map<String, PackageDependency> updateDeps = new HashMap<>();

      PackageDependency newDep = mock(PackageDependency.class);
      when(newDep.getVersion()).thenReturn("2.0.0");
      updateDeps.put("com.test:artifact", newDep);

      String key = "com.test:artifact";

      JSONObject result = changeVersion.buildDependencyInfo(currentDeps, updateDeps, key);

      assertAll("Dependency info should be built correctly",
          () -> assertEquals("com.test", result.getString("group")),
          () -> assertEquals("artifact", result.getString("artifact")),
          () -> assertEquals("", result.getString("version_v1")),
          () -> assertEquals("2.0.0", result.getString("version_v2")),
          () -> assertEquals("New Dependency", result.getString("status"))
      );
    }

    @Test
    @DisplayName("Should build dependency info correctly for updated dependency")
    void shouldBuildDependencyInfoCorrectlyForUpdatedDependency() throws Exception {
      Map<String, PackageDependency> currentDeps = new HashMap<>();
      Map<String, PackageDependency> updateDeps = new HashMap<>();

      PackageDependency currentDep = mock(PackageDependency.class);
      PackageDependency updatedDep = mock(PackageDependency.class);

      when(currentDep.getVersion()).thenReturn("1.0.0");
      when(updatedDep.getVersion()).thenReturn("2.0.0");

      String key = "com.test:artifact";
      currentDeps.put(key, currentDep);
      updateDeps.put(key, updatedDep);

      JSONObject result = changeVersion.buildDependencyInfo(currentDeps, updateDeps, key);

      assertAll("Updated dependency info should be built correctly",
          () -> assertEquals("com.test", result.getString("group")),
          () -> assertEquals("artifact", result.getString("artifact")),
          () -> assertEquals("1.0.0", result.getString("version_v1")),
          () -> assertEquals("2.0.0", result.getString("version_v2")),
          () -> assertEquals("Updated", result.getString("status"))
      );
    }

    @Test
    @DisplayName("Should build dependency info without status for unchanged dependency")
    void shouldBuildDependencyInfoWithoutStatusForUnchangedDependency() throws Exception {
      Map<String, PackageDependency> currentDeps = new HashMap<>();
      Map<String, PackageDependency> updateDeps = new HashMap<>();

      PackageDependency sameDep = mock(PackageDependency.class);
      when(sameDep.getVersion()).thenReturn("1.0.0");

      String key = "com.test:artifact";
      currentDeps.put(key, sameDep);
      updateDeps.put(key, sameDep);

      JSONObject result = changeVersion.buildDependencyInfo(currentDeps, updateDeps, key);

      assertAll("Unchanged dependency info should be built correctly",
          () -> assertEquals("com.test", result.getString("group")),
          () -> assertEquals("artifact", result.getString("artifact")),
          () -> assertEquals("1.0.0", result.getString("version_v1")),
          () -> assertEquals("1.0.0", result.getString("version_v2")),
          () -> assertFalse(result.has("status"), "Status should not be present for unchanged dependencies")
      );
    }
  }

  private String createValidJsonContent(String dependencyId, String versionId, String isExternal) throws JSONException {
    JSONObject content = new JSONObject();
    JSONObject params = new JSONObject();

    params.put("version", versionId);
    content.put("_params", params);
    content.put("inpetdepDependencyId", dependencyId);
    content.put("inpisexternaldependency", isExternal);

    return content.toString();
  }

  private String createExternalDependencyJsonContent(String dependencyId, String externalVersion) throws JSONException {
    JSONObject content = new JSONObject();
    JSONObject params = new JSONObject();

    params.put("version", "null");
    params.put("externalVersion", externalVersion);
    content.put("_params", params);
    content.put("inpetdepDependencyId", dependencyId);
    content.put("inpisexternaldependency", "true");

    return content.toString();
  }

  private void setupSuccessfulVersionChangeMocks(String dependencyId, String newVersionId) {
    when(mockOBDal.get(Dependency.class, dependencyId)).thenReturn(mockDependency);
    when(mockDependency.getEntityName()).thenReturn("Test Dependency");
    when(mockDependency.getVersion()).thenReturn("1.0.0");
    when(mockDependency.getGroup()).thenReturn("com.test");
    when(mockDependency.getArtifact()).thenReturn("test-artifact");

    when(mockOBDal.get(PackageVersion.class, newVersionId)).thenReturn(mockPackageVersion);
    when(mockPackageVersion.getVersion()).thenReturn("2.0.0");
    when(mockPackageVersion.getPackage()).thenReturn(mockPackage);

    when(mockLatestVersion.getVersion()).thenReturn("2.1.0");
    mockedStaticPackageUtil.when(() -> PackageUtil.getLastPackageVersion(mockPackage))
        .thenReturn(mockLatestVersion);

    when(mockSelector.fetchPackageByGroupAndArtifact("com.test", "test-artifact"))
        .thenReturn(mockPackage);
    when(mockSelector.getDependenciesMap(eq(mockPackage), anyString()))
        .thenReturn(new HashMap<>());

    mockedStaticPackageUtil.when(() -> PackageUtil.updateOrCreateDependency(anyString(), anyString(), anyString()))
        .thenAnswer(invocation -> null);
  }

  private void setupExternalDependencyMocks(String dependencyId) {
    when(mockOBDal.get(Dependency.class, dependencyId)).thenReturn(mockDependency);
  }
}
