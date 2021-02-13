package modules.core.service

import modules.auth.model.Ticket
import modules.core.model.{Constraint, EntityType, ExtendedEntityType, VersionedEntityType}

import scala.concurrent.Future

trait ModelEntityService {

  abstract def getAllTypes()(implicit ticket: Ticket): Future[Seq[EntityType]]

  abstract def getAllVersions()(implicit ticket: Ticket): Future[Seq[VersionedEntityType]]

  /**
   * Get a EntityType of the specific subtype by its ID.
   * <p> Must fail without WORKER rights.
   * <p> This is must be a safe implementation and can be used by controller classes.
   *
   * @param id     id of the EntityType
   * @param ticket implicit authentication ticket
   * @return Future Option[EntityType]
   */
  abstract def getType(id: Long)(implicit ticket: Ticket): Future[Option[EntityType]]

  abstract def addVersion(typeId: Long)(implicit ticket: Ticket): Future[Long]

  abstract def deleteVersion(typeVersionId: Long)(implicit ticket: Ticket): Future[Unit]

  abstract def getVersionedType(typeVersionId: Long)(implicit ticket: Ticket): Future[Option[VersionedEntityType]]

  /**
   * Get an ExtendedEntityType  of the specific subtype.
   * <p> Must fail without WORKER rights.
   * <p> This must be a safe implementation and can be used by controller classes.
   *
   * @param typeVersionId od the EntityTypes TypeVersion
   * @param ticket        implicit authentication ticket
   * @return Future (EntityType, Seq[Constraint])
   */
  abstract def getExtendedType(typeVersionId: Long)(implicit ticket: Ticket): Future[ExtendedEntityType]

  abstract def getLatestExtendedType(typeId: Long)(implicit ticket: Ticket): Future[ExtendedEntityType]

  /**
   * Get an EntityType of the specific subtype by its value (name) field.
   * <p> Must fail without WORKER rights.
   * <p> This must be a safe implementation and can be used by controller classes.
   *
   * @param value  value filed (name) of the searched EntityType
   * @param ticket implicit authentication ticket
   * @return Future Option[CollectionType]
   */
  abstract def getTypeByValue(value: String)(implicit ticket: Ticket): Future[Option[EntityType]]

  /**
   * Update an already existing EntityType of the specific subtype. This includes 'value' (name) and 'active'.
   * <p> To change the 'active' property to true, the Constraint model must be valid!
   * <p> Must fail without MODELER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     of the Type to update
   * @param ticket implicit authentication ticket
   * @return Future[Int]
   */
  abstract def updateType(id: Long, value: String, active: Boolean)(implicit ticket: Ticket): Future[Int]

  /**
   * Get all Constraints associated to an EntityType of the specific subtype.
   * <p> Must fail without WORKER rights.
   * <p> This must be a safe implementation and can be used by controller classes.
   *
   * @param typeVersionId of the EntityTypes typeVersion
   * @param ticket         implicit authentication ticket
   * @return Future Seq[Constraint]
   */
  abstract def getConstraintsOfType(typeVersionId: Long)(implicit ticket: Ticket): Future[Seq[Constraint]]

  /**
   * Delete a Constraint by its ID.
   * <p> By deleting a Constraint, the associated EntityType model must stay valid.
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
  abstract def deleteConstraint(id: Long)(implicit ticket: Ticket): Future[Unit]

  /**
   * Add a Constraint to a EntityType of a specific subtype.
   * <p> ID must be 0 (else the future will fail). If the addition of the Constraint will invalidate the model,
   * the future will fail.
   * <p> Must fail without MODELER rights.
   * <p> This must be a safe implementation and can be used by controller classes.
   *
   * @param c      String value of the ConstraintType
   * @param v1     first Constraint parameter
   * @param v2     second Constraint parameter
   * @param typeId id of the parent EntityType
   * @param ticket implicit authentication ticket
   * @return Future[Long]
   */
  abstract def addConstraint(c: String, v1: String, v2: String, typeId: Long)(implicit ticket: Ticket): Future[Unit]

  /**
   * Delete an EntityType of a specific subtype.
   * <p> <strong> This operation will also delete all associated Constraints and all subtype instances which have this type! </strong>
   * <p> Must fail without MODELER rights
   * <p> This must be a safe implementation and can be used by controller classes.
   *
   * @param id     of the EntityType
   * @param ticket implicit authentication ticket
   * @return Future[Unit]
   */
  abstract def deleteType(id: Long)(implicit ticket: Ticket): Future[Unit]

}
