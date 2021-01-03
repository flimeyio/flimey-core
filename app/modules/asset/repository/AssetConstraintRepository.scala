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

package modules.asset.repository

import modules.asset.model.{AssetConstraint, AssetProperty}
import com.google.inject.Inject
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
class AssetConstraintRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val assetConstraints = TableQuery[AssetConstraintTable]
  val assetProperties = TableQuery[AssetPropertyTable]
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
  def addNonPropertyConstraint(constraint: AssetConstraint): Future[Long] = {
    db.run((assetConstraints returning assetConstraints.map(_.id)) += constraint)
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
  def addPropertyConstraint(constraint: AssetConstraint): Future[Unit] = {
    db.run((for {
      s <- assets.filter(_.typeId === constraint.typeId).map(_.id).result
      _ <- (assetConstraints returning assetConstraints.map(_.id)) += constraint
      _ <- assetProperties ++= s.map(assetId => AssetProperty(0, constraint.v1, "", assetId))
    } yield ()).transactionally)
  }

  /**
   * Delete an AssetConstraint.
   * <p> <strong> If the AssetConstraint is of HasProperty type, this method will lead to the destruction
   * of the database! - then please use [[deletePropertyConstraint]] instead</strong>
   *
   * @param id of the Constraint
   * @return Future[Int]
   */
  def deleteNonPropertyConstraint(id: Long): Future[Int] = {
    db.run(assetConstraints.filter(_.id === id).delete)
  }

  /**
   * Delete a AssetConstraint of the HasProperty type.
   * <p> <strong> If the AssetConstraint is not of HasProperty type, this method will lead to the destruction
   * of the database! </strong>
   *
   * @param constraint the <strong>HasProperty</strong> AssetConstraint to delete
   * @return Future[Unit]
   */
  def deletePropertyConstraint(constraint: AssetConstraint): Future[Unit] = {
    val assetsWithPropertyQuery = assets.filter(_.typeId === constraint.typeId).map(_.id)
    db.run((for {
      _ <- assetProperties.filter(_.parentId in assetsWithPropertyQuery).filter(_.key === constraint.v1).delete
      _ <- assetConstraints.filter(_.id === constraint.id).delete
    } yield ()).transactionally)
  }

  /**
   * Fetch a AssetConstraint or None if id not existent.
   *
   * @param id of the Constraint
   * @return future Constraint or None
   */
  def get(id: Long): Future[Option[AssetConstraint]] = {
    db.run(assetConstraints.filter(_.id === id).result.headOption)
  }

  /**
   * Get the sequence of all Constraints associated to a particular AssetType.
   *
   * @param assetTypeId id of the AssetType
   * @return future of Constraints of the given AssetType
   */
  def getAssociated(assetTypeId: Long): Future[Seq[AssetConstraint]] = {
    db.run(assetConstraints.filter(_.typeId === assetTypeId).sortBy(_.id).result)
  }

}