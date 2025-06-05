package com.etendoerp.dependencymanager.process;

import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.CRITERIA;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.DEPENDENCY_ID_1;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.DEPENDENCY_ID_2;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.FILTER;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.MODULE_ETENDOERP;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.MODULE_OPENBRAVO_CORE;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.MODULE_SECURE_WEBSERVICES;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.OPERATOR;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.VALUE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


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
  private OBDal mockOBDal;

  @InjectMocks
  private AddSubDependency addSubDependency;

  private MockedStatic<OBDal> mockedOBDalStatic;
  private Map<String, Object> parameters;

  @BeforeEach
  void setUp() {
    mockedOBDalStatic = mockStatic(OBDal.class);
    mockedOBDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    parameters = new HashMap<>();

  }

  @AfterEach
  void tearDown() {
    if (mockedOBDalStatic != null) {
      mockedOBDalStatic.close();
    }
  }

  @Nested
  @DisplayName("Valid Input Tests")
  class ValidInputTests {

    @Test
    @DisplayName("Should create single criteria for one dependency")
    void testExecuteWithSingleDependency() {
      when(mockDependency1.getId()).thenReturn(DEPENDENCY_ID_1);

      when(mockOBDal.get(eq(PackageDependency.class), eq(DEPENDENCY_ID_1)))
          .thenReturn(mockDependency1);

      String content = createJsonContent(DEPENDENCY_ID_1);

      JSONObject result = addSubDependency.execute(parameters, content);

      assertAll("Single dependency result validation",
          () -> assertTrue(result.has(FILTER), "Result should contain filter"),
          () -> {
            JSONObject filter = result.getJSONObject(FILTER);
            assertEquals("and", filter.getString(OPERATOR), "Filter operator should be 'and'");
            assertEquals("AdvancedCriteria", filter.getString("_constructor"), "Constructor should be AdvancedCriteria");

            JSONArray criteria = filter.getJSONArray(CRITERIA);
            assertEquals(1, criteria.length(), "Should have exactly one criteria item");

            JSONObject criteriaItem = criteria.getJSONObject(0);
            assertEquals(DependencyManagerConstants.PARENT, criteriaItem.getString("fieldName"));
            assertEquals("iEquals", criteriaItem.getString(OPERATOR));
            assertEquals(DEPENDENCY_ID_1, criteriaItem.getString(VALUE));
          }
      );

      verify(mockOBDal, times(1)).get(PackageDependency.class, DEPENDENCY_ID_1);
    }

    @Test
    @DisplayName("Should create OR criteria for multiple dependencies")
    void testExecuteWithMultipleDependencies() {
      when(mockDependency1.getId()).thenReturn(DEPENDENCY_ID_1);
      when(mockDependency2.getId()).thenReturn(DEPENDENCY_ID_2);

      when(mockOBDal.get(eq(PackageDependency.class), eq(DEPENDENCY_ID_1)))
          .thenReturn(mockDependency1);
      when(mockOBDal.get(eq(PackageDependency.class), eq(DEPENDENCY_ID_2)))
          .thenReturn(mockDependency2);
      String content = createJsonContent(DEPENDENCY_ID_1, DEPENDENCY_ID_2);

      JSONObject result = addSubDependency.execute(parameters, content);

      assertAll("Multiple dependencies result validation",
          () -> assertTrue(result.has(FILTER), "Result should contain filter"),
          () -> {
            JSONObject filter = result.getJSONObject(FILTER);
            assertEquals("and", filter.getString(OPERATOR), "Top level operator should be 'and'");

            JSONArray criteria = filter.getJSONArray(CRITERIA);
            assertEquals(1, criteria.length(), "Should have exactly one criteria item (OR group)");

            JSONObject orCriteria = criteria.getJSONObject(0);
            assertEquals("or", orCriteria.getString(OPERATOR), "Inner operator should be 'or'");

            JSONArray orCriteriaArray = orCriteria.getJSONArray(CRITERIA);
            assertEquals(2, orCriteriaArray.length(), "OR criteria should have 2 items");

            validateCriteriaItem(orCriteriaArray.getJSONObject(0), DEPENDENCY_ID_1);
            validateCriteriaItem(orCriteriaArray.getJSONObject(1), DEPENDENCY_ID_2);
          }
      );

      verify(mockOBDal, times(1)).get(PackageDependency.class, DEPENDENCY_ID_1);
      verify(mockOBDal, times(1)).get(PackageDependency.class, DEPENDENCY_ID_2);
    }

    private void validateCriteriaItem(JSONObject criteriaItem, String expectedValue) throws Exception {
      assertAll("Criteria item validation for " + expectedValue,
          () -> assertEquals(DependencyManagerConstants.PARENT, criteriaItem.getString("fieldName")),
          () -> assertEquals("iEquals", criteriaItem.getString(OPERATOR)),
          () -> assertEquals(expectedValue, criteriaItem.getString(VALUE)),
          () -> assertEquals("AdvancedCriteria", criteriaItem.getString("_constructor"))
      );
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesAndErrorHandling {

    @Test
    @DisplayName("Should return empty result when dependencyId is missing")
    void testExecuteWithoutDependencyId() {
      String content = "{}";

      JSONObject result = addSubDependency.execute(parameters, content);

      assertAll("Empty result validation",
          () -> assertNotNull(result, "Result should not be null"),
          () -> assertFalse(result.has(FILTER), "Result should not contain filter"),
          () -> assertEquals(0, result.length(), "Result should be empty")
      );

      verify(mockOBDal, never()).get(any(Class.class), any(String.class));
    }

    @Test
    @DisplayName("Should return empty result when dependency array is empty")
    void testExecuteWithEmptyDependencyArray() {
      String content = "{\"dependencyId\": []}";

      JSONObject result = addSubDependency.execute(parameters, content);

      assertAll("Empty array result validation",
          () -> assertNotNull(result, "Result should not be null"),
          () -> assertFalse(result.has(FILTER), "Result should not contain filter"),
          () -> assertEquals(0, result.length(), "Result should be empty")
      );

      verify(mockOBDal, never()).get(any(Class.class), any(String.class));
    }

    @Test
    @DisplayName("Should throw OBException when JSON content is invalid")
    void testExecuteWithInvalidJson() {
      String invalidContent = "invalid json content";

      OBException exception = assertThrows(OBException.class,
          () -> addSubDependency.execute(parameters, invalidContent));

      assertNotNull(exception.getCause(), "Exception should have a cause");

      verify(mockOBDal, never()).get(any(Class.class), any(String.class));
    }

    @Test
    @DisplayName("Should throw OBException when OBDal throws exception")
    void testExecuteWithOBDalException() {
      String content = createJsonContent(DEPENDENCY_ID_1);
      when(mockOBDal.get(eq(PackageDependency.class), eq(DEPENDENCY_ID_1)))
          .thenThrow(new RuntimeException("Database error"));

      OBException exception = assertThrows(OBException.class,
          () -> addSubDependency.execute(parameters, content));

      assertNotNull(exception.getCause(), "Exception should have a cause");
      assertInstanceOf(RuntimeException.class, exception.getCause());
      assertEquals("Database error", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should handle null content gracefully")
    void testExecuteWithNullContent() {
      assertThrows(OBException.class,
          () -> addSubDependency.execute(parameters, null));
    }
  }

  @Nested
  @DisplayName("Integration-like Tests")
  class IntegrationLikeTests {

    @Test
    @DisplayName("Should handle real-world scenario with mixed dependencies")
    void testRealWorldScenario() {
      String content = createJsonContent(
          MODULE_ETENDOERP,
          MODULE_OPENBRAVO_CORE,
          MODULE_SECURE_WEBSERVICES
      );

      PackageDependency module1 = mock(PackageDependency.class);
      PackageDependency coreModule = mock(PackageDependency.class);
      PackageDependency webservices = mock(PackageDependency.class);

      when(module1.getId()).thenReturn(MODULE_ETENDOERP);
      when(coreModule.getId()).thenReturn(MODULE_OPENBRAVO_CORE);
      when(webservices.getId()).thenReturn(MODULE_SECURE_WEBSERVICES);

      when(mockOBDal.get(eq(PackageDependency.class), eq(MODULE_ETENDOERP)))
          .thenReturn(module1);
      when(mockOBDal.get(eq(PackageDependency.class), eq(MODULE_OPENBRAVO_CORE)))
          .thenReturn(coreModule);
      when(mockOBDal.get(eq(PackageDependency.class), eq(MODULE_SECURE_WEBSERVICES)))
          .thenReturn(webservices);

      JSONObject result = addSubDependency.execute(parameters, content);

      assertAll("Real world scenario validation",
          () -> assertTrue(result.has(FILTER), "Should have filter"),
          () -> {
            JSONObject filter = result.getJSONObject(FILTER);
            JSONArray criteria = filter.getJSONArray(CRITERIA);
            JSONObject orCriteria = criteria.getJSONObject(0);
            JSONArray orCriteriaArray = orCriteria.getJSONArray(CRITERIA);

            assertEquals(3, orCriteriaArray.length(), "Should have 3 dependencies");

            boolean hasModule1 = false, hasCore = false, hasWebservices = false;
            for (int i = 0; i < orCriteriaArray.length(); i++) {
              String value = orCriteriaArray.getJSONObject(i).getString(VALUE);
              if (MODULE_ETENDOERP.equals(value)) hasModule1 = true;
              if (MODULE_OPENBRAVO_CORE.equals(value)) hasCore = true;
              if (MODULE_SECURE_WEBSERVICES.equals(value)) hasWebservices = true;
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
