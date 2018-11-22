package br.com.quintoandar.urlencoder;

import lombok.Getter;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

public class YourlsApiService {
  
  @Getter
  private YourlsApi instance;

  private static final Integer CONNECTION_POOL_SIZE = 20;
  
  public YourlsApiService(String uri) {
    this.instance = createService(uri);
  }
  
  private YourlsApi createService(String uri) {
    ResteasyClient client = new ResteasyClientBuilder()
        .connectionPoolSize(CONNECTION_POOL_SIZE)
        .build();
    ResteasyWebTarget target = client.target(uri);
    return target.proxy(YourlsApi.class);
  }

}
