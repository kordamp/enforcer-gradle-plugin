/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2021 The author and/or original authors.
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
package enforcer.rules

import enforcer.rules.internal.NormalizeLineSeparatorReader
import groovy.transform.CompileStatic
import org.apache.commons.io.input.ReaderInputStream
import org.codehaus.plexus.util.IOUtil
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject
import java.nio.charset.Charset
import java.nio.file.Files

import static org.apache.commons.lang3.StringUtils.isNotBlank

/**
 * Rule to validate a text file to match the specified checksum.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.RequireTextFileChecksum}.
 * Original authors: Konrad Windszus
 *
 * @author Andres Almiray
 * @since 0.9.0
 */
@CompileStatic
class RequireTextFileChecksum extends RequireFileChecksum {
    final Property<String> encoding
    final Property<String> lineSeparator

    @Inject
    RequireTextFileChecksum(ObjectFactory objects) {
        super(objects)
        encoding = objects.property(String).convention(System.getProperty('file.encoding'))
        lineSeparator = objects.property(String).convention('UNIX')
    }

    void setLineSeparator(String str) {
        if (isNotBlank(str)) {
            lineSeparator.set(NormalizeLineSeparatorReader.LineSeparator.valueOf(str.toUpperCase()).name())
        }
    }

    protected String calculateChecksum() throws EnforcerRuleException {
        NormalizeLineSeparatorReader.LineSeparator separator = NormalizeLineSeparatorReader.LineSeparator.valueOf(lineSeparator.get())
        Reader reader = null
        InputStream inputStream = null

        try {
            Charset encodingAsCharset = Charset.forName(encoding.get())
            reader = new NormalizeLineSeparatorReader(
                Files.newBufferedReader(file.get().toPath(), encodingAsCharset),
                separator)
            inputStream = new ReaderInputStream(reader, encodingAsCharset)
            calculateChecksum(inputStream)
        } catch (IOException e) {
            throw fail('Unable to calculate checksum', e)
        } finally {
            IOUtil.close(reader)
            IOUtil.close(inputStream)
        }
    }
}
