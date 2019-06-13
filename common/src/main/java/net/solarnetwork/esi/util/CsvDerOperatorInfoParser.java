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

import java.io.IOException;
import java.io.Reader;
import java.time.Instant;
import java.util.Map;

import net.solarnetwork.esi.domain.DerOperatorInfo;

/**
 * Parse {@link DerOperatorInfo} data from a CSV resource.
 * 
 * <p>
 * The following CSV column names are expected:
 * </p>
 * 
 * <dl>
 * <dt>name</dt>
 * <dd>The friendly name.</dd>
 * <dt>uid</dt>
 * <dd>The unique ID.</dd>
 * <dt>uri</dt>
 * <dd>The gRPC-compliant URI of the DER operator service to use.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class CsvDerOperatorInfoParser extends AbstractCsvIteratorParser<DerOperatorInfo> {

  private final DerOperatorInfo.Builder builder;

  /**
   * Constructor.
   * 
   * @param in
   *        the CSV data to parse
   */
  public CsvDerOperatorInfoParser(Reader in) {
    super(in);
    builder = DerOperatorInfo.newBuilder();
  }

  public static enum Columns {

    NAME("name"),

    UID("uid"),

    URI("uri");

    private final String key;

    private Columns(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }

  }

  @Override
  protected DerOperatorInfo parseRow(Instant start, Map<Integer, String> headers,
      Map<String, String> row) throws IOException {
    builder.clear();
    // @formatter:off
    builder.setName(row.get(Columns.NAME.getKey()))
      .setUid(row.get(Columns.UID.getKey()))
      .setEndpointUri(row.get(Columns.URI.getKey()));
    // @formatter:on
    return builder.build();
  }

}
