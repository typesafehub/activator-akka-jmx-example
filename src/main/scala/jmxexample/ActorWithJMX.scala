package jmxexample

import java.lang.management.ManagementFactory
import javax.management.ObjectName

import akka.actor.Actor

object AkkaJmxRegistrar {
  val mbs = ManagementFactory.getPlatformMBeanServer

  def registerToMBeanServer(actor: Actor, objName: ObjectName) = mbs.registerMBean(actor, objName)

  def unregisterFromMBeanServer(objName: ObjectName) = mbs.unregisterMBean(objName)
}

/**
 *
 */
trait ActorWithJMX extends Actor {
  import jmxexample.AkkaJmxRegistrar._

  val objName = new ObjectName("jmxexample", {
    import scala.collection.JavaConverters._
    new java.util.Hashtable(
      Map(
        "name" -> self.path.toStringWithoutAddress,
        "type" -> getMXTypeName
      ).asJava
    )
  })

  def getMXTypeName : String

  override def preStart() = mbs.registerMBean(this, objName)

  override def postStop() = mbs.unregisterMBean(objName)
}