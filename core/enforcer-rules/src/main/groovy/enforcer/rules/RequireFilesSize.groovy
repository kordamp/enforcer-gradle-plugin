/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 The author and/or original authors.
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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext

import javax.inject.Inject

/**
 * Rule to validate the main artifact is within certain size constraints.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.RequireFilesSize}.
 * Original authors: <a href="mailto:brianf@apache.org">Brian Fox</a, Roman Stumm
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class RequireFilesSize extends AbstractRequireFiles {
    final Property<Long> maxsize
    final Property<Long> minsize
    private String errorMsg

    @Inject
    RequireFilesSize(ObjectFactory objects) {
        super(objects)
        maxsize = objects.property(Long).convention(10_000L)
        minsize = objects.property(Long).convention(0L)
    }

    @Override
    protected boolean checkFile(EnforcerContext context, File file) {
        if (file == null) {
            // if we get here and it's null, treat it as a success.
            return true
        }

        // check the file now
        if (file.exists()) {
            long length = file.length()
            if (length < minsize.get()) {
                this.errorMsg = (file.name + ' size (' + length + ') too small. Min. is ' + minsize.get())
                return false
            } else if (length > maxsize.get()) {
                this.errorMsg = (file.name + ' size (' + length + ') too large. Max. is ' + maxsize.get())
                return false
            } else {
                context.logger.info(file.name
                    + ' size ('
                    + length
                    + ') is OK ('
                    + (minsize.get() == maxsize.get() || minsize.get() == 0 ? ('max. ' + maxsize.get())
                    : ('between ' + minsize.get() + ' and ' + maxsize.get())) + ' byte).')

                return true
            }
        } else {
            this.errorMsg = (file.absolutePath + ' does not exist!')
            return false
        }
    }

    @Override
    protected String getErrorMsg() {
        return this.errorMsg
    }
}
