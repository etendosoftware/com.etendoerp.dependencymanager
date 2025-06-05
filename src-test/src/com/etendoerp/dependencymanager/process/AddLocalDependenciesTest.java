package com.etendoerp.dependencymanager.process;

import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.MESSAGE;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.TEST_CONTENT;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.module.Module;
import org.openbravo.service.db.DbUtility;

import com.etendoerp.dependencymanager.data.Dependency;
import com.etendoerp.dependencymanager.util.DependencyUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Comprehensive test suite for AddLocalDependencies class.
 * Demonstrates best practices for mocking OBDal, OBContext, and handling static methods.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddLocalDependencies Tests")
class AddLocalDependenciesTest {

  @Mock
  private OBDal mockOBDal;
  @Mock
  private OBProvider mockOBProvider;
  @Mock
  private OBCriteria<Dependency> mockDependencyCriteria;
  @Mock
  private OBCriteria<Module> mockModuleCriteria;
  @Mock
  private Dependency mockDependency;
  @Mock
  private Module mockModule;

  private MockedStatic<OBDal> mockedOBDalStatic;
  private MockedStatic<OBContext> mockedOBContextStatic;
  private MockedStatic<OBProvider> mockedOBProviderStatic;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<DbUtility> mockedDbUtility;
  private MockedStatic<DependencyUtil> mockedDependencyUtil;

  @InjectMocks
  private AddLocalDependencies addLocalDependencies;

  /**
   * Sets up the test environment, including mocked static instances and default behaviors.
   */
  @BeforeEach
  void setUp() {
    mockedOBDalStatic = mockStatic(OBDal.class);
    mockedOBContextStatic = mockStatic(OBContext.class);
    mockedOBProviderStatic = mockStatic(OBProvider.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedDbUtility = mockStatic(DbUtility.class);
    mockedDependencyUtil = mockStatic(DependencyUtil.class);

    mockedOBDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedOBProviderStatic.when(OBProvider::getInstance).thenReturn(mockOBProvider);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(anyString())).thenReturn("Test Message");
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(eq("ETDEP_Deps_Successfully_Added")))
        .thenReturn("Successfully added %d dependencies");
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(eq("ProcessRunError")))
        .thenReturn("Process Run Error: ");
  }

  /**
   * Cleans up the mocked static instances after each test.
   */
  @AfterEach
  void tearDown() {
    if (mockedOBDalStatic != null) mockedOBDalStatic.close();
    if (mockedOBContextStatic != null) mockedOBContextStatic.close();
    if (mockedOBProviderStatic != null) mockedOBProviderStatic.close();
    if (mockedOBMessageUtils != null) mockedOBMessageUtils.close();
    if (mockedDbUtility != null) mockedDbUtility.close();
    if (mockedDependencyUtil != null) mockedDependencyUtil.close();
  }

  /**
   * Tests the successful addition of local dependencies when modules exist.
   */
  @Test
  @DisplayName("Should successfully add local dependencies when modules exist")
  void shouldSuccessfullyAddLocalDependencies() {
    setupSuccessfulExecution();

    JSONObject result = addLocalDependencies.execute(new HashMap<>(), TEST_CONTENT);

    assertAll("Success response validation",
        () -> assertNotNull(result, "Result should not be null"),
        () -> assertTrue(result.has(MESSAGE), "Result should have message"),
        () -> {
          JSONObject message = result.getJSONObject(MESSAGE);
          assertAll("Message content validation",
              () -> assertEquals("success", message.getString("severity"), "Severity should be success"),
              () -> assertEquals("Success", message.getString("title"), "Title should be Success"),
              () -> assertTrue(message.getString("text").contains("2"), "Text should mention 2 dependencies")
          );
        }
    );

    mockedOBContextStatic.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContextStatic.verify(OBContext::restorePreviousMode, times(1));

    verify(mockOBProvider, times(2)).get(Dependency.class);
    verify(mockOBDal, times(2)).save(any(Dependency.class));
    verify(mockOBDal, times(1)).flush();
  }

  /**
   * Tests the exclusion of core modules from processing.
   */
  @Test
  @DisplayName("Should handle case when no modules need to be added")
  void shouldHandleNoModulesToAdd() {
    setupNoModulesScenario();

    JSONObject result = addLocalDependencies.execute(new HashMap<>(), TEST_CONTENT);

    assertAll("No modules scenario validation",
        () -> assertNotNull(result),
        () -> assertTrue(result.has(MESSAGE)),
        () -> {
          JSONObject message = result.getJSONObject(MESSAGE);
          assertEquals("success", message.getString("severity"));
          assertTrue(message.getString("text").contains("0"));
        }
    );

    verify(mockOBProvider, never()).get(Dependency.class);
    verify(mockOBDal, never()).save(any(Dependency.class));
  }

  /**
   * Sets up a successful execution scenario with mock dependencies and modules.
   */
  private void setupSuccessfulExecution() {
    List<Dependency> existingDeps = Arrays.asList(
        createMockDependency("com.existing", "dependency1"),
        createMockDependency("org.sample", "dependency2")
    );

    when(mockOBDal.createCriteria(Dependency.class)).thenReturn(mockDependencyCriteria);
    when(mockDependencyCriteria.list()).thenReturn(existingDeps);

    List<Module> modulesToAdd = Arrays.asList(
        createMockModule("com.example.new.module1", "1.0.0"),
        createMockModule("org.custom.new.module2", "2.0.0")
    );

    when(mockOBDal.createCriteria(Module.class)).thenReturn(mockModuleCriteria);
    when(mockModuleCriteria.add(any())).thenReturn(mockModuleCriteria);
    when(mockModuleCriteria.list()).thenReturn(modulesToAdd);

    when(mockOBProvider.get(Dependency.class)).thenReturn(mockDependency);
  }

  /**
   * Sets up a scenario where no modules need to be added.
   */
  private void setupNoModulesScenario() {
    when(mockOBDal.createCriteria(Dependency.class)).thenReturn(mockDependencyCriteria);
    when(mockDependencyCriteria.list()).thenReturn(new ArrayList<>());

    when(mockOBDal.createCriteria(Module.class)).thenReturn(mockModuleCriteria);
    when(mockModuleCriteria.add(any())).thenReturn(mockModuleCriteria);
    when(mockModuleCriteria.list()).thenReturn(new ArrayList<>());
  }

  /**
   * Creates a mock `Dependency` entity with the specified group and artifact.
   *
   * @param group
   *     the group of the dependency.
   * @param artifact
   *     the artifact of the dependency.
   * @return the mocked `Dependency` entity.
   */
  private Dependency createMockDependency(String group, String artifact) {
    Dependency dep = mock(Dependency.class);
    when(dep.getGroup()).thenReturn(group);
    when(dep.getArtifact()).thenReturn(artifact);
    return dep;
  }

  /**
   * Creates a mock `Module` entity with the specified package, version, and name.
   *
   * @param javaPackage
   *     the Java package of the module.
   * @param version
   *     the version of the module.
   * @return the mocked `Module` entity.
   */
  private Module createMockModule(String javaPackage, String version) {
    Module module = mock(Module.class);
    when(module.getJavaPackage()).thenReturn(javaPackage);
    when(module.getVersion()).thenReturn(version);
    return module;
  }

  /**
   * Tests the exclusion of core modules from processing.
   * Ensures that no dependencies are added when only core modules exist.
   *
   * @throws JSONException
   *     if there is an error during JSON processing.
   */
  @Test
  @DisplayName("Should properly exclude core modules from processing")
  void shouldExcludeCoreModules() throws JSONException {
    when(mockOBDal.createCriteria(Dependency.class)).thenReturn(mockDependencyCriteria);
    when(mockDependencyCriteria.list()).thenReturn(new ArrayList<>());

    when(mockOBDal.createCriteria(Module.class)).thenReturn(mockModuleCriteria);
    when(mockModuleCriteria.add(any())).thenReturn(mockModuleCriteria);
    when(mockModuleCriteria.list()).thenReturn(new ArrayList<>());

    JSONObject result = addLocalDependencies.execute(new HashMap<>(), TEST_CONTENT);

    JSONObject message = result.getJSONObject(MESSAGE);
    assertTrue(message.getString("text").contains("0"),
        "Should not add any dependencies when only core modules exist");

    verify(mockModuleCriteria, times(2)).add(any());
  }
}
