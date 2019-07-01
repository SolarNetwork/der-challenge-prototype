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

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.grpc.stub.StreamObserver;

/**
 * API for a combination {@link StreamObserver} and {@link Future}.
 * 
 * @param <V>
 *        the stream object type
 * @param <R>
 *        the future result type
 * @author matt
 * @version 1.0
 */
public interface FutureStreamObserver<V, R> extends StreamObserver<V>, Future<R> {

  /**
   * Like {@link Future#get()}, but without the {@code java.util.concurrent.ExecutionException}.
   *
   * 
   * <p>
   * Any thrown execution exception will be converted into a runtime exception. If the cause of the
   * execution exception is itself a runtime exception, that exception will be thrown directly.
   * Otherwise, the cause will be wrapped in a runtime exception.
   * </p>
   * 
   * @return the computed result
   * @throws CancellationException
   *         if the computation was cancelled
   * @throws InterruptedException
   *         if the current thread was interrupted while waiting
   */
  R nab() throws InterruptedException;

  /**
   * Like {@link Future#get(long, TimeUnit)}, but without the
   * {@code java.util.concurrent.ExecutionException}.
   * 
   * <p>
   * Any thrown execution exception will be converted into a runtime exception. If the cause of the
   * execution exception is itself a runtime exception, that exception will be thrown directly.
   * Otherwise, the cause will be wrapped in a runtime exception.
   * </p>
   * 
   * @param timeout
   *        the maximum time to wait
   * @param unit
   *        the time unit of the timeout argument
   * @return the computed result
   * @throws CancellationException
   *         if the computation was cancelled
   * @throws InterruptedException
   *         if the current thread was interrupted while waiting
   * @throws TimeoutException
   *         if the wait timed out
   */
  R nab(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;
}
