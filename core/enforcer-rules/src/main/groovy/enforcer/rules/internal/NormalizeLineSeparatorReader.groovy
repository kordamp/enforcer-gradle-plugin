/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2023 The author and/or original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package enforcer.rules.internal

/**
 * Converts Unix line separators to Windows ones and vice-versa.
 * Adapted from {@org.apache.maven.plugins.enforcer.utils.NormalizeLineSeparatorReader}
 */
class NormalizeLineSeparatorReader extends FilterReader {
    private static final int EOL = -1

    /**
     * Type representing either Unix or Windows line separators
     */
    enum LineSeparator {
        WINDOWS('\r\n', null),
        UNIX('\n', '\r');

        private final char[] separatorChars

        private final Character notPrecededByChar

        LineSeparator(String separator, Character notPrecededByChar) {
            separatorChars = separator.toCharArray()
            this.notPrecededByChar = notPrecededByChar
        }

        enum MatchResult {
            NO_MATCH,
            POTENTIAL_MATCH,
            MATCH
        }

        /**
         * Checks if two given characters match the line separator represented by this object.
         * @param currentCharacter the character to check against
         * @param previousCharacter optional previous character (may be {@code null})
         * @return one of {@link MatchResult}
         */
        MatchResult matches(char currentCharacter, Character previousCharacter) {
            int len = separatorChars.length
            if (currentCharacter == separatorChars[len - 1]) {
                if (len > 1) {
                    if (previousCharacter == null || previousCharacter != separatorChars[len - 1]) {
                        return MatchResult.NO_MATCH
                    }
                }
                if (notPrecededByChar != null) {
                    if (previousCharacter != null && notPrecededByChar == previousCharacter) {
                        return MatchResult.NO_MATCH
                    }
                }
                return MatchResult.MATCH
            } else if (len > 1 && currentCharacter == separatorChars[len - 2]) {
                return MatchResult.POTENTIAL_MATCH
            }
            return MatchResult.NO_MATCH
        }
    }

    private final LineSeparator lineSeparator
    private Character bufferedCharacter;
    private Character previousCharacter

    NormalizeLineSeparatorReader(Reader reader, LineSeparator lineSeparator) {
        super(reader)
        this.lineSeparator = lineSeparator
        bufferedCharacter = null
    }

    @Override
    int read(char[] cbuf, int off, int len) throws IOException {
        int n
        for (n = off; n < off + len; n++) {
            int readResult = read()
            if (readResult == EOL) {
                return n == 0 ? EOL : n
            } else {
                cbuf[n] = (char) readResult
            }
        }
        return n
    }

    @Override
    int read() throws IOException {
        // spool buffered characters, if any
        if (bufferedCharacter != null) {
            char localBuffer = bufferedCharacter
            bufferedCharacter = null
            return localBuffer
        }

        int readResult = super.read()
        if (readResult == EOL) {
            return readResult
        }

        char currentCharacter = (char) readResult
        if (lineSeparator == LineSeparator.UNIX) {
            switch (LineSeparator.WINDOWS.matches(currentCharacter, previousCharacter)) {
                case MATCH:
                    return lineSeparator.separatorChars[0]
                case POTENTIAL_MATCH:
                    previousCharacter = currentCharacter
                    return read()
            }
        } else { // WINDOWS
            // if current is unix, convert
            switch (LineSeparator.UNIX.matches(currentCharacter, previousCharacter)) {
                case MATCH:
                    bufferedCharacter = lineSeparator.separatorChars[1]
                    // set buffered character and return current
                    return lineSeparator.separatorChars[0]
                case POTENTIAL_MATCH:
                    // invalid option
                    throw new IllegalStateException('No potential matches expected for Unix line separator')
            }
        }

        previousCharacter = currentCharacter
        return currentCharacter
    }
}
