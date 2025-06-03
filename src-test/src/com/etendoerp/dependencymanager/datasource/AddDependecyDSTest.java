package com.etendoerp.dependencymanager.datasource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.etendoerp.dependencymanager.data.Package;
import com.etendoerp.dependencymanager.data.PackageDependency;
import com.etendoerp.dependencymanager.data.PackageVersion;
import com.etendoerp.dependencymanager.util.DependencyManagerConstants;
import com.etendoerp.dependencymanager.util.DependencyTreeBuilder;

import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.COMPARATOR_NOT_NULL_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the `AddDependecyDS` class.
 * <p>
 * This test suite validates the behavior of the `AddDependecyDS` class, including:
 * <ul>
 *   <li>Handling of bundle and non-bundle packages.</li>
 *   <li>Dependency mapping and field validation.</li>
 *   <li>Factory methods for creating comparators and filters.</li>
 *   <li>Integration tests for exception handling.</li>
 *   <li>Validation of inner classes' behavior and inheritance.</li>
 * </ul>
 *
 * <p>Ensures the correctness and robustness of the dependency management logic.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddDependecyDS Tests")
class AddDependecyDSTest {

  private AddDependecyDS addDependecyDS;

  @Mock
  private PackageVersion mockPackageVersion;

  @Mock
  private Package mockPackage;

  @Mock
  private PackageDependency mockDependency1;

  private MockedStatic<DependencyTreeBuilder> mockedDependencyTreeBuilder;

  /**
   * Sets up the test environment before each test.
   * Initializes the `AddDependecyDS` instance and mocks the static `DependencyTreeBuilder`.
   */
  @BeforeEach
  void setUp() {
    addDependecyDS = new AddDependecyDS();
    mockedDependencyTreeBuilder = mockStatic(DependencyTreeBuilder.class);

  }

  /**
   * Cleans up the test environment after each test.
   * Closes the mocked static `DependencyTreeBuilder`.
   */
  @AfterEach
  void tearDown() {
    if (mockedDependencyTreeBuilder != null) {
      mockedDependencyTreeBuilder.close();
    }
  }

  @Nested
  @DisplayName("Bundle Package Tests")
  class BundlePackageTests {

    /**
     * Tests handling of an empty bundle dependencies list.
     *
     * @throws JSONException
     *     if JSON processing fails.
     */
    @Test
    @DisplayName("Should handle empty bundle dependencies list")
    void shouldHandleEmptyBundleDependenciesList() throws JSONException {
      when(mockPackageVersion.getPackage()).thenReturn(mockPackage);
      when(mockPackage.isBundle()).thenReturn(true);
      when(mockPackageVersion.getETDEPPackageDependencyList()).thenReturn(new ArrayList<>());

      Map<String, String> parameters = new HashMap<>();

      List<Map<String, Object>> result = addDependecyDS.getGridData(parameters, mockPackageVersion);

      assertAll("Empty bundle dependencies",
          () -> assertTrue(result.isEmpty(), "Should return empty list"),
          () -> verify(mockPackageVersion, times(1)).getETDEPPackageDependencyList(),
          () -> mockedDependencyTreeBuilder.verify(() ->
              DependencyTreeBuilder.removeDependecyCore(any()), times(1))
      );
    }

    /**
     * Tests handling of a null bundle dependencies list.
     */
    @Test
    @DisplayName("Should handle null bundle dependencies list")
    void shouldHandleNullBundleDependenciesList() {
      when(mockPackageVersion.getPackage()).thenReturn(mockPackage);
      when(mockPackage.isBundle()).thenReturn(true);
      when(mockPackageVersion.getETDEPPackageDependencyList()).thenReturn(null);

      Map<String, String> parameters = new HashMap<>();

      assertThrows(NullPointerException.class, () -> addDependecyDS.getGridData(parameters, mockPackageVersion),
          "Should throw NullPointerException for null dependency list");
    }
  }

  @Nested
  @DisplayName("Non-Bundle Package Tests")
  class NonBundlePackageTests {


    /**
     * Tests handling of an empty dependency tree.
     *
     * @throws JSONException
     *     if JSON processing fails.
     */
    @Test
    @DisplayName("Should handle empty dependency tree")
    void shouldHandleEmptyDependencyTree() throws JSONException {
      when(mockPackageVersion.getPackage()).thenReturn(mockPackage);
      when(mockPackage.isBundle()).thenReturn(false);
      mockedDependencyTreeBuilder.when(() ->
              DependencyTreeBuilder.createDependencyTree(mockPackageVersion))
          .thenReturn(new ArrayList<>());

      Map<String, String> parameters = new HashMap<>();

      List<Map<String, Object>> result = addDependecyDS.getGridData(parameters, mockPackageVersion);

      assertAll("Empty dependency tree",
          () -> assertTrue(result.isEmpty(), "Should return empty list"),
          () -> mockedDependencyTreeBuilder.verify(() ->
              DependencyTreeBuilder.createDependencyTree(mockPackageVersion), times(1))
      );
    }

    /**
     * Tests handling of a null dependency tree.
     */
    @Test
    @DisplayName("Should handle null dependency tree")
    void shouldHandleNullDependencyTree() {
      when(mockPackageVersion.getPackage()).thenReturn(mockPackage);
      when(mockPackage.isBundle()).thenReturn(false);
      mockedDependencyTreeBuilder.when(() ->
              DependencyTreeBuilder.createDependencyTree(mockPackageVersion))
          .thenReturn(null);

      Map<String, String> parameters = new HashMap<>();

      assertThrows(NullPointerException.class, () -> addDependecyDS.getGridData(parameters, mockPackageVersion),
          "Should throw NullPointerException for null dependency tree");
    }
  }

  @Nested
  @DisplayName("Dependency Mapping Tests")
  class DependencyMappingTests {

    /**
     * Tests correct mapping of all dependency fields.
     *
     * @throws JSONException
     *     if JSON processing fails.
     */
    @Test
    @DisplayName("Should correctly map all dependency fields")
    void shouldCorrectlyMapAllDependencyFields() throws JSONException {
      when(mockPackageVersion.getPackage()).thenReturn(mockPackage);

      when(mockPackage.isBundle()).thenReturn(false);
      List<PackageDependency> dependencies = Arrays.asList(mockDependency1);
      mockedDependencyTreeBuilder.when(() ->
              DependencyTreeBuilder.createDependencyTree(mockPackageVersion))
          .thenReturn(dependencies);

      Map<String, String> parameters = new HashMap<>();

      List<Map<String, Object>> result = addDependecyDS.getGridData(parameters, mockPackageVersion);

      assertEquals(1, result.size(), "Should have one mapped dependency");

      Map<String, Object> mappedDependency = result.get(0);
      assertAll("All dependency fields mapped correctly",
          () -> assertTrue(mappedDependency.containsKey(DependencyManagerConstants.GROUP),
              "Should contain GROUP key"),
          () -> assertTrue(mappedDependency.containsKey(DependencyManagerConstants.ARTIFACT),
              "Should contain ARTIFACT key"),
          () -> assertTrue(mappedDependency.containsKey(DependencyManagerConstants.VERSION),
              "Should contain VERSION key"),
          () -> assertTrue(mappedDependency.containsKey(DependencyManagerConstants.ID),
              "Should contain ID key"),
          () -> assertEquals(4, mappedDependency.size(), "Should have exactly 4 mapped fields")
      );
    }

    /**
     * Tests handling of dependencies with null values.
     *
     * @throws JSONException
     *     if JSON processing fails.
     */
    @Test
    @DisplayName("Should handle dependencies with null values")
    void shouldHandleDependenciesWithNullValues() throws JSONException {
      when(mockPackageVersion.getPackage()).thenReturn(mockPackage);

      when(mockPackageVersion.getPackage()).thenReturn(mockPackage);

      PackageDependency nullFieldDependency = mock(PackageDependency.class);
      when(nullFieldDependency.getGroup()).thenReturn(null);
      when(nullFieldDependency.getArtifact()).thenReturn("artifact");
      when(nullFieldDependency.getVersion()).thenReturn(null);
      when(nullFieldDependency.getId()).thenReturn("id");

      when(mockPackage.isBundle()).thenReturn(false);
      List<PackageDependency> dependencies = Arrays.asList(nullFieldDependency);
      mockedDependencyTreeBuilder.when(() ->
              DependencyTreeBuilder.createDependencyTree(mockPackageVersion))
          .thenReturn(dependencies);

      Map<String, String> parameters = new HashMap<>();

      List<Map<String, Object>> result = addDependecyDS.getGridData(parameters, mockPackageVersion);

      assertEquals(1, result.size(), "Should handle dependency with null fields");
      Map<String, Object> mappedDependency = result.get(0);

      assertAll("Null fields handling",
          () -> assertNull(mappedDependency.get(DependencyManagerConstants.GROUP),
              "Null group should remain null"),
          () -> assertEquals("artifact", mappedDependency.get(DependencyManagerConstants.ARTIFACT),
              "Non-null artifact should be preserved"),
          () -> assertNull(mappedDependency.get(DependencyManagerConstants.VERSION),
              "Null version should remain null"),
          () -> assertEquals("id", mappedDependency.get(DependencyManagerConstants.ID),
              "Non-null id should be preserved")
      );
    }
  }

  @Nested
  @DisplayName("Factory Methods Tests")
  class FactoryMethodsTests {

    /**
     * Tests creation of `DependencyResultComparator` with a correct sort field.
     */
    @Test
    @DisplayName("Should create DependencyResultComparator with correct sort field")
    void shouldCreateDependencyResultComparatorWithCorrectSortField() {
      String sortField = DependencyManagerConstants.GROUP;

      Object comparator = addDependecyDS.createResultComparator(sortField);

      assertAll("Comparator creation",
          () -> assertNotNull(comparator, COMPARATOR_NOT_NULL_MESSAGE),
          () -> assertInstanceOf(AddDependecyDS.DependencyResultComparator.class, comparator,
              "Should be instance of DependencyResultComparator")
      );
    }

    /**
     * Tests creation of `DependencyResultComparator` with a descending sort field.
     */
    @Test
    @DisplayName("Should create DependencyResultComparator with descending sort field")
    void shouldCreateDependencyResultComparatorWithDescendingSortField() {
      String sortField = "-" + DependencyManagerConstants.VERSION;

      Object comparator = addDependecyDS.createResultComparator(sortField);

      assertAll("Descending comparator creation",
          () -> assertNotNull(comparator, COMPARATOR_NOT_NULL_MESSAGE),
          () -> assertInstanceOf(AddDependecyDS.DependencyResultComparator.class, comparator,
              "Should be instance of DependencyResultComparator")
      );
    }

    /**
     * Tests creation of `DependencySelectedFilters`.
     */
    @Test
    @DisplayName("Should create DependencySelectedFilters")
    void shouldCreateDependencySelectedFilters() {
      Object filters = addDependecyDS.createSelectedFilters();

      assertAll("Filters creation",
          () -> assertNotNull(filters, "Filters should not be null"),
          () -> assertInstanceOf(AddDependecyDS.DependencySelectedFilters.class, filters,
              "Should be instance of DependencySelectedFilters")
      );
    }

    /**
     * Tests handling of a null sort field in comparator creation.
     */
    @Test
    @DisplayName("Should handle null sort field in comparator creation")
    void shouldHandleNullSortFieldInComparatorCreation() {
      Object comparator = addDependecyDS.createResultComparator(null);

      assertNotNull(comparator, "Should create comparator even with null sort field");
    }

    /**
     * Tests handling of an empty sort field in comparator creation.
     */
    @Test
    @DisplayName("Should handle empty sort field in comparator creation")
    void shouldHandleEmptySortFieldInComparatorCreation() {
      Object comparator = addDependecyDS.createResultComparator("");

      assertNotNull(comparator, "Should create comparator even with empty sort field");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    /**
     * Tests handling of exceptions during dependency tree creation.
     */
    @Test
    @DisplayName("Should handle exception during dependency tree creation")
    void shouldHandleExceptionDuringDependencyTreeCreation() {
      when(mockPackageVersion.getPackage()).thenReturn(mockPackage);

      when(mockPackage.isBundle()).thenReturn(false);
      mockedDependencyTreeBuilder.when(() ->
              DependencyTreeBuilder.createDependencyTree(mockPackageVersion))
          .thenThrow(new RuntimeException("Tree creation failed"));

      Map<String, String> parameters = new HashMap<>();

      assertThrows(RuntimeException.class, () -> addDependecyDS.getGridData(parameters, mockPackageVersion), "Should propagate exception from dependency tree creation");
    }

    /**
     * Tests handling of exceptions during core dependency removal.
     */
    @Test
    @DisplayName("Should handle exception during core dependency removal")
    void shouldHandleExceptionDuringCoreDependencyRemoval() {
      when(mockPackageVersion.getPackage()).thenReturn(mockPackage);

      when(mockPackage.isBundle()).thenReturn(true);
      List<PackageDependency> bundleDependencies = Arrays.asList(mockDependency1);
      when(mockPackageVersion.getETDEPPackageDependencyList()).thenReturn(bundleDependencies);

      mockedDependencyTreeBuilder.when(() ->
              DependencyTreeBuilder.removeDependecyCore(any()))
          .thenThrow(new RuntimeException("Core removal failed"));

      Map<String, String> parameters = new HashMap<>();

      assertThrows(RuntimeException.class, () -> addDependecyDS.getGridData(parameters, mockPackageVersion), "Should propagate exception from core dependency removal");
    }
  }

  @Nested
  @DisplayName("Inner Classes Tests")
  class InnerClassesTests {

    /**
     * Tests creation of `DependencyResultComparator` with proper inheritance.
     */
    @Test
    @DisplayName("Should create DependencyResultComparator with proper inheritance")
    void shouldCreateDependencyResultComparatorWithProperInheritance() {
      String sortField = DependencyManagerConstants.ARTIFACT;

      AddDependecyDS.DependencyResultComparator comparator =
          new AddDependecyDS.DependencyResultComparator(sortField);

      assertAll("DependencyResultComparator inheritance",
          () -> assertNotNull(comparator, COMPARATOR_NOT_NULL_MESSAGE)
      );
    }

    /**
     * Tests creation of `DependencySelectedFilters` with proper inheritance.
     */
    @Test
    @DisplayName("Should create DependencySelectedFilters with proper inheritance")
    void shouldCreateDependencySelectedFiltersWithProperInheritance() {
      AddDependecyDS.DependencySelectedFilters filters =
          new AddDependecyDS.DependencySelectedFilters();

      assertAll("DependencySelectedFilters inheritance",
          () -> assertNotNull(filters, "Filters should not be null")
      );
    }
  }
}
