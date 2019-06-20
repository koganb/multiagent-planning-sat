(define (problem BLOCKS-11-0)
(:domain ma-blocksworld)
(:objects
 r0 r1 r2 r3 - robot
 e j d c f k h g a i b - block
)
(:shared-data
  ((on ?b - block) - block)
  (ontable ?b - block)
  (clear ?b - block)
  ((holding ?r - robot) - block) - 
(either r0 r1 r3)
)
(:init
 (myAgent r2)
 (= (holding r0) nob)
 (= (holding r1) nob)
 (= (holding r2) nob)
 (= (holding r3) nob)
 (not (clear e))
 (ontable e)
 (= (on e) nob)
 (clear j)
 (= (on j) d)
 (not (ontable j))
 (not (clear d))
 (ontable d)
 (= (on d) nob)
 (clear c)
 (= (on c) e)
 (not (ontable c))
 (not (clear f))
 (= (on f) i)
 (not (ontable f))
 (not (clear k))
 (= (on k) a)
 (not (ontable k))
 (not (clear h))
 (= (on h) k)
 (not (ontable h))
 (not (clear g))
 (= (on g) h)
 (not (ontable g))
 (not (clear a))
 (= (on a) f)
 (not (ontable a))
 (not (clear i))
 (ontable i)
 (= (on i) nob)
 (clear b)
 (= (on b) g)
 (not (ontable b))
)
(:global-goal (and
 (= (on a) j)
 (= (on j) d)
 (= (on d) b)
 (= (on b) h)
 (= (on h) k)
 (= (on k) i)
 (= (on i) f)
 (= (on f) e)
 (= (on e) g)
 (= (on g) c)
)))
