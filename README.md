# Toy Scheduler

Lets split problem to distinct parts:

  * CLIENT: creating job requests
  * SERVER: executing the jobs
  * SCHEDULER: maintaining invariants

Invariants:

  1) at most one job for one group is concurently executed
  2) only read or only write jobs are concurently executed
  3) jobs are started in the same order as they were requested

Clients pass job requests to scheduler as they want and schedule pass
jobs to servers, when it is possible.  Each job has unique
identification and client identification.

Scheduler uses 5 datastructures:

  * set of currently processed groups (group_set)
  * signed counter (type_counter), initialized to zero
  * synchronized queue (sched_rx_queue) with job scheduling requests
  * synchronized queue (exec_rx_queue)  with job excecution requests
  * synchronized queue (exec_tx_queue)  with finished job notifications
  * synchronized queue (sched_tx_queue) with finished job notifications

Read jobs are flagged with -1, write jobs are flagged with +1.
Define:
  * `sgn(int) == 1  if int > 0`
  * `sgn(int) == -1 if int < 0`


Job request
-----------
```
clients > sched_tx_queue > scheduler > exec_tx_queue > servers
                              \/
                     group_set, type_counter
```

Clients put job requests to sched_rx_queue as they need.
Nothing is executing yet, so invariants 1 and 2 are maintained.


Scheduler pop one job request from sched_rx_queue.

If job request's group is in group_set, than process job confirmation
from exec_tx_queue. Repeat this step again (maintains invariant 1).

Put job request's group to group_set (maintains invariant 1).

If type_counter != 0 and job's flag != sgn(type_counter), than process
job confirmation from exec_tx_queue. Repeat this step again (maintains
invariant 2).

Add value of job's flag to type_counter (maintains invariant 2).

Put job execution request to exec_rx_queue.


Each servers pops one job execution at a time from exec_tx_queue and
immediately executes them. If strict job order is required, shared
semaphore is needed, that is taken before accessing queue and released
after jobs execution started.


Order is maintained through whole pipeline (maintains invariant 3).


Job finished
------------
```
servers > exec_rx_queue > scheduler > sched_rx_queue > clients
                             \/
                type_counter, group_set
```

After server finishes job, job confirmation is put to
exec_tx_queue. Order is not important anymore.


Scheduler pops job confirmation from exec_tx_queue, substracts job's
flag from type_counter, removes job's group from group_set and puts jobs
confirmations to sched_tx_queue.


Clients read jobs confirmations from sched_tx_queue and job is done.
Sched_tx_queue does't need to maintain strict order and topic support is
probably needed.
