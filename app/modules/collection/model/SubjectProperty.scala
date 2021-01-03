/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2021 Karl Kegel
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

package modules.collection.model

/**
 * A Property of a [[Subject]].
 * Represents an attribute which is objectified and associated to a single parent (Subject).
 * The Property may also be part of a Subject plugin which is defined by the [[SubjectType]].
 * <p> Has a repository representation.
 *
 * @param id           unique identifier
 * @param parentId     id of the parent Subject
 * @param key          property key
 * @param value        property value
 * @param partOfPlugin plugin, which the property is part of or none if not
 */
case class SubjectProperty(id: Long, parentId: Long, key: String, value: String, partOfPlugin: Option[SubjectPluginType.Type])
