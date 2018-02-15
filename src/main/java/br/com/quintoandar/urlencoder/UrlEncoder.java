package br.com.quintoandar.urlencoder;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.RandomStringUtils;

public class UrlEncoder {

  private YourlsApiService service;
  private int keywordLength;
  private String signature;
  private boolean withEnvironment;
  private String environment;

  public UrlEncoder(@NonNull String uri, @NonNull int keywordLength,
      @NonNull String signature) {
    this.service = new YourlsApiService(uri);
    this.keywordLength = keywordLength;
    this.signature = signature;
    this.withEnvironment = false;
  }

  public UrlEncoder(String uri, int keywordLength, String signature,
      @NonNull String environment) {
    this(uri, keywordLength, signature);
    this.withEnvironment = true;
    this.environment = String.format("%s-", environment);
  }

  public String encodeURL(@NonNull String urlToEncode) {
    return encodeURL(urlToEncode, null, false);
  }

  public String encodeURLWithSufix(String urlToEncode, @NonNull String keyword) {
    return encodeURL(urlToEncode, keyword, true);
  }

  private String encodeURL(String urlToEncode, String keyword, boolean overwrite) {
    keyword = keyword == null ? generateRandomAlphanumeric() : keyword;
    String keywordWithPrefix = this.environment + keyword;

    keywordWithPrefix = keywordWithPrefix.toLowerCase();
    Map<String, Object> map = this.service.getInstance()
        .shorturl(signature, "shorturl", "json", urlToEncode, keywordWithPrefix,
            "URL Shortned via QuinToUrlEncoder.java");

    if (map.get("status").equals("fail")) {
      if (map.get("message").toString().equalsIgnoreCase(
          "Short URL " + keywordWithPrefix + " already exists in database or is reserved")) {
        if (overwrite && keyword != null) {
          this.service.getInstance()
              .delete(signature, YourlsApi.ACTION_DELETE, YourlsApi.FORMAT_JSON, keywordWithPrefix);
          return encodeURL(urlToEncode, keyword, overwrite);
        }
        if (!overwrite && keyword == null) {
          return encodeURL(urlToEncode, null, overwrite);
        }
      }
    }

    if (map != null && map.containsKey("shorturl")) {
      return map.get("shorturl").toString();
    }
    return null;
  }

  private ShortUrlResponse shortUrlWithKeyword(String urlToEncode, String keywordWithPrefix) {
    Map<String, Object> result = this.service.getInstance()
        .shorturl(signature, "shorturl", "json", urlToEncode, keywordWithPrefix,
            "URL Shortned via QuinToUrlEncoder.java");
    boolean fail = result.get("status").equals("fail");
    String shorturl = Optional.ofNullable(result.get("shorturl"))
        .map(o -> o.toString()).orElse(null);
    FailReason failReason = Optional.ofNullable(result.get("message"))
        .map(m -> {
          if (fail) {
            if (m.toString().equalsIgnoreCase(
                String.format(
                    "Short URL %s already exists in database or is reserved",
                    keywordWithPrefix))
                ) {
              return FailReason.KEYWORD_ALREADY_EXIST;
            } else {
              return FailReason.UNKNOWN;
            }
          }
          return null;
        }).orElse(null);

    return ShortUrlResponse.builder()
        .shortUrl(shorturl)
        .fail(fail)
        .failReason(failReason)
        .build();
  }

  public String encodeURLWithHash(String urlToEncode, String hashLoginBypass, String prefix) {
    int tamHashEncurtado = 6;
    StringBuilder keyword = new StringBuilder(prefix);

    if (hashLoginBypass != null && hashLoginBypass.length() >= tamHashEncurtado) {
      int ini = (new Random()).nextInt(hashLoginBypass.length() - tamHashEncurtado);
      keyword.append("-" + hashLoginBypass.substring(ini, ini + tamHashEncurtado));
    }
    return encodeURL(urlToEncode, keyword.toString(), true);
  }

  public String encodeWithMultipleTries(String urlToEncode, String hashLoginBypass, String prefix,
      int maxTries) {
    int tries = 0;
    while (tries < maxTries) {
      try {
        return encodeURLWithHash(urlToEncode, hashLoginBypass, prefix);
      } catch (Exception e) {
        tries++;
      }
    }
    throw new RuntimeException(
        String.format("Url shortning unavailable: tried %d", maxTries));
  }

  private String generateRandomAlphanumeric() {
    return RandomStringUtils.randomAlphanumeric(keywordLength).toLowerCase();
  }

  public enum FailReason {
    KEYWORD_ALREADY_EXIST,
    UNKNOWN,
  }

  @Data
  @Builder
  public class ShortUrlResponse {

    private String shortUrl;
    private boolean fail;
    private FailReason failReason;
  }
}
