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

package net.solarnetwork.esi.simple.fac.test;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Helpers for tests.
 * 
 * @author matt
 * @version 1.0
 */
public final class TestUtils {

  /**
   * Create a Mockito answer that resolves to an argument passed to the invoked method.
   * 
   * <p>
   * This can be useful for situations like a DAO {@code save()} style method that returns the same
   * object as passed in. Use is like this:
   * </p>
   * 
   * <pre>
   * <code>
   * given(...).willAnswer(TestUtils.invocationArg(0, SomeClass.class))
   * </code>
   * </pre>
   * 
   * @param <T>
   *        the argument type
   * @param index
   *        the method invocation argument index
   * @param clazz
   *        the method argument class
   * @return the answer
   */
  public static <T> Answer<T> invocationArg(int index, Class<T> clazz) {
    return new Answer<T>() {

      @Override
      public T answer(InvocationOnMock invocation) throws Throwable {
        return invocation.getArgument(index, clazz);
      }

    };
  }

}
