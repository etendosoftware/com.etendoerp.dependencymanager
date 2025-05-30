package com.etendoerp.dependencymanager.datasource;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ModuleDependenciesPEHQLTransformer class.
 * This test suite covers the HQL transformation logic, including parameter replacement,
 * clause generation, and SQL injection protection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModuleDependenciesPEHQLTransformer Unit Tests")
class ModuleDependenciesPEHQLTransformerTest {

  @InjectMocks
  private ModuleDependenciesPEHQLTransformer transformer;

  private Map<String, String> requestParameters;
  private Map<String, Object> queryNamedParameters;
  private String baseHqlQuery;

  /**
   * Sets up initial parameters and state before each test.
   */
  @BeforeEach
  void setUp() {
    requestParameters = new HashMap<>();
    queryNamedParameters = new HashMap<>();

    baseHqlQuery = "SELECT @selectClause@ FROM @fromClause@ WHERE 1=1 @whereClause@ " +
        "GROUP BY @groupByClause@ ORDER BY @orderByClause@";
  }

  /**
   * Tests the transformation of an HQL query with all placeholders.
   */
  @Test
  @DisplayName("Should transform HQL query with all placeholders correctly")
  void shouldTransformHqlQueryWithAllPlaceholders() {
    String packageVersionId = "TEST-PACKAGE-VERSION-ID-123";
    requestParameters.put("@ETDEP_Package_Version.id@", packageVersionId);

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    assertAll("HQL transformation should replace all placeholders correctly",
        () -> assertFalse(result.contains("@selectClause@"),
            "Select clause placeholder should be replaced"),
        () -> assertTrue(result.contains("ETDEP_Package_Dependency e"),
            "From clause should contain entity alias"),
        () -> assertTrue(result.contains("e.packageVersion.id = :packageVersionId"),
            "Where clause should contain parameterized condition"),
        () -> assertTrue(result.contains("e.group <> 'com.etendoerp.platform'"),
            "Where clause should exclude platform group"),
        () -> assertTrue(result.contains("e.artifact <> 'etendo-core'"),
            "Where clause should exclude etendo-core artifact"),
        () -> assertTrue(result.contains("e.id"),
            "Group by content should be present"),
        () -> assertTrue(result.contains("e.group DESC, e.artifact DESC"),
            "Order by content should be present")
    );

    assertEquals(packageVersionId, queryNamedParameters.get("packageVersionId"),
        "Package version ID should be added to named parameters");
  }

  /**
   * Tests handling of a null package version ID in the request parameters.
   */
  @Test
  @DisplayName("Should handle null package version ID gracefully")
  void shouldHandleNullPackageVersionId() {
    requestParameters.put("@ETDEP_Package_Version.id@", null);

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    assertAll("Should handle null package version ID",
        () -> assertNotNull(result, "Result should not be null"),
        () -> assertNull(queryNamedParameters.get("packageVersionId"),
            "Package version ID parameter should be null"),
        () -> assertTrue(result.contains("e.packageVersion.id = :packageVersionId"),
            "Parameter placeholder should still be present")
    );
  }

  /**
   * Tests handling of an empty request parameters map.
   */
  @Test
  @DisplayName("Should handle empty request parameters map")
  void shouldHandleEmptyRequestParameters() {
    requestParameters.clear();

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    assertAll("Should handle empty request parameters",
        () -> assertNotNull(result, "Result should not be null"),
        () -> assertNull(queryNamedParameters.get("packageVersionId"),
            "Package version ID should be null when not provided"),
        () -> assertTrue(result.contains("ETDEP_Package_Dependency e"),
            "From clause should still be transformed")
    );
  }

  /**
   * Tests the generation of the SELECT clause.
   */
  @Test
  @DisplayName("Should generate correct SELECT clause")
  void shouldGenerateCorrectSelectClause() {
    String selectClause = transformer.getSelectClauseHQL();

    assertEquals("", selectClause, "Select clause should return empty string");
  }

  /**
   * Tests the generation of the FROM clause.
   */
  @Test
  @DisplayName("Should generate correct FROM clause")
  void shouldGenerateCorrectFromClause() {
    String fromClause = transformer.getFromClauseHQL();

    assertAll("FROM clause validation",
        () -> assertNotNull(fromClause, "FROM clause should not be null"),
        () -> assertTrue(fromClause.contains("ETDEP_Package_Dependency e"),
            "FROM clause should contain entity and alias"),
        () -> assertFalse(fromClause.trim().isEmpty(),
            "FROM clause should not be empty")
    );
  }

  /**
   * Tests the generation of the WHERE clause with security filters.
   */
  @Test
  @DisplayName("Should generate correct WHERE clause with security filters")
  void shouldGenerateCorrectWhereClause() {
    String whereClause = transformer.getWhereClauseHQL();

    assertAll("WHERE clause validation",
        () -> assertNotNull(whereClause, "WHERE clause should not be null"),
        () -> assertTrue(whereClause.contains("e.packageVersion.id = :packageVersionId"),
            "Should filter by package version ID"),
        () -> assertTrue(whereClause.contains("e.group <> 'com.etendoerp.platform'"),
            "Should exclude platform group"),
        () -> assertTrue(whereClause.contains("e.artifact <> 'etendo-core'"),
            "Should exclude etendo-core artifact"),
        () -> assertTrue(whereClause.startsWith(" AND "),
            "Should start with AND for proper concatenation")
    );
  }

  /**
   * Tests the generation of the GROUP BY clause.
   */
  @Test
  @DisplayName("Should generate correct GROUP BY clause")
  void shouldGenerateCorrectGroupByClause() {
    String groupByClause = transformer.getGroupByHQL();

    assertAll("GROUP BY clause validation",
        () -> assertNotNull(groupByClause, "GROUP BY clause should not be null"),
        () -> assertTrue(groupByClause.contains("e.id"),
            "Should group by entity ID"),
        () -> assertEquals(" e.id", groupByClause,
            "Should contain exactly the expected group by expression")
    );
  }

  /**
   * Tests the generation of the ORDER BY clause.
   */
  @Test
  @DisplayName("Should generate correct ORDER BY clause")
  void shouldGenerateCorrectOrderByClause() {
    String orderByClause = transformer.getOrderByHQL();

    assertAll("ORDER BY clause validation",
        () -> assertNotNull(orderByClause, "ORDER BY clause should not be null"),
        () -> assertTrue(orderByClause.contains("e.group DESC"),
            "Should order by group descending"),
        () -> assertTrue(orderByClause.contains("e.artifact DESC"),
            "Should order by artifact descending"),
        () -> assertEquals(" e.group DESC, e.artifact DESC", orderByClause,
            "Should contain exactly the expected order by expression")
    );
  }

  /**
   * Tests prevention of SQL injection in the package version parameter.
   */
  @Test
  @DisplayName("Should prevent SQL injection in package version parameter")
  void shouldPreventSqlInjection() {
    String maliciousInput = "'; DROP TABLE users; --";
    requestParameters.put("@ETDEP_Package_Version.id@", maliciousInput);

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    assertAll("SQL injection prevention",
        () -> assertEquals(maliciousInput, queryNamedParameters.get("packageVersionId"),
            "Malicious input should be stored as parameter value"),
        () -> assertTrue(result.contains(":packageVersionId"),
            "Should use parameterized query"),
        () -> assertFalse(result.contains("DROP TABLE"),
            "Malicious SQL should not be directly embedded in query")
    );
  }

  /**
   * Tests that the query structure integrity is maintained.
   */
  @Test
  @DisplayName("Should maintain query structure integrity")
  void shouldMaintainQueryStructureIntegrity() {
    String structuredQuery = "SELECT @selectClause@ FROM @fromClause@ WHERE active = true @whereClause@";
    requestParameters.put("@ETDEP_Package_Version.id@", "TEST-ID");

    String result = transformer.transformHqlQuery(structuredQuery, requestParameters, queryNamedParameters);

    assertAll("Query structure integrity",
        () -> assertTrue(result.contains("SELECT"), "Should maintain SELECT keyword"),
        () -> assertTrue(result.contains("FROM"), "Should maintain FROM keyword"),
        () -> assertTrue(result.contains("WHERE"), "Should maintain WHERE keyword"),
        () -> assertTrue(result.contains("active = true"),
            "Should preserve existing conditions"),
        () -> assertTrue(result.contains("AND e.packageVersion.id = :packageVersionId"),
            "Should properly append new conditions with AND")
    );
  }

  /**
   * Tests handling of an empty HQL query.
   */
  @Test
  @DisplayName("Should handle empty HQL query")
  void shouldHandleEmptyHqlQuery() {
    String emptyQuery = "";
    requestParameters.put("@ETDEP_Package_Version.id@", "TEST-ID");

    String result = transformer.transformHqlQuery(emptyQuery, requestParameters, queryNamedParameters);

    assertAll("Empty query handling",
        () -> assertNotNull(result, "Result should not be null"),
        () -> assertEquals("", result, "Empty query should remain empty"),
        () -> assertEquals("TEST-ID", queryNamedParameters.get("packageVersionId"),
            "Parameters should still be processed")
    );
  }

  /**
   * Tests thread safety for concurrent HQL transformations.
   */
  @Test
  @DisplayName("Should be thread-safe for concurrent transformations")
  void shouldBeThreadSafeForConcurrentTransformations() {
    String query1 = "SELECT @selectClause@ FROM @fromClause@ WHERE 1=1 @whereClause@";
    String query2 = "SELECT COUNT(*) FROM @fromClause@ @whereClause@";

    Map<String, String> params1 = new HashMap<>();
    params1.put("@ETDEP_Package_Version.id@", "ID-1");

    Map<String, String> params2 = new HashMap<>();
    params2.put("@ETDEP_Package_Version.id@", "ID-2");

    Map<String, Object> namedParams1 = new HashMap<>();
    Map<String, Object> namedParams2 = new HashMap<>();

    String result1 = transformer.transformHqlQuery(query1, params1, namedParams1);
    String result2 = transformer.transformHqlQuery(query2, params2, namedParams2);

    assertAll("Thread safety validation",
        () -> assertEquals("ID-1", namedParams1.get("packageVersionId"),
            "First transformation should have correct parameter"),
        () -> assertEquals("ID-2", namedParams2.get("packageVersionId"),
            "Second transformation should have correct parameter"),
        () -> assertNotEquals(result1, result2,
            "Results should be different for different inputs"),
        () -> assertTrue(result1.contains("SELECT"),
            "First result should contain SELECT"),
        () -> assertTrue(result2.contains("SELECT COUNT(*)"),
            "Second result should contain SELECT COUNT")
    );
  }
}
