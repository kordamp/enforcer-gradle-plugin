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
package org.kordamp.gradle.plugin.enforcer

import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.IgnoreRest
import spock.lang.Specification

class BuildEnforcerPluginKotlinIntegrationTest extends Specification {
    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile
    File settingsFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle.kts')
        buildFile << """            
            plugins {
                id ("base")
            }
        """

        settingsFile = testProjectDir.newFile('settings.gradle.kts')
        settingsFile << """
            import org.kordamp.gradle.plugin.enforcer.rule
            import org.kordamp.gradle.plugin.enforcer.api.BuildEnforcerExtension

            buildscript {
                repositories {
                    gradlePluginPortal()
                    flatDir { dirs (${System.getProperty('jars.dir').replace("'", '"')}) }
                }
                dependencies {
                    classpath ("org.kordamp.gradle:enforcer-api:${System.getProperty('project.version')}")
                    classpath ("org.kordamp.gradle:enforcer-rules:${System.getProperty('project.version')}")
                    classpath ("org.kordamp.gradle:enforcer-gradle-plugin:${System.getProperty('project.version')}")
                    classpath ("org.kordamp.gradle:enforcer-gradle-plugin-tests:${System.getProperty('project.version')}")    
                    classpath ("org.apache.commons:commons-lang3:${System.getProperty('commonsLang3Version')}")
                    classpath ("commons-codec:commons-codec:${System.getProperty('commonsCodecVersion')}")
                    classpath ("org.apache.maven:maven-artifact:${System.getProperty('mavenVersion')}")
                    classpath ("kr.motd.maven:os-maven-plugin:${System.getProperty('osMavenPluginVersion')}")
                }
            }
            apply(plugin= "org.kordamp.gradle.enforcer")
        """
    }

    def "Can disable all enforcer rules"() {
        given:
        settingsFile << """
            configure<BuildEnforcerExtension> {
                enabled.set(false)
                rule<enforcer.rules.AlwaysFail>()
//                allprojects {
//                   rule<enforcer.rules.AlwaysFail>()
//                }
            }
        """

        when:
        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('clean', '--stacktrace')
            .withPluginClasspath()
            .build()

        then:
        result.task(':clean').outcome == UP_TO_DATE
    }

    def "Can disable a single enforcer rule"() {
        given:
        settingsFile << """
            configure<BuildEnforcerExtension> {
                rule<enforcer.rules.AlwaysFail> {
                    enabled.set(false)
                }
            }   
        """

        when:
        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('clean', '--stacktrace')
            .withPluginClasspath()
            .build()

        then:
        result.task(':clean').outcome == UP_TO_DATE
    }

    def "Can fail a build"() {
        given:
        settingsFile << """
            configure<BuildEnforcerExtension> {
                rule<enforcer.rules.AlwaysFail> ()
            }   
        """

        when:
        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('clean', '--stacktrace')
            .withPluginClasspath()
            .buildAndFail()

        then:
        result.output.contains("[BEFORE_BUILD] An Enforcer rule has failed")
        result.output.contains("Enforcer rule 'enforcer.rules.AlwaysFail' was triggered.")
    }

    def "Can fail a build fast"() {
        given:
        settingsFile << """
            configure<BuildEnforcerExtension> {
                failFast.set(true)
                rule<enforcer.rules.AlwaysFail>()
                rule<org.kordamp.gradle.plugin.enforcer.Fail>()
            }
        """

        when:
        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('clean', '--stacktrace')
            .withPluginClasspath()
            .buildAndFail()

        then:
        result.output.contains("[BEFORE_BUILD] An Enforcer rule has failed")
        result.output.contains("Enforcer rule 'enforcer.rules.AlwaysFail' was triggered.")
    }

    def "Can fail multiple rules in the same phase"() {
        given:
        settingsFile << """
            configure<BuildEnforcerExtension> {
                failFast.set(false)
                rule<enforcer.rules.AlwaysFail>()
                rule<org.kordamp.gradle.plugin.enforcer.Fail>()
            }
        """

        when:
        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('clean', '--stacktrace')
            .withPluginClasspath()
            .buildAndFail()

        then:
        result.output.contains('[BEFORE_BUILD] 2 Enforcer rules have failed')
        result.output.contains("Enforcer rule 'enforcer.rules.AlwaysFail' was triggered.")
        result.output.contains("Enforcer rule 'org.kordamp.gradle.plugin.enforcer.Fail' was triggered.")
    }
}
