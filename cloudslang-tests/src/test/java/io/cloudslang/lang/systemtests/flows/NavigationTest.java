/*******************************************************************************
* (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Apache License v2.0 which accompany this distribution.
*
* The Apache License is available at
* http://www.apache.org/licenses/LICENSE-2.0
*
*******************************************************************************/

package io.cloudslang.lang.systemtests.flows;

import com.google.common.collect.Sets;
import io.cloudslang.lang.compiler.SlangSource;
import io.cloudslang.lang.entities.CompilationArtifact;
import io.cloudslang.lang.entities.ResultNavigation;
import io.cloudslang.lang.entities.ScoreLangConstants;
import io.cloudslang.lang.entities.SystemProperty;
import io.cloudslang.lang.entities.bindings.values.Value;
import io.cloudslang.lang.entities.bindings.values.ValueFactory;
import io.cloudslang.lang.systemtests.StepData;
import io.cloudslang.lang.systemtests.SystemsTestsParent;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.rules.ExpectedException;


/**
 * Date: 12/4/2014
 *
 * @author Bonczidai Levente
 */
public class NavigationTest extends SystemsTestsParent {

    public static final long FLOW_END_STEP_ID = 0L;
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testComplexNavigationEvenNumber() throws Exception {

        URI resource = getClass().getResource("/yaml/flow_complex_navigation.yaml").toURI();
        URI operation1Python = getClass().getResource("/yaml/check_number.sl").toURI();
        URI operation2Python = getClass().getResource("/yaml/process_even_number.sl").toURI();
        URI operation3Python = getClass().getResource("/yaml/process_odd_number.sl").toURI();
        URI operation4Python = getClass().getResource("/yaml/send_email_mock.sl").toURI();

        Set<SlangSource> path = Sets.newHashSet(SlangSource.fromFile(operation1Python),
                SlangSource.fromFile(operation2Python),
                SlangSource.fromFile(operation3Python),
                SlangSource.fromFile(operation4Python));
        CompilationArtifact compilationArtifact = slang.compile(SlangSource.fromFile(resource), path);

        Map<String, Value> userInputs = new HashMap<>();
        userInputs.put("userNumber", ValueFactory.create("12"));
        userInputs.put("emailHost", ValueFactory.create("emailHost"));
        userInputs.put("emailPort", ValueFactory.create("25"));
        userInputs.put("emailSender", ValueFactory.create("user@host.com"));
        userInputs.put("emailRecipient", ValueFactory.create("user@host.com"));

        Map<String, StepData> steps = triggerWithData(compilationArtifact, userInputs,new HashSet<SystemProperty>()).getSteps();

        Assert.assertEquals("check_number", steps.get(FIRST_STEP_PATH).getName());
        Assert.assertEquals("process_even_number", steps.get(SECOND_STEP_KEY).getName());
    }

    @Test
    public void testComplexNavigationOddNumber() throws Exception {

        URI resource = getClass().getResource("/yaml/flow_complex_navigation.yaml").toURI();
        URI operation1Python = getClass().getResource("/yaml/check_number.sl").toURI();
        URI operation2Python = getClass().getResource("/yaml/process_even_number.sl").toURI();
        URI operation3Python = getClass().getResource("/yaml/process_odd_number.sl").toURI();
        URI operation4Python = getClass().getResource("/yaml/send_email_mock.sl").toURI();

        Set<SlangSource> path = Sets.newHashSet(SlangSource.fromFile(operation1Python),
                                                SlangSource.fromFile(operation2Python),
                                                SlangSource.fromFile(operation3Python),
                                                SlangSource.fromFile(operation4Python));
        CompilationArtifact compilationArtifact = slang.compile(SlangSource.fromFile(resource), path);

        Map<String, Value> userInputs = new HashMap<>();
        userInputs.put("userNumber", ValueFactory.create("13"));
        userInputs.put("emailHost", ValueFactory.create("emailHost"));
        userInputs.put("emailPort", ValueFactory.create("25"));
        userInputs.put("emailSender", ValueFactory.create("user@host.com"));
        userInputs.put("emailRecipient", ValueFactory.create("user@host.com"));

        Map<String, StepData> steps = triggerWithData(compilationArtifact, userInputs,new HashSet<SystemProperty>()).getSteps();

        Assert.assertEquals("check_number", steps.get(FIRST_STEP_PATH).getName());
        Assert.assertEquals("process_odd_number", steps.get(SECOND_STEP_KEY).getName());
    }

    @Test
    public void testComplexNavigationFailure() throws Exception {

        URI resource = getClass().getResource("/yaml/flow_complex_navigation.yaml").toURI();
        URI operation1Python = getClass().getResource("/yaml/check_number.sl").toURI();
        URI operation2Python = getClass().getResource("/yaml/process_even_number.sl").toURI();
        URI operation3Python = getClass().getResource("/yaml/process_odd_number.sl").toURI();
        URI operation4Python = getClass().getResource("/yaml/send_email_mock.sl").toURI();

        Set<SlangSource> path = Sets.newHashSet(SlangSource.fromFile(operation1Python),
                SlangSource.fromFile(operation2Python),
                SlangSource.fromFile(operation3Python),
                SlangSource.fromFile(operation4Python));
        CompilationArtifact compilationArtifact = slang.compile(SlangSource.fromFile(resource), path);

        Map<String, Value> userInputs = new HashMap<>();
        userInputs.put("userNumber", ValueFactory.create("1024"));
        userInputs.put("emailHost", ValueFactory.create("emailHost"));
        userInputs.put("emailPort", ValueFactory.create("25"));
        userInputs.put("emailSender", ValueFactory.create("user@host.com"));
        userInputs.put("emailRecipient", ValueFactory.create("user@host.com"));

        Map<String, StepData> steps = triggerWithData(compilationArtifact, userInputs,new HashSet<SystemProperty>()).getSteps();

        Assert.assertEquals("check_number", steps.get(FIRST_STEP_PATH).getName());
        Assert.assertEquals("send_error_mail", steps.get(SECOND_STEP_KEY).getName());
    }

    @Test
    public void testDefaultSuccessNavigation() throws Exception {

        URI resource = getClass().getResource("/yaml/flow_default_navigation.yaml").toURI();
        URI operationPython = getClass().getResource("/yaml/produce_default_navigation.sl").toURI();
        URI operation2Python = getClass().getResource("/yaml/send_email_mock.sl").toURI();
        URI operation3Python = getClass().getResource("/yaml/check_weather.sl").toURI();

        Set<SlangSource> path = Sets.newHashSet(SlangSource.fromFile(operationPython), SlangSource.fromFile(operation2Python), SlangSource.fromFile(operation3Python));
        CompilationArtifact compilationArtifact = slang.compile(SlangSource.fromFile(resource), path);

        Map<String, Value> userInputs = new HashMap<>();
        userInputs.put("navigationType", ValueFactory.create("success"));
        userInputs.put("emailHost", ValueFactory.create("emailHost"));
        userInputs.put("emailPort", ValueFactory.create("25"));
        userInputs.put("emailSender", ValueFactory.create("user@host.com"));
        userInputs.put("emailRecipient", ValueFactory.create("user@host.com"));

        Map<String, StepData> steps = triggerWithData(compilationArtifact, userInputs,new HashSet<SystemProperty>()).getSteps();

        Assert.assertEquals("produce_default_navigation", steps.get(FIRST_STEP_PATH).getName());
        Assert.assertEquals("check_weather", steps.get(SECOND_STEP_KEY).getName());
    }

    @Test
    public void testDefaultOnFailureNavigation() throws Exception {

        URI resource = getClass().getResource("/yaml/flow_default_navigation.yaml").toURI();
        URI operationPython = getClass().getResource("/yaml/produce_default_navigation.sl").toURI();
        URI operation2Python = getClass().getResource("/yaml/send_email_mock.sl").toURI();
        URI operation3Python = getClass().getResource("/yaml/check_weather.sl").toURI();

        Set<SlangSource> path = Sets.newHashSet(SlangSource.fromFile(operationPython), SlangSource.fromFile(operation2Python), SlangSource.fromFile(operation3Python));
        CompilationArtifact compilationArtifact = slang.compile(SlangSource.fromFile(resource), path);

        Map<String, Value> userInputs = new HashMap<>();
        userInputs.put("navigationType", ValueFactory.create("failure"));
        userInputs.put("emailHost", ValueFactory.create("emailHost"));
        userInputs.put("emailPort", ValueFactory.create("25"));
        userInputs.put("emailSender", ValueFactory.create("user@host.com"));
        userInputs.put("emailRecipient", ValueFactory.create("user@host.com"));

        Map<String, StepData> steps = triggerWithData(compilationArtifact, userInputs,new HashSet<SystemProperty>()).getSteps();

        Assert.assertEquals("produce_default_navigation", steps.get(FIRST_STEP_PATH).getName());
        Assert.assertEquals("send_error_mail", steps.get(SECOND_STEP_KEY).getName());
    }

    @Test
    public void testOnFailureNavigationMoreSteps() throws Exception {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Below 'on_failure' property there should be only one step");

        URI resource = getClass().getResource("/yaml/on_failure_more_steps.sl").toURI();
        URI operationPython = getClass().getResource("/yaml/produce_default_navigation.sl").toURI();
        URI operation2Python = getClass().getResource("/yaml/send_email_mock.sl").toURI();
        URI operation3Python = getClass().getResource("/yaml/check_weather.sl").toURI();

        Set<SlangSource> path = Sets.newHashSet(SlangSource.fromFile(operationPython), SlangSource.fromFile(operation2Python), SlangSource.fromFile(operation3Python));
        slang.compile(SlangSource.fromFile(resource), path);
    }

    @Test
    public void testOnFailureNavigationOneStep() throws Exception {

        URI resource = getClass().getResource("/yaml/on_failure_one_step.sl").toURI();
        URI operation1Python = getClass().getResource("/yaml/check_number.sl").toURI();
        URI operation2Python = getClass().getResource("/yaml/process_even_number.sl").toURI();
        URI operation3Python = getClass().getResource("/yaml/process_odd_number.sl").toURI();
        URI operation4Python = getClass().getResource("/yaml/send_email_mock.sl").toURI();

        Set<SlangSource> path = Sets.newHashSet(SlangSource.fromFile(operation1Python),
                SlangSource.fromFile(operation2Python),
                SlangSource.fromFile(operation3Python),
                SlangSource.fromFile(operation4Python));
        CompilationArtifact compilationArtifact = slang.compile(SlangSource.fromFile(resource), path);

        Map<String, Value> userInputs = new HashMap<>();
        userInputs.put("userNumber", ValueFactory.create("1024"));
        userInputs.put("emailHost", ValueFactory.create("emailHost"));
        userInputs.put("emailPort", ValueFactory.create("25"));
        userInputs.put("emailSender", ValueFactory.create("user@host.com"));
        userInputs.put("emailRecipient", ValueFactory.create("user@host.com"));

        Map<String, StepData> steps = triggerWithData(compilationArtifact, userInputs,new HashSet<SystemProperty>()).getSteps();

        Assert.assertEquals("check_number", steps.get(FIRST_STEP_PATH).getName());
        Assert.assertEquals("send_error_mail", steps.get(SECOND_STEP_KEY).getName());
    }

    @Test
    public void testNavigationOnFailureNotDefined() throws Exception {
        URI resource = getClass().getResource("/yaml/on_failure_not_defined.sl").toURI();
        URI operationPython = getClass().getResource("/yaml/produce_default_navigation.sl").toURI();

        Set<SlangSource> path = Sets.newHashSet(SlangSource.fromFile(operationPython));
        CompilationArtifact compilationArtifact = slang.compile(SlangSource.fromFile(resource), path);
        Map stepNavigationValues = (Map) compilationArtifact.getExecutionPlan()
                .getSteps().get(3L).getActionData().get(ScoreLangConstants.STEP_NAVIGATION_KEY);
        ResultNavigation resultNavigation = (ResultNavigation) stepNavigationValues.get(ScoreLangConstants.FAILURE_RESULT);
        Assert.assertEquals(ScoreLangConstants.FAILURE_RESULT, resultNavigation.getPresetResult());
        Assert.assertEquals(FLOW_END_STEP_ID, resultNavigation.getNextStepId());

        Map<String, Value> userInputs = new HashMap<>();
        userInputs.put("navigationType", ValueFactory.create("failure"));
        userInputs.put("userNumber", ValueFactory.create("1024"));
        userInputs.put("emailHost", ValueFactory.create("emailHost"));
        userInputs.put("emailPort", ValueFactory.create("25"));
        userInputs.put("emailSender", ValueFactory.create("user@host.com"));
        userInputs.put("emailRecipient", ValueFactory.create("user@host.com"));

        Map<String, StepData> steps = triggerWithData(compilationArtifact, userInputs,new HashSet<SystemProperty>()).getSteps();

        // verify
        StepData flowData = steps.get(EXEC_START_PATH);
        StepData stepData = steps.get(FIRST_STEP_PATH);

        Assert.assertEquals(ScoreLangConstants.FAILURE_RESULT, flowData.getResult());
        Assert.assertEquals("default_output_value", flowData.getOutputs().get("default_output"));
        Assert.assertEquals(ScoreLangConstants.FAILURE_RESULT, stepData.getResult());
        Assert.assertEquals("default_output_value", stepData.getOutputs().get("default_output"));
    }

}
