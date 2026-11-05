## UI Pro Max Search Results
**Domain:** chart | **Query:** accessible mobile medication adherence trend charts monthly history compact healthcare analytics
**Source:** charts.csv | **Found:** 10 results

### Result 1
- **Data Type:** Performance vs Target (Compact)
- **Keywords:** bullet, compact, kpi, dashboard, target, benchmark, range, accessible chart, keyboard accessible chart
- **Best Chart Type:** Bullet Chart
- **Secondary Options:** Gauge, Progress Bar
- **When to Use:** Dashboard with multiple KPIs side by side; space-constrained contexts where a gauge is too large
- **When NOT to Use:** Single KPI with emphasis (use gauge); data has no defined target range; fewer than 3 KPIs
- **Data Volume Threshold:** Ideal for 3ÔÇô10 bullet charts in a grid; scales to any count efficiently
- **Color Guidance:** Qualitative ranges: #FFCDD2 / #FFF9C4 / #C8E6C9 (bad/ok/good). Performance bar: #1976D2. Target: black 3px marker
- **Accessibility Grade:** deprecated: use Accessibility Risk
- **Accessibility Risk:** risk:low
- **Accessibility Notes:** Label every qualitative range and target with text; color is supplementary.
- **A11y Fallback:** Visible KPI/target table plus status summary. Keyboard: focus reveals the same detail as hover.
- **Library Recommendation:** D3.js, Plotly, Custom SVG
- **Interactive Level:** Hover

### Result 2
- **Data Type:** Trend Over Time
- **Keywords:** trend, time-series, line, growth, timeline, progress, accessible chart, keyboard accessible chart
- **Best Chart Type:** Line Chart
- **Secondary Options:** Area Chart, Smooth Area
- **When to Use:** Data has a time axis; user needs to observe rise/fall trends or rate of change over a continuous period
- **When NOT to Use:** Fewer than 4 data points (use stat card); more than 6 series (visual noise); no time dimension exists
- **Data Volume Threshold:** <1000 pts: SVG; ÔëÑ1000 pts: Canvas + downsampling; >10000: aggregate to intervals
- **Color Guidance:** Primary: #0080FF. Multiple series: distinct colors + distinct line styles. Fill: 20% opacity
- **Accessibility Grade:** deprecated: use Accessibility Risk
- **Accessibility Risk:** risk:low
- **Accessibility Notes:** Use solid, dashed, and dotted line styles plus direct series labels; never distinguish series by hue alone.
- **A11y Fallback:** Visible data table plus concise trend summary. Keyboard: focus reveals hover values; +/- buttons zoom; Reset restores the full range.
- **Library Recommendation:** Chart.js, Recharts, ApexCharts
- **Interactive Level:** Hover + Zoom

### Result 3
- **Data Type:** Distribution / Statistical
- **Keywords:** distribution, statistical, spread, median, outlier, quartile, boxplot, accessible chart, keyboard accessible chart
- **Best Chart Type:** Box Plot
- **Secondary Options:** Violin Plot, Beeswarm
- **When to Use:** Showing spread, median, and outliers of a dataset; comparing distributions across multiple groups
- **When NOT to Use:** Fewer than 20 data points per group (distribution is not meaningful); audience unfamiliar with statistical charts
- **Data Volume Threshold:** Any sample size; aggregated representation so rendering is ÔÜí Excellent at any volume
- **Color Guidance:** Box fill: #BBDEFB. Border: #1976D2. Median line: #D32F2F bold. Outlier dots: #F44336
- **Accessibility Grade:** deprecated: use Accessibility Risk
- **Accessibility Risk:** risk:low
- **Accessibility Notes:** Label median, quartiles, whiskers, and outliers directly; do not use color alone for statistical roles.
- **A11y Fallback:** Statistics table plus distribution summary. Keyboard: focus reveals the same statistics as hover.
- **Library Recommendation:** Plotly, D3.js, Chart.js (plugin)
- **Interactive Level:** Hover

### Result 4
- **Data Type:** Multi-Variable Comparison
- **Keywords:** radar, spider, multi-variable, attributes, dimensions, comparison, accessible chart, keyboard accessible chart
- **Best Chart Type:** Radar / Spider Chart
- **Secondary Options:** Parallel Coordinates, Grouped Bar
- **When to Use:** Comparing multiple entities across the same fixed set of attributes (e.g., product feature comparison)
- **When NOT to Use:** Axes > 8 (unreadable); values need precise comparison (use grouped bar); audience unfamiliar with radar charts
- **Data Volume Threshold:** 2ÔÇô3 datasets maximum per chart; 5ÔÇô8 axes; beyond 8 axes switch to parallel coordinates
- **Color Guidance:** Single dataset: #0080FF at 20% fill. Multiple: distinct hues with 30% fill. Border: full opacity
- **Accessibility Grade:** deprecated: use Accessibility Risk
- **Accessibility Risk:** risk:conditional
- **Accessibility Notes:** Use line styles, point shapes, and direct series labels in addition to color. Do not rely on color alone.
- **A11y Fallback:** Raw data table and grouped-bar alternative plus comparison summary. Keyboard: focus reveals values; buttons toggle series.
- **Library Recommendation:** Chart.js, Recharts, ApexCharts
- **Interactive Level:** Hover + Toggle

### Result 5
- **Data Type:** Part-to-Whole
- **Keywords:** part-to-whole, pie, donut, percentage, proportion, share, accessible chart, keyboard accessible chart
- **Best Chart Type:** Pie Chart or Donut
- **Secondary Options:** Stacked Bar, Waffle Chart
- **When to Use:** Ôëñ5 categories; one dominant segment vs rest; emphasis on visual proportion over exact values
- **When NOT to Use:** Categories > 5; slice differences < 5% (visually indistinguishable); user needs precise values; accessibility-first context
- **Data Volume Threshold:** Max 6 slices; beyond that switch to stacked bar 100%
- **Color Guidance:** 5ÔÇô6 max colors. Contrasting palette. Largest slice at 12 o'clock. Always label slices with %
- **Accessibility Grade:** deprecated: use Accessibility Risk
- **Accessibility Risk:** risk:high
- **Accessibility Notes:** Pie charts do not inherently fail WCAG; unlabeled color-only slices are inaccessible. Use direct labels and patterns, with a non-pie fallback. Do not rely on color alone.
- **A11y Fallback:** Percentage data table and concise part-to-whole summary; offer a stacked bar view. Keyboard: focus reveals slice values; Enter/Space drills in; Back button returns.
- **Library Recommendation:** Chart.js, Recharts, D3.js
- **Interactive Level:** Hover + Drill

### Result 6
- **Data Type:** Geographic Data
- **Keywords:** geographic, map, location, region, geo, spatial, choropleth, accessible chart, keyboard accessible chart
- **Best Chart Type:** Choropleth Map or Bubble Map
- **Secondary Options:** Geographic Heat Map
- **When to Use:** Data has a regional/location dimension; spatial distribution is the core insight for the user
- **When NOT to Use:** Regions have very different sizes making visual comparison misleading (use bar); mobile-primary context
- **Data Volume Threshold:** <1000 regions: SVG; ÔëÑ1000: Canvas/WebGL (Deck.gl); global maps: tile-based rendering
- **Color Guidance:** Single color gradient per region group. Categorized colors for discrete types. Legend with clear scale breaks
- **Accessibility Grade:** deprecated: use Accessibility Risk
- **Accessibility Risk:** risk:conditional
- **Accessibility Notes:** Label regions directly and pair fills with boundaries or patterns; location meaning cannot depend on color alone.
- **A11y Fallback:** Sortable region table plus geographic summary. Keyboard: arrow keys or labeled pan buttons move the map; +/- buttons zoom; Enter drills into a focused region; Back returns.
- **Library Recommendation:** D3.js, Mapbox, Leaflet
- **Interactive Level:** Pan + Zoom + Drill

### Result 7
- **Data Type:** Hierarchical Proportional
- **Keywords:** sunburst, hierarchy, nested, proportion, radial, circle, accessible chart, keyboard accessible chart
- **Best Chart Type:** Sunburst Chart
- **Secondary Options:** Treemap, Icicle, Circle Packing
- **When to Use:** Exploring nested proportions where both hierarchy and relative size matter (e.g., org spend breakdown)
- **When NOT to Use:** More than 3 hierarchy levels (outer rings become unreadable); precision matters over overview; mobile
- **Data Volume Threshold:** <100 nodes: SVG; 100ÔÇô500: Canvas; >500: filter to top N before rendering
- **Color Guidance:** Center to outer: darker to lighter hue. Each level 15ÔÇô20% lighter. Contrasting border between sectors
- **Accessibility Grade:** deprecated: use Accessibility Risk
- **Accessibility Risk:** risk:high
- **Accessibility Notes:** Label hierarchy levels and segments and use boundaries/patterns as well as hue; the indented list is primary. Do not rely on color alone.
- **A11y Fallback:** Collapsible indented list/table plus hierarchy summary and breadcrumbs. Keyboard: Enter/Space drills or expands; Back button returns; focus reveals hover detail.
- **Library Recommendation:** D3.js (d3-hierarchy), Recharts, ApexCharts
- **Interactive Level:** Drilldown + Hover

### Result 8
- **Data Type:** Correlation / Distribution
- **Keywords:** correlation, distribution, scatter, relationship, pattern, cluster, accessible chart, keyboard accessible chart
- **Best Chart Type:** Scatter Plot or Bubble Chart
- **Secondary Options:** Heat Map, Matrix
- **When to Use:** Exploring relationship between two continuous variables; identifying clusters or outliers in a dataset
- **When NOT to Use:** Variables are categorical (use grouped bar); fewer than 20 points (patterns aren't meaningful); mobile-primary context
- **Data Volume Threshold:** <500 pts: SVG; 500ÔÇô5000: Canvas at 0.6ÔÇô0.8 opacity; >5000: hexbin or aggregate first
- **Color Guidance:** Color axis: gradient (blue ÔåÆ red). Bubble size: relative to 3rd variable. Opacity: 0.6ÔÇô0.8 to show density
- **Accessibility Grade:** deprecated: use Accessibility Risk
- **Accessibility Risk:** risk:conditional
- **Accessibility Notes:** Combine marker shapes with direct group labels; color may reinforce but must not be the only distinction.
- **A11y Fallback:** Visible data table plus correlation summary. Keyboard: focus reveals point values; labeled start/end range inputs replace brush dragging.
- **Library Recommendation:** D3.js, Plotly, Recharts
- **Interactive Level:** Hover + Brush

### Result 9
- **Data Type:** 3D Spatial Data
- **Keywords:** 3d, spatial, immersive, terrain, molecular, volumetric, point-cloud, accessible chart, keyboard accessible chart
- **Best Chart Type:** 3D Scatter / Surface Plot
- **Secondary Options:** Volumetric Rendering, Point Cloud
- **When to Use:** Scientific/engineering context where Z-axis carries essential info not expressible in 2D
- **When NOT to Use:** 2D projection conveys the same insight; mobile context; accessibility-required environments; standard business dashboards
- **Data Volume Threshold:** WebGL required. Deck.gl: up to 1M points. Three.js: LOD required beyond 50,000 pts
- **Color Guidance:** Depth cues: lighting and shading. Z-axis: color gradient (cool ÔåÆ warm). Transparent overlapping: opacity 0.4
- **Accessibility Grade:** deprecated: use Accessibility Risk
- **Accessibility Risk:** risk:high
- **Accessibility Notes:** Use labels, shapes, and depth-independent cues; color and 3D position cannot be the only carriers of meaning.
- **A11y Fallback:** Mandatory 2D projection, data table, and spatial summary. Keyboard: rotate/pan buttons and +/- zoom controls replace pointer/VR manipulation; Reset returns the camera.
- **Library Recommendation:** Three.js, Deck.gl, Plotly 3D
- **Interactive Level:** Rotate + Zoom + VR

### Result 10
- **Data Type:** Relationship / Connection Data
- **Keywords:** network, graph, nodes, edges, connections, relationships, force, accessible chart, keyboard accessible chart
- **Best Chart Type:** Network Graph
- **Secondary Options:** Hierarchical Tree, Adjacency Matrix
- **When to Use:** Mapping connections between entities; network topology or social graph exploration context
- **When NOT to Use:** Node count > 500 without clustering pre-applied; user needs precise connection counts; mobile context
- **Data Volume Threshold:** Ôëñ100 nodes: SVG; 101ÔÇô500: Canvas; >500: must apply clustering/LOD before rendering
- **Color Guidance:** Node types: categorical colors. Edges: #90A4AE at 60% opacity. Highlight path: #F59E0B
- **Accessibility Grade:** deprecated: use Accessibility Risk
- **Accessibility Risk:** risk:high
- **Accessibility Notes:** Use labeled node types, shapes, and edge styles in addition to color; the adjacency view is the accessible source of truth. Do not rely on color alone.
- **A11y Fallback:** Adjacency list/table and relationship summary; tree view when applicable. Keyboard: focus reveals node details; Enter drills; Move up/down/left/right buttons replace drag.
- **Library Recommendation:** D3.js (d3-force), Vis.js, Cytoscape.js
- **Interactive Level:** Drilldown + Hover + Drag

