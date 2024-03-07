```scala
//ActorCell.scala

final val emptyBehaviorStack: List[Actor.Receive] = Nil

private var behaviorStack: List[Actor.Receive] = emptyBehaviorStack

def become(behavior: Actor.Receive, discardOld: Boolean = true): Unit =
  behaviorStack = behavior :: (if (discardOld && behaviorStack.nonEmpty) behaviorStack.tail else behaviorStack)

def unbecome(): Unit = {
  val original = behaviorStack
  behaviorStack =
    if (original.isEmpty || original.tail.isEmpty) actor.receive :: emptyBehaviorStack
    else original.tail
}

final def receiveMessage(msg: Any): Unit = actor.aroundReceive(behaviorStack.head, msg)

final def invoke(messageHandle: Envelope): Unit = {
  val msg = messageHandle.message
  val timeoutBeforeReceive = cancelReceiveTimeoutIfNeeded(msg)
  try {
    currentMessage = messageHandle
    if (msg.isInstanceOf[AutoReceivedMessage]) {
      autoReceiveMessage(messageHandle)
    } else {
      receiveMessage(msg)
    }
    currentMessage = null // reset current message after successful invocation
  } catch handleNonFatalOrInterruptedException { e =>
    handleInvokeFailure(Nil, e)
  } finally
    // Schedule or reschedule receive timeout
    checkReceiveTimeoutIfNeeded(msg, timeoutBeforeReceive)
}
```


```scala
package bsw.akkasampler.behavior

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, Props}

import scala.concurrent.duration.DurationInt

object Vote {
  case class DoVote(candidate: Candidate.Value)

  case class AggregateVotes(citizens: Set[ActorRef])

  case object VoteRequest

  case class VoteReply(candidate: Candidate.Value)

  object Candidate extends Enumeration {
    val LEE, YUN, SIM = Value
  }
}

class Citizen extends Actor with ActorLogging {

  import Vote._

  override def receive: Receive = onReceive(None)

  private def onReceive(candidateOpt: Option[Candidate.Value]): Receive = {
    case DoVote(candidate) =>
      context.become(onReceive(Option(candidate)))

    case VoteRequest =>
      log.info(s"VoteRequest: ${self.path}")
      candidateOpt.map(c => sender() ! VoteReply(c))
  }
}

class VoteAggregator extends Actor with ActorLogging {

  import Vote._
  import context.dispatcher

  override def receive: Receive = initReceive

  private def initReceive: Receive = {
    case AggregateVotes(citizens) =>
      val citizenMap = citizens.map { citizen =>
        citizen -> context.system.scheduler.scheduleWithFixedDelay(0.seconds, 500.millis, citizen, VoteRequest)
      }.toMap

      context.become(waitingReceive(citizenMap, Map.empty))
  }

  private def waitingReceive(citizenMap: Map[ActorRef, Cancellable], stats: Map[Candidate.Value, Int]): Receive = {
    case VoteReply(candidate) =>
      val count = stats.getOrElse(candidate, 0) + 1
      val newStats = stats + (candidate -> count)

      val newCitizenMap = citizenMap.get(sender()) match {
        case Some(cancellable) =>
          cancellable.cancel()
          citizenMap - sender()
        case None =>
          citizenMap
      }

      if (newCitizenMap.isEmpty) {
        log.info(s"vote stats: ${newStats}")
      } else {
        context.become(waitingReceive(newCitizenMap, newStats))
      }
  }
}

object VoteApp extends App {

  import Vote._

  val system = ActorSystem("VoteSystem")

  val a = system.actorOf(Props[Citizen])
  val b = system.actorOf(Props[Citizen])
  val c = system.actorOf(Props[Citizen])
  val d = system.actorOf(Props[Citizen])

  val aggregator = system.actorOf(Props[VoteAggregator])
  aggregator ! AggregateVotes(Set(a, b, c, d))

  Thread.sleep(1000)

  a ! DoVote(Candidate.LEE)

  Thread.sleep(2000)

  b ! DoVote(Candidate.YUN)

  Thread.sleep(3000)

  c ! DoVote(Candidate.SIM)

  Thread.sleep(4000)

  d ! DoVote(Candidate.YUN)


}

```

