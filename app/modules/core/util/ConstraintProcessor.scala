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

import modules.core.model._
import modules.util.messages.{ERR, OK, Status}

/**
 * Trait which provides functionality for parsing and processing [[modules.core.model.Constraint Constraint]]s
 */
trait ConstraintProcessor {

  /**
   * Checks if a given [[modules.core.model.Constraint Constraint]] is a syntactically correct HasProperty constraint.
   * No semantic analysis is performed!
   *
   * @param v1      first constraint parameter
   * @param v2      second constraint parameter
   * @param options seq of possible (data)types
   * @return validation status with optional error message
   */
  protected def isHasPropertyConstraint(v1: String, v2: String, options: Seq[String]): Status = {
    if (v1.length > 0 && options.contains(v2)) return OK()
    ERR("Invalid 'Has Property' Configuration")
  }

  /**
   * Checks if a given [[modules.core.model.Constraint Constraint]] is a syntactically correct MustBeDefined constraint.
   * No semantic analysis is performed!
   *
   * @param v1 first constraint parameter
   * @param v2 second constraint parameter
   * @return validation status with optional error message
   */
  protected def isMustBeDefinedConstraint(v1: String, v2: String): Status = {
    if (v1.length > 0 && v2.length > 0) return OK()
    ERR("Invalid 'Must Be Defined' Configuration")
  }

  /**
   * Checks if a given [[modules.core.model.Constraint Constraint]] is a syntactically correct CanContain constraint.
   * No semantic analysis is performed!
   *
   * @param v1 first constraint parameter
   * @param v2 second constraint parameter
   * @return validation status with optional error message
   */
  protected def isCanContainConstraint(v1: String, v2: String): Status = {
    if (v1.length > 0 && v2.length == 0) return OK()
    ERR("Invalid 'Can Contain' Configuration")
  }

  /**
   * Checks if a given [[modules.core.model.Constraint Constraint]] is a syntactically correct UsesPlugin constraint.
   * No semantic analysis is performed!
   *
   * @param v1 first constraint parameter
   * @param v2 second constraint parameter
   * @return validation status with optional error message
   */
  protected def isUsesPluginConstraint(v1: String, v2: String): Status = {
    if (PluginType.find(v1).isDefined && v2.length == 0) return OK()
    ERR("Invalid 'Uses Plugin' Configuration")
  }

  /**
   * Checks if a given [[modules.core.model.Constraint Constraint]] is a syntactically correct Constraint of an
   * [[modules.core.model.EntityType EntityType]] specific subtype. No semantic analysis is done!
   *
   * @param constraint to check
   * @return Status with optional error message
   */
  def isValidConstraint(constraint: Constraint): Status

  /**
   * Checks every MustBeDefined [[modules.core.model.Constraint Constraint]] has a corresponding HasProperty rule
   *
   * @param constraints model
   * @return result
   */
  protected def hasMatchingProperties(constraints: Seq[Constraint]): Boolean = {
    val mdConstraints = constraints.filter(c => c.c == ConstraintType.MustBeDefined)
    val validMDs = mdConstraints.filter(c => constraints.exists(p => p.c == ConstraintType.HasProperty && p.v1 == c.v1))
    validMDs.size == mdConstraints.size
  }

  /**
   * Checks that no [[modules.core.model.Constraint Constraint]]s are duplicates
   *
   * @param constraints model
   * @return result
   */
  protected def hasNoDuplicates(constraints: Seq[Constraint]): Boolean = {
    constraints.count(c => constraints.exists(sym => sym.id != c.id && sym.c == c.c && sym.v1 == c.v1)) == 0
  }

  /**
   * Checks if a [[modules.core.model.Constraint Constraint]] model contains all HasProperty Constraints with correct keys
   * and PropertyTypes to fulfill the requirements of all defined UsesPlugin Constraints.
   *
   * @param constraints complete Constraint model
   * @return Boolean - false as soon as one missing HasProperty is found
   */
  protected def hasCompletePlugins(constraints: Seq[Constraint]): Boolean = {
    constraints.filter(_.c == ConstraintType.UsesPlugin).map(pluginConstraint => {
      val pluginType = PluginType.withName(pluginConstraint.v1)
      val requiredProperties = PluginSpec.getSpecFromType(pluginType)
      requiredProperties.map(propertySpec => {
        val (key: String, propertyType: PropertyType.Value) = propertySpec
        constraints.exists(c => c.c == ConstraintType.HasProperty && c.v1 == key && c.v2 == propertyType.toString)
      }).reduceOption((a, b) => a & b).getOrElse(true)
    }).reduceOption((a, b) => a & b).getOrElse(true)
  }

  /**
   * Checks a given [[modules.core.model.EntityType EntityType]] [[modules.core.model.Constraint Constraint]]
   * model for semantic correctness. It is checked that:
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

  /**
   * Build the sequence of helper [[modules.core.model.Constraint]]s that are needed to fulfill the Plugin definition.
   *
   * @see [[modules.core.util.ConstraintProcessor#applyConstraint]]
   * @param pluginType type of the new Plugin
   * @return Seq[Constraint] without the UsesPlugin Constraint
   */
  protected def deriveConstraintsFromPlugin(pluginType: PluginType.Type): Seq[Constraint] = {
    PluginSpec.getSpecFromType(pluginType).map(propertySpec => {
      val (key: String, propertyType: PropertyType.Value) = propertySpec
      Constraint(0, ConstraintType.HasProperty, key, propertyType.name, Option(pluginType), 0)
    }).toSeq
  }

  /**
   * Get the [[modules.core.model.Constraint]]s that are needed to fulfill the definition of a new Constraint to add.
   * <p> For example if a UsesPlugin Constraint is added, there may be multiple HasProperty Constraints needed to add
   * the properties the Plugin defines. This method creates those Constraints.
   * <p> If a Constraint does not need other Constraints, for example a MustBeDefined Constraint, only the Constraint
   * itself is returned.
   *
   * @param newConstraint new Constraint to add to a model
   * @return Seq[Constraint] of all needed derived Constraints, contains at least newConstraint.
   */
  def applyConstraint(newConstraint: Constraint): Seq[Constraint] = {
    if (newConstraint.c == ConstraintType.UsesPlugin) {
      val pluginType = PluginType.withName(newConstraint.v1)
      Seq(newConstraint) ++ deriveConstraintsFromPlugin(pluginType)
    } else {
      Seq(newConstraint)
    }
  }

  /**
   * Get the [[modules.core.model.Constraint]]s to be also removed if a Constraint is removed from a model.
   * <p> If a Constraint stands alone, like a MustBeDefined Constraint, only the Constraint itself is returned.
   * <p> If a Constraint is a UsesPlugin Constraint, all other Constraints of the model that have the same byPlugin
   * definition are also returned and must be deleted.
   * <p> In this method, only dependencies of UsesPlugin Constraints are resolved. Other dependencies can be for example
   * HasProperty - MustBeDefined must be resolved manually.
   *
   * @param removedConstraint the Constraint which is deleted (dependency root)
   * @param constraints       all Constraints of the [[modules.core.model.EntityType]]
   * @return Seq[Constraint] that must be deleted. Contains at least removedConstraint.
   */
  def removeConstraint(removedConstraint: Constraint, constraints: Seq[Constraint]): Seq[Constraint] = {
    if (removedConstraint.c == ConstraintType.UsesPlugin) {
      val pluginType = PluginType.withName(removedConstraint.v1)
      val pluginAssociatedConstraints = constraints.filter(c => c.byPlugin.isDefined && c.byPlugin.get == pluginType)
      Seq(removedConstraint) ++ pluginAssociatedConstraints
    } else {
      Seq(removedConstraint)
    }
  }

}
