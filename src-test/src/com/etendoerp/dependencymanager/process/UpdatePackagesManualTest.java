package com.etendoerp.dependencymanager.process;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DbUtility;

/**
 * Unit tests for UpdatePagackesManual class
 * Tests cover success scenarios, exception handling, and message formatting
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdatePackagesManual Tests")
class UpdatePackagesManualTest {

  private UpdatePagackesManual processHandler;

  @Mock
  private OBDal mockOBDal;

  private MockedStatic<OBDal> obdalMockedStatic;
  private MockedStatic<OBMessageUtils> obMessageUtilsMockedStatic;
  private MockedStatic<DbUtility> dbUtilityMockedStatic;

  private static final String SUCCESS_MESSAGE = "Process completed successfully";
  private static final String ERROR_MESSAGE = "An error occurred";
  private static final String SQL_ERROR_MESSAGE = "Database error";
  private static final String TRANSLATED_ERROR = "Translated error message";

  /**
   * Initializes the required objects and mocks before each test execution.
   * Ensures that static mocks and the `processHandler` instance are properly set up.
   */
  @BeforeEach
  void setUp() {
    processHandler = new UpdatePagackesManual();

    obdalMockedStatic = mockStatic(OBDal.class);
    obMessageUtilsMockedStatic = mockStatic(OBMessageUtils.class);
    dbUtilityMockedStatic = mockStatic(DbUtility.class);

    obdalMockedStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
    obMessageUtilsMockedStatic.when(() -> OBMessageUtils.messageBD("ProcessOK"))
        .thenReturn(SUCCESS_MESSAGE);
    obMessageUtilsMockedStatic.when(() -> OBMessageUtils.messageBD("success"))
        .thenReturn("Success");
  }

  /**
   * Cleans up the static mocks after each test execution.
   * Ensures that no static mock remains active, preventing side effects between tests.
   */
  @AfterEach
  void tearDown() {
    if (obdalMockedStatic != null) {
      obdalMockedStatic.close();
    }
    if (obMessageUtilsMockedStatic != null) {
      obMessageUtilsMockedStatic.close();
    }
    if (dbUtilityMockedStatic != null) {
      dbUtilityMockedStatic.close();
    }
  }

  /**
   * Tests that the `doExecute` method returns a success message when the process executes successfully.
   *
   * @throws Exception
   *     if any error occurs during the test execution.
   */
  @Test
  @DisplayName("Should return success message when process executes successfully")
  void testDoExecuteSuccess() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String data = "test-data";

    try (MockedConstruction<GetPackagesFromRepositories> mockConstruction =
             mockConstruction(GetPackagesFromRepositories.class,
                 (mock, context) -> doNothing().when(mock).doExecute(null))) {

      JSONObject result = processHandler.doExecute(parameters, data);

      assertAll("Success response validation",
          () -> assertNotNull(result, "Result should not be null"),
          () -> assertTrue(result.has("responseActions"), "Should contain responseActions"),
          () -> {
            JSONArray actions = result.getJSONArray("responseActions");
            assertEquals(2, actions.length(), "Should have 2 response actions");
          },
          () -> {
            JSONArray actions = result.getJSONArray("responseActions");
            JSONObject messageAction = actions.getJSONObject(0);
            assertTrue(messageAction.has("showMsgInProcessView"),
                "First action should be showMsgInProcessView");

            JSONObject message = messageAction.getJSONObject("showMsgInProcessView");
            assertEquals("success", message.getString("msgType"),
                "Message type should be success");
            assertEquals(SUCCESS_MESSAGE, message.getString("msgText"),
                "Message text should match expected");
          },
          () -> {
            JSONArray actions = result.getJSONArray("responseActions");
            JSONObject refreshAction = actions.getJSONObject(1);
            assertTrue(refreshAction.has("refreshGrid"),
                "Second action should be refreshGrid");
          }
      );

      List<GetPackagesFromRepositories> constructed = mockConstruction.constructed();
      assertEquals(1, constructed.size(), "Should construct exactly one instance");
      verify(constructed.get(0)).doExecute(null);
      verifyNoInteractions(mockOBDal);
    }
  }

  /**
   * Tests that the `doExecute` method handles exceptions and returns an error message with a retry option.
   *
   * @throws Exception
   *     if any error occurs during the test execution.
   */
  @Test
  @DisplayName("Should handle exception and return error message with retry")
  void testDoExecuteExceptionHandling() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String data = "test-data";

    RuntimeException testException = new RuntimeException(ERROR_MESSAGE);
    SQLException sqlException = new SQLException(SQL_ERROR_MESSAGE);
    OBError obError = new OBError();
    obError.setMessage(TRANSLATED_ERROR);

    try (MockedConstruction<GetPackagesFromRepositories> mockConstruction =
             mockConstruction(GetPackagesFromRepositories.class,
                 (mock, context) -> doThrow(testException).when(mock).doExecute(null))) {

      dbUtilityMockedStatic.when(() -> DbUtility.getUnderlyingSQLException(testException))
          .thenReturn(sqlException);
      obMessageUtilsMockedStatic.when(() -> OBMessageUtils.translateError(SQL_ERROR_MESSAGE))
          .thenReturn(obError);

      JSONObject result = processHandler.doExecute(parameters, data);

      assertAll("Error response validation",
          () -> assertNotNull(result, "Result should not be null"),
          () -> assertTrue(result.has("message"), "Should contain error message"),
          () -> assertTrue(result.has("retryExecution"), "Should allow retry execution"),
          () -> assertTrue(result.getBoolean("retryExecution"),
              "Retry execution should be true"),
          () -> {
            JSONObject message = result.getJSONObject("message");
            assertEquals("error", message.getString("severity"),
                "Message severity should be error");
            assertEquals(TRANSLATED_ERROR, message.getString("text"),
                "Message text should match translated error");
          }
      );

      List<GetPackagesFromRepositories> constructed = mockConstruction.constructed();
      assertEquals(1, constructed.size(), "Should construct exactly one instance");
      verify(constructed.get(0)).doExecute(null);

      verify(mockOBDal).rollbackAndClose();
    }
  }

  /**
   * Tests that the `getSuccessMessage` method creates a proper success message structure.
   *
   * @throws Exception
   *     if reflection or JSON processing fails.
   */
  @Test
  @DisplayName("Should create proper success message structure")
  void testGetSuccessMessage() throws Exception {
    Method getSuccessMessageMethod = UpdatePagackesManual.class
        .getDeclaredMethod("getSuccessMessage", String.class);
    getSuccessMessageMethod.setAccessible(true);

    String testMessage = "Test success message";

    obMessageUtilsMockedStatic.when(() -> OBMessageUtils.messageBD("success"))
        .thenReturn("Success");

    JSONObject result = (JSONObject) getSuccessMessageMethod.invoke(null, testMessage);

    assertAll("Success message structure",
        () -> assertNotNull(result, "Result should not be null"),
        () -> assertTrue(result.has("responseActions"), "Should have responseActions"),
        () -> {
          JSONArray actions = result.getJSONArray("responseActions");
          assertEquals(2, actions.length(), "Should have exactly 2 actions");

          JSONObject messageAction = actions.getJSONObject(0);
          assertTrue(messageAction.has("showMsgInProcessView"));

          JSONObject msgInBPTab = messageAction.getJSONObject("showMsgInProcessView");
          assertEquals("success", msgInBPTab.getString("msgType"));
          assertEquals("Success", msgInBPTab.getString("msgTitle"));
          assertEquals(testMessage, msgInBPTab.getString("msgText"));

          JSONObject refreshAction = actions.getJSONObject(1);
          assertTrue(refreshAction.has("refreshGrid"));
        }
    );
  }

  /**
   * Tests that the `getErrorMessage` method creates a proper error message structure.
   *
   * @throws Exception
   *     if reflection or JSON processing fails.
   */
  @Test
  @DisplayName("Should create proper error message structure")
  void testGetErrorMessage() throws Exception {
    Method getErrorMessageMethod = UpdatePagackesManual.class
        .getDeclaredMethod("getErrorMessage", String.class);
    getErrorMessageMethod.setAccessible(true);

    String testErrorMessage = "Test error message";

    JSONObject result = (JSONObject) getErrorMessageMethod.invoke(null, testErrorMessage);

    assertAll("Error message structure",
        () -> assertNotNull(result, "Result should not be null"),
        () -> assertTrue(result.has("message"), "Should contain message object"),
        () -> assertTrue(result.has("retryExecution"), "Should contain retryExecution flag"),
        () -> assertTrue(result.getBoolean("retryExecution"),
            "RetryExecution should be true"),
        () -> {
          JSONObject message = result.getJSONObject("message");
          assertEquals("error", message.getString("severity"),
              "Severity should be error");
          assertEquals(testErrorMessage, message.getString("text"),
              "Message text should match input");
        }
    );
  }

  /**
   * Tests that the `getSuccessMessage` method handles JSON exceptions gracefully.
   *
   * @throws Exception
   *     if reflection or JSON processing fails.
   */
  @Test
  @DisplayName("Should handle JSON exception in success message creation")
  void testGetSuccessMessageJSONException() throws Exception {

    Method getSuccessMessageMethod = UpdatePagackesManual.class
        .getDeclaredMethod("getSuccessMessage", String.class);
    getSuccessMessageMethod.setAccessible(true);

    obMessageUtilsMockedStatic.when(() -> OBMessageUtils.messageBD(anyString()))
        .thenThrow(new RuntimeException("JSON creation error"));

    assertDoesNotThrow(() -> {
      JSONObject result = (JSONObject) getSuccessMessageMethod.invoke(null, "test");
      assertNotNull(result, "Should return a JSONObject even on error");
    });
  }

  /**
   * Custom exception class that extends {@link Exception}.
   * Represents an SQL-related error with a specific message.
   */
  private static class SQLException extends Exception {
    /**
     * Constructs a new {@code SQLException} with the specified detail message.
     *
     * @param message
     *     the detail message describing the SQL error.
     */
    public SQLException(String message) {
      super(message);
    }
  }
}
