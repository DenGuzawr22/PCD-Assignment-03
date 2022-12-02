package area

import akka.actor.Terminated
import akka.actor.typed.internal.Terminate
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import area.AreaUtils.*
import area.Sensor.SensorMsg

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

object Sensor:
  type SensorMsg = SensorState | Receptionist.Listing
  var  Service: ServiceKey[SensorMsg] = _
  def apply(id: ID, area: Area, coordinates: (Int, Int)): Behavior[SensorMsg] = Behaviors.setup(ctx =>
    Service = ServiceKey[SensorMsg]("Sensor"+ area);

    ctx.spawnAnonymous[Receptionist.Listing](
      Behaviors.setup(internal =>
        internal.system.receptionist ! Receptionist.Subscribe(FireStation.Service, internal.self);
        internal.system.receptionist ! Receptionist.Subscribe(Service, internal.self);
        Behaviors.receiveMessage(msg =>
          ctx.self ! msg;
          Behaviors.same
        )
      )
    );
    ctx.system.receptionist ! Receptionist.Register(Service, ctx.self);
    Behaviors.withTimers(timers =>
      timers.startSingleTimer("Measuring", Measuring(), getTimeout)
      new SensorBehaviour(ctx, id, area, (coordinates))
    )
  )

  sealed trait SensorState
  case class Measuring() extends SensorState with Message
  case class LeaderCandidate(candidate: Leader) extends SensorState with Message
  case class SensorAlarm(sensorId: ID, active: Boolean) extends SensorState with Message
  case class AreaAlarm() extends SensorState with Message
  case class RemoveWater() extends SensorState with Message


  case class Leader(id: ID, leaderRef: ActorRef[SensorMsg])

  private class SensorBehaviour(context: ActorContext[SensorMsg], val id: ID, val area: Area,val coordinates: (Int,Int))
      extends AbstractBehavior[SensorMsg](context):
    var fireStationRef: List[ActorRef[State]] = List.empty
    var sensorsRefs: List[ActorRef[SensorMsg]] = List.empty
    private var isAreaAlarm: Boolean = false
    private var isLocalAlarm: Boolean = false
    private var leader: Leader = Leader(id, context.self)
    private var currentWaterLevel: WaterLevel = 50
    private var sensorsInAlarm: Set[ID] = Set.empty

    override def onMessage(msg: SensorMsg): Behavior[SensorMsg] = msg match
      case m: Receptionist.Listing =>
        m match
          case FireStation.Service.Listing(listings) =>
            if fireStationRef.isEmpty && listings.nonEmpty then
              listings.foreach(_ ! Hello(id, area, coordinates))
            else if listings.size > fireStationRef.size then
              listings.last ! Hello(id, area, coordinates)
            fireStationRef = listings.toList
          case Service.Listing(listings) =>
            if leader.id == id then
              listings.toList
                .filter(!sensorsRefs.contains(_))
                .foreach(_ ! LeaderCandidate(Leader(id, context.self)))
            else if !listings.contains(leader.leaderRef) then
              leader = Leader(id, context.self)
              sensorsRefs.foreach(_ ! LeaderCandidate(leader))
            sensorsRefs = listings.toList;
        this
      case Measuring() =>
        val waterMeasurement = detectWaterLevel()
        if !isAreaAlarm && waterMeasurement > MAX_NORMAL_WATER_LEVEL then
          isLocalAlarm = true
          leader.leaderRef ! SensorAlarm(id, true)
        println(s"New measuring: $waterMeasurement | my leader is: ${leader.id}")
        var msg: State with Message = Ok(id, area, waterMeasurement)
//        if leader.id != id then leader.leaderRef ! Ciao(id)
        if isAreaAlarm then
          msg = Alarm(id, area, waterMeasurement)

        if fireStationRef.nonEmpty then fireStationRef.foreach(_ ! msg)
        Behaviors.withTimers(timers =>
          timers.startSingleTimer("Measuring", Measuring(), getTimeout)
          this
        )
      case LeaderCandidate(candidate) =>
        if candidate.id <= leader.id then
          leader = candidate
          println(s"I have a new leader $candidate")
        else if id == leader.id then
          candidate.leaderRef ! LeaderCandidate(leader)
        this
      case SensorAlarm(sensorId, active) =>
        if leader.id != id then
          println("ERROR, I am not a leader ")
        else if active then
          sensorsInAlarm = sensorsInAlarm + sensorId
          if sensorsRefs.size / 2 + 1  <= sensorsInAlarm.size then
            sensorsRefs.foreach(_ ! AreaAlarm())
        this
      case AreaAlarm() =>
        isAreaAlarm = true
        this
      case RemoveWater() =>
        currentWaterLevel -= 30
        if currentWaterLevel < 0 then currentWaterLevel = 0
        isAreaAlarm = false
        this


    def detectWaterLevel(): WaterLevel =
      if currentWaterLevel < 100 then
        currentWaterLevel += (id % 3) + 4
      else if currentWaterLevel < 110 then
        currentWaterLevel += 1
      currentWaterLevel

  def getTimeout: FiniteDuration = FiniteDuration(3000 + Random.nextInt(500), TimeUnit.MILLISECONDS)
