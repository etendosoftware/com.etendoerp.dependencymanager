package com.etendoerp.dependencymanager.process;

import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.ARTIFACT;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.BOOLEAN_FALSE;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.GROUP;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.NEW_VERSION;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.PACK_ARTIFACT;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.RESULT_NOT_NULL_MESSAGE;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.STATUS;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.TEST_DEPENDENCY_ID;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.TEST_PACKAGE;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.VERSION;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.VERSION_V1;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.VERSION_V2;
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
      String dependencyId = TEST_DEPENDENCY_ID;
      String newVersionId = "test-version-id";
      String content = createValidJsonContent(dependencyId, newVersionId, BOOLEAN_FALSE);
      Map<String, Object> parameters = new HashMap<>();

      when(mockOBDal.get(Dependency.class, dependencyId)).thenReturn(mockDependency);
      when(mockDependency.getEntityName()).thenReturn("Test Dependency");
      when(mockDependency.getVersion()).thenReturn(VERSION);
      when(mockDependency.getGroup()).thenReturn(TEST_PACKAGE);
      when(mockDependency.getArtifact()).thenReturn("test-artifact");

      when(mockOBDal.get(PackageVersion.class, newVersionId)).thenReturn(mockPackageVersion);
      when(mockPackageVersion.getVersion()).thenReturn(NEW_VERSION);
      when(mockPackageVersion.getPackage()).thenReturn(mockPackage);

      when(mockLatestVersion.getVersion()).thenReturn("2.1.0");
      mockedStaticPackageUtil.when(() -> PackageUtil.getLastPackageVersion(mockPackage))
          .thenReturn(mockLatestVersion);

      when(mockSelector.fetchPackageByGroupAndArtifact(TEST_PACKAGE, "test-artifact"))
          .thenReturn(mockPackage);
      when(mockSelector.getDependenciesMap(eq(mockPackage), anyString()))
          .thenReturn(new HashMap<>());

      mockedStaticPackageUtil.when(() -> PackageUtil.updateOrCreateDependency(anyString(), anyString(), anyString()))
          .thenAnswer(invocation -> null);
      JSONObject result = changeVersion.doExecute(parameters, content);

      assertAll("Version change should be successful",
          () -> assertNotNull(result, "Result should not be null"),
          () -> verify(mockDependency).setVersion(NEW_VERSION),
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
      String dependencyId = TEST_DEPENDENCY_ID;
      String content = createExternalDependencyJsonContent(dependencyId, "3.0.0-EXTERNAL");
      Map<String, Object> parameters = new HashMap<>();

      when(mockOBDal.get(Dependency.class, dependencyId)).thenReturn(mockDependency);

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
      String content = createValidJsonContent(dependencyId, "test-version-id", BOOLEAN_FALSE);
      Map<String, Object> parameters = new HashMap<>();

      when(mockOBDal.get(Dependency.class, dependencyId)).thenReturn(null);
      mockedStaticOBMessageUtils.when(() -> OBMessageUtils.messageBD("ETDEP_Package_Version_Not_Found_ID"))
          .thenReturn("Dependency not found: ");

      JSONObject result = changeVersion.doExecute(parameters, content);

      assertNotNull(result, RESULT_NOT_NULL_MESSAGE);
      verify(mockOBDal, never()).save(any());
      mockedStaticOBContext.verify(OBContext::restorePreviousMode);
    }

    @Test
    @DisplayName("Should throw exception when package version not found")
    void shouldThrowExceptionWhenPackageVersionNotFound() throws Exception {
      String dependencyId = TEST_DEPENDENCY_ID;
      String newVersionId = "non-existent-version";
      String content = createValidJsonContent(dependencyId, newVersionId, BOOLEAN_FALSE);
      Map<String, Object> parameters = new HashMap<>();

      when(mockOBDal.get(Dependency.class, dependencyId)).thenReturn(mockDependency);
      when(mockOBDal.get(PackageVersion.class, newVersionId)).thenReturn(null);
      mockedStaticOBMessageUtils.when(() -> OBMessageUtils.messageBD("ETDEP_Dependency_Not_Found_ID"))
          .thenReturn("Version not found: ");

      JSONObject result = changeVersion.doExecute(parameters, content);

      assertNotNull(result, RESULT_NOT_NULL_MESSAGE);
      verify(mockOBDal, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when external version is empty")
    void shouldThrowExceptionWhenExternalVersionIsEmpty() throws Exception {
      String dependencyId = TEST_DEPENDENCY_ID;
      String content = createExternalDependencyJsonContent(dependencyId, "");
      Map<String, Object> parameters = new HashMap<>();

      when(mockOBDal.get(Dependency.class, dependencyId)).thenReturn(mockDependency);
      mockedStaticOBMessageUtils.when(() -> OBMessageUtils.messageBD("ETDEP_External_Version_Empty"))
          .thenReturn("External version is empty");

      JSONObject result = changeVersion.doExecute(parameters, content);

      assertNotNull(result, RESULT_NOT_NULL_MESSAGE);
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
      when(newDep.getVersion()).thenReturn(NEW_VERSION);
      updateDeps.put(PACK_ARTIFACT, newDep);

      String key = PACK_ARTIFACT;

      JSONObject result = changeVersion.buildDependencyInfo(currentDeps, updateDeps, key);

      assertAll("Dependency info should be built correctly",
          () -> assertEquals(TEST_PACKAGE, result.getString(GROUP)),
          () -> assertEquals(ARTIFACT, result.getString(ARTIFACT)),
          () -> assertEquals("", result.getString(VERSION_V1)),
          () -> assertEquals(NEW_VERSION, result.getString(VERSION_V2)),
          () -> assertEquals("New Dependency", result.getString(STATUS))
      );
    }

    @Test
    @DisplayName("Should build dependency info correctly for updated dependency")
    void shouldBuildDependencyInfoCorrectlyForUpdatedDependency() throws Exception {
      Map<String, PackageDependency> currentDeps = new HashMap<>();
      Map<String, PackageDependency> updateDeps = new HashMap<>();

      PackageDependency currentDep = mock(PackageDependency.class);
      PackageDependency updatedDep = mock(PackageDependency.class);

      when(currentDep.getVersion()).thenReturn(VERSION);
      when(updatedDep.getVersion()).thenReturn(NEW_VERSION);

      String key = PACK_ARTIFACT;
      currentDeps.put(key, currentDep);
      updateDeps.put(key, updatedDep);

      JSONObject result = changeVersion.buildDependencyInfo(currentDeps, updateDeps, key);

      assertAll("Updated dependency info should be built correctly",
          () -> assertEquals(TEST_PACKAGE, result.getString(GROUP)),
          () -> assertEquals(ARTIFACT, result.getString(ARTIFACT)),
          () -> assertEquals(VERSION, result.getString(VERSION_V1)),
          () -> assertEquals(NEW_VERSION, result.getString(VERSION_V2)),
          () -> assertEquals("Updated", result.getString(STATUS))
      );
    }

    @Test
    @DisplayName("Should build dependency info without status for unchanged dependency")
    void shouldBuildDependencyInfoWithoutStatusForUnchangedDependency() throws Exception {
      Map<String, PackageDependency> currentDeps = new HashMap<>();
      Map<String, PackageDependency> updateDeps = new HashMap<>();

      PackageDependency sameDep = mock(PackageDependency.class);
      when(sameDep.getVersion()).thenReturn(VERSION);

      String key = PACK_ARTIFACT;
      currentDeps.put(key, sameDep);
      updateDeps.put(key, sameDep);

      JSONObject result = changeVersion.buildDependencyInfo(currentDeps, updateDeps, key);

      assertAll("Unchanged dependency info should be built correctly",
          () -> assertEquals(TEST_PACKAGE, result.getString(GROUP)),
          () -> assertEquals(ARTIFACT, result.getString(ARTIFACT)),
          () -> assertEquals(VERSION, result.getString(VERSION_V1)),
          () -> assertEquals(VERSION, result.getString(VERSION_V2)),
          () -> assertFalse(result.has(STATUS), "Status should not be present for unchanged dependencies")
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
}
