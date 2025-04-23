package com.etendoerp.dependencymanager.filterexpression;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.FilterExpression;

import com.etendoerp.dependencymanager.util.DependencyUtil;

public class DependencyManagerDefaultValuesExpression implements FilterExpression {

  private static final String NEW_FORMAT = "newFormat";
  private static final String EXTERNAL_VERSION = "externalVersion";
  private static final String VERSION_DISPLAY_LOGIC = "version_display_logic";

  private static final Logger log = LogManager.getLogger();
  @Override
  public String getExpression(Map<String, String> requestMap) {
    String currentParam = requestMap.get("currentParam");
    try {
      JSONObject context = new JSONObject(requestMap.get("context"));
      switch (currentParam) {
        case NEW_FORMAT:
            switch (context.getString("inpformat")) {
              case DependencyUtil.FORMAT_SOURCE:
                return DependencyUtil.FORMAT_JAR;
              case DependencyUtil.FORMAT_JAR:
                return DependencyUtil.FORMAT_SOURCE;
              case DependencyUtil.FORMAT_LOCAL:
              default:
                break;
            }
            break;
        case EXTERNAL_VERSION:
          return StringUtils.isNotEmpty(context.getString("inpversion")) ? context.getString("inpversion") : null;
        case VERSION_DISPLAY_LOGIC:
          return StringUtils.equals(context.getString("inpisexternaldependency"), "N") ? "Y" : "N";
        default:
          return null;
      }
    } catch (JSONException e) {
      log.error("Error parsing context JSON", e);
      throw new OBException(e);
    }
    return null;
  }
}
