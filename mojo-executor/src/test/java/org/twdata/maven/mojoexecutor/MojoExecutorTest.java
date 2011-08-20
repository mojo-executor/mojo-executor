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
package org.twdata.maven.mojoexecutor;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@RunWith(MockitoJUnitRunner.class)
public class MojoExecutorTest {
    @Mock MavenProject project;
    @Mock MavenSession session;
    @Mock PluginManager pluginManager;
    @Mock PluginDescriptor mavenDependencyPluginDescriptor;
    @Mock MojoDescriptor copyDependenciesMojoDescriptor;

    @Before
    public void setUpMocks() throws Exception {
        when(pluginManager.verifyPlugin(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("2.0")
                ),
                project,
                session.getSettings(),
                session.getLocalRepository()
        )).thenReturn(mavenDependencyPluginDescriptor);
        when(mavenDependencyPluginDescriptor.getMojo(goal("copy-dependencies")))
                .thenReturn(copyDependenciesMojoDescriptor);
    }

    @Test
    public void executeMojoWithoutExecutionIdExecutesMojoWithExplicitConfiguration() throws Exception {

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
        verify(pluginManager)
                .executeMojo(
                        same(project),
                        argThat(is(equalTo(new MojoExecution(
                                copyDependenciesMojoDescriptor,
                                configuration(
                                        element(name("outputDirectory"), "${project.build.directory}/foo")
                                )
                        )))),
                        same(session)
                );
    }

    @Test
    public void executeMojoWithExecutionIdExecutesMojoWithMatchingExecutionId() throws Exception {

        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("2.0")
                ),
                goal("copy-dependencies#execution"),
                configuration(
                        element(name("outputDirectory"), "${project.build.directory}/foo")
                ),
                executionEnvironment(
                        project,
                        session,
                        pluginManager
                )
        );
        verify(pluginManager)
                .executeMojo(
                        same(project),
                        argThat(is(equalTo(new MojoExecution(copyDependenciesMojoDescriptor, "execution")))),
                        same(session)
                );
    }

    private static Matcher<MojoExecution> equalTo(MojoExecution mojoExecution) {
        return new MojoExecutionIsEqual(mojoExecution);
    }

    // This is needed because the equalTo(MojoExecution method above shadows a static import of CoreMatchers.equalTo.
    private static <T> Matcher<? super T> equalTo(T match) {
        return CoreMatchers.equalTo(match);
    }

    private static class MojoExecutionIsEqual extends TypeSafeDiagnosingMatcher<MojoExecution> {
        private final Matcher<? super String> executionId;
        private final Matcher<? super MojoDescriptor> mojoDescriptor;
        private final Matcher<? super Xpp3Dom> configuration;
//        private final Matcher<? super List> forkedExecutions;
//        private final Matcher<? super List> reports;

        MojoExecutionIsEqual(MojoExecution mojoExecution) {
            executionId = is(equalTo(mojoExecution.getExecutionId()));
            mojoDescriptor = is(equalTo(mojoExecution.getMojoDescriptor()));
            configuration = is(equalTo(mojoExecution.getConfiguration()));
//            forkedExecutions = is(equalTo(mojoExecution.getForkedExecutions()));
//            reports = is(equalTo(mojoExecution.getReports()));
        }

        @Override
        protected boolean matchesSafely(MojoExecution mojoExecution, Description mismatchDescription) {
            boolean matches = tryMatch("executionId", executionId, mojoExecution.getExecutionId(), mismatchDescription,
                    true);
            matches = tryMatch("mojoDescriptor", mojoDescriptor, mojoExecution.getMojoDescriptor(), mismatchDescription,
                    matches);
            matches = tryMatch("configuration", configuration, mojoExecution.getConfiguration(), mismatchDescription,
                    matches);
//            matches = tryMatch("forkedExecutions", forkedExecutions, mojoExecution.getForkedExecutions(),
//                    mismatchDescription, matches);
//            matches = tryMatch("reports", reports, mojoExecution.getReports(), mismatchDescription, matches);
            return matches;
        }

        private boolean tryMatch(String name, Matcher<?> matcher, Object item, Description mismatchDescription,
                                 boolean matches) {
            if (!matcher.matches(item)) {
                reportMismatch(name, matcher, item, mismatchDescription, matches);
                return false;
            }
            return matches;
        }

        private void reportMismatch(String name, Matcher<?> matcher, Object item, Description mismatchDescription,
                                    boolean firstMismatch) {
            if (!firstMismatch) mismatchDescription.appendText(", ");
            mismatchDescription.appendText(name).appendText(" ");
            matcher.describeMismatch(item, mismatchDescription);
        }

        public void describeTo(Description description) {
            description.appendText("MojoExecution with executionId ")
                    .appendDescriptionOf(executionId)
                    .appendText(", mojoDescriptor ")
                    .appendDescriptionOf(mojoDescriptor)
                    .appendText(", configuration ")
                    .appendDescriptionOf(configuration)
                    .appendText(", forkedExecutions ")
//                    .appendDescriptionOf(forkedExecutions)
                    .appendText(", reports ");
//                    .appendDescriptionOf(reports);
        }
    }
}
