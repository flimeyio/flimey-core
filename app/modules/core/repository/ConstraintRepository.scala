/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2020  Karl Kegel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * */

package modules.core.repository

import com.google.inject.Inject
import modules.asset.repository.AssetTable
import modules.core.model.{Constraint, Property}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB Interface for AssetConstraints.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected database config
 * @param executionContext future execution context
 */
class ConstraintRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val constraints = TableQuery[ConstraintTable]
  val properties = TableQuery[PropertyTable]
  val assets = TableQuery[AssetTable]

  /**
   * Add a new AssetConstraint to the db.
   * <p> The id must be set to 0 to enable auto increment.
   * <p> <strong> This method must only be used for Constraints that are NOT of HasProperty type. Otherwise this method
   * will lead to the destruction of the database! - the please use [[addPropertyConstraint]] instead</strong>
   *
   * @param constraint new Constraint entity
   * @return Future[Long]
   */
  def addNonPropertyConstraint(constraint: Constraint): Future[Long] = {
    db.run((constraints returning constraints.map(_.id)) += constraint)
  }

  /**
   * Add a new 'HasProperty' AssetConstraint to the db.
   * <p> The id must be set to 0 to enable auto increment.
   * <p> <strong> This method must only be used for Constraints that are of HasProperty type. Otherwise this method
   * will lead to the destruction of the database!</strong>
   *
   * @param constraint new <strong>HasProperty</strong> AssetConstraint
   * @return Future[Unit]
   */
    //FIXME move to asset repository
  def addPropertyConstraint(constraint: Constraint): Future[Unit] = {
    db.run((for {
      s <- assets.filter(_.typeId === constraint.typeId).map(_.entityId).result
      _ <- (constraints returning constraints.map(_.id)) += constraint
      _ <- properties ++= s.map(entityId => Property(0, constraint.v1, "", entityId))
    } yield ()).transactionally)
  }

  /**
   * Delete an Constraint.
   * <p> <strong> If the AssetConstraint is of HasProperty type, this method will lead to the destruction
   * of the database! - then please use [[deletePropertyConstraint]] instead</strong>
   *
   * @param id of the Constraint
   * @return Future[Int]
   */
  def deleteNonPropertyConstraint(id: Long): Future[Int] = {
    db.run(constraints.filter(_.id === id).delete)
  }

  /**
   * Delete a Constraint of the HasProperty type.
   * <p> <strong> If the Constraint is not of HasProperty type, this method will lead to the destruction
   * of the database! </strong>
   *
   * @param constraint the <strong>HasProperty</strong> AssetConstraint to delete
   * @return Future[Unit]
   */
    //FIXME move to asset repository
  def deletePropertyConstraint(constraint: Constraint): Future[Unit] = {
    val assetsWithPropertyQuery = assets.filter(_.typeId === constraint.typeId).map(_.entityId)
    db.run((for {
      _ <- properties.filter(_.parentId in assetsWithPropertyQuery).filter(_.key === constraint.v1).delete
      _ <- constraints.filter(_.id === constraint.id).delete
    } yield ()).transactionally)
  }

  /**
   * Fetch a Constraint or None if id not existent.
   *
   * @param id of the Constraint
   * @return future Constraint or None
   */
  def get(id: Long): Future[Option[Constraint]] = {
    db.run(constraints.filter(_.id === id).result.headOption)
  }

  /**
   * Get the sequence of all Constraints associated to a particular Type.
   *
   * @param typeId id of the Type
   * @return future of Constraints of the given Type
   */
  def getAssociated(typeId: Long): Future[Seq[Constraint]] = {
    db.run(constraints.filter(_.typeId === typeId).sortBy(_.id).result)
  }

}