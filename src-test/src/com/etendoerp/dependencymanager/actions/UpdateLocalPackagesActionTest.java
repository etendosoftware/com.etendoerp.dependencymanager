package com.etendoerp.dependencymanager.actions;

import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.PACKAGES_UPDATED_SUCCESSFULLY;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.PACKAGE_UPDATE_SUCCESS_CODE;
import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.RESULT_NOT_NULL_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.etendoerp.dependencymanager.util.UpdateLocalPackagesUtil;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Unit tests for UpdateLocalPackagesAction class.
 * This test class covers the main functionality of the action including:
 * - Successful package update scenarios
 * - Error handling scenarios
 * - Input class validation
 * - Mock management for static dependencies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateLocalPackagesAction Tests")
class UpdateLocalPackagesActionTest {

  @InjectMocks
  private UpdateLocalPackagesAction updateLocalPackagesAction;

  private MockedStatic<UpdateLocalPackagesUtil> mockedUpdateUtil;
  private MockedStatic<OBMessageUtils> mockedMessageUtils;

  private JSONObject testParameters;
  private MutableBoolean testIsStopped;

  /**
   * Sets up the required static mocks and test objects before each test execution.
   * Ensures that the static dependencies and test parameters are initialized,
   * providing a clean and isolated environment for every test case.
   */
  @BeforeEach
  void setUp() {
    mockedUpdateUtil = mockStatic(UpdateLocalPackagesUtil.class);
    mockedMessageUtils = mockStatic(OBMessageUtils.class);

    testParameters = new JSONObject();
    testIsStopped = new MutableBoolean(false);
  }

  /**
   * Releases and closes all static mocks after each test execution.
   * Ensures that no static mock state leaks between tests, maintaining test isolation.
   */
  @AfterEach
  void tearDown() {
    if (mockedUpdateUtil != null) {
      mockedUpdateUtil.close();
    }
    if (mockedMessageUtils != null) {
      mockedMessageUtils.close();
    }
  }

  /**
   * Tests that the action method returns SUCCESS when the package update completes successfully.
   * Verifies that the correct success message and result type are returned.
   */
  @Test
  @DisplayName("Should return SUCCESS when package update completes successfully")
  void testActionSuccessfulUpdate() {
    String expectedSuccessMessage = PACKAGES_UPDATED_SUCCESSFULLY;

    mockedUpdateUtil.when(UpdateLocalPackagesUtil::update)
        .then(invocation -> null); // void method returns null

    mockedMessageUtils.when(() -> OBMessageUtils.messageBD(PACKAGE_UPDATE_SUCCESS_CODE))
        .thenReturn(expectedSuccessMessage);

    ActionResult result = updateLocalPackagesAction.action(testParameters, testIsStopped);

    assertAll("Successful update result validation",
        () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
        () -> assertEquals(Result.Type.SUCCESS, result.getType(),
            "Result type should be SUCCESS"),
        () -> assertEquals(expectedSuccessMessage, result.getMessage(),
            "Success message should match expected value")
    );

    mockedUpdateUtil.verify(UpdateLocalPackagesUtil::update, times(1));
    mockedMessageUtils.verify(() -> OBMessageUtils.messageBD(PACKAGE_UPDATE_SUCCESS_CODE),
        times(1));
  }

  /**
   * Tests that the action method returns ERROR when a RuntimeException is thrown during update.
   * Verifies that the error message matches the exception message and the result type is ERROR.
   */
  @Test
  @DisplayName("Should return ERROR when package update throws RuntimeException")
  void testActionRuntimeExceptionHandling() {
    String expectedErrorMessage = "Runtime error occurred during update";
    RuntimeException testException = new RuntimeException(expectedErrorMessage);

    mockedUpdateUtil.when(UpdateLocalPackagesUtil::update)
        .thenThrow(testException);

    ActionResult result = updateLocalPackagesAction.action(testParameters, testIsStopped);

    assertAll("Runtime exception handling validation",
        () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
        () -> assertEquals(Result.Type.ERROR, result.getType(),
            "Result type should be ERROR"),
        () -> assertEquals(expectedErrorMessage, result.getMessage(),
            "Error message should match exception message")
    );

    mockedUpdateUtil.verify(UpdateLocalPackagesUtil::update, times(1));
    mockedMessageUtils.verify(() -> OBMessageUtils.messageBD(PACKAGE_UPDATE_SUCCESS_CODE),
        never());
  }

  /**
   * Tests that the getInputClass method returns the correct class type (Package).
   */
  @Test
  @DisplayName("Should return correct input class")
  void testGetInputClassReturnsCorrectClass() {
    Class<?> inputClass = updateLocalPackagesAction.getInputClass();

    assertEquals(com.etendoerp.dependencymanager.data.Package.class, inputClass,
        "Input class should be Package class");
  }

  /**
   * Tests that the action method handles null parameters without throwing exceptions.
   * Verifies that the result is SUCCESS and the correct message is returned.
   */
  @Test
  @DisplayName("Should handle null parameters without throwing exception")
  void testActionNullParametersHandling() {
    String expectedSuccessMessage = PACKAGES_UPDATED_SUCCESSFULLY;

    mockedUpdateUtil.when(UpdateLocalPackagesUtil::update)
        .then(invocation -> null);

    mockedMessageUtils.when(() -> OBMessageUtils.messageBD(PACKAGE_UPDATE_SUCCESS_CODE))
        .thenReturn(expectedSuccessMessage);

    ActionResult result = updateLocalPackagesAction.action(null, testIsStopped);

    assertAll("Null parameters handling validation",
        () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
        () -> assertEquals(Result.Type.SUCCESS, result.getType(),
            "Result type should be SUCCESS even with null parameters"),
        () -> assertEquals(expectedSuccessMessage, result.getMessage(),
            "Success message should be returned normally")
    );
  }

  /**
   * Tests that the action method handles a null isStopped parameter without throwing exceptions.
   * Verifies that the result is SUCCESS and the correct message is returned.
   */
  @Test
  @DisplayName("Should handle null isStopped parameter without throwing exception")
  void testActionNullIsStoppedHandling() {
    String expectedSuccessMessage = PACKAGES_UPDATED_SUCCESSFULLY;

    mockedUpdateUtil.when(UpdateLocalPackagesUtil::update)
        .then(invocation -> null);

    mockedMessageUtils.when(() -> OBMessageUtils.messageBD(PACKAGE_UPDATE_SUCCESS_CODE))
        .thenReturn(expectedSuccessMessage);

    ActionResult result = updateLocalPackagesAction.action(testParameters, null);

    assertAll("Null isStopped handling validation",
        () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
        () -> assertEquals(Result.Type.SUCCESS, result.getType(),
            "Result type should be SUCCESS even with null isStopped"),
        () -> assertEquals(expectedSuccessMessage, result.getMessage(),
            "Success message should be returned normally")
    );
  }

  /**
   * Tests that the method signatures of action and getInputClass are compatible and callable.
   * Ensures that no exceptions are thrown when calling these methods.
   */
  @Test
  @DisplayName("Should maintain method signature compatibility")
  void testMethodSignatures() {
    // This test ensures that the action method has the correct signature
    // and can be called without compilation errors

    assertDoesNotThrow(() -> {
      ActionResult result = updateLocalPackagesAction.action(testParameters, testIsStopped);
      assertNotNull(result);
    }, "Action method should be callable with correct parameters");

    assertDoesNotThrow(() -> {
      Class<?> inputClass = updateLocalPackagesAction.getInputClass();
      assertNotNull(inputClass);
    }, "GetInputClass method should be callable without parameters");
  }
}
