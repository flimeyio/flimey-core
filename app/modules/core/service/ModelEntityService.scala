package modules.core.service

import modules.auth.model.Ticket
import modules.core.model.{Constraint, EntityType, ExtendedEntityType, VersionedEntityType}

import scala.concurrent.Future

trait ModelEntityService {

  /**
   * Get all [[modules.core.model.EntityType EntityTypes]].
   * <p> Does not consider [[modules.core.model.TypeVersion TypeVersions]] and only return the pure EntityTypes.
   *
   * @param ticket implicit authentication ticket.
   * @return Future Seq[EntityType]
   */
  def getAllTypes()(implicit ticket: Ticket): Future[Seq[EntityType]]

  /**
   * Get all [[modules.core.model.EntityType EntityTypes]] together with their [[modules.core.model.TypeVersion TypeVersions]] as
   * [[modules.core.model.VersionedEntityType VersionedEntityTypes]].
   * <p> If an EntityType has multiple TypeVersions, every combination will be part of the result.
   *
   * @param ticket implicit authentication ticket
   * @return Future Seq[VersionedEntityType]
   */
  def getAllVersions()(implicit ticket: Ticket): Future[Seq[VersionedEntityType]]

  /**
   * Get a [[modules.core.model.EntityType EntityType]] of the specific subtype by its ID.
   * <p> Must fail without WORKER rights.
   * <p> This is must be a safe implementation and can be used by controller classes.
   *
   * @param id     id of the EntityType
   * @param ticket implicit authentication ticket
   * @return Future Option[EntityType]
   */
  def getType(id: Long)(implicit ticket: Ticket): Future[Option[EntityType]]

  /**
   * Add a new [[modules.core.model.TypeVersion TypeVersion]] to a specified [[modules.core.model.EntityType EntityType]].
   * <p> The new TypeVersion will receive the highest version number.
   *
   * @param typeId of the parent EntityType
   * @param ticket implicit authentication ticket
   * @return Future[Long]
   */
  def addVersion(typeId: Long)(implicit ticket: Ticket): Future[Long]

  /**
   * Delete an existing [[modules.core.model.TypeVersion TypeVersion]].
   * <p> <strong> This will also delete all entities which use this TypeVersion! </strong>
   *
   * @param typeVersionId of the TypeVersion to delete
   * @param ticket        implicit authentication ticket
   * @return Future[Unit]
   */
  def deleteVersion(typeVersionId: Long)(implicit ticket: Ticket): Future[Unit]

  /**
   * Creates a new [[modules.core.model.TypeVersion TypeVersion]] by copying an existing one.
   * <p> The new TypeVersion will receive the highest version number no matter which version was forked.
   *
   * @param typeVersionId of the TypeVersion to fork
   * @param ticket        implicit authentication ticket
   * @return Future[Unit]
   */
  def forkVersion(typeVersionId: Long)(implicit ticket: Ticket): Future[Unit]

  /**
   * Get a [[modules.core.model.VersionedEntityType VersionedEntityType]] of the given id.
   * <p> If the given id is invalid, the returned option will be empty.
   *
   * @param typeVersionId of the [[modules.core.model.TypeVersion TypeVersion]] to fetch
   * @param ticket        implicit authentication ticket
   * @return Future Option[VersionedEntityType]
   */
  def getVersionedType(typeVersionId: Long)(implicit ticket: Ticket): Future[Option[VersionedEntityType]]

  /**
   * Get an [[modules.core.model.ExtendedEntityType ExtendedEntityType]] of the specific [[modules.core.model.TypeVersion TypeVersion]].
   * <p> Must fail without WORKER rights.
   * <p> This must be a safe implementation and can be used by controller classes.
   *
   * @param typeVersionId of the [[modules.core.model.EntityType EntityTypes]] TypeVersion
   * @param ticket        implicit authentication ticket
   * @return Future[ExtendedEntityType]
   */
  def getExtendedType(typeVersionId: Long)(implicit ticket: Ticket): Future[ExtendedEntityType]

  /**
   * Get the [[modules.core.model.TypeVersion TypeVersion]] of a specified [[modules.core.model.EntityType EntityType]]
   * with the highest version number (the newest version).
   * <p> If an EntityType has no TypeVersions, the future will fail.
   *
   * @param typeId id of the parent EntityType
   * @param ticket implicit authentication ticket
   * @return Future[ExtendedEntityType]
   */
  def getLatestExtendedType(typeId: Long)(implicit ticket: Ticket): Future[ExtendedEntityType]

  /**
   * Get an [[modules.core.model.EntityType EntityType]] of the specific subtype by its value (name) field.
   * <p> Must fail without WORKER rights.
   * <p> This must be a safe implementation and can be used by controller classes.
   *
   * @param value  value filed (name) of the searched EntityType
   * @param ticket implicit authentication ticket
   * @return Future Option[CollectionType]
   */
  def getTypeByValue(value: String)(implicit ticket: Ticket): Future[Option[EntityType]]

  /**
   * Update an already existing [[modules.core.model.EntityType EntityType]] of the specific subtype.
   * This includes 'value' (name) and 'active'.
   * <p> To change the 'active' property to true, the [[modules.core.model.Constraint Constraint]] model must be valid!
   * <p> Must fail without MODELER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     of the Type to update
   * @param ticket implicit authentication ticket
   * @return Future[Int]
   */
  def updateType(id: Long, value: String, active: Boolean)(implicit ticket: Ticket): Future[Int]

  /**
   * Get all [[modules.core.model.Constraint Constraints]] associated to a [[modules.core.model.TypeVersion TypeVersion]]
   * of the specific subtype.
   * <p> Must fail without WORKER rights.
   * <p> This must be a safe implementation and can be used by controller classes.
   *
   * @param typeVersionId of the parent TypeVersion
   * @param ticket        implicit authentication ticket
   * @return Future Seq[Constraint]
   */
  def getConstraintsOfType(typeVersionId: Long)(implicit ticket: Ticket): Future[Seq[Constraint]]

  /**
   * Delete a [[modules.core.model.Constraint Constraint]] by its ID.
   * <p> By deleting a Constraint, the associated [[modules.core.model.TypeVersion TypeVersion]] model must stay valid.
   * If the removal of the Constraint will invalidate the model, the future will fail.
   * <p> <strong>The removal of a 'HasProperty' and similar Constraint leads to the system wide removal of all corresponding
   * data properties of the specific subtype instances!</strong>
   * <p> Must fail without MODELER rights.
   * <p> This must be a safe implementation and can be used by controller classes.
   *
   * @param id     of the Constraint to delete
   * @param ticket implicit authentication ticket
   * @return Future[Int]
   */
  def deleteConstraint(id: Long)(implicit ticket: Ticket): Future[Unit]

  /**
   * Add a [[modules.core.model.Constraint Constraint]] to a [[modules.core.model.TypeVersion TypeVersion]] of a specific subtype.
   * <p> ID must be 0 (else the future will fail). If the addition of the Constraint will invalidate the model,
   * the future will fail.
   * <p> Must fail without MODELER rights.
   * <p> This must be a safe implementation and can be used by controller classes.
   *
   * @param c             String value of the ConstraintType
   * @param v1            first Constraint parameter
   * @param v2            second Constraint parameter
   * @param typeVersionId id of the parent TypeVersion
   * @param ticket        implicit authentication ticket
   * @return Future[Long]
   */
  def addConstraint(c: String, v1: String, v2: String, typeVersionId: Long)(implicit ticket: Ticket): Future[Unit]

  /**
   * Delete an [[modules.core.model.EntityType EntityType]] of a specific subtype.
   * <p> This operation deletes all [[modules.core.model.TypeVersion TypeVersions]] of this type.
   * <p> <strong> This operation will also delete all associated [[modules.core.model.Constraint Constraints]] and all
   * subtype instances which have this type! </strong>
   * <p> Must fail without MODELER rights
   * <p> This must be a safe implementation and can be used by controller classes.
   *
   * @param id     of the EntityType
   * @param ticket implicit authentication ticket
   * @return Future[Unit]
   */
  def deleteType(id: Long)(implicit ticket: Ticket): Future[Unit]

}
