package com.wcc.testukpcode;

import com.wcc.testukpcode.model.PostCode;
import com.wcc.testukpcode.model.PostCodeDistence;
import com.wcc.testukpcode.utils.DistanceCalc;
import io.vertx.core.json.Json;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.wcc.testukpcode.utils.Constants.UKPCODE;

/**
 * DistanceCalc Tester.
 *
 * @author SebastianX
 * @version 1.0
 */
public class DistanceCalcTest {
  static final Logger LOGGER = LogManager.getLogger(UKPCODE);
  // 300,BT19,54.65170,-5.66663
  // 301,BT2,54.59309,-5.92932
  private static PostCode postcode1 = new PostCode("", "BT19", 54.65170, -5.66663);
  private static PostCode postcode2 = new PostCode("", "BT2", 54.59309, -5.92932);

  @Before
  public void before() throws Exception {}

  @After
  public void after() throws Exception {}

  /** Method: calculateDistance(PostCode p1, PostCode p2) */
  @Test
  public void testCalculateDistanceForP1P2() throws Exception {

    PostCodeDistence postCodeDistence = DistanceCalc.calculateDistance(postcode1, postcode2);
    assert (postCodeDistence != null);
    String restr = Json.encodePrettily(postCodeDistence);
    assert (restr != null && !restr.isEmpty());
    LOGGER.debug("testCalculateDistanceForP1P2 = " + restr);
    assert (postCodeDistence.getDistance() != 0);
  }
}
