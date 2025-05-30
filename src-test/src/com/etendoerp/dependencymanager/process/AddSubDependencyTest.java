package com.etendoerp.dependencymanager.process;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
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
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;

import com.etendoerp.dependencymanager.data.PackageDependency;
import com.etendoerp.dependencymanager.util.DependencyManagerConstants;

/**
 * Unit tests for AddSubDependency class using JUnit 5 and Mockito 5.0
 *
 * This test class demonstrates:
 * - Proper mocking of OBDal static methods
 * - Comprehensive test coverage for all scenarios
 * - Use of JUnit 5 features like @Nested and @DisplayName
 * - Proper resource cleanup to avoid test pollution
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddSubDependency Tests")
class AddSubDependencyTest {

  @Mock
  private PackageDependency mockDependency1;

  @Mock
  private PackageDependency mockDependency2;

  @Mock
  private PackageDependency mockDependency3;

  @Mock
  private OBDal mockOBDal;

  @InjectMocks
  private AddSubDependency addSubDependency;

  private MockedStatic<OBDal> mockedOBDalStatic;
  private Map<String, Object> parameters;

  // Test constants
  private static final String DEPENDENCY_ID_1 = "dep-001";
  private static final String DEPENDENCY_ID_2 = "dep-002";
  private static final String DEPENDENCY_ID_3 = "dep-003";

  @BeforeEach
  void setUp() {
    // Setup static mock for OBDal
    mockedOBDalStatic = mockStatic(OBDal.class);
    mockedOBDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Setup test parameters
    parameters = new HashMap<>();

    // Configure mock dependencies
    setupMockDependencies();
  }

  @AfterEach
  void tearDown() {
    // Critical: Close static mocks to prevent test pollution
    if (mockedOBDalStatic != null) {
      mockedOBDalStatic.close();
    }
  }

  private void setupMockDependencies() {

  }

  @Nested
  @DisplayName("Valid Input Tests")
  class ValidInputTests {

    @Test
    @DisplayName("Should create single criteria for one dependency")
    void testExecuteWithSingleDependency() throws Exception {
      // Given
      when(mockDependency1.getId()).thenReturn(DEPENDENCY_ID_1);

      // Configure OBDal mock to return specific dependencies
      when(mockOBDal.get(eq(PackageDependency.class), eq(DEPENDENCY_ID_1)))
          .thenReturn(mockDependency1);

      String content = createJsonContent(DEPENDENCY_ID_1);

      // When
      JSONObject result = addSubDependency.execute(parameters, content);

      // Then
      assertAll("Single dependency result validation",
          () -> assertTrue(result.has("filter"), "Result should contain filter"),
          () -> {
            JSONObject filter = result.getJSONObject("filter");
            assertEquals("and", filter.getString("operator"), "Filter operator should be 'and'");
            assertEquals("AdvancedCriteria", filter.getString("_constructor"), "Constructor should be AdvancedCriteria");

            JSONArray criteria = filter.getJSONArray("criteria");
            assertEquals(1, criteria.length(), "Should have exactly one criteria item");

            JSONObject criteriaItem = criteria.getJSONObject(0);
            assertEquals(DependencyManagerConstants.PARENT, criteriaItem.getString("fieldName"));
            assertEquals("iEquals", criteriaItem.getString("operator"));
            assertEquals(DEPENDENCY_ID_1, criteriaItem.getString("value"));
          }
      );

      // Verify OBDal interaction
      verify(mockOBDal, times(1)).get(PackageDependency.class, DEPENDENCY_ID_1);
    }

    @Test
    @DisplayName("Should create OR criteria for multiple dependencies")
    void testExecuteWithMultipleDependencies() throws Exception {
      // Given
      when(mockDependency1.getId()).thenReturn(DEPENDENCY_ID_1);
      when(mockDependency2.getId()).thenReturn(DEPENDENCY_ID_2);

      // Configure OBDal mock to return specific dependencies
      when(mockOBDal.get(eq(PackageDependency.class), eq(DEPENDENCY_ID_1)))
          .thenReturn(mockDependency1);
      when(mockOBDal.get(eq(PackageDependency.class), eq(DEPENDENCY_ID_2)))
          .thenReturn(mockDependency2);
      String content = createJsonContent(DEPENDENCY_ID_1, DEPENDENCY_ID_2);

      // When
      JSONObject result = addSubDependency.execute(parameters, content);

      // Then
      assertAll("Multiple dependencies result validation",
          () -> assertTrue(result.has("filter"), "Result should contain filter"),
          () -> {
            JSONObject filter = result.getJSONObject("filter");
            assertEquals("and", filter.getString("operator"), "Top level operator should be 'and'");

            JSONArray criteria = filter.getJSONArray("criteria");
            assertEquals(1, criteria.length(), "Should have exactly one criteria item (OR group)");

            JSONObject orCriteria = criteria.getJSONObject(0);
            assertEquals("or", orCriteria.getString("operator"), "Inner operator should be 'or'");

            JSONArray orCriteriaArray = orCriteria.getJSONArray("criteria");
            assertEquals(2, orCriteriaArray.length(), "OR criteria should have 2 items");

            // Validate individual criteria items
            validateCriteriaItem(orCriteriaArray.getJSONObject(0), DEPENDENCY_ID_1);
            validateCriteriaItem(orCriteriaArray.getJSONObject(1), DEPENDENCY_ID_2);
          }
      );

      // Verify OBDal interactions
      verify(mockOBDal, times(1)).get(PackageDependency.class, DEPENDENCY_ID_1);
      verify(mockOBDal, times(1)).get(PackageDependency.class, DEPENDENCY_ID_2);
    }

    private void validateCriteriaItem(JSONObject criteriaItem, String expectedValue) throws Exception {
      assertAll("Criteria item validation for " + expectedValue,
          () -> assertEquals(DependencyManagerConstants.PARENT, criteriaItem.getString("fieldName")),
          () -> assertEquals("iEquals", criteriaItem.getString("operator")),
          () -> assertEquals(expectedValue, criteriaItem.getString("value")),
          () -> assertEquals("AdvancedCriteria", criteriaItem.getString("_constructor"))
      );
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesAndErrorHandling {

    @Test
    @DisplayName("Should return empty result when dependencyId is missing")
    void testExecuteWithoutDependencyId() throws Exception {
      // Given
      String content = "{}";

      // When
      JSONObject result = addSubDependency.execute(parameters, content);

      // Then
      assertAll("Empty result validation",
          () -> assertNotNull(result, "Result should not be null"),
          () -> assertFalse(result.has("filter"), "Result should not contain filter"),
          () -> assertEquals(0, result.length(), "Result should be empty")
      );

      // Verify no OBDal interactions
      verify(mockOBDal, never()).get(any(Class.class), any(String.class));
    }

    @Test
    @DisplayName("Should return empty result when dependency array is empty")
    void testExecuteWithEmptyDependencyArray() throws Exception {
      // Given
      String content = "{\"dependencyId\": []}";

      // When
      JSONObject result = addSubDependency.execute(parameters, content);

      // Then
      assertAll("Empty array result validation",
          () -> assertNotNull(result, "Result should not be null"),
          () -> assertFalse(result.has("filter"), "Result should not contain filter"),
          () -> assertEquals(0, result.length(), "Result should be empty")
      );

      // Verify no OBDal interactions
      verify(mockOBDal, never()).get(any(Class.class), any(String.class));
    }

    @Test
    @DisplayName("Should throw OBException when JSON content is invalid")
    void testExecuteWithInvalidJson() {
      // Given
      String invalidContent = "invalid json content";

      // When & Then
      OBException exception = assertThrows(OBException.class,
          () -> addSubDependency.execute(parameters, invalidContent));

      assertNotNull(exception.getCause(), "Exception should have a cause");

      // Verify no OBDal interactions
      verify(mockOBDal, never()).get(any(Class.class), any(String.class));
    }

    @Test
    @DisplayName("Should throw OBException when OBDal throws exception")
    void testExecuteWithOBDalException() {
      // Given
      String content = createJsonContent(DEPENDENCY_ID_1);
      when(mockOBDal.get(eq(PackageDependency.class), eq(DEPENDENCY_ID_1)))
          .thenThrow(new RuntimeException("Database error"));

      // When & Then
      OBException exception = assertThrows(OBException.class,
          () -> addSubDependency.execute(parameters, content));

      assertNotNull(exception.getCause(), "Exception should have a cause");
      assertTrue(exception.getCause() instanceof RuntimeException);
      assertEquals("Database error", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should handle null content gracefully")
    void testExecuteWithNullContent() {
      // When & Then
      assertThrows(OBException.class,
          () -> addSubDependency.execute(parameters, null));
    }
  }

  @Nested
  @DisplayName("Integration-like Tests")
  class IntegrationLikeTests {

    @Test
    @DisplayName("Should handle real-world scenario with mixed dependencies")
    void testRealWorldScenario() throws Exception {
      // Given - Simulate a real scenario with actual dependency IDs
      String content = createJsonContent(
          "com.etendoerp.module1",
          "org.openbravo.core",
          "com.smf.securewebservices"
      );

      // Configure mocks for realistic IDs
      PackageDependency module1 = mock(PackageDependency.class);
      PackageDependency coreModule = mock(PackageDependency.class);
      PackageDependency webservices = mock(PackageDependency.class);

      when(module1.getId()).thenReturn("com.etendoerp.module1");
      when(coreModule.getId()).thenReturn("org.openbravo.core");
      when(webservices.getId()).thenReturn("com.smf.securewebservices");

      when(mockOBDal.get(eq(PackageDependency.class), eq("com.etendoerp.module1")))
          .thenReturn(module1);
      when(mockOBDal.get(eq(PackageDependency.class), eq("org.openbravo.core")))
          .thenReturn(coreModule);
      when(mockOBDal.get(eq(PackageDependency.class), eq("com.smf.securewebservices")))
          .thenReturn(webservices);

      // When
      JSONObject result = addSubDependency.execute(parameters, content);

      // Then
      assertAll("Real world scenario validation",
          () -> assertTrue(result.has("filter"), "Should have filter"),
          () -> {
            JSONObject filter = result.getJSONObject("filter");
            JSONArray criteria = filter.getJSONArray("criteria");
            JSONObject orCriteria = criteria.getJSONObject(0);
            JSONArray orCriteriaArray = orCriteria.getJSONArray("criteria");

            assertEquals(3, orCriteriaArray.length(), "Should have 3 dependencies");

            // Verify all dependencies are included
            boolean hasModule1 = false, hasCore = false, hasWebservices = false;
            for (int i = 0; i < orCriteriaArray.length(); i++) {
              String value = orCriteriaArray.getJSONObject(i).getString("value");
              if ("com.etendoerp.module1".equals(value)) hasModule1 = true;
              if ("org.openbravo.core".equals(value)) hasCore = true;
              if ("com.smf.securewebservices".equals(value)) hasWebservices = true;
            }

            assertTrue(hasModule1, "Should include module1");
            assertTrue(hasCore, "Should include core");
            assertTrue(hasWebservices, "Should include webservices");
          }
      );
    }
  }

  /**
   * Helper method to create JSON content with dependency IDs
   */
  private String createJsonContent(String... dependencyIds) {
    try {
      JSONObject json = new JSONObject();
      JSONArray array = new JSONArray();
      for (String id : dependencyIds) {
        array.put(id);
      }
      json.put("dependencyId", array);
      return json.toString();
    } catch (Exception e) {
      throw new RuntimeException("Failed to create test JSON content", e);
    }
  }
}