import AndroidServer.CalculateDirections
import Main.CreateInfrastracture
import MapWorker.CalculateReduction
import MappersGroup.RespondAllMapResults
import Master.{RequestTrackMapper, RequestTrackReducer}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.google.maps.model.DirectionsResult

object Master {
  def props: Props = Props(new Master)
  final case class RequestTrackMapper(str: String)
  final case class RequestTrackReducer(a: String)
}

class Master extends Actor with ActorLogging {
  var mappersGroupActor: ActorRef = _
  var reducersGroupActor: ActorRef = _
  override def preStart(): Unit = log.info("MasterImpl started")
  override def postStop(): Unit = log.info("MasterImpl stopped")
  override def receive: Receive = {
    case CreateInfrastracture =>
      log.info("Creating reducers group actor.")
      reducersGroupActor = context.actorOf(ReducersGroup.props(mappersGroupActor, this.self))
      reducersGroupActor ! RequestTrackReducer("moscow")
      log.info("Creating mappers group actor.")
      mappersGroupActor = context.actorOf(MappersGroup.props(reducersGroupActor, this.self))
      context.watch(mappersGroupActor)
      mappersGroupActor ! RequestTrackMapper("havana")
      mappersGroupActor ! RequestTrackMapper("saoPaolo")
      mappersGroupActor ! RequestTrackMapper("athens")
      mappersGroupActor ! RequestTrackMapper("jamaica")
    case request @ CalculateDirections =>
      mappersGroupActor forward request
    case RespondAllMapResults(request, results) =>
      val merged = results.foldLeft(List.empty[Map[GeoPointPair, DirectionsResult]])(x, (y, z) => x.add(z))
      reducersGroupActor ! CalculateReduction(request.requestId, merged)
  }
}

