package com.etendoerp.dependencymanager.actions;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.module.Module;

import com.etendoerp.dependencymanager.data.Dependency;
import com.etendoerp.dependencymanager.data.Package;
import com.etendoerp.dependencymanager.util.ChangeFormatUtil;
import com.etendoerp.dependencymanager.util.DependencyUtil;
import com.etendoerp.dependencymanager.util.PackageUtil;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Data;
import com.smf.jobs.Result;

/**
 * Unit tests for the {@link ChangeFormat} action, which handles changing the format of dependencies
 * in the EtendoERP dependency manager.
 * <p>
 * This test class uses JUnit 5 and Mockito to mock the persistence layer and utility classes,
 * and verifies the behavior of the action in various scenarios, including successful format changes,
 * error handling, and reflection-based input manipulation.
 * </p>
 *
 * <ul>
 *   <li>Verifies successful format changes (e.g., LOCAL to SOURCE, to JAR, etc.)</li>
 *   <li>Handles error scenarios such as missing dependency packages</li>
 *   <li>Tests reflection helpers for manipulating action input</li>
 *   <li>Validates the structure of returned results</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeFormat Action Tests with Reflection")
class ChangeFormatTest {
  /**
   * Helper class for manipulating the input of the action using reflection.
   * Allows injection and retrieval of mock data for testing purposes.
   */
  static class ReflectionTestHelper {
    private static Data currentMockData = null;

    /**
     * Sets the input contents of the action using reflection.
     *
     * @param action
     *     The action instance to modify.
     * @param entityClass
     *     The entity class of the input.
     * @param contents
     *     The list of mock input objects.
     * @param <T>
     *     The entity type.
     */
    @SuppressWarnings("unchecked")
    public static <T extends BaseOBObject> void setInputContents(Object action, Class<T> entityClass,
        List<T> contents) {
      try {
        currentMockData = mock(Data.class);
        when(currentMockData.getContents(entityClass)).thenReturn(contents);

        Method setInputMethod = action.getClass().getSuperclass().getDeclaredMethod("setInput", Data.class);
        setInputMethod.setAccessible(true);
        setInputMethod.invoke(action, currentMockData);

        try {
          Field inputField = action.getClass().getSuperclass().getDeclaredField("input");
          inputField.setAccessible(true);
          inputField.set(action, currentMockData);
        } catch (NoSuchFieldException e) {
        }

      } catch (Exception e) {
        throw new RuntimeException("Error setting input contents via reflection", e);
      }
    }

    /**
     * Retrieves the input contents of the action using reflection.
     *
     * @param action
     *     The action instance.
     * @param entityClass
     *     The entity class of the input.
     * @param <T>
     *     The entity type.
     * @return The list of input objects.
     */
    @SuppressWarnings("unchecked")
    public static <T extends BaseOBObject> List<T> getInputContents(Object action, Class<T> entityClass) {
      try {
        Method getInputContentsMethod = action.getClass().getSuperclass()
            .getDeclaredMethod("getInputContents", Class.class);
        getInputContentsMethod.setAccessible(true);
        List<T> result = (List<T>) getInputContentsMethod.invoke(action, entityClass);

        if (result == null || result.isEmpty()) {
          if (currentMockData != null) {
            result = currentMockData.getContents(entityClass);
          }
        }

        return result != null ? result : Collections.emptyList();
      } catch (Exception e) {
        if (currentMockData != null) {
          return currentMockData.getContents(entityClass);
        }
        throw new RuntimeException("Error getting input contents via reflection", e);
      }
    }

    /**
     * Retrieves the input contents as a list of BaseOBObject.
     *
     * @param action
     *     The action instance.
     * @return The list of input objects.
     */
    public static List<BaseOBObject> getInputContents(Object action) {
      try {
        Method getInputContentsMethod = action.getClass().getSuperclass()
            .getDeclaredMethod("getInputContents");
        getInputContentsMethod.setAccessible(true);
        List<BaseOBObject> result = (List<BaseOBObject>) getInputContentsMethod.invoke(action);

        if (result == null || result.isEmpty()) {
          if (currentMockData != null) {
            result = currentMockData.getContents();
          }
        }

        return result != null ? result : Collections.emptyList();
      } catch (Exception e) {
        if (currentMockData != null) {
          return currentMockData.getContents();
        }
        throw new RuntimeException("Error getting input contents via reflection", e);
      }
    }

    /**
     * Cleans up the internal state of the helper.
     */
    public static void cleanup() {
      currentMockData = null;
    }
  }

  @Mock
  private OBDal mockOBDal;
  @Mock
  private OBCriteria<Package> mockPackageCriteria;
  @Mock
  private OBCriteria<Module> mockModuleCriteria;
  @Mock
  private Dependency mockDependency;
  @Mock
  private Package mockPackage;
  @Mock
  private Module mockModule;
  @Mock
  private org.codehaus.jettison.json.JSONObject mockParameters;
  @Mock
  private MutableBoolean mockIsStopped;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<PackageUtil> mockedPackageUtil;

  private ChangeFormat changeFormatAction;

  private static final String TEST_GROUP = "com.etendo";
  private static final String TEST_ARTIFACT = "test-module";
  private static final String TEST_DEPENDENCY_NAME = TEST_GROUP + "." + TEST_ARTIFACT;
  private static final String NEW_FORMAT_SOURCE = DependencyUtil.FORMAT_SOURCE;
  private static final String NEW_FORMAT_JAR = "jar";
  private static final String TEST_VERSION = "1.0.0";

  /**
   * Initializes mocks and the action before each test.
   */
  @BeforeEach
  void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedPackageUtil = mockStatic(PackageUtil.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    changeFormatAction = new ChangeFormat();
  }

  /**
   * Releases mock resources and cleans up after each test.
   */
  @AfterEach
  void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mockedPackageUtil != null) {
      mockedPackageUtil.close();
    }

    ReflectionTestHelper.cleanup();
  }

  /**
   * Tests for successful format change scenarios.
   */
  @Nested
  @DisplayName("Successful Format Change Tests")
  class SuccessfulFormatChangeTests {


    /**
     * Verifies that the format is successfully changed from LOCAL to SOURCE.
     *
     * @throws Exception
     *     if an unexpected error occurs during the test.
     */
    @Test
    @DisplayName("Should successfully change format from LOCAL to SOURCE")
    void shouldSuccessfullyChangeFormatFromLocalToSource() throws Exception {
      when(mockOBDal.createCriteria(Package.class)).thenReturn(mockPackageCriteria);

      when(mockPackageCriteria.add(any(org.hibernate.criterion.Criterion.class))).thenReturn(mockPackageCriteria);
      when(mockPackageCriteria.setMaxResults(1)).thenReturn(mockPackageCriteria);

      when(mockDependency.getGroup()).thenReturn(TEST_GROUP);
      when(mockDependency.getArtifact()).thenReturn(TEST_ARTIFACT);

      when(mockParameters.getString(ChangeFormatUtil.NEW_FORMAT_PARAM))
          .thenReturn(NEW_FORMAT_SOURCE);
      setupSuccessfulScenario();
      when(mockOBDal.createCriteria(Module.class)).thenReturn(mockModuleCriteria);
      when(mockModuleCriteria.add(any(org.hibernate.criterion.Criterion.class))).thenReturn(mockModuleCriteria);
      when(mockModuleCriteria.setMaxResults(1)).thenReturn(mockModuleCriteria);
      when(mockDependency.getFormat()).thenReturn(DependencyUtil.FORMAT_LOCAL);
      when(mockParameters.getString(ChangeFormatUtil.NEW_FORMAT_PARAM))
          .thenReturn(NEW_FORMAT_SOURCE);
      mockedPackageUtil.when(() -> PackageUtil.getCoreCompatibleOrLatestVersion(mockPackage))
          .thenReturn(TEST_VERSION);
      mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("ETDEP_Format_Changed_Source"))
          .thenReturn("Format changed to source");

      ActionResult result = changeFormatAction.action(mockParameters, mockIsStopped);

      assertAll("Successful format change",
          () -> assertEquals(Result.Type.SUCCESS, result.getType()),
          () -> assertTrue(result.getMessage().contains("Format changed to source")),
          () -> assertTrue(result.getMessage().contains(TEST_DEPENDENCY_NAME))
      );

      verify(mockDependency).setVersion(TEST_VERSION);
      verify(mockDependency).setFormat(NEW_FORMAT_SOURCE);
      verify(mockDependency).setInstallationStatus(DependencyUtil.STATUS_PENDING);
      verify(mockOBDal).save(mockDependency);
      verify(mockOBDal, atLeastOnce()).flush();
    }

    /**
     * Verifies that the format is successfully changed to JAR.
     *
     * @throws Exception
     *     if an unexpected error occurs during the test.
     */
    @Test
    @DisplayName("Should successfully change format to JAR")
    void shouldSuccessfullyChangeFormatToJar() throws Exception {
      setupSuccessfulScenario();
      when(mockOBDal.createCriteria(Package.class)).thenReturn(mockPackageCriteria);

      when(mockPackageCriteria.add(any(org.hibernate.criterion.Criterion.class))).thenReturn(mockPackageCriteria);
      when(mockPackageCriteria.setMaxResults(1)).thenReturn(mockPackageCriteria);

      when(mockDependency.getGroup()).thenReturn(TEST_GROUP);
      when(mockDependency.getArtifact()).thenReturn(TEST_ARTIFACT);

      when(mockParameters.getString(ChangeFormatUtil.NEW_FORMAT_PARAM))
          .thenReturn(NEW_FORMAT_SOURCE);
      when(mockOBDal.createCriteria(Module.class)).thenReturn(mockModuleCriteria);
      when(mockModuleCriteria.add(any(org.hibernate.criterion.Criterion.class))).thenReturn(mockModuleCriteria);
      when(mockModuleCriteria.setMaxResults(1)).thenReturn(mockModuleCriteria);
      when(mockDependency.getFormat()).thenReturn(DependencyUtil.FORMAT_SOURCE);
      when(mockParameters.getString(ChangeFormatUtil.NEW_FORMAT_PARAM))
          .thenReturn(NEW_FORMAT_JAR);
      mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("ETDEP_Format_Changed_Jar"))
          .thenReturn("Format changed to jar");

      ActionResult result = changeFormatAction.action(mockParameters, mockIsStopped);

      assertAll("Successful JAR format change",
          () -> assertEquals(Result.Type.SUCCESS, result.getType()),
          () -> assertTrue(result.getMessage().contains("Format changed to jar")),
          () -> assertTrue(result.getMessage().contains(TEST_DEPENDENCY_NAME))
      );

      verify(mockDependency, never()).setVersion(anyString());
      verify(mockDependency).setFormat(NEW_FORMAT_JAR);
    }

    /**
     * Sets up a successful scenario for testing by configuring mock dependencies
     * and reflection test helpers.
     */
    private void setupSuccessfulScenario() {
      List<Dependency> dependencies = Collections.singletonList(mockDependency);
      ReflectionTestHelper.setInputContents(changeFormatAction, Dependency.class, dependencies);

      when(mockPackageCriteria.uniqueResult()).thenReturn(mockPackage);
      when(mockModuleCriteria.uniqueResult()).thenReturn(null);
    }
  }

  /**
   * Nested class for error-handling-related tests.
   */
  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {
    /**
     * Tests the handling of a scenario where a dependency package is not found.
     * Ensures the appropriate error message is displayed in the action result.
     *
     * @throws Exception
     *     if any unexpected exception occurs during the test.
     */
    @Test
    @DisplayName("Should handle dependency package not found")
    void shouldHandleDependencyPackageNotFound() throws Exception {
      when(mockOBDal.createCriteria(Package.class)).thenReturn(mockPackageCriteria);

      when(mockPackageCriteria.add(any(org.hibernate.criterion.Criterion.class))).thenReturn(mockPackageCriteria);
      when(mockPackageCriteria.setMaxResults(1)).thenReturn(mockPackageCriteria);

      when(mockDependency.getGroup()).thenReturn(TEST_GROUP);
      when(mockDependency.getArtifact()).thenReturn(TEST_ARTIFACT);

      when(mockParameters.getString(ChangeFormatUtil.NEW_FORMAT_PARAM))
          .thenReturn(NEW_FORMAT_SOURCE);
      setupSingleDependency();
      when(mockPackageCriteria.uniqueResult()).thenReturn(null);
      mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("ETDEP_No_Dependency_Package"))
          .thenReturn("No dependency package found for %s");

      ActionResult result = changeFormatAction.action(mockParameters, mockIsStopped);

      assertAll("Package not found error",
          () -> assertEquals(Result.Type.ERROR, result.getType()),
          () -> assertTrue(result.getMessage().contains("No dependency package found"))
      );
    }

    /**
     * Tests the utility methods of the reflection test helper, including setting
     * and retrieving input contents.
     */
    @Test
    @DisplayName("Should test reflection helper methods")
    void shouldTestReflectionHelperMethods() {
      List<Dependency> dependencies = Collections.singletonList(mockDependency);

      ReflectionTestHelper.setInputContents(changeFormatAction, Dependency.class, dependencies);

      List<Dependency> retrievedDependencies = ReflectionTestHelper.getInputContents(changeFormatAction,
          Dependency.class);
      List<BaseOBObject> allContents = ReflectionTestHelper.getInputContents(changeFormatAction);

      assertAll("Reflection helper verification",
          () -> assertNotNull(retrievedDependencies, "Retrieved dependencies should not be null"),
          () -> assertEquals(1, retrievedDependencies.size(), "Should have exactly 1 dependency"),
          () -> assertEquals(mockDependency, retrievedDependencies.get(0), "Should return the same mock dependency"),
          () -> assertNotNull(allContents, "All contents should not be null")
      );
    }

    /**
     * Sets up a single dependency for testing purposes by configuring mock data
     * with the reflection test helper.
     */
    private void setupSingleDependency() {
      List<Dependency> dependencies = Collections.singletonList(mockDependency);
      ReflectionTestHelper.setInputContents(changeFormatAction, Dependency.class, dependencies);
    }
  }

  /**
   * Nested class for input/output-related tests.
   */
  @Nested
  @DisplayName("Input/Output Tests")
  class InputOutputTests {
    /**
     * Tests that the input class returned by the action is correct.
     */
    @Test
    @DisplayName("Should return correct input class")
    void shouldReturnCorrectInputClass() {
      Class<?> inputClass = changeFormatAction.getInputClass();

      assertEquals(Dependency.class, inputClass);
    }

    /**
     * Tests that a successful result is built correctly, ensuring the correct
     * message and result type are returned.
     *
     * @throws Exception
     *     if any unexpected exception occurs during the test.
     */
    @Test
    @DisplayName("Should build success result correctly")
    void shouldBuildSuccessResultCorrectly() throws Exception {
      when(mockOBDal.createCriteria(Package.class)).thenReturn(mockPackageCriteria);

      when(mockPackageCriteria.add(any(org.hibernate.criterion.Criterion.class))).thenReturn(mockPackageCriteria);
      when(mockPackageCriteria.setMaxResults(1)).thenReturn(mockPackageCriteria);

      when(mockDependency.getGroup()).thenReturn(TEST_GROUP);
      when(mockDependency.getArtifact()).thenReturn(TEST_ARTIFACT);

      when(mockParameters.getString(ChangeFormatUtil.NEW_FORMAT_PARAM))
          .thenReturn(NEW_FORMAT_SOURCE);
      setupSingleDependency();
      when(mockOBDal.createCriteria(Module.class)).thenReturn(mockModuleCriteria);
      when(mockModuleCriteria.add(any(org.hibernate.criterion.Criterion.class))).thenReturn(mockModuleCriteria);
      when(mockModuleCriteria.setMaxResults(1)).thenReturn(mockModuleCriteria);
      when(mockPackageCriteria.uniqueResult()).thenReturn(mockPackage);
      when(mockModuleCriteria.uniqueResult()).thenReturn(null);
      mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("ETDEP_Format_Changed_Source"))
          .thenReturn("Success message");

      ActionResult result = changeFormatAction.action(mockParameters, mockIsStopped);

      assertAll("Success result structure",
          () -> assertEquals(Result.Type.SUCCESS, result.getType()),
          () -> assertNotNull(result.getMessage()),
          () -> assertTrue(result.getMessage().contains("Success message"))
      );
    }

    /**
     * Sets up a single dependency for testing purposes by configuring mock data
     * with the reflection test helper.
     */
    private void setupSingleDependency() {
      List<Dependency> dependencies = Collections.singletonList(mockDependency);
      ReflectionTestHelper.setInputContents(changeFormatAction, Dependency.class, dependencies);
    }
  }
}
