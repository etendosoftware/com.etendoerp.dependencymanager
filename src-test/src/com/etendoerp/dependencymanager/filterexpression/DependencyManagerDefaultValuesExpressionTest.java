package com.etendoerp.dependencymanager.filterexpression;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;

import com.etendoerp.dependencymanager.util.DependencyUtil;

import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.CONTEXT;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.CURRENT_PARAM;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.INPUT_FORMAT;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.IS_EXTERNAL_DEPENDENCY;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.NEW_FORMAT;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.VERSION_DISPLAY_LOGIC;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for DependencyManagerDefaultValuesExpression class.
 * Tests cover all branches including edge cases and error handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DependencyManagerDefaultValuesExpression Tests")
class DependencyManagerDefaultValuesExpressionTest {

  private DependencyManagerDefaultValuesExpression filterExpression;
  private Map<String, String> requestMap;

  /**
   * Initializes the filter expression and request map before each test execution.
   * Ensures a clean state for every test case.
   */
  @BeforeEach
  void setUp() {
    filterExpression = new DependencyManagerDefaultValuesExpression();
    requestMap = new HashMap<>();
  }

  /**
   * Tests the logic for the NEW_FORMAT parameter, including switching between JAR and SOURCE formats,
   * and handling unknown or local formats.
   */
  @Nested
  @DisplayName("NEW_FORMAT Parameter Tests")
  class NewFormatTests {

    /**
     * Verifies that the filter expression returns the JAR format
     * when the current format in the context is SOURCE.
     *
     * @throws Exception
     *     if there is an error during the test execution.
     */
    @Test
    @DisplayName("Should return JAR format when current format is SOURCE")
    void shouldReturnJarWhenFormatIsSource() throws Exception {
      JSONObject context = new JSONObject();
      context.put(INPUT_FORMAT, DependencyUtil.FORMAT_SOURCE);

      requestMap.put(CURRENT_PARAM, NEW_FORMAT);
      requestMap.put(CONTEXT, context.toString());

      String result = filterExpression.getExpression(requestMap);

      assertEquals(DependencyUtil.FORMAT_JAR, result,
          "Should return JAR format when current format is SOURCE");
    }

    /**
     * Verifies that the filter expression returns the SOURCE format
     * when the current format in the context is JAR.
     *
     * @throws Exception
     *     if there is an error during the test execution.
     */
    @Test
    @DisplayName("Should return SOURCE format when current format is JAR")
    void shouldReturnSourceWhenFormatIsJar() throws Exception {
      JSONObject context = new JSONObject();
      context.put(INPUT_FORMAT, DependencyUtil.FORMAT_JAR);

      requestMap.put(CURRENT_PARAM, NEW_FORMAT);
      requestMap.put(CONTEXT, context.toString());

      String result = filterExpression.getExpression(requestMap);

      assertEquals(DependencyUtil.FORMAT_SOURCE, result,
          "Should return SOURCE format when current format is JAR");
    }

    /**
     * Verifies that the filter expression returns null for LOCAL format,
     * unknown formats, or empty format values.
     *
     * @param format
     *     the format value to test (LOCAL, unknown, or empty)
     * @throws Exception
     *     if there is an error during the test execution.
     */
    @ParameterizedTest
    @ValueSource(strings = { DependencyUtil.FORMAT_LOCAL, "UNKNOWN_FORMAT", "" })
    @DisplayName("Should return null for LOCAL format and unknown formats")
    void shouldReturnNullForLocalAndUnknownFormats(String format) throws Exception {
      JSONObject context = new JSONObject();
      context.put(INPUT_FORMAT, format);

      requestMap.put(CURRENT_PARAM, NEW_FORMAT);
      requestMap.put(CONTEXT, context.toString());

      String result = filterExpression.getExpression(requestMap);

      assertNull(result,
          "Should return null for LOCAL format and unknown formats: " + format);
    }
  }

  /**
   * Tests the extraction of the external version from the context for the "externalVersion" parameter.
   */
  @Nested
  @DisplayName("EXTERNAL_VERSION Parameter Tests")
  class ExternalVersionTests {

    /**
     * Verifies that the filter expression returns the version from the context
     * when the version is not empty for the "externalVersion" parameter.
     *
     * @throws Exception
     *     if there is an error during the test execution.
     */
    @Test
    @DisplayName("Should return version when version is not empty")
    void shouldReturnVersionWhenVersionIsNotEmpty() throws Exception {
      String expectedVersion = "1.2.3";
      JSONObject context = new JSONObject();
      context.put("inpversion", expectedVersion);

      requestMap.put(CURRENT_PARAM, "externalVersion");
      requestMap.put(CONTEXT, context.toString());

      String result = filterExpression.getExpression(requestMap);

      assertEquals(expectedVersion, result,
          "Should return the version when it's not empty");
    }

  }


  /**
   * Tests the display logic for the version field based on the "inpisexternaldependency" context value.
   */
  @Nested
  @DisplayName("VERSION_DISPLAY_LOGIC Parameter Tests")
  class VersionDisplayLogicTests {

    /**
     * Verifies that the filter expression returns "Y" when the external dependency
     * context value is "N" for the "version_display_logic" parameter.
     *
     * @throws Exception
     *     if there is an error during the test execution.
     */
    @Test
    @DisplayName("Should return Y when external dependency is N")
    void shouldReturnYWhenExternalDependencyIsN() throws Exception {
      JSONObject context = new JSONObject();
      context.put(IS_EXTERNAL_DEPENDENCY, "N");

      requestMap.put(CURRENT_PARAM, VERSION_DISPLAY_LOGIC);
      requestMap.put(CONTEXT, context.toString());

      String result = filterExpression.getExpression(requestMap);

      assertEquals("Y", result,
          "Should return Y when external dependency is N");
    }

    /**
     * Verifies that the filter expression returns "N" when the external dependency
     * context value is not "N" for the "version_display_logic" parameter.
     *
     * @param value
     *     the value of the external dependency context to test
     * @throws Exception
     *     if there is an error during the test execution.
     */
    @ParameterizedTest
    @ValueSource(strings = { "Y", "true", "1", "anything_else" })
    @DisplayName("Should return N when external dependency is not N")
    void shouldReturnNWhenExternalDependencyIsNotN(String value) throws Exception {
      JSONObject context = new JSONObject();
      context.put(IS_EXTERNAL_DEPENDENCY, value);

      requestMap.put(CURRENT_PARAM, VERSION_DISPLAY_LOGIC);
      requestMap.put(CONTEXT, context.toString());

      String result = filterExpression.getExpression(requestMap);

      assertEquals("N", result,
          "Should return N when external dependency is not N: " + value);
    }
  }

  /**
   * Tests error handling, including invalid JSON in the context and unknown parameters.
   */
  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    /**
     * Verifies that an OBException is thrown when the context JSON is invalid.
     */
    @Test
    @DisplayName("Should throw OBException when context JSON is invalid")
    void shouldThrowOBExceptionWhenContextJsonIsInvalid() {
      requestMap.put(CURRENT_PARAM, NEW_FORMAT);
      requestMap.put(CONTEXT, "invalid-json-string");

      OBException exception = assertThrows(OBException.class, () -> filterExpression.getExpression(requestMap), "Should throw OBException when context JSON is invalid");

      assertNotNull(exception.getCause(), "Exception should have a cause");
      assertInstanceOf(JSONException.class, exception.getCause(), "Exception cause should be JSONException");
    }

    /**
     * Verifies that the filter expression returns null when the current parameter is unknown.
     *
     * @throws Exception
     *     if there is an error during the test execution.
     */
    @Test
    @DisplayName("Should return null when currentParam is unknown")
    void shouldReturnNullWhenCurrentParamIsUnknown() throws Exception {
      JSONObject context = new JSONObject();
      context.put(INPUT_FORMAT, DependencyUtil.FORMAT_SOURCE);

      requestMap.put(CURRENT_PARAM, "unknownParameter");
      requestMap.put(CONTEXT, context.toString());

      String result = filterExpression.getExpression(requestMap);

      assertNull(result,
          "Should return null when currentParam is unknown");
    }

  }

  /**
   * Integration tests that validate the complete workflow for all supported parameters and contexts.
   */
  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    /**
     * Integration test that verifies the complete workflow for all supported parameters
     * and valid contexts, ensuring correct results for each case.
     */
    @Test
    @DisplayName("Should handle complete workflow for all parameters")
    void shouldHandleCompleteWorkflowForAllParameters() {
      // Test all parameters with valid contexts
      assertAll("All parameter combinations should work correctly",
          () -> {
            // Test newFormat with SOURCE
            JSONObject context1 = new JSONObject();
            context1.put(INPUT_FORMAT, DependencyUtil.FORMAT_SOURCE);
            Map<String, String> map1 = new HashMap<>();
            map1.put(CURRENT_PARAM, NEW_FORMAT);
            map1.put(CONTEXT, context1.toString());
            assertEquals(DependencyUtil.FORMAT_JAR,
                filterExpression.getExpression(map1));
          },
          () -> {
            // Test externalVersion
            JSONObject context2 = new JSONObject();
            context2.put("inpversion", "2.0.0");
            Map<String, String> map2 = new HashMap<>();
            map2.put(CURRENT_PARAM, "externalVersion");
            map2.put(CONTEXT, context2.toString());
            assertEquals("2.0.0", filterExpression.getExpression(map2));
          },
          () -> {
            // Test version_display_logic
            JSONObject context3 = new JSONObject();
            context3.put(IS_EXTERNAL_DEPENDENCY, "N");
            Map<String, String> map3 = new HashMap<>();
            map3.put(CURRENT_PARAM, VERSION_DISPLAY_LOGIC);
            map3.put(CONTEXT, context3.toString());
            assertEquals("Y", filterExpression.getExpression(map3));
          }
      );
    }
  }

}
