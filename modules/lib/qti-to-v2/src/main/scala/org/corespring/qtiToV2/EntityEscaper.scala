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

  /**
   * Replace all entity characters (e.g., "&radic;" or "&#945;") with nodes matching their unicode values, (e.g.,
   * <entity value='8730'/> or <entity value='945'/>).
   */
  def escapeEntities(xml: String): String =
    entities.foldLeft(encodeSafeEntities("""(?s)<!\[CDATA\[(.*?)\]\]>""".r.replaceAllIn(xml, "$1"))){ case(acc, entity) =>
      acc
        .replaceAllLiterally(entity.char.toString, s"""<entity value="${entity.unicode.toString}/>""")
        .replaceAllLiterally(s"&#${entity.unicode.toString};", s"""<entity value="${entity.unicode.toString}"/>""")
        .replaceAllLiterally(s"&${entity.name};", s"""<entity value="${entity.unicode.toString}"/>""")
    }

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

  case class Entity(name: String, char: Char, unicode: Int)

  /**
   * A mapping of all HTML entity names to their corresponding unicode decimal values (see
   * http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references as reference).
   */
  val entities: Seq[Entity] = Seq(("quot", '"', 34), ("amp", '&', 38), ("apos", '\'', 39), ("lt", '<', 60),
    ("gt", '>', 62), ("nbsp", ' ', 160), ("iexcl", '¡', 161), ("cent", '¢', 162), ("pound", '£', 163),
    ("curren", '¤', 164), ("yen", '¥', 165), ("brvbar", '¦', 166), ("sect", '§', 167), ("uml", '¨', 168),
    ("copy", '©', 169), ("ordf", 'ª', 170), ("laquo", '«', 171), ("not", '¬', 172), ("shy", ' ', 173),
    ("reg", '®', 174), ("macr", '¯', 175), ("deg", '°', 176), ("plusmn", '±', 177), ("sup2", '²', 178),
    ("sup3", '³', 179), ("acute", '´', 180), ("micro", 'µ', 181), ("para", '¶', 182), ("middot", '·', 183),
    ("cedil", '¸', 184), ("sup1", '¹', 185), ("ordm", 'º', 186), ("raquo", '»', 187), ("frac14", '¼', 188),
    ("frac12", '½', 189), ("frac34", '¾', 190), ("iquest", '¿', 191), ("Agrave", 'À', 192), ("Aacute", 'Á', 193),
    ("Acirc", 'Â', 194), ("Atilde", 'Ã', 195), ("Auml", 'Ä', 196), ("Aring", 'Å', 197), ("AElig", 'Æ', 198),
    ("Ccedil", 'Ç', 199), ("Egrave", 'È', 200), ("Eacute", 'É', 201), ("Ecirc", 'Ê', 202), ("Euml", 'Ë', 203),
    ("Igrave", 'Ì', 204), ("Iacute", 'Í', 205), ("Icirc", 'Î', 206), ("Iuml", 'Ï', 207), ("ETH", 'Ð', 208),
    ("Ntilde", 'Ñ', 209), ("Ograve", 'Ò', 210), ("Oacute", 'Ó', 211), ("Ocirc", 'Ô', 212), ("Otilde", 'Õ', 213),
    ("Ouml", 'Ö', 214), ("times", '×', 215), ("Oslash", 'Ø', 216), ("Ugrave", 'Ù', 217), ("Uacute", 'Ú', 218),
    ("Ucirc", 'Û', 219), ("Uuml", 'Ü', 220), ("Yacute", 'Ý', 221), ("THORN", 'Þ', 222), ("szlig", 'ß', 223),
    ("agrave", 'à', 224), ("aacute", 'á', 225), ("acirc", 'â', 226), ("atilde", 'ã', 227), ("auml", 'ä', 228),
    ("aring", 'å', 229), ("aelig", 'æ', 230), ("ccedil", 'ç', 231), ("egrave", 'è', 232), ("eacute", 'é', 233),
    ("ecirc", 'ê', 234), ("euml", 'ë', 235), ("igrave", 'ì', 236), ("iacute", 'í', 237), ("icirc", 'î', 238),
    ("iuml", 'ï', 239), ("eth", 'ð', 240), ("ntilde", 'ñ', 241), ("ograve", 'ò', 242), ("oacute", 'ó', 243),
    ("ocirc", 'ô', 244), ("otilde", 'õ', 245), ("ouml", 'ö', 246), ("divide", '÷', 247), ("oslash", 'ø', 248),
    ("ugrave", 'ù', 249), ("uacute", 'ú', 250), ("ucirc", 'û', 251), ("uuml", 'ü', 252), ("yacute", 'ý', 253),
    ("thorn", 'þ', 254), ("yuml", 'ÿ', 255), ("OElig", 'Œ', 338), ("oelig", 'œ', 339), ("Scaron", 'Š', 352),
    ("scaron", 'š', 353), ("Yuml", 'Ÿ', 376), ("fnof", 'ƒ', 402), ("circ", 'ˆ', 710), ("tilde", '˜', 732),
    ("Alpha", 'Α', 913), ("Beta", 'Β', 914), ("Gamma", 'Γ', 915), ("Delta", 'Δ', 916), ("Epsilon", 'Ε', 917),
    ("Zeta", 'Ζ', 918), ("Eta", 'Η', 919), ("Theta", 'Θ', 920), ("Iota", 'Ι', 921), ("Kappa", 'Κ', 922),
    ("Lambda", 'Λ', 923), ("Mu", 'Μ', 924), ("Nu", 'Ν', 925), ("Xi", 'Ξ', 926), ("Omicron", 'Ο', 927),
    ("Pi", 'Π', 928), ("Rho", 'Ρ', 929), ("Sigma", 'Σ', 931), ("Tau", 'Τ', 932), ("Upsilon", 'Υ', 933),
    ("Phi", 'Φ', 934), ("Chi", 'Χ', 935), ("Psi", 'Ψ', 936), ("Omega", 'Ω', 937), ("alpha", 'α', 945),
    ("beta", 'β', 946), ("gamma", 'γ', 947), ("delta", 'δ', 948), ("epsilon", 'ε', 949), ("zeta", 'ζ', 950),
    ("eta", 'η', 951), ("theta", 'θ', 952), ("iota", 'ι', 953), ("kappa", 'κ', 954), ("lambda", 'λ', 955),
    ("mu", 'μ', 956), ("nu", 'ν', 957), ("xi", 'ξ', 958), ("omicron", 'ο', 959), ("pi", 'π', 960), ("rho", 'ρ', 961),
    ("sigmaf", 'ς', 962), ("sigma", 'σ', 963), ("tau", 'τ', 964), ("upsilon", 'υ', 965), ("phi", 'φ', 966),
    ("chi", 'χ', 967), ("psi", 'ψ', 968), ("omega", 'ω', 969), ("thetasym", 'ϑ', 977), ("upsih", 'ϒ', 978),
    ("piv", 'ϖ', 982), ("ensp", ' ', 8194), ("emsp", ' ', 8195), ("thinsp", ' ', 8201), ("zwnj", ' ', 8204),
    ("zwj", ' ', 8205), ("lrm", ' ', 8206), ("rlm", ' ', 8207), ("ndash", '–', 8211), ("mdash", '—', 8212),
    ("lsquo", '‘', 8216), ("rsquo", '’', 8217), ("sbquo", '‚', 8218), ("ldquo", '“', 8220), ("rdquo", '”', 8221),
    ("bdquo", '„', 8222), ("dagger", '†', 8224), ("Dagger", '‡', 8225), ("bull", '•', 8226), ("hellip", '…', 8230),
    ("permil", '‰', 8240), ("prime", '′', 8242), ("Prime", '″', 8243), ("lsaquo", '‹', 8249), ("rsaquo", '›', 8250),
    ("oline", '‾', 8254), ("frasl", '⁄', 8260), ("euro", '€', 8364), ("image", 'ℑ', 8465), ("weierp", '℘', 8472),
    ("real", 'ℜ', 8476), ("trade", '™', 8482), ("alefsym", 'ℵ', 8501), ("larr", '←', 8592), ("uarr", '↑', 8593),
    ("rarr", '→', 8594), ("darr", '↓', 8595), ("harr", '↔', 8596), ("crarr", '↵', 8629), ("lArr", '⇐', 8656),
    ("uArr", '⇑', 8657), ("rArr", '⇒', 8658), ("dArr", '⇓', 8659), ("hArr", '⇔', 8660), ("forall", '∀', 8704),
    ("part", '∂', 8706), ("exist", '∃', 8707), ("empty", '∅', 8709), ("nabla", '∇', 8711), ("isin", '∈', 8712),
    ("notin", '∉', 8713), ("ni", '∋', 8715), ("prod", '∏', 8719), ("sum", '∑', 8721), ("minus", '−', 8722),
    ("lowast", '∗', 8727), ("radic", '√', 8730), ("prop", '∝', 8733), ("infin", '∞', 8734), ("ang", '∠', 8736),
    ("and", '∧', 8743), ("or", '∨', 8744), ("cap", '∩', 8745), ("cup", '∪', 8746), ("int", '∫', 8747),
    ("there4", '∴', 8756), ("sim", '∼', 8764), ("cong", '≅', 8773), ("asymp", '≈', 8776), ("ne", '≠', 8800),
    ("equiv", '≡', 8801), ("le", '≤', 8804), ("ge", '≥', 8805), ("sub", '⊂', 8834), ("sup", '⊃', 8835),
    ("nsub", '⊄', 8836), ("sube", '⊆', 8838), ("supe", '⊇', 8839), ("oplus", '⊕', 8853), ("otimes", '⊗', 8855),
    ("perp", '⊥', 8869), ("sdot", '⋅', 8901), ("lceil", '⌈', 8968), ("rceil", '⌉', 8969), ("lfloor", '⌊', 8970),
    ("rfloor", '⌋', 8971), ("lang", '〈', 9001), ("rang", '〉', 9002), ("loz", '◊', 9674), ("spades", '♠', 9824),
    ("clubs", '♣', 9827), ("hearts", '♥', 9829), ("diams", '♦', 9830))
  .map{ case (name, char, unicode) => Entity(name, char, unicode) }
  .toSeq

  val safe = Seq(39).map(c => entities.find(_.unicode == c)).flatten

}