(define (problem wireless-10) (:domain wireless)
(:objects
	msg1-1 - message
	msg2-1 - message
	msg3-1 - message
	msg4-1 - message
	msg5-1 - message
	msg6-1 - message
	msg7-1 - message
	msg8-1 - message
	msg9-1 - message
	msg10-1 - message
)
(:init
	(higher High Low)
	(higher High Normal)
	(higher High Zero)
	(higher Normal Low)
	(higher Normal Zero)
	(higher Low Zero)
	(next High Normal)
	(next Normal Low)
	(next Low Zero)
	(energy node1 High)
	(is-message-at msg1-1 node1)
	(energy node2 High)
	(is-message-at msg2-1 node2)
	(energy node3 High)
	(is-message-at msg3-1 node3)
	(energy node4 High)
	(is-message-at msg4-1 node4)
	(energy node5 High)
	(is-message-at msg5-1 node5)
	(energy node6 High)
	(is-message-at msg6-1 node6)
	(energy node7 High)
	(is-message-at msg7-1 node7)
	(energy node8 High)
	(is-message-at msg8-1 node8)
	(energy node9 High)
	(is-message-at msg9-1 node9)
	(neighbor node1 node2)
	(neighbor node1 node9)
	(neighbor node2 node1)
	(neighbor node2 node3)
	(neighbor node2 node9)
	(neighbor node3 node2)
	(neighbor node3 node4)
	(neighbor node4 node3)
	(neighbor node3 node5)
	(neighbor node5 node3)
	(neighbor node4 node5)
	(neighbor node5 node4)
	(neighbor node5 base)
	(neighbor node5 node6)
	(neighbor base node5)
	(neighbor node6 node5)
	(neighbor node6 node7)
	(neighbor node6 node8)
	(neighbor node6 node9)
	(neighbor node7 node6)
	(neighbor node7 node8)
	(neighbor node8 node6)
	(neighbor node8 node7)
	(neighbor node9 node1)
	(neighbor node9 node2)
	(neighbor node9 node6)
)
(:goal
	(and
		(has-data base node1)
		(has-data base node2)
		(has-data base node3)
		(has-data base node4)
		(has-data base node5)
		(has-data base node6)
		(has-data base node7)
		(has-data base node8)
		(has-data base node9)
	)
)
)