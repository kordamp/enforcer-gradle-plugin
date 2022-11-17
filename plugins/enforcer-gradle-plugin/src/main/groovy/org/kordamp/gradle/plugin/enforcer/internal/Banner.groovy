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
package org.kordamp.gradle.plugin.enforcer.internal

import groovy.transform.CompileStatic
import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle

import java.text.MessageFormat

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
final class Banner {
    private final ResourceBundle bundle = ResourceBundle.getBundle('org.kordamp.gradle.plugin.enforcer.Banner')
    private final String productVersion = bundle.getString('product.version')
    private final String productId = bundle.getString('product.id')
    private final String productName = bundle.getString('product.name')
    private final String banner = MessageFormat.format(bundle.getString('product.banner'), productName, productVersion)
    private final List<String> visited = []

    private static final Banner b = new Banner()
    private static final String ORG_KORDAMP_BANNER = 'org.kordamp.banner'

    private Banner() {
        // noop
    }

    static void display(Settings settings) {
        if (b.visited.contains(settings.rootProject.path)) {
            return
        }
        b.visited.add(settings.rootProject.path)
        settings.gradle.addBuildListener(new BuildAdapter() {
            @Override
            void buildFinished(BuildResult result) {
                b.visited.clear()
            }
        })

        checkMarkerFile(settings.gradle)
    }

    static void display(Project project) {
        if (b.visited.contains(project.rootProject.path)) {
            return
        }
        b.visited.add(project.rootProject.path)
        project.gradle.addBuildListener(new BuildAdapter() {
            @Override
            void buildFinished(BuildResult result) {
                b.visited.clear()
            }
        })

        checkMarkerFile(project.gradle)
    }

    static private checkMarkerFile(Gradle gradle) {
        boolean printBanner = null == System.getProperty(ORG_KORDAMP_BANNER) || Boolean.getBoolean(ORG_KORDAMP_BANNER)

        File parent = new File(gradle.gradleUserHomeDir, 'caches')
        File markerFile = b.getMarkerFile(parent)
        if (!markerFile.exists()) {
            markerFile.parentFile.mkdirs()
            markerFile.text = '1'
            if (printBanner) println(b.banner)
        } else {
            try {
                int count = Integer.parseInt(markerFile.text)
                if (count < 3) {
                    if (printBanner) println(b.banner)
                }
                markerFile.text = (count + 1) + ''
            } catch (NumberFormatException e) {
                markerFile.text = '1'
                if (printBanner) println(b.banner)
            }
        }
    }

    private File getMarkerFile(File parent) {
        new File(parent,
            'kordamp' +
                File.separator +
                productId +
                File.separator +
                productVersion +
                File.separator +
                'marker.txt')
    }
}
