package scheduler

import com.typesafe.config.ConfigFactory
import com.rabbitmq.client.{QueueingConsumer, Channel, ConnectionFactory, Connection}
import play.api.libs.concurrent.Akka
import akka.util.duration._
import akka.util.Duration
import akka.actor.{Actor, Props}
import play.api.Play.current
import actors.threadpool.{ExecutorService, Executors}
import play.api.libs.json._

object RabbitMQ {
  private val RABBITMQ_HOST = ConfigFactory.load().getString("rabbitmq.host");
  private val SESSION_AGGREGATE_QUEUE = "session_aggregate";
  private val GENERAL_QUEUE = "generic";
  private val e:ExecutorService = Executors.newFixedThreadPool(4);
  private var connection: Connection = null;
  private var generalChannel:Channel = null;

  def init = {
    initializeConnection
    initializeSessionAggregateQueue
    initializeGeneralQueue
  }

  private def initializeConnection = {
    val factory = new ConnectionFactory();
    factory.setHost(RABBITMQ_HOST);
    connection = factory.newConnection();
  }
  private def initializeSessionAggregateQueue {
    val sendingChannel = connection.createChannel();
    sendingChannel.queueDeclare(SESSION_AGGREGATE_QUEUE,false,false,false,null)
    Akka.system.scheduler.schedule(1 minute, 1 minute,
      Akka.system.actorOf(Props(new Actor {
        protected def receive = {
          case msg:String => {

          }
          case _ => throw new RuntimeException("did not receive a string message when running session aggregate actor")
        }
      })),
      "")
  }
  private def initializeGeneralQueue {
    generalChannel match {
      case null => {
        generalChannel = connection.createChannel();
        generalChannel.queueDeclare(GENERAL_QUEUE,false,false,false,null);
        generalChannel
      }
      case _ => generalChannel
    }
  }

  private def initializeGeneralListener(receivingChannel: Channel, queue: String) {
//    Akka.system.scheduler.scheduleOnce(2 seconds, Akka.system.actorOf(Props(new Actor {
//      protected def receive = {
//        case _ => {
//
//          val consumer = new QueueingConsumer(receivingChannel);
//          receivingChannel.basicConsume(queue, false, consumer);
//
//          while (true) {
//            // wait for the message
//            val delivery = consumer.nextDelivery();
//            val msg = new String(delivery.getBody());
//
//            // send the message to the provided callback function
//            // and execute this in a subactor
//            context.actorOf(Props(new Actor {
//              def receive = {
//                case some: String => Json.parse(some) match {
//                  case JsObject(fields) => fields.find(_._1 == "funcid") match {
//                    case Some((_,JsString(funcid))) => {
//                      val optdata = fields.find(_._1 == "data").flatMap(field => if(field._2.isInstanceOf[JsObject]) Some(field._2.asInstanceOf[JsObject]) else None)
//                      tasks.get(funcid) match {
//                        case Some(fn) => {
//                          tasks = tasks - "funcid"
//                          e.execute(new Runnable {
//                            def run() {fn(optdata)}
//                          })
//                        }
//                        case None => throw new RuntimeException("no function found for given id")
//                      }
//                    }
//                    case _ => throw new RuntimeException("no function id found in json")
//                  }
//                  case _ => throw new RuntimeException("could not parse message into json")
//                }
//              }
//            })) ! msg
//          }
//        }
//      }
//    })), "")
  }
  def schedule(initialDelay: Duration = Duration.Zero, frequency: Duration = Duration.Zero, optdata:Option[JsValue] = None)(fn: (Option[JsValue]) => Unit) {
//    def addFuncId:Option[JsValue] = optdata match {
//      case Some(data) => {
//        currentId = currentId + 1
//        Some(JsObject(Seq(
//          "funcid" -> JsString(currentId.toString),
//          "data" -> data
//        )))
//      }
//      case _ => None
//    }
//    Akka.system.scheduler.schedule(initialDelay, frequency, Akka.system.actorOf(Props(new Actor {
//      protected def receive = {
//        case Some(data) => {
//          tasks = tasks
//        }
//        case _ =>
//      }
//    })),addFuncId)
  }
}
