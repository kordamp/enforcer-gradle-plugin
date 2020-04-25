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
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext

import javax.inject.Inject

/**
 * Adapted from {@code org.apache.maven.plugins.enforcer.RequireFilesDontExist}.
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class RequireFilesDontExist extends AbstractRequireFiles {
    @Inject
    RequireFilesDontExist(ObjectFactory objects) {
        super(objects)
    }

    @Override
    protected boolean checkFile(EnforcerContext context, File file) {
        // if we get here and the handle is null, treat it as a success
        return file == null ? true : !file.exists()
    }

    @Override
    protected String getErrorMsg() {
        return 'Some files should not exist:' + System.lineSeparator()
    }
}