package testplayer.controllers

import xml.Elem

class QtiScriptLoader {

}

object QtiScriptLoader{
  def apply( qti : Elem ) : QtiScriptLoader = {
    new QtiScriptLoader()
  }
}
