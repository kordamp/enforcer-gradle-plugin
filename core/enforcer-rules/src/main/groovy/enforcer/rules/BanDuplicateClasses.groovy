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

import enforcer.rules.internal.ClassFile
import enforcer.rules.internal.ClassesWithSameName
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.codehaus.plexus.util.FileUtils
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Pattern

import static enforcer.rules.internal.JarUtils.isJarFile
import static org.apache.commons.lang3.StringUtils.isBlank
import static org.apache.commons.lang3.StringUtils.isNotBlank
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECT
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECTS

/**
 * Bans duplicate classes on the classpath.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.BanDuplicateClasses}.
 * Original author: Robert Scholte
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class BanDuplicateClasses extends AbstractResolveDependencies {
    /**
     * Default ignores which are needed for JDK 9, cause in JDK 9 and above the <code>module-info.class</code> will be
     * duplicated in any jar file. Furthermore in use cases for multi release jars the <code>module-info.class</code> is
     * also contained several times.
     */
    private static final List<String> DEFAULT_CLASSES_IGNORES = ['module-info', 'META-INF/versions/*/module-info']

    final ListProperty<String> ignoreClasses
    final ListProperty<String> configurations
    final Property<Boolean> findAllDuplicates
    final Property<Boolean> ignoreWhenIdentical

    final ListProperty<Dependency> dependencies

    @Inject
    BanDuplicateClasses(ObjectFactory objects) {
        super(objects, [AFTER_PROJECT, AFTER_PROJECTS] as EnforcerPhase[])
        ignoreClasses = objects.listProperty(String).convention([])
        dependencies = objects.listProperty(Dependency).convention([])
        configurations = objects.listProperty(String).convention([])
        findAllDuplicates = objects.property(Boolean).convention(false)
        ignoreWhenIdentical = objects.property(Boolean).convention(false)
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        if (context.enforcerPhase == AFTER_PROJECTS) {
            enforceBanDuplicates(context)
        } else {
            enforceBanDuplicates(context)
        }
    }

    private void enforceBanDuplicates(EnforcerContext context) throws EnforcerRuleException {
        Set<ResolvedArtifact> artifactsSeen = []
        Map<String, ClassesWithSameName> all_classesSeen = [:]
        Set<String> all_duplicateClassNames = []
        context.project.configurations.each { Configuration c ->
            Map<String, ClassesWithSameName> classesSeen = [:]
            Set<String> duplicateClassNames = []
            handleConfiguration(context, c, duplicateClassNames, classesSeen, artifactsSeen)
            all_classesSeen.putAll(classesSeen)
            all_duplicateClassNames.addAll(duplicateClassNames)
        }
        for (Project project : context.project.childProjects.values()) {
            project.configurations.each { Configuration c ->
                Map<String, ClassesWithSameName> classesSeen = [:]
                Set<String> duplicateClassNames = []
                handleConfiguration(context, c, duplicateClassNames, classesSeen, artifactsSeen)
                all_classesSeen.putAll(classesSeen)
                all_duplicateClassNames.addAll(duplicateClassNames)
            }
        }
        reportDuplicates(all_duplicateClassNames, all_classesSeen)
    }

    private void handleConfiguration(EnforcerContext context,
                                     Configuration configuration,
                                     Set<String> duplicateClassNames,
                                     Map<String, ClassesWithSameName> classesSeen,
                                     Set<ResolvedArtifact> artifactsSeen) {
        if (!(configurations.get().empty) && !(configurations.get().contains(configuration.name))) {
            return
        }

        if (!configuration.canBeResolved) {
            context.logger.debug("Configuration '${configuration.name}' cannot be resolved. Skipping duplicate class check.")
            return
        }

        context.logger.debug("Inspecting '${configuration.name}' for duplicate classes.")

        List<IgnorableDependency> ignorableDependencies = []

        IgnorableDependency ignoreableClasses = new IgnorableDependency()
        ignoreableClasses.applyIgnoreClasses(DEFAULT_CLASSES_IGNORES)
        if (ignoreClasses.get()) {
            ignoreableClasses.applyIgnoreClasses((List<String>) ignoreClasses.get())
        }
        ignorableDependencies.add(ignoreableClasses)

        for (Dependency dependency : dependencies.get()) {
            IgnorableDependency ignorableDependency = new IgnorableDependency()
            if (isNotBlank(dependency.groupId)) {
                ignorableDependency.groupId = Pattern.compile(asRegex(dependency.groupId))
            }
            if (isNotBlank(dependency.artifactId)) {
                ignorableDependency.artifactId = Pattern.compile(asRegex(dependency.artifactId))
            }
            if (isNotBlank(dependency.version)) {
                ignorableDependency.version = Pattern.compile(asRegex(dependency.version))
            }
            if (isNotBlank(dependency.classifier)) {
                ignorableDependency.classifier = Pattern.compile(asRegex(dependency.classifier))
            }
            ignorableDependency.applyIgnoreClasses(dependency.ignoreClasses)
            ignorableDependencies.add(ignorableDependency)
        }

        configuration.resolve()

        Set<String> duplicates = []

        for (ResolvedArtifact o : configuration.resolvedConfiguration.resolvedArtifacts) {
            if (artifactsSeen.contains(o)) {
                continue
            }
            artifactsSeen.add(o)

            File file = o.file
            context.logger.debug('Searching for duplicate classes in ' + file)
            if (file == null || !file.exists()) {
                context.logger.debug('Could not find ' + o + ' at ' + file)
            } else if (file.directory) {
                try {
                    for (String name : FileUtils.getFileNames(file, null, null, false)) {
                        context.logger.debug('  ' + name)
                        checkAndAddName(context, o, name, classesSeen, duplicates, ignorableDependencies)
                    }
                } catch (IOException e) {
                    throw fail('Unable to process dependency ' + o.toString() + ' due to ' + e.getLocalizedMessage(), e)
                }
            } else if (isJarFile(o)) {
                try {
                    JarFile jar = new JarFile(file)
                    try {
                        for (JarEntry entry : Collections.<JarEntry> list(jar.entries())) {
                            checkAndAddName(context, o, entry.name, classesSeen, duplicates, ignorableDependencies)
                        }
                    } finally {
                        try {
                            jar.close()
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                } catch (IOException e) {
                    throw fail('Unable to process dependency ' + o.toString() + ' due to ' + e.getLocalizedMessage(), e)
                }
            }
        }

        duplicateClassNames.addAll(duplicates)
    }

    private void checkAndAddName(EnforcerContext context,
                                 ResolvedArtifact artifact,
                                 String pathToClassFile,
                                 Map<String, ClassesWithSameName> classesSeen,
                                 Set<String> duplicateClasses,
                                 Collection<IgnorableDependency> ignores) throws EnforcerRuleException {
        if (!pathToClassFile.endsWith('.class')) {
            return
        }

        for (IgnorableDependency c : ignores) {
            if (c.matchesArtifact(artifact) && c.matches(pathToClassFile)) {
                if (classesSeen.containsKey(pathToClassFile)) {
                    context.logger.debug('Ignoring excluded class ' + pathToClassFile)
                }
                return
            }
        }

        ClassesWithSameName classesWithSameName = classesSeen.get(pathToClassFile)
        boolean isFirstTimeSeeingThisClass = (classesWithSameName == null)
        ClassFile classFile = new ClassFile(pathToClassFile, artifact)

        if (isFirstTimeSeeingThisClass) {
            classesSeen.put(pathToClassFile, new ClassesWithSameName(context, classFile))
            return
        }

        classesWithSameName.add(classFile)

        if (!classesWithSameName.hasDuplicates(ignoreWhenIdentical.get())) {
            return
        }

        if (findAllDuplicates.get()) {
            duplicateClasses.add(pathToClassFile)
        } else {
            ResolvedArtifact previousArtifact = classesWithSameName.previous().getArtifactThisClassWasFoundIn()

            throw fail(new StringBuilder(!message.present ? 'Duplicate class found:' : message.get())
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append('  Found in:')
                .append(System.lineSeparator())
                .append('    ')
                .append(previousArtifact)
                .append(System.lineSeparator())
                .append('    ')
                .append(artifact)
                .append(System.lineSeparator())
                .append('  Duplicate classes:')
                .append(System.lineSeparator())
                .append('    ')
                .append(pathToClassFile)
                .append(System.lineSeparator())
                .append("There may be others but 'findAllDuplicates' was set to false, so failing fast.")
                .append(System.lineSeparator())
                .append("Disable this rule temporarily with -D${resolveClassName()}.enabled=false and")
                .append(System.lineSeparator())
                .append("invoke 'dependencyInsight' or 'dependencies' to locate the source of the banned dependencies.")
                .toString())
        }
    }

    private void reportDuplicates(Set<String> duplicateClassNames, Map<String, ClassesWithSameName> classesSeen) throws EnforcerRuleException {
        if (!duplicateClassNames.isEmpty()) {
            Map<Set<ResolvedArtifact>, List<String>> inverted = [:]
            for (String className : duplicateClassNames) {
                ClassesWithSameName classesWithSameName = classesSeen.get(className)
                Set<ResolvedArtifact> artifactsOfDuplicateClass = classesWithSameName.getAllArtifactsThisClassWasFoundIn()

                List<String> s = inverted.get(artifactsOfDuplicateClass)
                if (s == null) {
                    s = []
                }
                s.add(classesWithSameName.toOutputString(ignoreWhenIdentical.get()))
                inverted.put(artifactsOfDuplicateClass, s)
            }
            StringBuilder buf = new StringBuilder(!message.present ? 'Duplicate classes found:' : message.get())
            buf.append(System.lineSeparator())
            for (Map.Entry<Set<ResolvedArtifact>, List<String>> entry : inverted.entrySet()) {
                buf.append(System.lineSeparator())
                buf.append('  Found in:');
                for (ResolvedArtifact a : entry.key) {
                    buf.append(System.lineSeparator())
                    buf.append('    ')
                    buf.append(a)
                }
                buf.append(System.lineSeparator())
                buf.append('  Duplicate classes:')
                for (String classNameWithDuplicationInfo : entry.value) {
                    buf.append(System.lineSeparator())
                    buf.append('    ')
                    buf.append(classNameWithDuplicationInfo)
                }
                buf.append(System.lineSeparator())
            }
            throw fail(buf.toString())
        }
    }

    void ignore(String str) {
        if(isNotBlank(str)) ignoreClasses.add(str)
    }

    void dependency(String str, Action<? extends Dependency> configurer) {
        Dependency dependency = parseDependency(str)
        configurer.execute(dependency)
        dependencies.add(dependency)
    }

    void dependency(Map<String, String> map, Action<? extends Dependency> configurer) {
        Dependency dependency = parseDependency(map)
        configurer.execute(dependency)
        dependencies.add(dependency)
    }

    @Canonical
    static class Dependency {
        final String groupId
        final String artifactId
        final String version
        final String classifier
        final List<String> ignoreClasses = []

        void ignore(String className) {
            if (isNotBlank(className)) ignoreClasses << className
        }
    }

    private Dependency parseDependency(String str) {
        if (isBlank(str)) {
            throw illegalArgumentException('Unparseable dependency definition: empty input')
        }

        String[] parts = str.split(':')
        if (parts.length == 3) {
            if (isBlank(parts[0])) {
                throw illegalArgumentException('Invalid dependency definition: empty groupId. ' + str)
            }
            if (isBlank(parts[1])) {
                throw illegalArgumentException('Invalid dependency definition: empty artifactId. ' + str)
            }
            if (isBlank(parts[2])) {
                throw illegalArgumentException('Invalid dependency definition: empty version. ' + str)
            }
            return new Dependency(parts[0].trim(), parts[1].trim(), parts[2].trim())
        } else if (parts.length == 3) {
            if (isBlank(parts[0])) {
                throw illegalArgumentException('Invalid dependency definition: empty groupId. ' + str)
            }
            if (isBlank(parts[1])) {
                throw illegalArgumentException('Invalid dependency definition: empty artifactId. ' + str)
            }
            if (isBlank(parts[2])) {
                throw illegalArgumentException('Invalid dependency definition: empty version. ' + str)
            }
            if (isBlank(parts[3])) {
                throw illegalArgumentException('Invalid dependency definition: empty classifier. ' + str)
            }
            return new Dependency(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim())
        } else {
            throw illegalArgumentException('Invalid dependency definition. ' + str)
        }
    }

    private Dependency parseDependency(Map<String, String> map) {
        if (map.isEmpty()) {
            throw illegalArgumentException('Unparseable dependency definition: empty input')
        }

        String groupId = map.groupId
        String artifactId = map.artifactId
        String version = map.version
        String classifier = map.classifier

        if (isBlank(groupId)) {
            throw illegalArgumentException('Invalid dependency definition: empty groupId. ' + map)
        }
        if (isBlank(artifactId)) {
            throw illegalArgumentException('Invalid dependency definition: empty artifactId. ' + map)
        }
        if (isBlank(version)) {
            throw illegalArgumentException('Invalid dependency definition: empty version. ' + map)
        }
        if (isBlank(classifier) && map.containsKey('classifier')) {
            throw illegalArgumentException('Invalid dependency definition: empty classifier. ' + map)
        }

        return new Dependency(groupId.trim(), artifactId.trim(), version.trim(), classifier?.trim())
    }
}
