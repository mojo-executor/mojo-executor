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
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.PlexusConfigurationUtils.toXpp3Dom;

/**
 * Execute a Mojo using the MojoExecutor.
 */
@SuppressWarnings("unused")
@Mojo(name = "execute-mojo", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class MojoExecutorMojo extends AbstractMojo {
    private static final Set<String> lockedKeys = new HashSet<>();

    /**
     * Plugin to execute.
     */
    @Parameter(required = true)
    private Plugin plugin;

    /**
     * Plugin goal to execute.
     */
    @Parameter(required = true)
    private String goal;

    /**
     * Plugin configuration to use in the execution.
     */
    @Parameter
    private XmlPlexusConfiguration configuration;

    /**
     * The project currently being build.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    /**
     * The current Maven session.
     */
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    /**
     * The Maven BuildPluginManager component.
     */
    @Component
    private BuildPluginManager pluginManager;

    /**
     * Disable logging on executed mojos
     */
    @Parameter(defaultValue = "false")
    private boolean quiet;

    /**
     * Enable thread blocking per plugin GA (groupId & artifactId)
     */
    @Parameter(defaultValue = "false")
    private boolean blocking;

    /**
     * Ignore injected maven projetc
     */
    @Parameter(defaultValue = "false")
    private boolean ignoreMavenProject;

    public void execute() throws MojoExecutionException {

        getLog().info("Executing with maven project " + mavenProject + " for session " + mavenSession);

        if (quiet) {
            disableLogging();
        }

        // Blocking case.
        if (blocking) {
            final String key = format("%s:%s", plugin.getGroupId(), plugin.getArtifactId());

            try {
                lock(key);

                getLog().info(String.format("Executing '%s' in blocking mode.", key));

                //Put your code here.
                //For different keys it is executed in parallel.
                //For equal keys it is executed synchronously.
                executeMojoImpl();

            } catch (InterruptedException e) {
                final String failed = "Failed to execute mojo";
                getLog().error(failed, e);
                throw new MojoExecutionException(failed, e);
            } finally {
                unlock(key);

                getLog().info(String.format("Released '%s' block.", key));
            }
        } else {
            // Non blocking case.
            executeMojoImpl();
        }
    }

    private void executeMojoImpl() throws MojoExecutionException {
        executeMojo(plugin, goal, toXpp3Dom(configuration),
                (ignoreMavenProject ?
                        executionEnvironment(mavenSession, pluginManager) :
                        executionEnvironment(mavenProject, mavenSession, pluginManager)));
    }

    private void disableLogging() throws MojoExecutionException {
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
    }

    private void lock(String key) throws InterruptedException {
        synchronized (lockedKeys) {
            while (!lockedKeys.add(key)) {
                lockedKeys.wait();
            }
        }
    }

    private void unlock(String key) {
        synchronized (lockedKeys) {
            lockedKeys.remove(key);
            lockedKeys.notifyAll();
        }
    }
}
