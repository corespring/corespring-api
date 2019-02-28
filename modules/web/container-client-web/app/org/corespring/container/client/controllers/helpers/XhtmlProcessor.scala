package org.corespring.container.client.controllers.helpers

import java.io.StringReader

import org.htmlcleaner._
import org.xml.sax.InputSource

trait XhtmlProcessor {
  def toWellFormedXhtml(html: String, wrapperTag: String = "div"): String
}

object XhtmlProcessor extends XhtmlProcessor {

  def process(transformations: Seq[TagTransformation],
    postProcessors: Seq[TagNode => Unit],
    xhtml: String): String = {

    val cleaner: HtmlCleaner = getCleaner
    val transformationHolder: CleanerTransformations = new CleanerTransformations()
    transformations.foreach(transformationHolder.addTransformation)
    cleaner.getProperties.setCleanerTransformations(transformationHolder)
    val n: TagNode = cleaner.clean(xhtml)
    postProcessors.foreach(pp => pp(n))
    serialize(n, cleaner)
  }

  /**
   * Returns wellformed xhtml aka it is parseable by an xml parser
   * Makes minimal changes and only wraps the content if it needs to
   *
   * @param html
   * @param wrapperTag
   * @return
   */
  def toWellFormedXhtml(html: String, wrapperTag: String = "div"): String = {
    val cleaner = getCleaner
    val n: TagNode = cleaner.clean(html)
    val out = serialize(n, cleaner)

    if (isValidXml(out)) {
      out
    } else {
      require(Seq("div", "span").contains(wrapperTag), s"You can only wrap in div or span - not: $wrapperTag")
      s"<$wrapperTag>$out</$wrapperTag>"
    }
  }

  def isValidXml(html: String): Boolean = try {
    scala.xml.XML.loadString(html)
    true
  } catch {
    case e: Throwable => false
  }

  private def getCleaner = {
    val cleaner: HtmlCleaner = new HtmlCleaner(tagProvider)
    cleaner.getProperties.setUseEmptyElementTags(false)
    cleaner.getProperties.setOmitXmlDeclaration(true)
    cleaner.getProperties.setOmitHtmlEnvelope(true)
    cleaner.getProperties.setNamespacesAware(false)
    cleaner
  }

  private def serialize(n: TagNode, cleaner: HtmlCleaner): String = {
    val serializer = new SimpleHtmlSerializer(cleaner.getProperties)
    serializer.getAsString(n)
  }

  val tagProvider = new ConfigFileTagProvider(new InputSource(new StringReader(
    <tags>
      <tag name="div" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="span" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="meta" content="none" section="head" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="link" content="none" section="head" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="title" content="text" section="head" deprecated="false" unique="true" ignore-permitted="false"/>
      <tag name="bgsound" content="none" section="head" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="h1" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          h1,h2,h3,h4,h5,h6,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="h2" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          h1,h2,h3,h4,h5,h6,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="h3" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          h1,h2,h3,h4,h5,h6,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="h4" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          h1,h2,h3,h4,h5,h6,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="h5" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          h1,h2,h3,h4,h5,h6,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="h6" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          h1,h2,h3,h4,h5,h6,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="p" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="strong" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="em" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="abbr" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="acronym" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="address" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="bdo" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="blockquote" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="cite" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="q" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="code" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="ins" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="del" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="dfn" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="kbd" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="pre" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="samp" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="listing" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="var" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="br" content="none" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="wbr" content="none" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="nobr" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-tags>nobr</close-before-tags>
      </tag>
      <tag name="xmp" content="text" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="a" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-tags>a</close-before-tags>
      </tag>
      <tag name="base" content="none" section="head" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="img" content="none" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="area" content="none" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <fatal-tags>map</fatal-tags>
        <close-before-tags>area</close-before-tags>
      </tag>
      <tag name="map" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-tags>map</close-before-tags>
      </tag>
      <tag name="object" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="param" content="none" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="applet" content="all" section="body" deprecated="true" unique="false" ignore-permitted="false"/>
      <tag name="xml" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="ul" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="ol" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="li" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          li,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="dl" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="dt" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-tags>dt,dd</close-before-tags>
      </tag>
      <tag name="dd" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-tags>dt,dd</close-before-tags>
      </tag>
      <tag name="menu" content="all" section="body" deprecated="true" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="dir" content="all" section="body" deprecated="true" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="table" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <allowed-children-tags>tr,tbody,thead,tfoot,colgroup,col,form,caption,tr</allowed-children-tags>
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          tr,thead,tbody,tfoot,caption,colgroup,table,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param
        </close-before-tags>
      </tag>
      <tag name="tr" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <fatal-tags>table</fatal-tags>
        <req-enclosing-tags>tbody</req-enclosing-tags>
        <allowed-children-tags>td,th</allowed-children-tags>
        <higher-level-tags>thead,tfoot</higher-level-tags>
        <close-before-tags>tr,td,th,caption,colgroup</close-before-tags>
      </tag>
      <tag name="td" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <fatal-tags>table</fatal-tags>
        <req-enclosing-tags>tr</req-enclosing-tags>
        <close-before-tags>td,th,caption,colgroup</close-before-tags>
      </tag>
      <tag name="th" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <fatal-tags>table</fatal-tags>
        <req-enclosing-tags>tr</req-enclosing-tags>
        <close-before-tags>td,th,caption,colgroup</close-before-tags>
      </tag>
      <tag name="tbody" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <fatal-tags>table</fatal-tags>
        <allowed-children-tags>tr,form</allowed-children-tags>
        <close-before-tags>td,th,tr,tbody,thead,tfoot,caption,colgroup</close-before-tags>
      </tag>
      <tag name="thead" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <fatal-tags>table</fatal-tags>
        <allowed-children-tags>tr,form</allowed-children-tags>
        <close-before-tags>td,th,tr,tbody,thead,tfoot,caption,colgroup</close-before-tags>
      </tag>
      <tag name="tfoot" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <fatal-tags>table</fatal-tags>
        <allowed-children-tags>tr,form</allowed-children-tags>
        <close-before-tags>td,th,tr,tbody,thead,tfoot,caption,colgroup</close-before-tags>
      </tag>
      <tag name="col" content="none" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <fatal-tags>table</fatal-tags>
      </tag>
      <tag name="colgroup" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <fatal-tags>table</fatal-tags>
        <allowed-children-tags>col</allowed-children-tags>
        <close-before-tags>td,th,tr,tbody,thead,tfoot,caption,colgroup</close-before-tags>
      </tag>
      <tag name="caption" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <fatal-tags>table</fatal-tags>
        <close-before-tags>td,th,tr,tbody,thead,tfoot,caption,colgroup</close-before-tags>
      </tag>
      <tag name="form" content="all" section="body" deprecated="false" unique="false" ignore-permitted="true">
        <forbidden-tags>form</forbidden-tags>
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          option,optgroup,textarea,select,fieldset,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="input" content="none" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-tags>select,optgroup,option</close-before-tags>
      </tag>
      <tag name="textarea" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-tags>select,optgroup,option</close-before-tags>
      </tag>
      <tag name="select" content="all" section="body" deprecated="false" unique="false" ignore-permitted="true">
        <allowed-children-tags>option,optgroup</allowed-children-tags>
        <close-before-tags>option,optgroup,select</close-before-tags>
      </tag>
      <tag name="option" content="text" section="body" deprecated="false" unique="false" ignore-permitted="true">
        <fatal-tags>select</fatal-tags>
        <close-before-tags>option</close-before-tags>
      </tag>
      <tag name="optgroup" content="all" section="body" deprecated="false" unique="false" ignore-permitted="true">
        <fatal-tags>select</fatal-tags>
        <allowed-children-tags>option</allowed-children-tags>
        <close-before-tags>optgroup</close-before-tags>
      </tag>
      <tag name="button" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-tags>select,optgroup,option</close-before-tags>
      </tag>
      <tag name="label" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="fieldset" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="legend" content="text" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <req-enclosing-tags>fieldset</req-enclosing-tags>
        <close-before-tags>legend</close-before-tags>
      </tag>
      <tag name="isindex" content="none" section="body" deprecated="true" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="script" content="all" section="all" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="style" content="all" section="all" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="noscript" content="all" section="all" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="b" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-inside-copy-after-tags>u,i,tt,sub,sup,big,small,strike,blink,s</close-inside-copy-after-tags>
      </tag>
      <tag name="i" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-inside-copy-after-tags>b,u,tt,sub,sup,big,small,strike,blink,s</close-inside-copy-after-tags>
      </tag>
      <tag name="u" content="all" section="body" deprecated="true" unique="false" ignore-permitted="false">
        <close-inside-copy-after-tags>b,i,tt,sub,sup,big,small,strike,blink,s</close-inside-copy-after-tags>
      </tag>
      <tag name="tt" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-inside-copy-after-tags>b,u,i,sub,sup,big,small,strike,blink,s</close-inside-copy-after-tags>
      </tag>
      <tag name="sub" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-inside-copy-after-tags>b,u,i,tt,sup,big,small,strike,blink,s</close-inside-copy-after-tags>
      </tag>
      <tag name="sup" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-inside-copy-after-tags>b,u,i,tt,sub,big,small,strike,blink,s</close-inside-copy-after-tags>
      </tag>
      <tag name="big" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-inside-copy-after-tags>b,u,i,tt,sub,sup,small,strike,blink,s</close-inside-copy-after-tags>
      </tag>
      <tag name="small" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-inside-copy-after-tags>b,u,i,tt,sub,sup,big,strike,blink,s</close-inside-copy-after-tags>
      </tag>
      <tag name="strike" content="all" section="body" deprecated="true" unique="false" ignore-permitted="false">
        <close-inside-copy-after-tags>b,u,i,tt,sub,sup,big,small,blink,s</close-inside-copy-after-tags>
      </tag>
      <tag name="blink" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-inside-copy-after-tags>b,u,i,tt,sub,sup,big,small,strike,s</close-inside-copy-after-tags>
      </tag>
      <tag name="marquee" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="s" content="all" section="body" deprecated="true" unique="false" ignore-permitted="false">
        <close-inside-copy-after-tags>b,u,i,tt,sub,sup,big,small,strike,blink</close-inside-copy-after-tags>
      </tag>
      <tag name="hr" content="none" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="font" content="all" section="body" deprecated="true" unique="false" ignore-permitted="false"/>
      <tag name="basefont" content="none" section="body" deprecated="true" unique="false" ignore-permitted="false"/>
      <tag name="center" content="all" section="body" deprecated="true" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
      <tag name="comment" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="server" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="iframe" content="all" section="body" deprecated="false" unique="false" ignore-permitted="false"/>
      <tag name="embed" content="none" section="body" deprecated="false" unique="false" ignore-permitted="false">
        <close-before-copy-inside-tags>
          a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font
        </close-before-copy-inside-tags>
        <close-before-tags>
          p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml
        </close-before-tags>
      </tag>
    </tags>.toString)))

}

