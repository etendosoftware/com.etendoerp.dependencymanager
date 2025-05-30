package com.etendoerp.dependencymanager.datasource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import com.etendoerp.dependencymanager.util.PackageUtil;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.module.Module;

import com.etendoerp.dependencymanager.data.Dependency;
import com.etendoerp.dependencymanager.data.Package;
import com.etendoerp.dependencymanager.data.PackageDependency;
import com.etendoerp.dependencymanager.data.PackageVersion;

/**
 * Unit tests for the `PackageUtil` class.
 * <p>
 * This test suite validates the behavior of utility methods for managing packages,
 * dependencies, and version compatibility. It includes:
 * <ul>
 *   <li>Core compatibility checks.</li>
 *   <li>Version compatibility validation.</li>
 *   <li>Version format recognition.</li>
 *   <li>Package version retrieval.</li>
 *   <li>Dependency management operations.</li>
 *   <li>Version range processing.</li>
 *   <li>Core compatible version selection.</li>
 * </ul>
 *
 * <p>Ensures that the `PackageUtil` methods function correctly and handle edge cases.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PackageUtil Tests")
class PackageUtilTest {

  private OBDal mockOBDal;
  private OBCriteria<PackageVersion> mockPackageVersionCriteria;
  private OBCriteria<Dependency> mockDependencyCriteria;
  private OBQuery<PackageVersion> mockPackageVersionQuery;
  private OBQuery<Package> mockPackageQuery;
  private OBQuery<Dependency> mockDependencyQuery;
  private Package mockPackage;
  private PackageVersion mockPackageVersion;
  private PackageDependency mockCoreDependency;
  private Module mockCoreModule;
  private Dependency mockDependency;

  private MockedStatic<OBDal> mockedOBDal;

  private static final String TEST_PACKAGE_VERSION = "2.1.0";
  private static final String TEST_GROUP = "com.etendo";
  private static final String TEST_ARTIFACT = "test-package";

  /**
   * Sets up the mock objects and test environment before each test.
   */
  @BeforeEach
  void setUp() {
    mockOBDal = mock(OBDal.class);
    mockPackageVersionCriteria = mock(OBCriteria.class);
    mockDependencyCriteria = mock(OBCriteria.class);
    mockPackageVersionQuery = mock(OBQuery.class);
    mockPackageQuery = mock(OBQuery.class);
    mockDependencyQuery = mock(OBQuery.class);
    mockPackage = mock(Package.class);
    mockPackageVersion = mock(PackageVersion.class);
    mockCoreDependency = mock(PackageDependency.class);
    mockCoreModule = mock(Module.class);
    mockDependency = mock(Dependency.class);

    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);


  }

  /**
   * Cleans up the mock objects and test environment after each test.
   */
  @AfterEach
  void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
  }

  /**
   * Tests related to core compatibility checks.
   */
  @Nested
  @DisplayName("Core Compatibility Tests")
  class CoreCompatibilityTests {

    /**
     * Verifies that exceptions during core compatibility checks are handled gracefully.
     */
    @Test
    @DisplayName("Should handle exceptions gracefully")
    void testCheckCoreCompatibilityWithException() {
      when(mockOBDal.createCriteria(PackageVersion.class)).thenThrow(new RuntimeException("Database error"));

      JSONObject result = PackageUtil.checkCoreCompatibility(mockPackage, TEST_PACKAGE_VERSION);

      assertAll("Error handling",
          () -> assertFalse(result.getBoolean(PackageUtil.IS_COMPATIBLE)),
          () -> assertTrue(result.getString("error").contains("Database error"))
      );
    }

  }

  /**
   * Tests related to version compatibility validation.
   */
  @Nested
  @DisplayName("Version Compatibility Tests")
  class VersionCompatibilityTests {

    /**
     * Verifies that malformed version ranges are handled correctly.
     */
    @Test
    @DisplayName("Should handle malformed version ranges")
    void testIsCompatibleMalformedRange() {
      String malformedRange = "1.0.0-2.0.0";
      String version = "1.5.0";

      boolean result = PackageUtil.isCompatible(malformedRange, version);

      assertFalse(result, "Malformed version range should return false");
    }
  }

  /**
   * Tests related to version format recognition.
   */
  @Nested
  @DisplayName("Version Format Tests")
  class VersionFormatTests {

    /**
     * Verifies that valid semantic versions are recognized.
     */
    @ParameterizedTest
    @ValueSource(strings = { "1.0.0", "1.0", "1", "10.20.30", "0.0.1" })
    @DisplayName("Should recognize valid semantic versions")
    void testIsMajorMinorPatchVersionValidVersions(String version) {
      boolean result = PackageUtil.isMajorMinorPatchVersion(version);

      assertTrue(result, "Version " + version + " should be recognized as valid");
    }

    /**
     * Verifies that invalid semantic versions are rejected.
     */
    @ParameterizedTest
    @ValueSource(strings = { "1.0.0-SNAPSHOT", "1.0.0.1", "v1.0.0", "1.0.0-beta", "" })
    @DisplayName("Should reject invalid semantic versions")
    void testIsMajorMinorPatchVersionInvalidVersions(String version) {
      boolean result = PackageUtil.isMajorMinorPatchVersion(version);

      assertFalse(result, "Version " + version + " should be recognized as invalid");
    }
  }

  /**
   * Tests related to package version retrieval.
   */
  @Nested
  @DisplayName("Package Version Retrieval Tests")
  class PackageVersionRetrievalTests {

    /**
     * Verifies that null is returned when a package version is not found.
     */
    @Test
    @DisplayName("Should return null when package version not found")
    void testGetPackageVersionNotFound() {
      setupPackageVersionCriteriaMock();
      when(mockPackageVersionCriteria.uniqueResult()).thenReturn(null);

      PackageVersion result = PackageUtil.getPackageVersion(mockPackage, TEST_PACKAGE_VERSION);

      assertNull(result);
    }

    /**
     * Verifies that the latest package version is retrieved correctly.
     */
    @Test
    @DisplayName("Should retrieve latest package version")
    void testGetLastPackageVersion() {
      when(mockOBDal.createQuery(eq(PackageVersion.class), anyString())).thenReturn(mockPackageVersionQuery);
      when(mockPackageVersionQuery.setNamedParameter(anyString(), any())).thenReturn(mockPackageVersionQuery);
      when(mockPackageVersionQuery.setMaxResult(1)).thenReturn(mockPackageVersionQuery);
      when(mockPackageVersionQuery.uniqueResult()).thenReturn(mockPackageVersion);

      PackageVersion result = PackageUtil.getLastPackageVersion(mockPackage);

      assertNotNull(result);
      assertEquals(mockPackageVersion, result);
      verify(mockPackageVersionQuery).setNamedParameter("packageId", mockPackage.getId());
    }

    private void setupPackageVersionCriteriaMock() {
      when(mockOBDal.createCriteria(PackageVersion.class)).thenReturn(mockPackageVersionCriteria);
      when(mockPackageVersionCriteria.add(any())).thenReturn(mockPackageVersionCriteria);
      when(mockPackageVersionCriteria.setMaxResults(anyInt())).thenReturn(mockPackageVersionCriteria);
    }
  }

  /**
   * Tests related to dependency management operations.
   */
  @Nested
  @DisplayName("Dependency Management Tests")
  class DependencyManagementTests {

    /**
     * Verifies that an existing dependency is updated correctly.
     */
    @Test
    @DisplayName("Should update existing dependency")
    void testUpdateOrCreateDependencyExistingDependency() {
      setupDependencyMocks();
      when(mockDependencyQuery.uniqueResult()).thenReturn(mockDependency);

      PackageUtil.updateOrCreateDependency(TEST_GROUP, TEST_ARTIFACT, TEST_PACKAGE_VERSION);

      verify(mockDependency).setVersion(TEST_PACKAGE_VERSION);
      verify(mockOBDal).save(mockDependency);
    }

    /**
     * Verifies that a new dependency is created when it does not exist.
     */
    @Test
    @DisplayName("Should create new dependency when not exists")
    void testUpdateOrCreateDependencyNewDependency() {
      setupDependencyMocks();
      when(mockDependencyQuery.uniqueResult()).thenReturn(null);

      PackageUtil.updateOrCreateDependency(TEST_GROUP, TEST_ARTIFACT, TEST_PACKAGE_VERSION);

      verify(mockOBDal).save(any(Dependency.class));
    }

    /**
     * Configures the mock behavior for dependency-related queries.
     * <p>
     * This method sets up the mocked `OBDal` and `OBQuery` objects to simulate
     * the creation and configuration of queries for `Package` and `Dependency` entities.
     */
    private void setupDependencyMocks() {
      when(mockOBDal.createQuery(eq(Package.class), anyString())).thenReturn(mockPackageQuery);
      when(mockOBDal.createQuery(eq(Dependency.class), anyString())).thenReturn(mockDependencyQuery);
      when(mockPackageQuery.setNamedParameter(anyString(), any())).thenReturn(mockPackageQuery);
      when(mockPackageQuery.setMaxResult(1)).thenReturn(mockPackageQuery);
      when(mockPackageQuery.uniqueResult()).thenReturn(null);
      when(mockDependencyQuery.setNamedParameter(anyString(), any())).thenReturn(mockDependencyQuery);
      when(mockDependencyQuery.setMaxResult(1)).thenReturn(mockDependencyQuery);
    }
  }

  /**
   * Tests related to version range processing.
   */
  @Nested
  @DisplayName("Version Range Processing Tests")
  class VersionRangeProcessingTests {

    /**
     * Verifies that valid version ranges are split correctly.
     */
    @Test
    @DisplayName("Should split valid version range")
    void testSplitCoreVersionRangeValidRange() {
      String versionRange = "[1.0.0, 2.0.0)";

      String[] result = PackageUtil.splitCoreVersionRange(versionRange);

      assertAll("Version range split",
          () -> assertEquals(2, result.length),
          () -> assertEquals("1.0.0", result[0].trim()),
          () -> assertEquals("2.0.0", result[1].trim())
      );
    }

  }

  /**
   * Tests related to core compatible version selection.
   */
  @Nested
  @DisplayName("Core Compatible Version Tests")
  class CoreCompatibleVersionTests {

    /**
     * Verifies that a compatible version is returned when found.
     */
    @Test
    @DisplayName("Should return compatible version when found")
    void testGetCoreCompatibleOrLatestVersionCompatibleFound() throws JSONException {
      PackageVersion compatibleVersion = mock(PackageVersion.class);
      PackageVersion latestVersion = mock(PackageVersion.class);

      when(compatibleVersion.getVersion()).thenReturn("1.5.0");
      when(latestVersion.getVersion()).thenReturn("2.0.0");

      setupPackageVersionCriteriaMock();
      when(mockPackageVersionCriteria.list()).thenReturn(Arrays.asList(latestVersion, compatibleVersion));

      try (MockedStatic<PackageUtil> mockedPackageUtil = mockStatic(PackageUtil.class, CALLS_REAL_METHODS)) {
        JSONObject incompatibleResult = new JSONObject();
        incompatibleResult.put(PackageUtil.IS_COMPATIBLE, false);

        JSONObject compatibleResult = new JSONObject();
        compatibleResult.put(PackageUtil.IS_COMPATIBLE, true);

        mockedPackageUtil.when(() -> PackageUtil.checkCoreCompatibility(mockPackage, "2.0.0"))
            .thenReturn(incompatibleResult);
        mockedPackageUtil.when(() -> PackageUtil.checkCoreCompatibility(mockPackage, "1.5.0"))
            .thenReturn(compatibleResult);

        String result = PackageUtil.getCoreCompatibleOrLatestVersion(mockPackage);

        assertEquals("1.5.0", result);
      }
    }

    /**
     * Verifies that the latest version is returned when no compatible version is found.
     */
    @Test
    @DisplayName("Should return latest version when no compatible version found")
    void testGetCoreCompatibleOrLatestVersionNoCompatible() throws JSONException {
      PackageVersion latestVersion = mock(PackageVersion.class);
      when(latestVersion.getVersion()).thenReturn("2.0.0");

      setupPackageVersionCriteriaMock();
      when(mockPackageVersionCriteria.list()).thenReturn(Collections.singletonList(latestVersion));

      try (MockedStatic<PackageUtil> mockedPackageUtil = mockStatic(PackageUtil.class, CALLS_REAL_METHODS)) {
        JSONObject incompatibleResult = new JSONObject();
        incompatibleResult.put(PackageUtil.IS_COMPATIBLE, false);

        mockedPackageUtil.when(() -> PackageUtil.checkCoreCompatibility(mockPackage, "2.0.0"))
            .thenReturn(incompatibleResult);

        String result = PackageUtil.getCoreCompatibleOrLatestVersion(mockPackage);

        assertEquals("2.0.0", result);
      }
    }

    /**
     * Verifies that JSON exceptions are handled correctly.
     */
    @Test
    @DisplayName("Should handle JSON exception")
    void testGetCoreCompatibleOrLatestVersionJSONException() {
      PackageVersion latestVersion = mock(PackageVersion.class);
      when(latestVersion.getVersion()).thenReturn("2.0.0");

      setupPackageVersionCriteriaMock();
      when(mockPackageVersionCriteria.list()).thenReturn(Collections.singletonList(latestVersion));

      try (MockedStatic<PackageUtil> mockedPackageUtil = mockStatic(PackageUtil.class, CALLS_REAL_METHODS)) {
        JSONObject malformedResult = new JSONObject();

        mockedPackageUtil.when(() -> PackageUtil.checkCoreCompatibility(mockPackage, "2.0.0"))
            .thenReturn(malformedResult);

        assertThrows(OBException.class,
            () -> PackageUtil.getCoreCompatibleOrLatestVersion(mockPackage));
      }
    }

    /**
     * Configures the mock behavior for the `PackageVersion` criteria.
     * <p>
     * This method sets up the mocked `OBDal` and `OBCriteria` objects to simulate
     * the creation and configuration of a criteria query for `PackageVersion`.
     */
    private void setupPackageVersionCriteriaMock() {
      when(mockOBDal.createCriteria(PackageVersion.class)).thenReturn(mockPackageVersionCriteria);
      when(mockPackageVersionCriteria.add(any())).thenReturn(mockPackageVersionCriteria);
    }
  }
}
