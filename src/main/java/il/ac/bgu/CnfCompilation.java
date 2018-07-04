package il.ac.bgu;

import com.google.common.collect.ImmutableSet;
import il.ac.bgu.failureModel.FailureModelFunction;
import il.ac.bgu.failureModel.NewNoEffectFailureModel;
import lombok.extern.slf4j.Slf4j;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.CnfCompilationUtils.updateVariableState;
import static il.ac.bgu.CnfEncodingUtils.ActionState.*;
import static il.ac.bgu.CnfEncodingUtils.*;
import static java.util.stream.Collectors.*;

/**
 * Created by Boris on 01/07/2017.
 */

@Slf4j
public class CnfCompilation {

    public static final String UNDEFINED = "UNDEFINED";
    private Map<String, ImmutableSet<ImmutablePair<String, Boolean>>> variablesStateBeforeStepExec;
    private Map<String, ImmutableSet<ImmutablePair<String, Boolean>>> variablesStateAfterStepExec;


    private TreeMap<Integer, Set<Step>> plan;

    private FailureModelFunction failureModel;


    CnfCompilation(TreeMap<Integer, Set<Step>> plan,
                   FailureModelFunction failureModel) {
        this.plan = plan;
//        this.variablesStateBeforeStepExec = plan.get(-1).iterator().next().getPopEffs().stream().
//                collect(groupingBy(CnfEncodingUtils::createEffKey,
//                        mapping(eff -> ImmutablePair.of(createEffId(eff, eff.getValue()), true), toSet())));
        this.failureModel = failureModel;
        this.variablesStateBeforeStepExec = plan.get(-1).iterator().next().getPopEffs().stream().
                flatMap(eff -> Stream.of(
                        ImmutableTriple.of(eff, eff.getValue(), true),
                        ImmutableTriple.of(eff, UNDEFINED, false)
                )).
                collect(groupingBy(el -> CnfEncodingUtils.createEffKey(el.getLeft()),
                        mapping(el -> ImmutablePair.of(createEffId(el.getLeft(), el.getMiddle()), el.getRight()), ImmutableSet.toImmutableSet())));

        log.debug("Initialized variable state to: {}", variablesStateBeforeStepExec);
    }


    public ImmutableSet<ImmutableSet<ImmutablePair<String, Boolean>>> calcInitFacts() {
        return plan.entrySet().stream().filter(i -> i.getKey() == -1).
                flatMap(t -> t.getValue().stream()).
                flatMap(t -> t.getPopEffs().stream()).
                flatMap(eff -> Stream.of(
                        ImmutablePair.of(createEffInstance(eff, 0, eff.getValue()), true),
                        ImmutablePair.of(createEffInstance(eff, 0, UNDEFINED), false)
                )).
                map(ImmutableSet::of).
                collect(ImmutableSet.toImmutableSet());
    }


    public ImmutableSet<ImmutableSet<ImmutablePair<String, Boolean>>> encodeHealthyClauses() {
        return plan.entrySet().stream().
                filter(i -> i.getKey() != -1).
                flatMap(entry -> entry.getValue().stream().flatMap(
                        step -> Stream.of(
                                ImmutableSet.of(encodeActionState(step, entry.getKey(), HEALTHY, true))
                                //,Lists.newArrayList(encodeActionState(step, entry.getKey(), FAILED, true))
                        ))).
                collect(ImmutableSet.toImmutableSet());
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


    public ImmutableSet<ImmutableSet<ImmutablePair<String, Boolean>>> calcFinalFacts(String... failedSteps) {

        log.debug("Start final values calculation");

        ImmutableSet<ImmutableSet<ImmutablePair<String, Boolean>>> finalValues =
                new FinalVariableStateCalc(plan, new NewNoEffectFailureModel()).getFinalVariableState(failedSteps).stream()
                        .map(t -> ImmutableSet.of(ImmutablePair.of(t.getLeft().formatVariable(), t.getRight())))
                        .collect(ImmutableSet.toImmutableSet());
        log.debug("Final Values: \n{}", finalValues.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End final values calculation");


        return finalValues;
    }

    Stream<ImmutableSet<ImmutablePair<String, Boolean>>> calculatePassThroughClauses(Integer stage, Set<Step> actions) {
        //calculate "pass through" variables

        //get effects keys for stage actions
        Set<String> effectKeys = actions.stream().flatMap(k ->
                k.getPopEffs().stream()).map(CnfEncodingUtils::createEffKey).
                collect(toSet());

        log.debug("Start pass through...");

        ImmutableSet<ImmutableSet<ImmutablePair<String, Boolean>>> passThroughValues = variablesStateBeforeStepExec.entrySet().stream().
                filter(entry -> !effectKeys.contains(entry.getKey())).
                map(Map.Entry::getValue).
                flatMap(Collection::stream).
                flatMap(g -> Stream.of(
                        ImmutableSet.of(
                                new ImmutablePair<>(createEffInstance(g.getKey(), stage), false),
                                new ImmutablePair<>(createEffInstance(g.getKey(), stage + 1), true)),
                        ImmutableSet.of(
                                new ImmutablePair<>(createEffInstance(g.getKey(), stage), true),
                                new ImmutablePair<>(createEffInstance(g.getKey(), stage + 1), false))

                )).collect(ImmutableSet.toImmutableSet());

        log.debug("\n{}", passThroughValues.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End pass through");
        return passThroughValues.stream();
    }


    Stream<ImmutableSet<ImmutablePair<String, Boolean>>> calculateHealthyClauses(Integer stage, Set<Step> actions) {
        log.debug("Start add healthy clause");

        ImmutableSet<ImmutableSet<ImmutablePair<String, Boolean>>> resultClauses = actions.stream().flatMap(action -> {

            ImmutableSet<ImmutablePair<String, Boolean>> preconditionList =
                    action.getPopPrecs().stream().
                            map(actionPrec -> ImmutablePair.of(
                                    createEffInstance(actionPrec, stage, actionPrec.getValue()),
                                    encodeValue(actionPrec, false))).
                            collect(ImmutableSet.toImmutableSet());

            //healthy function
            Stream<ImmutablePair<String, Boolean>> effectStream = action.getPopEffs().stream().flatMap(actionEff -> {
                //effect variable
                return Optional.ofNullable(variablesStateAfterStepExec.get(createEffKey(actionEff))).
                        //add multiple value variable fluents
                                map(stateVars ->
                                stateVars.stream().
                                        map(effPair -> new ImmutablePair<>(
                                                createEffInstance(effPair.getKey(), stage + 1),
                                                encodeValue(effPair.getValue(), true)))).
                                orElse(Stream.empty());
            });


            return effectStream.map(u ->
                    Stream.concat(
                            preconditionList.stream(),
                            Stream.of(encodeActionState(action, stage, HEALTHY, false), u)).
                            collect(ImmutableSet.toImmutableSet())
            );
        }).collect(ImmutableSet.toImmutableSet());

        log.debug("healthy clauses\n{}", resultClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End add healthy clause");

        return resultClauses.stream();
    }

    Stream<ImmutableSet<ImmutablePair<String, Boolean>>> calculateActionFailedClauses(
            Integer stage, Set<Step> actions) {
        log.debug("Start failed clause");

        ImmutableSet<ImmutableSet<ImmutablePair<String, Boolean>>> resultClauses = actions.stream().flatMap(action -> {

            ImmutableSet<ImmutablePair<String, Boolean>> preconditionList =
                    action.getPopPrecs().stream().
                            map(actionPrec -> ImmutablePair.of(
                                    createEffInstance(actionPrec, stage, actionPrec.getValue()),
                                    encodeValue(actionPrec, false))).
                            collect(ImmutableSet.toImmutableSet());

            Stream<ImmutablePair<String, Boolean>> effectStream = action.getPopEffs().stream().flatMap(actionEff -> {
                //effect variable
                String effKey = createEffKey(actionEff);
                return failureModel.apply(stage, effKey,
                        variablesStateBeforeStepExec.get(effKey),
                        variablesStateAfterStepExec.get(effKey));
            });


            return effectStream.map(u ->
                    Stream.concat(
                            preconditionList.stream(),
                            Stream.of(encodeActionState(action, stage, FAILED, false), u)).
                            collect(ImmutableSet.toImmutableSet())
            );
        }).collect(ImmutableSet.toImmutableSet());

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


    Stream<ImmutableSet<ImmutablePair<String, Boolean>>> calculateConditionsNotMetClauses(Integer stage, Set<Step> actions) {

        log.debug("Start conditions not met clause");

        ImmutableSet<ImmutableSet<ImmutablePair<String, Boolean>>> resultClauses = actions.stream().flatMap(action -> {

            // unknown -> not(prec1) v not(prec2) => not(unknown) v not(prec1) v not(prec2)
            Stream<ImmutableSet<ImmutablePair<String, Boolean>>> precClauses1 = Stream.of(Stream.concat(
                    Stream.of(encodeActionState(action, stage, UNKNOWN, false)),
                    action.getPopPrecs().stream().flatMap(actionPrec ->
                            Optional.ofNullable(variablesStateBeforeStepExec.get(createEffKey(actionPrec))).
                                    map(g -> g.stream().map(stateVar ->
                                            new ImmutablePair<>(
                                                    createEffInstance(stateVar.getKey(), stage),
                                                    encodeValue(stateVar.getValue(), false))
                                    )).
                                    orElseGet(Stream::empty))).collect(ImmutableSet.toImmutableSet()));


            // not(prec1) v not(prec2) -> unknown => (prec1 v unknown) ^  (prec2 v unknown)
            Stream<ImmutableSet<ImmutablePair<String, Boolean>>> precClauses2 = action.getPopPrecs().stream().flatMap(actionPrec ->
                    Optional.ofNullable(variablesStateBeforeStepExec.get(createEffKey(actionPrec))).
                            map(g -> g.stream().map(stateVar ->
                                    ImmutableSet.of(
                                            encodeActionState(action, stage, UNKNOWN, true),
                                            new ImmutablePair<>(
                                                    createEffInstance(stateVar.getKey(), stage),
                                                    encodeValue(stateVar.getValue(), true)))
                            )).
                            orElseGet(Stream::empty));

            Stream<ImmutableSet<ImmutablePair<String, Boolean>>> effectClauses = action.getPopEffs().stream().flatMap(actionEff ->
                    Optional.ofNullable(variablesStateAfterStepExec.get(createEffKey(actionEff))).
                            map(g -> g.stream().flatMap(stateVar -> Stream.of(
                                    ImmutableSet.of(encodeActionState(action, stage, UNKNOWN, false),
                                            new ImmutablePair<>(
                                                    createEffInstance(stateVar.getKey(), stage),
                                                    encodeValue(stateVar.getValue(), true)),
                                            new ImmutablePair<>(
                                                    createEffInstance(stateVar.getKey(), stage + 1),
                                                    encodeValue(stateVar.getValue(), false))),
                                    ImmutableSet.of(
                                            encodeActionState(action, stage, UNKNOWN, false),
                                            new ImmutablePair<>(
                                                    createEffInstance(stateVar.getKey(), stage),
                                                    encodeValue(stateVar.getValue(), false)),
                                            new ImmutablePair<>(
                                                    createEffInstance(stateVar.getKey(), stage + 1),
                                                    encodeValue(stateVar.getValue(), true)))
                            ))).
                            orElseGet(Stream::empty));

            return Stream.concat(Stream.concat(precClauses1, precClauses2), effectClauses);

        }).collect(ImmutableSet.toImmutableSet());


        log.debug("\n{}", resultClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End conditions not met clause");

        return resultClauses.stream();

    }


    Stream<ImmutableSet<ImmutablePair<String, Boolean>>> addActionHealthyStatusConstraints(Integer stage, Set<Step> actions) {
        return actions.stream().flatMap(action ->
                Stream.of(
                        ImmutableSet.of(
                                encodeActionState(action, stage, HEALTHY, true),
                                encodeActionState(action, stage, FAILED, true),
                                encodeActionState(action, stage, UNKNOWN, true)
                        ),
                        ImmutableSet.of(
                                encodeActionState(action, stage, HEALTHY, true),
                                encodeActionState(action, stage, FAILED, false),
                                encodeActionState(action, stage, UNKNOWN, false)
                        ),
                        ImmutableSet.of(
                                encodeActionState(action, stage, HEALTHY, false),
                                encodeActionState(action, stage, FAILED, true),
                                encodeActionState(action, stage, UNKNOWN, false)
                        ),
                        ImmutableSet.of(
                                encodeActionState(action, stage, HEALTHY, false),
                                encodeActionState(action, stage, FAILED, false),
                                encodeActionState(action, stage, UNKNOWN, true)
                        ),
                        ImmutableSet.of(
                                encodeActionState(action, stage, HEALTHY, false),
                                encodeActionState(action, stage, FAILED, false),
                                encodeActionState(action, stage, UNKNOWN, false)
                        )

                ));
    }


    public ImmutableSet<ImmutableSet<ImmutablePair<String, Boolean>>> compileToCnf() {
        ImmutableSet<ImmutableSet<ImmutablePair<String, Boolean>>> cnfClauses = plan.entrySet().stream().
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
                }).collect(ImmutableSet.toImmutableSet());

        return cnfClauses;
    }


    Stream<ImmutableSet<ImmutablePair<String, Boolean>>> executeStageAndAddFluents(Integer stage, Set<Step> actions) {
        log.debug("Add new fluents to the variable state");

        Set<ImmutablePair<String, Boolean>> newFluents = new HashSet<>();

        if (variablesStateAfterStepExec != null) {
            //copy after state to before state
            variablesStateBeforeStepExec = variablesStateAfterStepExec.entrySet().stream().
                    collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        //add all preconditions to variable state
        actions.forEach(action -> action.getPopPrecs().forEach(eff -> {
            String effKey = createEffKey(eff);

            ImmutablePair<String, Boolean> precondition =
                    ImmutablePair.of(createEffId(eff, eff.getValue()), encodeValue(eff, true));

            variablesStateBeforeStepExec.putIfAbsent(effKey, ImmutableSet.of());
            if (variablesStateBeforeStepExec.get(effKey).stream().
                    noneMatch(effect -> effect.getLeft().equals(precondition.getLeft()))) {
                log.debug("Adding precondition to the var state {}", precondition);

                variablesStateBeforeStepExec.put(effKey, ImmutableSet.<ImmutablePair<String, Boolean>>builder().
                        addAll(variablesStateBeforeStepExec.get(effKey)).
                        add(precondition).build());
                newFluents.add(ImmutablePair.of(createEffInstance(eff, stage, eff.getValue()), encodeValue(eff, true)));
            }
        }));


        //copy before state to after state
        variablesStateAfterStepExec = variablesStateBeforeStepExec.entrySet().stream().
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        //update state according to step effects
        actions.stream().flatMap(action -> action.getPopEffs().stream()).
                forEach(eff -> {
                    String effKey = createEffKey(eff);
                    variablesStateAfterStepExec.putIfAbsent(effKey, ImmutableSet.of());
                    variablesStateAfterStepExec.put(effKey,
                            updateVariableState(variablesStateAfterStepExec.get(effKey), createEffId(eff, eff.getValue())));
                });


        //add all preconditions to variable state
        actions.forEach(action -> action.getPopEffs().forEach(eff -> {
            String effKey = createEffKey(eff);

            ImmutablePair<String, Boolean> precondition =
                    ImmutablePair.of(createEffId(eff, eff.getValue()), encodeValue(eff, false));

            variablesStateBeforeStepExec.putIfAbsent(effKey, ImmutableSet.of());
            if (variablesStateBeforeStepExec.get(effKey).stream().
                    noneMatch(effect -> effect.getLeft().equals(precondition.getLeft()))) {
                log.debug("Adding effect to the var state {}", precondition);
                variablesStateBeforeStepExec.put(effKey, ImmutableSet.<ImmutablePair<String, Boolean>>builder().
                        addAll(variablesStateBeforeStepExec.get(effKey)).add(precondition).build());
                newFluents.add(ImmutablePair.of(createEffInstance(eff, stage, eff.getValue()), encodeValue(eff, false)));

            }
        }));
        log.debug("Adding effect to the var state {}\n", newFluents.stream().map(Objects::toString).collect(Collectors.joining("\n")));

        return newFluents.stream().map(ImmutableSet::of);
    }


}
