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
import kr.motd.maven.os.Detector
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject

import static org.apache.commons.lang3.StringUtils.isBlank
import static org.apache.commons.lang3.StringUtils.isNotBlank
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.BEFORE_BUILD

/**
 * This rule checks that the OS is allowed by combinations of name, version and cpu architecture.
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class RequireOS extends AbstractStandardEnforcerRule {
    final Property<String> arch
    final Property<String> name
    final Property<String> version
    final Property<String> release
    final Property<String> classifier
    final ListProperty<String> classifierWithLikes
    final Property<Boolean> failOnUnknownOS

    @Inject
    RequireOS(ObjectFactory objects) throws EnforcerRuleException {
        super(objects, [BEFORE_BUILD] as EnforcerPhase[])
        arch = objects.property(String)
        name = objects.property(String)
        version = objects.property(String)
        release = objects.property(String)
        classifier = objects.property(String)
        classifierWithLikes = objects.listProperty(String).convention([])
        failOnUnknownOS = objects.property(Boolean).convention(true)
    }

    @Override
    protected void doValidate(EnforcerContext context) throws EnforcerRuleException {
        super.doValidate(context);

        if (allParamsEmpty()) {
            throw fail('All parameters can not be empty. ' +
                'You must pick at least one of (name, arch, version, release, classifier)')
        }
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        MyDetector detector = new MyDetector(
            context.logger,
            failOnUnknownOS.get(),
            (List<String>) classifierWithLikes.get())

        if (!isAllowed(detector)) {
            String message = message.orNull
            if (isBlank(message)) {
                message =
                    ('OS Name: ' + detector.get(Detector.DETECTED_NAME) +
                        ' Arch: ' + detector.get(Detector.DETECTED_ARCH) +
                        ' Version: ' + detector.get(Detector.DETECTED_VERSION) +
                        (isNotBlank(detector.get(Detector.DETECTED_RELEASE)) ? ' Release: ' + detector.get(Detector.DETECTED_RELEASE) : '') +
                        (isNotBlank(detector.get(Detector.DETECTED_CLASSIFIER)) ? ' Classifier: ' + detector.get(Detector.DETECTED_CLASSIFIER) : '') +
                        ' is not allowed by' +
                        (arch.orNull ? ' Arch=' + arch.get() : '') +
                        (name.orNull ? ' Name=' + name.get() : '') +
                        (version.orNull ? ' Version=' + version.get() : '') +
                        (release.orNull ? ' Release=' + release.get() : '') +
                        (classifier.orNull ? ' Classifier=' + classifier.get() : ''))
            }
            throw fail(message)
        }
    }

    private boolean isAllowed(MyDetector detector) {
        if (isNotBlank(name.orNull)) {
            if (!match(name.get(), detector.get(Detector.DETECTED_NAME))) {
                return false
            }
        }
        if (isNotBlank(arch.orNull)) {
            if (!match(arch.get(), detector.get(Detector.DETECTED_ARCH))) {
                return false
            }
        }
        if (isNotBlank(version.orNull)) {
            if (!match(version.get(), detector.get(Detector.DETECTED_VERSION))) {
                return false
            }
        }
        if (isNotBlank(release.orNull)) {
            if (!match(release.get(), detector.get(Detector.DETECTED_RELEASE))) {
                return false
            }
        }
        if (isNotBlank(classifier.orNull)) {
            if (!matchAny(classifier.get(), detector.get(Detector.DETECTED_CLASSIFIER))) {
                return false
            }
        }

        true
    }

    private boolean match(String actual, String expected) {
        String test = actual
        boolean reverse = false

        if (test.startsWith('!')) {
            reverse = true
            test = test.substring(1)
        }

        boolean result = test == expected

        return reverse ? !result : result
    }

    private boolean matchAny(String actual, String expected) {
        String test = actual
        boolean reverse = false

        if (test.startsWith('!')) {
            reverse = true
            test = test.substring(1)
        }

        boolean result = expected.contains(test)

        return reverse ? !result : result
    }

    /**
     * Helper method to check that at least one of family, name, version or arch is set.
     *
     * @return true if all parameters are empty.
     */
    private boolean allParamsEmpty() {
        return (isBlank(arch.orNull) &&
            isBlank(name.orNull) &&
            isBlank(version.orNull) &&
            isBlank(release.orNull) &&
            isBlank(classifier.orNull))
    }

    private static class MyDetector extends Detector {
        private final Logger logger
        private final Properties props = new Properties()

        MyDetector(Logger logger, boolean failOnUnknownOS, List<String> classifierWithLikes) {
            this.logger = logger
            props.put('failOnUnknownOS', String.valueOf(failOnUnknownOS))
            detect(props, classifierWithLikes)
        }

        String get(String key) {
            props.get(key)
        }

        @Override
        protected void log(String message) {
            logger.info(message)
        }

        @Override
        protected void logProperty(String name, String value) {
            logger.info(name + '=' + value)
        }
    }
}
