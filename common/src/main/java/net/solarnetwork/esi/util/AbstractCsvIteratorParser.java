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

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

/**
 * Abstract class to help with parsing CSV data via a {@link ICsvMapReader}.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class AbstractCsvIteratorParser<T> implements Iterable<T>, Closeable {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Reader input;

  private ICsvMapReader reader;

  /**
   * Construct from a {@link Reader} of a CSV data stream.
   * 
   * @param in
   *        the CSV data stream to read from
   */
  public AbstractCsvIteratorParser(Reader in) {
    super();
    this.input = in;
  }

  @Override
  public Iterator<T> iterator() {
    try {
      if (reader == null) {
        reader = new CsvMapReader(input, CsvPreference.STANDARD_PREFERENCE);
      }
      return new CsvIterator(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws IOException {
    if (reader != null) {
      reader.close();
    }
  }

  /**
   * Parse a single row of CSV data.
   * 
   * @param start
   *        the time parsing started
   * @param headers
   *        a mapping of 1-based column numbers to associated header names
   * @param row
   *        the row data, with header names as keys
   * @return the parsed object, or {@literal null} to skip row and continue
   * @throws IOException
   *         if any IO error occurs
   */
  protected abstract T parseRow(Instant start, Map<Integer, String> headers,
      Map<String, String> row) throws IOException;

  private class CsvIterator implements Iterator<T> {

    private final Instant start = Instant.now();
    private final ICsvMapReader reader;
    private final String[] headers;
    private final Map<Integer, String> headerColMap;

    private T next;

    private CsvIterator(ICsvMapReader reader) throws IOException {
      super();
      this.reader = reader;
      this.headers = reader.getHeader(true);
      if (this.headers == null) {
        throw new IOException("CSV headers not avaialble");
      }
      Map<Integer, String> headerMap = new HashMap<>(this.headers.length);
      for (int i = 0, len = this.headers.length; i < len; i += 1) {
        headerMap.put((i + 1), this.headers[i]);
      }
      this.headerColMap = Collections.unmodifiableMap(headerMap);
    }

    @Override
    public boolean hasNext() {
      if (next == null) {
        try {
          // read in rows of data until we parse a non-null value
          Map<String, String> row = null;
          do {
            row = reader.read(headers);
            if (row != null) {
              next = parseRow(start, headerColMap, row);
            }
          } while (next == null && row != null);
        } catch (IOException e) {
          log.warn("IO error reading CSV GeoLocation data: {}", e.getMessage());
        }
      }
      return next != null;
    }

    @Override
    public T next() {
      T result = null;
      if (hasNext() && next != null) {
        result = next;
        next = null;
      }
      return result;
    }

  }

}
