package com.etendoerp.dependencymanager.util;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.module.Module;

import com.etendoerp.dependencymanager.actions.InstallDependency;
import com.etendoerp.dependencymanager.data.Dependency;
import com.etendoerp.dependencymanager.data.Package;
import com.etendoerp.dependencymanager.data.PackageDependency;
import com.etendoerp.dependencymanager.data.PackageVersion;

public class PackageUtil {
  public static final String ETENDO_CORE = "etendo-core";
  public static final String CURRENT_CORE_VERSION = "currentCoreVersion";
  public static final String CORE_VERSION_RANGE = "coreVersionRange";
  public static final String STATUS = "status";
  public static final String NEW_DEPENDENCY = "New Dependency";
  public static final String UPDATED = "Updated";
  public static final String VERSION_V1 = "version_v1";
  public static final String VERSION_V2 = "version_v2";
  public static final String PACKAGE_VERSION_ID = "packageVersion.id";
  public static final String IS_COMPATIBLE = "isCompatible";
  // Constants
  private static final Logger log = LogManager.getLogger();

  /**
   * Private constructor to prevent instantiation.
   */
  private PackageUtil() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Checks the compatibility of a package with the core version and returns a JSONObject with the result.
   *
   * @param pkg
   *     The package to check compatibility for.
   * @param version
   *     The version of the package being checked.
   * @return JSONObject with compatibility result and version details.
   */
  public static JSONObject checkCoreCompatibility(Package pkg, String version) {
    JSONObject result = new JSONObject();
    try {
      PackageVersion pkgVersion = getPackageVersion(pkg, version);
      PackageDependency coreDep = getCoreDependency(pkgVersion);

      String currentCoreVersion = OBDal.getInstance().get(Module.class, "0").getVersion();
      result.put(CURRENT_CORE_VERSION, currentCoreVersion);

      if (coreDep == null) {
        handleNoCoreDependency(pkgVersion, currentCoreVersion, result);
      } else {
        handleCoreDependency(coreDep, currentCoreVersion, pkgVersion, result);
      }
    } catch (Exception e) {
      handleError(result, e);
    }
    return result;
  }

  /**
   * Retrieves the core dependency from the package version if it exists.
   *
   * @param pkgVersion the package version to check for the core dependency
   * @return the core dependency, or null if not found
   */
  private static PackageDependency getCoreDependency(PackageVersion pkgVersion) {
    return pkgVersion.getETDEPPackageDependencyList().stream()
            .filter(dep -> StringUtils.equals(ETENDO_CORE, dep.getArtifact()))
            .findFirst()
            .orElse(null);
  }

  /**
   * Handles the scenario where no core dependency is found.
   *
   * @param pkgVersion        the package version to check
   * @param currentCoreVersion the current core version
   * @param result            the JSONObject to store the compatibility result
   * @throws JSONException if an error occurs while updating the result
   */
  private static void handleNoCoreDependency(PackageVersion pkgVersion, String currentCoreVersion, JSONObject result) throws JSONException {
    String fromCore = pkgVersion.getFromCore();
    String latestCore = pkgVersion.getLatestCore();

    if (StringUtils.isBlank(fromCore) && StringUtils.isBlank(latestCore)) {
      result.put(IS_COMPATIBLE, true);
      result.put(CORE_VERSION_RANGE, "No version range available");
    } else {
      String coreVersionRange = "[" + fromCore + ", " + latestCore + ")";
      result.put(CORE_VERSION_RANGE, coreVersionRange);

      boolean isCompatible = isCompatible(coreVersionRange, currentCoreVersion);
      result.put(IS_COMPATIBLE, isCompatible);
    }
  }

  /**
   * Handles the scenario where a core dependency is found.
   *
   * @param coreDep           the core dependency found
   * @param currentCoreVersion the current core version
   * @param pkgVersion        the package version being checked
   * @param result            the JSONObject to store the compatibility result
   * @throws JSONException if an error occurs while updating the result
   */
  private static void handleCoreDependency(PackageDependency coreDep, String currentCoreVersion, PackageVersion pkgVersion, JSONObject result) throws JSONException {
    String coreVersionRange = coreDep.getVersion();

    if (StringUtils.isEmpty(coreVersionRange)) {
      handleNoCoreDependency(pkgVersion, currentCoreVersion, result);
    } else {
      result.put(CORE_VERSION_RANGE, coreVersionRange);

      boolean isCompatible = isCompatible(coreVersionRange, currentCoreVersion);
      result.put(IS_COMPATIBLE, isCompatible);
    }
  }

  /**
   * Handles exceptions by setting an error message in the result.
   *
   * @param result the JSONObject to store the error message
   * @param e      the exception that occurred
   */
  private static void handleError(JSONObject result, Exception e) {
    try {
      result.put(IS_COMPATIBLE, false);
      result.put("error", "An error occurred: " + e.getMessage());
    } catch (JSONException jsonEx) {
      log.error(jsonEx);
    }
  }

  /**
   * Retrieves the latest version of a given package.
   *
   * @param depPackage The package for which to retrieve the latest version.
   * @return The latest PackageVersion object for the specified package.
   */
  public static PackageVersion getLastPackageVersion(Package depPackage) {
    return OBDal.getInstance()
        .createQuery(PackageVersion.class, "as pv where pv.package.id = :packageId order by etdep_split_string1(pv.version) desc, etdep_split_string2(pv.version) desc, etdep_split_string3(pv.version) desc")
        .setNamedParameter("packageId", depPackage.getId())
        .setMaxResult(1)
        .uniqueResult();
  }

  /**
   * Checks if the provided version string follows the Major.Minor.Patch semantic versioning format.
   *
   * @param version The version string to check.
   * @return true if the version string follows the Major.Minor.Patch format, false otherwise.
   */
  public static boolean isMajorMinorPatchVersion (String version) {
    return version.matches("^\\d+(\\.\\d+)?(\\.\\d+)?$");
  }

  /**
   * Retrieves the package version based on the specified package and version.
   *
   * @param depPackage
   *     The package to retrieve the version for.
   * @param version
   *     The version of the package to retrieve.
   * @return The PackageVersion object corresponding to the specified package and version, or null if not found.
   */
  public static PackageVersion getPackageVersion(Package depPackage, String version) {
    OBCriteria<PackageVersion> packageVersionCriteria = OBDal.getInstance().createCriteria(PackageVersion.class);
    packageVersionCriteria.add(Restrictions.eq(PackageVersion.PROPERTY_PACKAGE, depPackage));
    packageVersionCriteria.add(Restrictions.eq(PackageVersion.PROPERTY_VERSION, version));
    packageVersionCriteria.setMaxResults(1);
    return (PackageVersion) packageVersionCriteria.uniqueResult();
  }

  /**
   * Checks if a given version falls within a specified version range.
   *
   * @param versionRange
   *     The version range to check against.
   * @param versionToCheck
   *     The version to check compatibility for.
   * @return true if the version falls within the range, false otherwise.
   */
  public static boolean isCompatible(String versionRange, String versionToCheck) {
    if (StringUtils.isEmpty(versionRange) || StringUtils.isEmpty(versionToCheck)) {
      return false;
    }

    boolean isLowerInclusive = StringUtils.startsWith(versionRange, "[");
    boolean isUpperInclusive = StringUtils.endsWith(versionRange, "]");

    String cleanRange = "";
    if (StringUtils.isNotEmpty(versionRange) && versionRange.length() > 2) {
      cleanRange = StringUtils.substring(versionRange, 1, versionRange.length() - 1);
    }

    String[] limits = StringUtils.split(cleanRange, ",");
    if (limits == null || limits.length < 2) {
      return false;
    }

    String lowerLimit = StringUtils.trim(limits[0]);
    String upperLimit = StringUtils.trim(limits[1]);

    int lowerComparison = compareVersions(versionToCheck, lowerLimit);
    int upperComparison = compareVersions(versionToCheck, upperLimit);

    boolean isAboveLowerLimit = isLowerInclusive ? lowerComparison >= 0 : lowerComparison > 0;
    boolean isBelowUpperLimit = isUpperInclusive ? upperComparison <= 0 : upperComparison < 0;

    return isAboveLowerLimit && isBelowUpperLimit;
  }

  /**
   * Compares two version strings numerically.
   *
   * @param version1
   *     The first version string to compare.
   * @param version2
   *     The second version string to compare.
   * @return An integer value representing the comparison result:
   *     0 if the versions are equal, a positive value if version1 is greater, and a negative value if version2 is greater.
   */
  public static int compareVersions(String version1, String version2) {
    String[] parts1 = version1.split("\\.");
    String[] parts2 = version2.split("\\.");
    int length = Math.max(parts1.length, parts2.length);
    for (int i = 0; i < length; i++) {
      int part1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
      int part2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
      if (part1 != part2) {
        return part1 - part2;
      }
    }
    return 0;
  }

  /**
   * Updates an existing dependency or creates a new one if it does not exist.
   *
   * @param group
   *     The group of the dependency.
   * @param artifact
   *     The artifact of the dependency.
   * @param version
   *     The version of the dependency.
   * @return The updated or created Dependency object.
   */
  public static synchronized void updateOrCreateDependency(String group, String artifact, String version) {
    Package existingPakage = OBDal.getInstance()
        .createQuery(Package.class, "as pv where pv.group = :group and pv.artifact = :artifact")
        .setNamedParameter(DependencyManagerConstants.GROUP, group)
        .setNamedParameter(DependencyManagerConstants.ARTIFACT, artifact)
        .setMaxResult(1)
        .uniqueResult();
    PackageVersion lastVersion = null;
    if (existingPakage != null) {
      lastVersion = getLastPackageVersion(existingPakage);
    }
    String versionStatus = InstallDependency.determineVersionStatus(version, lastVersion != null ? lastVersion.getVersion() : null);

    Dependency existingDependency = OBDal.getInstance()
        .createQuery(Dependency.class, "as pv where pv.group = :group and pv.artifact = :artifact")
        .setNamedParameter(DependencyManagerConstants.GROUP, group)
        .setNamedParameter(DependencyManagerConstants.ARTIFACT, artifact)
        .setMaxResult(1)
        .uniqueResult();
    if (existingDependency != null) {
      existingDependency.setVersion(version);
      existingDependency.setVersionStatus(versionStatus);
    } else {
      Dependency newDependency = new Dependency();
      newDependency.setGroup(group);
      newDependency.setArtifact(artifact);
      newDependency.setVersion(version);
      newDependency.setVersionStatus(versionStatus);
      existingDependency = newDependency;
    }
    if (existingPakage == null) {
      existingDependency.setFormat(DependencyUtil.FORMAT_JAR);
      existingDependency.setExternalDependency(true);
      existingDependency.setVersionStatus(DependencyUtil.UNTRACKED_STATUS);
    } else {
      existingDependency.setExternalDependency(false);
    }
    OBDal.getInstance().save(existingDependency);
  }

  /**
   * Splits the provided version range string into a two-element array.
   *
   * @param versionRange The version range string to split.
   * @return A two-element array with the start and end versions.
   */
  public static String[] splitCoreVersionRange(String versionRange) {
    String cleanedRange = versionRange.replaceAll("[\\[\\]()]", "");
    String[] versionSplit = cleanedRange.split(",");
    if (versionSplit.length != 2) {
      String errorMessage = String.format(
          OBMessageUtils.messageBD("ETDEP_Invalid_Version_Range_Format"),
          versionRange
      );
      throw new IllegalArgumentException(errorMessage);
    }
    return versionSplit;
  }
  
  public static String getCoreCompatibleOrLatestVersion(Package pkg) {
    // Get Package Versions
    OBCriteria<PackageVersion> versionCriteria = OBDal.getInstance().createCriteria(PackageVersion.class);
    versionCriteria.add(Restrictions.eq(PackageVersion.PROPERTY_PACKAGE, pkg));
    List<PackageVersion> pkgVersionList = versionCriteria.list();
    JSONObject compatibilityInfo;

    // Order by version number (from latest to oldest)
    pkgVersionList.sort(Collections.reverseOrder((v1, v2) -> compareVersions(v1.getVersion(), v2.getVersion())));
    PackageVersion latest = pkgVersionList.get(0);

    try {
      // Cycle through each version found and check for core compatibility. Return version if compatible found
      for (PackageVersion pkgVersion : pkgVersionList) {
        compatibilityInfo = checkCoreCompatibility(pkg, pkgVersion.getVersion());
        if (compatibilityInfo.getBoolean(IS_COMPATIBLE)) {
          return pkgVersion.getVersion();
        }
      }
    } catch (JSONException e) {
      throw new OBException(e);
    }

    // If no version is compatible, return the latest
    return latest.getVersion();
  }
}
