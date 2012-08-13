package controllers

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 8/13/12
 * Time: 12:37 PM
 * To change this template use File | Settings | File Templates.
 */
case class InternalError(message: String, logType:LogType.LogType = LogType.printNone, addMessageToClientOutput:Boolean = false, var clientOutput:Option[String] = None){
  Log.u(logType,message)
  clientOutput = if (addMessageToClientOutput) clientOutput match{
    case Some(x) => Some(clientOutput+".\n"+message)
    case None => Some(message)
  } else clientOutput
}
