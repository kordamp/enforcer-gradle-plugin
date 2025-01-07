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
package enforcer.rules.internal

import groovy.transform.CompileStatic
import org.gradle.api.artifacts.ResolvedArtifact

/**
 * This class represents a binary class file.
 *
 * The path to the class file should be a relative, file system path to the
 * actual file. Examples:
 *
 *   - CORRECT: org/apache/maven/Stuff.class
 *   - NO:  /org/apache/maven/Stuff.class
 *   - NO:  org.apache.maven.Stuff
 *   - NO:  maven.jar!org.apache.maven.Stuff
 *   - NO:  maven.jar!/org/apache/maven/Stuff.class
 *   - NO:  /path/to/some/directory/org.apache.maven.Stuff
 *   - NO:  /path/to/some/directory/org/apache/maven/Stuff.class
 *
 * The file must exist in either a directory or a jar file, but the path
 * of the directory/jar is not included in the class file path. Rather,
 * it's included in the Artifact.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.ClassFile}.
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class ClassFile {
    /** the path to the .class file. Example: org/apache/maven/Stuff.class */
    private final String classFilePath
    private final ResolvedArtifact artifactThisClassWasFoundIn
    private final Hasher hasher
    private String lazilyComputedHash

    /**
     * Constructor.
     * @param classFilePath path to the class file. Example: org/apache/maven/Stuff.class
     * @param artifactThisClassWasFoundIn the maven artifact the class appeared in (example: a jar file)
     */
    ClassFile(String classFilePath, ResolvedArtifact artifactThisClassWasFoundIn) {
        this.classFilePath = classFilePath
        this.artifactThisClassWasFoundIn = artifactThisClassWasFoundIn
        this.hasher = new Hasher(classFilePath)
    }

    /**
     * @return the path to the .class file. Example: org/apache/maven/Stuff.class
     */
    String getClassFilePath() {
        return classFilePath
    }

    /**
     * @return the maven artifact the class appeared in (example: a jar file)
     */
    ResolvedArtifact getArtifactThisClassWasFoundIn() {
        return artifactThisClassWasFoundIn
    }

    /**
     * @return a hash or checksum of the binary file. If two files have the same hash
     * then they are the same binary file.
     */
    String getHash() {
        if (lazilyComputedHash == null) {
            lazilyComputedHash = hasher.generateHash(artifactThisClassWasFoundIn)
        }

        return lazilyComputedHash
    }

}
