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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.stub.StreamObserver;

// CHECKSTYLE IGNORE LineLength FOR NEXT 20 LINES

/**
 * A {@link StreamObserver} that collects stream objects in a queue and can be used as a
 * {@link Future} to wait for an expected number of objects.
 * 
 * <p>
 * This class can simplify the common gRPC pattern of asynchronously requesting a single result or
 * an otherwise known in advance number of results in a synchronous way. For example, to post a
 * stream of {@code Updates} objects and handle a single {@code Empty} result you would write
 * something like this:
 * </p>
 * 
 * <pre>
 * <code>
 * ExampleStub client = ExampleGrpc.newStub(channel);
 * FutureStreamObserver&lt;Empty, Iterable&lt;Empty&gt;&gt; out = new QueuingStreamObserver&lt;&gt;(1);
 * StreamObserver&lt;Updates&gt; in = client.provideUpdates(out);
 * for ( Updates u : updatesCollection ) {
 *   in.onNext(u);
 * }
 * in.onCompleted();
 * Empty result = out.nab();
 * </code>
 * </pre>
 * 
 * @param <V>
 *        the observer type
 * @author matt
 * @version 1.0
 */
public class QueuingStreamObserver<V> extends CompletableStreamObserver<V, Iterable<V>> {

  private final BlockingQueue<V> queue;
  private final long timeout;
  private final TimeUnit timeoutUnit;
  private int remaining;

  private static final Logger log = LoggerFactory.getLogger(QueuingStreamObserver.class);

  /**
   * Construct with no timeout.
   * 
   * @param count
   *        the number of objects to wait for
   */
  public QueuingStreamObserver(int count) {
    this(count, 0, TimeUnit.MILLISECONDS);
  }

  /**
   * Constructor.
   * 
   * @param count
   *        the number of objects to wait for
   * @param timeout
   *        a timeout in offering extra stream objects to the internal queue
   * @param timeoutUnit
   *        the time unit to use for the timeout
   */
  public QueuingStreamObserver(int count, long timeout, TimeUnit timeoutUnit) {
    super(new CompletableFuture<>());
    this.queue = new ArrayBlockingQueue<>(count);
    this.timeout = timeout;
    this.timeoutUnit = timeoutUnit;
    this.remaining = count;
  }

  /**
   * Get direct access to the backing queue.
   * 
   * @return the queue used to hold observed stream objects
   */
  public BlockingQueue<V> getQueue() {
    return queue;
  }

  @Override
  public void onNext(V value) {
    try {
      if (this.timeout < 1) {
        if (!queue.offer(value)) {
          log.info("Discarding excess stream object: {}", value);
        }
      } else {
        if (!queue.offer(value, timeout, timeoutUnit)) {
          log.info("Discarding excess stream object from timeout: {}", value);
        }
      }
    } catch (InterruptedException e) {
      log.warn("Interrupted receiving stream object; discarding: {}", value);
    } finally {
      remaining--;
      if (remaining < 1) {
        getFuture().complete(queue);
      }
    }
  }

  @Override
  public void onCompleted() {
    getFuture().complete(queue);
  }

}
