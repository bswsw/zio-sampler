# 스케쥴러

Schedulers are not the best at precision and long-term planning.
- 긴 스케쥴링에 정밀성이 떨어질 수 있다.

## 한번 스케쥴 실행하기.
```scala
system.scheduler.scheduleOnce(1.second) {
  actor ! "hello world"
}

system.scheduler.scheduleOnce(1.second, actor, "hello world")
```

## 반복적으로 실행하기.
```scala
// 2.6 부터 deprecated
system.scheduler.schedule(1.second, 2.seconds) {
  actor ! "hello world"
}

// fixed rate (schedule 함수와 동일) 
system.scheduler.scheduleAtFixedRate(1.seconds, 2.seconds)(
  () => {
    actor ! "hello world"
  }
)

// fixed delay
system.scheduler.scheduleWithFixedDelay(1.seconds, 2.seconds)(
  () => {
    actor ! "hello world"
  }
)

system.scheduler.scheduleAtFixedRate(1.seconds, 2.seconds, actor, "hello world")
system.scheduler.scheduleWithFixedDelay(1.seconds, 2.seconds, actor, "hello world")
```

## scheduleAtFixedRate, scheduleWithFixedDelay 의 차이

```scala
system.scheduler.scheduleAtFixedRate(1.seconds, 1.seconds)(() => {
  logger.info("scheduleAtFixedRate")
  Thread.sleep(800)
})

system.scheduler.scheduleWithFixedDelay(1.seconds, 1.seconds)(() => {
  logger.info("scheduleWithFixedDelay")
  Thread.sleep(800)
})
```
```scala
17:37:52.098 [TimersSchedulersSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleWithFixedDelay
17:37:52.098 [TimersSchedulersSystem-akka.actor.default-dispatcher-4] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleAtFixedRate
17:37:53.081 [TimersSchedulersSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleAtFixedRate
17:37:53.913 [TimersSchedulersSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleWithFixedDelay
17:37:54.092 [TimersSchedulersSystem-akka.actor.default-dispatcher-4] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleAtFixedRate
17:37:55.092 [TimersSchedulersSystem-akka.actor.default-dispatcher-4] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleAtFixedRate
17:37:55.732 [TimersSchedulersSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleWithFixedDelay
17:37:56.093 [TimersSchedulersSystem-akka.actor.default-dispatcher-4] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleAtFixedRate
17:37:57.091 [TimersSchedulersSystem-akka.actor.default-dispatcher-4] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleAtFixedRate
17:37:57.552 [TimersSchedulersSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleWithFixedDelay
17:37:58.083 [TimersSchedulersSystem-akka.actor.default-dispatcher-4] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleAtFixedRate
17:37:59.081 [TimersSchedulersSystem-akka.actor.default-dispatcher-4] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleAtFixedRate
17:37:59.372 [TimersSchedulersSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleWithFixedDelay
17:38:00.092 [TimersSchedulersSystem-akka.actor.default-dispatcher-4] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleAtFixedRate
17:38:01.082 [TimersSchedulersSystem-akka.actor.default-dispatcher-4] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleAtFixedRate
17:38:01.192 [TimersSchedulersSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleWithFixedDelay
17:38:02.083 [TimersSchedulersSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.TimersSchedulers$ -- scheduleAtFixedRate
```
```scala
def scheduleWithFixedDelay(initialDelay: FiniteDuration, delay: FiniteDuration)(runnable: Runnable)(
  implicit executor: ExecutionContext): Cancellable = {
  try new AtomicReference[Cancellable](Cancellable.initialNotCancelled) with Cancellable { self =>
    compareAndSet(
      Cancellable.initialNotCancelled,
      scheduleOnce(
        initialDelay,
        new Runnable {
          override def run(): Unit = {
            try {
              runnable.run()
              if (self.get != null)
                swap(scheduleOnce(delay, this))
            } catch {
              // ignore failure to enqueue or terminated target actor
              case _: SchedulerException                                                                         =>
              case e: IllegalStateException if e.getCause != null && e.getCause.isInstanceOf[SchedulerException] =>
            }
          }
        }))

      ...

    override def isCancelled: Boolean = get == null
  } catch {
    case SchedulerException(msg) => throw new IllegalStateException(msg)
  }
}

override def schedule(initialDelay: FiniteDuration, delay: FiniteDuration, runnable: Runnable)(
  implicit executor: ExecutionContext): Cancellable = {
  checkMaxDelay(roundUp(delay).toNanos)
  try new AtomicReference[Cancellable](InitialRepeatMarker) with Cancellable { self =>
    compareAndSet(
      InitialRepeatMarker,
      schedule(
        executor,
        new AtomicLong(clock() + initialDelay.toNanos) with Runnable {
          override def run(): Unit = {
            try {
              runnable.run()
              val driftNanos = clock() - getAndAdd(delay.toNanos)
              if (self.get != null)
                swap(schedule(executor, this, Duration.fromNanos(Math.max(delay.toNanos - driftNanos, 1))))
            } catch {
              case _: SchedulerException => // ignore failure to enqueue or terminated target actor
            }
          }
        },
        roundUp(initialDelay)))

      ...

    override def isCancelled: Boolean = get == null
  } catch {
    case cause @ SchedulerException(msg) => throw new IllegalStateException(msg, cause)
  }
}
```

## timer base actor
```scala
class TimerActor extends Timers {
  
  override def receive: Receive = ???
}
```
```scala
  private def startTimer(key: Any, msg: Any, timeout: FiniteDuration, mode: TimerMode): Unit = {
    timers.get(key) match {
      case Some(t) => cancelTimer(t)
      case None    =>
    }
    val nextGen = nextTimerGen()

    val timerMsg =
      if (msg.isInstanceOf[NotInfluenceReceiveTimeout])
        NotInfluenceReceiveTimeoutTimerMsg(key, nextGen, this)
      else
        InfluenceReceiveTimeoutTimerMsg(key, nextGen, this)

    val task = mode match {
      case SingleMode =>
        ctx.system.scheduler.scheduleOnce(timeout, ctx.self, timerMsg)(ctx.dispatcher)
      case FixedDelayMode =>
        ctx.system.scheduler.scheduleWithFixedDelay(timeout, timeout, ctx.self, timerMsg)(ctx.dispatcher)
      case FixedRateMode =>
        ctx.system.scheduler.scheduleAtFixedRate(timeout, timeout, ctx.self, timerMsg)(ctx.dispatcher)
    }

    val nextTimer = Timer(key, msg, mode.repeat, nextGen, task)
    timers = timers.updated(key, nextTimer)
  }
```

# 라우터

## 라우팅 로직

RoundRobinRoutingLogic
- 우선순위 없이 순서대로 전달, 동시 호출의 경우 최선.

RandomRoutingLogic
- 무작위로 선택

SmallestMailboxRoutingLogic
- 비어있는 메일박스가 있는 처리하고 있지 않은 라우티를 찾음
- 메일박스가 비어있는 라우티를 찾음
- 메일박스에 대기열이 가장 적은 라우티를 찾음
- 원격 라우티의 경우 메일박스의 대기열을 알 수가 없어서 가장 낮은 우선순위로 간주됨

BroadcastRoutingLogic
- 모든 라우티에게 메시지를 전달

ScatterGatherFirstCompletedRoutingLogic
- 모든 라우티에게 메시지를 전달하지만 첫번째 응답만 받음

TailChoppingRoutingLogic
- 무작위 하나의 라우티에게 메시지를 보낸 후 지연 이후 (interval) 다음 라우티에게 메시지를 보내고 첫번째 응답만 받음
- 지연 보다 첫 응답이 빨리온다면 보내지 않음

ConsistentHashingRoutingLogic
- 해싱에 따라 라우티를 선택


## BroadcastPool, ScatterGatherFirstCompletedPool, TailChoppingPool

```scala
class Worker extends Actor with ActorLogging {
  override def receive: Receive = {
    case message =>
      Thread.sleep(1000)
      log.info(s"${self.path}: ${message.toString}")
      sender() ! "pong"
  }
}
```

### BroadcastRoutingLogic

```scala
private val broadcastMaster = system.actorOf(BroadcastPool(5, None).props(Props[Worker]))
(broadcastMaster ? "hello world").map { a =>
  logger.info(a.toString)
}
```
```scala
21:21:28.522 [RouterSystem-akka.actor.default-dispatcher-10] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/$a/$e: hello world
21:21:28.524 [RouterSystem-akka.actor.default-dispatcher-8] INFO bsw.akkasampler.infra.RouterApp$ -- pong
21:21:28.525 [RouterSystem-akka.actor.default-dispatcher-10] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/$a/$b: hello world
21:21:28.525 [RouterSystem-akka.actor.default-dispatcher-10] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/$a/$c: hello world
21:21:28.525 [RouterSystem-akka.actor.default-dispatcher-10] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/$a/$d: hello world
21:21:28.525 [RouterSystem-akka.actor.default-dispatcher-10] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/$a/$a: hello world
21:21:28.527 [RouterSystem-akka.actor.default-dispatcher-10] INFO akka.actor.DeadLetterActorRef -- Message [java.lang.String] from Actor[akka://RouterSystem/user/$a/$c#169782117] to Actor[akka://RouterSystem/deadLetters] was not delivered. [1] dead letters encountered. If this is not an expected behavior then Actor[akka://RouterSystem/deadLetters] may have terminated unexpectedly. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
21:21:28.527 [RouterSystem-akka.actor.default-dispatcher-10] INFO akka.actor.DeadLetterActorRef -- Message [java.lang.String] from Actor[akka://RouterSystem/user/$a/$b#-1439474351] to Actor[akka://RouterSystem/deadLetters] was not delivered. [2] dead letters encountered. If this is not an expected behavior then Actor[akka://RouterSystem/deadLetters] may have terminated unexpectedly. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
21:21:28.527 [RouterSystem-akka.actor.default-dispatcher-10] INFO akka.actor.DeadLetterActorRef -- Message [java.lang.String] from Actor[akka://RouterSystem/user/$a/$e#942425554] to Actor[akka://RouterSystem/deadLetters] was not delivered. [3] dead letters encountered. If this is not an expected behavior then Actor[akka://RouterSystem/deadLetters] may have terminated unexpectedly. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
21:21:28.527 [RouterSystem-akka.actor.default-dispatcher-10] INFO akka.actor.DeadLetterActorRef -- Message [java.lang.String] from Actor[akka://RouterSystem/user/$a/$d#938221646] to Actor[akka://RouterSystem/deadLetters] was not delivered. [4] dead letters encountered. If this is not an expected behavior then Actor[akka://RouterSystem/deadLetters] may have terminated unexpectedly. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
```

### ScatterGatherFirstCompletedPool

```scala
  private val scatterGatherMaster = system.actorOf(ScatterGatherFirstCompletedPool(nrOfInstances = 5, resizer = None, within = 1.seconds).props(Props[Worker]))
  (scatterGatherMaster ? "hello world").map { a =>
    logger.info(a.toString)
  }
```
```scala
21:22:44.645 [RouterSystem-akka.actor.default-dispatcher-9] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/$a/$b: hello world
21:22:44.646 [RouterSystem-akka.actor.default-dispatcher-9] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/$a/$a: hello world
21:22:44.646 [RouterSystem-akka.actor.default-dispatcher-9] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/$a/$e: hello world
21:22:44.646 [RouterSystem-akka.actor.default-dispatcher-9] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/$a/$c: hello world
21:22:44.646 [RouterSystem-akka.actor.default-dispatcher-9] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/$a/$d: hello world
21:22:44.648 [RouterSystem-akka.actor.default-dispatcher-9] INFO bsw.akkasampler.infra.RouterApp$ -- pong
```

### TailChoppingPool

```scala
  private val tailChoppingMaster = system.actorOf(TailChoppingPool(nrOfInstances = 5, within = 1.seconds, interval = 500.milli).props(Props[Worker]), "tailChoppingMaster")
  (tailChoppingMaster ? "hello world").map { a =>
    logger.info(a.toString)
  }
```
```scala
21:23:33.854 [RouterSystem-akka.actor.default-dispatcher-10] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/tailChoppingMaster/$b: hello world
21:23:33.859 [RouterSystem-akka.actor.default-dispatcher-10] INFO bsw.akkasampler.infra.RouterApp$ -- pong
21:23:34.368 [RouterSystem-akka.actor.default-dispatcher-10] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/tailChoppingMaster/$e: hello world
```

## Special message

### Broadcast

```scala
  private val roundRobinPool = system.actorOf(RoundRobinPool(5).props(Props[Worker]), "roundRobinMaster2")
  roundRobinPool ! Broadcast("hello world!!")
```
```scala
21:25:05.231 [RouterSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/roundRobinMaster2/$a: hello world!!
21:25:05.233 [RouterSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/roundRobinMaster2/$d: hello world!!
21:25:05.233 [RouterSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/roundRobinMaster2/$e: hello world!!
21:25:05.233 [RouterSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/roundRobinMaster2/$b: hello world!!
21:25:05.233 [RouterSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/roundRobinMaster2/$c: hello world!!
```

### PoisonPill

```scala
class Worker extends Actor with ActorLogging {
  override def receive: Receive = {
    case message =>
      Thread.sleep(1000)
      log.info(s"${self.path}: ${message.toString}")
  }

  override def postStop(): Unit = {
    log.info(s"${self.path} is stopping...")
  }
}

private val roundRobinPool = system.actorOf(RoundRobinPool(5).props(Props[Worker]), "roundRobinMaster2")
roundRobinPool ! PoisonPill
```
```scala
21:27:06.616 [RouterSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/roundRobinMaster2/$c is stopping...
21:27:06.617 [RouterSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/roundRobinMaster2/$d is stopping...
21:27:06.617 [RouterSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/roundRobinMaster2/$a is stopping...
21:27:06.617 [RouterSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/roundRobinMaster2/$b is stopping...
21:27:06.617 [RouterSystem-akka.actor.default-dispatcher-5] INFO bsw.akkasampler.infra.Worker -- akka://RouterSystem/user/roundRobinMaster2/$e is stopping...
```

이 방식은 라우티가 아니라 라우터 액터에게 메시지를 전달한다.
그러면 라우터가 중지되면서 하위 라우티도 종료가 되는데, 하위 라우티가 무언가 처리중이었다면 문제가 될 수 있다.

```scala
private val roundRobinPool = system.actorOf(RoundRobinPool(5).props(Props[Worker]), "roundRobinMaster2")
roundRobinPool ! Broadcast(PoisonPill)
```

이렇게하면 하위 라우티 액터들은 현재 메시지를 처리한 후에 중지하게 된다. 

