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
package uk.org.raje.maven.plugin.msbuild.parser;

import java.io.File;
import java.util.List;

/**
 * Bean that holds properties retrieved from parsing a Visual C++ project file. The following properties are available:
 * <ul>
 *      <li>Include Directories (<em>i.e.</em> additional header locations)</li> 
 *      <li>Preprocessor Definitions (<em>i.e.</em> {@code #define}s used during code compilation such as {@code WIN32},
 *      {@code _DEBUG})</li> 
 *      <li>Output Directory (location of the generated output file)</li>
 * </ul>  
 */
public class VCProject 
{
    /**
     * Create a bean that holds properties parsed from a Visual C++ project file.
     * @param name the name of the Visual C++ project
     * @param projectFile the project file
     * @param platform the platform used to parse the project properties (<em>e.g</em>. {@code Win32}, {@code x64})
     * @param configuration the configuration identifier used to parse the project properties (<em>e.g.</em> 
     * {@code Release}, {@code Debug})
     */
    public VCProject( String name, File projectFile, String platform, String configuration ) 
    {
        this.name = name;
        this.projectFile = projectFile;
        this.platform = platform;
        this.configuration = configuration;
    }

    /**
     * Return the project GUID. The GUID automatically generated by Visual Studio when the [project is created.
     * @return the project GUID
     */
    public String getGuid() 
    {
        return guid;
    }
        
    protected void setGuid( String guid ) 
    {
        this.guid = guid;
    }

    /**
     * Return the solution GUID, is this project is part of a solution. The GUID automatically generated by Visual 
     * Studio when the project is created.
     * @return the solution GUID
     */
    public String getSolutionGuid() 
    {
        return solutionGuid;
    }
    
    protected void setSolutionGuid( String solutionGuid ) 
    {
        this.solutionGuid = solutionGuid;
    }

    /**
     * Return the name of the project
     * @return name of the project
     */
    public String getName()
    {
        return name;
    }
 
    /**
     * Return the target name that indicates this project. Only valid for projects that are part of a Visual Studio 
     * solution.
     * @return the target name or null if this project is not part of a Visual Studio solution
     */
    public String getTargetName()
    {
        return targetName;
    }

    /**
     * Create a bean that holds properties parsed from a Visual C++ project file. The use of this constructor is 
     * reserved for the {@link VCSolutionParser}  
     * @param name the name of the Visual C++ project
     * @param projectFile the project file
     */
    protected VCProject( String name, File projectFile ) 
    {
        this.name = name;
        this.projectFile = projectFile;
    }
    
    protected void setTargetName( String targetName )
    {
        this.targetName = targetName;
    }

    /**
     * Return the project file.
     * @return the project file.
     */
    public File getProjectFile() 
    {
        return projectFile;
    }

    /**
     * Retrieve the base directory for the project. If this project is part of a Visual Studio solution, the base 
     * directory is the solution directory; for standalone projects, the base directory is the project directory.
     * @return the base directory for the project
     */
    public File getBaseDirectory() 
    {
        return baseDirectory;
    }
    
    protected void setBaseDirectory( File baseDirectory )
    {
        this.baseDirectory = baseDirectory;
    }
    
    /**
     * Return the configuration identifier used to parse the project properties (<em>e.g.</em> {@code Release}, 
     * {@code Debug})
     * @return the configuration identifier used to parse the project properties
     */
    public String getConfiguration() 
    {
        return configuration;
    }
    
    protected void setConfiguration( String configuration ) 
    {
        this.configuration = configuration;
    }
    
    /**
     * Return the platform used to parse the project properties (<em>e.g</em>. {@code Win32}, {@code x64})
     * @return the platform used to parse the project properties 
     */
    public String getPlatform() 
    {
        return platform;
    }
    
    protected void setPlatform( String platform ) 
    {
        this.platform = platform;
    }

    /**
     * Get the value of the configured 'Output Directory'
     * @return the string value
     */
    public File getOutputDirectory()
    {
        return outputDirectory;
    }
    
    @Override
    public String toString()
    { 
        return name + "-" + platform + "-" + configuration;
    }

    /**
     * Set the stored value for outDir
     * @param outputDirectory the new value
     */
    protected void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    /**
     * Return the include directories for this project (<em>i.e.</em> additional header locations)
     * @return the include directories for this project
     */
    public List<File> getIncludeDirectories() 
    {
        return includeDirectories;
    }

    protected void setIncludeDirectories( List<File> includeDirectories ) 
    {
        this.includeDirectories = includeDirectories;
    }

    /**
     * Return the preprocessor definitions for this project (<em>i.e.</em> {@code #define}s used during code compilation
     * such as {@code WIN32}, {@code _DEBUG})
     * @return the preprocessor definitions for this project
     */
    public List<String> getPreprocessorDefs() 
    {
        return preprocessorDefs;
    }

    protected void setPreprocessorDefs( List<String> preprocessorDefs ) 
    {
        this.preprocessorDefs = preprocessorDefs;
    }

    private String guid;
    private String solutionGuid;
    private String name;
    private String targetName;
    private File projectFile;
    private File baseDirectory;
    private String configuration;
    private String platform;
    private File outputDirectory;
    private List<File> includeDirectories;
    private List<String> preprocessorDefs;
}
