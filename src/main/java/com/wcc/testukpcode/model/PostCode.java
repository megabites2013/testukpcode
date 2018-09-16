package com.wcc.testukpcode.model;

import com.opencsv.bean.CsvBindByName;
import io.vertx.core.json.JsonObject;

/**
 * PostCode Entity
 *
 * @author SebastianX
 * @version 1.0
 */
public class PostCode {
  // _id,postcode,latitude,longitude

  @CsvBindByName private String _id;
  @CsvBindByName private String postcode;
  @CsvBindByName private double latitude;
  @CsvBindByName private double longitude;

  /**
   * Instantiates a new Post code.
   *
   * @param _id the id
   * @param postcode the postcode
   * @param latitude the latitude
   * @param longitude the longitude
   */
  public PostCode(String _id, String postcode, double latitude, double longitude) {
    this._id = _id;
    this.postcode = postcode;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  /**
   * Instantiates a new Post code.
   *
   * @param postcode the postcode
   * @param latitude the latitude
   * @param longitude the longitude
   */
  public PostCode(String postcode, double latitude, double longitude) {
    this.postcode = postcode;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  /**
   * Instantiates a new Post code.
   *
   * @param jsonobj the jsonobj
   */
  public PostCode(JsonObject jsonobj) {
    this.postcode = jsonobj.getString("postcode");
    this.latitude = jsonobj.getDouble("latitude");
    this.longitude = jsonobj.getDouble("longitude");
    this._id = jsonobj.getString("_id");
  }

  /**
   * To json raw json object.
   *
   * @return the json object
   */
  public JsonObject toJsonRaw() {
    JsonObject json =
        new JsonObject()
            .put("postcode", postcode)
            .put("latitude", latitude)
            .put("longitude", longitude);
    return json;
  }

  /**
   * To json obj json object.
   *
   * @return the json object
   */
  public JsonObject toJsonObj() {
    JsonObject json =
        new JsonObject()
            .put("postcode", postcode)
            .put("latitude", latitude)
            .put("longitude", longitude)
            .put("_id", _id);

    return json;
  }

  /** Instantiates a new Post code. */
  public PostCode() {
    this._id = "";
  }

  /**
   * Gets id.
   *
   * @return the id
   */
  public String get_id() {
    return _id;
  }

  /**
   * Sets id.
   *
   * @param _id the id
   */
  public void set_id(String _id) {
    this._id = _id;
  }

  /**
   * Gets postcode.
   *
   * @return the postcode
   */
  public String getPostcode() {
    return postcode;
  }

  /**
   * Sets postcode.
   *
   * @param postcode the postcode
   */
  public void setPostcode(String postcode) {
    this.postcode = postcode;
  }

  /**
   * Gets latitude.
   *
   * @return the latitude
   */
  public double getLatitude() {
    return latitude;
  }

  /**
   * Sets latitude.
   *
   * @param latitude the latitude
   */
  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  /**
   * Gets longitude.
   *
   * @return the longitude
   */
  public double getLongitude() {
    return longitude;
  }

  /**
   * Sets longitude.
   *
   * @param longitude the longitude
   */
  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public String toString() {
    return _id + ", " + postcode + ", " + latitude + ", " + longitude;
  }
}
