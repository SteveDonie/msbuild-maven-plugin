/*
 * Copyright 2013 Andrew Everitt, Andrew Heckford, Daniele Masato
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
package uk.org.raje.maven.plugin.msbuild;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;

/**
 * Abstract base class for MSBuild Mojos.
 */
public abstract class AbstractMSBuildMojo extends AbstractMojo
{
    /**
     * The file extension for zip archives.
     */
    public static final String ZIP_EXTENSION = "zip";

    /**
     * Log out configuration values at DEBUG.
     */
    protected void dumpConfiguration()
    {
        getLog().debug( "MSBuild path: " + msbuildPath );
        getLog().debug( "Platforms: " + platforms );
    }

    /**
     * Check the Mojo configuration provides sufficient data to construct an MSBuild command line.
     * @throws MojoExecutionException is a validation step encounters an error.
     */
    protected final void validateForMSBuild() throws MojoExecutionException
    {
        if ( !MSBuildPackaging.isValid( mavenProject.getPackaging() ) )
        {
            throw new MojoExecutionException( "Please set packaging to one of " + MSBuildPackaging.validPackaging() );
        }
        findMSBuild();
        MojoHelper.validateProjectFile( getLog(), mavenProject.getPackaging(), projectFile );
        platforms = MojoHelper.validatePlatforms( platforms );
    }

    /**
     * Attempts to locate MSBuild.
     * First looks at the mojo configuration property, if not found there try
     * the system environment.
     * @throws MojoExecutionException if MSBuild cannot be located
     */
    protected final void findMSBuild() throws MojoExecutionException
    {
        if ( msbuildPath == null )
        {
            // not set in configuration try system environment
            String msbuildEnv = System.getenv( ENV_MSBUILD_PATH );
            if ( msbuildEnv != null )
            {
                msbuildPath = new File( msbuildEnv );
            }
        }
        if ( msbuildPath != null
                && msbuildPath.exists()
                && msbuildPath.isFile() )
        {
            getLog().debug( "Using MSBuild at " + msbuildPath );
            // TODO: Could check that this is really MSBuild
            return;
        }
        throw new MojoExecutionException(
                "MSBuild could not be found. You need to configure it in the "
                + "plugin configuration section in the pom file using "
                + "<msbuild.path>...</msbuild.path> or "
                + "<properties><msbuild.path>...</msbuild.path></properties> "
                + "or on command-line using -Dmsbuild.path=... or by setting "
                + "the environment variable " + ENV_MSBUILD_PATH );
    }


    /**
     * The name of the environment variable that can store the location of
     * MSBuild.
     */
    private static final String ENV_MSBUILD_PATH = "MSBUILD_PATH";

    /**
     * The MavenProject for the current build.
     */
    @Parameter( defaultValue = "${project}" )
    protected MavenProject mavenProject;

    /**
     * Helper for attaching artifacts provided by the container. 
     */
    @Component
    protected MavenProjectHelper projectHelper;

    /**
     * The path to MSBuild.
     */
    @Parameter( 
            property = "msbuild.path",
            readonly = false,
            required = true )
    protected File msbuildPath;

    /**
     * The project or solution file to build.
     */
    @Parameter( 
            readonly = false,
            required = true )
    protected File projectFile;

    /**
     * The set of platforms to build.
     */
    @Parameter(
            readonly = false,
            required = false )
    protected List<BuildPlatform> platforms;
}
