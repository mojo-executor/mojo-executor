The Mojo Executor provides a way to to execute other Mojos (plugins) within a Maven plugin, allowing you to easily create Maven plugins that are composed of other plugins.

Downloads
=========

You can download the jars, source, and javadocs from the Maven 2 repository:

http://mojo-executor.googlecode.com/svn/repo/org/twdata/maven/mojo-executor/

Example Usage
=============

This is how you would execute the "copy-dependencies" goal of the Maven Dependency Plugin programmatically:

``` java
executeMojo(
    plugin(
        groupId("org.apache.maven.plugins"),
        artifactId("maven-dependency-plugin"),
        version("2.0")
    ),
    goal("copy-dependencies"),
    configuration(
        element(name("outputDirectory"), "${project.build.directory}/foo")
    ),
    executionEnvironment(
        project,
        session,
        pluginManager
    )
);
```

The project, session, and pluginManager variables should be injected via the normal Mojo injection:

``` java
/**
 * The Maven Project Object
 *
 * @parameter expression="${project}"
 * @required
 * @readonly
 */
protected MavenProject project;

/**
 * The Maven Session Object
 *
 * @parameter expression="${session}"
 * @required
 * @readonly
 */
protected MavenSession session;

/**
 * The Maven PluginManager Object
 *
 * @component
 * @required
 */
protected PluginManager pluginManager;
```

Maven Repository
================

Add this to your pom.xml:

``` xml
<repositories>
    <repository>
        <id>mojo-executor-repository</id>
        <name>Mojo Executor Repository for Maven</name>
        <url>http://twdata-m2-repository.googlecode.com/svn/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>org.twdata.maven</groupId>
        <artifactId>mojo-executor</artifactId>
        <version>0.2.2</version>
    </dependency>
</dependencies>
```