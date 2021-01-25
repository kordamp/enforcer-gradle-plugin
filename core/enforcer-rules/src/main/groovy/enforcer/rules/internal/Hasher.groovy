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
package enforcer.rules.internal

import groovy.transform.CompileStatic
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.artifacts.ResolvedArtifact

import java.util.jar.JarFile

import static JarUtils.isJarFile

/**
 * Utility class to generate hashes/checksums for binary files.
 * Typically used to generate a hashes for .class files to compare
 * those files for equality.
 * <p>
 * Adapted from {@code org.apache.maven.plugins.enforcer.Hasher}.
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class Hasher {
    /**
     * the path to the .class file. Example: org/apache/maven/Stuff.class
     */
    private final String classFilePath

    /**
     * Constructor.
     *
     * @param classFilePath The path to the .class file. This is the file we'll generate a hash for.
     *                      Example: org/apache/maven/Stuff.class
     */
    Hasher(String classFilePath) {
        this.classFilePath = classFilePath
    }

    /**
     * @param artifact The artifact (example: jar file) which contains the {@link #classFilePath}.
     *                 We'll generate a hash for the class file inside this artifact.
     * @return generate a hash/checksum for the .class file in the provided artifact.
     */
    String generateHash(ResolvedArtifact artifact) {
        File artifactFile = artifact.getFile()
        try {
            if (artifactFile.isDirectory()) {
                return hashForFileInDirectory(artifactFile)
            } else if (isJarFile(artifact)) {
                return hashForFileInJar(artifactFile)
            } else {
                throw new IllegalArgumentException('Expected either a directory or a jar file, but instead received: ' + artifactFile)
            }
        } catch (IOException e) {
            throw new RuntimeException('Problem calculating hash for ' + artifact + ' ' + classFilePath, e)
        }
    }

    private String hashForFileInDirectory(File artifactFile) throws IOException {
        File classFile = new File(artifactFile, classFilePath)
        InputStream inputStream = new FileInputStream(classFile)
        try {
            return DigestUtils.md5Hex(inputStream)
        } finally {
            closeAll(inputStream)
        }
    }

    private String hashForFileInJar(File artifactFile) throws IOException {
        JarFile jar = new JarFile(artifactFile)
        InputStream inputStream = jar.getInputStream(jar.getEntry(classFilePath))
        try {
            return DigestUtils.md5Hex(inputStream)
        } finally {
            closeAll(inputStream, jar)
        }
    }

    private void closeAll(Closeable... closeables) throws IOException {
        IOException firstException = null

        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close()
                } catch (IOException exception) {
                    if (firstException == null) {
                        firstException = exception
                    }
                }
            }
        }

        if (firstException != null) {
            throw firstException
        }
    }
}
