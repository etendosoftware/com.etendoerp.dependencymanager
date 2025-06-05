package com.etendoerp.dependencymanager.datasource;

import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.PARENT1;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.PARENT2;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.RESULT_NOT_NULL_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.etendoerp.dependencymanager.data.PackageDependency;
import com.etendoerp.dependencymanager.data.PackageVersion;
import com.etendoerp.dependencymanager.util.DependencyManagerConstants;
import com.etendoerp.dependencymanager.util.DependencyTreeBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for AddSubDependencyDS class.
 * Tests the functionality of sub-dependency retrieval, filtering, and sorting.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddSubDependencyDS Tests")
class AddSubDependencyDSTest {

  @InjectMocks
  private AddSubDependencyDS addSubDependencyDS;

  @Mock
  private PackageVersion mockPackageVersion;

  @Mock
  private PackageDependency mockDependency1;

  @Mock
  private PackageDependency mockDependency2;

  @Mock
  private PackageDependency mockDependency3;

  private Map<String, String> parameters;

  /**
   * Sets up the test environment before each test.
   * Initializes parameters, dependencies, and their mock behavior.
   */
  @BeforeEach
  void setUp() {
    parameters = new HashMap<>();
    List<PackageDependency> dependenciesList = new ArrayList<>();

    dependenciesList.addAll(Arrays.asList(mockDependency1, mockDependency2, mockDependency3));

  }


  @Nested
  @DisplayName("getGridData Tests")
  class GetGridDataTests {

    /**
     * Tests that an empty list is returned when no dependencies exist.
     *
     * @throws JSONException
     *     if JSON processing fails.
     */
    @Test
    @DisplayName("Should return empty list when no dependencies exist")
    void shouldReturnEmptyListWhenNoDependencies() throws JSONException {
      when(mockPackageVersion.getETDEPPackageDependencyList()).thenReturn(new ArrayList<>());

      try (MockedStatic<DependencyTreeBuilder> mockedTreeBuilder = mockStatic(DependencyTreeBuilder.class)) {
        mockedTreeBuilder.when(() -> DependencyTreeBuilder.removeDependecyCore(any()))
            .thenAnswer(invocation -> null);
        mockedTreeBuilder.when(() -> DependencyTreeBuilder.addDependenciesWithParents(
            any(), any(), any())).thenAnswer(invocation -> null);

        List<Map<String, Object>> result = addSubDependencyDS.getGridData(parameters, mockPackageVersion);

        assertNotNull(result, RESULT_NOT_NULL_MESSAGE);
        assertTrue(result.isEmpty(), "Result should be empty when no dependencies exist");
      }
    }

    /**
     * Tests handling of dependencies without a parent.
     *
     * @throws JSONException
     *     if JSON processing fails.
     */
    @Test
    @DisplayName("Should handle dependencies without parent")
    void shouldHandleDependenciesWithoutParent() throws JSONException {
      Map<String, PackageDependency> mockDependencyMap = new HashMap<>();
      mockDependencyMap.put("dep1", mockDependency1);

      Map<String, String> mockParentMap = new HashMap<>();

      try (MockedStatic<DependencyTreeBuilder> mockedTreeBuilder = mockStatic(DependencyTreeBuilder.class)) {
        mockedTreeBuilder.when(() -> DependencyTreeBuilder.removeDependecyCore(any()))
            .thenAnswer(invocation -> null);
        mockedTreeBuilder.when(() -> DependencyTreeBuilder.addDependenciesWithParents(
            any(), any(), any())).thenAnswer(invocation -> {
          Map<String, PackageDependency> dependencyMap = invocation.getArgument(1);
          Map<String, String> parentMap = invocation.getArgument(2);
          dependencyMap.putAll(mockDependencyMap);
          parentMap.putAll(mockParentMap);
          return null;
        });

        List<Map<String, Object>> result = addSubDependencyDS.getGridData(parameters, mockPackageVersion);

        assertAll(
            () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
            () -> assertEquals(1, result.size(), "Should return 1 dependency"),
            () -> assertFalse(result.get(0).containsKey(DependencyManagerConstants.PARENT),
                "Should not contain parent key when no parent exists")
        );
      }
    }
  }

  @Nested
  @DisplayName("applyFilterAndSort Tests")
  class ApplyFilterAndSortTests {

    /**
     * Tests handling of dependencies without a parent.
     *
     * @throws JSONException
     *     if JSON processing fails.
     */
    @Test
    @DisplayName("Should filter by parent when parent filter is applied")
    void shouldFilterByParentWhenParentFilterIsApplied() throws JSONException {
      List<Map<String, Object>> inputResult = createTestDependencyMaps();

      AddSubDependencyDS.SubDependencySelectedFilters filters =
          (AddSubDependencyDS.SubDependencySelectedFilters) addSubDependencyDS.createSelectedFilters();
      filters.addParent(PARENT1);

      List<Map<String, Object>> result = addSubDependencyDS.applyFilterAndSort(
          parameters, inputResult, filters);

      assertAll(
          () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
          () -> assertEquals(1, result.size(), "Should filter to 1 dependency with parent1"),
          () -> assertEquals(PARENT1, result.get(0).get(DependencyManagerConstants.PARENT))
      );
    }

    /**
     * Tests that all items are returned when no parent filter is applied.
     *
     * @throws JSONException
     *     if JSON processing fails.
     */
    @Test
    @DisplayName("Should return all items when no parent filter is applied")
    void shouldReturnAllItemsWhenNoParentFilterIsApplied() throws JSONException {
      List<Map<String, Object>> inputResult = createTestDependencyMaps();

      AddSubDependencyDS.SubDependencySelectedFilters filters =
          (AddSubDependencyDS.SubDependencySelectedFilters) addSubDependencyDS.createSelectedFilters();

      List<Map<String, Object>> result = addSubDependencyDS.applyFilterAndSort(
          parameters, inputResult, filters);

      assertAll(
          () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
          () -> assertEquals(3, result.size(), "Should return all dependencies when no filter applied")
      );
    }

    /**
     * Tests filtering by multiple parents.
     *
     * @throws JSONException
     *     if JSON processing fails.
     */
    @Test
    @DisplayName("Should filter by multiple parents")
    void shouldFilterByMultipleParents() throws JSONException {
      List<Map<String, Object>> inputResult = createTestDependencyMaps();

      AddSubDependencyDS.SubDependencySelectedFilters filters =
          (AddSubDependencyDS.SubDependencySelectedFilters) addSubDependencyDS.createSelectedFilters();
      filters.addParent(PARENT1);
      filters.addParent(PARENT2);

      List<Map<String, Object>> result = addSubDependencyDS.applyFilterAndSort(
          parameters, inputResult, filters);

      assertAll(
          () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
          () -> assertEquals(2, result.size(), "Should filter to 2 dependencies with parent1 or parent2")
      );
    }

    private List<Map<String, Object>> createTestDependencyMaps() {
      List<Map<String, Object>> result = new ArrayList<>();

      Map<String, Object> dep1 = new HashMap<>();
      dep1.put(DependencyManagerConstants.PARENT, PARENT1);
      dep1.put(DependencyManagerConstants.ARTIFACT, "artifact1");

      Map<String, Object> dep2 = new HashMap<>();
      dep2.put(DependencyManagerConstants.PARENT, PARENT2);
      dep2.put(DependencyManagerConstants.ARTIFACT, "artifact2");

      Map<String, Object> dep3 = new HashMap<>();
      dep3.put(DependencyManagerConstants.PARENT, "parent3");
      dep3.put(DependencyManagerConstants.ARTIFACT, "artifact3");

      result.addAll(Arrays.asList(dep1, dep2, dep3));
      return result;
    }
  }

  @Nested
  @DisplayName("createResultComparator Tests")
  class CreateResultComparatorTests {

    /**
     * Tests the creation of a comparator for valid sort fields.
     *
     * @param sortField
     *     The sort field to test.
     */
    @ParameterizedTest
    @ValueSource(strings = { "group", "artifact", "version", "parent" })
    @DisplayName("Should create comparator for valid sort fields")
    void shouldCreateComparatorForValidSortFields(String sortField) {
      AbstractResultComparator comparator = addSubDependencyDS.createResultComparator(sortField);

      assertAll(
          () -> assertNotNull(comparator, "Comparator should not be null"),
          () -> assertInstanceOf(AddSubDependencyDS.SubDependencyResultComparator.class, comparator,
              "Should return SubDependencyResultComparator instance")
      );
    }
  }

  @Nested
  @DisplayName("createSelectedFilters Tests")
  class CreateSelectedFiltersTests {

    /**
     * Tests the creation of a `SubDependencySelectedFilters` instance.
     */
    @Test
    @DisplayName("Should create SubDependencySelectedFilters instance")
    void shouldCreateSubDependencySelectedFiltersInstance() {
      AbstractSelectedFilters filters = addSubDependencyDS.createSelectedFilters();

      assertAll(
          () -> assertNotNull(filters, "Filters should not be null"),
          () -> assertInstanceOf(AddSubDependencyDS.SubDependencySelectedFilters.class, filters,
              "Should return SubDependencySelectedFilters instance"),
          () -> {
            assert filters instanceof AddSubDependencyDS.SubDependencySelectedFilters;
            AddSubDependencyDS.SubDependencySelectedFilters subFilters =
                (AddSubDependencyDS.SubDependencySelectedFilters) filters;
            assertTrue(subFilters.getParent().isEmpty(),
                "Parent filter list should be empty initially");
          }
      );
    }
  }

  @Nested
  @DisplayName("addCriteria Tests")
  class AddCriteriaTests {

    /**
     * Tests adding parent criteria correctly.
     *
     * @throws JSONException
     *     if JSON processing fails.
     */
    @Test
    @DisplayName("Should add parent criteria correctly")
    void shouldAddParentCriteriaCorrectly() throws JSONException {
      AddSubDependencyDS.SubDependencySelectedFilters filters =
          (AddSubDependencyDS.SubDependencySelectedFilters) addSubDependencyDS.createSelectedFilters();

      JSONObject criteria = new JSONObject();
      criteria.put(DependencyManagerConstants.FIELD_NAME, DependencyManagerConstants.PARENT);
      criteria.put(DependencyManagerConstants.VALUE, "test-parent");

      addSubDependencyDS.addCriteria(filters, criteria);

      assertAll(
          () -> assertEquals(1, filters.getParent().size(), "Should have 1 parent filter"),
          () -> assertTrue(filters.getParent().contains("test-parent"),
              "Should contain the added parent value")
      );
    }

    /**
     * Tests handling of `JSONException` when processing invalid criteria.
     */
    @Test
    @DisplayName("Should handle JSONException when processing invalid criteria")
    void shouldHandleJSONExceptionWhenProcessingInvalidCriteria() {
      AddSubDependencyDS.SubDependencySelectedFilters filters =
          (AddSubDependencyDS.SubDependencySelectedFilters) addSubDependencyDS.createSelectedFilters();

      JSONObject invalidCriteria = new JSONObject();

      assertThrows(JSONException.class,
          () -> addSubDependencyDS.addCriteria(filters, invalidCriteria),
          "Should throw JSONException for invalid criteria");
    }

    /**
     * Tests that criteria for non-parent fields are not added.
     *
     * @throws JSONException
     *     if JSON processing fails.
     */
    @Test
    @DisplayName("Should not add criteria for non-parent fields")
    void shouldNotAddCriteriaForNonParentFields() throws JSONException {
      AddSubDependencyDS.SubDependencySelectedFilters filters =
          (AddSubDependencyDS.SubDependencySelectedFilters) addSubDependencyDS.createSelectedFilters();

      JSONObject criteria = new JSONObject();
      criteria.put(DependencyManagerConstants.FIELD_NAME, "someOtherField");
      criteria.put(DependencyManagerConstants.VALUE, "test-value");

      addSubDependencyDS.addCriteria(filters, criteria);

      assertTrue(filters.getParent().isEmpty(),
          "Should not add parent criteria for non-parent fields");
    }
  }

  @Nested
  @DisplayName("SubDependencySelectedFilters Tests")
  class SubDependencySelectedFiltersTests {

    /**
     * Tests that the parent list is initialized as empty.
     */
    @Test
    @DisplayName("Should initialize with empty parent list")
    void shouldInitializeWithEmptyParentList() {
      AddSubDependencyDS.SubDependencySelectedFilters filters =
          new AddSubDependencyDS.SubDependencySelectedFilters();

      assertAll(
          () -> assertNotNull(filters.getParent(), "Parent list should not be null"),
          () -> assertTrue(filters.getParent().isEmpty(), "Parent list should be empty initially")
      );
    }

    /**
     * Tests setting and getting the parent list.
     */
    @Test
    @DisplayName("Should allow setting and getting parent list")
    void shouldAllowSettingAndGettingParentList() {
      AddSubDependencyDS.SubDependencySelectedFilters filters =
          new AddSubDependencyDS.SubDependencySelectedFilters();
      List<String> parentList = Arrays.asList(PARENT1, PARENT2);

      filters.setParent(parentList);

      assertAll(
          () -> assertEquals(parentList, filters.getParent(), "Should return the set parent list"),
          () -> assertEquals(2, filters.getParent().size(), "Should have 2 parents")
      );
    }

    /**
     * Tests adding individual parents to the filter.
     */
    @Test
    @DisplayName("Should allow adding individual parents")
    void shouldAllowAddingIndividualParents() {
      AddSubDependencyDS.SubDependencySelectedFilters filters =
          new AddSubDependencyDS.SubDependencySelectedFilters();

      filters.addParent(PARENT1);
      filters.addParent(PARENT2);

      assertAll(
          () -> assertEquals(2, filters.getParent().size(), "Should have 2 parents"),
          () -> assertTrue(filters.getParent().contains(PARENT1), "Should contain parent1"),
          () -> assertTrue(filters.getParent().contains(PARENT2), "Should contain parent2")
      );
    }
  }

}
