package br.com.quintoandar.urlencoder;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

public interface YourlsApi {

  String ACTION_SHORTURL = "shorturl";
  String FORMAT_JSON = "json";
  String ACTION_DELETE = "delete";

  @GET
  @Path("/yourls-api.php")
  @Produces({MediaType.APPLICATION_JSON, "text/javascript",
      MediaType.APPLICATION_JSON + "; charset=UTF-8",
      "text/javascript; charset=UTF-8", "*/*"})
  @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
  Map<String, Object> shorturl(@QueryParam("signature") String signature,
      @QueryParam("action") @DefaultValue(ACTION_SHORTURL) String action,
      @QueryParam("format") @DefaultValue(FORMAT_JSON) String format, @QueryParam("url") String url,
      @QueryParam("keyword") String keyword, @QueryParam("title") String title);

  @GET
  @Path("/yourls-api.php")
  @Produces({MediaType.APPLICATION_JSON, "text/javascript",
      MediaType.APPLICATION_JSON + "; charset=UTF-8",
      "text/javascript; charset=UTF-8", "*/*"})
  @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
  Map<String, Object> delete(@QueryParam("signature") String signature,
      @QueryParam("action") @DefaultValue(ACTION_DELETE) String action,
      @QueryParam("format") @DefaultValue(FORMAT_JSON) String format,
      @QueryParam("shorturl") String keyword);

}
