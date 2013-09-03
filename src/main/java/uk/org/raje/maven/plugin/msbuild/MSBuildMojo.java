package uk.org.raje.maven.plugin.msbuild;
/*
 * Copyright 2013 Andrew Everitt, Andrew Heckford, Daniele Daniele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;;

/**
 * Mojo to execute MSBuild to build the required platform/configuration pairs.
 */
@Mojo(name = "msbuild",
        defaultPhase = LifecyclePhase.COMPILE)
//@Execute(phase = LifecyclePhase.COMPILE)
public class MSBuildMojo extends AbstractMojo {

    /**
     * @throws MojoExecutionException if execution fails
     */
    public final void execute() throws MojoExecutionException {
        dumpConfiguration();
        findMSBuild();
        validateProjectFile();
        validatePlatforms();
        validateConfigurations();

        for (String platform: platforms) {
            for (String configuration : configurations) {
                List<String> command = new ArrayList<String>();
                command.add(msbuildPath.getAbsolutePath());
                command.add("/maxcpucount");
                command.add("/p:Configuration=" + configuration);
                command.add("/p:Platform=" + platform);
                //.append("/t:${msbuild-build.targets}")
                command.add(projectFile.getAbsolutePath());

                try {
                    ProcessBuilder pb = new ProcessBuilder(command);
                    Process proc = pb.start();
                    int exitCode = proc.waitFor();
                    getLog().info("MSBuild returned " + exitCode);
                } catch (IOException ioe) {
                    throw new MojoExecutionException(
                            "MSBUild execution failed", ioe);
                } catch (InterruptedException ie) {
                    throw new MojoExecutionException("Interrupted waiting for "
                            + "MSBUild execution to complete", ie);
                }
            }
        }
    }

    /**
     * Attempts to locate MSBuild.
     * First looks at the mojo configuration property, if not found there try
     * the system environment.
     * @throws MojoExecutionException if MSBuild cannot be located
     */
    private void findMSBuild() throws MojoExecutionException {
        if (msbuildPath == null) {
            // not set in configuration try system environment
            String msbuildEnv = System.getenv(ENV_MSBUILD_PATH);
            if (msbuildEnv != null) {
                msbuildPath = new File(msbuildEnv);
            }
        }
        if (msbuildPath != null
                && msbuildPath.exists()
                && msbuildPath.isFile()) {
            getLog().debug("Using MSBuild at " + msbuildPath);
            // TODO: Could check that this is really MSBuild
            return;
        }
        throw new MojoExecutionException(
                "MSBuild could not be found. You need to configure it in the "
                + "plugin configuration section in the pom file using "
                + "<msbuild.path>...</msbuild.path> or "
                + "<properties><msbuild.path>...</msbuild.path></properties> "
                + "or on command-line using -Dmsbuild.path=... or by setting "
                + "the environment variable " + ENV_MSBUILD_PATH);
    }

    /**
     * Check that we have a valid project or solution file.
     * @throws MojoExecutionException if the specified projectFile is invalid.
     */
    private void validateProjectFile() throws MojoExecutionException {
        if (projectFile != null
                && projectFile.exists()
                && projectFile.isFile()) {
            getLog().debug("Project file validated at " + projectFile);
            return;
        }
        String prefix = "Missing projectFile";
        if (projectFile != null) {
            prefix = "The specified projectFile '" + projectFile
                    + "' is not valid";
        }
        throw new MojoExecutionException(prefix
                + ", please check your configuration");
    }

    /**
     * Check that we have a valid set of platforms.
     * If no platforms are configured we apply the default of 'Win32'.
     * @throws MojoExecutionException if the configuration is invalid.
     */
    private void validatePlatforms() throws MojoExecutionException {
        if (platforms == null) {
            platforms = new String[1];
            platforms[0] = "Win32";
        }
    }

    /**
     * Check that we have a valid set of configurations.
     * If no configurations are configured we apply the default of 'Release'.
     * @throws MojoExecutionException if the configuration is invalid.
     */
    private void validateConfigurations() throws MojoExecutionException {
        if (configurations == null) {
            configurations = new String[1];
            configurations[0] = "Release";
        }
    }

    /**
     * Log out configuration values at DEBUG.
     */
    private void dumpConfiguration() {
        getLog().info("MSBuild path: " + msbuildPath);
        getLog().info("Platforms: " + Arrays.toString(platforms));
        getLog().info("Configurations: " + Arrays.toString(configurations));
    }

    /**
     * The name of the environment variable that can store the location of
     * MSBuild.
     */
    private static final String ENV_MSBUILD_PATH = "MSBUILD_PATH";

    /**
     * The path to MSBuild.
     */
    @Parameter(property = "msbuild.path",
            readonly = false,
            required = true)
    private File msbuildPath;

    /**
     * The project or solution file to build.
     */
    @Parameter(property = "projectFile",
            readonly = false,
            required = true)
    private File projectFile;

    /**
     * The set of platforms to build.
     */
    @Parameter(
            defaultValue = "Win32",
            readonly = false,
            required = false)
    private String[] platforms;

    /**
     * The set of configurations to build.
     */
    @Parameter(
            defaultValue = "Release",
            readonly = false,
            required = false)
    private String[] configurations;
}