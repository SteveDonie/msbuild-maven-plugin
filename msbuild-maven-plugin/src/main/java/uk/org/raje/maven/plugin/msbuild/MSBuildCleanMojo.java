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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Mojo to execute MSBuild to clean the required platform/configuration pairs.
 */
@Mojo( name = MSBuildCleanMojo.MOJO_NAME,
defaultPhase = LifecyclePhase.CLEAN )
@Execute( phase = LifecyclePhase.CLEAN )
public class MSBuildCleanMojo extends AbstractMSBuildMojo
{
    /**
     * The name this Mojo declares, also represents the goal.
     */
    public static final String MOJO_NAME = "clean";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        dumpConfiguration();
        validateForMSBuild();

        List<String> cleanTargets = new ArrayList<String>();
        // For now we just add the single 'Clean' target which cleans everything
        // We could add each target from the pom as <targetName>:Clean
        // but we don't feel that this mirrors normal Maven clean behaviour
        cleanTargets.add( "Clean" );

        runMSBuild( cleanTargets );
    }

}
