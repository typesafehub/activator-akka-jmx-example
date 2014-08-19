package jmxexample

import akka.actor._

import scala.concurrent.duration._

case object Greet

case class WhoToGreet(who: String)

case class Greeting(message: String)

case object GreetingAcknowledged

/**
 * MXBean interface: this determines what the JMX tool will see.
 */
trait GreeterMXBean {
  def getGreetingHistory: GreetingHistory

  def getGreetingHistoryMXView: GreetingHistoryMXView
}

/**
 * An actor with the ActorWithJMX
 */
class Greeter extends ActorWithJMX with GreeterMXBean {

  private[this] var greeting: String = ""

  // IMPORTANT: because JMX and the actor access greetingHistory through
  // different threads, it should be marked as volatile to keep memory synchronized.
  @volatile
  private[this] var greetingHistory: Option[GreetingHistory] = None

  def getGreetingHistory: GreetingHistory = greetingHistory.orNull

  def getGreetingHistoryMXView: GreetingHistoryMXView = greetingHistory.map(GreetingHistoryMXView(_)).orNull

  def receive: Receive = {
    case WhoToGreet(who) =>
      greeting = s"hello, $who"
    case Greet =>
      sender ! Greeting(greeting) // Send the current greeting back to the sender
    case GreetingAcknowledged =>
      greetingHistory = Some(GreetingHistory(new java.util.Date(), greeting, sender))
  }

  // Maps the MXType to this actor.
  override def getMXTypeName: String = "GreeterMXBean"
}

// prints a greeting
class GreetPrinter extends Actor {

  override def receive: Receive = {
    case Greeting(message) =>
      sender ! GreetingAcknowledged
      println(message)
  }
}

/**
 * The supervisor uses a custom supervisor strategy to stop an actor when JMX exceptions are received.
 */
class Supervisor extends ActorJMXSupervisor {

  override def preStart(): Unit = {
    super.preStart()

    val greeter: ActorRef = context.actorOf(Props[Greeter], "greeter")

    val inbox: Inbox = Inbox.create(context.system)

    // Tell the 'greeter' to change its 'greeting' message
    greeter.tell(WhoToGreet("akka"), ActorRef.noSender)

    // Ask the 'greeter for the latest 'greeting'
    // Reply should go to the "actor-in-a-box"
    inbox.send(greeter, Greet)

    // Wait 5 seconds for the reply with the 'greeting' message
    val Greeting(message1) = inbox.receive(5.seconds)
    println(s"Greeting: $message1")

    // Change the greeting and ask for it again
    greeter.tell(WhoToGreet("typesafe"), ActorRef.noSender)
    inbox.send(greeter, Greet)

    val Greeting(message2) = inbox.receive(5.seconds)
    println(s"Greeting: $message2")

    val greetPrinter: ActorRef = context.actorOf(Props[GreetPrinter], "greetPrinter")

    context.system.scheduler.schedule(0.seconds, 1.second, greeter, Greet)(context.system.dispatcher, greetPrinter)
  }

  override def receive: Actor.Receive = {
    case _ =>
      throw new IllegalStateException("This should never be called")
  }

}

object Main extends App {

  val system: ActorSystem = ActorSystem("jmxexample")

  val supervisor: ActorRef = system.actorOf(Props[Supervisor], "supervisor")

}
