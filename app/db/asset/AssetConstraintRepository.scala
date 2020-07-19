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

package db.asset

import com.google.inject.Inject
import model.Constraint
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB Interface for (Asset)Constraints.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected database config
 * @param executionContext future execution context
 */
class AssetConstraintRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val assetConstraints = TableQuery[AssetConstraintTable]

  /**
   * Add a new (Asset)Constraint to the db.
   * The id must be set to 0 to enable auto increment.
   *
   * @param constraint new Constraint entity
   * @return new id future
   */
  def add(constraint: Constraint): Future[Long] = {
    db.run((assetConstraints returning assetConstraints.map(_.id)) += constraint)
  }

  /**
   * Delete a (Asset)Constraint.
   *
   * @param id of the Constraint
   * @return result future
   */
  def delete(id: Long): Future[Int] = {
    db.run(assetConstraints.filter(_.id === id).delete)
  }

  /**
   * Fetch a (Asset)Constraint or None if id not existent.
   *
   * @param id of the Constraint
   * @return future Constraint or None
   */
  def get(id: Long): Future[Option[Constraint]] = {
    db.run(assetConstraints.filter(_.id === id).result.headOption)
  }

  /**
   * Get the sequence of all Constraints associated to a particular AssetType.
   *
   * @param assetTypeId id of the AssetType
   * @return future of Constraints of the given AssetType
   */
  def getAssociated(assetTypeId: Long): Future[Seq[Constraint]] = {
    db.run(assetConstraints.filter(_.typeId === assetTypeId).sortBy(_.id).result)
  }

  /**
   * Get the sequence of all existing (Asset)Constraints
   *
   * @return sequence of all (Asset)Constraints
   */
  def getAll: Future[Seq[Constraint]] = {
    db.run(assetConstraints.result)
  }

}