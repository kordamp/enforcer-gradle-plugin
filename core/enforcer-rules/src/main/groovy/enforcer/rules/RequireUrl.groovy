/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2025 The author and/or original authors.
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
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException
import org.kordamp.gradle.plugin.enforcer.api.RepeatableEnforcerRule

import javax.inject.Inject
import java.util.function.Function
import java.util.regex.Matcher
import java.util.regex.Pattern

import static org.apache.commons.lang3.StringUtils.isBlank
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECT

/**
 * This rule checks the given URL is present and optionally matches against a regex.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.RequireProjectUrl}
 *
 * @author Andres Almiray
 * @since 0.7.0
 */
@CompileStatic
class RequireUrl extends AbstractStandardEnforcerRule implements RepeatableEnforcerRule {
    final Property<String> regex
    final Function<Project, String> urlExtractor

    @Inject
    RequireUrl(ObjectFactory objects) throws EnforcerRuleException {
        super(objects, [AFTER_PROJECT] as EnforcerPhase[])
        regex = objects.property(String).convention('^.+$')
    }

    @Override
    protected void doValidate(EnforcerContext context) throws EnforcerRuleException {
        super.doValidate(context);

        if (!urlExtractor) {
            throw fail('You must supply a value for urlExtractor')
        }
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        try {
            String url = urlExtractor.apply(context.project)

            if (!url) {
                String msg = message.orNull
                if (isBlank(msg)) {
                    msg = 'Undefined url'
                }
                throw fail(msg)
            }

            Matcher matcher = Pattern.compile(regex.get()).matcher(url)
            if (!matcher.matches()) {
                String msg = message.orNull
                if (isBlank(msg)) {
                    msg = "URL '${url}' does not match the required regex: ${regex.get()}".toString()
                }
                throw fail(msg)
            }
        } catch (EnforcerRuleException ere) {
            throw ere
        } catch (Exception e) {
            throw fail(e.toString())
        }
    }
}
