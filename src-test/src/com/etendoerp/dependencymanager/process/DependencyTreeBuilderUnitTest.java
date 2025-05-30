package com.etendoerp.dependencymanager.process;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.etendoerp.dependencymanager.util.DependencyTreeBuilder;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.dal.service.OBDal;

import com.etendoerp.dependencymanager.data.PackageDependency;

/**
 * Unit tests for the `DependencyTreeBuilder` class.
 * <p>
 * This test suite validates the behavior of methods related to dependency tree building,
 * including sub-dependency search, parameter-based dependency addition, and parent-child
 * relationship mapping.
 * </p>
 * <ul>
 *   <li>Tests handling of external dependencies.</li>
 *   <li>Validates JSON-based dependency addition.</li>
 *   <li>Ensures proper mapping of parent-child relationships.</li>
 *   <li>Handles edge cases such as null or empty inputs.</li>
 * </ul>
 *
 * <p>Ensures the correctness and robustness of dependency tree operations.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DependencyTreeBuilder Test Suite")
class DependencyTreeBuilderUnitTest {

  private MockedStatic<OBDal> mockedOBDal;

  private OBDal mockOBDalInstance;
  private PackageDependency mockDependency;
  private PackageDependency mockSubDependency;

  private static final String TEST_DEPENDENCY_ID = "test-dep-id";
  private static final String TEST_SUB_DEPENDENCY_ID = "test-sub-dep-id";

  /**
   * Sets up the test environment, including mocked instances and default behaviors.
   */
  @BeforeEach
  void setUp() {
    mockedOBDal = mockStatic(OBDal.class);

    mockOBDalInstance = mock(OBDal.class);
    mockDependency = mock(PackageDependency.class);
    mockSubDependency = mock(PackageDependency.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDalInstance);

  }

  /**
   * Cleans up the mocked static instances after each test.
   */
  @AfterEach
  void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
  }

  /**
   * Tests that `searchSubDependency` returns an empty list for external dependencies.
   */
  @Test
  @DisplayName("searchSubDependency - Should return empty list for external dependency")
  void testSearchSubDependencyExternalDependency() {
    when(mockDependency.isExternalDependency()).thenReturn(true);
    Map<String, String> parentMap = new HashMap<>();

    List<PackageDependency> result = DependencyTreeBuilder.searchSubDependency(mockDependency, parentMap);

    assertAll(
        () -> assertNotNull(result, "Result should not be null"),
        () -> assertTrue(result.isEmpty(), "Result should be empty for external dependency"),
        () -> assertTrue(parentMap.isEmpty(), "Parent map should remain empty")
    );
  }

  /**
   * Tests that `addDependenciesFromParams` processes a valid JSON array and returns dependencies.
   */
  @Test
  @DisplayName("addDependenciesFromParams - Should process JSON array and return dependencies")
  void testAddDependenciesFromParamsValidInput() throws JSONException {
    JSONArray jsonArray = new JSONArray();
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("id", TEST_DEPENDENCY_ID);
    jsonArray.put(jsonObject);

    when(mockOBDalInstance.get(PackageDependency.class, TEST_DEPENDENCY_ID)).thenReturn(mockDependency);


    try (MockedStatic<DependencyTreeBuilder> mockedBuilder = mockStatic(DependencyTreeBuilder.class)) {
      mockedBuilder.when(() -> DependencyTreeBuilder.addDependenciesFromParams(any(JSONArray.class)))
          .thenCallRealMethod();
      mockedBuilder.when(() -> DependencyTreeBuilder.addDependency(any(Map.class), any(PackageDependency.class)))
          .thenCallRealMethod();
      mockedBuilder.when(() -> DependencyTreeBuilder.searchDependency(any(PackageDependency.class), any(Map.class)))
          .thenReturn(new ArrayList<>());
      mockedBuilder.when(() -> DependencyTreeBuilder.isBundle(any(PackageDependency.class)))
          .thenReturn(false);

      List<PackageDependency> result = DependencyTreeBuilder.addDependenciesFromParams(jsonArray);

      assertAll(
          () -> assertNotNull(result, "Result should not be null"),
          () -> assertFalse(result.isEmpty(), "Result should not be empty"),
          () -> assertTrue(result.contains(mockDependency), "Result should contain the dependency")
      );
    }
  }

  /**
   * Tests that `addDependenciesFromParams` handles invalid JSON input gracefully.
   */
  @Test
  @DisplayName("addDependenciesFromParams - Should handle JSON exception")
  void testAddDependenciesFromParamsInvalidJSON() throws JSONException {
    JSONArray jsonArray = mock(JSONArray.class);
    when(jsonArray.length()).thenReturn(1);
    when(jsonArray.getJSONObject(0)).thenThrow(new JSONException("Invalid JSON"));

    assertThrows(JSONException.class, () -> {
      DependencyTreeBuilder.addDependenciesFromParams(jsonArray);
    }, "Should throw JSONException for invalid JSON");
  }

  /**
   * Tests that `addDependenciesWithParents` processes dependencies and updates maps correctly.
   */
  @Test
  @DisplayName("addDependenciesWithParents - Should process dependencies and update maps")
  void testAddDependenciesWithParentsValidInput() {
    List<PackageDependency> dependenciesList = new ArrayList<>();
    dependenciesList.add(mockDependency);

    Map<String, PackageDependency> dependencyMap = new HashMap<>();
    Map<String, String> parentMap = new HashMap<>();

    List<PackageDependency> subDependencies = new ArrayList<>();
    subDependencies.add(mockSubDependency);

    try (MockedStatic<DependencyTreeBuilder> mockedBuilder = mockStatic(DependencyTreeBuilder.class)) {
      mockedBuilder.when(() -> DependencyTreeBuilder.addDependenciesWithParents(
          any(List.class), any(Map.class), any(Map.class))).thenCallRealMethod();
      mockedBuilder.when(() -> DependencyTreeBuilder.searchSubDependency(
          eq(mockDependency), any(Map.class))).thenAnswer(invocation -> {
        Map<String, String> parentMapArg = invocation.getArgument(1);
        parentMapArg.put(TEST_SUB_DEPENDENCY_ID, TEST_DEPENDENCY_ID);
        return subDependencies;
      });
      mockedBuilder.when(() -> DependencyTreeBuilder.addDependency(
          any(Map.class), eq(mockSubDependency))).thenCallRealMethod();

      DependencyTreeBuilder.addDependenciesWithParents(dependenciesList, dependencyMap, parentMap);

      assertAll(
          () -> assertEquals(1, dependencyMap.size(), "Dependency map should contain one entry"),
          () -> assertTrue(dependencyMap.containsValue(mockSubDependency),
              "Dependency map should contain the sub-dependency"),
          () -> assertEquals(2, parentMap.size(), "Parent map should contain two entries"),
          () -> assertEquals(TEST_DEPENDENCY_ID, parentMap.get(TEST_SUB_DEPENDENCY_ID),
              "Parent map should correctly map sub-dependency to parent")
      );
    }
  }

  /**
   * Tests that `addDependenciesWithParents` handles an empty dependencies list.
   */
  @Test
  @DisplayName("addDependenciesWithParents - Should handle empty dependencies list")
  void testAddDependenciesWithParentsEmptyList() {
    List<PackageDependency> emptyList = new ArrayList<>();
    Map<String, PackageDependency> dependencyMap = new HashMap<>();
    Map<String, String> parentMap = new HashMap<>();

    DependencyTreeBuilder.addDependenciesWithParents(emptyList, dependencyMap, parentMap);

    assertAll(
        () -> assertTrue(dependencyMap.isEmpty(), "Dependency map should remain empty"),
        () -> assertTrue(parentMap.isEmpty(), "Parent map should remain empty")
    );
  }

  /**
   * Tests that `addDependenciesWithParents` handles null input gracefully.
   */
  @Test
  @DisplayName("addDependenciesWithParents - Should handle null input gracefully")
  void testAddDependenciesWithParentsNullInput() {
    Map<String, PackageDependency> dependencyMap = new HashMap<>();
    Map<String, String> parentMap = new HashMap<>();

    assertThrows(NullPointerException.class,
        () -> DependencyTreeBuilder.addDependenciesWithParents(null, dependencyMap, parentMap),
        "Should throw NullPointerException for null dependencies list");
  }

  /**
   * Tests that `searchSubDependency` handles a null dependency version gracefully.
   */
  @Test
  @DisplayName("searchSubDependency - Should handle null getDependencyVersion gracefully")
  void testSearchSubDependency_NullDependencyVersion() {
    when(mockDependency.isExternalDependency()).thenReturn(false);
    when(mockDependency.getDependencyVersion()).thenReturn(null);
    Map<String, String> parentMap = new HashMap<>();

    assertThrows(NullPointerException.class, () -> DependencyTreeBuilder.searchSubDependency(mockDependency, parentMap),
        "Should throw NullPointerException when getDependencyVersion returns null");
  }
}
