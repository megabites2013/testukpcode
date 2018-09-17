package com.wcc.testukpcode;

import com.opencsv.bean.CsvToBeanBuilder;
import com.wcc.testukpcode.model.PostCode;
import com.wcc.testukpcode.model.PostCodeDistence;
import com.wcc.testukpcode.utils.DistanceCalc;
import com.wcc.testukpcode.utils.MongoCfgHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.wcc.testukpcode.utils.Constants.*;

/**
 * MainVerticle the main
 *
 * @author SebastianX
 * @version 1.0
 */
public class MainVerticle extends AbstractVerticle {

  /** The Logger. */
  static final Logger LOGGER = LogManager.getLogger(UKPCODE);

  private MongoClient mongo;

  /** @param fut the future */
  @Override
  public void start(Future<Void> fut) {

    mongo = MongoClient.createShared(vertx, MongoCfgHelper.getConfig(config()));
    LOGGER.debug("MainVerticle - connected to Mongo");
    prepareDataService(nothing -> startWebApp(http -> completeStartup(http, fut)), fut);
  }

  private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
    Router router = Router.router(vertx);
    LOGGER.debug("MainVerticle - startWebApp");

    router
        .route("/")
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.putHeader(CONTENT_TYPE, "text/plain").end(THE1PAGE);
            });

    router.route("/gui/*").handler(StaticHandler.create("gui"));
    router.route("/api/postcodes*").handler(BodyHandler.create());
    router.get("/api/postcodes").handler(this::getAll);
    router.get("/api/postcodes/:p").handler(this::getOne);
    router.get("/api/postcodescalc").handler(this::calcDistence);
    router.post("/api/postcodes").handler(this::addOne);
    router.delete("/api/postcodes/:p").handler(this::deleteOne);
    router.put("/api/postcodes/:p").handler(this::updateOne);

    vertx
        .createHttpServer()
        .requestHandler(router::accept)
        .listen(config().getInteger("http.port", 8080), next::handle);
  }

  private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
    if (http.succeeded()) {
      fut.complete();
      LOGGER.debug("MainVerticle - completeStartup");
    } else {
      fut.fail(http.cause());
    }
  }

  @Override
  public void stop() throws Exception {
    mongo.close();
    LOGGER.debug("MainVerticle - stop");
  }

  private void addOne(RoutingContext routingContext) {

    String bodyAsString = routingContext.getBodyAsString();
    final PostCode p = Json.decodeValue(bodyAsString, PostCode.class);
    if (p == null || p.getPostcode() == null || p.getPostcode().isEmpty()) {
      routingContext.response().setStatusCode(404).end();
      return;
    }
    LOGGER.debug("MainVerticle - addOne: body = " + bodyAsString);

    mongo.removeDocuments(
        COLLECTION,
        new JsonObject().put("postcode", p.getPostcode()),
        ar -> {
          mongo.insert(
              COLLECTION,
              p.toJsonRaw(),
              (AsyncResult<String> r) -> {
                if (r.failed()) {
                  routingContext.response().setStatusCode(500).end();
                  LOGGER.debug("MainVerticle - addOne: XXX  " + ar.cause().getMessage());
                  return;
                }
                p.set_id(r.result());
                routingContext
                    .response()
                    .setStatusCode(201)
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                    .end(Json.encodePrettily(p));
                LOGGER.info("MainVerticle - addOne: => " + p.toString());
              });
        });
  }

  private void getOne(RoutingContext routingContext) {
    final String _id = routingContext.request().getParam("p");
    LOGGER.debug("MainVerticle - getOne: _id = " + _id);

    if (_id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      mongo.findOne(
          COLLECTION,
          new JsonObject().put("_id", _id),
          null,
          ar -> {
            if (ar.succeeded()) {
              if (ar.result() == null) {
                routingContext.response().setStatusCode(404).end();
                return;
              }
              PostCode resPostCode = new PostCode(ar.result());
              routingContext
                  .response()
                  .setStatusCode(200)
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                  .end(Json.encodePrettily(resPostCode));
              LOGGER.info("MainVerticle - getOne: =>  " + resPostCode.toString());

            } else {
              routingContext.response().setStatusCode(404).end();
              LOGGER.debug("MainVerticle - getOne: XXX  " + ar.cause().getMessage());
            }
          });
    }
  }

  private void updateOne(RoutingContext routingContext) {
    final String _id = routingContext.request().getParam("p");
    LOGGER.debug("MainVerticle - updateOne: _id = " + _id);

    JsonObject jsonobj = routingContext.getBodyAsJson();
    if (_id == null || jsonobj == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      mongo.save(
          COLLECTION,
          jsonobj.put("_id", _id),
          v -> {
            if (v.failed()) {
              routingContext.response().setStatusCode(404).end();
              LOGGER.debug("MainVerticle - updateOne: XXX " + v.cause().getMessage());

            } else {
              routingContext
                  .response()
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                  .end(Json.encodePrettily(new PostCode(jsonobj)));
              LOGGER.info("MainVerticle - updateOne: => " + jsonobj.toString());
            }
          });
    }
  }

  private void deleteOne(RoutingContext routingContext) {
    String _id = routingContext.request().getParam("p");
    LOGGER.debug("MainVerticle - deleteOne: _id = " + _id);

    if (_id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      mongo.removeDocument(
          COLLECTION,
          new JsonObject().put("_id", _id),
          ar -> routingContext.response().setStatusCode(204).end());
    }
  }

  private void getAll(RoutingContext routingContext) {

    mongo.find(
        COLLECTION,
        new JsonObject(),
        results -> {
          List<JsonObject> objects = results.result();
          List<PostCode> postCodes =
              objects.stream().map(PostCode::new).collect(Collectors.toList());
          LOGGER.debug("MainVerticle - getAll: count = " + postCodes.size());

          routingContext
              .response()
              .setStatusCode(200)
              .putHeader(CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
              .end(Json.encodePrettily(postCodes));
        });
  }

  private void calcDistence(RoutingContext routingContext) {
    final String pcode1 = routingContext.request().getParam("p1");
    final String pcode2 = routingContext.request().getParam("p2");
    if (pcode1 == null || pcode2 == null) {
      routingContext.response().setStatusCode(400).end();
    } else {

      LOGGER.debug(
          "MainVerticle - calcDistence [ _id1, _id2 ] =  [ " + pcode1 + ", " + pcode2 + " ]");

      JsonObject query = new JsonObject();
      JsonArray arrayid = new JsonArray();
      arrayid.add(pcode1).add(pcode2);

      query.put("_id", new JsonObject().put("$in", arrayid));
      LOGGER.debug(query.toString());
      mongo.find(
          COLLECTION,
          query,
          results -> {
            List<JsonObject> objects = results.result();
            List<PostCode> postCodes =
                objects.stream().map(PostCode::new).collect(Collectors.toList());
            LOGGER.debug("MainVerticle - calcDistence: count = " + postCodes.size());
            if (postCodes.size() != MAGIC_TWO) {
              postCodes.stream().forEach(x -> LOGGER.debug((x.toJsonObj().toString())));
              routingContext.response().setStatusCode(404).end();
              return;
            }
            PostCode p1 = postCodes.get(0);
            LOGGER.debug("MainVerticle - calcDistence: p1 = " + p1.toString());

            PostCode p2 = postCodes.get(1);
            LOGGER.debug("MainVerticle - calcDistence: p2 = " + p2.toString());

            PostCodeDistence pcd = DistanceCalc.calculateDistance(p1, p2);
            String s = Json.encodePrettily(pcd.toJsonObj());
            LOGGER.info("calcDistence = "+ s);
            routingContext
                .response()
                .setStatusCode(200)
                .putHeader(CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                .end(s);
          });
    }
  }

  private void prepareDataService(Handler<AsyncResult<Void>> next, Future<Void> fut) {
    LOGGER.debug("MainVerticle - prepareDataService");
    // Do we have data in the collection ?
    mongo.count(
        COLLECTION,
        new JsonObject(),
        count -> {
          if (count.succeeded()) {
            if (count.result() == 0) {
              LOGGER.debug("prepareDataService - no records, insert data");
              // no records, insert data
              mongo.bulkWrite(
                  COLLECTION,
                  loadPostCodeData(),
                  ar -> {
                    if (!ar.succeeded()) {
                      LOGGER.error("prepareDataService faile - Document may missing", ar.cause());
                      fut.fail(ar.cause());
                    } else {
                      LOGGER.debug("prepareDataService done - Document inserted");
                      next.handle(Future.<Void>succeededFuture());
                    }
                  });
            } else {
              next.handle(Future.<Void>succeededFuture());
            }
          } else {
            // report the error
            fut.fail(count.cause());
          }
        });
  }


  /**
   * Load some postcode from reading csv file.
   *
   * @return the List<BulkOperation> later inject to db
   */
  private List<BulkOperation> loadPostCodeData() {

    List<BulkOperation> retlst = new ArrayList<>();
    InputStream in = getClass().getResourceAsStream(FILE_POSTCODE_OUTCODES_CSV);
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));

    List<PostCode> postCodeList = new ArrayList<>();
    try {
      postCodeList = new CsvToBeanBuilder(reader).withType(PostCode.class).build().parse();
      LOGGER.debug("processing number of  entries in file:" + postCodeList.size());
    } catch (Exception e) {
      LOGGER.error("processing csv file error:" + e.getMessage());
    }

    if (postCodeList != null && !postCodeList.isEmpty()) {
      retlst =
          postCodeList
              .stream()
              .map(x -> BulkOperation.createInsert(x.toJsonRaw()))
              .collect(Collectors.toList());
      LOGGER.debug("mapping number of entries from csv file:" + retlst.size());
    }
    return retlst;
  }
}
