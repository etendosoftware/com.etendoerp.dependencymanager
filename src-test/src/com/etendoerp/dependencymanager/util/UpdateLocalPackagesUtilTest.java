package com.etendoerp.dependencymanager.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.xml.XMLUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the `UpdateLocalPackagesUtil` class.
 * <p>
 * This test suite validates the behavior of the utility class responsible for updating
 * local packages. It includes tests for exception handling, admin mode management, and
 * integration with mocked dependencies.
 * </p>
 * <ul>
 *   <li>Ensures proper handling of `IOException` during file download.</li>
 *   <li>Validates correct admin mode handling, even in exceptional cases.</li>
 *   <li>Mocks static dependencies to isolate test behavior.</li>
 * </ul>
 *
 * <p>Ensures that the utility behaves as expected under various scenarios.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateLocalPackagesUtil Tests")
class UpdateLocalPackagesUtilTest {

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBPropertiesProvider mockPropertiesProvider;

  @Mock
  private Properties mockProperties;

  @Mock
  private XMLUtil mockXMLUtil;

  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBPropertiesProvider> mockedPropertiesProvider;
  private MockedStatic<XMLUtil> mockedXMLUtil;

  /**
   * Sets up the mocked static dependencies and initializes the test environment.
   */
  @BeforeEach
  void setUp() {
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBDal = mockStatic(OBDal.class);
    mockedPropertiesProvider = mockStatic(OBPropertiesProvider.class);
    mockedXMLUtil = mockStatic(XMLUtil.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedPropertiesProvider.when(OBPropertiesProvider::getInstance).thenReturn(mockPropertiesProvider);
    mockedXMLUtil.when(XMLUtil::getInstance).thenReturn(mockXMLUtil);

    when(mockPropertiesProvider.getOpenbravoProperties()).thenReturn(mockProperties);
  }

  /**
   * Closes the mocked static dependencies and cleans up the test environment.
   */
  @AfterEach
  void tearDown() {
    mockedOBContext.close();
    mockedOBDal.close();
    mockedPropertiesProvider.close();
    mockedXMLUtil.close();
  }

  /**
   * Validates that an `IOException` is handled correctly during file download.
   */
  @Test
  @DisplayName("Should handle IOException during file download")
  void testUpdateWithIOException() {
    when(mockProperties.getProperty("branch.update.local.packages", "main"))
        .thenReturn("main");

    when(mockXMLUtil.getRootElement(any(InputStream.class)))
        .thenThrow(new RuntimeException("XML parsing error"));

    IOException exception = assertThrows(IOException.class,
        UpdateLocalPackagesUtil::update);

    assertAll(
        () -> assertNotNull(exception),
        () -> assertEquals("Error when updating packages", exception.getMessage()),
        () -> assertNotNull(exception.getCause())
    );

    verifyAdminModeHandling();
  }

  /**
   * Validates that admin mode is handled correctly, even when an exception occurs.
   */
  @Test
  @DisplayName("Should handle admin mode correctly even on exception")
  void testAdminModeHandlingOnException() {
    when(mockProperties.getProperty("branch.update.local.packages", "main"))
        .thenReturn("main");

    when(mockXMLUtil.getRootElement(any(InputStream.class)))
        .thenThrow(new RuntimeException("Test exception"));

    assertThrows(IOException.class, UpdateLocalPackagesUtil::update);

    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(OBContext::restorePreviousMode);
  }

  /**
   * Verifies that admin mode is set and restored correctly.
   */
  private void verifyAdminModeHandling() {
    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(OBContext::restorePreviousMode);
  }

}
