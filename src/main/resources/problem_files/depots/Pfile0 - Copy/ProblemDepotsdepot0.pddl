(define (problem depotprob1818_)
(:domain depot)
(:objects
 depot0 - depot
 truck1 - truck
 crate1 - crate
 pallet0 - pallet
 hoist0 - hoist
)
(:shared-data
  (clear ?x - (either surface hoist))
  ((at ?t - truck) - place)
  ((pos ?c - crate) - (either place truck))
  ((on ?c - crate) - (either surface hoist truck)) - 
(either truck1)
)
(:init
 (myAgent depot0)
 (= (pos crate1) depot0)
 (clear crate1)
 (= (on crate1) pallet0)
 (= (at truck1) depot0)
 (= (located hoist0) depot0)
 (clear hoist0)
 (= (placed pallet0) depot0)
 (not (clear pallet0))
)
(:global-goal (and
 (= (on crate1) truck1)
))
)
