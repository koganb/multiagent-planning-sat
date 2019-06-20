(define (problem logistics-11-0)
(:domain logistics)
(:objects
 apn1 - airplane
 apt4 apt3 apt2 apt1 - airport
 pos4 pos3 pos2 pos1 - location
 cit4 cit3 cit2 cit1 - city
 tru4 tru3 tru2 tru1 - truck
 obj43 obj42 obj41 obj33 obj32 obj31 obj23 obj22 obj21 obj13 obj12 obj11 - package
)
(:shared-data
    ((in ?pkg - package) - (either place agent)) - 
(either apn1 tru3 tru2 tru1)
)
(:init (myAgent tru4)
 (= (at tru4) pos4)
 (= (in obj43) pos4)
 (= (in obj42) pos4)
 (= (in obj41) pos4)
 (= (in obj33) pos3)
 (= (in obj32) pos3)
 (= (in obj31) pos3)
 (= (in obj23) pos2)
 (= (in obj22) pos2)
 (= (in obj21) pos2)
 (= (in obj13) pos1)
 (= (in obj12) pos1)
 (= (in obj11) pos1)
 (in-city apt4 cit4)
 (not (in-city apt4 cit3))
 (not (in-city apt4 cit2))
 (not (in-city apt4 cit1))
 (not (in-city apt3 cit4))
 (in-city apt3 cit3)
 (not (in-city apt3 cit2))
 (not (in-city apt3 cit1))
 (not (in-city apt2 cit4))
 (not (in-city apt2 cit3))
 (in-city apt2 cit2)
 (not (in-city apt2 cit1))
 (not (in-city apt1 cit4))
 (not (in-city apt1 cit3))
 (not (in-city apt1 cit2))
 (in-city apt1 cit1)
 (in-city pos4 cit4)
 (not (in-city pos4 cit3))
 (not (in-city pos4 cit2))
 (not (in-city pos4 cit1))
 (not (in-city pos3 cit4))
 (in-city pos3 cit3)
 (not (in-city pos3 cit2))
 (not (in-city pos3 cit1))
 (not (in-city pos2 cit4))
 (not (in-city pos2 cit3))
 (in-city pos2 cit2)
 (not (in-city pos2 cit1))
 (not (in-city pos1 cit4))
 (not (in-city pos1 cit3))
 (not (in-city pos1 cit2))
 (in-city pos1 cit1)
)
(:global-goal (and
 (= (in obj33) apt1)
 (= (in obj22) apt2)
 (= (in obj43) pos4)
 (= (in obj11) pos1)
 (= (in obj23) pos1)
 (= (in obj31) pos1)
 (= (in obj12) apt2)
 (= (in obj13) pos4)
 (= (in obj42) apt2)
 (= (in obj21) pos4)
 (= (in obj41) pos4)
))
)
