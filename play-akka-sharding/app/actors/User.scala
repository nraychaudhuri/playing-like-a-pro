
package actors

import akka.actor._
import akka.persistence.{SnapshotOffer, PersistentActor}
import play.api.Logger
import akka.contrib.pattern.ShardRegion
import scala.concurrent.duration._

case class GameState(level: Int, state: Any)


class User extends Actor {
  import User._

  import context.dispatcher


//  override def persistenceId = "users"

  var state: GameState = _

  context.system.scheduler.schedule(2 seconds, 5 seconds, self, TakeSnapshot)

//  override val receiveRecover: Receive = {
//    case e: GameStateChanged => updateState(e)
//
//    case SnapshotOffer(_, snapshot: GameState) => state = snapshot
//  }


  override def receive: Receive = {
  	case Login(userId) =>
      Logger.info("New user logged in")
      loadLastSavedStateFromStorage()
  	case Logout(userId) =>
      Logger.info("Stopping the actor as user logout")
      context.stop(self)
  	case ChangeState(userId, newState) =>
      //persist(GameStateChanged(userId, newState))(updateState)
      updateState(GameStateChanged(userId, newState))

//    case TakeSnapshot =>
//      saveSnapshot(state)
  }



  private def updateState(gs: GameStateChanged) = {
    Logger.info(s"Applying the delta change for the userId = ${gs.userId}")
    state = state.copy(state = gs.newState)
  }

  private def loadLastSavedStateFromStorage() = {
    state = GameState(level = 0, "initial state")
  }
}

object User {
  def props: Props = Props(classOf[User])

  trait UserMessage { val userId: String }


  private case object TakeSnapshot

  //commands
  case class Login(userId: String) extends UserMessage
  case class Logout(userId: String) extends UserMessage
  case class ChangeState(userId: String, newState: Any) extends UserMessage


  //events
  case class GameStateChanged(userId: String, newState: Any) extends UserMessage

  val shardName = "users"

  val idExtractor : ShardRegion.IdExtractor = {
    case u: UserMessage => (u.userId, u)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case u: UserMessage =>  (u.userId.hashCode % 100).toString
  }

}