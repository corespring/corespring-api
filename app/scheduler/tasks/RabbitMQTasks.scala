package scheduler.tasks

object RabbitMQTasks {
  val tasks:Seq[RabbitMQTask] = Seq(
    new SessionAggregateTask()
  )
}


