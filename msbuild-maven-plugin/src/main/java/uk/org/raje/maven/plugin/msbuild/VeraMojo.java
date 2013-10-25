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
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.configuration.VeraConfiguration;
import uk.org.raje.maven.plugin.msbuild.parser.VCProject;
import uk.org.raje.maven.plugin.msbuild.streamconsumers.StdoutStreamToLog;

/**
 * This class configures and runs Vera++, a tool to perform coding style analysis on C++ source code. 
 * @see {@link https://bitbucket.org/verateam}
 */
@Mojo( name = VeraMojo.MOJO_NAME, defaultPhase = LifecyclePhase.VERIFY )
public class VeraMojo extends AbstractMSBuildPluginMojo 
{
    /**
     * The name this Mojo declares, also represents the goal.
     */
    public static final String MOJO_NAME = "vera";

    /**
     * The name of the directory created under 'target' where we store CppCheck report files.
     */
    public static final String REPORT_DIRECTORY = "checkstyle-reports";
    
    public static File getVeraExecutablePath( File veraHome ) 
    {
        return new File( veraHome, "bin/vera++.exe" ).getAbsoluteFile();
    }    

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException 
    {
        List<Boolean> allChecksPassed = new ArrayList<Boolean>();
        
        if ( !isVeraEnabled( false ) ) 
        {
            return;
        }
     
        validateVeraConfiguration();

        for ( BuildPlatform platform : platforms ) 
        {
            for ( BuildConfiguration configuration : platform.getConfigurations() )
            {
                for ( VCProject vcProject : getParsedProjects( platform, configuration, 
                        vera.getExcludeProjectRegex() ) )
                {
                    getLog().info( "Running coding style analysis for project " + vcProject.getName() + ", platform=" 
                            + vcProject.getPlatform() + ", configuration=" + vcProject.getConfiguration() );
                    
                    try 
                    {
                        allChecksPassed.add( runVera( vcProject ) );
                    }
                    catch ( MojoExecutionException mee )
                    {
                        getLog().error( mee.getMessage() );
                        throw mee;
                    }
                }
            }
        }
        
        if ( allChecksPassed.contains( false ) )
        {
            throw new MojoFailureException( "Coding style analysis failed" );
        }
        
        getLog().info( "Coding style analysis complete" );
    }
    
    private String getProjectSourcesForStdin( VCProject vcProject ) throws MojoExecutionException
    {
        StringBuilder stringBuilder = new StringBuilder();
        
        for ( File sourceFile : getProjectSources( vcProject, true ) )
        {
            try 
            {
                stringBuilder.append( getRelativeFile( vcProject.getBaseDirectory(), sourceFile ) + "\n" );
            }
            catch ( IOException ioe )
            {
                throw new MojoExecutionException( "Failed to compute relative path for file " + sourceFile, ioe );
            }
        }
        
        return stringBuilder.toString();
    }

    private Writer createVeraReportWriter( File reportFile ) throws MojoExecutionException
    {
        BufferedWriter veraReportWriter;
        
        try 
        {
            FileUtils.forceMkdir( reportFile.getParentFile() );
            veraReportWriter = new BufferedWriter( new FileWriter( reportFile ) );
        } 
        catch ( IOException ioe ) 
        {
            throw new MojoExecutionException( "Failed to create " + VeraConfiguration.TOOL_NAME + " report " 
                    + reportFile, ioe );
        }

        return veraReportWriter;
    }
    
    private CommandLineRunner createVeraRunner( VCProject vcProject, Writer reportWriter )
            throws MojoExecutionException
    {
        VeraRunner veraRunner = new VeraRunner( vera.getVeraHome(), reportWriter, getLog() );
        veraRunner.setWorkingDirectory( projectFile.getParentFile() );
        veraRunner.setStandardInputString( getProjectSourcesForStdin( vcProject ) );
        veraRunner.setProfile( vera.getProfile() );
        
        return veraRunner;
    }

    private Boolean executeVeraRunner( CommandLineRunner veraRunner ) 
        throws MojoExecutionException
    {
        try
        {
            return veraRunner.runCommandLine() == 0;
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "I/O error while executing command line ", ioe );
        }
        catch ( InterruptedException ie )
        {
            throw new MojoExecutionException( "Process interrupted while executing command line ", ie );
        }
    }
    
    private void finaliseReportWriter( Writer reportWriter, File reportFile ) throws MojoExecutionException
    {
        try 
        {
            reportWriter.close();
        } 
        catch ( IOException ioe ) 
        { 
            throw new MojoExecutionException( "Failed to finalise " + VeraConfiguration.TOOL_NAME + " report" 
                    + reportFile, ioe );
        }
    }    

    private Boolean runVera( VCProject vcProject ) throws MojoExecutionException, MojoFailureException
    {
        File reportFile = getReportFile( vcProject );
        Writer reportWriter = createVeraReportWriter( reportFile );
        CommandLineRunner veraRunner = createVeraRunner( vcProject, reportWriter );
        Boolean checksPassed = executeVeraRunner( veraRunner );
        finaliseReportWriter( reportWriter, reportFile );
        
        return checksPassed;
    }
    
    private File getReportFile( VCProject vcProject ) 
    {
        File reportDirectory = new File( vcProject.getFile().getParentFile(), REPORT_DIRECTORY );
        return new File( reportDirectory, vera.getReportName() + "-" + vcProject + ".xml" );
    }
    
    private static class VeraRunner extends CommandLineRunner
    {
        /**
         * Construct the CppCheckRunner
         * @param veraHome the path to CppCheck.exe
         * @param sourcePath the relative path from the working directory to the source files to check
         * @param outputConsumer StreamConsumer for standard output 
         * @param errorConsumer StreamConsumer for standard error
         */
        public VeraRunner( File veraHome, Writer reportWriter, Log log )
        {
            super( new StdoutStreamToLog( log ), new WriterStreamConsumer( reportWriter ) );
            VERA_RUNNER_LOGHANDLER.setLog( log );

            this.veraHome = veraHome;
        }
        
        public void setProfile( String profile ) 
        {
            this.profile = profile;
        }        

        @Override
        protected List<String> getCommandLineArguments() 
        {
            List<String> commandLineArguments = new LinkedList<String>();
            
            commandLineArguments.add( getVeraExecutablePath( veraHome ).toString() );

            commandLineArguments.add( "--root" );
            commandLineArguments.add( "\"" + new File( veraHome, "lib/vera++" ).getAbsolutePath() + "\"" );
            
            commandLineArguments.add( "--profile" );
            commandLineArguments.add( profile );
            
            commandLineArguments.add( "--checkstyle-report" );
            commandLineArguments.add( "-" );
            
            commandLineArguments.add( "--warning" );
            commandLineArguments.add( "--quiet" );
            
            return commandLineArguments;
        }

        /**
         * This handler capture standard Java logging produced by {@link VeraRunner} and relays it to the Maven logger
         * provided by the Mojo. It needs to be static to prevent duplicate log output. 
         * @see {@link LoggingHandler#LoggingHandler(String name)} 
         */
        private static final LoggingHandler VERA_RUNNER_LOGHANDLER = new LoggingHandler( VeraRunner.class.getName() );
        
        private File veraHome;
        private String profile = "full";
    }
    
}