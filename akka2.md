
## 자식 액터의 종료

액터를 종료시키면 자식 액터부터 모두 종료 시킨 이후에 액터가 종료된다.

```scala
class Parent extends Actor with ActorLogging {

  import Parent._

  override def receive: Receive = {
    case CreatChild(name) =>
      (1 to 5).foreach { num =>
        context.actorOf(Props[Child], s"${name}-${num}")
//        println(s"creating child: ${name}-${num}")
//        log.info(s"creating child: ${name}-${num}")
      }
  }

  override def postStop(): Unit = {
    log.info("[Parent] postStop")
  }
}

object Parent {
  case class CreatChild(name: String)
}

class Child extends Actor with ActorLogging {

  override def receive: Receive = {
    case msg => log.info(s"${self.path}: I got: ${msg}")
  }

  override def preStart(): Unit = {
    log.info(s"[Child] ${self.path} preStart")
  }

  override def postStop(): Unit = {
    log.info(s"[Child] ${self.path} postStop")
  }
}

```

하위 액터를 start 하는 것과 stop 하는 순서가 정해져있지는 않다.
```
19:31:48.327 [default-akka.actor.default-dispatcher-9] INFO bsw.akkasampler.childactor.Child -- [Child] akka://default/user/myParent/myChild-2 preStart
19:31:48.328 [default-akka.actor.default-dispatcher-9] INFO bsw.akkasampler.childactor.Child -- [Child] akka://default/user/myParent/myChild-5 preStart
19:31:48.328 [default-akka.actor.default-dispatcher-9] INFO bsw.akkasampler.childactor.Child -- [Child] akka://default/user/myParent/myChild-4 preStart
19:31:48.328 [default-akka.actor.default-dispatcher-9] INFO bsw.akkasampler.childactor.Child -- [Child] akka://default/user/myParent/myChild-1 preStart
19:31:48.328 [default-akka.actor.default-dispatcher-9] INFO bsw.akkasampler.childactor.Child -- [Child] akka://default/user/myParent/myChild-3 preStart
19:31:48.328 [default-akka.actor.default-dispatcher-6] INFO bsw.akkasampler.childactor.Child -- [Child] akka://default/user/myParent/myChild-3 postStop
19:31:48.329 [default-akka.actor.default-dispatcher-6] INFO bsw.akkasampler.childactor.Child -- [Child] akka://default/user/myParent/myChild-2 postStop
19:31:48.329 [default-akka.actor.default-dispatcher-6] INFO bsw.akkasampler.childactor.Child -- [Child] akka://default/user/myParent/myChild-4 postStop
19:31:48.329 [default-akka.actor.default-dispatcher-6] INFO bsw.akkasampler.childactor.Child -- [Child] akka://default/user/myParent/myChild-5 postStop
19:31:48.329 [default-akka.actor.default-dispatcher-6] INFO bsw.akkasampler.childactor.Child -- [Child] akka://default/user/myParent/myChild-1 postStop
19:31:48.329 [default-akka.actor.default-dispatcher-8] INFO bsw.akkasampler.childactor.Parent -- [Parent] postStop
```

## 액터 계층구조

https://doc.akka.io/docs/akka/current/typed/guide/tutorial_1.html#the-akka-actor-hierarchy

## 액터 경로 구조
https://doc.akka.io/docs/akka/current/general/addressing.html