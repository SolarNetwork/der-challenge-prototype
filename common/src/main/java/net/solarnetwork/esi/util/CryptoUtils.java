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

package net.solarnetwork.esi.util;

import java.nio.charset.Charset;

/**
 * Cryptographic utilities.
 * 
 * @author matt
 * @version 1.0
 */
public final class CryptoUtils {

  /** A standard UTF-8 charset. */
  public static final Charset STANDARD_CHARSET = Charset.forName("UTF-8");

  /** A standard crypto helper. */
  public static final CryptoHelper STANDARD_HELPER = new EcCryptoHelper();

}
