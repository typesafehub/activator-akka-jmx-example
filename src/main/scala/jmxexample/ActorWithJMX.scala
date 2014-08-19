package jmxexample

import java.lang.management.ManagementFactory
import javax.management._

import akka.actor.{ Actor, ActorLogging }

object AkkaJmxRegistrar {
  val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer

  @throws[InstanceAlreadyExistsException]
  @throws[MBeanRegistrationException]
  @throws[RuntimeMBeanException]
  @throws[RuntimeErrorException]
  @throws[NotCompliantMBeanException]
  @throws[RuntimeOperationsException]
  def registerToMBeanServer(actor: Actor, objName: ObjectName): ObjectInstance = mbs.registerMBean(actor, objName)

  @throws[RuntimeOperationsException]
  @throws[RuntimeMBeanException]
  @throws[RuntimeErrorException]
  @throws[InstanceNotFoundException]
  @throws[MBeanRegistrationException]
  def unregisterFromMBeanServer(objName: ObjectName): Unit = mbs.unregisterMBean(objName)
}

/**
 *
 */
trait ActorWithJMX extends Actor {

  import jmxexample.AkkaJmxRegistrar._

  val objName: ObjectName = new ObjectName("jmxexample", {
    import scala.collection.JavaConverters._
    new java.util.Hashtable(
      Map(
        "name" -> self.path.toStringWithoutAddress,
        "type" -> getMXTypeName
      ).asJava
    )
  })

  def getMXTypeName: String

  override def preStart(): Unit = mbs.registerMBean(this, objName)

  override def postStop(): Unit = mbs.unregisterMBean(objName)
}

/**
 * Defines a supervisor strategy for a parent actor that contains JMX registered actors.
 */
trait ActorJMXSupervisor extends Actor with ActorLogging {

  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._
  import scala.concurrent.duration._

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case e: JMRuntimeException =>
        log.error(e, "Supervisor strategy STOPPING actor from errors during JMX invocation")
        Stop
      case e: JMException =>
        log.error(e, "Supervisor strategy STOPPING actor from incorrect invocation of JMX registration")
        Stop
      case t =>
        // Use the default supervisor strategy otherwise.
        super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
    }

}