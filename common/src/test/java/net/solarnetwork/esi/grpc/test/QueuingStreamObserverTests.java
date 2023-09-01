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

package net.solarnetwork.esi.grpc.test;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

import org.junit.Test;

import io.grpc.stub.StreamObserver;
import net.solarnetwork.esi.grpc.QueuingStreamObserver;

/**
 * Test cases for the {@link QueuingStreamObserver} class.
 * 
 * @author matt
 * @version 1.0
 */
public class QueuingStreamObserverTests {

  private static class UuidGenerator implements Runnable {

    private final StreamObserver<UUID> observer;
    private final int count;

    private UuidGenerator(int count, StreamObserver<UUID> observer) {
      super();
      this.count = count;
      this.observer = observer;
    }

    @Override
    public void run() {
      int i = count;
      while (i > 0) {
        i--;
        try {
          Thread.sleep(40);
        } catch (InterruptedException e) {
          // ignore
        }
        observer.onNext(UUID.randomUUID());
      }
      observer.onCompleted();
    }

  }

  @Test
  public void nabOne() throws Exception {
    // given
    QueuingStreamObserver<UUID> qso = new QueuingStreamObserver<>(1);
    ForkJoinPool.commonPool().submit(new UuidGenerator(1, qso));

    // when
    Iterable<UUID> results = qso.nab();

    // then
    assertThat("Results available", results, notNullValue());
    List<UUID> list = stream(results.spliterator(), false).collect(toList());
    assertThat("List count", list, hasSize(1));
  }

  @Test
  public void nabSeveral() throws Exception {
    // given
    QueuingStreamObserver<UUID> qso = new QueuingStreamObserver<>(3);
    ForkJoinPool.commonPool().submit(new UuidGenerator(3, qso));

    // when
    Iterable<UUID> results = qso.nab();

    // then
    assertThat("Results available", results, notNullValue());
    List<UUID> list = stream(results.spliterator(), false).collect(toList());
    assertThat("List count", list, hasSize(3));
  }

  @Test
  public void nabSome() throws Exception {
    // given
    QueuingStreamObserver<UUID> qso = new QueuingStreamObserver<>(2);
    ForkJoinPool.commonPool().submit(new UuidGenerator(3, qso));

    // when
    Iterable<UUID> results = qso.nab();

    // then
    assertThat("Results available", results, notNullValue());
    List<UUID> list = stream(results.spliterator(), false).collect(toList());
    assertThat("List count", list, hasSize(2));
  }

  @Test
  public void nabNotEnough() throws Exception {
    // given
    QueuingStreamObserver<UUID> qso = new QueuingStreamObserver<>(3);
    ForkJoinPool.commonPool().submit(new UuidGenerator(2, qso));

    // when
    Iterable<UUID> results = qso.nab();

    // then
    assertThat("Results available", results, notNullValue());
    List<UUID> list = stream(results.spliterator(), false).collect(toList());
    assertThat("List count", list, hasSize(2));
  }
}
