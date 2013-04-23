package controllers

case class InternalError(message: String, logType:LogType.LogType = LogType.printNone, addMessageToClientOutput:Boolean = false, var clientOutput:Option[String] = None){
  Log.u(logType,message)
  clientOutput = if (addMessageToClientOutput) clientOutput match{
    case Some(x) => Some(clientOutput+".\n"+message)
    case None => Some(message)
  } else clientOutput
}
