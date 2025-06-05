package com.etendoerp.dependencymanager;

import java.util.Arrays;
import java.util.List;

import org.openbravo.base.session.OBPropertiesProvider;

public class DependencyManagerTestConstants {
  public static final String FIRST_VERSION = "1.0.0";
  public static final String FORMAT_JAR = "J";
  public static final String FORMAT_SOURCE = "S";
  public static final String GROUP_COM_ETENDOERP = "com.etendoerp";
  public static final String MODULE_JAR_PGK_1 = "com.etendoerp.module.jar1";
  public static final String MODULE_SOURCE_PKG_1 = "com.etendoerp.module.source1";
  public static final String AUTHOR_ETENDO_SOFTWARE = "Etendo Software";
  public static final String LICENSE_APACHE = "Apache2.0";
  public static final String TYPE_MODULE = "M";
  public static final String MODULES_PATH = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(
      "source.path") + "/modules/";
  public static final String SUCCESS_MSG_JAR_1 = "The " + MODULE_JAR_PGK_1 + " module was uninstalled successfully";
  public static final String SUCCESS_MSG_SOURCE_1 = "The " + MODULE_SOURCE_PKG_1 + " module was uninstalled successfully";
  public static final String VERSION = "1.0.0";
  public static final String NEW_VERSION = "2.0.0";
  public static final String PACKAGE_NAME = "com.example";
  public static final String UNKNOWN_FIELD = "unknownField";
  public static final String PACKAGES_UPDATED_SUCCESSFULLY = "Packages updated successfully";
  public static final String COMPARATOR_NOT_NULL_MESSAGE = "Comparator should not be null";
  public static final String PACKAGE_UPDATE_SUCCESS_CODE = "ETDEP_Package_Update_Success";
  public static final String PARENT1 = "parent1";
  public static final String PARENT2 = "parent2";
  public static final String RESULT_NOT_NULL_MESSAGE = "Result should not be null";
  public static final String PACKAGE_DEPENDENCY = "ETDEP_Package_Dependency e";

  public static final String PACKAGE_VERSION_ID = "@ETDEP_Package_Version.id@";

  public static final String PACKAGE_VERSION_QUERY = "e.packageVersion.id = :packageVersionId";

  public static final String PACKAGE_VERSION_ID_PARAMETER = "packageVersionId";

  public static final String TEST_ID = "TEST-ID";
  public static final String TEST_PACKAGE_VERSION = "2.1.0";
  public static final String VERSION_1_5 = "1.5.0";

  public static final String TEST_GROUP = "com.etendo";
  public static final String TEST_ARTIFACT = "test-package";
  public static final String VALID_JSON_CONTENT = "{\"currentFormat\":\"DD/MM/YYYY\"}";
  public static final String INVALID_JSON_CONTENT = "{invalid json}";
  public static final List<String> EXPECTED_FORMATS = Arrays.asList("MM/DD/YYYY", "YYYY-MM-DD", "DD-MM-YYYY");

  public static final String INPUT_FORMAT = "inpformat";
  public static final String CURRENT_PARAM = "currentParam";
  public static final String NEW_FORMAT = "newFormat";
  public static final String CONTEXT = "context";
  public static final String IS_EXTERNAL_DEPENDENCY = "inpisexternaldependency";
  public static final String VERSION_DISPLAY_LOGIC = "version_display_logic";

  public static final String PACKAGE_VERSION_ID_FIELD = "Etdep_Package_Version_ID";
  public static final String SHOW_MESSAGE_IN_PROCESS_VIEW = "showMsgInProcessView";
  public static final String ERROR = "Error";
  public static final String SUCCESS = "success";
  public static final String PROCESS_DEPENDENCY = "processDependency";
  public static final String LATEST = "LATEST";
  public static final String TEST_PACKAGE = "com.test";
  public static final String ARTIFACT = "artifact";
  public static final String MESSAGE = "message";
  public static final String MESSAGE_TEXT = "msgText";
  public static final String ERROR_MESSAGE = "error";
  public static final String MESSAGE_TYPE = "msgType";
  public static final String TEST_CONTENT = "test-content";
  public static final String DEPENDENCY_ID_1 = "dep-001";
  public static final String DEPENDENCY_ID_2 = "dep-002";

  public static final String FILTER = "filter";
  public static final String OPERATOR = "operator";
  public static final String CRITERIA = "criteria";
  public static final String VALUE = "value";
  public static final String MODULE_ETENDOERP = "com.etendoerp.module1";
  public static final String MODULE_OPENBRAVO_CORE = "org.openbravo.core";
  public static final String MODULE_SECURE_WEBSERVICES = "com.smf.securewebservices";

  public static final String TEST_DEPENDENCY_ID = "test-dependency-id";
  public static final String BOOLEAN_FALSE = "false";
  public static final String PACK_ARTIFACT = "com.test:artifact";
  public static final String GROUP = "group";
  public static final String VERSION_V1 = "version_v1";
  public static final String VERSION_V2 = "version_v2";
  public static final String STATUS = "status";

  public static final String COMPATIBLE = "compatible";
  public static final String CORE_VERSION = "coreVersion";
  public static final String CORE_VERSION_23_4_0 = "23.4.0";
  public static final String SHOULD_CONTAIN_COMPATIBLE_FIELD = "Should contain 'compatible' field";
  public static final String SHOULD_THROW_OB_EXCEPTION = "Should throw OBException";
  public static final String SHOULD_CONTAIN_ORIGINAL_CAUSE = "Should contain original cause";
  public static final String REASON = "reason";

  public static final String MODULE_ETENDO = "com.etendo";
  public static final String VERSION_1_1_0 = "1.1.0";
  public static final String DEPENDENCIES = "dependencies";
  public static final String INVALID = "invalid";
  public static final String BUILD_DEPENDENCY_INFO = "buildDependencyInfo";

  public static final String UPDATED_ARTIFACT = "updated-artifact";

  public static final String RESPONSE_ACTIONS = "responseActions";
  public static final String RETRY_EXECUTION = "retryExecution";
  public static final String MINIMUM_VERSION = "minimumVersion";


  private DependencyManagerTestConstants() {
  }
}
