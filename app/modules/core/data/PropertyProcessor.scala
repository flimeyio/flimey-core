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

import modules.core.model.{Constraint, ConstraintType, Property, PropertyType}
import modules.util.data.StringProcessor
import modules.util.messages.{ERR, OK, Status}

/**
 * Trait which provides functionality for parsing, processing and validation Properties
 */
trait PropertyProcessor extends StringProcessor {

  /**
   * Extract all values from a constraint model which serve as Property keys.
   * Each key is associated with its expected datatype.
   *
   * @param constraints model
   * @return Property keys with their datatype [(key, type)]
   */
  def getPropertyKeys(constraints: Seq[Constraint]): Seq[(String, String)] = {
    constraints.filter(_.c == ConstraintType.HasProperty).map(c => (c.v1, c.v2))
  }

  /**
   * Extract all values from a constraint model which serve as Property keys and MUST be defined (not empty).
   * Each key is mapped to its default value (or 'hint').
   *
   * @param constraints model
   * @return obligatory Property keys with default value 'key -> default'
   */
  def getObligatoryPropertyKeys(constraints: Seq[Constraint]): Map[String, String] = {
    constraints.filter(_.c == ConstraintType.MustBeDefined).map(c => (c.v1, c.v2)).toMap
  }

  /**
   * Derives a Property configuration from raw data and a Constraint model.<br />
   * It is expected, that the propData entries are in the same order as the received keys from
   * getAssetPropertyKeys() on the same model.<br />
   * <br />
   * The returned Properties have the parentId set to 0, because this id is only known, after the parent
   * Asset is successfully inserted to the db.
   *
   * @param constraints model
   * @param propData raw configuration data
   * @return valid Property configuration
   */
  def derivePropertiesFromRawData(constraints: Seq[Constraint], propData: Seq[String]): Seq[Property] = {
    val propKeys = getPropertyKeys(constraints)
    if(propKeys.length != propData.length) return Seq()
    var res = Seq[Property]()
    for(i <- propKeys.indices){
      res = res :+ Property(0, propKeys(i)._1, propData(i), 0)
    }
    res
  }

  /**
   * Checks if a Property value is of the given type.<br />
   * Does not check on empty values.
   *
   * @param value to check
   * @param typeName to match
   * @return result
   */
  def isOfType(value: String, typeName: String): Boolean = {
    typeName match {
      case PropertyType.StringType.name => true
      case PropertyType.NumericType.name => isNumericString(value)
      case _ => false
    }

  }

  /**
   * Maps the values of a new configuration candidate to an existing configuration.<br />
   * The configurations must have the same length and order.<br />
   * On error, an empty configuration will be returned (because this will surely fail the model check)
   *
   * @param oldConfiguration existing configuration
   * @param newConfiguration new raw configuration candidate
   * @return new configuration values mapped to existing properties
   */
  def mapConfigurations(oldConfiguration: Seq[Property], newConfiguration: Seq[String]): Seq[Property] = {
    if(oldConfiguration.length != newConfiguration.length) return Seq()
    oldConfiguration.zip(newConfiguration).map(c => {
      val (property, newValue) = c
      Property(property.id, property.key, newValue, property.parentId)
    })
  }

  /**
   * Validates, if a given sequence of raw property data is a correct model configuration.<br />
   * The passed property values must be in the same order as their corresponding keys from getAssetPropertyKeys().
   *
   * @param constraints model of the parent AssetType
   * @param rawProps raw configuration Properties
   * @return Status with optional error message
   */
  def isModelConfiguration(constraints: Seq[Constraint], rawProps: Seq[Property]): Status = {
    val propKeys = getPropertyKeys(constraints)
    val obligatoryKeys = getObligatoryPropertyKeys(constraints)

    for(i <- propKeys.indices){

      val (key, typeName) = propKeys(i)
      val property = rawProps.find(_.key == key)

      //check if the key has a property (basically true by architecture) just a defensive check
      if(property.isEmpty) return ERR("Property "+key+" is missing")

      //check if the property type is correct
      if(!isOfType(property.get.value, typeName)) return ERR("Property "+key+" is of a wrong type")

      //check if property has a non blank value, if it must be defined
      if(obligatoryKeys.contains(key) && property.get.value.isBlank) return ERR("Property "+key+" is not defined")
    }

    OK()
  }

}
