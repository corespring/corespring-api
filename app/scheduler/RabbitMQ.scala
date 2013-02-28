package scheduler

import com.typesafe.config.ConfigFactory
import com.rabbitmq.client.{QueueingConsumer, Channel, ConnectionFactory, Connection}
import akka.actor.{ActorSystem, Actor, Props}
import play.api.libs.json._
import play.api.Logger

object RabbitMQ {
  private val RABBITMQ_HOST = ConfigFactory.load().getString("rabbitmq.host");
  private val GENERAL_QUEUE = "generic";
  private val RABBITMQ_WORKER_DISPATCHER = "rabbitmq-worker-dispatcher"
  val system = ActorSystem("MySystem")

  def main(args: Array[String]) {
    init
  }

  def init = {
    val connection = initializeConnection
    Logger.info("connection initialized")
    val generalChannel = initializeGeneralChannel(connection)
    Logger.info("general channel initialized")
    initializeGeneralListener(generalChannel)
    Logger.info("queueing tasks")
    queueTasks(generalChannel)
    Logger.info("tasks have been queued");
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
    Logger.info("initialized consumer")
    val deliveryActor = system.actorOf(Props(new Actor {
      protected def receive = {
        case delivery:QueueingConsumer.Delivery => {
          try{
            val msg = new String(delivery.getBody());
            Json.parse(msg) match {
              case JsObject(fields) => fields.find(_._1 == "taskName") match {
                case Some((_,JsString(taskName))) => {
                  RabbitMQTasks.tasks.get(taskName) match {
                    case Some(task) => system.actorOf(Props(new Actor {
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
    }).withDispatcher(RABBITMQ_WORKER_DISPATCHER))
    new Thread(new Runnable {
      def run() {
        Logger.info("begin consuming messages")
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
      Logger.info("scheduling task: "+taskName)
      val json = JsObject(Seq(
        "taskName" -> JsString(taskName),
        "data" -> task.data
      ))
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
