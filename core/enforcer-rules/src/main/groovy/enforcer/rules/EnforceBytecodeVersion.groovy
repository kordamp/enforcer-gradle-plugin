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

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import org.codehaus.plexus.util.IOUtil
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
import java.util.regex.Matcher
import java.util.regex.Pattern

import static enforcer.rules.internal.JarUtils.isJarFile
import static java.util.stream.Collectors.toSet
import static org.apache.commons.lang3.StringUtils.isBlank
import static org.apache.commons.lang3.StringUtils.isNotBlank
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECT
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECTS

/**
 * Enforcer rule that will check the bytecode version of each class of each dependency.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.EnforceBytecodeVersion}.
 * @see <a href="http://en.wikipedia.org/wiki/Java_class_file#General_layout" >Java class file general layout</a>
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class EnforceBytecodeVersion extends AbstractResolveDependencies {
    private static final Map<String, Integer> JDK_TO_MAJOR_VERSION_NUMBER_MAPPING = [:]

    /**
     * Default ignores when validating against jdk < 9 because <code>module-info.class</code> will always have level 1.9.
     */
    private static final List<String> DEFAULT_CLASSES_IGNORE_BEFORE_JDK_9 = ['module-info']
    private final Pattern MULTIRELEASE = Pattern.compile('META-INF/versions/(\\d+)/.*')

    static {
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put('1.1', 45)
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put('1.2', 46)
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put('1.3', 47)
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put('1.4', 48)
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put('1.5', 49)

        for (int i = 6; i < 18; i++) {
            JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put(String.valueOf(i), 44 + i)
            JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put('1.' + String.valueOf(i), 44 + i)
        }
    }

    static String renderVersion(int major, int minor) {
        if (minor == 0) {
            for (Map.Entry<String, Integer> entry : JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.entrySet()) {
                if (major == entry.getValue()) {
                    return 'JDK ' + entry.getKey()
                }
            }
        }
        return major + '.' + minor
    }

    /**
     * JDK version as used for example in the maven-compiler-plugin: 1.5, 1.6 and so on. If in need of more precise
     * configuration please see {@link #maxJavaMajorVersionNumber} and {@link #maxJavaMinorVersionNumber} Mandatory if
     * {@link #maxJavaMajorVersionNumber} not specified.
     */
    final Property<String> maxJdkVersion

    /**
     * If unsure, don't use that parameter. Better look at {@link #maxJdkVersion}. Mandatory if {@link #maxJdkVersion}
     * is not specified. see http://en.wikipedia.org/wiki/Java_class_file#General_layout
     */
    final Property<Integer> maxJavaMajorVersionNumber

    /**
     * This parameter is here for potentially advanced use cases, but it seems like it is actually always 0.
     *
     * @see #maxJavaMajorVersionNumber
     * @see http://en.wikipedia.org/wiki/Java_class_file#General_layout
     */
    final Property<Integer> maxJavaMinorVersionNumber

    final ListProperty<Dependency> includes
    final ListProperty<Dependency> excludes

    /**
     * List of classes to ignore. Wildcard at the end accepted
     */
    final ListProperty<String> ignoreClasses

    /**
     * Optional list of dependency configurations to search.
     */
    final ListProperty<String> configurations

    final Property<Boolean> showErrors

    private List<IgnorableDependency> ignorableDependencies = []

    @Inject
    EnforceBytecodeVersion(ObjectFactory objects) {
        super(objects, [AFTER_PROJECT, AFTER_PROJECTS] as EnforcerPhase[])
        ignoreClasses = objects.listProperty(String).convention([])
        configurations = objects.listProperty(String).convention([])
        includes = objects.listProperty(Dependency).convention([])
        excludes = objects.listProperty(Dependency).convention([])
        maxJdkVersion = objects.property(String)
        maxJavaMajorVersionNumber = objects.property(Integer).convention(-1)
        maxJavaMinorVersionNumber = objects.property(Integer).convention(0)
        showErrors = objects.property(Boolean).convention(false)
    }

    @Override
    protected void doValidate(EnforcerContext context) throws EnforcerRuleException {
        super.doValidate(context);

        if (maxJdkVersion.present && maxJavaMajorVersionNumber.getOrElse(-1) != -1) {
            throw illegalArgumentException('Only maxJdkVersion or maxJavaMajorVersionNumber '
                + 'configuration parameters should be set. Not both.')
        }
        if (!maxJdkVersion.present && maxJavaMajorVersionNumber.getOrElse(-1) == -1) {
            throw illegalArgumentException('Exactly one of maxJdkVersion or '
                + 'maxJavaMajorVersionNumber options should be set.')
        }
        if (maxJdkVersion.present) {
            Integer needle = JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.get(maxJdkVersion.get())
            if (!needle) {
                throw illegalArgumentException('Unknown JDK version given. Should be something like ' +
                    '"1.7", "8", "11", "12"')
            }
            maxJavaMajorVersionNumber.set(needle)
            if (needle < 53) {
                IgnorableDependency ignoreModuleInfoDependency = new IgnorableDependency()
                ignoreModuleInfoDependency.applyIgnoreClasses(DEFAULT_CLASSES_IGNORE_BEFORE_JDK_9)
                ignorableDependencies.add(ignoreModuleInfoDependency)
            }
        }
        if (maxJavaMajorVersionNumber.getOrElse(-1) == -1) {
            throw fail('maxJavaMajorVersionNumber must be set in the plugin configuration')
        }
        if (ignoreClasses.get()) {
            IgnorableDependency ignorableDependency = new IgnorableDependency()
            ignorableDependency.applyIgnoreClasses((List<String>) ignoreClasses.get())
            ignorableDependencies.add(ignorableDependency)
        }
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        if (context.enforcerPhase == AFTER_PROJECTS) {
            enforceAfterProjectsEvaluated(context)
        } else {
            enforceAfterProjectEvaluated(context)
        }
    }

    private void enforceAfterProjectsEvaluated(EnforcerContext context) throws EnforcerRuleException {
        Set<ResolvedArtifact> artifactsSeen = []
        Map<ResolvedArtifact, String> allProblems = [:]
        context.project.configurations.each { Configuration c ->
            Map<ResolvedArtifact, String> problemAccumulator = [:]
            handleConfiguration(context, c, problemAccumulator, artifactsSeen)
            allProblems.putAll(problemAccumulator)
        }
        for (Project project : context.project.childProjects.values()) {
            project.configurations.each { Configuration c ->
                Map<ResolvedArtifact, String> problemAccumulator = [:]
                handleConfiguration(context, c, problemAccumulator, artifactsSeen)
                allProblems.putAll(problemAccumulator)
            }
        }
        reportProblems(context, allProblems)
    }

    private void enforceAfterProjectEvaluated(EnforcerContext context) throws EnforcerRuleException {
        Set<ResolvedArtifact> artifactsSeen = []
        Map<ResolvedArtifact, String> allProblems = [:]
        context.project.configurations.each { Configuration c ->
            Map<ResolvedArtifact, String> problemAccumulator = [:]
            handleConfiguration(context, c, problemAccumulator, artifactsSeen)
            allProblems.putAll(problemAccumulator)
        }
        reportProblems(context, allProblems)
    }

    private void handleConfiguration(EnforcerContext context,
                                     Configuration configuration,
                                     Map<ResolvedArtifact, String> problemAccumulator,
                                     Set<ResolvedArtifact> artifactsSeen) {
        if (!(configurations.get().empty) && !(configurations.get().contains(configuration.name))) {
            return
        }

        if (!configuration.canBeResolved) {
            context.logger.debug("Configuration '${configuration.name}' cannot be resolved. Skipping bytecode version check.")
            return
        }

        Configuration cfg = configuration.copyRecursive()
        cfg.resolve()

        Set<ResolvedArtifact> resolvedArtifacts = cfg.resolvedConfiguration.resolvedArtifacts
        resolvedArtifacts.removeAll(artifactsSeen)
        artifactsSeen.addAll(resolvedArtifacts)

        if (resolvedArtifacts) {
            Set<ResolvedArtifact> artifactsToCheck = filterArtifacts(context, resolvedArtifacts)
            // remove those that we have checked already
            artifactsToCheck.removeAll(problemAccumulator.keySet())
            // check artifacts
            Map<ResolvedArtifact, String> problems = checkDependencies(context, artifactsToCheck)
            // add them to the problemAccumulator
            problemAccumulator.putAll(problems)
        }
    }

    private Set<ResolvedArtifact> filterArtifacts(EnforcerContext context, Set<ResolvedArtifact> artifacts) {
        Set<ArtifactMatcher> artifactsToCheck = []
        for (ResolvedArtifact resolvedArtifact : artifacts) {
            File file = resolvedArtifact.file
            context.logger.debug('Checking bytecode version in ' + file)
            if (file == null || !file.exists()) {
                context.logger.debug('Could not find ' + resolvedArtifact + ' at ' + file)
            } else if (isJarFile(resolvedArtifact)) {
                artifactsToCheck.add(new ArtifactMatcher(resolvedArtifact))
            }
        }

        Set<ArtifactMatcher> excludedArtifacts = []
        for (ArtifactMatcher artifactMatcher : artifactsToCheck) {
            for (Dependency exclusion : excludes.get()) {
                if (artifactMatcher.matchesArtifact(exclusion)) {
                    excludedArtifacts.add(artifactMatcher)
                }
            }
            for (Dependency inclusion : includes.get()) {
                if (artifactMatcher.matchesArtifact(inclusion)) {
                    excludedArtifacts.remove(artifactMatcher)
                }
            }
        }

        artifactsToCheck.removeAll(excludedArtifacts)

        return artifactsToCheck.stream()
            .map({ am -> am.resolvedArtifact })
            .collect(toSet())
    }

    protected Map<ResolvedArtifact, String> checkDependencies(EnforcerContext context, Set<ResolvedArtifact> dependencies) throws EnforcerRuleException {
        long beforeCheck = System.currentTimeMillis()
        Map<ResolvedArtifact, String> problematic = [:]
        for (Iterator<ResolvedArtifact> it = dependencies.iterator(); it.hasNext();) {
            ResolvedArtifact artifact = it.next()
            context.logger.debug('Analyzing artifact ' + artifact)
            String problem = isBadArtifact(context, artifact)
            if (problem != null) {
                context.logger.info(problem)
                problematic.put(artifact, problem)
            }
        }
        context.logger.debug('Bytecode version analysis took ' + (System.currentTimeMillis() - beforeCheck) + ' ms')
        return problematic
    }

    private String isBadArtifact(EnforcerContext context, ResolvedArtifact a) throws EnforcerRuleException {
        File f = a.file
        context.logger.debug('isBadArtifact() a:' + a + ' Artifact getFile():' + a.file)
        if (f == null) {
            // This happens if someone defines dependencies instead of dependencyManagement in a pom file
            // which packaging type is pom.
            return null;
        }
        if (!f.name.endsWith('.jar')) {
            return null
        }
        JarFile jarFile = null
        try {
            jarFile = new JarFile(f)
            context.logger.debug(f.name + ' => ' + f.path)
            byte[] magicAndClassFileVersion = new byte[8]
            JAR:
            for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
                JarEntry entry = e.nextElement()
                if (!entry.directory && entry.name.endsWith('.class')) {
                    for (IgnorableDependency i : ignorableDependencies) {
                        if (i.matches(entry.name)) {
                            continue JAR
                        }
                    }

                    InputStream is = null
                    try {
                        is = jarFile.getInputStream(entry)
                        int total = magicAndClassFileVersion.length
                        while (total > 0) {
                            int read =
                                is.read(magicAndClassFileVersion, magicAndClassFileVersion.length - total, total)
                            if (read == -1) {
                                throw new EOFException(f.toString())
                            }

                            total -= read
                        }

                        is.close()
                        is = null
                    } finally {
                        IOUtil.close(is)
                    }

                    int minor = (magicAndClassFileVersion[4] << 8) + magicAndClassFileVersion[5]
                    int major = (magicAndClassFileVersion[6] << 8) + magicAndClassFileVersion[7]

                    // Assuming regex match is more expensive, verify bytecode versions first

                    if ((major > maxJavaMajorVersionNumber.get())
                        || (major == maxJavaMajorVersionNumber.get() && minor > maxJavaMinorVersionNumber.getOrElse(0))) {

                        Matcher matcher = MULTIRELEASE.matcher(entry.getName())

                        if (matcher.matches()) {
                            int expectedMajor = JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.get(matcher.group(1))

                            if (major != expectedMajor) {
                                context.logger.warn('Invalid bytecodeVersion for ' + a + ' : '
                                    + entry.name + ': expected ' + expectedMajor + ', but was ' + major)
                            }
                        } else {
                            return 'Restricted to ' + renderVersion(maxJavaMajorVersionNumber.get(), maxJavaMinorVersionNumber.getOrElse(0)) +
                                ' yet ' + a + ' contains ' + entry.getName() + ' targeted to ' +
                                renderVersion(major, minor)
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw fail('IOException while reading ' + f, e)
        } catch (IllegalArgumentException e) {
            throw fail('Error while reading ' + f, e)
        } finally {
            closeQuietly(context, jarFile)
        }
        return null
    }

    private void closeQuietly(EnforcerContext context, JarFile jarFile) {
        if (jarFile != null) {
            try {
                jarFile.close()
            } catch (IOException ioe) {
                context.logger.warn('Exception caught while closing ' + jarFile.getName(), ioe)
            }
        }
    }

    private void reportProblems(EnforcerContext context, Map<ResolvedArtifact, String> problems) throws EnforcerRuleException {
        if (problems) {
            StringBuilder buf = new StringBuilder()
            String message = message.orNull
            if (isNotBlank(message)) {
                buf.append(message).append(System.lineSeparator())
            }

            problems.each { artifact, error ->
                buf.append('Found Banned Dependency: ')
                    .append(formatArtifact(artifact))
                    .append(System.lineSeparator())
                if (showErrors.get()) {
                    context.logger.warn(error)
                }
            }
            message = buf.append("Disable this rule temporarily with -D${resolveClassName()}.enabled=false and")
                .append(System.lineSeparator())
                .append("invoke 'dependencyInsight' or 'dependencies' to locate the source of the banned dependencies.").toString()

            throw fail(message)
        }
    }

    private String formatArtifact(ResolvedArtifact artifact) {
        StringBuilder b = new StringBuilder(artifact.moduleVersion.id.group)
            .append(':')
            .append(artifact.moduleVersion.id.name)
            .append(':')
            .append(artifact.moduleVersion.id.version)

        if (isNotBlank(artifact.classifier)) {
            b.append(':')
                .append(artifact.classifier)
        }

        b.toString()
    }

    @EqualsAndHashCode(includes = 'resolvedArtifact')
    protected static class ArtifactMatcher {
        final Pattern groupId
        final Pattern artifactId
        final Pattern version
        Pattern classifier

        final ResolvedArtifact resolvedArtifact

        ArtifactMatcher(ResolvedArtifact resolvedArtifact) {
            this.resolvedArtifact = resolvedArtifact
            groupId = Pattern.compile(asRegex(resolvedArtifact.moduleVersion.id.group))
            artifactId = Pattern.compile(asRegex(resolvedArtifact.moduleVersion.id.name))
            version = Pattern.compile(asRegex(resolvedArtifact.moduleVersion.id.version))
            if (isNotBlank(resolvedArtifact.classifier)) {
                classifier = Pattern.compile(asRegex(resolvedArtifact.classifier))
            }
        }

        boolean matchesArtifact(Dependency dependency) {
            return (artifactId == null || artifactId.matcher(dependency.artifactId).matches()) &&
                (groupId == null || groupId.matcher(dependency.groupId).matches()) &&
                (version == null || version.matcher(dependency.version).matches()) &&
                (classifier == null || classifier.matcher(dependency.classifier).matches())
        }
    }

    void ignore(String str) {
        if (isNotBlank(str)) ignoreClasses.add(str)
    }

    void include(String str) {
        includes.add(parseDependency(str))
    }

    void include(Map<String, String> map) {
        includes.add(parseDependency(map))
    }

    void exclude(String str) {
        excludes.add(parseDependency(str))
    }

    void exclude(Map<String, String> map) {
        excludes.add(parseDependency(map))
    }

    @Canonical
    static class Dependency {
        final String groupId
        final String artifactId
        final String version
        final String classifier
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
        } else if (parts.length == 4) {
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