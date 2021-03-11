/**
 * Copyright 2021 Avgustin Marinov
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
 */
package am24j.commons;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class JsonReader {

  /**
   * Protect against draining after JSOM read
   *
   * @param is input stream
   * @return protected stream
   */
  public static InputStream wrapper(final InputStream is) {
    return new InputStream() {

      private int depth;
      private boolean inString;
      private boolean end;

      @Override
      public int read() throws IOException {
        if (end) return -1;

        final int read = is.read();
        if (read == -1) {
          return -1;
        }

        if (inString) {
          if (read == '\\') {
            final int escaped = is.read();
            if (read == -1) throw new EOFException("Illegal end of stream (after escaping)!");
            return escaped;
          } else {
            if (read == '\"') {
              inString = false;
            }
            return read;
          }
        } else {
          switch (read) {
            case '\"': {
              inString = true;
              break;
            }
            case '{': {
              depth++;
              break;
            }
            case '}': {
              if (--depth == 0) {
                end = true;
              }
              break;
            }
          }
          // TODO Auto-generated method stub
          return read;
        }
      }
    };
  }
}
