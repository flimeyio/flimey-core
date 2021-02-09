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

console.log("Hello flimey!")

let scroll_open = true;

document.addEventListener('scroll', function (event) {
    let pos = $("body").scrollTop()
    if(pos > 10 && scroll_open){
        scroll_open = false;
        $( ".main-content" ).addClass( "main-content-scrolled" );
        $( ".navbar-global" ).addClass( "navbar-global-scrolled" );
        $( ".navbar-layout" ).addClass( "navbar-layout-scrolled" );
        console.log("set closed");
    }else if(pos < 10 && !scroll_open){
        scroll_open = true
        $( ".main-content" ).removeClass( "main-content-scrolled" );
        $( ".navbar-global" ).removeClass( "navbar-global-scrolled" );
        $( ".navbar-layout" ).removeClass( "navbar-layout-scrolled" );
        console.log("set open");
    }
}, true);