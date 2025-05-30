package com.etendoerp.dependencymanager;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.ComponentProvider;

/**
 * Comprehensive unit tests for DependencyManagerComponentProvider class.
 * <p>
 * This test class demonstrates:
 * - Proper mocking of inherited behavior from BaseComponentProvider
 * - Testing static constants and their values
 * - Exception handling scenarios
 * - Inner class behavior testing through reflection
 * - Proper setup and teardown of test environment
 * <p>
 * Compatible with JUnit 5 and Mockito-Core 5.0
 */
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("DependencyManagerComponentProvider Unit Tests")
class DependencyManagerComponentProviderTest {

  @InjectMocks
  private DependencyManagerComponentProvider componentProvider;


  private MockedStatic<ComponentProvider> mockedComponentProvider;

  private static final String EXPECTED_COMPONENT_QUALIFIER = "ETDEP_DependencyManagerComponentProvider";
  private static final String[] EXPECTED_JS_FILES = {
      "uninstallDependencyWarnings.js",
      "changeVersionDropdown.js",
      "changeFormat.js",
      "dependenciesStatusField.js",
      "addDependency.js"
  };
  private static final String EXPECTED_JS_PREFIX = "web/com.etendoerp.dependencymanager/js/";

  /**
   * Sets up the test environment before each test.
   */
  @BeforeEach
  void setUp() {
    componentProvider = spy(new DependencyManagerComponentProvider());
  }

  /**
   * Cleans up the test environment after each test.
   */
  @AfterEach
  void tearDown() {
    if (mockedComponentProvider != null) {
      mockedComponentProvider.close();
    }
  }

  /**
   * Validates that the component qualifier constant matches the expected value.
   *
   * @throws Exception
   *     if reflection fails to access the field.
   */
  @Test
  @DisplayName("Should have correct component qualifier constant")
  void shouldHaveCorrectComponentQualifier() throws Exception {
    Field componentField = DependencyManagerComponentProvider.class
        .getDeclaredField("ETDEP_COMPONENT");
    componentField.setAccessible(true);

    String actualQualifier = (String) componentField.get(null);

    assertEquals(EXPECTED_COMPONENT_QUALIFIER, actualQualifier,
        "Component qualifier should match expected value");
  }

  /**
   * Validates that the JavaScript files array matches the expected values.
   *
   * @throws Exception
   *     if reflection fails to access the field.
   */
  @Test
  @DisplayName("Should have correct JavaScript files array")
  void shouldHaveCorrectJavaScriptFiles() throws Exception {
    Field jsFilesField = DependencyManagerComponentProvider.class
        .getDeclaredField("JS_FILES");
    jsFilesField.setAccessible(true);

    String[] actualJsFiles = (String[]) jsFilesField.get(null);

    assertAll("JavaScript files validation",
        () -> assertNotNull(actualJsFiles, "JS files array should not be null"),
        () -> assertEquals(EXPECTED_JS_FILES.length, actualJsFiles.length,
            "Should have correct number of JS files"),
        () -> {
          for (int i = 0; i < EXPECTED_JS_FILES.length; i++) {
            assertEquals(EXPECTED_JS_FILES[i], actualJsFiles[i],
                String.format("JS file at index %d should match expected value", i));
          }
        }
    );
  }

  /**
   * Ensures that an `IllegalArgumentException` is thrown for unsupported component IDs.
   */
  @Test
  @DisplayName("Should throw IllegalArgumentException for unsupported component ID")
  void shouldThrowExceptionForUnsupportedComponentId() {
    String invalidComponentId = "INVALID_COMPONENT";
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("testParam", "testValue");

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> componentProvider.getComponent(invalidComponentId, parameters),
        "Should throw IllegalArgumentException for unsupported component ID"
    );

    assertAll("Exception validation",
        () -> assertTrue(exception.getMessage().contains(invalidComponentId),
            "Exception message should contain the invalid component ID"),
        () -> assertTrue(exception.getMessage().contains("not supported"),
            "Exception message should indicate component is not supported")
    );
  }

  /**
   * Ensures that an `IllegalArgumentException` is thrown for null component IDs.
   */
  @Test
  @DisplayName("Should throw IllegalArgumentException for null component ID")
  void shouldThrowExceptionForNullComponentId() {
    String nullComponentId = null;
    Map<String, Object> parameters = new HashMap<>();

    assertThrows(
        IllegalArgumentException.class,
        () -> componentProvider.getComponent(nullComponentId, parameters),
        "Should throw IllegalArgumentException for null component ID"
    );
  }

  /**
   * Ensures that an `IllegalArgumentException` is thrown for empty component IDs.
   */
  @Test
  @DisplayName("Should throw IllegalArgumentException for empty component ID")
  void shouldThrowExceptionForEmptyComponentId() {
    String emptyComponentId = "";
    Map<String, Object> parameters = new HashMap<>();

    assertThrows(
        IllegalArgumentException.class,
        () -> componentProvider.getComponent(emptyComponentId, parameters),
        "Should throw IllegalArgumentException for empty component ID"
    );
  }

  /**
   * Validates the behavior of the public interface, including resource generation.
   */
  @Test
  @DisplayName("Should test behavior through public interface")
  void shouldTestBehaviorThroughPublicInterface() {
    List<BaseComponentProvider.ComponentResource> resources =
        componentProvider.getGlobalComponentResources();

    assertAll("Public behavior validation",
        () -> assertNotNull(resources),
        () -> assertEquals(EXPECTED_JS_FILES.length, resources.size()),
        () -> {
          for (int i = 0; i < resources.size(); i++) {
            BaseComponentProvider.ComponentResource resource = resources.get(i);
            String expectedPath = EXPECTED_JS_PREFIX + EXPECTED_JS_FILES[i];

            assertAll("Resource " + i + " validation",
                () -> assertEquals(expectedPath, resource.getPath()),
                () -> assertEquals(BaseComponentProvider.ComponentResource.ComponentResourceType.Static,
                    resource.getType()),
                () -> assertTrue(resource.isValidForApp("OB3"))
            );
          }
        }
    );
  }

  /**
   * Ensures that null parameters are handled gracefully and still result in an exception.
   */
  @Test
  @DisplayName("Should handle null parameters map gracefully")
  void shouldHandleNullParametersMapGracefully() {
    String componentId = "TEST_COMPONENT";
    Map<String, Object> nullParameters = null;

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> componentProvider.getComponent(componentId, nullParameters),
        "Should handle null parameters and still validate component ID"
    );

    assertTrue(exception.getMessage().contains("not supported"),
        "Exception should indicate component is not supported");
  }

  /**
   * Verifies that the `ComponentProvider.Qualifier` annotation is present and correct.
   */
  @Test
  @DisplayName("Should verify annotation presence on class")
  void shouldVerifyAnnotationPresenceOnClass() {
    Class<DependencyManagerComponentProvider> clazz = DependencyManagerComponentProvider.class;

    assertAll("Annotation validation",
        () -> assertTrue(clazz.isAnnotationPresent(ComponentProvider.Qualifier.class),
            "Class should have ComponentProvider.Qualifier annotation"),
        () -> {
          ComponentProvider.Qualifier qualifier =
              clazz.getAnnotation(ComponentProvider.Qualifier.class);
          assertEquals(EXPECTED_COMPONENT_QUALIFIER, qualifier.value(),
              "Qualifier annotation should have correct value");
        }
    );
  }

}
