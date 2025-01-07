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
package org.kordamp.gradle.plugin.enforcer.api

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import groovy.transform.PackageScope
import org.gradle.api.logging.LogLevel
import org.slf4j.Marker

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@PackageScope
@InheritConstructors
@CompileStatic
abstract class EnforcerContextLogger extends DelegatingLogger {
    protected abstract String getPrefix()

    @Override
    void debug(String s, Object... objects) {
        super.debug(prefix + s, objects)
    }

    @Override
    void lifecycle(String s) {
        super.lifecycle(prefix + s)
    }

    @Override
    void lifecycle(String s, Object... objects) {
        super.lifecycle(prefix + s, objects)
    }

    @Override
    void lifecycle(String s, Throwable throwable) {
        super.lifecycle(prefix + s, throwable)
    }

    @Override
    void quiet(String s) {
        super.quiet(prefix + s)
    }

    @Override
    void quiet(String s, Object... objects) {
        super.quiet(prefix + s, objects)
    }

    @Override
    void info(String s, Object... objects) {
        super.info(prefix + s, objects)
    }

    @Override
    void quiet(String s, Throwable throwable) {
        super.quiet(prefix + s, throwable)
    }

    @Override
    void log(LogLevel logLevel, String s) {
        super.log(logLevel, prefix + s)
    }

    @Override
    void log(LogLevel logLevel, String s, Object... objects) {
        super.log(logLevel, prefix + s, objects)
    }

    @Override
    void log(LogLevel logLevel, String s, Throwable throwable) {
        super.log(logLevel, prefix + s, throwable)
    }

    @Override
    void trace(String s) {
        super.trace(prefix + s)
    }

    @Override
    void trace(String s, Object o) {
        super.trace(prefix + s, o)
    }

    @Override
    void trace(String s, Object o, Object o1) {
        super.trace(prefix + s, o, o1)
    }

    @Override
    void trace(String s, Object... objects) {
        super.trace(prefix + s, objects)
    }

    @Override
    void trace(String s, Throwable throwable) {
        super.trace(prefix + s, throwable)
    }

    @Override
    void trace(Marker marker, String s) {
        super.trace(marker, prefix + s)
    }

    @Override
    void trace(Marker marker, String s, Object o) {
        super.trace(marker, prefix + s, o)
    }

    @Override
    void trace(Marker marker, String s, Object o, Object o1) {
        super.trace(marker, prefix + s, o, o1)
    }

    @Override
    void trace(Marker marker, String s, Object... objects) {
        super.trace(marker, prefix + s, objects)
    }

    @Override
    void trace(Marker marker, String s, Throwable throwable) {
        super.trace(marker, prefix + s, throwable)
    }

    @Override
    void debug(String s) {
        super.debug(prefix + s)
    }

    @Override
    void debug(String s, Object o) {
        super.debug(prefix + s, o)
    }

    @Override
    void debug(String s, Object o, Object o1) {
        super.debug(prefix + s, o, o1)
    }

    @Override
    void debug(String s, Throwable throwable) {
        super.debug(prefix + s, throwable)
    }

    @Override
    void debug(Marker marker, String s) {
        super.debug(marker, prefix + s)
    }

    @Override
    void debug(Marker marker, String s, Object o) {
        super.debug(marker, prefix + s, o)
    }

    @Override
    void debug(Marker marker, String s, Object o, Object o1) {
        super.debug(marker, prefix + s, o, o1)
    }

    @Override
    void debug(Marker marker, String s, Object... objects) {
        super.debug(marker, prefix + s, objects)
    }

    @Override
    void debug(Marker marker, String s, Throwable throwable) {
        super.debug(marker, prefix + s, throwable)
    }

    @Override
    void info(String s) {
        super.info(prefix + s)
    }

    @Override
    void info(String s, Object o) {
        super.info(prefix + s, o)
    }

    @Override
    void info(String s, Object o, Object o1) {
        super.info(prefix + s, o, o1)
    }

    @Override
    void info(String s, Throwable throwable) {
        super.info(prefix + s, throwable)
    }

    @Override
    void info(Marker marker, String s) {
        super.info(marker, prefix + s)
    }

    @Override
    void info(Marker marker, String s, Object o) {
        super.info(marker, prefix + s, o)
    }

    @Override
    void info(Marker marker, String s, Object o, Object o1) {
        super.info(marker, prefix + s, o, o1)
    }

    @Override
    void info(Marker marker, String s, Object... objects) {
        super.info(marker, prefix + s, objects)
    }

    @Override
    void info(Marker marker, String s, Throwable throwable) {
        super.info(marker, prefix + s, throwable)
    }

    @Override
    void warn(String s) {
        super.warn(prefix + s)
    }

    @Override
    void warn(String s, Object o) {
        super.warn(prefix + s, o)
    }

    @Override
    void warn(String s, Object... objects) {
        super.warn(prefix + s, objects)
    }

    @Override
    void warn(String s, Object o, Object o1) {
        super.warn(prefix + s, o, o1)
    }

    @Override
    void warn(String s, Throwable throwable) {
        super.warn(prefix + s, throwable)
    }

    @Override
    void warn(Marker marker, String s) {
        super.warn(marker, prefix + s)
    }

    @Override
    void warn(Marker marker, String s, Object o) {
        super.warn(marker, prefix + s, o)
    }

    @Override
    void warn(Marker marker, String s, Object o, Object o1) {
        super.warn(marker, prefix + s, o, o1)
    }

    @Override
    void warn(Marker marker, String s, Object... objects) {
        super.warn(marker, prefix + s, objects)
    }

    @Override
    void warn(Marker marker, String s, Throwable throwable) {
        super.warn(marker, prefix + s, throwable)
    }

    @Override
    void error(String s) {
        super.error(prefix + s)
    }

    @Override
    void error(String s, Object o) {
        super.error(prefix + s, o)
    }

    @Override
    void error(String s, Object o, Object o1) {
        super.error(prefix + s, o, o1)
    }

    @Override
    void error(String s, Object... objects) {
        super.error(prefix + s, objects)
    }

    @Override
    void error(String s, Throwable throwable) {
        super.error(prefix + s, throwable)
    }

    @Override
    void error(Marker marker, String s) {
        super.error(marker, prefix + s)
    }

    @Override
    void error(Marker marker, String s, Object o) {
        super.error(marker, prefix + s, o)
    }

    @Override
    void error(Marker marker, String s, Object o, Object o1) {
        super.error(marker, prefix + s, o, o1)
    }

    @Override
    void error(Marker marker, String s, Object... objects) {
        super.error(marker, prefix + s, objects)
    }

    @Override
    void error(Marker marker, String s, Throwable throwable) {
        super.error(marker, prefix + s, throwable)
    }
}
