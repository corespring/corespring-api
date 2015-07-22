package org.corespring.reporting.services

import org.corespring.models.Standard

import scala.collection.mutable
import play.api.cache.Cache

trait StandardsHelper extends StandardGroup {

  def findAll: Stream[Standard]

  lazy val legacy = findAll.filter(_.legacyItem).toSeq

  lazy val standardSorter: (Standard, Standard) => Boolean = (one, two) => {
    StandardOrdering.compare(one, two) < 0
  }
  lazy val sorter: (String, String) => Boolean = (a, b) => {
    val standards: Seq[Standard] = cachedStandards()
    ((standards.find(_.dotNotation == Some(a)), standards.find(_.dotNotation == Some(b))) match {
      case (Some(one), Some(two)) => standardSorter(one, two)
      case _ => throw new IllegalArgumentException("Could not find standard for dot notation")
    })
  }

  lazy val groupMap = {
    val blacklist = Seq("3.W.9", "7.W.3.c")
    cachedStandards().sortWith(standardSorter)
      .filterNot(standard => blacklist.map(Option(_)).contains(standard.dotNotation))
      .foldLeft(mutable.Map.empty[String, Seq[Standard]]) { (map, standard) =>
        group match {
          case Some(group) => map.get(group) match {
            case Some(standards) => map + (group -> (standards :+ standard))
            case _ => map + (group -> Seq(standard))
          }
          case _ => map
        }
      }
  }

  def cachedStandards(): Seq[Standard] = {
    val cacheKey = "standards_sort"
    Cache.get(cacheKey) match {
      case Some(standardsJson: String) => Json.parse(standardsJson).as[Seq[Standard]]
      case _ => {
        val standards = findAll().toSeq
        Cache.set(cacheKey, Json.toJson(standards).toString)
        standards.toList
      }
    }
  }

}