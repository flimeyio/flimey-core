/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2021 Tom-Maurice Schreiber
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

String.prototype.toDate = function(format)
{
    const normalized      = this.replace(/[^a-zA-Z0-9]/g, '-');
    const normalizedFormat= format.toLowerCase().replace(/[^a-zA-Z0-9]/g, '-');
    const formatItems     = normalizedFormat.split('-');
    const dateItems       = normalized.split('-');

    const monthIndex  = formatItems.indexOf("mm");
    const dayIndex    = formatItems.indexOf("dd");
    const yearIndex   = formatItems.indexOf("yyyy");

    const today = new Date();

    const year  = yearIndex>-1  ? dateItems[yearIndex]    : today.getFullYear();
    const month = monthIndex>-1 ? dateItems[monthIndex]-1 : today.getMonth()-1;
    const day   = dayIndex>-1   ? dateItems[dayIndex]     : today.getDate();

    return new Date(year,month,day,0,0,0);
};