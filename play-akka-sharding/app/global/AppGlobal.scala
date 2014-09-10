package global

import actors.User
import akka.actor._
import akka.contrib.pattern.{ShardRegion, ClusterSharding}
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import play.api.{Application, GlobalSettings}

object AppGlobal extends GlobalSettings {

  private[this] var _system: Option[ActorSystem] = None

  def system = _system.getOrElse(throw new RuntimeException("You are screwed!"))

  override def onStart(app: Application): Unit = {
    _system = Option(ActorSystem("ClusterSystem"))
    _system.foreach(createSharding)

    startupSharedJournal(system)


  }

  override def onStop(app: Application): Unit = {
    _system.foreach { s =>
      s.shutdown()
      s.awaitTermination()
    }
  }

  private def createSharding(system: ActorSystem) = {
    ClusterSharding(system).start(
      typeName = User.shardName,
      entryProps = Some(User.props),
      idExtractor = User.idExtractor,
      shardResolver = User.shardResolver
    )
  }


  private def startupSharedJournal(system: ActorSystem): Unit = {
    import akka.pattern._
    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext.Implicits.global
    // Start the shared journal one one node (don't crash this SPOF)
    // This will not be needed with a distributed journal


    val path = ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store")
    val startStore = sys.props.get("akka.remote.netty.tcp.port").map(_ == "2551").getOrElse(false)

    println(">>>>>>>>!!!!!!!!!! " + startStore)
    if (startStore)
      system.actorOf(Props[SharedLeveldbStore], "store")

    // register the shared journal
    implicit val timeout = Timeout(1.minute)
    val f = (system.actorSelection(path) ? Identify(None))
    f.onSuccess {
      case ActorIdentity(_, Some(ref)) => SharedLeveldbJournal.setStore(ref, system)
      case _ =>
        system.log.error("Shared journal not started at {}", path)
        system.shutdown()
    }
    f.onFailure {
      case _ =>
        system.log.error("Lookup of shared journal at {} timed out", path)
        system.shutdown()
    }
  }


}
