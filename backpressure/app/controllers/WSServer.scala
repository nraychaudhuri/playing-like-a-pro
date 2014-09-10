package controllers

import akka.actor.Actor.Receive
import akka.actor.{Props, Actor, ActorRef}
import play.api.libs.concurrent.Akka
import play.api.mvc.{Action, WebSocket, Controller}
import views.html.ws
import play.api.Play.current


object WSServer extends Controller {

  def r = Action {
     Ok(ws())
  }
  def join = WebSocket.acceptWithActor[String, String] { req => out =>
    VideoController.props(out)
  }

}

object VideoController {
  def props(out: ActorRef) = Props(new VideoController(out))
}

class VideoController(out: ActorRef) extends Actor {
  override def receive: Receive = {
    case msg =>
      println(">>>>>> received messag " + msg )
      context.actorSelection("../../*") ! msg
  }
}
