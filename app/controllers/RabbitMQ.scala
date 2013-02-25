package controllers

import com.typesafe.config.{Config, ConfigFactory}
import com.rabbitmq.client.{QueueingConsumer, Channel, ConnectionFactory, Connection}
import play.api.libs.concurrent.Akka
import akka.util.duration._
import akka.actor.{Actor, Props}
import play.api.Play.current

object RabbitMQ {
  val RABBITMQ_HOST = ConfigFactory.load().getString("rabbitmq.host");
  val RABBITMQ_QUEUE = ConfigFactory.load().getString("rabbitmq.queue");
  val RABBITMQ_EXCHANGEE = ConfigFactory.load().getString("rabbitmq.exchange");

  private val connection: Connection = null;

  /**
   * Return a connection if one doesn't exist. Else create
   * a new one
   */
  def getConnection(): Connection = {
    connection match {
      case null => {
        val factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.newConnection();
      }
      case _ => connection
    }
  }

  private def setupListener(receivingChannel: Channel, queue: String, f: (String) => Any) {
    Akka.system.scheduler.scheduleOnce(2 seconds,
      Akka.system.actorOf(Props(new ListeningActor(receivingChannel, queue, f))), "");
  }
  def startSending = {
    // create the connection
    val connection = getConnection();
    // create the channel we use to send
    val sendingChannel = connection.createChannel();
    // make sure the queue exists we want to send to
    sendingChannel.queueDeclare(RABBITMQ_QUEUE, false, false, false, null);

    val callback1 = (x: String) => Log.i("Received on queue callback 1: " + x);

    setupListener(connection.createChannel(), RABBITMQ_QUEUE, callback1);

    // create an actor that starts listening on the specified queue and passes the
    // received message to the provided callback
    val callback2 = (x: String) => Log.i("Received on queue callback 2: " + x);

    // setup the listener that sends to a specific queue using the SendingActor
    setupListener(connection.createChannel(), RABBITMQ_QUEUE, callback2);

    Akka.system.scheduler.schedule(2 seconds, 1 seconds
      , Akka.system.actorOf(Props(
        new SendingActor(channel = sendingChannel,
          queue = RABBITMQ_QUEUE)))
      , "MSG to Queue");
  }

  private class SendingActor(channel: Channel, queue: String) extends Actor {

    def receive = {
      case some: String => {
        val msg = (some + " : " + System.currentTimeMillis());
        channel.basicPublish("", queue, null, msg.getBytes());
        Log.i(msg);
      }
      case _ => {}
    }
  }

  private class ListeningActor(channel: Channel, queue: String, f: (String) => Any) extends Actor {

    // called on the initial run
    def receive = {
      case _ => startReceiving
    }

    def startReceiving = {

      val consumer = new QueueingConsumer(channel);
      channel.basicConsume(queue, true, consumer);

      while (true) {
        // wait for the message
        val delivery = consumer.nextDelivery();
        val msg = new String(delivery.getBody());

        // send the message to the provided callback function
        // and execute this in a subactor
        context.actorOf(Props(new Actor {
          def receive = {
            case some: String => f(some);
          }
        })) ! msg
      }
    }
  }
}
