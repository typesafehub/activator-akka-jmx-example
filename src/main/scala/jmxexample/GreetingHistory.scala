package jmxexample

import java.beans.ConstructorProperties
import javax.management.openmbean._

import akka.actor.ActorRef

import scala.beans.BeanProperty
import scala.collection.mutable.ListBuffer

/**
 * GreetingHistory is a case class with an ActorRef, which can't be mapped into an MXBean.
 *
 * There are two ways of handling this.
 *
 * Either you define CompositeDataView on the case class, and then add "sender" in the
 * CompositeDataSupport by hand.
 *
 * Or you define a custom view class for the MXBean, with mapped properties.
 *
 * They will look the same from the outside.  I personally prefer using a view class
 * as it can be defined well away from the case class itself.
 */
case class GreetingHistory(@BeanProperty lastGreetedDate: java.util.Date,
    @BeanProperty greeting: String,
    sender: ActorRef,
    randomSet: Set[String] = Set("1", "2", "3")) extends CompositeDataView {

  /**
   * Converts the GreetingHistory into a CompositeData object, including the "sender" value.
   */
  override def toCompositeData(ct: CompositeType): CompositeData = {
    import scala.collection.JavaConverters._

    // Deal with all the known properties...
    val itemNames = new ListBuffer[String]()
    itemNames ++= ct.keySet().asScala

    val itemDescriptions = new ListBuffer[String]()
    val itemTypes = new ListBuffer[OpenType[_]]()
    for (item <- itemNames) {
      itemDescriptions += ct.getDescription(item)
      itemTypes += ct.getType(item)
    }

    // Add the sender here, as it doesn't correspond to a known SimpleType...
    itemNames += "sender"
    itemDescriptions += "the sender"
    itemTypes += SimpleType.STRING

    val compositeType = new CompositeType(ct.getTypeName,
      ct.getDescription,
      itemNames.toArray,
      itemDescriptions.toArray,
      itemTypes.toArray)

    // Set up the data in given order explicitly.
    val data = Map(
      "lastGreetedDate" -> lastGreetedDate,
      "greeting" -> greeting,
      "sender" -> sender.path.name
    ).asJava

    val compositeData = new CompositeDataSupport(compositeType, data)
    require(ct.isValue(compositeData))

    compositeData
  }
}
