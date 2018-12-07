(define (problem depotprob1212)
(:domain depot)
(:objects
 depot0 - depot
 distributor0 distributor1 - distributor
 truck0 truck1 - truck
 crate0 crate1 crate2 crate3 crate4 crate5 crate6 crate7 crate8 crate9 - crate
 pallet0 pallet1 pallet2 - pallet
 hoist0 hoist1 hoist2 - hoist
)
(:shared-data
  (clear ?x - (either surface hoist))
  ((at ?t - truck) - place)
  ((pos ?c - crate) - (either place truck))
  ((on ?c - crate) - (either surface hoist truck)) - 
(either distributor0 distributor1 truck0 truck1)
)
(:init
 (myAgent depot0)
 (= (pos crate0) distributor1)
 (not (clear crate0))
 (= (on crate0) pallet2)
 (= (pos crate1) depot0)
 (not (clear crate1))
 (= (on crate1) pallet0)
 (= (pos crate2) distributor1)
 (not (clear crate2))
 (= (on crate2) crate0)
 (= (pos crate3) depot0)
 (not (clear crate3))
 (= (on crate3) crate1)
 (= (pos crate4) depot0)
 (clear crate4)
 (= (on crate4) crate3)
 (= (pos crate5) distributor1)
 (not (clear crate5))
 (= (on crate5) crate2)
 (= (pos crate6) distributor0)
 (not (clear crate6))
 (= (on crate6) pallet1)
 (= (pos crate7) distributor0)
 (not (clear crate7))
 (= (on crate7) crate6)
 (= (pos crate8) distributor0)
 (clear crate8)
 (= (on crate8) crate7)
 (= (pos crate9) distributor1)
 (clear crate9)
 (= (on crate9) crate5)
 (= (at truck0) depot0)
 (= (at truck1) distributor0)
 (= (located hoist0) depot0)
 (clear hoist0)
 (= (located hoist1) distributor0)
 (clear hoist1)
 (= (located hoist2) distributor1)
 (clear hoist2)
 (= (placed pallet0) depot0)
 (not (clear pallet0))
 (= (placed pallet1) distributor0)
 (not (clear pallet1))
 (= (placed pallet2) distributor1)
 (not (clear pallet2))
)
(:global-goal (and
 (= (on crate0) crate5)
 (= (on crate1) pallet1)
 (= (on crate2) crate0)
 (= (on crate3) pallet2)
 (= (on crate4) crate6)
 (= (on crate5) crate4)
 (= (on crate6) crate9)
 (= (on crate7) crate1)
 (= (on crate8) crate3)
 (= (on crate9) pallet0)
))
)
