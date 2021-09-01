[![Build Status](https://travis-ci.org/TimMoore/mojo-executor.svg?branch=master)](https://travis-ci.org/TimMoore/mojo-executor)
[![Maven Central](https://img.shields.io/maven-central/v/org.twdata.maven/mojo-executor)](https://search.maven.org/artifact/org.twdata.maven/mojo-executor)

The Mojo Executor provides a way to to execute other Mojos (plugins) within a Maven plugin, allowing you to easily create Maven plugins that are composed of other plugins.

Note from the Maintainers
=========================


Tim Moore
---------

I'm no longer maintaining this project actively, as I no longer use it (and have moved away from Maven and Java entirely). It's a simple library that does its job, and a lot of people are using it effectively in its current state.

I am happy to continue reviewing and merging pull requests, and releasing new versions to Maven Central. Most of the contributions so far have come from other people, and I'm very grateful to the people that have helped to improve Mojo Executor.

I do want to make it clear, however, that I won't be personally working on any bug reports or feature requests that come through the issue tracker without a pull request. I hope the community of Mojo Executor users will help answer questions and troubleshoot problems reported there.

If anyone in the community would like to take over as full-time maintainer, let's talk! Email me at tmoore@incrementalism.net and we can set up a Skype call or Google Hangout to discuss it in detail.

Cheers,
&mdash; Tim


Nick Cross
----------

While I am no longer actively using this plugin I am happy to review, assist with contributions, merge PRs and release new versions. I have released the last few versions rolling up all the various fixes and improvements.

Cheers,
&mdash; Nick



News
====

*  1 Sep 2021 &mdash; Mojo Executor 2.3.2 released (by Nick Cross) with various bug fixes and minor improvements
* 21 Nov 2019 &mdash; Mojo Executor 2.3.1 released (by Nick Cross) with various bug fixes and minor improvements
-  4 May 2017 &mdash; Mojo Executor 2.3.0 released (by Nick Cross) with various bug fixes and minor improvements.
- 27 Mar 2014 &mdash; Mojo Executor 1.5.2 released with support for Maven 2 through 3.1.
- 12 Feb 2014 &mdash; I'm looking for a new maintainer for this project. If you're interested, please get in touch!
- 26 Nov 2013 &mdash; Mojo Executor 2.2.0 released with support for attributes in plugin configuration.
- 25 Aug 2013 &mdash; Mojo Executor 2.1.0 released with support for Maven 3.1.

Downloads
=========

You can download the JARs, source, and Javadocs from Maven central:

https://search.maven.org/search?q=g:org.twdata.maven

Example Usage
=============

MojoExecutor defines a number of builder methods that are intended to be imported statically:

``` java
import static org.twdata.maven.mojoexecutor.MojoExecutor.*
```

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
        mavenProject,
        mavenSession,
        pluginManager
    )
);
```

The project, session, and pluginManager variables should be injected via the normal Mojo injection:

``` java
@Component
private MavenProject mavenProject;

@Component
private MavenSession mavenSession;

@Component
private BuildPluginManager pluginManager;
```

An alternative form for the executionEnvironment, ignoring the optional MavenProject, is:
```
    executionEnvironment(
        mavenSession,
        pluginManager
    )
```
You might need to add other annotations to your Mojo class, depending on the needs of your plugin. Annotations declared by Mojos that you execute are _not_ automatically inherited by your enclosing Mojo.

For example, if you are using the `maven-dependency-plugin`, as in this example, you will need to add `@requiresDependencyResolution <scope>` to your class annotations to ensure that Maven resolves the project dependencies before invoking your plugin.

See the [Mojo API Specification][mojo-api] for details on available annotations. Look at the included [example plugin](mojo-executor-maven-plugin/) for an example of use.

Maven Dependency
================

Add this to your pom.xml:

``` xml
<dependencies>
    <dependency>
        <groupId>org.twdata.maven</groupId>
        <artifactId>mojo-executor</artifactId>
        <version>2.3.0</version>
    </dependency>
</dependencies>
```

There are a few versions available, and the best one to use will depend on the version(s) of Maven you need to support:

  - 1.0.1 &mdash; Supports Maven 2.x only
  - 1.5   &mdash; Supports both Maven 2.x and Maven 3.x
  - 2.0.x &mdash; Supports Maven 3.0.x only
  - 2.1.x &mdash; Supports Maven 3.0.x and 3.1.x

License
=======

Copyright 2008-2013 Don Brown

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

Contributors
============

Mojo Executor was originally created by [Don Brown][mrdon] (mrdon@twdata.org).

It is currently maintained by [Tim Moore][TimMoore] (tmoore@incrementalism.net) and [Nick Cross][rnc] (ncross@redhat.com)

Thanks to the following contributors, who have provided patches and other assistance:

-   [Matthew McCullough][matthewmccullough]
-   Gili Tzabari (cowwoc@bbs.darktech.org) &mdash; Maven 3 support
-   [Joseph Walton][josephw] &mdash; support for both Maven 2 and Maven 3 in the same artifact
-   [Olivier Lamy][olamy] &amp; [Robert Munteanu][rombert] &mdash; Maven 3.1 support
-   [Jelmer Kuperus][jelmerk] &mdash; support for plugin dependencies
-   [msavelyev][msavelyev] &amp; [Ivan Dyachenko][ivan-dyachenko] &mdash; support for attributes in Mojo configuration
-   [Christof Schoell][cschoell] &mdash; Maven 3.1 support in the 1.5.x branch

[rnc]: https://github.com/rnc
[mrdon]: https://github.com/mrdon
[TimMoore]: https://github.com/TimMoore/
[matthewmccullough]: https://github.com/matthewmccullough
[josephw]: https://github.com/josephw
[olamy]: https://github.com/olamy
[rombert]: https://github.com/rombert
[jelmerk]: https://github.com/jelmerk
[msavelyev]: https://github.com/msavelyev
[ivan-dyachenko]: https://github.com/ivan-dyachenko
[cschoell]: https://github.com/cschoell

[mojo-api]: http://maven.apache.org/developers/mojo-api-specification.html
