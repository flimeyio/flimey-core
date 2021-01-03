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

package modules.core.data

import modules.core.model.{Constraint, ConstraintType}
import modules.util.messages.{ERR, OK, Status}

/**
 * Trait which provides functionality for parsing and processing constraints
 */
trait ConstraintProcessor {

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
  def isValidConstraint(constraint: Constraint): Status

  /**
   * Checks every MustBeDefined rule has a corresponding HasProperty rule
   *
   * @param constraints model
   * @return result
   */
  def hasMatchingProperties(constraints: Seq[Constraint]): Boolean = {
    val mdConstraints = constraints.filter(c => c.c == ConstraintType.MustBeDefined)
    val validMDs = mdConstraints.filter(c => constraints.exists(p => p.c == ConstraintType.HasProperty && p.v1 == c.v1))
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

  /**
   * Checks a given AssetType constraint model for semantic correctness.
   * It is checked that:
   * 1) exactly one DerivesFrom rule exists
   * 2) every MustBeDefined rule has a corresponding HasProperty rule
   * 3) no rules are duplicates
   *
   * @param constraints model to check
   * @return Status with optional error message
   */
  def isAssetConstraintModel(constraints: Seq[Constraint]): Status = {
    //exactly one derives rule
    val derivationCount = constraints.count(c => c.c == ConstraintType.DerivesFrom)
    if(derivationCount < 1){
      return ERR("Asset Type must have a 'Derives From' constraint")
    } else if (derivationCount > 1) {
      return ERR("Asset Type must have only one 'Derives From' constraint")
    }
    //must constrains only on existing properties
    if(!hasMatchingProperties(constraints)){
      return ERR("Every 'Must Be Defined' constraint needs a corresponding 'Has Property' constraint")
    }
    //no duplicates
    if(!hasNoDuplicates(constraints)){
      return ERR("Constraints must not have duplicates")
    }
    //everything ok
    OK()
  }

}
