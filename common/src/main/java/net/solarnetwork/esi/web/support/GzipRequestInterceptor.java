/* ========================================================================
 * Copyright 2019 SolarNetwork Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */

package net.solarnetwork.esi.web.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Request interceptor that adds support for Gzip.
 * 
 * @author matt
 * @version 1.0
 */
public class GzipRequestInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) throws IOException {
    HttpHeaders headers = request.getHeaders();
    if (!headers.containsKey(HttpHeaders.ACCEPT_ENCODING)) {
      headers.add(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate");
    }
    final ClientHttpResponse resp = execution.execute(request, body);
    return new CompressedSupportingClientHttpResponse(resp);
  }

  private static class CompressedSupportingClientHttpResponse extends AbstractClientHttpResponse {

    private final ClientHttpResponse resp;

    private CompressedSupportingClientHttpResponse(ClientHttpResponse resp) {
      super();
      this.resp = resp;
    }

    @Override
    public HttpHeaders getHeaders() {
      return resp.getHeaders();
    }

    @Override
    public InputStream getBody() throws IOException {
      InputStream in = resp.getBody();
      if (resp.getHeaders().containsKey(HttpHeaders.CONTENT_ENCODING)) {
        String enc = resp.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
        if ("gzip".equalsIgnoreCase(enc)) {
          return new GZIPInputStream(in);
        } else if ("deflate".equalsIgnoreCase(enc)) {
          return new DeflaterInputStream(in);
        }
      }
      return in;
    }

    @Override
    public String getStatusText() throws IOException {
      return resp.getStatusText();
    }

    @Override
    public int getRawStatusCode() throws IOException {
      return resp.getRawStatusCode();
    }

    @Override
    public void close() {
      resp.close();
    }

  }

}
