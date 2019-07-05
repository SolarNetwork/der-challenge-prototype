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

package net.solarnetwork.esi.grpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of {@link FutureStreamObserver} using a provided {@link CompletableFuture} to
 * hold the future result.
 * 
 * @author matt
 * @version 1.0
 */
public class CompletableStreamObserver<V, R> implements FutureStreamObserver<V, R> {

  private final CompletableFuture<R> future;

  private static final Logger log = LoggerFactory.getLogger(CompletableStreamObserver.class);

  /**
   * Constructor.
   * 
   * @param future
   *        the completable future
   */
  public CompletableStreamObserver(CompletableFuture<R> future) {
    super();
    this.future = future;
  }

  /**
   * Get the future instance.
   * 
   * @return the future the future
   */
  protected final CompletableFuture<R> getFuture() {
    return future;
  }

  @Override
  public void onNext(V value) {
    log.debug("Received object: {}", value);
  }

  @Override
  public void onError(Throwable t) {
    future.completeExceptionally(t);
  }

  @Override
  public void onCompleted() {
    // nothing to do here, overriding classes can implement
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return future.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return future.isCancelled();
  }

  @Override
  public boolean isDone() {
    return future.isDone();
  }

  @Override
  public R get() throws InterruptedException, ExecutionException {
    return future.get();
  }

  @Override
  public R get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return future.get(timeout, unit);
  }

  @Override
  public R nab() throws InterruptedException {
    try {
      return get();
    } catch (ExecutionException e) {
      Throwable t = e.getCause();
      if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      } else {
        throw new RuntimeException(t);
      }
    }
  }

  @Override
  public R nab(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
    try {
      return get(timeout, unit);
    } catch (ExecutionException e) {
      Throwable t = e.getCause();
      if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      } else {
        throw new RuntimeException(t);
      }
    }
  }

}
