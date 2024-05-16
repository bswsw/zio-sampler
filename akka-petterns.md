# Stash

stash된 메시지는 Vector에 저장된다.

```scala
private var theStash = Vector.empty[Envelope]
```

stash 할수 있는 stash vector 용량이 정해져있고 설정이 가능하다.

```scala
private val capacity: Int =
    context.system.mailboxes.stashCapacity(context.props.dispatcher, context.props.mailbox)
```

```
default-mailbox {
  # FQCN of the MailboxType. The Class of the FQCN must have a public
  # constructor with
  # (akka.actor.ActorSystem.Settings, com.typesafe.config.Config) parameters.
  mailbox-type = "akka.dispatch.UnboundedMailbox"

  # If the mailbox is bounded then it uses this setting to determine its
  # capacity. The provided value must be positive.
  # NOTICE:
  # Up to version 2.1 the mailbox type was determined based on this setting;
  # this is no longer the case, the type must explicitly be a bounded mailbox.
  mailbox-capacity = 1000

  # If the mailbox is bounded then this is the timeout for enqueueing
  # in case the mailbox is full. Negative values signify infinite
  # timeout, which should be avoided as it bears the risk of dead-lock.
  mailbox-push-timeout-time = 10s

  # For Actor with Stash: The default capacity of the stash.
  # If negative (or zero) then an unbounded stash is used (default)
  # If positive then a bounded stash is used and the capacity is set using
  # the property
  stash-capacity = -1
}
```

동일한 메시지를 연속적으로 stash 하면 예외가 던져진다.
(equality가 아니라 identity)

정해진 용량 이상을 stash하면 예외가 던져진다.

```scala
  def stash(): Unit = {
    val currMsg = actorCell.currentMessage
    if (theStash.nonEmpty && (currMsg eq theStash.last))
      throw new IllegalStateException(s"Can't stash the same message $currMsg more than once")
    if (capacity <= 0 || theStash.size < capacity) theStash :+= currMsg
    else throw new StashOverflowException(
      s"Couldn't enqueue message ${currMsg.message.getClass.getName} from ${currMsg.sender} to stash of $self")
  }
```

액터가 재시작되면 unstashAll 이 호출된다.

```scala
  @throws(classOf[Exception])
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    try unstashAll()
    finally super.preRestart(reason, message)
  }
```

특정 메시지만 unstash 할 수 있게 구현되어있지만,, 사용자는 사용할 수 없다.

```scala
  @InternalStableApi
  private[akka] def unstashAll(filterPredicate: Any => Boolean): Unit = {
    try {
      val i = theStash.reverseIterator.filter(envelope => filterPredicate(envelope.message))
      while (i.hasNext) enqueueFirst(i.next())
    } finally {
      theStash = Vector.empty[Envelope]
    }
  }
```

UnboundedStash: 무제한 메일박스가 적용된 Stash

UnrestrictedStash: 메일박스가 적용되지 않은 Stash. 메일박스를 직접 적용하지 않으면 액터 생성시 예외 발생

```scala
class MyStashActor extends Actor with ActorLogging with UnrestrictedStash {
  override def receive: Receive = {
    case message =>
      log.info(s"message: ${message}")
      stash()
  }
}

val actor = system.actorOf(Props[MyStashActor])
```

```
akka.actor.ActorInitializationException: akka://StashApp/user/$a: exception during creation
	at akka.actor.ActorInitializationException$.apply(Actor.scala:196)
	at akka.actor.ActorCell.create(ActorCell.scala:661)
	at akka.actor.ActorCell.invokeAll$1(ActorCell.scala:513)
	at akka.actor.ActorCell.systemInvoke(ActorCell.scala:535)
	at akka.dispatch.Mailbox.processAllSystemMessages(Mailbox.scala:295)
	at akka.dispatch.Mailbox.run(Mailbox.scala:230)
	at akka.dispatch.Mailbox.exec(Mailbox.scala:243)
	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:290)
	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1020)
	at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1656)
	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1594)
	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:183)
Caused by: java.lang.reflect.InvocationTargetException: null
	at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
	at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)
	at java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
	at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:490)
	at akka.util.Reflect$.instantiate(Reflect.scala:51)
	at akka.actor.NoArgsReflectConstructor.produce(IndirectActorProducer.scala:110)
	at akka.actor.Props.newActor(Props.scala:226)
	at akka.actor.ActorCell.newActor(ActorCell.scala:613)
	at akka.actor.ActorCell.create(ActorCell.scala:640)
	... 10 common frames omitted
Caused by: akka.actor.ActorInitializationException: akka://StashApp/user/$a: DequeBasedMailbox required, got: akka.dispatch.UnboundedMailbox$MessageQueue
An (unbounded) deque-based mailbox can be configured as follows:
  my-custom-mailbox {
    mailbox-type = "akka.dispatch.UnboundedDequeBasedMailbox"
  }

	at akka.actor.ActorInitializationException$.apply(Actor.scala:196)
	at akka.actor.StashSupport.$init$(Stash.scala:154)
	at bsw.akkasampler.patterns.MyStashActor.<init>(StashDemo.scala:22)
	... 19 common frames omitted
```

# FSM

액터가 어떤 상태일 때 어떤 메시지를 어떻게 처리를 하게 되는지 선언 해둔다.

조금 더 정확하게는 메시지와 상태가 가지는 데이터에 따라 처리를 선언할 수 있다.

즉 FSM은 상태 * 메시지 * 데이터 3가지로 구성되어 있다.

개인적으로 메시지는 State Machine 에게 던지는 명령으로 봐도 좋을 것 같다.


when(state) {} 함수로 상태에 대한 메시지를 처리하고

whenUnhandled {} 함수로 어떤 상태로도 처리되지 않은 메시지를 처리할 수 있다. (기존 액터의 unhandled 함수와 동일)


onTransaction 함수는 기존 액터와 비교 했을때 특이한 부분인데,

기존 액터에는 상태라는 개념이 존재하지 않기 때문에 (물론 온몸을 비틀면서 가능은 하겠지만) 받은 메시지의 타입에 따른 처리만 가능했지만

onTransaction 에서는 상태 변화에 따른 비즈니스 로직을 추가할 수 있다.

기존 액터
- receive 함수에 RequestProduct 메시지를 받으면 처리할 로직을 구현한다.

FSM 액터
- when 구문으로 어떤 상태에서 RequestProduct 메시지를 Initialized 데이터와 받으면 처리할 로직을 구현한다.
- onTransaction 함수에 Idle 상태에서 Operational 상태로 변화하면 처리할 로직을 구현한다.


FSM 액터의 상태가 변하는 것을 모니터할 수 있다.

```scala
object MyFSM {
  sealed trait State
  case object Created extends State
  case object Activated extends State
  case object Deactivated extends State

  sealed trait Data
  case object NoData extends Data

  sealed trait Command
  case object Activate extends Command
  case object Deactivate extends Command
  case object Recreate extends Command
}


class MyFSM extends FSM[MyFSM.State, MyFSM.Data] {

  import MyFSM._

  startWith(Created, NoData)

  when(Created) {
    case Event(Activate, NoData) =>
      log.info("Activate command.")
      goto(Activated) using NoData
  }

  when(Activated) {
    case Event(Deactivate, NoData) =>
      log.info("Deactivate command.")
      goto(Deactivated) using NoData
  }

  when(Deactivated) {
    case Event(Recreate, NoData) =>
      log.info("Created command.")
      goto(Created) using NoData
  }

  onTransition {
    case before -> after =>
      log.info(s"Change state: ${before} -> ${after}")
  }

}

class FSMSpec extends BaseActorSpec("FSMSpecSystem") {

  private val monitor = TestProbe()
  private val actor = system.actorOf(Props[MyFSM])

  actor ! SubscribeTransitionCallBack(monitor.ref)
  monitor expectMsg CurrentState(actor, MyFSM.Created)

  actor ! MyFSM.Activate
  monitor expectMsg Transition(actor, MyFSM.Created, MyFSM.Activated)

  actor ! MyFSM.Deactivate
  monitor expectMsg Transition(actor, MyFSM.Activated, MyFSM.Deactivated)

  actor ! MyFSM.Recreate
  monitor expectMsg Transition(actor, MyFSM.Deactivated, MyFSM.Created)
}

```