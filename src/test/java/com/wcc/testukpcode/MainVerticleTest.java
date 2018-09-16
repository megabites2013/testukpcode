package com.wcc.testukpcode;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import com.wcc.testukpcode.model.PostCode;
import com.wcc.testukpcode.model.PostCodeDistence;
import com.wcc.testukpcode.utils.DistanceCalc;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

import static com.jayway.restassured.RestAssured.*;
import static com.wcc.testukpcode.utils.Constants.UKPCODE;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

/**
 * MainVerticle Tester.
 *
 * @author SebastianX
 * @version 1.0
 */
@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {
  /** The Logger. */
  static final Logger LOGGER = LogManager.getLogger(UKPCODE);

  // 300,BT19,54.65170,-5.66663
  // 301,BT2,54.59309,-5.92932
  private static PostCode postcode1 = new PostCode("", "BT19", 54.65170, -5.66663);
  private static PostCode postcode2 = new PostCode("", "BT2", 54.59309, -5.92932);

  private Vertx vertx;
  private Integer port;
  private static MongodProcess MONGO;
  private static int MONGO_PORT = 27018;
  private static String[] TWO_POSTCODES_ID = new String[2];

  /**
   * Initialize.
   *
   * @throws IOException the io exception
   */
  @BeforeClass
  public static void initialize() throws IOException {

    // We can test with embedded mongoDB
    MongodStarter starter = MongodStarter.getDefaultInstance();

    IMongodConfig mongodConfig =
        new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(MONGO_PORT, Network.localhostIsIPv6()))
            .build();

    MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
    MONGO = mongodExecutable.start();
  }

  /** Shutdown. */
  @AfterClass
  public static void shutdown() {
    MONGO.stop();
  }

  /**
   * Before executing our test, let's deploy our verticle.
   *
   * <p>This method instantiates a new Vertx and deploy the verticle. Then, it waits in the verticle
   * has successfully completed its start sequence.
   *
   * @param context the test context.
   * @throws IOException the io exception
   */
  @Before
  public void setUp(TestContext context) throws IOException {
    vertx = Vertx.vertx();

    // Let's configure the verticle to listen on the 'test' port (randomly picked).
    // We create deployment options and set the _configuration_ json object:
    ServerSocket socket = new ServerSocket(0);
    port = socket.getLocalPort();
    socket.close();

    DeploymentOptions options =
        new DeploymentOptions()
            .setConfig(
                new JsonObject()
                    .put("http.port", port)
                    .put("db_name", "postcodes-test")
                    .put("connection_string", "mongodb://localhost:" + MONGO_PORT));

    // We pass the options as the second parameter of the deployVerticle method.
    vertx.deployVerticle(MainVerticle.class.getName(), options, context.asyncAssertSuccess());
  }

  /**
   * This method, called after our test, just cleanup everything by closing the vert.x instance
   *
   * @param context the test context
   */
  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  /**
   * Let's ensure that our application behaves correctly.
   *
   * @param context the test context
   */
  @Test
  public void testMyApplication(TestContext context) {

    final Async async = context.async();

    vertx
        .createHttpClient()
        .getNow(
            port,
            "localhost",
            "/",
            response -> {
              response.handler(
                  body -> {
                    String s = body.toString();
                    context.assertEquals(response.headers().get("content-type"), "text/plain");
                    context.assertNotNull(s);
                    async.complete();
                  });
            });
  }

  /**
   * Check that the index page is served.
   *
   * @param context the context
   */
  @Test
  public void checkThatTheIndexInformationPageIsServed(TestContext context) {
    LOGGER.debug("MainVerticleTest - checkThatTheIndexPageIsServed");

    Async async = context.async();
    vertx
        .createHttpClient()
        .getNow(
            port,
            "localhost",
            "/gui/index.html",
            response -> {
              context.assertEquals(response.statusCode(), 200);
              context.assertEquals(
                  response.headers().get("content-type"), "text/html"); // 3.2.1 v 3.5.3
              response.bodyHandler(
                  body -> {
                    context.assertTrue(
                        body.toString().contains("<title>UK PostCode Rest Test</title>"));
                    async.complete();
                  });
            });
  }

  /**
   * Check that we can list all the post codes availible.
   *
   * @param context the context
   */
  @Test
  public void checkThatWeCanListAllThePostCodesAvailible(TestContext context) {
    LOGGER.debug("MainVerticleTest - checkThatWeCanListAllThePostCodesAvailible");

    Async async = context.async();
    vertx
        .createHttpClient()
        .getNow(
            port,
            "localhost",
            "/api/postcodes",
            response -> {
              context.assertEquals(response.statusCode(), 200);
              context.assertEquals(
                  response.headers().get("content-type"), "application/json; charset=utf-8"); // 3.2.1 v 3.5.3
              response.bodyHandler(
                  body -> {
                    context.assertTrue(
                        body.toString().contains("postcode"));
                    async.complete();
                  });
            });
  }

  /**
   * Check that we can add.
   *
   * @param context the context
   */
  @Test
  public void checkThatWeCanAddByHttpClient(TestContext context) {
    LOGGER.debug("MainVerticleTest - checkThatWeCanAddByHttpClient ");

    Async async = context.async();
    final String json = postcode1.toJsonObj().toString();
    vertx
        .createHttpClient()
        .post(port, "localhost", "/api/postcodes")
        .putHeader("content-type", "application/json")
        .putHeader("content-length", Integer.toString(json.length()))
        .handler(
            response -> {
              context.assertEquals(response.statusCode(), 201);
              context.assertTrue(
                  response
                      .headers()
                      .get("content-type")
                      .contains("application/json; charset=utf-8"));
              response.bodyHandler(
                  body -> {
                    String s = body.toString();
                    LOGGER.debug("MainVerticleTest - checkThatWeCanAdd, server response: \n" + s);
                    final PostCode postCode1Ret = Json.decodeValue(s, PostCode.class);
                    context.assertEquals(postCode1Ret.getPostcode(), postcode1.getPostcode());
                    context.assertEquals(postCode1Ret.getLatitude(), postcode1.getLatitude());
                    context.assertEquals(postCode1Ret.getLongitude(), postcode1.getLongitude());
                    context.assertNotNull(postCode1Ret.get_id());
                    LOGGER.debug(
                        "MainVerticleTest - checkThatWeCanAdd, postCode1Ret _id:="
                            + postCode1Ret.get_id());
                    postcode1.set_id(postCode1Ret.get_id()); // update for calc
                    async.complete();
                  });
            })
        .write(json)
        .end();
  }

  /**
   * Check we can add and delete a postcode.
   *
   * @param context the context
   */
  @Test
  public void checkWeCanAddUpdateAndDeleteAPostcodeAndCalculateDistanceOfTwoByRestAssured(
      TestContext context) {

    LOGGER.debug(
        "MainVerticleTest entry -\n"
            + "ByRestAssured\n"
            + "_id1 = \n"
            + postcode1.get_id()
            + " , "
            + "_id2 = \n"
            + postcode2.get_id());

    // id,postcode,latitude,longitude
    // 18,AB34,57.09393,-2.81204
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;

    PostCode postCode2Ret =
        given()
            .body(postcode2.toJsonObj().toString())
            .request()
            .post("/api/postcodes")
            .thenReturn()
            .as(PostCode.class);
    context.assertEquals(postCode2Ret.getPostcode(), postcode2.getPostcode());
    context.assertEquals(postCode2Ret.getLatitude(), postcode2.getLatitude());
    context.assertEquals(postCode2Ret.getLongitude(), postcode2.getLongitude());
    context.assertNotNull(postCode2Ret.get_id());

    postcode2.set_id(postCode2Ret.get_id()); // update for calc

    LOGGER.debug(
        "MainVerticleTest -\n"
            + "ByRestAssured\n"
            + "add immediate return postCode2Ret = \n"
            + postCode2Ret.toString());

    // Check that it has created an individual resource, and check the content.
    Response resp2 =
        get("/api/postcodes/" + postcode2.get_id())
            .then()
            .assertThat()
            .statusCode(200)
            .body("postcode", equalToIgnoringCase(postcode2.getPostcode()))
            // .body("latitude", is(new Double(postcode2.getLatitude())))
            // .body("longitude", is(new Double(postcode2.getLongitude())))
            .body("_id", equalToIgnoringCase(postcode2.get_id()))
            .extract()
            .response();
    LOGGER.debug(
        "MainVerticleTest -\n"
            + "ByRestAssured\n"
            + "after add then get it, response = \n"
            + resp2.asString());

    LOGGER.debug(
        "MainVerticleTest -\n"
            + "ByRestAssured\n"
            + "_id1 = \n"
            + postcode1.get_id()
            + " , "
            + "_id2 = \n"
            + postcode2.get_id());

    // calc distance
    Async async = context.async();
    vertx
        .createHttpClient()
        .getNow(
            port,
            "localhost",
            "/api/postcodescalc?p1=" + postcode1.get_id() + "&p2=" + postcode2.get_id(),
            response -> {
              context.assertEquals(response.statusCode(), 200);
              context.assertEquals(
                  response.headers().get("content-type"), "application/json; charset=utf-8");
              response.bodyHandler(
                  body -> {
                    String s = body.toString();
                    LOGGER.debug("MainVerticleTest - calc distance =\n" + s);
                    final PostCodeDistence postcodedistence =
                        Json.decodeValue(s, PostCodeDistence.class);
                    context.assertEquals(
                        postcodedistence.getPostCode1().get_id(), postcode1.get_id());
                    context.assertEquals(
                        postcodedistence.getPostCode2().get_id(), postcode2.get_id());
                    context.assertEquals(
                        postcodedistence.getDistance(),
                        DistanceCalc.calculateDistance(postcode1, postcode2).getDistance());
                    async.complete();
                  });
            });

    // put update the postcode2
    postCode2Ret.setLatitude(0);
    postCode2Ret.setLongitude(0);

    PostCode postcode2U =
        given()
            .body(postCode2Ret)
            .request()
            .put("/api/postcodes/" + postCode2Ret.get_id())
            .thenReturn()
            .as(PostCode.class);
    LOGGER.debug(
        "MainVerticleTest - update postCode2Ret, \n return postcode2U =\n" + postcode2U.toString());

    context.assertEquals(postcode2U.getPostcode(), postCode2Ret.getPostcode());
    context.assertInRange(0, postcode2U.getLatitude(), 0);
    context.assertInRange(0, postcode2U.getLongitude(), 0);
    context.assertEquals(postcode2U.get_id(), postCode2Ret.get_id());
    // update done

    // Delete the postcode1
    delete("/api/postcodes/" + postcode1.get_id()).then().assertThat().statusCode(204);
    // Delete the postcode2
    delete("/api/postcodes/" + postcode2.get_id()).then().assertThat().statusCode(204);

    // Check that the resource is not available anymore
    get("/api/postcodes/" + postcode1.get_id()).then().assertThat().statusCode(404);
    // Check that the resource is not available anymore
    get("/api/postcodes/" + postcode2.get_id()).then().assertThat().statusCode(404);
  }
}
