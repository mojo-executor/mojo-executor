/*
 * Copyright 2008-2013 Don Brown
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

import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.maven.cli.logging.Slf4jConfiguration;
import org.apache.maven.cli.logging.Slf4jConfigurationFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.PlexusConfigurationUtils.toXpp3Dom;

/**
 * Execute a Mojo using the MojoExecutor.
 *
 * @goal execute-mojo
 * @requiresDependencyResolution test
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
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject mavenProject;

    /**
     * The current Maven session.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession mavenSession;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    private BuildPluginManager pluginManager;

    public void execute() throws MojoExecutionException {
        integrationTestSetup();

        getLog().info("Executing with maven project " + mavenProject + " for session " + mavenSession);
        executeMojo(plugin, goal, toXpp3Dom(configuration),
            executionEnvironment(mavenProject, mavenSession, pluginManager));
    }

    /**
     * Used purely to customise the setups for the integration tests.
     *
     * @throws MojoExecutionException
     */
    private void integrationTestSetup() throws MojoExecutionException {
        // Test specific conditionals...
        if (mavenProject.getArtifactId().equals("mojo-executor-test-project-quiet")) {
            // Maven < 3.1
            Logger logger;
            try {
                logger = (Logger) FieldUtils.readField(getLog(), "logger", true);
            } catch (IllegalAccessException e) {
                throw new MojoExecutionException("Unable to access logger field ", e);
            }
            logger.setThreshold(5);

            // Maven >= 3.1
            ILoggerFactory slf4jLoggerFactory = LoggerFactory.getILoggerFactory();
            Slf4jConfiguration slf4jConfiguration = Slf4jConfigurationFactory.getConfiguration(slf4jLoggerFactory);
            slf4jConfiguration.setRootLoggerLevel(Slf4jConfiguration.Level.ERROR);
            slf4jConfiguration.activate();
        } else if (mavenProject.getArtifactId().equals("mojo-executor-test-project-null-maven-project")) {
            mavenProject = null;
        }
    }
}
