package org.corespring.services

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ CollectionInfo, ContentCollRef, ContentCollection, Organization }
import org.corespring.services.errors.PlatformServiceError

import scalaz.Validation

trait OrgCollectionService {
  //-- access ---
  /**
   * does the given organization have access to the given collection with given permission.
   * Aka - can org 'a' 'write' to collection 'c'?
   * @param orgId
   * @param collId
   */
  def isAuthorized(orgId: ObjectId, collId: ObjectId, p: Permission): Validation[PlatformServiceError, Unit]

  /**
   * does the given organization have access to all the given collections with given permissions?
   * @param orgId
   * @param collIds
   */
  def isAuthorized(orgId: ObjectId, collIds: Seq[ObjectId], p: Permission): Validation[PlatformServiceError, Unit]

  def canAccessCollection(orgId: ObjectId, collectionId: ObjectId, permission: Permission): Boolean

  def canAccessCollection(org: Organization, collectionId: ObjectId, permission: Permission): Boolean
  def getPermissions(orgId: ObjectId, collId: ObjectId): Option[Permission]
  //---access

  def shareCollectionWithOrg(collectionId: ObjectId, orgId: ObjectId, p: Permission): Validation[PlatformServiceError, ContentCollRef]

  def ownsCollection(org: Organization, collectionId: ObjectId): Validation[PlatformServiceError, Unit]

  //ENABLE--
  /** Enable this collection for this org */
  def enableCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef]

  /** Enable the collection for the org */
  def disableCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef]
  /** Enable this collection for this org */
  def enableCollectionForOrg(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef]

  /** Enable the collection for the org */
  def disableCollectionForOrg(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef]
  //ENABLE--

  //def getContentCollRefs(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ContentCollRef]

  def getCollectionIds(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ObjectId]

  def getCollections(orgId: ObjectId, p: Permission): Validation[PlatformServiceError, Seq[ContentCollection]]

  /**
   * List all collections that the given orgId has access to.
   * @param orgId
   * @return a stream of [[org.corespring.models.CollectionInfo]]
   */
  def listAllCollectionsAvailableForOrg(orgId: ObjectId): Stream[CollectionInfo]
  def listCollectionsByOrg(orgId: ObjectId): Stream[ContentCollection]

  def getOrgsWithAccessTo(collectionId: ObjectId): Stream[Organization]

  def getOrCreateDefaultCollection(orgId: ObjectId): Validation[PlatformServiceError, ContentCollection]

  //new private function def orgsWithPath(orgId: ObjectId, deep: Boolean): Seq[Organization]

  //DELETE---
  def removeCollection(orgId: ObjectId, collId: ObjectId): Validation[PlatformServiceError, Unit]
  def deleteCollectionFromAllOrganizations(collId: ObjectId): Validation[String, Unit]
  //DELETE---

  // new private def addCollectionReference(orgId: ObjectId, reference: ContentCollRef): Validation[PlatformServiceError, Unit]
  /**
   * Add the public collection to all orgs to that they have access to it
   * @param collectionId
   * @return
   */
  def addPublicCollectionToAllOrgs(collectionId: ObjectId): Validation[PlatformServiceError, Unit]

  def addCollection(orgId: ObjectId, collId: ObjectId, p: Permission): Validation[PlatformServiceError, ContentCollRef]

}
