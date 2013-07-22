package scheduler

import com.typesafe.config.ConfigFactory
import com.rabbitmq.client.{QueueingConsumer, Channel, ConnectionFactory, Connection}
import akka.actor.{ActorSystem, Actor, Props}
import play.api.libs.json._
import play.api.Logger
import tasks.{RabbitMQTask, RabbitMQTasks}
import scala.concurrent.ExecutionContext

object RabbitMQ {
  private val RABBITMQ_HOST = ConfigFactory.load().getString("rabbitmq.host");
  private val GENERAL_QUEUE = "generic";
  private val RABBITMQ_WORKER_DISPATCHER = "rabbitmq-worker-dispatcher"
  private val system = ActorSystem("MySystem")
  private val tasks:Map[String,RabbitMQTask] = RabbitMQTasks.tasks.foldRight[Map[String,RabbitMQTask]](Map())((task,acc) => acc + (task.getClass.getSimpleName -> task))

  def main(args: Array[String]) {
    init
  }

  def init = {
    val connection = initializeConnection
    val generalChannel = initializeGeneralChannel(connection)
    initializeGeneralListener(generalChannel)
    queueTasks(generalChannel)
  }

  private def initializeConnection:Connection = {
    val factory = new ConnectionFactory();
    if(RABBITMQ_HOST.startsWith("amqp://")) factory.setUri(RABBITMQ_HOST)
      else factory.setHost(RABBITMQ_HOST);
    factory.newConnection();
  }

  private def initializeGeneralChannel(conn:Connection):Channel = {
    val generalChannel = conn.createChannel();
    generalChannel.queueDeclare(GENERAL_QUEUE,false,false,false,null);
    generalChannel
  }

  private def initializeGeneralListener(channel:Channel) {
    val consumer = new QueueingConsumer(channel);
    channel.basicConsume(GENERAL_QUEUE, false, consumer);
    val deliveryActor = system.actorOf(Props(new Actor {
      protected def receive = {
        case delivery:QueueingConsumer.Delivery => {
          try{
            val msg = new String(delivery.getBody());
            Json.parse(msg) match {
              case JsObject(fields) => fields.find(_._1 == "taskName") match {
                case Some((_,JsString(taskName))) => {
                  Logger.info("retrieved taskName. finding corresponding task")
                  tasks.get(taskName) match {
                    case Some(task) => system.actorOf(Props(new Actor {
                      protected def receive = {
                        case data:JsValue => {
                          Logger.info("found task. running.")
                          task.data = data
                          task.run()
                        }
                      }
                    })) ! fields.find(_._1 == "data").map(_._2).getOrElse(JsNull)
                    case None => throw new RuntimeException("no function found for given id: "+taskName)
                  }
                }
                case _ => throw new RuntimeException("no function id found in json")
              }
              case _ => throw new RuntimeException("could not parse message into json")
            }
          }finally{
            channel.basicAck(delivery.getEnvelope().getDeliveryTag,false);
          }
        }
        case _ =>
      }
    }).withDispatcher(RABBITMQ_WORKER_DISPATCHER))
    new Thread(new Runnable {
      def run() {
        while(true){
          val delivery = consumer.nextDelivery();
          deliveryActor ! delivery
          Thread.sleep(500)
        }
      }
    }).start()
  }
  def queueTasks(channel:Channel) {
    tasks.foreach{case(taskName,task) => {
      Logger.info("scheduling task: "+taskName)
      val json = JsObject(Seq(
        "taskName" -> JsString(taskName),
        "data" -> task.data
      ))
      import ExecutionContext.Implicits.global
      system.scheduler.schedule(task.initialDelay, task.frequency, system.actorOf(Props(new Actor {
        protected def receive = {
          case data:String => {
            channel.basicPublish("",GENERAL_QUEUE,null,data.getBytes)
          }
          case _ =>
        }
      })),json.toString())
    }}
  }
}
