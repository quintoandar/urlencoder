package br.com.quintoandar.urlencoder;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.RandomStringUtils;

@Data
public class UrlEncoder {

  private static final int SIZE_SHORT_HASH = 6;

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
    this.environment = environment;
  }

  public String encodeURL(String url) {
    String generatedKeyword = generateRandomAlphanumeric();
    String finalKeyword = isWithEnvironment() ?
        generateKeywordWithEnvironment(generatedKeyword)
        : generatedKeyword;
    ShortUrlResponse shortUrlResponse = shortUrlWithKeyword(url, finalKeyword);
    if (shortUrlResponse.isFail() && shortUrlResponse.getFailReason().equals(FailReason.KEYWORD_ALREADY_EXIST)) {
      return this.encodeURL(url);
    } else if (!shortUrlResponse.isFail()) {
      return shortUrlResponse.getShortUrl();
    }
    return null;
  }

  public String encodeUrlWithKeyword(String url, String keyword) {
    String finalKeyword = isWithEnvironment() ?
        generateKeywordWithEnvironment(keyword)
        : keyword;
    ShortUrlResponse shortUrlResponse = shortUrlWithKeyword(url, finalKeyword);
    if (shortUrlResponse.isFail() && shortUrlResponse.getFailReason().equals(FailReason.KEYWORD_ALREADY_EXIST)) {
      deleteKeyword(finalKeyword);
      return this.encodeUrlWithKeyword(url, keyword);
    } else if (!shortUrlResponse.isFail()) {
      return shortUrlResponse.getShortUrl();
    }
    return null;
  }

  public String encodeURLWithHash(String url, @NonNull String hash, String prefix) {
    String finalKeyword = hash.length() >= SIZE_SHORT_HASH ? generateShortHash(hash, prefix) : prefix;
    return encodeUrlWithKeyword(url, finalKeyword);
  }

  public String encodeWithHashMultipleTries(String url, String hash, String prefix, int maxTries) {
    int nTries = maxTries;
    while (nTries > 0) {
      try {
        return encodeURLWithHash(url, hash, prefix);
      } catch (Exception e) {
        nTries--;
      }
    }
    throw new RuntimeException(
        String.format("Url shortning unavailable: tried %d times.", maxTries));
  }

  private String generateShortHash(String hash, String prefix) {
    int ini = (new Random()).nextInt(hash.length() - SIZE_SHORT_HASH);
    return String.format("%s-%s", prefix, hash.substring(ini, ini + SIZE_SHORT_HASH));
  }

  private String generateKeywordWithEnvironment(String generatedKeyword) {
    return String.format("%s-%s", getEnvironment(), generatedKeyword);
  }

  private void deleteKeyword(String keyword) {
    this.service.getInstance()
        .delete(signature, YourlsApi.ACTION_DELETE, YourlsApi.FORMAT_JSON, keyword);
  }

  private ShortUrlResponse shortUrlWithKeyword(String urlToEncode, String keyword) {
    Map<String, Object> result = this.service.getInstance()
        .shorturl(signature, "shorturl", "json", urlToEncode, keyword.toLowerCase(),
            "URL Shortned via UrlEncoder.java");
    boolean fail = result.get("status").equals("fail");
    String shorturl = Optional.ofNullable(result.get("shorturl"))
        .map(o -> o.toString()).orElse(null);
    FailReason failReason = Optional.ofNullable(result.get("message"))
        .map(m -> {
          if (fail) {
            if (m.toString().equalsIgnoreCase(
                String.format(
                    "Short URL %s already exists in database or is reserved",
                    keyword))
                ) {
              return FailReason.KEYWORD_ALREADY_EXIST;
            } else {
              return FailReason.UNKNOWN;
            }
          }
          return null;
        }).orElse(null);
    
    return new ShortUrlResponse(shorturl,fail,failReason);
  }
  
  private String generateRandomAlphanumeric() {
    return RandomStringUtils.randomAlphanumeric(keywordLength).toLowerCase();
  }
  
  protected enum FailReason {
    KEYWORD_ALREADY_EXIST,
    UNKNOWN,
  }

  @Data
  protected class ShortUrlResponse {
    
    private String shortUrl;
    private boolean fail;
    private FailReason failReason;
    
    public ShortUrlResponse(String shortUrl, boolean fail, FailReason failReason) {
      this.shortUrl = shortUrl;
      this.fail = fail;
      this.failReason = failReason;
    }
  }
}
