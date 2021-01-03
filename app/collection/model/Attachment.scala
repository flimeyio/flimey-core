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

package collection.model

/**
 * Attachment represent the connection between a Subject and an Asset.
 * This connection is basically bidirectional but is always meant from Subject to Asset.
 * <p> Has a repository representation.
 *
 * @param id unique identifier
 * @param subjectId id of the Subject
 * @param assetId id of the Asset
 */
case class Attachment(id: Long, subjectId: Long, assetId: Long)
