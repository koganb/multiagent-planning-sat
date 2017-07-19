package il.ac.bgu;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Created by Boris on 01/07/2017.
 */

@Slf4j
public class CnfCompilation {

    private Map<String, Set<POPPrecEff>> variablesState;


    private TreeMap<Integer, Set<Step>> plan;


    public CnfCompilation(TreeMap<Integer, Set<Step>> plan) {
        this.plan = plan;
        this.variablesState = plan.get(-1).iterator().next().getPopEffs().stream().collect(
                Collectors.toMap(t -> t.getFunction().toKey(), Sets::newHashSet));

        log.info("Initialized variable state to: {}", variablesState);
    }


    public List<List<ImmutablePair<String, Boolean>>> calcInitFacts() {
        return plan.entrySet().stream().filter(i -> i.getKey() == -1).
                flatMap(t -> t.getValue().stream()).
                flatMap(t -> t.getPopEffs().stream()).
                map(t -> Lists.newArrayList(new ImmutablePair<>(encode(t, 0), true))).
                collect(Collectors.toList());
    }


    public List<List<ImmutablePair<String, Boolean>>> calcFinalFacts(Integer... failedSteps) {
        Set<Integer> failedStepsSet = Arrays.stream(failedSteps).collect(Collectors.toSet());

        Map<String, ImmutablePair<POPPrecEff, Set<POPPrecEff>>> currentState = new HashMap<>();

        //initial state
        plan.entrySet().stream().filter(i -> i.getKey() == -1).
                flatMap(t -> t.getValue().stream()).
                flatMap(t -> t.getPopEffs().stream()).
                forEach(t -> currentState.put(
                        t.getFunction().toKey(), new ImmutablePair<>(t, Sets.newHashSet())));

        final MutableInt stepCounter = new MutableInt(0);

        plan.entrySet().stream().filter(i -> i.getKey() != -1).forEach(t -> {
            Set<POPPrecEff> stepPostTrueState = new HashSet<>();
            Set<POPPrecEff> stepPostFalseState = new HashSet<>();

            Set<POPPrecEff> stepPreTrueState = currentState.values().stream().map(
                    ImmutablePair::getLeft).collect(Collectors.toSet());

            //try to run action for step
            t.getValue().stream().forEach(k -> {
                if (k.getPopPrecs().stream().allMatch(stepPreTrueState::contains) &&
                        !failedStepsSet.contains(stepCounter.getValue())) {
                    stepPostTrueState.addAll(k.getPopEffs());
                    stepPostFalseState.removeAll(k.getPopEffs());
                } else {

                    System.out.println(String.format("Failed step: %s", k));

                    stepPostFalseState.addAll(k.getPopEffs());
                    stepPostTrueState.removeAll(k.getPopEffs());
                }
                stepCounter.add(1);

            });

            //update true state
            stepPostTrueState.stream().forEach(v -> {
                Optional.ofNullable(currentState.putIfAbsent(
                        v.getFunction().toKey(), new ImmutablePair<>(v, Sets.newHashSet()))).
                        ifPresent(i -> {
                            ImmutablePair<POPPrecEff, Set<POPPrecEff>> value = new ImmutablePair<>(v,
                                    Stream.concat(i.getValue().stream(), Stream.of(i.getKey())).
                                            filter(j -> ObjectUtils.notEqual(j, v)).
                                            collect(Collectors.toSet()));

                            currentState.put(v.getFunction().toKey(), value);

                        });
            });

            //update false state
            stepPostFalseState.stream().forEach(v -> {
                Optional.ofNullable(currentState.putIfAbsent(
                        v.getFunction().toKey(), new ImmutablePair<>(v, Sets.newHashSet()))).
                        ifPresent(i -> {
                            if (ObjectUtils.notEqual(i.getLeft(), v)) {  //add to the false state if not already true
                                ImmutablePair<POPPrecEff, Set<POPPrecEff>> value = new ImmutablePair<>(i.getKey(),
                                        Stream.concat(i.getValue().stream(), Stream.of(v)).
                                                collect(Collectors.toSet()));
                                currentState.put(v.getFunction().toKey(), value);
                            }
                        });
            });
        });

        int maxStep = plan.keySet().stream().mapToInt(Integer::intValue).max().getAsInt() + 1;

        return currentState.values().stream().flatMap(i ->
                Stream.concat(
                        Stream.of(new ImmutablePair<>(encode(i.getLeft(), maxStep), true)),
                        i.getRight().stream().map(f -> new ImmutablePair<>(encode(f, maxStep), false)))).
                map(Lists::newArrayList).collect(Collectors.toList());
    }

    private Stream<List<ImmutablePair<String, Boolean>>> calculatePassThroughClauses(Integer
                                                                                             stage, Set<Step> actions) {
        //calculate "pass through" variables
        Set<String> stageEffectKeys = actions.stream().flatMap(k -> k.getPopEffs().stream().map(f ->
                f.getFunction().toKey())).collect(Collectors.toSet());
        Set<String> currentStateKeys = new HashSet<>(variablesState.keySet());
        Collection<String> passVariables = CollectionUtils.subtract(currentStateKeys, stageEffectKeys);

        log.info("Start pass through...");

        List<List<ImmutablePair<String, Boolean>>> passThroughValues = passVariables.stream().
                flatMap(g -> variablesState.get(g).stream()).
                flatMap(g -> Stream.of(
                        Lists.newArrayList(
                                new ImmutablePair<>(encode(g, stage), false),
                                new ImmutablePair<>(encode(g, stage + 1), true)),
                        Lists.newArrayList(
                                new ImmutablePair<>(encode(g, stage), true),
                                new ImmutablePair<>(encode(g, stage + 1), false))

                )).collect(Collectors.toList());

        log.info("\n{}", passThroughValues.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.info("End pass through");
        return passThroughValues.stream();
    }


    private Stream<List<ImmutablePair<String, Boolean>>> calculateHealthyClauses(Integer
                                                                                         stage, Set<Step> actions) {
        log.info("Start add healthy clause");

        List<List<ImmutablePair<String, Boolean>>> resultClauses = actions.stream().flatMap(action -> {

            List<ImmutablePair<String, Boolean>> preconditionList =
                    action.getPopPrecs().stream().map(l -> new ImmutablePair<>(encode(l, stage), false)).
                            collect(Collectors.toList());

            //healthy function
            Stream<ImmutablePair<String, Boolean>> effectStream = action.getPopEffs().stream().flatMap(actionEff -> {
                //effect variable
                Stream<ImmutablePair<String, Boolean>> effect = Stream.of(new ImmutablePair<>(
                        encode(actionEff, stage + 1), true));
                return Optional.ofNullable(variablesState.get(actionEff.getFunction().toKey())).
                        //add state variables with false status
                                map(stateVars ->
                                Stream.concat(effect,
                                        stateVars.stream().
                                                filter(x -> ObjectUtils.notEqual(x, actionEff)).
                                                map(j -> new ImmutablePair<>(encode(j, stage + 1), false)))).
                                orElse(effect);
            });


            Stream<List<ImmutablePair<String, Boolean>>> healthyClauses = effectStream.map(u ->
                    Stream.concat(
                            preconditionList.stream(),
                            Stream.of(new ImmutablePair<>(encode(action, stage), false), u)).
                            collect(Collectors.toList())
            );

            return healthyClauses;
        }).collect(Collectors.toList());

        log.info("\n{}", resultClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.info("End add healthy clause");

        return resultClauses.stream();
    }


    private Stream<List<ImmutablePair<String, Boolean>>> calculateActionFailedClauses(Integer
                                                                                              stage, Set<Step> actions) {

        log.info("Start failed clause");

        List<List<ImmutablePair<String, Boolean>>> resultList = actions.stream().flatMap(action -> {

            List<ImmutablePair<String, Boolean>> preconditionList =
                    action.getPopPrecs().stream().map(l -> new ImmutablePair<>(encode(l, stage), false)).
                            collect(Collectors.toList());

            Stream<List<ImmutablePair<String, Boolean>>> clauses = action.getPopEffs().stream().flatMap(actionEff -> {
                Stream<List<ImmutablePair<String, Boolean>>> listStream = Optional.ofNullable(variablesState.get(actionEff.getFunction().toKey())).
                        map(g -> g.stream().flatMap(stateVar -> Stream.of(
                                Stream.of(new ImmutablePair<>(encode(stateVar, stage), false),
                                        new ImmutablePair<>(encode(stateVar, stage + 1), true)),
                                Stream.of(new ImmutablePair<>(encode(stateVar, stage), true),
                                        new ImmutablePair<>(encode(stateVar, stage + 1), false))).
                                map(s -> Stream.concat(preconditionList.stream(),
                                        Stream.concat(Stream.of(new ImmutablePair<>(encode(action, stage), true)),
                                                s)).collect(Collectors.toList())))).
                        orElseGet(Stream::empty);

                return listStream;


            });

            return clauses;
        }).collect(Collectors.toList());

        log.info("\n{}", resultList.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.info("End failed clause");
        return resultList.stream();
    }


    private Stream<List<ImmutablePair<String, Boolean>>> calculateConditionsNotMetClauses(Integer
                                                                                                  stage, Set<Step> actions) {

        log.info("Start conditions not met clause");

        List<List<ImmutablePair<String, Boolean>>> resultClauses = actions.stream().flatMap(t -> t.getPopPrecs().stream().
                map(l -> new ImmutablePair<>(encode(l, stage), true)).
                flatMap(p ->
                        t.getPopEffs().stream().flatMap(actionEff -> {
                            Stream<ArrayList<ImmutablePair<String, Boolean>>> arrayListStream = Optional.ofNullable(variablesState.get(actionEff.getFunction().toKey())).
                                    map(g -> g.stream().flatMap(stateVar -> Stream.of(
                                            Lists.newArrayList(p,
                                                    new ImmutablePair<>(encode(stateVar, stage), false),
                                                    new ImmutablePair<>(encode(stateVar, stage + 1), true)),
                                            Lists.newArrayList(p,
                                                    new ImmutablePair<>(encode(stateVar, stage), true),
                                                    new ImmutablePair<>(encode(stateVar, stage + 1), false))))).
                                    orElseGet(Stream::empty);
                            return arrayListStream;
                        }))).collect(Collectors.toList());

        log.info("\n{}", resultClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.info("End conditions not met clause");

        return resultClauses.stream();


    }

    public List<List<ImmutablePair<String, Boolean>>> compileToCnf() {
        return plan.entrySet().stream().filter(i -> i.getKey() != -1).flatMap(t -> {
            Stream<List<ImmutablePair<String, Boolean>>> resultStream =
                    Stream.concat(
                            Stream.concat(
                                    Stream.concat(
                                            Stream.concat(
                                                    Stream.concat(Stream.empty(),
                                                            updateVariableState(t.getKey(), t.getValue())),
                                                    calculatePassThroughClauses(t.getKey(), t.getValue())),
                                            calculateHealthyClauses(t.getKey(), t.getValue())),
                                    calculateActionFailedClauses(t.getKey(), t.getValue())),
                            calculateConditionsNotMetClauses(t.getKey(), t.getValue()));

            return resultStream;
        }).collect(Collectors.toList());
    }


    private Stream<List<ImmutablePair<String, Boolean>>> updateVariableState(Integer stage, Set<Step> actions) {
        log.info("Start update variable state clause");

        List<List<ImmutablePair<String, Boolean>>> updateVariableState = actions.stream().flatMap(g -> g.getPopEffs().stream()).flatMap(u -> {
            variablesState.putIfAbsent(u.getFunction().toKey(), Sets.newHashSet());

            Set<POPPrecEff> popPrecEffs = variablesState.get(u.getFunction().toKey());
            if (popPrecEffs.contains(u)) {
                return Stream.empty();
            }
            popPrecEffs.add(u);

            //add new variable as false clause
            return Stream.of(Lists.newArrayList(new ImmutablePair<>(encode(u, stage), false)));
        }).collect(Collectors.toList());

        log.info("\n{}", updateVariableState.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.info("End update variable state clause");

        return updateVariableState.stream();
    }

    private String encode(POPPrecEff precEff, Integer stage) {
        return format("%s:%s=%s", stage, precEff.getFunction().toKey().replace(" ", "~"), precEff.getValue());
    }

    private String encode(Step step, Integer stage) {
        return format("%s:h(%s)", stage, step.getActionName().replace(" ", "~"));

    }


}
