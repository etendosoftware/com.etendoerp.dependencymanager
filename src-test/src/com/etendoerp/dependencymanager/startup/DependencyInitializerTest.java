package com.etendoerp.dependencymanager.startup;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.etendoerp.dependencymanager.util.UpdateLocalPackagesUtil;

import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBDal;

/**
 * Unit tests for the {@link DependencyInitializer} class.
 * <p>
 * This test suite validates the initialization logic of the dependency initializer,
 * ensuring proper handling of configuration properties and interactions with utility classes.
 * </p>
 * <ul>
 *   <li>Tests behavior when the "no.update.local.packages" property is set to "true".</li>
 *   <li>Tests behavior when the "no.update.local.packages" property is set to "yes".</li>
 *   <li>Tests handling of null properties from the {@link OBPropertiesProvider}.</li>
 * </ul>
 *
 * <p>Ensures that the initialization process behaves as expected under different scenarios.</p>
 */
@ExtendWith(MockitoExtension.class)
class DependencyInitializerTest {

  @Mock
  private OBPropertiesProvider propertiesProvider;

  @Mock
  private Properties properties;

  private DependencyInitializer dependencyInitializer;

  /**
   * Sets up the test environment before each test.
   * Initializes the {@link DependencyInitializer} instance.
   */
  @BeforeEach
  void setUp() {
    dependencyInitializer = new DependencyInitializer();
  }

  /**
   * Tests that the `initialize` method does not run the update process
   * when the "no.update.local.packages" property is set to "true".
   *
   * @throws InterruptedException
   *     if the thread sleep is interrupted.
   */
  @Test
  void testInitializeWhenUpdateLocalPackagesIsTrueShouldNotRunUpdate() throws InterruptedException {
    when(properties.getProperty("no.update.local.packages", "")).thenReturn("true");

    try (MockedStatic<OBPropertiesProvider> propertiesProviderMock = mockStatic(OBPropertiesProvider.class);
         MockedStatic<UpdateLocalPackagesUtil> updateUtilMock = mockStatic(UpdateLocalPackagesUtil.class);
         MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {

      propertiesProviderMock.when(OBPropertiesProvider::getInstance).thenReturn(propertiesProvider);
      when(propertiesProvider.getOpenbravoProperties()).thenReturn(properties);

      dependencyInitializer.initialize();

      Thread.sleep(200);

      updateUtilMock.verify(UpdateLocalPackagesUtil::update, never());
      obDalMock.verify(OBDal::getInstance, never());
    }
  }

  /**
   * Tests that the `initialize` method does not run the update process
   * when the "no.update.local.packages" property is set to "yes".
   *
   * @throws InterruptedException
   *     if the thread sleep is interrupted.
   */
  @Test
  void testInitializeWhenUpdateLocalPackagesIsYesShouldNotRunUpdate() throws InterruptedException {
    when(properties.getProperty("no.update.local.packages", "")).thenReturn("yes");

    try (MockedStatic<OBPropertiesProvider> propertiesProviderMock = mockStatic(OBPropertiesProvider.class);
         MockedStatic<UpdateLocalPackagesUtil> updateUtilMock = mockStatic(UpdateLocalPackagesUtil.class);
         MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {

      propertiesProviderMock.when(OBPropertiesProvider::getInstance).thenReturn(propertiesProvider);
      when(propertiesProvider.getOpenbravoProperties()).thenReturn(properties);

      dependencyInitializer.initialize();

      Thread.sleep(200);

      updateUtilMock.verify(UpdateLocalPackagesUtil::update, never());
      obDalMock.verify(OBDal::getInstance, never());
    }
  }

  /**
   * Tests that the `initialize` method handles a null properties object
   * from the {@link OBPropertiesProvider} gracefully by throwing a {@link NullPointerException}.
   */
  @Test
  void testInitializeWhenPropertiesProviderReturnsNullShouldHandleGracefully() {
    try (MockedStatic<OBPropertiesProvider> propertiesProviderMock = mockStatic(OBPropertiesProvider.class)) {
      propertiesProviderMock.when(OBPropertiesProvider::getInstance).thenReturn(propertiesProvider);
      when(propertiesProvider.getOpenbravoProperties()).thenReturn(null);

      assertThrows(NullPointerException.class, () -> dependencyInitializer.initialize());
    }
  }

}
