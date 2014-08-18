package jmxexample

import java.lang.management.ManagementFactory
import javax.management.{ ObjectInstance, MBeanServer, ObjectName }

import akka.actor.Actor

object AkkaJmxRegistrar {
  val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer

  def registerToMBeanServer(actor: Actor, objName: ObjectName): ObjectInstance = mbs.registerMBean(actor, objName)

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