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

package modules.subject.model

/**
 * Collectible is a subtype of Subject. It represents a single process step and can not contain other Subjects.
 * In other words, a Collectible is a leaf of the Subject tree.
 * <p> Has a repository representation.
 *
 * @param id unique identifier
 * @param subjectId id of the parent Subject
 * @param collectionId id of the Collection which contains the Collectible
 * @param typeId id of the parent EntityType
 */
case class Collectible(id: Long, subjectId: Long, collectionId: Long, typeId: Long)
