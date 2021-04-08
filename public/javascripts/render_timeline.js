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


let margin = { top: 30, right: 0, bottom: 30, left: 0 };

function getTodayDate() {
    let today_date = new Date(2021, 5, 25);
    let dd = String(today_date.getDate()).padStart(2, '0');
    let mm = String(today_date.getMonth() + 1).padStart(2, '0');
    let yyyy = today_date.getFullYear();
    return (mm + '/' + dd + '/' + yyyy + ' 0:00:00');

}

function getTodayStart() {
    let today_start = new Date(getTodayDate());
    return today_start.getTime();
}


const render = (project_collection, data, width, viewportid) => {
    const heightColTimeline = 60;
    const barHeight = 8.5;
    const barMargin = 20;
    const axisTickWidth = 4;

    console.log('data: ', data);
    const svgHeight = (barHeight + barMargin) * data.length;

    // current day as a timespan
    const TODAY_SPAN = 24 * 60 * 60 * 1000;
    let today_start = getTodayStart();
    let today_mid = today_start + (0.5 * TODAY_SPAN);
    let today_end = today_start + TODAY_SPAN;

    // Min and Max Value of the X-Axis
    let min_date = (today_start < project_collection.start) ? today_start : project_collection.start;
    let max_date = project_collection.end;

    // Order of the bars along the Y-Axis
    const yScale = d3.scaleBand()
        .domain(data.map(dataPoint => dataPoint.collectible_name))
        .rangeRound([svgHeight, 0]).padding(0);


    // Scales the timelines (startDate - endDate) to a lower range of values [0, width]
    let xScale = (d3.scaleLinear()
        //.domain([d3.min(data, d => d.start), d3.max(data, d => d.end)])
        .domain([min_date, max_date])
        .range([0, width - margin.left, - margin.right]));

    const isLabelRight = coll => {
        let coll_width = xScale(coll.end) - xScale(coll.start);
        return (xScale(coll.start) > width/2 ? xScale(coll.start) + coll_width : xScale(coll.start) - coll_width > 0);
    }

    // View of the collection and X-Axis
    const collection_timeline = d3.select("#"+viewportid+'-collection-timeline')
        .append("svg")
        .attr('width', width - margin.left - margin.right)
        .attr('height', heightColTimeline + barHeight);

    // View of all collectible items
    const coll_timeline = d3.select("#"+viewportid+'-coll-tile-timeline')
        .append("svg")
        .attr('width', width - margin.left - margin.right)
        .attr('height', svgHeight);


    const bar = coll_timeline.selectAll('rect')
        .data(data)
        .enter().append('g')
        //.attr('class', 'bar');

    // Draw the timeline of all collectibles
    bar.append('rect')
        //.classed('bar', true)
        .attr('fill', '#4fb3bf')
        .attr('margin', '0.3em 0 0.3em 0')
        .attr('height', barHeight)
        .attr('width', d => (width * (d.end - d.start) / (max_date - min_date)))
        .attr('x', d => xScale(d.start))
        .attr('y', d => yScale(d.collectible_name))
        .attr('rx', barHeight / 2)
        .attr('ry', barHeight / 2);

    bar.append('text')
        //.classed('below', true)
        .attr('x', d => xScale(d.start))
        .attr('y', d => yScale(d.collectible_name))
        .attr('dy', barHeight + 10)
        .attr('font-size', '10pt')
        .attr('text-anchor', 'left')
        .text(function(d) {return d.collectible_name})
        .attr('fill', '#555555');

    // Draw X-Axis
    collection_timeline.append('rect')
        .attr('fill', '#555555')
        .attr('width', (width * (max_date - min_date) / (max_date - min_date)))
        .attr('height', 1)
        .attr('x', 0)
        .attr('y', 25);

    // First entry not shown ??
    const axisTicks = [
        1,
        {x : project_collection.start, date : ""},
        {x : (today_mid - (0.5 * axisTickWidth)), date : ""},
        {x : (max_date - 12000000), date : ""}
    ];

    // Draw Axis Ticks
    collection_timeline.selectAll('rect').data(axisTicks)
        .enter().append('rect')
        .classed('tick', true)
        .attr('fill', '#555555')
        .attr('width', 0.1 + 'em')
        .attr('height', 10)
        .attr('x', axisTicks => xScale(axisTicks.x))
        .attr('y', 15);

    // Draw the timeline of the collection
    collection_timeline.append('rect')
        .attr('fill', '#00838f')
        .attr('width', (width * (project_collection.end - project_collection.start) / (max_date - min_date)))
        .attr('height', barHeight)
        .attr('x', xScale(project_collection.start))
        .attr('y', heightColTimeline - barHeight)
        .attr('rx', barHeight / 2)
        .attr('ry', barHeight / 2);

    // Draw the timeline of the actual date
    collection_timeline.append('rect')
        .attr('fill', '#FF8800')
        .attr('width', (xScale(today_end) - xScale(today_start)))
        //.attr('width',  (width * (today_end - today_start) / (max_date - min_date)))
        .attr('height', barHeight)
        .attr('x', xScale(getTodayStart()))
        .attr('y', heightColTimeline - barHeight);

    console.log('Skaliertes: ', xScale(getTodayStart()));
    console.log('Skaliertes: ', xScale(project_collection.start));
};