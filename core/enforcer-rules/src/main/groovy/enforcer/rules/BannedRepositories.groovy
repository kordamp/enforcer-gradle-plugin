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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.UrlArtifactRepository
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject

import static org.apache.commons.lang3.StringUtils.isBlank
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECT
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECTS

/**
 * This rule checks that this project's maven session whether have banned repositories.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.BannedRepositories}.
 * Original author: <a href="mailto:wangyf2010@gmail.com">Simon Wang</a>
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class BannedRepositories extends AbstractFilteringEnforcerRule {
    final Property<Boolean> mavenLocalAllowed
    final ListProperty<String> bannedRepositories
    final ListProperty<String> allowedRepositories

    @Inject
    BannedRepositories(ObjectFactory objects) {
        super(objects, [AFTER_PROJECT, AFTER_PROJECTS] as EnforcerPhase[])
        mavenLocalAllowed = objects.property(Boolean).convention(true)
        bannedRepositories = objects.listProperty(String).convention([])
        allowedRepositories = objects.listProperty(String).convention([])
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        Set<? super ArtifactRepository> resultBannedRepos = checkRepositories(
            (Collection<? extends ArtifactRepository>) context.project.repositories.collect(),
            (List<String>) allowedRepositories.get(),
            (List<String>) bannedRepositories.get())
        for (Project project : context.project.childProjects.values()) {
            resultBannedRepos.addAll(checkRepositories(
                (Collection<? extends ArtifactRepository>) project.repositories.collect(),
                (List<String>) allowedRepositories.get(),
                (List<String>) bannedRepositories.get()))
        }

        String repoErrMsg = populateErrorMessage(resultBannedRepos, ' ')
        if (repoErrMsg != null && !isBlank(repoErrMsg.toString())) {
            throw fail(repoErrMsg.toString())
        }
    }

    @CompileDynamic
    private Set<? extends ArtifactRepository> checkRepositories(Collection<? extends ArtifactRepository> repositories,
                                                                List<String> includes,
                                                                List<String> excludes) {
        Set<? super ArtifactRepository> bannedRepos = new LinkedHashSet<>()
        for (ArtifactRepository repo : repositories) {
            if (!mavenLocalAllowed.get() && repo.name =~ 'MavenLocal') {
                bannedRepos.add(repo)
                continue
            }

            if (repo instanceof UrlArtifactRepository) {
                String url = repo.url.toString()
                if (includes.size() > 0 && !match(url, includes)) {
                    bannedRepos.add(repo)
                    continue
                }

                if (excludes.size() > 0 && match(url, excludes)) {
                    bannedRepos.add(repo)
                }
            }
        }

        return bannedRepos
    }

    private boolean match(String url, List<String> patterns) {
        for (String pattern : patterns) {
            if (match(url, pattern)) {
                return true
            }
        }

        return false
    }

    private boolean match(String text, String pattern) {
        String p = pattern.replace('?', '.?').replace('*', '.*?')
        text =~ p ? true : false
    }

    private String populateErrorMessage(Collection<ArtifactRepository> resultBannedRepos, String errorMessagePrefix) {
        StringBuffer errMsg = new StringBuffer('')
        if (!resultBannedRepos.isEmpty()) {
            errMsg.append('Current session contains banned' + errorMessagePrefix
                + 'repository urls, please double check your build files:' + System.lineSeparator()
                + getRepositoryUrlString(resultBannedRepos) + System.lineSeparator())
        }

        return errMsg.toString()
    }

    private String getRepositoryUrlString(Collection<ArtifactRepository> resultBannedRepos) {
        StringBuffer urls = new StringBuffer("")
        for (ArtifactRepository repo : resultBannedRepos) {
            urls.append(repo.getName())
                .append(' - ')
                .append(((UrlArtifactRepository) repo).url)
                .append(System.lineSeparator())
        }
        return urls.toString()
    }
}
