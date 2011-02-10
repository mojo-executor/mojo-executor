package org.twdata.maven.mojoexecutor.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;

import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;

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
        executeMojo(plugin, goal, configuration.getXpp3Dom(),
                executionEnvironment(mavenProject, mavenSession, pluginManager));
    }
}
