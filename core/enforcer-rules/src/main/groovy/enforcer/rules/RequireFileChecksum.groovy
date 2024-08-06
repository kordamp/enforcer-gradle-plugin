/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2024 The author and/or original authors.
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

import groovy.transform.CompileStatic
import org.apache.commons.codec.digest.DigestUtils
import org.codehaus.plexus.util.IOUtil
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject

import static org.apache.commons.lang3.StringUtils.isBlank
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.BEFORE_BUILD

/**
 * Rule to validate a file to match the specified checksum.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.RequireFileChecksum}.
 * Original authors: Edward Samson, Lyubomyr Shaydariv
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class RequireFileChecksum extends AbstractStandardEnforcerRule {
    final Property<File> file
    final Property<String> checksum
    final Property<String> type

    @Inject
    RequireFileChecksum(ObjectFactory objects) {
        super(objects)
        phases.set([BEFORE_BUILD])
        checksum = objects.property(String)
        type = objects.property(String)
        file = objects.property(File)
    }

    @Override
    void execute(EnforcerContext context) throws EnforcerRuleException {
        if (context.enforcerPhase in phases.get()) {
            context.logger.debug("Enforcing rule ${resolveClassName()} on ${context}")
            doExecute(context)
        }
    }

    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        if (!file.present) {
            throw fail('Input file unspecified')
        }

        if (!type.present) {
            throw fail('Hash type unspecified. Valid value may be one of [md5, sha1, sha256, sha384, sha512]')
        }

        if (!checksum.present) {
            throw fail('Checksum unspecified')
        }

        if (!file.get().exists()) {
            String message = message.orNull
            if (isBlank(message)) {
                message = 'File does not exist: ' + file.get().getAbsolutePath()
            }
            throw fail(message)
        }

        if (file.get().isDirectory()) {
            throw fail('Cannot calculate the checksum of directory: '
                + file.get().getAbsolutePath())
        }

        if (!file.get().canRead()) {
            throw fail('Cannot read file: ' + file.get().getAbsolutePath())
        }

        String checksum = calculateChecksum()
        println "checksum ${this.checksum.get()}"
        println "checksum ${checksum}"

        if (!checksum.equalsIgnoreCase(this.checksum.get())) {
            String exceptionMessage = message.orNull
            if (isBlank(exceptionMessage)) {
                exceptionMessage = type.get() + ' hash of ' + file.get() + ' was ' + checksum +
                    ' but expected ' + this.checksum.get()
            }
            throw fail(exceptionMessage)
        }
    }

    protected String calculateChecksum(InputStream inputStream) throws EnforcerRuleException {
        if ('md5'.equals(type.get())) {
            return DigestUtils.md5Hex(inputStream)
        } else if ('sha1'.equals(type.get())) {
            return DigestUtils.shaHex(inputStream)
        } else if ('sha256'.equals(type.get())) {
            return DigestUtils.sha256Hex(inputStream)
        } else if ('sha384'.equals(type.get())) {
            return DigestUtils.sha384Hex(inputStream)
        } else if ('sha512'.equals(type.get())) {
            return DigestUtils.sha512Hex(inputStream)
        } else {
            throw fail('Unsupported hash type: ' + type.get())
        }
    }

    protected String calculateChecksum() throws EnforcerRuleException {
        InputStream inputStream = null
        try {
            inputStream = new FileInputStream(file.get())
            calculateChecksum(inputStream)
        } catch (IOException e) {
            throw fail('Unable to calculate checksum', e)
        } finally {
            IOUtil.close(inputStream)
        }
    }
}
