package com.wcc.testukpcode.utils;

import com.wcc.testukpcode.model.PostCode;
import com.wcc.testukpcode.model.PostCodeDistence;

/** The type Distance calc. */
public class DistanceCalc {

  /** The constant EARTH_RADIUS. */
  public static final double EARTH_RADIUS = 6371; // radius in kilometers

  /**
   * Calculate distance double.
   *
   * @param latitude the latitude
   * @param longitude the longitude
   * @param latitude2 the latitude 2
   * @param longitude2 the longitude 2
   * @return the double
   */
  public static final double calculateDistance(
      double latitude, double longitude, double latitude2, double longitude2) {

    // Using Haversine formula! See Wikipedia;
    double lon1Radians = Math.toRadians(longitude);
    double lon2Radians = Math.toRadians(longitude2);
    double lat1Radians = Math.toRadians(latitude);
    double lat2Radians = Math.toRadians(latitude2);
    double a =
        haversine(lat1Radians, lat2Radians)
            + Math.cos(lat1Radians) * Math.cos(lat2Radians) * haversine(lon1Radians, lon2Radians);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return (EARTH_RADIUS * c);
  }

  /**
   * Haversine double.
   *
   * @param deg1 the deg 1
   * @param deg2 the deg 2
   * @return the double
   */
  public static final double haversine(double deg1, double deg2) {
    return square(Math.sin((deg1 - deg2) / 2.0));
  }

  /**
   * Square double.
   *
   * @param x the x
   * @return the double
   */
  public static final double square(double x) {
    return x * x;
  }

  /**
   * Calculate distance post code distence.
   *
   * @param p1 the p 1
   * @param p2 the p 2
   * @return the post code distence
   */
  public static final PostCodeDistence calculateDistance(PostCode p1, PostCode p2) {
    double dis =
        calculateDistance(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());
    return new PostCodeDistence(p1, p2, dis);
  }
}
