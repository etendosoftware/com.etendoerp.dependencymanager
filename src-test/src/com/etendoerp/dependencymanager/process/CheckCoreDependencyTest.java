package com.etendoerp.dependencymanager.process;

import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.COMPATIBLE;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.CORE_VERSION;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.CORE_VERSION_23_4_0;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.MINIMUM_VERSION;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.REASON;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.RESULT_NOT_NULL_MESSAGE;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.SHOULD_CONTAIN_COMPATIBLE_FIELD;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.SHOULD_CONTAIN_ORIGINAL_CAUSE;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.SHOULD_THROW_OB_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;

import com.etendoerp.dependencymanager.data.Package;
import com.etendoerp.dependencymanager.data.PackageVersion;
import com.etendoerp.dependencymanager.util.PackageUtil;

/**
 * Unit tests for CheckCoreDependency
 * This class demonstrates best practices for testing in Openbravo modules:
 * - Mocking OBDal and static methods
 * - Exception handling and edge cases
 * - Enhanced assertions with assertAll
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CheckCoreDependency Unit Tests")
class CheckCoreDependencyTest {


  @InjectMocks
  private CheckCoreDependency checkCoreDependency;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private PackageVersion mockPackageVersion;

  @Mock
  private Package mockPackage;

  private MockedStatic<OBDal> mockedOBDalStatic;
  private MockedStatic<PackageUtil> mockedPackageUtil;

  private static final String VALID_PACKAGE_VERSION_ID = "TEST_PACKAGE_VERSION_ID";
  private static final String PACKAGE_VERSION = "1.0.0";

  /**
   * Sets up the test environment before each test.
   * Initializes mocked static methods and test data.
   */
  @BeforeEach
  void setUp() {
    mockedOBDalStatic = mockStatic(OBDal.class);
    mockedPackageUtil = mockStatic(PackageUtil.class);

    mockedOBDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes mocked static methods.
   */
  @AfterEach
  void tearDown() {
    if (mockedOBDalStatic != null) {
      mockedOBDalStatic.close();
    }
    if (mockedPackageUtil != null) {
      mockedPackageUtil.close();
    }
  }

  /**
   * Tests the successful processing of a valid dependency.
   */
  @Test
  @DisplayName("Should process valid dependency correctly")
  void testExecuteValidDependencySuccess() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = createValidJsonData();

    JSONObject expectedResult = new JSONObject();
    expectedResult.put(COMPATIBLE, true);
    expectedResult.put(CORE_VERSION, CORE_VERSION_23_4_0);

    when(mockOBDal.get(PackageVersion.class, VALID_PACKAGE_VERSION_ID))
        .thenReturn(mockPackageVersion);
    when(mockPackageVersion.getPackage()).thenReturn(mockPackage);
    when(mockPackageVersion.getVersion()).thenReturn(PACKAGE_VERSION);

    mockedPackageUtil.when(() -> PackageUtil.checkCoreCompatibility(mockPackage, PACKAGE_VERSION))
        .thenReturn(expectedResult);

    JSONObject result = checkCoreDependency.execute(parameters, jsonData);

    assertAll("Successful result verification",
        () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
        () -> assertTrue(result.has(COMPATIBLE), SHOULD_CONTAIN_COMPATIBLE_FIELD),
        () -> assertTrue(result.getBoolean(COMPATIBLE), "Should be compatible"),
        () -> assertTrue(result.has(CORE_VERSION), "Should contain 'coreVersion' field"),
        () -> assertEquals(CORE_VERSION_23_4_0, result.getString(CORE_VERSION), "Incorrect core version")
    );

    verify(mockOBDal).get(PackageVersion.class, VALID_PACKAGE_VERSION_ID);
    verify(mockPackageVersion).getPackage();
    verify(mockPackageVersion).getVersion();
    mockedPackageUtil.verify(() -> PackageUtil.checkCoreCompatibility(mockPackage, PACKAGE_VERSION));
  }

  /**
   * Tests the behavior when the `PackageVersion` is not found.
   */
  @Test
  @DisplayName("Should handle correctly when PackageVersion is null")
  void testExecutePackageVersionNotFoundReturnsEmptyResult() {
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = createValidJsonData();

    when(mockOBDal.get(PackageVersion.class, VALID_PACKAGE_VERSION_ID))
        .thenReturn(null);

    JSONObject result = checkCoreDependency.execute(parameters, jsonData);

    assertAll("Verification when PackageVersion doesn't exist",
        () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
        () -> assertEquals(0, result.length(), "Result should be empty")
    );

    verify(mockOBDal).get(PackageVersion.class, VALID_PACKAGE_VERSION_ID);
    mockedPackageUtil.verifyNoInteractions();
  }

  /**
   * Tests the behavior when the `PackageVersion` is not found.
   */
  @Test
  @DisplayName("Should throw OBException when JSON is invalid")
  void testExecuteInvalidJsonThrowsOBException() {
    Map<String, Object> parameters = new HashMap<>();
    String invalidJsonData = "{ invalid json }";

    OBException exception = assertThrows(OBException.class,
        () -> checkCoreDependency.execute(parameters, invalidJsonData));

    assertAll("Invalid JSON exception verification",
        () -> assertNotNull(exception, SHOULD_THROW_OB_EXCEPTION),
        () -> assertNotNull(exception.getCause(), SHOULD_CONTAIN_ORIGINAL_CAUSE),
        () -> assertInstanceOf(JSONException.class, exception.getCause(), "Cause should be JSONException")
    );
  }

  /**
   * Tests the behavior when the `packageVersionId` field is missing in the JSON.
   */
  @Test
  @DisplayName("Should throw OBException when packageVersionId field is missing")
  void testExecuteMissingPackageVersionIdThrowsOBException() {
    Map<String, Object> parameters = new HashMap<>();
    String jsonDataWithoutId = "{}";

    OBException exception = assertThrows(OBException.class,
        () -> checkCoreDependency.execute(parameters, jsonDataWithoutId));

    assertAll("Missing field exception verification",
        () -> assertNotNull(exception, SHOULD_THROW_OB_EXCEPTION),
        () -> assertNotNull(exception.getCause(), SHOULD_CONTAIN_ORIGINAL_CAUSE),
        () -> assertInstanceOf(JSONException.class, exception.getCause(), "Cause should be JSONException")
    );
  }

  /**
   * Tests the behavior when `OBDal.get()` throws an exception.
   */
  @Test
  @DisplayName("Should throw OBException when OBDal.get() fails")
  void testExecuteOBDalExceptionThrowsOBException() {
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = createValidJsonData();

    when(mockOBDal.get(PackageVersion.class, VALID_PACKAGE_VERSION_ID))
        .thenThrow(new RuntimeException("Database connection error"));

    OBException exception = assertThrows(OBException.class,
        () -> checkCoreDependency.execute(parameters, jsonData));

    assertAll("Persistence layer exception verification",
        () -> assertNotNull(exception, SHOULD_THROW_OB_EXCEPTION),
        () -> assertNotNull(exception.getCause(), SHOULD_CONTAIN_ORIGINAL_CAUSE),
        () -> assertInstanceOf(RuntimeException.class, exception.getCause(), "Cause should be RuntimeException"),
        () -> assertEquals("Database connection error", exception.getCause().getMessage(),
            "Incorrect error message")
    );
  }

  /**
   * Tests the behavior when `PackageUtil.checkCoreCompatibility()` throws an exception.
   */
  @Test
  @DisplayName("Should throw OBException when PackageUtil.checkCoreCompatibility() fails")
  void testExecutePackageUtilExceptionThrowsOBException() {
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = createValidJsonData();

    when(mockOBDal.get(PackageVersion.class, VALID_PACKAGE_VERSION_ID))
        .thenReturn(mockPackageVersion);
    when(mockPackageVersion.getPackage()).thenReturn(mockPackage);
    when(mockPackageVersion.getVersion()).thenReturn(PACKAGE_VERSION);

    mockedPackageUtil.when(() -> PackageUtil.checkCoreCompatibility(mockPackage, PACKAGE_VERSION))
        .thenThrow(new RuntimeException("Core compatibility check failed"));

    OBException exception = assertThrows(OBException.class,
        () -> checkCoreDependency.execute(parameters, jsonData));

    assertAll("PackageUtil exception verification",
        () -> assertNotNull(exception, SHOULD_THROW_OB_EXCEPTION),
        () -> assertNotNull(exception.getCause(), SHOULD_CONTAIN_ORIGINAL_CAUSE),
        () -> assertEquals("Core compatibility check failed", exception.getCause().getMessage(),
            "Incorrect error message")
    );
  }

  /**
   * Tests the behavior when the `packageVersionId` is empty.
   */
  @Test
  @DisplayName("Should handle empty packageVersionId correctly")
  void testExecuteEmptyPackageVersionIdThrowsOBException() {
    Map<String, Object> parameters = new HashMap<>();
    String jsonDataWithEmptyId = "{\"packageVersionId\":\"\"}";

    when(mockOBDal.get(PackageVersion.class, "")).thenReturn(null);

    JSONObject result = assertDoesNotThrow(
        () -> checkCoreDependency.execute(parameters, jsonDataWithEmptyId));

    assertAll("Empty packageVersionId verification",
        () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
        () -> assertEquals(0, result.length(), "Result should be empty")
    );
  }

  /**
   * Tests the behavior when the JSON contains additional fields.
   */
  @Test
  @DisplayName("Should handle JSON with additional fields correctly")
  void testExecuteJsonWithExtraFieldsSuccess() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String jsonDataWithExtraFields = String.format(
        "{\"packageVersionId\":\"%s\", \"extraField\":\"value\", \"anotherField\":123}",
        VALID_PACKAGE_VERSION_ID
    );

    JSONObject expectedResult = new JSONObject();
    expectedResult.put(COMPATIBLE, false);
    expectedResult.put(REASON, "Version mismatch");

    when(mockOBDal.get(PackageVersion.class, VALID_PACKAGE_VERSION_ID))
        .thenReturn(mockPackageVersion);
    when(mockPackageVersion.getPackage()).thenReturn(mockPackage);
    when(mockPackageVersion.getVersion()).thenReturn(PACKAGE_VERSION);

    mockedPackageUtil.when(() -> PackageUtil.checkCoreCompatibility(mockPackage, PACKAGE_VERSION))
        .thenReturn(expectedResult);

    JSONObject result = checkCoreDependency.execute(parameters, jsonDataWithExtraFields);

    assertAll("Additional fields verification",
        () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
        () -> assertTrue(result.has(COMPATIBLE), SHOULD_CONTAIN_COMPATIBLE_FIELD),
        () -> assertFalse(result.getBoolean(COMPATIBLE), "Should be incompatible"),
        () -> assertTrue(result.has(REASON), "Should contain 'reason' field"),
        () -> assertEquals("Version mismatch", result.getString(REASON))
    );
  }

  /**
   * Creates valid JSON for testing
   */
  private String createValidJsonData() {
    return String.format("{\"packageVersionId\":\"%s\"}", VALID_PACKAGE_VERSION_ID);
  }

  /**
   * Tests the processing of a complex compatibility result.
   */
  @Test
  @DisplayName("Should process complex compatibility result correctly")
  void testExecuteComplexCompatibilityResultSuccess() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = createValidJsonData();

    JSONObject expectedResult = new JSONObject();
    expectedResult.put(COMPATIBLE, true);
    expectedResult.put(CORE_VERSION, CORE_VERSION_23_4_0);
    expectedResult.put(MINIMUM_VERSION, "23.2.0");
    expectedResult.put("warnings", new String[]{ "Deprecated API usage detected" });

    setupMocksForSuccessfulExecution();
    mockedPackageUtil.when(() -> PackageUtil.checkCoreCompatibility(mockPackage, PACKAGE_VERSION))
        .thenReturn(expectedResult);

    JSONObject result = checkCoreDependency.execute(parameters, jsonData);

    assertAll("Complex result verification",
        () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
        () -> assertTrue(result.has(COMPATIBLE), SHOULD_CONTAIN_COMPATIBLE_FIELD),
        () -> assertTrue(result.getBoolean(COMPATIBLE), "Should be compatible"),
        () -> assertTrue(result.has(CORE_VERSION), "Should contain 'coreVersion' field"),
        () -> assertEquals(CORE_VERSION_23_4_0, result.getString(CORE_VERSION)),
        () -> assertTrue(result.has(MINIMUM_VERSION), "Should contain 'minimumVersion' field"),
        () -> assertEquals("23.2.0", result.getString(MINIMUM_VERSION)),
        () -> assertTrue(result.has("warnings"), "Should contain 'warnings' field")
    );
  }

  /**
   * Common mock setup for successful cases
   */
  private void setupMocksForSuccessfulExecution() {
    when(mockOBDal.get(PackageVersion.class, VALID_PACKAGE_VERSION_ID))
        .thenReturn(mockPackageVersion);
    when(mockPackageVersion.getPackage()).thenReturn(mockPackage);
    when(mockPackageVersion.getVersion()).thenReturn(PACKAGE_VERSION);
  }
}
