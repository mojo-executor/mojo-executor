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
package org.twdata.maven.mojoexecutor;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.twdata.maven.mojoexecutor.PlexusConfigurationUtils.toXpp3Dom;

/**
 * Executes an arbitrary mojo using a fluent interface.  This is meant to be executed within the context of a Maven mojo. Here is an
 * execution that invokes the dependency plugin:
 * <pre>
 * executeMojo(
 *              plugin(
 *                      groupId("org.apache.maven.plugins"),
 *                      artifactId("maven-dependency-plugin"),
 *                      version("2.0")
 *              ),
 *              goal("copy-dependencies"),
 *              configuration(
 *                      element(name("outputDirectory"), "${project.build.directory}/foo")
 *              ),
 *              executionEnvironment(
 *                      project,
 *                      session,
 *                      pluginManager
 *              )
 *          );
 * </pre>
 */
@SuppressWarnings("WeakerAccess")
public class MojoExecutor {

    private static final Logger logger = LoggerFactory.getLogger( MojoExecutor.class );

   /**
     * Entry point for executing a mojo
     *
     * @param plugin        The plugin to execute
     * @param goal          The goal to execute
     * @param configuration The execution configuration
     * @param env           The execution environment
     * @throws MojoExecutionException If there are any exceptions locating or executing the mojo
     */
    public static void executeMojo(Plugin plugin, String goal, Xpp3Dom configuration, ExecutionEnvironment env)
            throws MojoExecutionException {
        logger.debug("Running executeMojo for {}", plugin);
        if (configuration == null) {
            throw new NullPointerException("configuration may not be null");
        }
        try {
            String executionId = null;
            if (goal != null && goal.length() > 0 && goal.indexOf('#') > -1) {
                int pos = goal.indexOf('#');
                executionId = goal.substring(pos + 1);
                goal = goal.substring(0, pos);
            }

            MavenSession session = env.getMavenSession();

            MavenProject currentProject = env.getMavenSession().getCurrentProject();
            if ((plugin.getVersion() == null || plugin.getVersion().length() == 0) && currentProject != null) {
                PluginManagement pm = currentProject.getPluginManagement();
                if (pm != null) {
                    for (Plugin p : pm.getPlugins()) {
                        if (plugin.getGroupId().equals(p.getGroupId()) && plugin.getArtifactId().equals(p.getArtifactId())) {
                            plugin.setVersion(p.getVersion());
                            break;
                        }
                    }
                }
            }

            PluginDescriptor pluginDescriptor = MavenCompatibilityHelper.loadPluginDescriptor(plugin, env, session);
            MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo(goal);
            if (mojoDescriptor == null) {
                throw new MojoExecutionException("Could not find goal '" + goal + "' in plugin "
                    + plugin.getGroupId() + ":"
                    + plugin.getArtifactId() + ":"
                    + plugin.getVersion());
            }
            MojoExecution exec = mojoExecution(mojoDescriptor, executionId, configuration);
            env.getPluginManager().executeMojo(session, exec);
        } catch (PluginConfigurationException | PluginNotFoundException | InvalidPluginDescriptorException | PluginManagerException | PluginDescriptorParsingException | MojoFailureException | PluginResolutionException e) {
            throw new MojoExecutionException("Unable to execute mojo", e);
        }
    }

    private static MojoExecution mojoExecution(MojoDescriptor mojoDescriptor, String executionId,
                                               Xpp3Dom configuration) {
        if (executionId != null) {
            return new MojoExecution(mojoDescriptor, executionId);
        } else {
            configuration = Xpp3DomUtils.mergeXpp3Dom(configuration, toXpp3Dom(mojoDescriptor.getMojoConfiguration()));
            return new MojoExecution(mojoDescriptor, configuration);
        }
    }

    /**
     * Constructs the {@link ExecutionEnvironment} instance fluently
     *
     * @param mavenProject  The current Maven project
     * @param mavenSession  The current Maven session
     * @param pluginManager The Build plugin manager
     * @return The execution environment
     * @throws NullPointerException if mavenProject, mavenSession or pluginManager
     *                              are null
     */
    public static ExecutionEnvironment executionEnvironment(MavenProject mavenProject,
                                                            MavenSession mavenSession,
                                                            BuildPluginManager pluginManager) {
        return new ExecutionEnvironment(mavenProject, mavenSession, pluginManager);
    }

    /**
     * Constructs the {@link ExecutionEnvironment} instance fluently
     *
     * @param mavenSession  The current Maven session
     * @param pluginManager The Build plugin manager
     * @return The execution environment
     * @throws NullPointerException if mavenProject, mavenSession or pluginManager
     *                              are null
     */
    public static ExecutionEnvironment executionEnvironment(MavenSession mavenSession,
        BuildPluginManager pluginManager) {
        return new ExecutionEnvironment(mavenSession, pluginManager);
    }

    /**
     * Builds the configuration for the goal using Elements
     *
     * @param elements A list of elements for the configuration section
     * @return The elements transformed into the Maven-native XML format
     */
    public static Xpp3Dom configuration(Element... elements) {
        Xpp3Dom dom = new Xpp3Dom("configuration");
        for (Element e : elements) {
            dom.addChild(e.toDom());
        }
        return dom;
    }

    /**
     * Defines the plugin without its version or dependencies.
     *
     * @param groupId    The group id
     * @param artifactId The artifact id
     * @return The plugin instance
     */
    public static Plugin plugin(String groupId, String artifactId) {
        return plugin(groupId, artifactId, null);
    }

    /**
     * Defines a plugin without dependencies.
     *
     * @param groupId    The group id
     * @param artifactId The artifact id
     * @param version    The plugin version
     * @return The plugin instance
     */
    public static Plugin plugin(String groupId, String artifactId, String version) {
        return plugin(groupId, artifactId, version, Collections.emptyList());
    }

    /**
     * Defines a plugin.
     *
     * @param groupId      The group id
     * @param artifactId   The artifact id
     * @param version      The plugin version
     * @param dependencies The plugin dependencies
     * @return The plugin instance
     */
    public static Plugin plugin(String groupId, String artifactId, String version, List<Dependency> dependencies) {
        Plugin plugin = new Plugin();
        plugin.setArtifactId(artifactId);
        plugin.setGroupId(groupId);
        plugin.setVersion(version);
        plugin.setDependencies(dependencies);
        return plugin;
    }

    /**
     * Wraps the group id string in a more readable format
     *
     * @param groupId The value
     * @return The value
     */
    public static String groupId(String groupId) {
        return groupId;
    }

    /**
     * Wraps the artifact id string in a more readable format
     *
     * @param artifactId The value
     * @return The value
     */
    public static String artifactId(String artifactId) {
        return artifactId;
    }

    /**
     * Wraps the version string in a more readable format
     *
     * @param version The value
     * @return The value
     */
    public static String version(String version) {
        return version;
    }

    /**
     * Creates a list of dependencies.
     *
     * @param dependencies the dependencies
     * @return A list of dependencies
     */
    public static List<Dependency> dependencies(Dependency... dependencies) {
        return Arrays.asList(dependencies);
    }

    /**
     * Defines a dependency
     *
     * @param groupId    The group id
     * @param artifactId The artifact id
     * @param version    The plugin version
     * @return the dependency instance
     */
    public static Dependency dependency(String groupId, String artifactId, String version) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        return dependency;
    }

    /**
     * Wraps the goal string in a more readable format
     *
     * @param goal The value
     * @return The value
     */
    public static String goal(String goal) {
        return goal;
    }

    /**
     * Wraps the element name string in a more readable format
     *
     * @param name The value
     * @return The value
     */
    public static String name(String name) {
        return name;
    }

    /**
     * Constructs the element with a textual body
     *
     * @param name  The element name
     * @param value The element text value
     * @return The element object
     */
    public static Element element(String name, String value) {
        return new Element(name, value);
    }

    /**
     * Constructs the element with a textual body and attributes
     *
     * @param name       The element name
     * @param value      The element text value
     * @param attributes The element attributes
     * @return The element object
     */
    public static Element element(String name, String value, Attributes attributes) {
        return new Element(name, value, attributes);
    }

    /**
     * Constructs the element with a textual body and only attribute
     *
     * @param name       The element name
     * @param value      The element text value
     * @param attribute  The element attribute
     * @return The element object
     */
    public static Element element(String name, String value, Attribute attribute) {
        return new Element(name, value, new Attributes(attribute));
    }

    /**
     * Constructs the element containing child elements
     *
     * @param name     The element name
     * @param elements The child elements
     * @return The Element object
     */
    public static Element element(String name, Element... elements) {
        return new Element(name, elements);
    }

    /**
     * Constructs the element containing child elements and attributes
     *
     * @param name       The element name
     * @param attributes The element attributes
     * @param elements   The child elements
     * @return The Element object
     */
    public static Element element(String name, Attributes attributes, Element... elements) {
        return new Element(name, attributes, elements);
    }

    /**
     * Constructs the element containing child elements and only attribute
     *
     * @param name       The element name
     * @param attribute  The element attribute
     * @param elements   The child elements
     * @return The Element object
     */
    public static Element element(String name, Attribute attribute, Element... elements) {
        return new Element(name, new Attributes(attribute), elements);
    }

    /**
     * Constructs the attributes wrapper
     *
     * @param attributes The attributes
     * @return The Attributes object
     */
    public static Attributes attributes(Attribute ... attributes) {
        return new Attributes(attributes);
    }

    /**
     * Constructs the attribute
     *
     * @param name  The attribute name
     * @param value The attribute value
     * @return The Attribute object
     */
    public static Attribute attribute(String name, String value) {
        return new Attribute(name, value);
    }

    /**
     * Element wrapper class for configuration elements
     */
    public static class Element {
        private final Element[] children;
        private final String name;
        private final String text;
        private final Attributes attributes;

        public Element(String name, Element... children) {
            this(name, null, new Attributes(), children);
        }

        public Element(String name, Attributes attributes, Element... children) {
            this(name, null, attributes, children);
        }

        public Element(String name, String text, Element... children) {
            this.name = name;
            this.text = text;
            this.children = children;
            this.attributes = new Attributes();
        }

        public Element(String name, String text, Attributes attributes, Element... children) {
            this.name = name;
            this.text = text;
            this.children = children;
            this.attributes = attributes;
        }

        public Xpp3Dom toDom() {
            Xpp3Dom dom = new Xpp3Dom(name);
            if (text != null) {
                dom.setValue(text);
            }
            for (Element e : children) {
                dom.addChild(e.toDom());
            }
            for(Attribute attribute : attributes.attributes) {
                dom.setAttribute(attribute.name, attribute.value);
            }

            return dom;
        }
    }

    /**
     * Collection of attributes wrapper class
     */
    public static class Attributes {
        private List<Attribute> attributes;

        public Attributes(Attribute ... attributes) {
            this.attributes = Arrays.asList(attributes);
        }
    }

    /**
     * Attribute wrapper class
     */
    public static class Attribute {
        private final String name;
        private final String value;

        public Attribute(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * Collects Maven execution information
     */
    public static class ExecutionEnvironment {
        private final MavenProject mavenProject;
        private final MavenSession mavenSession;
        private final BuildPluginManager pluginManager;

        public ExecutionEnvironment(MavenProject mavenProject,
                                    MavenSession mavenSession,
                                    BuildPluginManager pluginManager) {
            if (mavenSession == null) {
                throw new NullPointerException("mavenSession may not be null");
            }
            if (pluginManager == null) {
                throw new NullPointerException("pluginManager may not be null");
            }
            this.mavenProject = mavenProject;
            this.mavenSession = mavenSession;
            this.pluginManager = pluginManager;
        }

        public ExecutionEnvironment(MavenSession mavenSession,
            BuildPluginManager pluginManager) {
            this ( null, mavenSession, pluginManager);
        }

        public MavenProject getMavenProject() {
            return mavenProject;
        }

        public MavenSession getMavenSession() {
            return mavenSession;
        }

        public BuildPluginManager getPluginManager() {
            return pluginManager;
        }
    }
}
