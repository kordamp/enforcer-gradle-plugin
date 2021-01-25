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

import groovy.transform.CompileStatic
import org.codehaus.plexus.util.DirectoryScanner
import org.freebsd.file.FileEncoding
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECT

/**
 * Checks file encodings to see if they match the given encoding.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.RequireEncoding}
 *
 * @author Andres Almiray
 * @since 0.7.0
 */
@CompileStatic
class RequireEncoding extends AbstractFilteringEnforcerRule {
    final Property<String> encoding
    final ListProperty<String> includes
    final ListProperty<String> excludes
    final Property<Boolean> useDefaultExcludes
    final Property<Boolean> failFast
    final Property<Boolean> acceptAsciiSubset

    @Inject
    RequireEncoding(ObjectFactory objects) throws EnforcerRuleException {
        super(objects, [AFTER_PROJECT] as EnforcerPhase[])
        encoding = objects.property(String).convention(System.getProperty('file.encoding'))
        includes = objects.listProperty(String).convention([])
        excludes = objects.listProperty(String).convention([])
        useDefaultExcludes = objects.property(Boolean).convention(true)
        failFast = objects.property(Boolean).convention(true)
        acceptAsciiSubset = objects.property(Boolean).convention(false)
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        try {
            String encoding = this.encoding.get()

            Set<String> acceptedEncodings = new HashSet<String>(Arrays.asList(encoding))
            if (encoding.equals(StandardCharsets.US_ASCII.name())) {
                context.logger.warn('Encoding US-ASCII is hard to detect. Use UTF-8 or ISO-8859-1')
            }

            if (acceptAsciiSubset.get() && (encoding.equals(StandardCharsets.ISO_8859_1.name()) || encoding.equals(StandardCharsets.UTF_8.name()))) {
                acceptedEncodings.add(StandardCharsets.US_ASCII.name())
            }

            DirectoryScanner ds = new DirectoryScanner()
            ds.basedir = context.project.projectDir

            String[] r = includes.get().toArray(new String[0])
            ds.setIncludes(r)
            r = excludes.get().toArray(new String[0])
            ds.setExcludes(r)
            if (useDefaultExcludes.get()) ds.addDefaultExcludes()
            ds.scan()

            StringBuilder filesInMsg = new StringBuilder()
            for (String file : ds.getIncludedFiles()) {
                String fileEncoding = resolveEncoding(new File(ds.basedir, file), context)
                if (context.logger.debugEnabled) {
                    context.logger.debug(file + '==>' + fileEncoding)
                }
                if (fileEncoding != null && !acceptedEncodings.contains(fileEncoding)) {
                    filesInMsg.append(file)
                    filesInMsg.append('==>')
                    filesInMsg.append(fileEncoding)
                    filesInMsg.append('\n')
                    if (failFast.get()) {
                        throw fail(filesInMsg.toString())
                    }
                }
            }
            if (filesInMsg.length() > 0) {
                throw fail('Files not encoded in ' + encoding + ':\n' + filesInMsg)
            }
        } catch (EnforcerRuleException ere) {
            throw ere
        } catch (Exception e) {
            throw fail(e.toString())
        }
    }

    private String resolveEncoding(File file, EnforcerContext context) throws IOException {
        FileEncoding fileEncoding = new FileEncoding()
        if (!fileEncoding.guessFileEncoding(Files.readAllBytes(file.toPath()))) {
            return null
        }

        if (context.logger.debugEnabled) {
            context.logger.debug(String.format('%s: (%s) %s charset=%s', file, fileEncoding.code, fileEncoding.type, fileEncoding.codeMime))
        }

        return fileEncoding.codeMime.toUpperCase()
    }
}
