package org.corespring.qtiToV2

import org.corespring.qtiToV2.kds.TagCleaner
import play.api.libs.json._

import scala.xml.{Text, Unparsed, Node, XML}
import scala.xml.transform.{RuleTransformer, RewriteRule}

/**
 * Scala's XML parser wants convert entities to values. We want to preserve them, so we introduce a step that encodes
 * them as regular nodes so they won't be converted. When our XML is ready to be written back to a string, we change
 * those nodes back to their initial entity declarations.
 */
trait EntityEscaper {

  import EntityEscaper._

  private val entityRegex = "&#([0-9]*);".r

  /**
   * Replace all entity characters (e.g., "&radic;" or "&#945;") with nodes matching their unicode values, (e.g.,
   * <entity value='8730'/> or <entity value='945'/>).
   */
  def escapeEntities(xml: String): String =
    entityRegex.replaceAllIn(entities.foldLeft(encodeSafeEntities("""(?s)<!\[CDATA\[(.*?)\]\]>""".r.replaceAllIn(xml, "$1"))){ case(acc, entity) =>
      ((string: String) => dontEncode.contains(entity.char) match {
        case true => string
        case _ => string.replaceAllLiterally(entity.char.toString, entity.toXmlString)
      }).apply(((string: String) => entity.name match {
        case Some(name) => string.replaceAllLiterally(s"&${name};", entity.toXmlString)
        case _ => string
      }).apply(acc.replaceAllLiterally(s"&#${entity.unicode.toString};", entity.toXmlString)))
    }, """<entity value="$1"/>""")

  def unescapeEntities(xml: String) = (new RuleTransformer(new RewriteRule {
    override def transform(node: Node) = node.label match {
      case "entity" => Unparsed(s"&#${(node \ "@value").text};")
      case _ => node
    }
  }).transform(XML.loadString(s"<entity-escaper>$xml</entity-escaper>")).head.child.map(TagCleaner.clean)).mkString

  def encodeSafeEntities(xml: String): String =
    safe.foldLeft(xml){ case (acc, entity) => {
      acc
        .replaceAll(s"&${entity.name};", entity.char.toString)
        .replaceAll(s"&#${entity.unicode.toString};", entity.char.toString)
    }}


}

object EntityEscaper {

  val dontEncode = Seq('"', '&', '\'', '<', '>', '-') ++ 65.to(90).map(_.toChar)

  case class Entity(name: Option[String], char: Char, unicode: Int) {
    def toXmlString = s"""<entity value="${this.unicode.toString}"/>"""
  }

  /**
   * A mapping of all HTML entity names to their corresponding unicode decimal values (see
   * http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references as reference).
   */
  val entities: Seq[Entity] = Seq((Some("quot"), '"', 34), (Some("amp"), '&', 38), (Some("apos"), '\'', 39),
    (Some("lt"), '<', 60), (Some("gt"), '>', 62), (Some("nbsp"), ' ', 160), (Some("iexcl"), '¡', 161),
    (Some("cent"), '¢', 162), (Some("pound"), '£', 163), (Some("curren"), '¤', 164), (Some("yen"), '¥', 165),
    (Some("brvbar"), '¦', 166), (Some("sect"), '§', 167), (Some("uml"), '¨', 168), (Some("copy"), '©', 169),
    (Some("ordf"), 'ª', 170), (Some("laquo"), '«', 171), (Some("not"), '¬', 172), (Some("shy"), ' ', 173),
    (Some("reg"), '®', 174), (Some("macr"), '¯', 175), (Some("deg"), '°', 176), (Some("plusmn"), '±', 177),
    (Some("sup2"), '²', 178), (Some("sup3"), '³', 179), (Some("acute"), '´', 180), (Some("micro"), 'µ', 181),
    (Some("para"), '¶', 182), (Some("middot"), '·', 183), (Some("cedil"), '¸', 184), (Some("sup1"), '¹', 185),
    (Some("ordm"), 'º', 186), (Some("raquo"), '»', 187), (Some("frac14"), '¼', 188), (Some("frac12"), '½', 189),
    (Some("frac34"), '¾', 190), (Some("iquest"), '¿', 191), (Some("Agrave"), 'À', 192), (Some("Aacute"), 'Á', 193),
    (Some("Acirc"), 'Â', 194), (Some("Atilde"), 'Ã', 195), (Some("Auml"), 'Ä', 196), (Some("Aring"), 'Å', 197),
    (Some("AElig"), 'Æ', 198), (Some("Ccedil"), 'Ç', 199), (Some("Egrave"), 'È', 200), (Some("Eacute"), 'É', 201),
    (Some("Ecirc"), 'Ê', 202), (Some("Euml"), 'Ë', 203), (Some("Igrave"), 'Ì', 204), (Some("Iacute"), 'Í', 205),
    (Some("Icirc"), 'Î', 206), (Some("Iuml"), 'Ï', 207), (Some("ETH"), 'Ð', 208), (Some("Ntilde"), 'Ñ', 209),
    (Some("Ograve"), 'Ò', 210), (Some("Oacute"), 'Ó', 211), (Some("Ocirc"), 'Ô', 212), (Some("Otilde"), 'Õ', 213),
    (Some("Ouml"), 'Ö', 214), (Some("times"), '×', 215), (Some("Oslash"), 'Ø', 216), (Some("Ugrave"), 'Ù', 217),
    (Some("Uacute"), 'Ú', 218), (Some("Ucirc"), 'Û', 219), (Some("Uuml"), 'Ü', 220), (Some("Yacute"), 'Ý', 221),
    (Some("THORN"), 'Þ', 222), (Some("szlig"), 'ß', 223), (Some("agrave"), 'à', 224), (Some("aacute"), 'á', 225),
    (Some("acirc"), 'â', 226), (Some("atilde"), 'ã', 227), (Some("auml"), 'ä', 228), (Some("aring"), 'å', 229),
    (Some("aelig"), 'æ', 230), (Some("ccedil"), 'ç', 231), (Some("egrave"), 'è', 232), (Some("eacute"), 'é', 233),
    (Some("ecirc"), 'ê', 234), (Some("euml"), 'ë', 235), (Some("igrave"), 'ì', 236), (Some("iacute"), 'í', 237),
    (Some("icirc"), 'î', 238), (Some("iuml"), 'ï', 239), (Some("eth"), 'ð', 240), (Some("ntilde"), 'ñ', 241),
    (Some("ograve"), 'ò', 242), (Some("oacute"), 'ó', 243), (Some("ocirc"), 'ô', 244), (Some("otilde"), 'õ', 245),
    (Some("ouml"), 'ö', 246), (Some("divide"), '÷', 247), (Some("oslash"), 'ø', 248), (Some("ugrave"), 'ù', 249),
    (Some("uacute"), 'ú', 250), (Some("ucirc"), 'û', 251), (Some("uuml"), 'ü', 252), (Some("yacute"), 'ý', 253),
    (Some("thorn"), 'þ', 254), (Some("yuml"), 'ÿ', 255), (Some("OElig"), 'Œ', 338), (Some("oelig"), 'œ', 339),
    (Some("Scaron"), 'Š', 352), (Some("scaron"), 'š', 353), (Some("Yuml"), 'Ÿ', 376), (Some("fnof"), 'ƒ', 402),
    (Some("circ"), 'ˆ', 710), (Some("tilde"), '˜', 732), (Some("Alpha"), 'Α', 913), (Some("Beta"), 'Β', 914),
    (Some("Gamma"), 'Γ', 915), (Some("Delta"), 'Δ', 916), (Some("Epsilon"), 'Ε', 917), (Some("Zeta"), 'Ζ', 918),
    (Some("Eta"), 'Η', 919), (Some("Theta"), 'Θ', 920), (Some("Iota"), 'Ι', 921), (Some("Kappa"), 'Κ', 922),
    (Some("Lambda"), 'Λ', 923), (Some("Mu"), 'Μ', 924), (Some("Nu"), 'Ν', 925), (Some("Xi"), 'Ξ', 926),
    (Some("Omicron"), 'Ο', 927), (Some("Pi"), 'Π', 928), (Some("Rho"), 'Ρ', 929), (Some("Sigma"), 'Σ', 931),
    (Some("Tau"), 'Τ', 932), (Some("Upsilon"), 'Υ', 933), (Some("Phi"), 'Φ', 934), (Some("Chi"), 'Χ', 935),
    (Some("Psi"), 'Ψ', 936), (Some("Omega"), 'Ω', 937), (Some("alpha"), 'α', 945), (Some("beta"), 'β', 946),
    (Some("gamma"), 'γ', 947), (Some("delta"), 'δ', 948), (Some("epsilon"), 'ε', 949), (Some("zeta"), 'ζ', 950),
    (Some("eta"), 'η', 951), (Some("theta"), 'θ', 952), (Some("iota"), 'ι', 953), (Some("kappa"), 'κ', 954),
    (Some("lambda"), 'λ', 955), (Some("mu"), 'μ', 956), (Some("nu"), 'ν', 957), (Some("xi"), 'ξ', 958),
    (Some("omicron"), 'ο', 959), (Some("pi"), 'π', 960), (Some("rho"), 'ρ', 961), (Some("sigmaf"), 'ς', 962),
    (Some("sigma"), 'σ', 963), (Some("tau"), 'τ', 964), (Some("upsilon"), 'υ', 965), (Some("phi"), 'φ', 966),
    (Some("chi"), 'χ', 967), (Some("psi"), 'ψ', 968), (Some("omega"), 'ω', 969), (Some("thetasym"), 'ϑ', 977),
    (Some("upsih"), 'ϒ', 978), (Some("piv"), 'ϖ', 982), (Some("ensp"), ' ', 8194), (Some("emsp"), ' ', 8195),
    (Some("thinsp"), ' ', 8201), (Some("zwnj"), ' ', 8204), (Some("zwj"), ' ', 8205), (Some("lrm"), ' ', 8206),
    (Some("rlm"), ' ', 8207), (Some("ndash"), '–', 8211), (Some("mdash"), '—', 8212), (Some("lsquo"), '‘', 8216),
    (Some("rsquo"), '’', 8217), (Some("sbquo"), '‚', 8218), (Some("ldquo"), '“', 8220), (Some("rdquo"), '”', 8221),
    (Some("bdquo"), '„', 8222), (Some("dagger"), '†', 8224), (Some("Dagger"), '‡', 8225), (Some("bull"), '•', 8226),
    (Some("hellip"), '…', 8230), (Some("permil"), '‰', 8240), (Some("prime"), '′', 8242), (Some("Prime"), '″', 8243),
    (Some("lsaquo"), '‹', 8249), (Some("rsaquo"), '›', 8250), (Some("oline"), '‾', 8254), (Some("frasl"), '⁄', 8260),
    (Some("euro"), '€', 8364), (Some("image"), 'ℑ', 8465), (Some("weierp"), '℘', 8472), (Some("real"), 'ℜ', 8476),
    (Some("trade"), '™', 8482), (Some("alefsym"), 'ℵ', 8501), (Some("larr"), '←', 8592), (Some("uarr"), '↑', 8593),
    (Some("rarr"), '→', 8594), (Some("darr"), '↓', 8595), (Some("harr"), '↔', 8596), (Some("crarr"), '↵', 8629),
    (Some("lArr"), '⇐', 8656), (Some("uArr"), '⇑', 8657), (Some("rArr"), '⇒', 8658), (Some("dArr"), '⇓', 8659),
    (Some("hArr"), '⇔', 8660), (Some("forall"), '∀', 8704), (Some("part"), '∂', 8706), (Some("exist"), '∃', 8707),
    (Some("empty"), '∅', 8709), (Some("nabla"), '∇', 8711), (Some("isin"), '∈', 8712), (Some("notin"), '∉', 8713),
    (Some("ni"), '∋', 8715), (Some("prod"), '∏', 8719), (Some("sum"), '∑', 8721), (Some("minus"), '−', 8722),
    (Some("lowast"), '∗', 8727), (Some("radic"), '√', 8730), (Some("prop"), '∝', 8733), (Some("infin"), '∞', 8734),
    (Some("ang"), '∠', 8736), (Some("and"), '∧', 8743), (Some("or"), '∨', 8744), (Some("cap"), '∩', 8745),
    (Some("cup"), '∪', 8746), (Some("int"), '∫', 8747), (Some("there4"), '∴', 8756), (Some("sim"), '∼', 8764),
    (Some("cong"), '≅', 8773), (Some("asymp"), '≈', 8776), (Some("ne"), '≠', 8800), (Some("equiv"), '≡', 8801),
    (Some("le"), '≤', 8804), (Some("ge"), '≥', 8805), (Some("sub"), '⊂', 8834), (Some("sup"), '⊃', 8835),
    (Some("nsub"), '⊄', 8836), (Some("sube"), '⊆', 8838), (Some("supe"), '⊇', 8839), (Some("oplus"), '⊕', 8853),
    (Some("otimes"), '⊗', 8855), (Some("perp"), '⊥', 8869), (Some("sdot"), '⋅', 8901), (Some("lceil"), '⌈', 8968),
    (Some("rceil"), '⌉', 8969), (Some("lfloor"), '⌊', 8970), (Some("rfloor"), '⌋', 8971), (Some("lang"), '〈', 9001),
    (Some("rang"), '〉', 9002), (Some("loz"), '◊', 9674), (Some("spades"), '♠', 9824), (Some("clubs"), '♣', 9827),
    (Some("hearts"), '♥', 9829), (Some("diams"), '♦', 9830), (None, '∘', 8728))
  .map{ case (name, char, unicode) => Entity(name, char, unicode) }
  .toSeq

  val safe = Seq(39).map(c => entities.find(_.unicode == c)).flatten

}