/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2021-2021 Karl Kegel
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

package modules.core.model

/**
 * The Constraint meta model class to represent model rules.
 * Constraints are used by the [[EntityType]] here associated by the typeId.
 * <p> Note that the validity of a Constraint is not guaranteed by the scala or sql model but needs always to be checked
 * by the application logic!
 * <p> Has a repository representation.
 *
 * @param id       unique primary key (given by db interface)
 * @param c        constraint rule type
 * @param v1       first rule argument
 * @param v2       second rule argument
 * @param byPlugin optional name of a parent plugin this Constraint is part of
 * @param typeId   id of the associated EntityType
 */
case class Constraint(id: Long, c: ConstraintType.Type, v1: String, v2: String, byPlugin: Option[PluginType.Type], typeId: Long)

object Constraint {

  def applyRaw(id: Long, c: String, v1: String, v2: String, byPlugin: Option[String], typeId: Long): Constraint = {
    if(byPlugin.isDefined) {
      Constraint(id, ConstraintType.withName(c), v1, v2, Option(PluginType.withName(byPlugin.get)), typeId)
    }else{
      Constraint(id, ConstraintType.withName(c), v1, v2, None, typeId)
    }
  }

  def unapplyToRaw(arg: Constraint): Option[(Long, String, String, String, Option[String], Long)] = {
    if(arg.byPlugin.isDefined) {
      Option((arg.id, arg.c.toString, arg.v1, arg.v2, Option(arg.byPlugin.get.toString), arg.typeId))
    }else{
      Option((arg.id, arg.c.toString, arg.v1, arg.v2, None, arg.typeId))
    }
  }

  val tupledRaw: ((Long, String, String, String, Option[String], Long)) => Constraint = (this.applyRaw _).tupled

}
