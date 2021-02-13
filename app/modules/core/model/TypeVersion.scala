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

package modules.core.model

/**
 * The TypeVersion data class represents a version of a modeled [[modules.core.model.EntityType]].
 * The associated Constraints are not held by this class but hold a reference to this class on their own.
 * <p> Has a repository representation.
 *
 * @param id      unique primary key (given by db interface)
 * @param typeId  id of the parent of the EntityType
 * @param version positive version number
 */
case class TypeVersion(id: Long, typeId: Long, version: Long)
