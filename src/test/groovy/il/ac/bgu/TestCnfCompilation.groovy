package il.ac.bgu

import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.tuple.ImmutablePair
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

class TestCnfCompilation extends Specification {

    @Shared
    TreeMap<Integer, Set<Step>> sortedPlan

    @Shared
    CnfCompilation cnfCompilation

    def setupSpec() {
        String[] agentDefs = Files.readAllLines(
                Paths.get(this.getClass().getClassLoader().getResource("problems/‏‏‏‏‏‏‏‏satelate1.problem").toURI())).stream().
                flatMap({ t -> Arrays.stream(t.split("\t")) }).
                collect(Collectors.toList()).toArray(new String[0])

        //calculate solution plan
        sortedPlan = SatSolver.calculateSolution(agentDefs)

        cnfCompilation = new CnfCompilation(sortedPlan)


    }

    def "test initial facts calculation"() {
        //TODO check why multi-functions are not part of the initial state
        expect:
        cnfCompilation.calcInitFacts().stream().
                collect(Collectors.toList()) == [
                [ImmutablePair.of("0:power_avail~satellite0", true)],
                [ImmutablePair.of("0:power_on~instrument0", false)],
                [ImmutablePair.of("0:calibrated~instrument0", false)],
                [ImmutablePair.of("0:have_image~phenomenon4~thermograph0", false)],
                [ImmutablePair.of("0:have_image~star5~thermograph0", false)],
                [ImmutablePair.of("0:have_image~phenomenon6~thermograph0", false)],
                [ImmutablePair.of("0:pointing~satellite0=phenomenon6", true)]
        ]
    }


    def "test final facts calculation"() {
        expect:
        //TODO check the possibility to convert predicates to the boolean fluents

        cnfCompilation.calcFinalFacts().stream().
                collect(Collectors.toList()) == [

                [
                        ImmutablePair.of("1:have_image~star5~thermograph0", false)
                ],
                //goal
                [
                        ImmutablePair.of("1:pointing~satellite0=phenomenon6", true)
                ],
                //goal
                [
                        ImmutablePair.of("1:calibrated~instrument0", false)
                ],
                [
                        ImmutablePair.of("1:have_image~phenomenon4~thermograph0", false)
                ],
                [
                        ImmutablePair.of("1:have_image~phenomenon6~thermograph0", false)
                ],
                [
                        ImmutablePair.of("1:power_on~instrument0", true)
                ],
                [
                        ImmutablePair.of("1:power_avail~satellite0", false)
                ],
        ]
    }


    def "test that variables that exist in the effects are added to variable state"() {
        expect:
        cnfCompilation.executeStage(0, sortedPlan.get(0)).
                collect(Collectors.toList()) == [
        ]
    }

    def "test pass through clauses calculation"() {
        expect:
        cnfCompilation.calculatePassThroughClauses(0, sortedPlan.get(0)).
                collect(Collectors.toList()).sort() == [
                [
                        ImmutablePair.of("0:have_image~star5~thermograph0", false),
                        ImmutablePair.of("1:have_image~star5~thermograph0", true)
                ],
                [
                        ImmutablePair.of("0:have_image~star5~thermograph0", true),
                        ImmutablePair.of("1:have_image~star5~thermograph0", false)
                ],
                [
                        ImmutablePair.of("0:have_image~phenomenon4~thermograph0", false),
                        ImmutablePair.of("1:have_image~phenomenon4~thermograph0", true)
                ],
                [
                        ImmutablePair.of("0:have_image~phenomenon4~thermograph0", true),
                        ImmutablePair.of("1:have_image~phenomenon4~thermograph0", false)
                ],
                [
                        ImmutablePair.of("0:pointing~satellite0=phenomenon6", false),
                        ImmutablePair.of("1:pointing~satellite0=phenomenon6", true)
                ],
                [
                        ImmutablePair.of("0:pointing~satellite0=phenomenon6", true),
                        ImmutablePair.of("1:pointing~satellite0=phenomenon6", false)
                ],
                [
                        ImmutablePair.of("0:have_image~phenomenon6~thermograph0", false),
                        ImmutablePair.of("1:have_image~phenomenon6~thermograph0", true)
                ],
                [
                        ImmutablePair.of("0:have_image~phenomenon6~thermograph0", true),
                        ImmutablePair.of("1:have_image~phenomenon6~thermograph0", false)
                ]
        ].sort()
    }


    def "test healthy clauses calculation"() {
        expect:
        cnfCompilation.calculateHealthyClauses(0, sortedPlan.get(0)).
                collect(Collectors.toList()) == [
                [
                        ImmutablePair.of("0:power_avail~satellite0", false),
                        ImmutablePair.of("0:h(switch_on~instrument0~satellite0)", false),
                        ImmutablePair.of("1:power_on~instrument0", true),
                ],
                [
                        ImmutablePair.of("0:power_avail~satellite0", false),
                        ImmutablePair.of("0:h(switch_on~instrument0~satellite0)", false),
                        ImmutablePair.of("1:calibrated~instrument0", false),
                ],

                [
                        ImmutablePair.of("0:power_avail~satellite0", false),
                        ImmutablePair.of("0:h(switch_on~instrument0~satellite0)", false),
                        ImmutablePair.of("1:power_avail~satellite0", false),
                ],
        ]
    }

    def "test failed clauses calculation"() {
        expect:
        cnfCompilation.calculateActionFailedClauses(0, sortedPlan.get(0)).
                collect(Collectors.toList()) == [
                [
                        ImmutablePair.of("0:power_avail~satellite0", false),
                        ImmutablePair.of("0:h(switch_on~instrument0~satellite0)", true),
                        ImmutablePair.of("1:power_on~instrument0", false)
                ],
                [
                        ImmutablePair.of("0:power_avail~satellite0", false),
                        ImmutablePair.of("0:h(switch_on~instrument0~satellite0)", true),
                        ImmutablePair.of("1:calibrated~instrument0", false)
                ],
                [
                        ImmutablePair.of("0:power_avail~satellite0", false),
                        ImmutablePair.of("0:h(switch_on~instrument0~satellite0)", true),
                        ImmutablePair.of("1:power_avail~satellite0", true)
                ]
        ]

    }

    def "test condition not met clauses calculation"() {
        expect:
        cnfCompilation.calculateConditionsNotMetClauses(0, sortedPlan.get(0)).
                collect(Collectors.toList()) == [
                [
                        ImmutablePair.of("0:power_avail~satellite0", true),
                        ImmutablePair.of("0:h(switch_on~instrument0~satellite0)", false),
                        ImmutablePair.of("1:power_on~instrument0", false)
                ],
                [
                        ImmutablePair.of("0:power_avail~satellite0", true),
                        ImmutablePair.of("0:h(switch_on~instrument0~satellite0)", false),
                        ImmutablePair.of("1:calibrated~instrument0", false)
                ],

                [
                        ImmutablePair.of("0:power_avail~satellite0", true),
                        ImmutablePair.of("0:h(switch_on~instrument0~satellite0)", false),
                        ImmutablePair.of("1:power_avail~satellite0", true)
                ]
        ]
    }


}