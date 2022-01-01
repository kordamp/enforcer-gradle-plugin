/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2022 The author and/or original authors.
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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import static org.apache.commons.lang3.StringUtils.isNotBlank
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.BEFORE_BUILD

/**
 * Contains the common code to compare an array of files against a requirement.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.AbstractRequireFiles}
 * Original author: <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
abstract class AbstractRequireFiles extends AbstractStandardEnforcerRule {
    final Property<Boolean> allowNulls
    final ListProperty<File> files
    final ListProperty<String> includePatterns
    final ListProperty<String> excludePatterns

    AbstractRequireFiles(ObjectFactory objects) {
        super(objects)
        allowNulls = objects.property(Boolean).convention(false)
        files = objects.listProperty(File).convention([])
        includePatterns = objects.listProperty(String).convention([])
        excludePatterns = objects.listProperty(String).convention([])
        phases.set([BEFORE_BUILD])
    }

    void include(String str) {
        if (isNotBlank(str)) {
            includePatterns.add(str)
        }
    }

    void exclude(String str) {
        if (isNotBlank(str)) {
            excludePatterns.add(str)
        }
    }

    void file(String str) {
        if (isNotBlank(str)) {
            files.add(new File(str))
        }
    }

    void file(File file) {
        if (file) {
            files.add(file)
        }
    }

    @Override
    protected void doValidate(EnforcerContext context) throws EnforcerRuleException {
        if (phases.get().isEmpty()) {
            throw fail('No explicit phase(s) defined.')
        }

        if (!allowNulls.get() && files.get().isEmpty()) {
            if (includePatterns.get().isEmpty() && excludePatterns.get().isEmpty()) {
                throw fail('The file list is empty and Null files are disabled.')
            }
        }
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        List<File> failures = []
        for (File file : files.get()) {
            if (!allowNulls.get() && !file) {
                failures.add(file)
            } else if (!checkFile(context, file)) {
                failures.add(file)
            }
        }

        if (!includePatterns.get().isEmpty() || !excludePatterns.get().isEmpty()) {
            DirectoryScanner ds = new DirectoryScanner()
            ds.basedir = context.basedir

            String[] include = includePatterns.get().toArray(new String[0])
            ds.setIncludes(include)
            String[] exclude = excludePatterns.get().toArray(new String[0])
            ds.setExcludes(exclude)
            ds.scan()

            String[] includedFiles = ds.includedFiles
            if (!allowNulls.get() && !includedFiles.length && failIfNoMatches()) {
                // we didn't get any matches
                String msg = message.orNull

                StringBuilder buf = new StringBuilder()
                if (isNotBlank(msg)) {
                    buf.append(msg).append(System.lineSeparator())
                }
                buf.append(getErrorMsg()).append(System.lineSeparator())

                if (include.length > 0) {
                    buf.append('Including patterns are:').append(System.lineSeparator())
                    for (String s : include) {
                        buf.append(s).append(System.lineSeparator())
                    }
                }
                if (exclude.length > 0) {
                    buf.append('Excluding patterns are:').append(System.lineSeparator())
                    for (String s : exclude) {
                        buf.append(s).append(System.lineSeparator())
                    }
                }

                throw fail(buf.toString())
            }

            for (String filename : includedFiles) {
                File file = new File(ds.basedir, filename)
                if (!checkFile(context, file)) {
                    failures.add(file)
                }
            }
        }

        // if anything was found, log it with the optional message.
        if (failures) {
            String msg = message.orNull

            StringBuilder buf = new StringBuilder()
            if (isNotBlank(msg)) {
                buf.append(msg).append(System.lineSeparator())
            }
            buf.append(getErrorMsg()).append(System.lineSeparator())

            for (File file : failures) {
                if (file) {
                    buf.append(file.getAbsolutePath()).append(System.lineSeparator())
                } else {
                    buf.append('(an empty filename was given and allowNulls is false)').append(System.lineSeparator())
                }
            }

            throw fail(buf.toString())
        }
    }

    protected abstract boolean checkFile(EnforcerContext context, File file)

    protected abstract String getErrorMsg()

    protected boolean failIfNoMatches() {
        true
    }
}
