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
import controllers.Log
import akka.dispatch.Dispatchers

object RabbitMQ {
  private val RABBITMQ_HOST = ConfigFactory.load().getString("rabbitmq.host");
  private val GENERAL_QUEUE = "generic";
  private val RABBITMQ_WORKER_DISPATCHER = "rabbitmq-worker-dispatcher"


  def init = {
    val connection = initializeConnection
    val generalChannel = initializeGeneralChannel(connection)
    initializeGeneralListener(generalChannel)
    queueTasks(generalChannel)
  }

  private def initializeConnection:Connection = {
    val factory = new ConnectionFactory();
    factory.setHost(RABBITMQ_HOST);
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
    val deliveryActor = Akka.system.actorOf(Props(new Actor {
      protected def receive = {
        case delivery:QueueingConsumer.Delivery => {
          try{
            val msg = new String(delivery.getBody());
            Json.parse(msg) match {
              case JsObject(fields) => fields.find(_._1 == "taskName") match {
                case Some((_,JsString(taskName))) => {
                  RabbitMQTasks.tasks.get(taskName) match {
                    case Some(task) => Akka.system.actorOf(Props(new Actor {
                      protected def receive = {
                        case data:JsValue => {
                          task.data = data
                          task.run()
                        }
                      }
                    })) ! fields.find(_._1 == "data").map(_._2).getOrElse(JsNull)
                    case None => throw new RuntimeException("no function found for given id")
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
    }))
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
    RabbitMQTasks.tasks.foreach{case (taskName,task) => {
      val json = JsObject(Seq(
        "taskName" -> JsString(taskName),
        "data" -> task.data
      ))
      Akka.system.scheduler.schedule(task.initialDelay, task.frequency, Akka.system.actorOf(Props(new Actor {
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
