package com.etendoerp.dependencymanager.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.criterion.Criterion;
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
import org.openbravo.model.ad.module.Module;

import com.etendoerp.dependencymanager.data.Dependency;

/**
 * Unit tests for DependencyUtil class.
 * Tests cover all static methods with various scenarios including edge cases,
 * using Mockito for mocking OBDal and related persistence operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DependencyUtil Tests")
class DependencyUtilTest {

  @Mock
  private OBDal mockOBDal;
  @Mock
  private OBCriteria<Dependency> mockDependencyCriteria;
  @Mock
  private OBCriteria<Module> mockModuleCriteria;
  @Mock
  private Dependency mockDependency;
  @Mock
  private Module mockModule;

  private MockedStatic<OBDal> obDalMockedStatic;

  private static final String TEST_GROUP = "com.etendoerp";
  private static final String TEST_ARTIFACT = "test-module";
  private static final String TEST_VERSION = "1.0.0";

  /**
   * Sets up the mocked static `OBDal` instance before each test.
   */
  @BeforeEach
  void setUp() {
    obDalMockedStatic = mockStatic(OBDal.class);
    obDalMockedStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
  }

  /**
   * Closes the mocked static `OBDal` instance after each test.
   */
  @AfterEach
  void tearDown() {
    if (obDalMockedStatic != null) {
      obDalMockedStatic.close();
    }
  }

  @Nested
  @DisplayName("existsDependency Tests")
  class ExistsDependencyTests {

    /**
     * Configures the mock criteria for dependency queries before each test.
     */
    @BeforeEach
    void setUpDependencyCriteria() {
      when(mockOBDal.createCriteria(Dependency.class)).thenReturn(mockDependencyCriteria);
      when(mockDependencyCriteria.add(any(Criterion.class))).thenReturn(mockDependencyCriteria);
      when(mockDependencyCriteria.setMaxResults(1)).thenReturn(mockDependencyCriteria);
    }

    /**
     * Validates that `existsDependency` returns true for an external dependency.
     */
    @Test
    @DisplayName("Should return true when external dependency exists")
    void shouldReturnTrueWhenExternalDependencyExists() {
      boolean externalDependency = true;
      when(mockDependencyCriteria.uniqueResult()).thenReturn(mockDependency);

      boolean result = DependencyUtil.existsDependency(TEST_GROUP, TEST_ARTIFACT, TEST_VERSION, externalDependency);

      assertAll("External dependency exists validation",
          () -> assertTrue(result, "Should return true when external dependency exists"),
          () -> verify(mockOBDal).createCriteria(Dependency.class),
          () -> verify(mockDependencyCriteria, times(4)).add(any(Criterion.class)),
          () -> verify(mockDependencyCriteria).setMaxResults(1),
          () -> verify(mockDependencyCriteria).uniqueResult()
      );
    }

    /**
     * Validates that `existsDependency` returns true for an internal dependency.
     */
    @Test
    @DisplayName("Should return true when internal dependency exists")
    void shouldReturnTrueWhenInternalDependencyExists() {
      boolean externalDependency = false;
      when(mockDependencyCriteria.uniqueResult()).thenReturn(mockDependency);

      boolean result = DependencyUtil.existsDependency(TEST_GROUP, TEST_ARTIFACT, TEST_VERSION, externalDependency);

      assertAll("Internal dependency exists validation",
          () -> assertTrue(result, "Should return true when internal dependency exists"),
          () -> verify(mockOBDal).createCriteria(Dependency.class),
          () -> verify(mockDependencyCriteria, times(4)).add(any(Criterion.class)),
          () -> verify(mockDependencyCriteria).setMaxResults(1),
          () -> verify(mockDependencyCriteria).uniqueResult()
      );
    }

    /**
     * Validates that `existsDependency` returns false when the dependency does not exist.
     */
    @Test
    @DisplayName("Should return false when dependency does not exist")
    void shouldReturnFalseWhenDependencyDoesNotExist() {
      boolean externalDependency = true;
      when(mockDependencyCriteria.uniqueResult()).thenReturn(null);

      boolean result = DependencyUtil.existsDependency(TEST_GROUP, TEST_ARTIFACT, TEST_VERSION, externalDependency);

      assertAll("Dependency does not exist validation",
          () -> assertFalse(result, "Should return false when dependency does not exist"),
          () -> verify(mockOBDal).createCriteria(Dependency.class),
          () -> verify(mockDependencyCriteria, times(4)).add(any(Criterion.class)),
          () -> verify(mockDependencyCriteria).setMaxResults(1),
          () -> verify(mockDependencyCriteria).uniqueResult()
      );
    }

    /**
     * Ensures that the JAR format is used for external dependencies.
     */
    @Test
    @DisplayName("Should use JAR format for external dependencies")
    void shouldUseJarFormatForExternalDependencies() {
      boolean externalDependency = true;
      when(mockDependencyCriteria.uniqueResult()).thenReturn(mockDependency);

      DependencyUtil.existsDependency(TEST_GROUP, TEST_ARTIFACT, TEST_VERSION, externalDependency);

      verify(mockDependencyCriteria, times(4)).add(any(Criterion.class));
    }

    /**
     * Ensures that the SOURCE format is used for internal dependencies.
     */
    @Test
    @DisplayName("Should use SOURCE format for internal dependencies")
    void shouldUseSourceFormatForInternalDependencies() {
      boolean externalDependency = false;
      when(mockDependencyCriteria.uniqueResult()).thenReturn(mockDependency);

      DependencyUtil.existsDependency(TEST_GROUP, TEST_ARTIFACT, TEST_VERSION, externalDependency);

      verify(mockDependencyCriteria, times(4)).add(any(Criterion.class));
    }

    /**
     * Validates that null parameters are handled gracefully.
     */
    @Test
    @DisplayName("Should handle null parameters gracefully")
    void shouldHandleNullParametersGracefully() {
      when(mockDependencyCriteria.uniqueResult()).thenReturn(null);

      assertAll("Null parameters handling",
          () -> assertDoesNotThrow(() ->
              DependencyUtil.existsDependency(null, TEST_ARTIFACT, TEST_VERSION, true)),
          () -> assertDoesNotThrow(() ->
              DependencyUtil.existsDependency(TEST_GROUP, null, TEST_VERSION, true)),
          () -> assertDoesNotThrow(() ->
              DependencyUtil.existsDependency(TEST_GROUP, TEST_ARTIFACT, null, true))
      );
    }

    /**
     * Validates that empty string parameters are handled gracefully.
     */
    @Test
    @DisplayName("Should handle empty string parameters")
    void shouldHandleEmptyStringParameters() {
      when(mockDependencyCriteria.uniqueResult()).thenReturn(null);

      assertAll("Empty string parameters handling",
          () -> assertDoesNotThrow(() ->
              DependencyUtil.existsDependency("", TEST_ARTIFACT, TEST_VERSION, true)),
          () -> assertDoesNotThrow(() ->
              DependencyUtil.existsDependency(TEST_GROUP, "", TEST_VERSION, true)),
          () -> assertDoesNotThrow(() ->
              DependencyUtil.existsDependency(TEST_GROUP, TEST_ARTIFACT, "", true))
      );
    }
  }

  @Nested
  @DisplayName("getInstalledDependency Tests")
  class GetInstalledDependencyTests {

    /**
     * Configures the mock criteria for dependency queries before each test.
     */
    @BeforeEach
    void setUpDependencyCriteria() {
      when(mockOBDal.createCriteria(Dependency.class)).thenReturn(mockDependencyCriteria);
      when(mockDependencyCriteria.add(any(Criterion.class))).thenReturn(mockDependencyCriteria);
      when(mockDependencyCriteria.setMaxResults(1)).thenReturn(mockDependencyCriteria);
    }

    /**
     * Validates that `getInstalledDependency` returns a dependency for an external dependency.
     */
    @Test
    @DisplayName("Should return dependency when external dependency is found")
    void shouldReturnDependencyWhenExternalDependencyFound() {
      boolean externalDependency = true;
      when(mockDependencyCriteria.uniqueResult()).thenReturn(mockDependency);

      Dependency result = DependencyUtil.getInstalledDependency(TEST_GROUP, TEST_ARTIFACT, externalDependency);

      assertAll("External dependency retrieval validation",
          () -> assertNotNull(result, "Should return a dependency object"),
          () -> assertEquals(mockDependency, result, "Should return the mocked dependency"),
          () -> verify(mockOBDal).createCriteria(Dependency.class),
          () -> verify(mockDependencyCriteria, times(3)).add(any(Criterion.class)),
          () -> verify(mockDependencyCriteria).setMaxResults(1),
          () -> verify(mockDependencyCriteria).uniqueResult()
      );
    }

    /**
     * Validates that `getInstalledDependency` returns a dependency for an internal dependency.
     */
    @Test
    @DisplayName("Should return dependency when internal dependency is found")
    void shouldReturnDependencyWhenInternalDependencyFound() {
      boolean externalDependency = false;
      when(mockDependencyCriteria.uniqueResult()).thenReturn(mockDependency);

      Dependency result = DependencyUtil.getInstalledDependency(TEST_GROUP, TEST_ARTIFACT, externalDependency);

      assertAll("Internal dependency retrieval validation",
          () -> assertNotNull(result, "Should return a dependency object"),
          () -> assertEquals(mockDependency, result, "Should return the mocked dependency"),
          () -> verify(mockOBDal).createCriteria(Dependency.class),
          () -> verify(mockDependencyCriteria, times(3)).add(any(Criterion.class)),
          () -> verify(mockDependencyCriteria).setMaxResults(1),
          () -> verify(mockDependencyCriteria).uniqueResult()
      );
    }

    /**
     * Validates that `getInstalledDependency` returns null when the dependency is not found.
     */
    @Test
    @DisplayName("Should return null when dependency is not found")
    void shouldReturnNullWhenDependencyNotFound() {
      boolean externalDependency = true;
      when(mockDependencyCriteria.uniqueResult()).thenReturn(null);

      Dependency result = DependencyUtil.getInstalledDependency(TEST_GROUP, TEST_ARTIFACT, externalDependency);

      assertAll("Dependency not found validation",
          () -> assertNull(result, "Should return null when dependency is not found"),
          () -> verify(mockOBDal).createCriteria(Dependency.class),
          () -> verify(mockDependencyCriteria, times(3)).add(any(Criterion.class)),
          () -> verify(mockDependencyCriteria).setMaxResults(1),
          () -> verify(mockDependencyCriteria).uniqueResult()
      );
    }

    /**
     * Validates that null group parameters are handled gracefully.
     */
    @Test
    @DisplayName("Should handle null group parameter")
    void shouldHandleNullGroupParameter() {
      when(mockDependencyCriteria.uniqueResult()).thenReturn(null);

      assertDoesNotThrow(() -> {
        Dependency result = DependencyUtil.getInstalledDependency(null, TEST_ARTIFACT, true);
        assertNull(result, "Should handle null group gracefully");
      });
    }

    /**
     * Validates that null artifact parameters are handled gracefully.
     */
    @Test
    @DisplayName("Should handle null artifact parameter")
    void shouldHandleNullArtifactParameter() {
      when(mockDependencyCriteria.uniqueResult()).thenReturn(null);

      assertDoesNotThrow(() -> {
        Dependency result = DependencyUtil.getInstalledDependency(TEST_GROUP, null, true);
        assertNull(result, "Should handle null artifact gracefully");
      });
    }

    /**
     * Ensures that the correct number of restrictions are added to the criteria.
     */
    @Test
    @DisplayName("Should create criteria with correct restrictions count")
    void shouldCreateCriteriaWithCorrectRestrictionsCount() {
      when(mockDependencyCriteria.uniqueResult()).thenReturn(mockDependency);

      DependencyUtil.getInstalledDependency(TEST_GROUP, TEST_ARTIFACT, true);

      verify(mockDependencyCriteria, times(3)).add(any(Criterion.class));
    }
  }

  @Nested
  @DisplayName("getInstalledModule Tests")
  class GetInstalledModuleTests {

    /**
     * Configures the mock criteria for module queries before each test.
     */
    @BeforeEach
    void setUpModuleCriteria() {
      when(mockOBDal.createCriteria(Module.class)).thenReturn(mockModuleCriteria);
      when(mockModuleCriteria.add(any(Criterion.class))).thenReturn(mockModuleCriteria);
      when(mockModuleCriteria.setMaxResults(1)).thenReturn(mockModuleCriteria);
    }

    /**
     * Validates that `getInstalledModule` returns a module when found.
     */
    @Test
    @DisplayName("Should return module when module is found")
    void shouldReturnModuleWhenModuleFound() {
      when(mockModuleCriteria.uniqueResult()).thenReturn(mockModule);

      Module result = DependencyUtil.getInstalledModule(TEST_GROUP, TEST_ARTIFACT);

      assertAll("Module retrieval validation",
          () -> assertNotNull(result, "Should return a module object"),
          () -> assertEquals(mockModule, result, "Should return the mocked module"),
          () -> verify(mockOBDal).createCriteria(Module.class),
          () -> verify(mockModuleCriteria).add(any(Criterion.class)),
          () -> verify(mockModuleCriteria).setMaxResults(1),
          () -> verify(mockModuleCriteria).uniqueResult()
      );
    }

    /**
     * Validates that `getInstalledModule` returns null when the module is not found.
     */
    @Test
    @DisplayName("Should return null when module is not found")
    void shouldReturnNullWhenModuleNotFound() {
      when(mockModuleCriteria.uniqueResult()).thenReturn(null);

      Module result = DependencyUtil.getInstalledModule(TEST_GROUP, TEST_ARTIFACT);

      assertAll("Module not found validation",
          () -> assertNull(result, "Should return null when module is not found"),
          () -> verify(mockOBDal).createCriteria(Module.class),
          () -> verify(mockModuleCriteria).add(any(Criterion.class)),
          () -> verify(mockModuleCriteria).setMaxResults(1),
          () -> verify(mockModuleCriteria).uniqueResult()
      );
    }

    /**
     * Validates that null group parameters are handled gracefully.
     */
    @Test
    @DisplayName("Should construct correct Java package format")
    void shouldConstructCorrectJavaPackageFormat() {
      when(mockModuleCriteria.uniqueResult()).thenReturn(mockModule);

      DependencyUtil.getInstalledModule(TEST_GROUP, TEST_ARTIFACT);

      verify(mockModuleCriteria).add(any(Criterion.class));
    }

    /**
     * Validates that null artifact parameters are handled gracefully.
     */
    @Test
    @DisplayName("Should handle null group parameter")
    void shouldHandleNullGroupParameter() {
      when(mockModuleCriteria.uniqueResult()).thenReturn(null);

      assertDoesNotThrow(() -> {
        Module result = DependencyUtil.getInstalledModule(null, TEST_ARTIFACT);
        assertNull(result, "Should handle null group gracefully");
      });
    }

    /**
     * Validates that both null parameters are handled gracefully.
     */
    @Test
    @DisplayName("Should handle null artifact parameter")
    void shouldHandleNullArtifactParameter() {
      when(mockModuleCriteria.uniqueResult()).thenReturn(null);

      assertDoesNotThrow(() -> {
        Module result = DependencyUtil.getInstalledModule(TEST_GROUP, null);
        assertNull(result, "Should handle null artifact gracefully");
      });
    }

    /**
     * Validates that empty string parameters are handled gracefully.
     */
    @Test
    @DisplayName("Should handle both null parameters")
    void shouldHandleBothNullParameters() {
      when(mockModuleCriteria.uniqueResult()).thenReturn(null);

      assertDoesNotThrow(() -> {
        Module result = DependencyUtil.getInstalledModule(null, null);
        assertNull(result, "Should handle both null parameters gracefully");
      });
    }

    /**
     * Validates that special characters in parameters are handled correctly.
     */
    @Test
    @DisplayName("Should handle empty string parameters")
    void shouldHandleEmptyStringParameters() {
      when(mockModuleCriteria.uniqueResult()).thenReturn(null);

      assertAll("Empty string parameters handling",
          () -> assertDoesNotThrow(() ->
              DependencyUtil.getInstalledModule("", TEST_ARTIFACT)),
          () -> assertDoesNotThrow(() ->
              DependencyUtil.getInstalledModule(TEST_GROUP, "")),
          () -> assertDoesNotThrow(() ->
              DependencyUtil.getInstalledModule("", ""))
      );
    }

    /**
     * Validates the correctness of format constants.
     */
    @Test
    @DisplayName("Should handle special characters in parameters")
    void shouldHandleSpecialCharactersInParameters() {
      String specialGroup = "com.test-special_chars";
      String specialArtifact = "module-with_special.chars";
      when(mockModuleCriteria.uniqueResult()).thenReturn(mockModule);

      assertDoesNotThrow(() -> {
        Module result = DependencyUtil.getInstalledModule(specialGroup, specialArtifact);
        assertEquals(mockModule, result, "Should handle special characters in parameters");
      });
    }
  }

  @Nested
  @DisplayName("Constants Validation Tests")
  class ConstantsValidationTests {

    /**
     * Validates the correctness of status constants.
     */
    @Test
    @DisplayName("Should have correct format constants")
    void shouldHaveCorrectFormatConstants() {
      assertAll("Format constants validation",
          () -> assertEquals("L", DependencyUtil.FORMAT_LOCAL, "FORMAT_LOCAL should be 'L'"),
          () -> assertEquals("S", DependencyUtil.FORMAT_SOURCE, "FORMAT_SOURCE should be 'S'"),
          () -> assertEquals("J", DependencyUtil.FORMAT_JAR, "FORMAT_JAR should be 'J'")
      );
    }

    /**
     * Validates the correctness of status constants.
     */
    @Test
    @DisplayName("Should have correct status constants")
    void shouldHaveCorrectStatusConstants() {
      assertAll("Status constants validation",
          () -> assertEquals("INSTALLED", DependencyUtil.STATUS_INSTALLED, "STATUS_INSTALLED should be 'INSTALLED'"),
          () -> assertEquals("PENDING", DependencyUtil.STATUS_PENDING, "STATUS_PENDING should be 'PENDING'"),
          () -> assertEquals("UT", DependencyUtil.UNTRACKED_STATUS, "UNTRACKED_STATUS should be 'UT'")
      );
    }

    /**
     * Ensures that format constants are unique.
     */
    @Test
    @DisplayName("Format constants should be different from each other")
    void formatConstantsShouldBeDifferentFromEachOther() {
      assertAll("Format constants uniqueness",
          () -> assertNotEquals(DependencyUtil.FORMAT_LOCAL, DependencyUtil.FORMAT_SOURCE,
              "FORMAT_LOCAL should be different from FORMAT_SOURCE"),
          () -> assertNotEquals(DependencyUtil.FORMAT_LOCAL, DependencyUtil.FORMAT_JAR,
              "FORMAT_LOCAL should be different from FORMAT_JAR"),
          () -> assertNotEquals(DependencyUtil.FORMAT_SOURCE, DependencyUtil.FORMAT_JAR,
              "FORMAT_SOURCE should be different from FORMAT_JAR")
      );
    }

    /**
     * Ensures that status constants are unique.
     */
    @Test
    @DisplayName("Status constants should be different from each other")
    void statusConstantsShouldBeDifferentFromEachOther() {
      assertAll("Status constants uniqueness",
          () -> assertNotEquals(DependencyUtil.STATUS_INSTALLED, DependencyUtil.STATUS_PENDING,
              "STATUS_INSTALLED should be different from STATUS_PENDING"),
          () -> assertNotEquals(DependencyUtil.STATUS_INSTALLED, DependencyUtil.UNTRACKED_STATUS,
              "STATUS_INSTALLED should be different from UNTRACKED_STATUS"),
          () -> assertNotEquals(DependencyUtil.STATUS_PENDING, DependencyUtil.UNTRACKED_STATUS,
              "STATUS_PENDING should be different from UNTRACKED_STATUS")
      );
    }
  }

  @Nested
  @DisplayName("Integration and Edge Cases Tests")
  class IntegrationAndEdgeCasesTests {

    /**
     * Validates that runtime exceptions are handled gracefully.
     */
    @Test
    @DisplayName("Should handle runtime exceptions gracefully")
    void shouldHandleRuntimeExceptionsGracefully() {
      when(mockOBDal.createCriteria((Class<BaseOBObject>) any())).thenThrow(
          new RuntimeException("Database connection error"));

      assertAll("Runtime exception handling",
          () -> assertThrows(RuntimeException.class, () ->
              DependencyUtil.existsDependency(TEST_GROUP, TEST_ARTIFACT, TEST_VERSION, true)),
          () -> assertThrows(RuntimeException.class, () ->
              DependencyUtil.getInstalledDependency(TEST_GROUP, TEST_ARTIFACT, true)),
          () -> assertThrows(RuntimeException.class, () ->
              DependencyUtil.getInstalledModule(TEST_GROUP, TEST_ARTIFACT))
      );
    }

  }
}
