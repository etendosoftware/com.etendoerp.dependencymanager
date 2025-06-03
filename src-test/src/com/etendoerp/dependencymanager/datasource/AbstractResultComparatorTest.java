package com.etendoerp.dependencymanager.datasource;

import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.NEW_VERSION;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.PACKAGE_NAME;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.UNKNOWN_FIELD;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.VERSION;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.etendoerp.dependencymanager.util.DependencyManagerConstants;
import com.etendoerp.dependencymanager.util.PackageUtil;

@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractResultComparator Tests")
class AbstractResultComparatorTest {

  private MockedStatic<PackageUtil> mockedPackageUtil;

  /**
   * Concrete implementation of {@link AbstractResultComparator} for testing purposes.
   * Allows access to protected fields for assertions.
   */
  private static class ConcreteResultComparator extends AbstractResultComparator {
    public ConcreteResultComparator(String sortByField) {
      super(sortByField);
    }

    public String getSortByField() {
      return sortByField;
    }

    public int getAscending() {
      return ascending;
    }

    public java.util.List<String> getStringFieldList() {
      return stringFieldList;
    }
  }

  /**
   * Sets up the static mock for {@link PackageUtil} before each test.
   * Ensures a clean mock environment for every test case.
   */
  @BeforeEach
  void setUp() {
    mockedPackageUtil = mockStatic(PackageUtil.class);
  }

  /**
   * Closes the static mock for {@link PackageUtil} after each test.
   * Prevents static mock state from leaking between tests.
   */
  @AfterEach
  void tearDown() {
    if (mockedPackageUtil != null) {
      mockedPackageUtil.close();
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    /**
     * Tests that the constructor initializes the comparator with ascending order
     * when a normal field name is provided.
     */
    @Test
    @DisplayName("Should initialize with ascending order for normal field")
    void shouldInitializeWithAscendingOrder() {
      String fieldName = "group";

      ConcreteResultComparator comparator = new ConcreteResultComparator(fieldName);

      assertAll("Constructor initialization",
          () -> assertEquals(fieldName, comparator.getSortByField(), "Sort field should match"),
          () -> assertEquals(1, comparator.getAscending(), "Should be ascending order"),
          () -> assertTrue(comparator.getStringFieldList().containsAll(AbstractResultComparator.BASE_STRING_FIELD_LIST),
              "Should contain base string fields")
      );
    }

    /**
     * Tests that the constructor initializes the comparator with descending order
     * when the field name starts with a dash.
     */
    @Test
    @DisplayName("Should initialize with descending order for field starting with '-'")
    void shouldInitializeWithDescendingOrder() {
      String fieldName = "-version";
      String expectedField = "version";

      ConcreteResultComparator comparator = new ConcreteResultComparator(fieldName);

      assertAll("Constructor with descending order",
          () -> assertEquals(expectedField, comparator.getSortByField(), "Should remove '-' prefix"),
          () -> assertEquals(-1, comparator.getAscending(), "Should be descending order")
      );
    }

    /**
     * Tests that the constructor handles an empty field name correctly.
     */
    @Test
    @DisplayName("Should handle empty field name")
    void shouldHandleEmptyFieldName() {
      String fieldName = "";

      ConcreteResultComparator comparator = new ConcreteResultComparator(fieldName);

      assertAll("Empty field name",
          () -> assertEquals("", comparator.getSortByField()),
          () -> assertEquals(1, comparator.getAscending())
      );
    }

    /**
     * Tests that the constructor handles a field name with only a dash.
     */
    @Test
    @DisplayName("Should handle field name with only '-'")
    void shouldHandleOnlyDashFieldName() {
      String fieldName = "-";

      ConcreteResultComparator comparator = new ConcreteResultComparator(fieldName);

      assertAll("Only dash field name",
          () -> assertEquals("", comparator.getSortByField()),
          () -> assertEquals(-1, comparator.getAscending())
      );
    }
  }

  @Nested
  @DisplayName("Version Comparison Tests")
  class VersionComparisonTests {

    /**
     * Tests that the comparator uses version comparison for the version field.
     */
    @Test
    @DisplayName("Should use version comparison for version field")
    void shouldUseVersionComparisonForVersionField() {
      ConcreteResultComparator comparator = new ConcreteResultComparator(DependencyManagerConstants.VERSION);
      Map<String, Object> map1 = createMapWithValue(DependencyManagerConstants.VERSION, VERSION);
      Map<String, Object> map2 = createMapWithValue(DependencyManagerConstants.VERSION, NEW_VERSION);

      mockedPackageUtil.when(() -> PackageUtil.compareVersions(VERSION, NEW_VERSION))
          .thenReturn(-1);

      int result = comparator.compare(map1, map2);

      assertAll("Version comparison",
          () -> assertEquals(-1, result, "Should return negative for older version"),
          () -> mockedPackageUtil.verify(() -> PackageUtil.compareVersions(VERSION, NEW_VERSION), times(1))
      );
    }

    /**
     * Tests that the comparator handles null values in version comparison.
     */
    @Test
    @DisplayName("Should handle null values in version comparison")
    void shouldHandleNullValuesInVersionComparison() {
      ConcreteResultComparator comparator = new ConcreteResultComparator(DependencyManagerConstants.VERSION);
      Map<String, Object> map1 = createMapWithValue(DependencyManagerConstants.VERSION, null);
      Map<String, Object> map2 = createMapWithValue(DependencyManagerConstants.VERSION, VERSION);

      mockedPackageUtil.when(() -> PackageUtil.compareVersions("", VERSION))
          .thenReturn(-1);

      int result = comparator.compare(map1, map2);

      assertAll("Null version handling",
          () -> assertEquals(-1, result),
          () -> mockedPackageUtil.verify(() -> PackageUtil.compareVersions("", VERSION), times(1))
      );
    }

    /**
     * Tests that the comparator applies descending order to version comparison.
     */
    @Test
    @DisplayName("Should apply descending order to version comparison")
    void shouldApplyDescendingOrderToVersionComparison() {
      ConcreteResultComparator comparator = new ConcreteResultComparator("-" + DependencyManagerConstants.VERSION);
      Map<String, Object> map1 = createMapWithValue(DependencyManagerConstants.VERSION, VERSION);
      Map<String, Object> map2 = createMapWithValue(DependencyManagerConstants.VERSION, NEW_VERSION);

      mockedPackageUtil.when(() -> PackageUtil.compareVersions(VERSION, NEW_VERSION))
          .thenReturn(-1);

      int result = comparator.compare(map1, map2);

      assertEquals(1, result, "Should invert result for descending order");
    }
  }

  @Nested
  @DisplayName("String Comparison Tests")
  class StringComparisonTests {

    /**
     * Tests that the comparator uses string comparison for the group field.
     */
    @Test
    @DisplayName("Should use string comparison for group field")
    void shouldUseStringComparisonForGroupField() {
      ConcreteResultComparator comparator = new ConcreteResultComparator(DependencyManagerConstants.GROUP);
      Map<String, Object> map1 = createMapWithValue(DependencyManagerConstants.GROUP, "com.example.a");
      Map<String, Object> map2 = createMapWithValue(DependencyManagerConstants.GROUP, "com.example.b");

      int result = comparator.compare(map1, map2);

      assertTrue(result < 0, "Should return negative for lexicographically smaller string");
    }

    /**
     * Tests that the comparator uses string comparison for the artifact field.
     */
    @Test
    @DisplayName("Should use string comparison for artifact field")
    void shouldUseStringComparisonForArtifactField() {
      ConcreteResultComparator comparator = new ConcreteResultComparator(DependencyManagerConstants.ARTIFACT);
      Map<String, Object> map1 = createMapWithValue(DependencyManagerConstants.ARTIFACT, "artifact-a");
      Map<String, Object> map2 = createMapWithValue(DependencyManagerConstants.ARTIFACT, "artifact-b");

      int result = comparator.compare(map1, map2);

      assertTrue(result < 0, "Should return negative for lexicographically smaller string");
    }

    /**
     * Tests that the comparator handles null values in string comparison.
     */
    @Test
    @DisplayName("Should handle null values in string comparison")
    void shouldHandleNullValuesInStringComparison() {
      ConcreteResultComparator comparator = new ConcreteResultComparator(DependencyManagerConstants.GROUP);
      Map<String, Object> map1 = createMapWithValue(DependencyManagerConstants.GROUP, null);
      Map<String, Object> map2 = createMapWithValue(DependencyManagerConstants.GROUP, PACKAGE_NAME);

      int result = comparator.compare(map1, map2);

      assertTrue(result < 0, "Empty string should be less than non-empty string");
    }

    /**
     * Tests that the comparator handles equal string values correctly.
     */
    @Test
    @DisplayName("Should handle equal string values")
    void shouldHandleEqualStringValues() {
      ConcreteResultComparator comparator = new ConcreteResultComparator(DependencyManagerConstants.GROUP);
      Map<String, Object> map1 = createMapWithValue(DependencyManagerConstants.GROUP, PACKAGE_NAME);
      Map<String, Object> map2 = createMapWithValue(DependencyManagerConstants.GROUP, PACKAGE_NAME);

      int result = comparator.compare(map1, map2);

      assertEquals(0, result, "Should return 0 for equal strings");
    }

    /**
     * Tests that the comparator applies descending order to string comparison.
     */
    @Test
    @DisplayName("Should apply descending order to string comparison")
    void shouldApplyDescendingOrderToStringComparison() {
      ConcreteResultComparator comparator = new ConcreteResultComparator("-" + DependencyManagerConstants.GROUP);
      Map<String, Object> map1 = createMapWithValue(DependencyManagerConstants.GROUP, "com.example.a");
      Map<String, Object> map2 = createMapWithValue(DependencyManagerConstants.GROUP, "com.example.b");

      int result = comparator.compare(map1, map2);

      assertTrue(result > 0, "Should return positive for descending order");
    }
  }

  @Nested
  @DisplayName("General Comparison Tests")
  class GeneralComparisonTests {

    /**
     * Tests that the comparator uses general comparison for unknown fields.
     */
    @Test
    @DisplayName("Should use general comparison for unknown fields")
    void shouldUseGeneralComparisonForUnknownFields() {
      String unknownField = UNKNOWN_FIELD;
      ConcreteResultComparator comparator = new ConcreteResultComparator(unknownField);
      Map<String, Object> map1 = createMapWithValue(unknownField, "valueA");
      Map<String, Object> map2 = createMapWithValue(unknownField, "valueB");

      int result = comparator.compare(map1, map2);

      assertTrue(result < 0, "Should use string comparison for unknown fields");
    }

    /**
     * Tests that the comparator handles different object types in general comparison.
     */
    @Test
    @DisplayName("Should handle different object types in general comparison")
    void shouldHandleDifferentObjectTypesInGeneralComparison() {
      String unknownField = "numericField";
      ConcreteResultComparator comparator = new ConcreteResultComparator(unknownField);
      Map<String, Object> map1 = createMapWithValue(unknownField, 100);
      Map<String, Object> map2 = createMapWithValue(unknownField, 200);

      int result = comparator.compare(map1, map2);

      assertTrue(result < 0, "Should convert to string and compare");
    }

    /**
     * Tests that the comparator handles different object types in general comparison.
     */
    @Test
    @DisplayName("Should handle null values in general comparison")
    void shouldHandleNullValuesInGeneralComparison() {
      // Given
      String unknownField = UNKNOWN_FIELD;
      ConcreteResultComparator comparator = new ConcreteResultComparator(unknownField);
      Map<String, Object> map1 = createMapWithValue(unknownField, null);
      Map<String, Object> map2 = createMapWithValue(unknownField, "value");

      int result = comparator.compare(map1, map2);

      assertTrue(result < 0, "Null should be converted to empty string and compared");
    }

    /**
     * Tests that the comparator handles null values in general comparison.
     */
    @Test
    @DisplayName("Should handle both null values in general comparison")
    void shouldHandleBothNullValuesInGeneralComparison() {
      // Given
      String unknownField = UNKNOWN_FIELD;
      ConcreteResultComparator comparator = new ConcreteResultComparator(unknownField);
      Map<String, Object> map1 = createMapWithValue(unknownField, null);
      Map<String, Object> map2 = createMapWithValue(unknownField, null);

      int result = comparator.compare(map1, map2);

      assertEquals(0, result, "Both nulls should be equal");
    }
  }

  @Nested
  @DisplayName("Edge Cases and Integration Tests")
  class EdgeCasesAndIntegrationTests {

    /**
     * Tests that the comparator handles empty maps correctly.
     */
    @Test
    @DisplayName("Should handle empty maps")
    void shouldHandleEmptyMaps() {
      // Given
      ConcreteResultComparator comparator = new ConcreteResultComparator("anyField");
      Map<String, Object> map1 = new HashMap<>();
      Map<String, Object> map2 = new HashMap<>();

      int result = comparator.compare(map1, map2);

      assertEquals(0, result, "Empty maps should be equal");
    }

    /**
     * Tests that the comparator handles maps with missing fields.
     */
    @Test
    @DisplayName("Should handle maps with missing fields")
    void shouldHandleMapsWithMissingFields() {
      ConcreteResultComparator comparator = new ConcreteResultComparator("missingField");
      Map<String, Object> map1 = createMapWithValue("otherField", "value1");
      Map<String, Object> map2 = createMapWithValue("otherField", "value2");

      int result = comparator.compare(map1, map2);

      assertEquals(0, result, "Maps without the field should be equal");
    }

    /**
     * Tests that the comparator is consistent with the equals contract.
     */
    @Test
    @DisplayName("Should be consistent with equals contract")
    void shouldBeConsistentWithEqualsContract() {
      ConcreteResultComparator comparator = new ConcreteResultComparator(DependencyManagerConstants.GROUP);
      Map<String, Object> map1 = createMapWithValue(DependencyManagerConstants.GROUP, "same");
      Map<String, Object> map2 = createMapWithValue(DependencyManagerConstants.GROUP, "same");
      Map<String, Object> map3 = createMapWithValue(DependencyManagerConstants.GROUP, "same");

      assertAll("Equals contract consistency",
          () -> assertEquals(0, comparator.compare(map1, map2), "Should be equal"),
          () -> assertEquals(0, comparator.compare(map2, map1), "Should be symmetric"),
          () -> assertEquals(0, comparator.compare(map1, map3), "Should be transitive"),
          () -> assertEquals(0, comparator.compare(map2, map3), "Should be transitive")
      );
    }

    /**
     * Tests that the comparator maintains the transitivity property.
     */
    @Test
    @DisplayName("Should maintain transitivity property")
    void shouldMaintainTransitivityProperty() {
      ConcreteResultComparator comparator = new ConcreteResultComparator(DependencyManagerConstants.GROUP);
      Map<String, Object> map1 = createMapWithValue(DependencyManagerConstants.GROUP, "a");
      Map<String, Object> map2 = createMapWithValue(DependencyManagerConstants.GROUP, "b");
      Map<String, Object> map3 = createMapWithValue(DependencyManagerConstants.GROUP, "c");

      int result1vs2 = comparator.compare(map1, map2);
      int result2vs3 = comparator.compare(map2, map3);
      int result1vs3 = comparator.compare(map1, map3);

      assertAll("Transitivity property",
          () -> assertTrue(result1vs2 < 0, "map1 < map2"),
          () -> assertTrue(result2vs3 < 0, "map2 < map3"),
          () -> assertTrue(result1vs3 < 0, "map1 < map3 (transitivity)")
      );
    }
  }

  /**
   * Helper method to create a map with a single key-value pair for testing.
   *
   * @param key
   *     the key to insert
   * @param value
   *     the value to associate with the key
   * @return a map containing the provided key-value pair
   */
  private Map<String, Object> createMapWithValue(String key, Object value) {
    Map<String, Object> map = new HashMap<>();
    map.put(key, value);
    return map;
  }
}
