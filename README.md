The Mojo Executor provides a way to to execute other Mojos (plugins) within a Maven plugin, allowing you to easily create Maven plugins that are composed of other plugins.

Downloads
=========

You can download the jars, source, and javadocs from the Maven 2 repository:

http://twdata-m2-repository.googlecode.com/svn/org/twdata/maven/mojo-executor/

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
        <version>1.0</version>
    </dependency>
</dependencies>
```

License
=======

Copyright 2008-2011 Don Brown

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

Contributors
============

Mojo Executor was originally created by [Don Brown][mrdon] (mrdon@twdata.org).

It is currently maintained by [Tim Moore][TimMoore] (tmoore@incrementalism.net).

Thanks to the following contributors, who have provided patches and other assistance:

-   [Matthew McCullough][matthewmccullough]
-   Gili Tzabari

[mrdon]: https://github.com/mrdon
[TimMoore]: https://github.com/TimMoore/
[matthewmccullough]: https://github.com/matthewmccullough