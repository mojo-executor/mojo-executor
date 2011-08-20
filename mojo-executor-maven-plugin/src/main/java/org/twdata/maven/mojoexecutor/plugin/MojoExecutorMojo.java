/*
 * Copyright 2008-2011 Don Brown
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.twdata.maven.mojoexecutor.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.twdata.maven.mojoexecutor.MojoExecutor.ExecutionEnvironment;

import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.PlexusConfigurationUtils.toXpp3Dom;

/**
 * Execute a Mojo using the MojoExecutor.
 *
 * @goal execute-mojo
 */
public class MojoExecutorMojo extends AbstractMojo {
    /**
     * Plugin to execute.
     *
     * @parameter
     * @required
     */
    private Plugin plugin;

    /**
     * Plugin goal to execute.
     *
     * @parameter
     * @required
     */
    private String goal;

    /**
     * Plugin configuration to use in the execution.
     *
     * @parameter
     */
    private XmlPlexusConfiguration configuration;

    /**
     * The project currently being build.
     *
     * @parameter default-value="${project}"
     * @parameter required
     * @readonly
     */
    private MavenProject mavenProject;

    /**
     * The current Maven session.
     *
     * @parameter default-value="${session}"
     * @parameter required
     * @readonly
     */
    private MavenSession mavenSession;

    /**
     * The Maven PluginManager component.
     *
     * @component
     * @required
     */
    private PluginManager pluginManager;

    public void execute() throws MojoExecutionException {
        ExecutionEnvironment env;
        
        try {
            Object o = mavenSession.lookup("org.apache.maven.plugin.BuildPluginManager");
            
            env = executionEnvironment(mavenProject, mavenSession, (BuildPluginManager) o);
        } catch (ComponentLookupException e) {
            env = executionEnvironment(mavenProject, mavenSession, pluginManager);
        }
        
        executeMojo(plugin, goal, toXpp3Dom(configuration),
                env);
    }
}
