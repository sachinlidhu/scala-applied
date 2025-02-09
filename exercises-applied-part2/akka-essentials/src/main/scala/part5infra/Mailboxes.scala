package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object Mailboxes extends App {

  val system = ActorSystem("MailboxDemo", ConfigFactory.load().getConfig("mailboxesDemo"))

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case  message => log.info(message.toString)
    }
  }

  /**
   * Interesting case --1 - custom priorty mailbox...if ticket name start with P0 then its most impoertant
   * p0>p1>p2 ...
   */
  //step 1- mailbox defination
  class SupportTicketPriortyMailbox(settings: ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(
      PriorityGenerator {
        case message: String if message.startsWith("[P0]") => 0
        case message: String if message.startsWith("[P1]") => 1
        case message: String if message.startsWith("[P2]") => 2
        case message: String if message.startsWith("[P3]") => 3
        case _ => 4
      })

  //step 2 - make it known in config
  //step 3 - attach the dispatcher to an actor

  val supportTicketLogger = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))
  supportTicketLogger ! PoisonPill //even this will be least prioty
  supportTicketLogger ! "[P3]  this thing would be nice to have"
  supportTicketLogger ! "[P0]  this needs to be solved now"
  supportTicketLogger ! "[P1]  do this when u have the time"

  //after what time can i send another message and be prioritised accordingly

  /**
   * interesting case 2 - control-aware mailbox
   * we will use UnboundedControlAwareMailBox
   */
  //step 1 - mark important messages as control messages
  case object ManagementTicket extends ControlMessage

  /*
  step 2 - configure who gets the mailbox
  - make the actor attach to the mailbox
   */
   //method 1
   val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))

  controlAwareActor ! "[P3]  this thing would be nice to have"
  controlAwareActor ! "[P0]  this needs to be solved now"
  controlAwareActor ! ManagementTicket

  //method 2 - using deployment config
  val altControlAwareActor = system.actorOf(Props[SimpleActor],"altControlAwareActor")

  altControlAwareActor ! "[P3]  this thing would be nice to have"
  altControlAwareActor ! "[P0]  this needs to be solved now"
  altControlAwareActor ! ManagementTicket
}
