package jmxexample

import java.beans.ConstructorProperties

import scala.beans.BeanProperty

/**
 * MXBean interface: this determines what the JMX tool will see.
 */
trait GreeterMXBean {

  /**
   * Uses composite data view to show the greeting history.
   */
  def getGreetingHistory: GreetingHistory

  /**
   * Uses a mapping JMX to show the greeting history.
   */
  def getGreetingHistoryMXView: GreetingHistoryMXView
}

/**
 * The JMX view into the Greeter
 */
class GreeterMXBeanActor extends ActorWithJMX with GreeterMXBean {

  // Because JMX and the actor model access from different threads
  @volatile private[this] var greetingHistory: Option[GreetingHistory] = None

  def receive = {
    case gh: GreetingHistory =>
      greetingHistory = Some(gh)
  }

  def getGreetingHistory: GreetingHistory = greetingHistory.orNull

  def getGreetingHistoryMXView: GreetingHistoryMXView = greetingHistory.map(GreetingHistoryMXView(_)).orNull

  // Maps the MXType to this actor.
  override def getMXTypeName: String = "GreeterMXBean"
}

/**
 * The custom MX view class for GreetingHistory.  Private so it can only be
 * called by the companion object.
 */
class GreetingHistoryMXView @ConstructorProperties(Array(
  "lastGreetingDate",
  "greeting",
  "sender")
) private (@BeanProperty val lastGreetingDate: java.util.Date,
  @BeanProperty val greeting: String,
  @BeanProperty val sender: String,
  @BeanProperty val randomSet: java.util.Set[String])

/**
 * Companion object for the GreetingHistory view class.  Takes a GreetingHistory and
 * returns GreetingHistoryMXView
 */
object GreetingHistoryMXView {
  def apply(greetingHistory: GreetingHistory): GreetingHistoryMXView = {
    val lastGreetingDate: java.util.Date = greetingHistory.lastGreetedDate
    val greeting: String = greetingHistory.greeting
    val actorName: String = greetingHistory.sender.path.name
    val randomSet = scalaToJavaSetConverter(greetingHistory.randomSet)
    new GreetingHistoryMXView(lastGreetingDate, greeting, actorName, randomSet)
  }

  // http://stackoverflow.com/a/24840520/5266
  def scalaToJavaSetConverter[T](scalaSet: Set[T]): java.util.Set[String] = {
    val javaSet = new java.util.HashSet[String]()
    scalaSet.foreach(entry => javaSet.add(entry.toString))
    javaSet
  }
}