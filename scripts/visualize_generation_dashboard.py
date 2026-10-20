#!/usr/bin/env python3
"""Interactive Streamlit dashboard for Settlements generation JSON exports.

Usage:
    streamlit run scripts/visualize_generation_dashboard.py

Features:
    - Drag & drop generation JSON files
    - Optional direct file-path loading from the sidebar
    - 2D settlement map with roads, plots, terrain, and anchor
    - 3D terrain height visualization
    - Summary cards and distribution charts
    - Raw/debug data expanders

Dependencies:
    pip install streamlit plotly
"""

from __future__ import annotations

import json
from collections import Counter
from pathlib import Path
from typing import Any

import plotly.graph_objects as go
import streamlit as st


ROAD_STYLES = {
    "MAIN": {"color": "#222222", "width": 4},
    "SECONDARY": {"color": "#666666", "width": 2.6},
    "SIDE": {"color": "#aaaaaa", "width": 1.6},
}

ZONE_COLORS = {
    "CORE": "#d73027",
    "DOWNTOWN": "#fc8d59",
    "MIDTOWN": "#fee08b",
    "OUTER": "#91cf60",
    "SUBURB": "#4575b4",
}

WATER_BIOME_KEYWORDS = (
    "ocean",
    "river",
    "lake",
    "swamp",
    "marsh",
    "mangrove",
    "pond",
)

SHORE_BIOME_KEYWORDS = (
    "beach",
    "shore",
    "coast",
)

LAND_COLORSCALE = [
    [0.0, "#d9c89c"],
    [0.25, "#b6cc7a"],
    [0.5, "#84b55b"],
    [0.75, "#5f8f49"],
    [1.0, "#86623b"],
]

WATER_COLOR = "#4f9ddb"
SHORE_COLOR = "#9fd0f5"
SEA_LEVEL = 63.0


def classify_biome(biome_id: str) -> str:
    biome_key = biome_id.lower()
    if any(keyword in biome_key for keyword in WATER_BIOME_KEYWORDS):
        return "water"
    if any(keyword in biome_key for keyword in SHORE_BIOME_KEYWORDS):
        return "shore"
    return "land"


def extract_terrain_layers(terrain_grid: dict[str, Any]) -> tuple[list[list[float]], list[list[str]], list[list[str]]]:
    height_grid: list[list[float]] = []
    classification_grid: list[list[str]] = []
    biome_grid: list[list[str]] = []

    for row in terrain_grid["samples"]:
        height_row: list[float] = []
        class_row: list[str] = []
        biome_row: list[str] = []
        for sample in row:
            height_row.append(sample["height"])
            class_row.append(classify_biome(sample["biome"]))
            biome_row.append(sample["biome"])
        height_grid.append(height_row)
        classification_grid.append(class_row)
        biome_grid.append(biome_row)

    return height_grid, classification_grid, biome_grid


def build_visualization_data(data: dict[str, Any]) -> dict[str, Any]:
    site_report = data["siteReport"]
    profile = data["profile"]
    history = data.get("history", {})
    layout = data["layout"]
    terrain_grid = site_report["terrainGrid"]
    height_grid, classification_grid, biome_grid = extract_terrain_layers(terrain_grid)
    assignments = layout.get("assignments", [])
    roads = layout.get("roads", [])

    building_counts = Counter(assignment["building"]["id"] for assignment in assignments)
    road_counts = Counter(road["type"] for road in roads)

    return {
        "raw": data,
        "site_report": site_report,
        "profile": profile,
        "history": history,
        "layout": layout,
        "terrain_grid": terrain_grid,
        "height_grid": height_grid,
        "classification_grid": classification_grid,
        "biome_grid": biome_grid,
        "building_counts": building_counts,
        "road_counts": road_counts,
    }


def format_list(values: list[Any]) -> str:
    if not values:
        return "none"
    return ", ".join(normalize_identifier(value) for value in values)


def normalize_identifier(value: Any, default: str = "n/a") -> str:
    if value is None:
        return default
    if isinstance(value, dict):
        full_value = value.get("full")
        if full_value is not None:
            return str(full_value)
    text = str(value)
    return text if text else default


def as_text(value: Any, default: str = "n/a") -> str:
    return normalize_identifier(value, default)


def format_distribution(values: dict[str, Any], limit: int | None = None) -> str:
    if not values:
        return "none"
    items = sorted(values.items(), key=lambda item: item[1], reverse=True)
    if limit is not None:
        items = items[:limit]
    formatted = []
    for name, value in items:
        display_name = normalize_identifier(name)
        if isinstance(value, float):
            formatted.append(f"{display_name}={value:.3f}")
        else:
            formatted.append(f"{display_name}={value}")
    return ", ".join(formatted)


def format_plot_label(label: str, area: float) -> str:
    if area < 20:
        max_length = 6
    elif area < 30:
        max_length = 8
    else:
        max_length = 12

    if len(label) <= max_length:
        return label
    return label[:max_length] + "…"


def get_sample_axes(terrain_grid: dict[str, Any], rows: int, cols: int) -> tuple[list[float], list[float]]:
    interval = terrain_grid["sampleInterval"]
    origin_x = terrain_grid["originX"]
    origin_z = terrain_grid["originZ"]
    xs = [origin_x + col * interval for col in range(cols)]
    zs = [origin_z + row * interval for row in range(rows)]
    return xs, zs


def sample_height_at(visualization: dict[str, Any], x: float, z: float) -> float:
    terrain_grid = visualization["terrain_grid"]
    height_grid = visualization["height_grid"]
    interval = terrain_grid["sampleInterval"]
    origin_x = terrain_grid["originX"]
    origin_z = terrain_grid["originZ"]

    row_count = len(height_grid)
    col_count = len(height_grid[0]) if row_count else 0
    if row_count == 0 or col_count == 0:
        return 0.0

    col = min(max(round((x - origin_x) / interval), 0), col_count - 1)
    row = min(max(round((z - origin_z) / interval), 0), row_count - 1)
    return float(height_grid[row][col])


def building_base_height(visualization: dict[str, Any], assignment: dict[str, Any], exaggeration: float) -> float:
    plot = assignment["plot"]
    bounds = plot["bounds"]
    min_pos = bounds["min"]
    max_pos = bounds["max"]
    center_x = (min_pos["x"] + max_pos["x"]) / 2.0
    center_z = (min_pos["z"] + max_pos["z"]) / 2.0
    target_y = plot.get("targetY")
    if target_y is None:
        target_y = sample_height_at(visualization, center_x, center_z)
    return float(target_y) * exaggeration


def create_map_figure(
    visualization: dict[str, Any],
    *,
    show_terrain: bool,
    show_labels: bool,
    show_build_area: bool,
    show_anchor: bool,
) -> go.Figure:
    site_report = visualization["site_report"]
    layout = visualization["layout"]
    height_grid = visualization["height_grid"]
    classification_grid = visualization["classification_grid"]
    biome_grid = visualization["biome_grid"]
    terrain_grid = visualization["terrain_grid"]
    rows = len(height_grid)
    cols = len(height_grid[0]) if rows else 0
    xs, zs = get_sample_axes(terrain_grid, rows, cols)

    fig = go.Figure()

    if show_terrain and rows and cols:
        terrain_values: list[list[float | None]] = []
        terrain_custom_data: list[list[list[str]]] = []
        for row_index, row in enumerate(height_grid):
            terrain_row: list[float | None] = []
            custom_row: list[list[str]] = []
            for col_index, height in enumerate(row):
                classification = classification_grid[row_index][col_index]
                biome = biome_grid[row_index][col_index]
                if classification == "water":
                    terrain_row.append(None)
                else:
                    terrain_row.append(float(height))
                custom_row.append([classification, biome])
            terrain_values.append(terrain_row)
            terrain_custom_data.append(custom_row)

        fig.add_trace(
            go.Heatmap(
                x=xs,
                y=zs,
                z=terrain_values,
                customdata=terrain_custom_data,
                colorscale=LAND_COLORSCALE,
                hovertemplate=(
                    "x=%{x}<br>z=%{y}<br>height=%{z}<br>"
                    "class=%{customdata[0]}<br>biome=%{customdata[1]}<extra></extra>"
                ),
                showscale=True,
                colorbar={"title": "Height"},
                opacity=0.75,
                zsmooth=False,
            )
        )

        water_xs: list[float] = []
        water_zs: list[float] = []
        water_biomes: list[str] = []
        shore_xs: list[float] = []
        shore_zs: list[float] = []
        shore_biomes: list[str] = []
        for row_index, row in enumerate(classification_grid):
            for col_index, classification in enumerate(row):
                if classification == "water":
                    water_xs.append(xs[col_index])
                    water_zs.append(zs[row_index])
                    water_biomes.append(biome_grid[row_index][col_index])
                elif classification == "shore":
                    shore_xs.append(xs[col_index])
                    shore_zs.append(zs[row_index])
                    shore_biomes.append(biome_grid[row_index][col_index])

        if shore_xs:
            fig.add_trace(
                go.Scatter(
                    x=shore_xs,
                    y=shore_zs,
                    customdata=shore_biomes,
                    mode="markers",
                    name="Shore cells",
                    marker={"symbol": "square", "size": 8, "color": SHORE_COLOR, "opacity": 0.55},
                    hovertemplate="x=%{x}<br>z=%{y}<br>biome=%{customdata}<extra>shore</extra>",
                )
            )
        if water_xs:
            fig.add_trace(
                go.Scatter(
                    x=water_xs,
                    y=water_zs,
                    customdata=water_biomes,
                    mode="markers",
                    name="Water cells",
                    marker={"symbol": "square", "size": 8, "color": WATER_COLOR, "opacity": 0.8},
                    hovertemplate="x=%{x}<br>z=%{y}<br>biome=%{customdata}<extra>water</extra>",
                )
            )

    for road_type, style in ROAD_STYLES.items():
        type_roads = [road for road in layout.get("roads", []) if road.get("type") == road_type]
        if not type_roads:
            continue
        for index, road in enumerate(type_roads):
            fig.add_trace(
                go.Scatter(
                    x=[road["start"]["x"], road["end"]["x"]],
                    y=[road["start"]["z"], road["end"]["z"]],
                    mode="lines",
                    name=f"Road: {road_type}",
                    legendgroup=f"road-{road_type}",
                    showlegend=index == 0,
                    line={"color": style["color"], "width": style["width"]},
                    hovertemplate=(
                        f"type={road_type}<br>"
                        f"start=({road['start']['x']}, {road['start']['z']})<br>"
                        f"end=({road['end']['x']}, {road['end']['z']})"
                        "<extra></extra>"
                    ),
                )
            )

    for zone, color in ZONE_COLORS.items():
        zone_assignments = [assignment for assignment in layout.get("assignments", []) if assignment["plot"].get("zone") == zone]
        if not zone_assignments:
            continue
        for index, assignment in enumerate(zone_assignments):
            bounds = assignment["plot"]["bounds"]
            min_pos = bounds["min"]
            max_pos = bounds["max"]
            building_id = assignment["building"]["id"]
            area = (max_pos["x"] - min_pos["x"]) * (max_pos["z"] - min_pos["z"])
            label = format_plot_label(building_id.split(":", 1)[-1], area)
            xs_poly = [min_pos["x"], max_pos["x"], max_pos["x"], min_pos["x"], min_pos["x"]]
            zs_poly = [min_pos["z"], min_pos["z"], max_pos["z"], max_pos["z"], min_pos["z"]]

            hover_text = (
                f"building={building_id}<br>"
                f"zone={zone}<br>"
                f"area={area:.1f}<br>"
                f"min=({min_pos['x']}, {min_pos['z']})<br>"
                f"max=({max_pos['x']}, {max_pos['z']})"
            )

            fig.add_trace(
                go.Scatter(
                    x=xs_poly,
                    y=zs_poly,
                    mode="lines",
                    fill="toself",
                    name=f"Zone: {zone}",
                    legendgroup=f"zone-{zone}",
                    showlegend=index == 0,
                    line={"color": "rgba(0,0,0,0.85)", "width": 1},
                    fillcolor=color,
                    opacity=0.72,
                    hovertemplate=hover_text + "<extra></extra>",
                )
            )

            if show_labels:
                center_x = (min_pos["x"] + max_pos["x"]) / 2.0
                center_z = (min_pos["z"] + max_pos["z"]) / 2.0
                fig.add_annotation(
                    x=center_x,
                    y=center_z,
                    text=label,
                    showarrow=False,
                    font={"size": 10, "color": "black"},
                    bgcolor="rgba(255,255,255,0.7)",
                    borderpad=2,
                )

    if show_build_area:
        build_area = site_report["bounds"]["buildArea"]
        min_pos = build_area["min"]
        max_pos = build_area["max"]
        fig.add_shape(
            type="rect",
            x0=min_pos["x"],
            y0=min_pos["z"],
            x1=max_pos["x"],
            y1=max_pos["z"],
            line={"color": "black", "width": 2, "dash": "dash"},
            fillcolor="rgba(0,0,0,0)",
        )

    if show_anchor:
        anchor = layout["anchor"]
        fig.add_trace(
            go.Scatter(
                x=[anchor["x"]],
                y=[anchor["z"]],
                mode="markers",
                name="Anchor",
                marker={"symbol": "star", "size": 16, "color": "magenta", "line": {"color": "black", "width": 1}},
                hovertemplate=(
                    f"anchor=({anchor['x']}, {anchor['z']})<br>y={anchor.get('y', 'n/a')}<extra></extra>"
                ),
            )
        )

    sample_area = site_report["bounds"]["sampleArea"]
    min_pos = sample_area["min"]
    max_pos = sample_area["max"]
    fig.update_layout(
        title="Settlement Map",
        template="plotly_white",
        height=760,
        legend={"orientation": "h", "yanchor": "bottom", "y": 1.02, "x": 0},
        margin={"l": 10, "r": 10, "t": 60, "b": 10},
        xaxis={"title": "X", "range": [min_pos["x"] - 2, max_pos["x"] + 2], "scaleanchor": "y"},
        yaxis={"title": "Z", "range": [max_pos["z"] + 2, min_pos["z"] - 2]},
    )
    return fig


def create_terrain_3d_figure(
    visualization: dict[str, Any],
    *,
    overlay_roads: bool,
    overlay_anchor: bool,
    overlay_buildings: bool,
    overlay_water: bool,
) -> go.Figure:
    terrain_grid = visualization["terrain_grid"]
    height_grid = visualization["height_grid"]
    classification_grid = visualization["classification_grid"]
    biome_grid = visualization["biome_grid"]
    rows = len(height_grid)
    cols = len(height_grid[0]) if rows else 0
    xs, zs = get_sample_axes(terrain_grid, rows, cols)

    z_surface: list[list[float]] = []
    custom_data: list[list[list[str]]] = []
    for row_index, row in enumerate(height_grid):
        z_row: list[float] = []
        custom_row: list[list[str]] = []
        for col_index, height in enumerate(row):
            classification = classification_grid[row_index][col_index]
            biome = biome_grid[row_index][col_index]
            adjusted = float(height)
            if classification == "water":
                adjusted -= 1.2
            elif classification == "shore":
                adjusted -= 0.2
            z_row.append(adjusted)
            custom_row.append([classification, biome])
        z_surface.append(z_row)
        custom_data.append(custom_row)

    fig = go.Figure()
    fig.add_trace(
        go.Surface(
            x=xs,
            y=zs,
            z=z_surface,
            customdata=custom_data,
            colorscale=LAND_COLORSCALE,
            showscale=True,
            colorbar={"title": "Height"},
            contours={"z": {"show": True, "usecolormap": True, "project_z": True}},
            hovertemplate=(
                "x=%{x}<br>z=%{y}<br>height=%{z:.2f}<br>"
                "class=%{customdata[0]}<br>biome=%{customdata[1]}<extra></extra>"
            ),
        )
    )

    if overlay_water and rows and cols:
        water_plane = [[SEA_LEVEL for _ in range(cols)] for _ in range(rows)]
        water_custom = [["sea level water overlay" for _ in range(cols)] for _ in range(rows)]
        fig.add_trace(
            go.Surface(
                x=xs,
                y=zs,
                z=water_plane,
                customdata=water_custom,
                colorscale=[[0.0, WATER_COLOR], [1.0, WATER_COLOR]],
                showscale=False,
                opacity=0.4,
                hovertemplate=(
                    "x=%{x}<br>z=%{y}<br>water level=%{z}<br>"
                    "overlay=%{customdata}<extra></extra>"
                ),
            )
        )

    if overlay_roads:
        for road in visualization["layout"].get("roads", []):
            style = ROAD_STYLES.get(road["type"], ROAD_STYLES["SIDE"])
            start_height = sample_height_at(visualization, road["start"]["x"], road["start"]["z"]) + 0.2
            end_height = sample_height_at(visualization, road["end"]["x"], road["end"]["z"]) + 0.2
            fig.add_trace(
                go.Scatter3d(
                    x=[road["start"]["x"], road["end"]["x"]],
                    y=[road["start"]["z"], road["end"]["z"]],
                    z=[start_height, end_height],
                    mode="lines",
                    showlegend=False,
                    line={"color": style["color"], "width": style["width"] + 1},
                    hovertemplate=f"road={road['type']}<extra></extra>",
                )
            )

    if overlay_buildings:
        for assignment in visualization["layout"].get("assignments", []):
            plot = assignment["plot"]
            bounds = plot["bounds"]
            min_pos = bounds["min"]
            max_pos = bounds["max"]
            zone = plot.get("zone", "UNKNOWN")
            color = ZONE_COLORS.get(zone, "#cccccc")
            base_height = building_base_height(visualization, assignment, 1.0) + 0.25

            width = max_pos["x"] - min_pos["x"]
            depth = max_pos["z"] - min_pos["z"]
            footprint = max(width * depth, 1.0)
            height_boost = max(0.2, min(0.6, 0.015 * footprint))
            roof_height = base_height + height_boost

            x0 = min_pos["x"]
            x1 = max_pos["x"]
            z0 = min_pos["z"]
            z1 = max_pos["z"]
            building_id = assignment["building"]["id"]

            fig.add_trace(
                go.Mesh3d(
                    x=[x0, x1, x1, x0, x0, x1, x1, x0],
                    y=[z0, z0, z1, z1, z0, z0, z1, z1],
                    z=[base_height, base_height, base_height, base_height, roof_height, roof_height, roof_height, roof_height],
                    i=[0, 0, 4, 4, 0, 1, 2, 3, 0, 0, 1, 2],
                    j=[1, 2, 5, 6, 1, 2, 3, 0, 4, 3, 5, 6],
                    k=[2, 3, 6, 7, 5, 6, 7, 4, 5, 4, 6, 7],
                    color=color,
                    opacity=0.55,
                    flatshading=True,
                    showscale=False,
                    showlegend=False,
                    hovertemplate=(
                        f"building={building_id}<br>"
                        f"zone={zone}<br>"
                        f"base={base_height:.2f}<br>"
                        f"roof={roof_height:.2f}<extra></extra>"
                    ),
                )
            )

            edge_x = [x0, x1, x1, x0, x0, None, x0, x1, x1, x0, x0, None, x0, x0, None, x1, x1, None, x1, x1, None, x0, x0]
            edge_y = [z0, z0, z1, z1, z0, None, z0, z0, z1, z1, z0, None, z0, z0, None, z0, z0, None, z1, z1, None, z1, z1]
            edge_z = [base_height, base_height, base_height, base_height, base_height, None,
                      roof_height, roof_height, roof_height, roof_height, roof_height, None,
                      base_height, roof_height, None, base_height, roof_height, None,
                      base_height, roof_height, None, base_height, roof_height]
            fig.add_trace(
                go.Scatter3d(
                    x=edge_x,
                    y=edge_y,
                    z=edge_z,
                    mode="lines",
                    showlegend=False,
                    line={"color": "rgba(0,0,0,0.7)", "width": 3},
                    hoverinfo="skip",
                )
            )

    if overlay_anchor:
        anchor = visualization["layout"]["anchor"]
        fig.add_trace(
            go.Scatter3d(
                x=[anchor["x"]],
                y=[anchor["z"]],
                z=[float(anchor.get("y", sample_height_at(visualization, anchor["x"], anchor["z"]))) + 1.0],
                mode="markers",
                name="Anchor",
                marker={"size": 6, "color": "magenta", "symbol": "diamond"},
                hovertemplate=(
                    f"anchor=({anchor['x']}, {anchor['z']})<br>y={anchor.get('y', 'n/a')}<extra></extra>"
                ),
            )
        )

    fig.update_layout(
        title="Terrain 3D",
        template="plotly_white",
        height=760,
        margin={"l": 10, "r": 10, "t": 60, "b": 10},
        scene={
            "xaxis_title": "X",
            "yaxis_title": "Z",
            "zaxis_title": "Height",
            "zaxis": {"range": [30, 100]},
            "camera": {"eye": {"x": 1.4, "y": -1.6, "z": 0.95}},
        },
    )
    return fig


def create_bar_chart(counter: Counter[str] | dict[str, Any], title: str, color: str) -> go.Figure:
    if not counter:
        fig = go.Figure()
        fig.update_layout(title=title, template="plotly_white", height=360)
        return fig

    if isinstance(counter, Counter):
        items = counter.most_common()
    else:
        items = sorted(counter.items(), key=lambda item: item[1], reverse=True)

    labels = [normalize_identifier(item[0]) for item in items]
    values = [item[1] for item in items]

    fig = go.Figure(
        data=[
            go.Bar(
                x=values,
                y=labels,
                orientation="h",
                marker={"color": color},
                hovertemplate="%{y}: %{x}<extra></extra>",
            )
        ]
    )
    fig.update_layout(
        title=title,
        template="plotly_white",
        height=max(320, 40 * len(labels) + 120),
        margin={"l": 10, "r": 10, "t": 60, "b": 10},
        yaxis={"autorange": "reversed"},
    )
    return fig


def load_visualization_from_source(uploaded_file, file_path: str) -> tuple[dict[str, Any] | None, str | None]:
    if uploaded_file is not None:
        data = json.load(uploaded_file)
        return build_visualization_data(data), uploaded_file.name

    if file_path.strip():
        path = Path(file_path.strip())
        with path.open("r", encoding="utf-8") as handle:
            data = json.load(handle)
        return build_visualization_data(data), str(path)

    return None, None


def render_summary(visualization: dict[str, Any], source_name: str) -> None:
    site_report = visualization["site_report"]
    profile = visualization["profile"]
    layout = visualization["layout"]

    st.title("Settlements Generation Dashboard")
    st.caption(f"Source: {source_name}")

    row_1 = st.columns(4)
    row_1[0].metric("Generation Seed", visualization["raw"].get("generationSeed", "n/a"))
    row_1[1].metric("Scale Tier", as_text(profile.get("scaleTier", "n/a")))
    row_1[2].metric("Population", profile.get("estimatedPopulation", "n/a"))
    row_1[3].metric("Assignments", len(layout.get("assignments", [])))

    row_2 = st.columns(4)
    row_2[0].metric("Wealth", f"{profile.get('wealthLevel', 0.0):.3f}")
    row_2[1].metric("Defense", as_text(profile.get("defenseLevel", "n/a")))
    row_2[2].metric("Roads", len(layout.get("roads", [])))
    row_2[3].metric("Water Features", len(site_report.get("waterFeatureTypes", [])))

    row_3 = st.columns(1)
    row_3[0].metric("Resources", len(site_report.get("resourceTags", [])))

    st.markdown(f"**Primary trait:** {as_text(profile.get('primary', 'n/a'))}")

    badge_col_1, badge_col_2 = st.columns([1.25, 1.75])
    with badge_col_1:
        st.markdown(f"**Secondary traits:** {format_list(profile.get('secondary', []))}")
        st.markdown(f"**Flavor traits:** {format_list(profile.get('flavor', []))}")
    with badge_col_2:
        st.markdown(f"**Water features:** {format_list(site_report.get('waterFeatureTypes', []))}")
        st.markdown(f"**Resource tags:** {format_list(site_report.get('resourceTags', []))}")


def render_stats_tab(visualization: dict[str, Any]) -> None:
    site_report = visualization["site_report"]
    profile = visualization["profile"]
    history = visualization.get("history", {})
    road_counts = visualization["road_counts"]
    building_counts = visualization["building_counts"]

    chart_row_1 = st.columns(2)
    with chart_row_1[0]:
        st.plotly_chart(create_bar_chart(building_counts, "Building Frequency", "#4575b4"), width="stretch")
    with chart_row_1[1]:
        st.plotly_chart(create_bar_chart(road_counts, "Road Types", "#666666"), width="stretch")

    chart_row_2 = st.columns(2)
    with chart_row_2[0]:
        st.plotly_chart(
            create_bar_chart(site_report.get("biomeDistribution", {}), "Biome Distribution", "#84b55b"),
            width="stretch",
        )
    with chart_row_2[1]:
        st.plotly_chart(
            create_bar_chart(profile.get("adjustedWeights", {}), "Adjusted Weights", "#fc8d59"),
            width="stretch",
        )

    st.plotly_chart(
        create_bar_chart(
            history.get("modifiedWeights", profile.get("adjustedWeights", {})),
            "History-Modified Weights",
            "#d73027",
        ),
        width="stretch",
    )

    with st.expander("Text summary", expanded=False):
        elevation = site_report.get("elevation", {})
        high_point = elevation.get("highPoint", {})
        st.markdown(
            "\n".join(
                [
                    f"- **Profile seed:** {profile.get('seed', 'n/a')}",
                    f"- **Biome distribution:** {format_distribution(site_report.get('biomeDistribution', {}), limit=6)}",
                    f"- **Adjusted weights:** {format_distribution(profile.get('adjustedWeights', {}), limit=6)}",
                    f"- **History events:** {format_list([event.get('eventId') for event in history.get('events', [])])}",
                    f"- **Visual markers:** {format_list(history.get('visualMarkers', []))}",
                    f"- **Elevation min/max/mean:** {elevation.get('min', 'n/a')} / {elevation.get('max', 'n/a')} / {elevation.get('mean', 'n/a')}",
                    f"- **High point:** ({high_point.get('x', 'n/a')}, {high_point.get('z', 'n/a')}) @ y={high_point.get('y', 'n/a')}",
                ]
            )
        )


def render_raw_data_tab(visualization: dict[str, Any]) -> None:
    with st.expander("Profile", expanded=True):
        st.json(visualization["profile"])
    with st.expander("Site report"):
        st.json(visualization["site_report"])
    with st.expander("History"):
        st.json(visualization.get("history", {}))
    with st.expander("Layout"):
        st.json(visualization["layout"])
    with st.expander("Full generation payload"):
        st.json(visualization["raw"])


def main() -> None:
    st.set_page_config(page_title="Settlements Generation Dashboard", layout="wide")

    st.sidebar.header("Load generation JSON")
    uploaded_file = st.sidebar.file_uploader("Drag & drop a generation JSON", type=["json"])
    file_path = st.sidebar.text_input("...or load from a file path", value="")

    st.sidebar.header("Display options")
    show_terrain = st.sidebar.checkbox("Show terrain in 2D map", value=True)
    show_labels = st.sidebar.checkbox("Show building labels", value=True)
    show_build_area = st.sidebar.checkbox("Show build area", value=True)
    show_anchor = st.sidebar.checkbox("Show anchor", value=True)

    st.sidebar.header("3D terrain options")
    overlay_roads = st.sidebar.checkbox("Overlay roads in 3D", value=True)
    overlay_buildings = st.sidebar.checkbox("Overlay buildings in 3D", value=True)
    overlay_water = st.sidebar.checkbox("Show sea level water overlay", value=True)
    overlay_anchor = st.sidebar.checkbox("Overlay anchor in 3D", value=True)

    try:
        visualization, source_name = load_visualization_from_source(uploaded_file, file_path)
    except FileNotFoundError as exc:
        st.error(f"File not found: {exc}")
        return
    except json.JSONDecodeError as exc:
        st.error(f"Invalid JSON: {exc}")
        return
    except KeyError as exc:
        st.error(f"Unexpected generation JSON structure. Missing key: {exc}")
        return

    if visualization is None or source_name is None:
        st.title("Settlements Generation Dashboard")
        st.info("Upload a generation JSON file in the sidebar, or enter a local file path to inspect it.")
        st.markdown(
            """
            This dashboard is intended for lightweight generation debugging and includes:
            - a **2D settlement map**
            - a **3D terrain height view**
            - summary cards and counts
            - raw generation payload inspection
            """
        )
        return

    render_summary(visualization, source_name)

    tab_map, tab_terrain, tab_stats, tab_raw = st.tabs(["Map", "Terrain 3D", "Stats", "Raw Data"])

    with tab_map:
        st.plotly_chart(
            create_map_figure(
                visualization,
                show_terrain=show_terrain,
                show_labels=show_labels,
                show_build_area=show_build_area,
                show_anchor=show_anchor,
            ),
            width="stretch",
        )

    with tab_terrain:
        st.plotly_chart(
            create_terrain_3d_figure(
                visualization,
                overlay_roads=overlay_roads,
                overlay_buildings=overlay_buildings,
                overlay_water=overlay_water,
                overlay_anchor=overlay_anchor,
            ),
            width="stretch",
        )

    with tab_stats:
        render_stats_tab(visualization)

    with tab_raw:
        render_raw_data_tab(visualization)


if __name__ == "__main__":
    main()