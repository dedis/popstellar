package com.github.dedis.popstellar.repository.remote;

import androidx.annotation.NonNull;

import com.tinder.scarlet.websocket.okhttp.request.RequestFactory;

import okhttp3.Request;

public class LAORequestFactory implements RequestFactory {

  @NonNull private String url;

  public LAORequestFactory(@NonNull String defaultUrl) {
    this.url = defaultUrl;
  }

  @NonNull
  @Override
  public Request createRequest() {
    return new Request.Builder().url(url).build();
  }

  @NonNull
  public String getUrl() {
    return url;
  }

  public void setUrl(@NonNull String url) {
    this.url = url;
  }
}
