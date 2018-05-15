package il.ac.bgu;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.CnfEncodingUtils.ActionState.*;
import static il.ac.bgu.CnfEncodingUtils.*;
import static java.util.stream.Collectors.*;

/**
 * Created by Boris on 01/07/2017.
 */

@Slf4j
public class CnfCompilation {

    private Map<String, Set<ImmutablePair<String, Boolean>>> variablesStateBeforeStepExec;
    private Map<String, Set<ImmutablePair<String, Boolean>>> variablesStateAfterStepExec;


    private TreeMap<Integer, Set<Step>> plan;


    private Map<String, String> reverseActions;


    CnfCompilation(TreeMap<Integer, Set<Step>> plan) {
        this.plan = plan;
        this.variablesStateBeforeStepExec = plan.get(-1).iterator().next().getPopEffs().stream().
                collect(groupingBy(CnfEncodingUtils::createEffKey,
                        mapping(eff -> ImmutablePair.of(createEffId(eff), encodeValue(eff, true)), toSet())));

        this.reverseActions = calculateReversibleActions(plan);

        log.debug("Initialized variable state to: {}", variablesStateBeforeStepExec);
    }

    private Map<String, String> calculateReversibleActions(TreeMap<Integer, Set<Step>> plan) {
        Map<String, List<String>> groupedActions = plan.entrySet().stream().
                filter(i -> i.getKey() != -1).
                flatMap(entry ->
                        entry.getValue().stream().map(step -> {
                            String preconditions = step.getPopPrecs().stream().
                                    map(CnfEncodingUtils::createEffId).
                                    sorted().
                                    collect(Collectors.joining(","));
                            String effects = step.getPopEffs().stream().
                                    map(CnfEncodingUtils::createEffId).
                                    sorted().
                                    collect(Collectors.joining(","));

                            String key = Stream.of(preconditions, effects).sorted().collect(Collectors.joining("#"));
                            return ImmutablePair.of(key, CnfEncodingUtils.encodeActionKey(step, entry.getKey(), HEALTHY));
                        })).collect(Collectors.groupingBy(Pair::getLeft, Collectors.mapping(Pair::getRight, Collectors.toList())));


        Map<String, String> reversibleActions = new HashMap<>();
        groupedActions.values().stream().
                filter(actions -> actions.size() > 1).
                map(actions -> actions.stream().sorted().collect(Collectors.toList())).
                forEach(actions -> {
                    for (int i = 0; i < actions.size() - 1; i++) {
                        reversibleActions.put(actions.get(i), actions.get(i + 1));
                    }
                });
        return reversibleActions;


    }


    public List<List<ImmutablePair<String, Boolean>>> calcInitFacts() {
        return plan.entrySet().stream().filter(i -> i.getKey() == -1).
                flatMap(t -> t.getValue().stream()).
                flatMap(t -> t.getPopEffs().stream()).
                map(popPrecEff -> ImmutablePair.of(createEffId(popPrecEff, 0),
                        Optional.ofNullable(BooleanUtils.toBooleanObject(popPrecEff.getValue())).orElse(true))).
                map(Lists::newArrayList).
                collect(toList());
    }


    public List<List<ImmutablePair<String, Boolean>>> encodeHealthyClauses() {
        return plan.entrySet().stream().
                filter(i -> i.getKey() != -1).
                flatMap(entry -> entry.getValue().stream().flatMap(
                        step -> Stream.of(
                                Lists.newArrayList(encodeActionState(step, entry.getKey(), HEALTHY, true))
                                //,Lists.newArrayList(encodeActionState(step, entry.getKey(), FAILED, true))
                        ))).
                collect(Collectors.toList());
    }


//    public List<List<ImmutablePair<String, Boolean>>> calcFinalFacts() {
//        Integer maxStep = plan.keySet().stream().max(Comparator.naturalOrder()).
//                orElseThrow(() -> new RuntimeException("no max step"));
//
//        return variablesStateAfterStepExec.values().
//                stream().
//                flatMap(Collection::stream).
//                map(var -> ImmutablePair.of(createEffId(var.getLeft(), maxStep), var.getRight())).
//                map(Lists::newArrayList).
//                collect(Collectors.toList());
//    }


    public List<List<ImmutablePair<String, Boolean>>> calcFinalFacts(String... failedSteps) {

        log.debug("Start final values calculation");

        Set<String> failedStepsSet = Arrays.stream(failedSteps).collect(toSet());

        Map<String, Set<ImmutablePair<String, Boolean>>> variablesState = new HashMap<>();

        //initial state
        plan.entrySet().stream().
                filter(i -> i.getKey() == -1).                  //filter initial state
                flatMap(entry -> entry.getValue().stream()).    //get initial state steps
                flatMap(step -> step.getPopEffs().stream()).    //get initial state effects
                forEach(popPrecEff -> variablesState.put(                  //create entry for each effect
                createEffKey(popPrecEff),
                new HashSet<>(Collections.singletonList(ImmutablePair.of(
                        createEffId(popPrecEff),
                        encodeValue(popPrecEff, true)))))
        );


        plan.entrySet().stream().filter(i -> i.getKey() != -1).forEach(stepEntry -> {

            Set<ImmutablePair<String, Boolean>> currentState =
                    variablesState.values().stream().flatMap(Collection::stream).collect(toSet());


            //try to run action for step
            stepEntry.getValue().forEach(action -> {
                //all preconditions valid and step is not failed
                if (action.getPopPrecs().stream().
                        map(eff -> ImmutablePair.of(createEffId(eff), encodeValue(eff, true))).
                        allMatch(currentState::contains) &&
                        !failedStepsSet.contains(action.getUuid())) {

                    action.getPopEffs().forEach(eff ->
                            variablesState.put(createEffKey(eff),
                                    updateVariableState(variablesState.get(createEffKey(eff)), eff)));
                } else {

                    log.info("Failed step: {}", action);
                    action.getPopEffs().forEach(eff -> {
                        Set<ImmutablePair<String, Boolean>> variableValues = variablesState.get(createEffKey(eff));

                        if (variableValues.stream().map(Pair::getKey).noneMatch(t -> t.equals(createEffId(eff)))) {
                            variableValues.add(
                                    ImmutablePair.of(createEffId(eff), encodeValue(eff, false)));
                        }
                    });
                }
            });
        });


        int maxStep = plan.keySet().stream().mapToInt(Integer::intValue).max().getAsInt() + 1;

        List<List<ImmutablePair<String, Boolean>>> finalValues = variablesState.values().stream().flatMap(
                pairSet ->
                        pairSet.stream().map(pair -> ImmutablePair.of(createEffId(pair.getLeft(), maxStep), pair.getRight()))).
                map(Lists::newArrayList).
                collect(toList());

        log.debug("Final Values: \n{}", finalValues.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End final values calculation");


        return finalValues;
    }

    Stream<List<ImmutablePair<String, Boolean>>> calculatePassThroughClauses(Integer stage, Set<Step> actions) {
        //calculate "pass through" variables

        //get effects keys for stage actions
        Set<String> effectKeys = actions.stream().flatMap(k ->
                k.getPopEffs().stream()).map(CnfEncodingUtils::createEffKey).
                collect(toSet());

        log.debug("Start pass through...");

        List<List<ImmutablePair<String, Boolean>>> passThroughValues = variablesStateBeforeStepExec.entrySet().stream().
                filter(entry -> !effectKeys.contains(entry.getKey())).
                map(Map.Entry::getValue).
                flatMap(Collection::stream).
                flatMap(g -> Stream.of(
                        Lists.newArrayList(
                                new ImmutablePair<>(createEffId(g.getKey(), stage), false),
                                new ImmutablePair<>(createEffId(g.getKey(), stage + 1), true)),
                        Lists.newArrayList(
                                new ImmutablePair<>(createEffId(g.getKey(), stage), true),
                                new ImmutablePair<>(createEffId(g.getKey(), stage + 1), false))

                )).collect(toList());

        log.debug("\n{}", passThroughValues.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End pass through");
        return passThroughValues.stream();
    }


    Stream<List<ImmutablePair<String, Boolean>>> calculateHealthyClauses(Integer stage, Set<Step> actions) {
        log.debug("Start add healthy clause");

        List<List<ImmutablePair<String, Boolean>>> resultClauses = actions.stream().flatMap(action -> {

            List<ImmutablePair<String, Boolean>> preconditionList =
                    action.getPopPrecs().stream().
                            map(actionPrec -> ImmutablePair.of(
                                    createEffId(actionPrec, stage),
                                    encodeValue(actionPrec, false))).
                            collect(toList());

            //healthy function
            Stream<ImmutablePair<String, Boolean>> effectStream = action.getPopEffs().stream().flatMap(actionEff -> {
                //effect variable
                return Optional.ofNullable(variablesStateAfterStepExec.get(createEffKey(actionEff))).
                        //add multiple value variable fluents
                                map(stateVars ->
                                stateVars.stream().
                                        map(effPair -> new ImmutablePair<>(
                                                createEffId(effPair.getKey(), stage + 1),
                                                encodeValue(effPair.getValue(), true)))).
                                orElse(Stream.empty());
            });


            return effectStream.map(u ->
                    Stream.concat(
                            preconditionList.stream(),
                            Stream.of(encodeActionState(action, stage, HEALTHY, false), u)).
                            collect(toList())
            );
        }).collect(toList());

        log.debug("healthy clauses\n{}", resultClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End add healthy clause");

        return resultClauses.stream();
    }

    Stream<List<ImmutablePair<String, Boolean>>> calculateActionFailedClauses(Integer stage, Set<Step> actions) {
        log.debug("Start failed clause");

        List<List<ImmutablePair<String, Boolean>>> resultClauses = actions.stream().flatMap(action -> {

            List<ImmutablePair<String, Boolean>> preconditionList =
                    action.getPopPrecs().stream().
                            map(actionPrec -> ImmutablePair.of(
                                    createEffId(actionPrec, stage),
                                    encodeValue(actionPrec, false))).
                            collect(toList());

            Stream<ImmutablePair<String, Boolean>> effectStream = action.getPopEffs().stream().flatMap(actionEff -> {
                //effect variable
                return Optional.ofNullable(variablesStateBeforeStepExec.get(createEffKey(actionEff))).
                        //add multiple value variable fluents
                                map(stateVars ->
                                stateVars.stream().
                                        map(effPair -> new ImmutablePair<>(
                                                createEffId(effPair.getKey(), stage + 1),
                                                encodeValue(effPair.getValue(), true)))).
                                orElse(Stream.empty());
            });


            return effectStream.map(u ->
                    Stream.concat(
                            preconditionList.stream(),
                            Stream.of(encodeActionState(action, stage, FAILED, false), u)).
                            collect(toList())
            );
        }).collect(toList());

        log.debug("failed clauses\n{}", resultClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End failed clause");

        return resultClauses.stream();
    }


//    Stream<List<ImmutablePair<String, Boolean>>> calculateActionFailedClauses(Integer
//                                                                                      stage, Set<Step> actions) {
//
//        log.debug("Start failed clause");
//
//        List<List<ImmutablePair<String, Boolean>>> actionFailedClauses = actions.stream().flatMap(action -> {
//
//            Stream<List<ImmutablePair<String, Boolean>>> preconditionsStream =
//                    Stream.of(action.getPopPrecs().stream().map(l -> new ImmutablePair<>(
//                            createEffId(l, stage), encodeValue(l, false))).collect(toList()));
//
//            Stream<ImmutablePair<String, Boolean>> effectsStream = action.getPopEffs().stream().flatMap(actionEff ->
//                    Optional.ofNullable(variablesStateBeforeStepExec.get(createEffKey(actionEff))).
//                            map(g -> g.stream().flatMap(stateVar ->
//                                    Stream.of(new ImmutablePair<>(
//                                            createEffId(stateVar.getKey(), stage + 1),
//                                            encodeValue(stateVar.getValue(), true))
//                                    ))).
//                            orElse(Stream.empty()));
//
//            return preconditionsStream.flatMap(precs ->
//                    effectsStream.map(eff ->
//                            Stream.concat(
//                                    Stream.concat(precs.stream(),
//                                            Stream.of(new ImmutablePair<>(encodeActionState(action, stage), true))),
//                                    Stream.of(eff)).collect(toList())));
//
//        }).collect(toList());
//
//        log.debug("\n{}", actionFailedClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
//        log.debug("End failed clause");
//        return actionFailedClauses.stream();
//    }


    Stream<List<ImmutablePair<String, Boolean>>> calculateConditionsNotMetClauses(Integer stage, Set<Step> actions) {

        log.debug("Start conditions not met clause");

        List<List<ImmutablePair<String, Boolean>>> resultClauses = actions.stream().flatMap(action -> {

            // unknown -> not(prec1) v not(prec2) => not(unknown) v not(prec1) v not(prec2)
            Stream<List<ImmutablePair<String, Boolean>>> precClauses1 = Stream.of(Stream.concat(
                    Stream.of(encodeActionState(action, stage, UNKNOWN, false)),
                    action.getPopPrecs().stream().flatMap(actionPrec ->
                            Optional.ofNullable(variablesStateBeforeStepExec.get(createEffKey(actionPrec))).
                                    map(g -> g.stream().map(stateVar ->
                                            new ImmutablePair<>(
                                                    createEffId(stateVar.getKey(), stage),
                                                    encodeValue(stateVar.getValue(), false))
                                    )).
                                    orElseGet(Stream::empty))).collect(toList()));


            // not(prec1) v not(prec2) -> unknown => (prec1 v unknown) ^  (prec2 v unknown)
            Stream<ArrayList<ImmutablePair<String, Boolean>>> precClauses2 = action.getPopPrecs().stream().flatMap(actionPrec ->
                    Optional.ofNullable(variablesStateBeforeStepExec.get(createEffKey(actionPrec))).
                            map(g -> g.stream().map(stateVar ->
                                    Lists.newArrayList(
                                            encodeActionState(action, stage, UNKNOWN, true),
                                            new ImmutablePair<>(
                                                    createEffId(stateVar.getKey(), stage),
                                                    encodeValue(stateVar.getValue(), true)))
                            )).
                            orElseGet(Stream::empty));

            Stream<ArrayList<ImmutablePair<String, Boolean>>> effectClauses = action.getPopEffs().stream().flatMap(actionEff ->
                    Optional.ofNullable(variablesStateAfterStepExec.get(createEffKey(actionEff))).
                            map(g -> g.stream().flatMap(stateVar -> Stream.of(
                                    Lists.newArrayList(encodeActionState(action, stage, UNKNOWN, false),
                                            new ImmutablePair<>(
                                                    createEffId(stateVar.getKey(), stage),
                                                    encodeValue(stateVar.getValue(), true)),
                                            new ImmutablePair<>(
                                                    createEffId(stateVar.getKey(), stage + 1),
                                                    encodeValue(stateVar.getValue(), false))),
                                    Lists.newArrayList(
                                            encodeActionState(action, stage, UNKNOWN, false),
                                            new ImmutablePair<>(
                                                    createEffId(stateVar.getKey(), stage),
                                                    encodeValue(stateVar.getValue(), false)),
                                            new ImmutablePair<>(
                                                    createEffId(stateVar.getKey(), stage + 1),
                                                    encodeValue(stateVar.getValue(), true)))
                            ))).
                            orElseGet(Stream::empty));

            return Stream.concat(Stream.concat(precClauses1, precClauses2), effectClauses);

        }).collect(toList());


        log.debug("\n{}", resultClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End conditions not met clause");

        return resultClauses.stream();

    }


    Stream<List<ImmutablePair<String, Boolean>>> addActionHealthyStatusConstraints(Integer stage, Set<Step> actions) {
        return actions.stream().flatMap(action ->
                Stream.of(
                        Lists.newArrayList(
                                encodeActionState(action, stage, HEALTHY, true),
                                encodeActionState(action, stage, FAILED, true),
                                encodeActionState(action, stage, UNKNOWN, true)
                        ),
                        Lists.newArrayList(
                                encodeActionState(action, stage, HEALTHY, true),
                                encodeActionState(action, stage, FAILED, false),
                                encodeActionState(action, stage, UNKNOWN, false)
                        ),
                        Lists.newArrayList(
                                encodeActionState(action, stage, HEALTHY, false),
                                encodeActionState(action, stage, FAILED, true),
                                encodeActionState(action, stage, UNKNOWN, false)
                        ),
                        Lists.newArrayList(
                                encodeActionState(action, stage, HEALTHY, false),
                                encodeActionState(action, stage, FAILED, false),
                                encodeActionState(action, stage, UNKNOWN, true)
                        ),
                        Lists.newArrayList(
                                encodeActionState(action, stage, HEALTHY, false),
                                encodeActionState(action, stage, FAILED, false),
                                encodeActionState(action, stage, UNKNOWN, false)
                        )

                ));
    }


    public List<List<ImmutablePair<String, Boolean>>> compileToCnf() {
        List<List<ImmutablePair<String, Boolean>>> cnfClauses = plan.entrySet().stream().
                filter(i -> i.getKey() != -1).
                flatMap(entry -> {
                    return Stream.concat(
                            Stream.concat(
                                    Stream.concat(
                                            Stream.concat(
                                                    Stream.concat(
                                                            addActionHealthyStatusConstraints(entry.getKey(), entry.getValue()),
                                                            executeStageAndAddFluents(entry.getKey(), entry.getValue())),
                                                    calculatePassThroughClauses(entry.getKey(), entry.getValue())),
                                            calculateHealthyClauses(entry.getKey(), entry.getValue())),
                                    calculateActionFailedClauses(entry.getKey(), entry.getValue())),
                            calculateConditionsNotMetClauses(entry.getKey(), entry.getValue())
                    );
                }).collect(toList());

        return cnfClauses;
    }


    Stream<List<ImmutablePair<String, Boolean>>> executeStageAndAddFluents(Integer stage, Set<Step> actions) {
        log.debug("Add new fluents to the variable state");

        List<ImmutablePair<String, Boolean>> newFluents = new ArrayList<>();

        if (variablesStateAfterStepExec != null) {
            //copy after state to before state
            variablesStateBeforeStepExec = variablesStateAfterStepExec.entrySet().stream().map(entry ->
                    ImmutablePair.of(entry.getKey(), new HashSet<>(entry.getValue()))).
                    collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        }

        //add all preconditions to variable state
        actions.forEach(action -> action.getPopPrecs().forEach(eff -> {
            String effKey = createEffKey(eff);

            ImmutablePair<String, Boolean> precondition =
                    ImmutablePair.of(createEffId(eff), encodeValue(eff, true));

            variablesStateBeforeStepExec.putIfAbsent(effKey, Sets.newHashSet());
            if (variablesStateBeforeStepExec.get(effKey).stream().
                    noneMatch(effect -> effect.getLeft().equals(precondition.getLeft()))) {
                log.debug("Adding precondition to the var state {}", precondition);
                variablesStateBeforeStepExec.get(effKey).add(precondition);
                newFluents.add(ImmutablePair.of(createEffId(eff, stage), encodeValue(eff, true)));
            }
        }));


        //copy before state to after state
        variablesStateAfterStepExec = variablesStateBeforeStepExec.entrySet().stream().map(entry ->
                ImmutablePair.of(entry.getKey(), new HashSet<>(entry.getValue()))).
                collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        //update state according to step effects
        actions.stream().flatMap(action -> action.getPopEffs().stream()).
                forEach(eff -> {
                    String effKey = createEffKey(eff);
                    variablesStateAfterStepExec.putIfAbsent(effKey, Sets.newHashSet());
                    variablesStateAfterStepExec.put(effKey,
                            updateVariableState(variablesStateAfterStepExec.get(effKey), eff));
                });


        //add all preconditions to variable state
        actions.forEach(action -> action.getPopEffs().forEach(eff -> {
            String effKey = createEffKey(eff);

            ImmutablePair<String, Boolean> precondition =
                    ImmutablePair.of(createEffId(eff), encodeValue(eff, false));

            variablesStateBeforeStepExec.putIfAbsent(effKey, Sets.newHashSet());
            if (variablesStateBeforeStepExec.get(effKey).stream().
                    noneMatch(effect -> effect.getLeft().equals(precondition.getLeft()))) {
                log.debug("Adding effect to the var state {}", precondition);
                variablesStateBeforeStepExec.get(effKey).add(precondition);
                newFluents.add(ImmutablePair.of(createEffId(eff, stage), encodeValue(eff, false)));

            }
        }));
        log.debug("Adding effect to the var state {}\n", newFluents.stream().map(Objects::toString).collect(Collectors.joining("\n")));

        return newFluents.stream().map(Lists::newArrayList);
    }


    private Set<ImmutablePair<String, Boolean>> updateVariableState
            (Set<ImmutablePair<String, Boolean>> prevState,
             POPPrecEff eff) {
        return Stream.concat(
                //update previous values
                prevState.stream().
                        filter(pair -> !pair.getKey().equals(createEffId(eff))).
                        map(pair -> ImmutablePair.of(pair.getKey(), encodeValue(eff, false))),
                Stream.of(ImmutablePair.of(createEffId(eff), encodeValue(eff, true)))).
                collect(Collectors.toSet());

    }


    public Map<String, String> getReverseActions() {
        return reverseActions;
    }
}
