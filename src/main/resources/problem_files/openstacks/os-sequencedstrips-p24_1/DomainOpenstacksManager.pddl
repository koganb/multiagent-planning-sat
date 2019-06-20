(define (domain openstacks)
(:requirements :typing :equality :fluents)
(:types
    order product count - object)
(:constants
 p1 p2 p3 p4 p5 p6 p7 p8 p9 p10 p11 p12 p13 p14 p15 p16 p17 p18 p19 p20 p21 p22 p23 p24 - product
 o1 o2 o3 o4 o5 o6 o7 o8 o9 o10 o11 o12 o13 o14 o15 o16 o17 o18 o19 o20 o21 o22 o23 o24 - order
)
(:predicates
  (includes ?o - order ?p - product)
  (waiting ?o - order)
  (started ?o - order)
  (shipped ?o - order)
  (made ?p - product))
(:functions
  (stacks-avail) - count
  (next-count ?s) - count)
(:action start-order
 :parameters (?o - order ?avail ?new-avail - count)
 :precondition (and (waiting ?o) (= (stacks-avail) ?avail)
               (= (next-count ?new-avail) ?avail))
 :effect (and (not (waiting ?o)) (started ?o) (assign (stacks-avail) ?new-avail)))
(:action ship-order-o1
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o1)
(made p10)
(made p13)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o1)
)
(shipped o1)
))
(:action ship-order-o2
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o2)
(made p6)
(made p7)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o2)
)
(shipped o2)
))
(:action ship-order-o3
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o3)
(made p21)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o3)
)
(shipped o3)
))
(:action ship-order-o4
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o4)
(made p13)
(made p14)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o4)
)
(shipped o4)
))
(:action ship-order-o5
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o5)
(made p4)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o5)
)
(shipped o5)
))
(:action ship-order-o6
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o6)
(made p2)
(made p3)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o6)
)
(shipped o6)
))
(:action ship-order-o7
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o7)
(made p9)
(made p18)
(made p21)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o7)
)
(shipped o7)
))
(:action ship-order-o8
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o8)
(made p1)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o8)
)
(shipped o8)
))
(:action ship-order-o9
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o9)
(made p12)
(made p15)
(made p18)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o9)
)
(shipped o9)
))
(:action ship-order-o10
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o10)
(made p11)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o10)
)
(shipped o10)
))
(:action ship-order-o11
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o11)
(made p12)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o11)
)
(shipped o11)
))
(:action ship-order-o12
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o12)
(made p5)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o12)
)
(shipped o12)
))
(:action ship-order-o13
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o13)
(made p19)
(made p21)
(made p24)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o13)
)
(shipped o13)
))
(:action ship-order-o14
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o14)
(made p23)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o14)
)
(shipped o14)
))
(:action ship-order-o15
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o15)
(made p16)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o15)
)
(shipped o15)
))
(:action ship-order-o16
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o16)
(made p4)
(made p22)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o16)
)
(shipped o16)
))
(:action ship-order-o17
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o17)
(made p20)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o17)
)
(shipped o17)
))
(:action ship-order-o18
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o18)
(made p17)
(made p19)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o18)
)
(shipped o18)
))
(:action ship-order-o19
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o19)
(made p13)
(made p18)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o19)
)
(shipped o19)
))
(:action ship-order-o20
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o20)
(made p10)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o20)
)
(shipped o20)
))
(:action ship-order-o21
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o21)
(made p1)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o21)
)
(shipped o21)
))
(:action ship-order-o22
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o22)
(made p8)
(made p12)
(made p14)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o22)
)
(shipped o22)
))
(:action ship-order-o23
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o23)
(made p16)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o23)
)
(shipped o23)
))
(:action ship-order-o24
 :parameters (?avail ?new-avail - count)
 :precondition (and (= (stacks-avail) ?avail) (= (next-count ?avail) ?new-avail)
(started o24)
(made p19)
)
 :effect (and (assign (stacks-avail) ?new-avail)
(not 
(started o24)
)
(shipped o24)
))
)
