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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.configuration.SonarConfiguration;
import uk.org.raje.maven.plugin.msbuild.parser.VCProject;

import com.google.common.io.Files;

/**
 * Generates a Sonar configuration file for each platform/configuration pair. The configuration file tells Sonar
 * where to find the source code, the CxxTest reports and the CppCheck report so that it can populate the database. 
 */
@Mojo( name = SonarConfigGeneratorMojo.MOJO_NAME, defaultPhase = LifecyclePhase.VERIFY )
public class SonarConfigGeneratorMojo extends AbstractMSBuildPluginMojo
{
    /**
     * The name this Mojo declares, also represents the goal.
     */
    public static final String MOJO_NAME = "sonar";
    
    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException 
    {
        if ( ! isSonarEnabled() )
        {
            return;
        }
        
        for ( BuildPlatform platform : platforms ) 
        {
            for ( BuildConfiguration configuration : platform.getConfigurations() )
            {
                generateSonarConfiguration( platform, configuration );
            }
        }
        
    }
    
    private Writer createSonarConfigWriter( File configFile ) throws MojoExecutionException
    {
        try 
        {
            Files.createParentDirs( configFile );
            return new BufferedWriter( new FileWriter( configFile ) );
        }
        catch ( IOException ioe )
        { 
            throw new MojoExecutionException( "Could not create " + SonarConfiguration.SONAR_NAME 
                    + " configuration file " + configFile, ioe );
        }
    }
        
    private void finaliseConfigWriter( Writer configWriter, File configFile ) throws MojoExecutionException
    {
        try 
        {
            configWriter.close();
        } 
        catch ( IOException ioe ) 
        { 
            throw new MojoExecutionException( "Could not finalise " + SonarConfiguration.SONAR_NAME + " configuration " 
                    + configFile, ioe );
        }
    }
    
    private void generateSonarConfiguration( BuildPlatform platform, BuildConfiguration configuration )
            throws MojoExecutionException
    {
        String platformConfigPattern = "-*-" + platform.getName() + "-" + configuration.getName();
        File configFile = getSonarConfigFile( platform, configuration );
        Writer configWriter = createSonarConfigWriter( configFile );
        List<VCProject> vcProjects = getParsedProjects( platform, configuration );
        List<String> vcProjectNames = new ArrayList<String>( vcProjects.size() );
        List<File> systemIncludeDirs = getSystemIncludeDirs();
        
        for ( VCProject vcProject: vcProjects )
        {
            vcProjectNames.add( vcProject.getName() );
        }
        
        try 
        {
            configWriter.write( "sonar.projectKey=" + mavenProject.getModel().getGroupId() + ":" 
                    + mavenProject.getModel().getArtifactId() );

            configWriter.write( "sonar.projectName=" + mavenProject.getModel().getArtifactId() );
            configWriter.write( "sonar.projectVersion=" + mavenProject.getModel().getVersion() );
            configWriter.write( "sonar.sources=." );
            configWriter.write( "sonar.langage=c++" );
            configWriter.write( "sonar.modules=" + joinList( vcProjectNames ) );
            
            configWriter.write( "sonar.cxx.cppcheck.reportPath=" + cppCheck.getReportName() + platformConfigPattern
                    + ".xml" );
            
            configWriter.write( "sonar.cxx.xunit.reportPath=" + cxxTest.getReportName() + platformConfigPattern
                    + ".xml" );


            for ( VCProject vcProject: vcProjects )
            {
                generateProjectSonarConfiguration( vcProject, systemIncludeDirs, configWriter ); 
            }
        } 
        catch ( IOException ioe )
        { 
            throw new MojoExecutionException( "Could not write " + SonarConfiguration.SONAR_NAME 
                    + " configuration file " + configFile, ioe );
        }
        
        finaliseConfigWriter( configWriter, configFile );
    }
    
    private void generateProjectSonarConfiguration( VCProject vcProject, List<File> systemIncludeDirs, 
            Writer writer ) throws IOException, MojoExecutionException
    {
        List<File> includeDirectories = new ArrayList<File>( vcProject.getIncludeDirectories() );
        includeDirectories.addAll( systemIncludeDirs );
        
        List<String> preprocessorDefs = new ArrayList<String>( vcProject.getPreprocessorDefs() );
        preprocessorDefs.addAll( sonar.getPreprocessorDefs() );
        
        writer.write( vcProject.getName() + ".sonar.projectBaseDir=" 
                + getRelativeFile( mavenProject.getBasedir(), vcProject.getBaseDirectory() ) );
        
        if ( includeDirectories.size() > 0 )
        {
            List<String> includeDirectoryStr = new ArrayList<String>( includeDirectories.size() );
            
            for ( File includeDirectory : includeDirectories )
            {
                includeDirectoryStr.add( includeDirectory.getPath() );
            }
            
            writer.write( vcProject.getName() + ".sonar.cxx.include_directories=" + joinList( includeDirectoryStr ) );
        }
            
        if ( preprocessorDefs.size() > 0 )
        {
            writer.write( vcProject.getName() + ".sonar.cxx.defines==" + joinList( preprocessorDefs ) );
        }
        
        if ( sonar.getExcludes().size() > 0 )
        {
            writer.write( vcProject.getName() + ".sonar.exclusions==" + joinList( sonar.getExcludes() ) );
        }
    }
    
    private List<File> getSystemIncludeDirs()
    {
        List<File> systemIncludeDirs = new ArrayList<File>();
        String systemIncludeDirsEnv = System.getenv( "INCLUDE" );
        
        if ( systemIncludeDirsEnv != null )
        {
            for ( String includeDir : systemIncludeDirsEnv.split( ";" ) )
            {
                systemIncludeDirs.add( new File( includeDir ) );
            }
        }
        
        return systemIncludeDirs;
    }
            
    private File getSonarConfigFile( BuildPlatform platform, BuildConfiguration configuration )
    {
        return new File( mavenProject.getBuild().getOutputDirectory(), "sonar-configuration-" 
                + platform.getName() + "-" + configuration.getName() + ".properties" );
    }
    
    private String joinList( List<String> list )
    {
        StringBuilder builder = new StringBuilder();
        
        for ( String item : list )
        {
            builder.append( builder.length() > 0 ? "," + item : item );
        }
        
        return builder.toString();
    }

}
