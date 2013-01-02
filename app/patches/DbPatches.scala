package patches

import models.DbVersion
import controllers.Log

object DbPatches{
  private lazy val patches:Seq[DbPatch] = getPatches
  def run(version:String) = {
    if (version > DbVersion.getVersion){
      patches.filter(_.version == version).foldRight[Either[InternalError,Unit]](Right(()))((patch,result) => {
        if(result.isRight){
          patch.run
        }else result
      }) match {
        case Right(_) => DbVersion.updated(version)
        case Left(error) => Log.f("an error occurred while running patches: "+error.getMessage)
      }
    }
  }
  private def getPatches:Seq[DbPatch] = {
    val classes = getClasses("patches")
    classes.foldRight[Seq[DbPatch]](Seq())((c,acc) => {
      if(c.getSuperclass() == classOf[DbPatch]) acc :+ c.newInstance().asInstanceOf[DbPatch]
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
        classes = classes :+ Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6));
      }
    }
    return classes;
  }
}
