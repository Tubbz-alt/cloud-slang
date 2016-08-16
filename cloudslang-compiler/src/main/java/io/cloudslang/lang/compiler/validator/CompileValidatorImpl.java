package io.cloudslang.lang.compiler.validator;

import io.cloudslang.lang.compiler.SlangSource;
import io.cloudslang.lang.compiler.SlangTextualKeys;
import io.cloudslang.lang.compiler.modeller.model.Executable;
import io.cloudslang.lang.compiler.modeller.model.Flow;
import io.cloudslang.lang.compiler.modeller.model.Step;
import io.cloudslang.lang.entities.bindings.Argument;
import io.cloudslang.lang.entities.bindings.Input;
import io.cloudslang.lang.entities.bindings.Output;
import io.cloudslang.lang.entities.bindings.Result;
import io.cloudslang.lang.entities.utils.ListUtils;
import java.io.Serializable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: bancl
 * Date: 5/12/2016
 */
@Component
public class CompileValidatorImpl extends AbstractValidator implements CompileValidator {

    @Override
    public List<RuntimeException> validateModelWithDependencies(
            Executable executable,
            Map<String, Executable> filteredDependencies) {
        Map<String, Executable> dependencies = new HashMap<>(filteredDependencies);
        dependencies.put(executable.getId(), executable);
        Set<Executable> verifiedExecutables = new HashSet<>();
        return validateModelWithDependencies(executable, dependencies, verifiedExecutables, new ArrayList<RuntimeException>());
    }

    private boolean navigationListContainsKey(List<Map<String, String>> stepNavigations, String resultName) {
        for (Map<String, String> map : stepNavigations) {
            if (map.containsKey(resultName)) return true;
        }
        return false;
    }

    @Override
    public List<RuntimeException> validateModelWithDirectDependencies(Executable executable, Map<String, Executable> directDependencies) {
        List<RuntimeException> errors = new ArrayList<>();
        Flow flow = (Flow) executable;
        Collection<Step> steps = flow.getWorkflow().getSteps();

        for (Step step : steps) {
            errors.addAll(validateStepAgainstItsDependency(flow, step, directDependencies));
        }
        return errors;
    }

    @Override
    public void validateNoDuplicateExecutables(
            Executable currentExecutable,
            SlangSource currentSource,
            Map<Executable, SlangSource> allAvailableExecutables) {
        for (Map.Entry<Executable, SlangSource> entry : allAvailableExecutables.entrySet()) {
            Executable executable = entry.getKey();
            if (currentExecutable.getId().equalsIgnoreCase(executable.getId()) && !currentSource.equals(entry.getValue())) {
                throw new RuntimeException("Duplicate executable found: '" + currentExecutable.getId() + "'" );
            }
        }
    }

    private List<RuntimeException> validateModelWithDependencies(
            Executable executable,
            Map<String, Executable> dependencies,
            Set<Executable> verifiedExecutables,
            List<RuntimeException> errors) {
        //validate that all required & non private parameters with no default value of a reference are provided
        if(!SlangTextualKeys.FLOW_TYPE.equals(executable.getType()) || verifiedExecutables.contains(executable)){
            return errors;
        }
        verifiedExecutables.add(executable);

        Flow flow = (Flow) executable;
        Collection<Step> steps = flow.getWorkflow().getSteps();
        Set<Executable> flowReferences = new HashSet<>();

        for (Step step : steps) {
            Executable reference = dependencies.get(step.getRefId());
            errors.addAll(validateStepAgainstItsDependency(flow, step, dependencies));
            flowReferences.add(reference);
        }

        for (Executable reference : flowReferences) {
            validateModelWithDependencies(reference, dependencies, verifiedExecutables, errors);
        }
        return errors;
    }

    private List<RuntimeException> validateStepAgainstItsDependency(Flow flow, Step step, Map<String, Executable> dependencies) {
        List<RuntimeException> errors = new ArrayList<>();
        String refId = step.getRefId();
        Executable reference = dependencies.get(refId);
        if (reference == null) {
            throw new RuntimeException("Dependency " + step.getRefId() + " used by step: " + step.getName() + " must be supplied for validation");
        }
        errors.addAll(validateMandatoryInputsAreWired(flow, step, reference));
        errors.addAll(validateStepInputNamesDifferentFromDependencyOutputNames(flow, step, reference));
        errors.addAll(validateDependenciesResultsHaveMatchingNavigations(flow, refId, step, reference));
        errors.addAll(validateBreakSection(flow, step, reference));
        return errors;
    }

    private List<RuntimeException> validateBreakSection(Flow parentFlow, Step step, Executable reference) {
        List<RuntimeException> errors = new ArrayList<>();
        @SuppressWarnings("unchecked") // from BreakTransformer
        List<String> breakValues = (List<String>) step.getPostStepActionData().get(SlangTextualKeys.BREAK_KEY);

        if (isForLoop(step, breakValues)) {
            List<String> referenceResultNames = getResultNames(reference);
            Collection<String> nonExistingResults = ListUtils.subtract(breakValues, referenceResultNames);

            if (CollectionUtils.isNotEmpty(nonExistingResults)) {
                errors.add(new IllegalArgumentException("Cannot compile flow '" + parentFlow.getId() +
                        "' since in step '" + step.getName() + "' the results " +
                        nonExistingResults + " declared in '" + SlangTextualKeys.BREAK_KEY +
                        "' section are not declared in the dependency '" + reference.getId() + "' result section."));
            }
        }

        return errors;
    }

    private boolean isForLoop(Step step, List<String> breakValuesList) {
        Serializable forData = step.getPreStepActionData().get(SlangTextualKeys.FOR_KEY);
        return (forData != null) && CollectionUtils.isNotEmpty(breakValuesList);
    }

    private List<RuntimeException> validateDependenciesResultsHaveMatchingNavigations(Flow flow, String refId, Step step, Executable reference) {
        List<RuntimeException> errors = new ArrayList<>();
        if (!StringUtils.equals(refId, flow.getId())) {
            List<Map<String, String>> stepNavigationStrings = step.getNavigationStrings();
            if (reference == null) {
                errors.add(new IllegalArgumentException("Cannot compile flow: \'" + flow.getName() + "\' since for step: \'" + step.getName()
                        + "\', the dependency: \'" + refId + "\' is missing."));
            } else {
                List<Result> refResults = reference.getResults();
                for(Result result : refResults){
                    String resultName = result.getName();
                    if (!navigationListContainsKey(stepNavigationStrings, resultName)){
                        errors.add(new IllegalArgumentException("Cannot compile flow: \'" + flow.getName() +
                                "\' since for step: '" + step.getName() + "\', the result \'" + resultName+
                                "\' of its dependency: \'"+ refId + "\' has no matching navigation"));
                    }
                }
            }
        }
        return errors;
    }

    private List<RuntimeException> validateStepInputNamesDifferentFromDependencyOutputNames(Flow flow, Step step, Executable reference) {
        List<RuntimeException> errors = new ArrayList<>();
        List<Argument> stepArguments = step.getArguments();
        List<Output> outputs = reference.getOutputs();
        String errorMessage = "Cannot compile flow '" + flow.getId() +
                "'. Step '" + step.getName() +
                "' has input '" + NAME_PLACEHOLDER +
                "' with the same name as the one of the outputs of '" + reference.getId() + "'.";
        try {
            validateListsHaveMutuallyExclusiveNames(stepArguments, outputs, errorMessage);
        } catch (RuntimeException e) {
            errors.add(e);
        }
        return errors;
    }

    private List<String> getMandatoryInputNames(Executable executable) {
        List<String> inputNames = new ArrayList<>();
        for (Input input : executable.getInputs()) {
            if (!input.isPrivateInput() && input.isRequired() && (input.getValue() == null || input.getValue().get() == null)) {
                inputNames.add(input.getName());
            }
        }
        return inputNames;
    }

    private List<String> getStepInputNames(Step step) {
        List<String> inputNames = new ArrayList<>();
        for (Argument argument : step.getArguments()) {
            inputNames.add(argument.getName());
        }
        return inputNames;
    }

    private List<String> getInputsNotWired(List<String> mandatoryInputNames, List<String> stepInputNames) {
        List<String> inputsNotWired = new ArrayList<>(mandatoryInputNames);
        inputsNotWired.removeAll(stepInputNames);
        return inputsNotWired;
    }

    private List<RuntimeException> validateMandatoryInputsAreWired(Flow flow, Step step, Executable reference) {
        List<RuntimeException> errors = new ArrayList<>();
        List<String> mandatoryInputNames = getMandatoryInputNames(reference);
        List<String> stepInputNames = getStepInputNames(step);
        List<String> inputsNotWired = getInputsNotWired(mandatoryInputNames, stepInputNames);
        if (!CollectionUtils.isEmpty(inputsNotWired)) {
            errors.add(new IllegalArgumentException(prepareErrorMessageValidateInputNamesEmpty(inputsNotWired, flow, step, reference)));
        }
        return errors;
    }

    private String prepareErrorMessageValidateInputNamesEmpty(List<String> inputsNotWired, Flow flow, Step step, Executable reference) {
        StringBuilder inputsNotWiredBuilder = new StringBuilder();
        for (String inputName : inputsNotWired) {
            inputsNotWiredBuilder.append(inputName);
            inputsNotWiredBuilder.append(", ");
        }
        String inputsNotWiredAsString = inputsNotWiredBuilder.toString();
        if (StringUtils.isNotEmpty(inputsNotWiredAsString)) {
            inputsNotWiredAsString = inputsNotWiredAsString.substring(0, inputsNotWiredAsString.length() - 2);
        }
        return "Cannot compile flow '" + flow.getId() +
                "'. Step '" + step.getName() +
                "' does not declare all the mandatory inputs of its reference." +
                " The following inputs of '" + reference.getId() +
                "' are not private, required and with no default value: " +
                inputsNotWiredAsString + ".";
    }
}
