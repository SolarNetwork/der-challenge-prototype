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

package net.solarnetwork.esi.util.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import net.solarnetwork.esi.domain.DerOperatorInfo;
import net.solarnetwork.esi.util.CsvDerOperatorInfoParser;

/**
 * Test cases for the {@link CsvDerOperatorInfoParser} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CsvDerOperatorInfoParserTests {

  private void assertDerOperatorInfoMatches(String desc, DerOperatorInfo info, String name,
      String uid, String uri) {
    assertThat(desc + " name", info.getName(), equalTo(name));
  }

  @Test
  public void parseCsv() throws IOException {
    // given
    Reader in = new InputStreamReader(getClass().getResourceAsStream("registry-01.csv"), "UTF-8");

    // when
    List<DerOperatorInfo> results = new ArrayList<>(8);
    try (CsvDerOperatorInfoParser parser = new CsvDerOperatorInfoParser(in)) {
      for (DerOperatorInfo info : parser) {
        results.add(info);
      }
    }

    // then
    assertThat(results, hasSize(2));
    assertDerOperatorInfoMatches("Operator 1", results.get(0), "Monopoly Utility",
        "monopoly-utility", "dns:///localhost:7443");
    assertDerOperatorInfoMatches("Operator 2", results.get(1), "Foo Utility", "foo-utility",
        "dns://1.1.1.1/foo.example.com:443");
  }

}
