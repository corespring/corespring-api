package scheduler

import play.api.libs.json.{JsValue, JsObject}
import akka.util.Duration

object RabbitMQTasks {
  lazy val tasks:Map[String,RabbitMQTask] = getTasks
  private def getTasks:Map[String,RabbitMQTask] = {
    val classes = getClasses("scheduler/tasks")
    classes.foldRight[Map[String,RabbitMQTask]](Map())((c,acc) => {
      if(c.getInterfaces.exists(_ == classOf[RabbitMQTask])) acc + (c.getSimpleName -> c.newInstance().asInstanceOf[RabbitMQTask])
      else acc
    })
  }
  private def getClasses(packageName:String):Seq[Class[_]] = {
    val classLoader = Thread.currentThread().getContextClassLoader();
    val resources = classLoader.getResources(packageName);
    var dirs = Seq[java.io.File]()
    while (resources.hasMoreElements()) {
      val resource = resources.nextElement();
      dirs = dirs :+ new java.io.File(resource.getFile());
    }
    var classes = Seq[Class[_]]();
    for (directory <- dirs) {
      classes = classes ++ findClasses(directory, packageName);
    }
    return classes
  }

  private def findClasses(directory:java.io.File, packageName:String):Seq[Class[_]] = {
    var classes = Seq[Class[_]]();
    if (!directory.exists()) {
      return classes;
    }
    val files = directory.listFiles();
    for (file <- files) {
      if (file.isDirectory()) {
        classes = classes ++ findClasses(file, packageName + "." + file.getName());
      } else if (file.getName().endsWith(".class")) {
        classes = classes :+ Class.forName(packageName.replace('/','.') + '.' + file.getName().substring(0, file.getName().length() - 6));
      }
    }
    return classes;
  }
}


