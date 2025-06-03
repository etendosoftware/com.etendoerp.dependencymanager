package com.etendoerp.dependencymanager.defaults;

import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.EXPECTED_FORMATS;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.INVALID_JSON_CONTENT;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.VALID_JSON_CONTENT;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.RequestContext;

import com.etendoerp.dependencymanager.util.ChangeFormatUtil;

/**
 * Test class for ChangeFormatDefaults
 * This test suite demonstrates:
 * 1. Proper mocking of static methods (RequestContext, ChangeFormatUtil)
 * 2. Clean setup and teardown of static mocks
 * 3. Comprehensive error handling scenarios
 * 4. Improved assertions with assertAll for better readability
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeFormatDefaults Tests")
class ChangeFormatDefaultsTest {

  private ChangeFormatDefaults changeFormatDefaults;

  @Mock
  private RequestContext mockRequestContext;

  private MockedStatic<RequestContext> mockedStaticRequestContext;
  private MockedStatic<ChangeFormatUtil> mockedStaticChangeFormatUtil;

  private Map<String, Object> testParameters;


  /**
   * Sets up the test environment before each test.
   * <p>
   * Initializes the class under test, test parameters, and static mocks.
   * Configures default behaviors for mocked dependencies.
   * </p>
   */
  @BeforeEach
  void setUp() {
    changeFormatDefaults = new ChangeFormatDefaults();

    testParameters = new HashMap<>();

    setupStaticMocks();

    configureMockBehaviors();
  }

  /**
   * Cleans up the test environment after each test.
   * <p>
   * Ensures static mocks are properly closed to prevent memory leaks.
   * </p>
   */
  @AfterEach
  void tearDown() {
    cleanupStaticMocks();
  }

  /**
   * Setup static mocks with proper resource management
   */
  private void setupStaticMocks() {
    mockedStaticRequestContext = mockStatic(RequestContext.class);
    mockedStaticChangeFormatUtil = mockStatic(ChangeFormatUtil.class);
  }

  /**
   * Configure default behaviors for mocked dependencies
   */
  private void configureMockBehaviors() {
    // Configure RequestContext static mock
    mockedStaticRequestContext.when(RequestContext::get)
        .thenReturn(mockRequestContext);

  }

  /**
   * Clean up static mocks to prevent memory leaks
   */
  private void cleanupStaticMocks() {
    if (mockedStaticRequestContext != null) {
      mockedStaticRequestContext.close();
    }
    if (mockedStaticChangeFormatUtil != null) {
      mockedStaticChangeFormatUtil.close();
    }
  }

  /**
   * Verifies that an empty format list is handled gracefully.
   */
  @Test
  @DisplayName("Should handle empty format list gracefully")
  void testExecuteEmptyFormatListReturnsEmptyArray() {
    List<String> emptyFormats = List.of();
    mockedStaticChangeFormatUtil.when(() ->
        ChangeFormatUtil.getNewFormatList(anyString(), anyString(), any(), any())
    ).thenReturn(emptyFormats);

    JSONObject result = changeFormatDefaults.execute(testParameters, VALID_JSON_CONTENT);

    assertAll("Empty result validation",
        () -> assertNotNull(result, "Result should not be null"),
        () -> assertTrue(result.has("newFormats"), "Result should contain 'newFormats' key"),
        () -> {
          JSONArray newFormats = result.getJSONArray("newFormats");
          assertEquals(0, newFormats.length(), "New formats array should be empty");
        }
    );
  }

  /**
   * Verifies that an invalid JSON content throws an `OBException`.
   */
  @Test
  @DisplayName("Should throw OBException when JSON content is invalid")
  void testExecuteInvalidJsonContentThrowsOBException() {
    OBException exception = assertThrows(OBException.class,
        () -> changeFormatDefaults.execute(testParameters, INVALID_JSON_CONTENT),
        "Should throw OBException for invalid JSON content"
    );

    assertAll("Exception validation",
        () -> assertNotNull(exception.getCause(), "Exception should have a cause"),
        () -> assertInstanceOf(JSONException.class, exception.getCause(), "Root cause should be JSONException")
    );
  }

  /**
   * Verifies that missing `currentFormat` in JSON throws an `OBException`.
   */
  @Test
  @DisplayName("Should throw OBException when currentFormat is missing from JSON")
  void testExecuteMissingCurrentFormatThrowsOBException() {
    String jsonWithoutCurrentFormat = "{\"otherField\":\"value\"}";

    OBException exception = assertThrows(OBException.class,
        () -> changeFormatDefaults.execute(testParameters, jsonWithoutCurrentFormat),
        "Should throw OBException when currentFormat is missing"
    );

    assertNotNull(exception.getCause(), "Exception should have a cause");
  }

  /**
   * Verifies that an exception in `ChangeFormatUtil` is propagated as an `OBException`.
   */
  @Test
  @DisplayName("Should throw OBException when ChangeFormatUtil throws exception")
  void testExecuteUtilMethodThrowsExceptionThrowsOBException() {
    RuntimeException utilException = new RuntimeException("Utility method error");
    mockedStaticChangeFormatUtil.when(() ->
        ChangeFormatUtil.getNewFormatList(anyString(), anyString(), any(), any())
    ).thenThrow(utilException);

    OBException exception = assertThrows(OBException.class,
        () -> changeFormatDefaults.execute(testParameters, VALID_JSON_CONTENT),
        "Should throw OBException when utility method fails"
    );

    assertEquals(utilException, exception.getCause(),
        "Exception cause should be the original utility exception");
  }

  /**
   * Verifies that null content is handled gracefully by throwing an `OBException`.
   */
  @Test
  @DisplayName("Should handle null content gracefully")
  void testExecuteNullContentThrowsOBException() {
    assertThrows(OBException.class,
        () -> changeFormatDefaults.execute(testParameters, null),
        "Should throw OBException for null content"
    );
  }

  /**
   * Verifies that empty content is handled gracefully by throwing an `OBException`.
   */
  @Test
  @DisplayName("Should handle empty content gracefully")
  void testExecuteEmptyContentThrowsOBException() {
    assertThrows(OBException.class,
        () -> changeFormatDefaults.execute(testParameters, ""),
        "Should throw OBException for empty content"
    );
  }

  /**
   * Verifies that a null `RequestContext` throws an `OBException`.
   */
  @Test
  @DisplayName("Should handle RequestContext returning null")
  void testExecuteRequestContextReturnsNullThrowsOBException() {
    mockedStaticRequestContext.when(RequestContext::get).thenReturn(null);

    assertThrows(OBException.class,
        () -> changeFormatDefaults.execute(testParameters, VALID_JSON_CONTENT),
        "Should throw OBException when RequestContext returns null"
    );
  }

  /**
   * Verifies that a null `VariablesSecureApp` is handled gracefully.
   */
  @Test
  @DisplayName("Should handle VariablesSecureApp returning null")
  void testExecuteVariablesSecureAppReturnsNullThrowsOBException() {
    when(mockRequestContext.getVariablesSecureApp()).thenReturn(null);
    mockedStaticChangeFormatUtil.when(() ->
        ChangeFormatUtil.getNewFormatList(anyString(), anyString(), eq(null), any())
    ).thenReturn(EXPECTED_FORMATS);

    JSONObject result = changeFormatDefaults.execute(testParameters, VALID_JSON_CONTENT);

    assertNotNull(result, "Result should not be null even with null VariablesSecureApp");
  }

}
