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

package modules.core.util

import modules.core.model.{Constraint, ConstraintType, PluginType}
import modules.util.messages.{ERR, OK, Status}

/**
 * Trait which provides functionality for parsing and processing constraints
 */
trait ConstraintProcessor {

  /**
   * Checks if a given constraint is a syntactically correct HasProperty constraint.
   * No semantic analysis is performed!
   *
   * @param v1      first constraint parameter
   * @param v2      second constraint parameter
   * @param options seq of possible (data)types
   * @return validation status with optional error message
   */
  def isHasPropertyConstraint(v1: String, v2: String, options: Seq[String]): Status = {
    if (v1.length > 0 && options.contains(v2)) return OK()
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
    if (v1.length > 0 && v2.length > 0) return OK()
    ERR("Invalid 'Must Be Defined' Configuration")
  }

  /**
   * Checks if a given constraint is a syntactically correct CanContain constraint.
   * No semantic analysis is performed!
   *
   * @param v1 first constraint parameter
   * @param v2 second constraint parameter
   * @return validation status with optional error message
   */
  def isCanContainConstraint(v1: String, v2: String): Status = {
    if (v1.length > 0 && v2.length == 0) return OK()
    ERR("Invalid 'Can Contain' Configuration")
  }

  /**
   * Checks if a given constraint is a syntactically correct UsesPlugin constraint.
   * No semantic analysis is performed!
   *
   * @param v1 first constraint parameter
   * @param v2 second constraint parameter
   * @return validation status with optional error message
   */
  def isUsesPluginConstraint(v1: String, v2: String): Status = {
    if (PluginType.find(v1).isDefined && v2.length == 0) return OK()
    ERR("Invalid 'Uses Plugin' Configuration")
  }

  /**
   * Checks if a given Constraint is a syntactically correct Constraint of an EntityTypes specific subtype.
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
   * Checks a given EntityType constraint model for semantic correctness.
   * It is checked that:
   * <p> 1) every MustBeDefined rule has a corresponding HasProperty rule
   * <p> 2) no rules are duplicates
   *
   * <p> This method may be overwritten by entity specif processors.
   *
   * @param constraints model to check
   * @return Status with optional error message
   */
  def isConstraintModel(constraints: Seq[Constraint]): Status = {
    //must constrains only on existing properties
    if (!hasMatchingProperties(constraints)) {
      return ERR("Every 'Must Be Defined' constraint needs a corresponding 'Has Property' constraint")
    }
    //no duplicates
    if (!hasNoDuplicates(constraints)) {
      return ERR("Constraints must not have duplicates")
    }
    //everything ok
    OK()
  }

}
