package com.etendoerp.dependencymanager.util;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import static com.etendoerp.dependencymanager.DependencyManagerTestConstants.RESULT_NOT_NULL_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ChangeFormatUtil class.
 * Tests cover all public methods and edge cases, using Mockito for static method mocking.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeFormatUtil Tests")
class ChangeFormatUtilTest {

  @Mock
  private DalConnectionProvider mockConnectionProvider;
  @Mock
  private VariablesSecureApp mockVariablesSecureApp;
  @Mock
  private ComboTableData mockComboTableData;
  @Mock
  private FieldProvider mockFieldProvider1;
  @Mock
  private FieldProvider mockFieldProvider2;

  private MockedStatic<Utility> utilityMockedStatic;

  private static final String VALIDATION_RULE = "testValidationRule";
  private static final String ACCESS_ORG_TREE = "#AccessibleOrgTree";
  private static final String USER_CLIENT = "#User_Client";
  private static final String CONTEXT_VALUE = "testContextValue";

  /**
   * Sets up the test environment before each test.
   * Mocks static methods and initializes test data.
   */
  @BeforeEach
  void setUp() {
    utilityMockedStatic = mockStatic(Utility.class);

    utilityMockedStatic.when(() -> Utility.getContext(
        eq(mockConnectionProvider),
        eq(mockVariablesSecureApp),
        eq(ACCESS_ORG_TREE),
        eq(ChangeFormatUtil.class.getName())
    )).thenReturn(CONTEXT_VALUE);

    utilityMockedStatic.when(() -> Utility.getContext(
        eq(mockConnectionProvider),
        eq(mockVariablesSecureApp),
        eq(USER_CLIENT),
        eq(ChangeFormatUtil.class.getName())
    )).thenReturn(CONTEXT_VALUE);

    utilityMockedStatic.when(() -> Utility.fillSQLParameters(
        eq(mockConnectionProvider),
        eq(mockVariablesSecureApp),
        isNull(),
        eq(mockComboTableData),
        eq(ChangeFormatUtil.class.getName()),
        eq("")
    )).thenAnswer(invocation -> null);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes mocked static utilities.
   */
  @AfterEach
  void tearDown() {
    if (utilityMockedStatic != null) {
      utilityMockedStatic.close();
    }
  }

  /**
   * Nested test class for `getNewFormatList` method.
   */
  @Nested
  @DisplayName("getNewFormatList Tests")
  class GetNewFormatListTests {

    /**
     * Tests that the `getNewFormatList` method returns a list of format IDs
     * extracted from the provided field providers.
     * Validates the size and content of the returned list.
     */
    @Test
    @DisplayName("Should return list of format IDs from field providers")
    void shouldReturnFormatListFromFieldProviders() {
      String currentFormat = DependencyUtil.FORMAT_LOCAL;
      String expectedId1 = DependencyUtil.FORMAT_SOURCE;
      String expectedId2 = DependencyUtil.FORMAT_JAR;

      when(mockFieldProvider1.getField(ChangeFormatUtil.FORMAT_KEY_ID)).thenReturn(expectedId1);
      when(mockFieldProvider2.getField(ChangeFormatUtil.FORMAT_KEY_ID)).thenReturn(expectedId2);

      FieldProvider[] mockFieldProviders = { mockFieldProvider1, mockFieldProvider2 };

      try (var ignored = mockConstruction(ComboTableData.class,
          (mock, context) -> when(mock.select(false)).thenReturn(mockFieldProviders))) {

        List<String> result = ChangeFormatUtil.getNewFormatList(
            currentFormat, VALIDATION_RULE, mockVariablesSecureApp, mockConnectionProvider);

        assertAll("Format list validation",
            () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
            () -> assertEquals(2, result.size(), "Should return 2 format options"),
            () -> assertTrue(result.contains(expectedId1), "Should contain FORMAT_SOURCE"),
            () -> assertTrue(result.contains(expectedId2), "Should contain FORMAT_JAR")
        );
      }
    }

    /**
     * Tests that the `getNewFormatList` method returns an empty list
     * when no field providers are available.
     */
    @Test
    @DisplayName("Should return empty list when no field providers available")
    void shouldReturnEmptyListWhenNoFieldProviders() {
      String currentFormat = DependencyUtil.FORMAT_LOCAL;
      FieldProvider[] emptyFieldProviders = { };

      try (var ignored = mockConstruction(ComboTableData.class,
          (mock, context) -> when(mock.select(false)).thenReturn(emptyFieldProviders))) {

        List<String> result = ChangeFormatUtil.getNewFormatList(
            currentFormat, VALIDATION_RULE, mockVariablesSecureApp, mockConnectionProvider);

        assertAll("Empty list validation",
            () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
            () -> assertTrue(result.isEmpty(), "Should return empty list")
        );
      }
    }

    /**
     * Tests that the `getNewFormatList` method handles exceptions gracefully
     * and returns an empty list when an error occurs.
     */
    @Test
    @DisplayName("Should handle exception gracefully and return empty list")
    void shouldHandleExceptionGracefully() {
      // Given
      String currentFormat = DependencyUtil.FORMAT_LOCAL;

      try (var ignored = mockConstruction(ComboTableData.class,
          (mock, context) -> when(mock.select(false)).thenThrow(new RuntimeException("Database error")))) {

        List<String> result = ChangeFormatUtil.getNewFormatList(
            currentFormat, VALIDATION_RULE, mockVariablesSecureApp, mockConnectionProvider);

        assertAll("Exception handling validation",
            () -> assertNotNull(result, "Result should not be null even when exception occurs"),
            () -> assertTrue(result.isEmpty(), "Should return empty list when exception occurs")
        );
      }
    }
  }

  /**
   * Nested test class for `getNewFormatCombo` method.
   */
  @Nested
  @DisplayName("getNewFormatCombo Tests")
  class GetNewFormatComboTests {

    /**
     * Tests that the `getNewFormatCombo` method returns an empty array
     * when an invalid format is provided.
     */
    @Test
    @DisplayName("Should return empty array for invalid format")
    void shouldReturnEmptyArrayForInvalidFormat() {
      String invalidFormat = "INVALID_FORMAT";

      try (var ignored = mockConstruction(ComboTableData.class)) {
        FieldProvider[] result = ChangeFormatUtil.getNewFormatCombo(
            mockConnectionProvider, mockVariablesSecureApp, VALIDATION_RULE, invalidFormat);

        assertAll("Invalid format validation",
            () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
            () -> assertEquals(0, result.length, "Should return empty array for invalid format")
        );
      }
    }

    /**
     * Tests that the `getNewFormatCombo` method handles exceptions
     * in the `ComboTableData` class gracefully and returns an empty array.
     */
    @Test
    @DisplayName("Should handle ComboTableData exception and return empty array")
    void shouldHandleComboTableDataException() {
      String currentFormat = DependencyUtil.FORMAT_LOCAL;

      try (var ignored = mockConstruction(ComboTableData.class,
          (mock, context) -> when(mock.select(false)).thenThrow(new RuntimeException("ComboTableData error")))) {

        FieldProvider[] result = ChangeFormatUtil.getNewFormatCombo(
            mockConnectionProvider, mockVariablesSecureApp, VALIDATION_RULE, currentFormat);

        assertAll("Exception handling validation",
            () -> assertNotNull(result, "Result should not be null even when exception occurs"),
            () -> assertEquals(0, result.length, "Should return empty array when exception occurs")
        );
      }
    }

    /**
     * Tests that the `getNewFormatCombo` method handles null combo options
     * gracefully and returns an empty array.
     */
    @Test
    @DisplayName("Should handle null combo options gracefully")
    void shouldHandleNullComboOptions() {
      String currentFormat = DependencyUtil.FORMAT_LOCAL;

      try (var ignored = mockConstruction(ComboTableData.class,
          (mock, context) -> when(mock.select(false)).thenReturn(null))) {

        FieldProvider[] result = ChangeFormatUtil.getNewFormatCombo(
            mockConnectionProvider, mockVariablesSecureApp, VALIDATION_RULE, currentFormat);

        assertAll("Null combo options validation",
            () -> assertNotNull(result, RESULT_NOT_NULL_MESSAGE),
            () -> assertEquals(0, result.length, "Should return empty array when combo options is null")
        );
      }
    }

  }

  /**
   * Nested test class for edge cases and integration tests.
   */
  @Nested
  @DisplayName("Edge Cases and Integration Tests")
  class EdgeCasesAndIntegrationTests {

    /**
     * Tests that the `getNewFormatCombo` method verifies the parameters
     * passed to the `ComboTableData` constructor.
     */
    @Test
    @DisplayName("Should verify ComboTableData constructor parameters")
    void shouldVerifyComboTableDataConstructorParameters() {
      String currentFormat = DependencyUtil.FORMAT_LOCAL;

      try (var ignored = mockConstruction(ComboTableData.class,
          (mock, context) -> {
            when(mock.select(false)).thenReturn(new FieldProvider[0]);

            // Verify constructor arguments
            assertEquals(mockVariablesSecureApp, context.arguments().get(0));
            assertEquals(mockConnectionProvider, context.arguments().get(1));
            assertEquals("LIST", context.arguments().get(2));
            assertEquals(ChangeFormatUtil.NEW_FORMAT_PARAM, context.arguments().get(3));
            assertEquals("02B96BF686064B44BB33C70C43AEFC05", context.arguments().get(4));
            assertEquals(VALIDATION_RULE, context.arguments().get(5));
            assertEquals(CONTEXT_VALUE, context.arguments().get(6));
            assertEquals(CONTEXT_VALUE, context.arguments().get(7));
            assertEquals(0, context.arguments().get(8));
          })) {

        ChangeFormatUtil.getNewFormatCombo(
            mockConnectionProvider, mockVariablesSecureApp, VALIDATION_RULE, currentFormat);

      }
    }

    /**
     * Tests that the `getNewFormatCombo` method verifies the calls
     * to static methods in the `Utility` class.
     */
    @Test
    @DisplayName("Should verify Utility static method calls")
    void shouldVerifyUtilityStaticMethodCalls() {
      String currentFormat = DependencyUtil.FORMAT_LOCAL;

      try (var ignored = mockConstruction(ComboTableData.class,
          (mock, context) -> when(mock.select(false)).thenReturn(new FieldProvider[0]))) {

        ChangeFormatUtil.getNewFormatCombo(
            mockConnectionProvider, mockVariablesSecureApp, VALIDATION_RULE, currentFormat);

        utilityMockedStatic.verify(() -> Utility.getContext(
            eq(mockConnectionProvider),
            eq(mockVariablesSecureApp),
            eq(ACCESS_ORG_TREE),
            eq(ChangeFormatUtil.class.getName())
        ), times(1));

        utilityMockedStatic.verify(() -> Utility.getContext(
            eq(mockConnectionProvider),
            eq(mockVariablesSecureApp),
            eq(USER_CLIENT),
            eq(ChangeFormatUtil.class.getName())
        ), times(1));

        utilityMockedStatic.verify(() -> Utility.fillSQLParameters(
            eq(mockConnectionProvider),
            eq(mockVariablesSecureApp),
            isNull(),
            any(ComboTableData.class),
            eq(ChangeFormatUtil.class.getName()),
            eq("")
        ), times(1));
      }
    }
  }
}
