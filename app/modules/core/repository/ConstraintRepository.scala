/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2020-2021 Karl Kegel
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
import modules.core.model.Constraint
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB Interface for Constraints.
 * Provided methods are UNSAFE and must only be used by service classes!
 * <p> Note: some Constraint operations are sensitive to their actual usage/environment. So for specific add and delete
 * operations, a custom implementation or extension of the ConstraintRepository is needed!
 *
 * @param dbConfigProvider injected database config
 * @param executionContext future execution context
 */
class ConstraintRepository @Inject()(@NamedDatabase("flimey_data") protected val dbConfigProvider: DatabaseConfigProvider)(
  implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  val constraints = TableQuery[ConstraintTable]
  val properties = TableQuery[PropertyTable]
  val entityTypes = TableQuery[TypeTable]

  /**
   * Add a new Constraint to the db.
   * <p> The id must be set to 0 to enable auto increment.
   * <p> <strong> This method must only be used for Constraints that are NOT of HasProperty type. Otherwise this method
   * will lead to the destruction of the database! - the please use Entity specific methods instead</strong>
   *
   * @param constraint new Constraint entity
   * @return Future[Long]
   */
  def addConstraint(constraint: Constraint): Future[Long] = {
    db.run((constraints returning constraints.map(_.id)) += constraint)
  }

  /**
   * Delete an Constraint.
   * <p> <strong> If the Constraint is of a type which requires further actions (for example HasProperty), this method
   * will lead to the destruction of the database! - then please use Entity specific methods instead</strong>
   *
   * @param id of the Constraint
   * @return Future[Int]
   */
  def deleteConstraint(id: Long): Future[Int] = {
    db.run(constraints.filter(_.id === id).delete)
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
   * @param typeId      id of the Type
   * @param derivesFrom optional parent type specification
   * @return future of Constraints of the given Type
   */
  def getAssociated(typeId: Long, derivesFrom: Option[String] = None): Future[Seq[Constraint]] = {
    if(derivesFrom.isEmpty) {
      db.run(constraints.filter(_.typeId === typeId).sortBy(_.id.asc).result)
    }else{
      db.run((for {
        (c, s) <- constraints.filter(_.typeId === typeId).sortBy(_.id.asc) join entityTypes.filter(_.typeOf === derivesFrom.get)
      } yield (c, s)).result) map (res => res.map(_._1).sortBy(_.id))
    }
  }

}