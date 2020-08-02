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

package services.asset

import model.asset.AssetConstraintHelper
import model.asset.AssetConstraintHelper.ConstraintType
import model.generic.Constraint
import util.{ERR, OK, Status}

/**
 * Trait which provides functionality for parsing and processing constraints
 */
trait ConstraintProcessor {

  /**
   * Normalizes the possible input values of a CONSTRAINT
   *
   * @param constraint general Constraint
   * @return normalized Constraint
   */
  def preprocessConstraint(constraint: Constraint): Constraint = {
    val v1 = constraint.v1 //val v1 = toLowerCaseNoSpaces(constraint.v1)
    val v2 = constraint.v2 //val v2 = toLowerCaseNoSpaces(constraint.v2)
    val cType = ConstraintType.values find (t => t.name == constraint.c || t.short == constraint.c)
    val c = if(cType.isDefined) cType.get.short else "UNDEFINED"
    Constraint(constraint.id, c, v1, v2, constraint.typeId)
  }

  /**
   * Checks if a given constraint is a syntactically correct DerivesFrom constraint.
   * No semantic analysis is performed!
   *
   * @param v1 first constraint parameter
   * @param v2 second constraint parameter
   * @param options seq of possible supertypes
   * @return validation status with optional error message
   */
  def isDerivesFromConstraint(v1: String, v2: String, options: Seq[String]): Status = {
    if(options.contains(v1) && v2.length == 0) return OK()
    ERR("Invalid 'Derives From' Configuration")
  }

  /**
   * Checks if a given constraint is a syntactically correct HasProperty constraint.
   * No semantic analysis is performed!
   *
   * @param v1 first constraint parameter
   * @param v2 second constraint parameter
   * @param options seq of possible (data)types
   * @return validation status with optional error message
   */
  def isHasPropertyConstraint(v1: String, v2: String, options: Seq[String]): Status = {
    if(v1.length > 0 && options.contains(v2)) return OK()
    ERR("Invalid 'Has Property' Configuration")
  }

  /**
   * Checks if a given constraint is a syntactically correct MustBeDefined constraint.
   * No semantic analysis is performed!
   *
   * @param v1 first constraint parameter
   * @param v2 second constraint parameter
   * @return validation status with optional error message
   */
  def isMustBeDefinedConstraint(v1: String, v2: String): Status = {
    if(v1.length > 0 && v2.length > 0) return OK()
    ERR("Invalid 'Must Be Defined' Configuration")
  }

  /**
   * Checks if a given Constraint is a syntactically correct Constraint of an AssetType.
   * No semantic analysis is done!
   *
   * @param constraint to check
   * @return Status with optional error message
   */
  def isValidConstraint(constraint: Constraint): Status = {
    constraint.c match {
      case ConstraintType.DerivesFrom.short => isDerivesFromConstraint(constraint.v1, constraint.v2, AssetConstraintHelper.canDeriveFrom)
      case ConstraintType.HasProperty.short => isHasPropertyConstraint(constraint.v1, constraint.v2, AssetConstraintHelper.hasPropertyTypes)
      case ConstraintType.MustBeDefined.short => isMustBeDefinedConstraint(constraint.v1, constraint.v2)
      case _ => ERR("Invalid Asset Constraint Rule")
    }
  }

  /**
   * Checks every MustBeDefined rule has a corresponding HasProperty rule
   *
   * @param constraints model
   * @return result
   */
  def hasMatchingProperties(constraints: Seq[Constraint]): Boolean = {
    val mdConstraints = constraints.filter(c => c.c == ConstraintType.MustBeDefined.short)
    val validMDs = mdConstraints.filter(c => constraints.exists(p => p.c == ConstraintType.HasProperty.short && p.v1 == c.v1))
    validMDs.size == mdConstraints.size
  }

  /**
   * Checks no rules are duplicates
   *
   * @param constraints model
   * @return result
   */
  def hasNoDuplicates(constraints: Seq[Constraint]): Boolean = {
    constraints.count(c => constraints.exists(sym => sym.id != c.id && sym.c == c.c && sym.v1 == c.v1)) == 0
  }

}
