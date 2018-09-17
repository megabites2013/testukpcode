package com.wcc.testukpcode.utils;

import io.vertx.core.json.JsonObject;

import static com.wcc.testukpcode.utils.Constants.COL_POSTCODES_TEST;
import static com.wcc.testukpcode.utils.Constants.CONNECTION_STRING;
import static com.wcc.testukpcode.utils.Constants.DB_NAME;

public class MongoCfgHelper {

  protected static String getConnectionString() {
    return getProperty(CONNECTION_STRING);
  }

  protected static String getDatabaseName() {
    return getProperty(Constants.DB_NAME);
  }

  protected static String getProperty(String name) {
    String s = System.getProperty(name);
    if (s != null) {
      s = s.trim();
      if (s.length() > 0) {
        return s;
      }
    }

    return null;
  }

  public static JsonObject getConfig(JsonObject config) {

    if (config == null) {
      config = new JsonObject();
    }

    if (config.getString(CONNECTION_STRING)==null||config.getString(CONNECTION_STRING).isEmpty()) {
      String connectionString = getConnectionString();
      if (connectionString != null) {
        config.put(CONNECTION_STRING, connectionString);
      } else {
        config.put(CONNECTION_STRING, Constants.MONGODB_LOCALHOST_27017);
      }
    }

    if (config.getString(DB_NAME)==null||config.getString(DB_NAME).isEmpty()) {

      String databaseName = getDatabaseName();
      if (databaseName != null) {
        config.put(DB_NAME, databaseName);
      } else {
        config.put(DB_NAME, COL_POSTCODES_TEST);
      }
    }

    return config;
  }
}
