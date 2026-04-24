#!/usr/bin/env python3
"""Generate a sparse Minecraft structure NBT placeholder file with corner signs.

This script uses ``nbtlib`` to assemble a structure file that Minecraft can load as a
standard gzipped ``.nbt`` structure.

Example usages:
    from scripts.nbt_placeholder_generator import generate_placeholder_structure
    generate_placeholder_structure(width=5, depth=7, height=2, name="Market")

    # Command line usage:
    # python scripts/nbt_placeholder_generator.py 5 7 2 market
"""

from __future__ import annotations

import argparse
from pathlib import Path

import nbtlib
from nbtlib.tag import Compound, Int, List, String


DATA_VERSION = 3955


def _sanitize_file_component(value: str) -> str:
    """Return a Windows-safe filename fragment while preserving readable text."""
    invalid_chars = '<>:"/\\|?*'
    sanitized = "".join("_" if char in invalid_chars else char for char in value).strip()
    return sanitized or "placeholder"


def _get_or_create_palette_index(
    palette: List[Compound],
    palette_indices: dict[tuple[str, tuple[tuple[str, str], ...]], int],
    block_name: str,
    properties: dict[str, str] | None = None,
) -> int:
    """Return a stable palette index for a block state, creating it on first use.

    The palette stores unique block states. Every block entry in ``blocks`` references one
    of these states by integer index through its ``state`` field.
    """
    normalized_properties = tuple(sorted((properties or {}).items()))
    key = (block_name, normalized_properties)

    if key in palette_indices:
        return palette_indices[key]

    state = Compound({"Name": String(block_name)})
    if normalized_properties:
        state["Properties"] = Compound(
            {property_name: String(property_value) for property_name, property_value in normalized_properties}
        )

    palette_index = len(palette)
    palette.append(state)
    palette_indices[key] = palette_index
    return palette_index


def _build_sign_lines(first_line: str = "") -> List[String]:
    """Create a fresh four-line sign message list for one sign instance.

    Fresh tag instances matter here because NBT tags are container objects. Reusing the same
    ``List[String]`` across multiple sign compounds can lead to serialization behavior that does
    not mirror the per-sign data layout Minecraft expects.
    """
    return List[String]([
        String(first_line),
        String(""),
        String(""),
        String(""),
    ])


def generate_placeholder_structure(width: int, depth: int, height: int, name: str) -> Path:
    """Generate a sparse placeholder structure NBT and save it as ``<name>_placeholder.nbt``.

    The structure contains:
    - a bottom platform at ``y = 0``
    - four oak signs at the platform corners on ``y = 1``
    - sign block entity data embedded in the corresponding sign block entry
    """
    if width <= 0 or depth <= 0 or height <= 0:
        raise ValueError("width, depth, and height must all be positive integers")

    if height < 2:
        raise ValueError("height must be at least 2 so the corner signs can be placed at y = 1")

    palette = List[Compound]()
    palette_indices: dict[tuple[str, tuple[tuple[str, str], ...]], int] = {}
    blocks = List[Compound]()
    diamond_index = _get_or_create_palette_index(palette, palette_indices, "minecraft:diamond_block")
    iron_index = _get_or_create_palette_index(palette, palette_indices, "minecraft:iron_block")
    froglight_index = _get_or_create_palette_index(
        palette,
        palette_indices,
        "minecraft:pearlescent_froglight",
        {"axis": "z"},
    )
    sign_index = _get_or_create_palette_index(
        palette,
        palette_indices,
        "minecraft:oak_sign",
        {"rotation": "8", "waterlogged": "false"},
    )

    max_x = width - 1
    max_z = depth - 1
    corner_positions = {
        (0, 0),
        (max_x, 0),
        (0, max_z),
        (max_x, max_z),
    }

    # The structure format is sparse: only real blocks are written.
    # Empty coordinates are intentionally omitted so Minecraft treats them as air.
    for x in range(width):
        for z in range(depth):
            is_corner = (x, z) in corner_positions
            is_edge = x == 0 or x == max_x or z == 0 or z == max_z

            if is_corner:
                state_index = diamond_index
            elif is_edge:
                state_index = iron_index
            else:
                state_index = froglight_index

            blocks.append(
                Compound(
                    {
                        "pos": List[Int]([Int(x), Int(0), Int(z)]),
                        "state": Int(state_index),
                    }
                )
            )

    for x, z in sorted(corner_positions):
        sign_pos = List[Int]([Int(x), Int(1), Int(z)])

        # Structure files used by Structure Blocks store block entity data directly inside the
        # matching block entry under ``nbt``. That means the sign's position and state live in the
        # same compound as the sign text payload, which lets Minecraft restore the sign correctly
        # when the structure is placed.
        blocks.append(
            Compound(
                {
                    "pos": sign_pos,
                    "state": Int(sign_index),
                    "nbt": Compound(
                        {
                            "id": String("minecraft:sign"),
                            "is_waxed": nbtlib.Byte(0),
                            "front_text": Compound(
                                {
                                    "has_glowing_text": nbtlib.Byte(0),
                                    "color": String("black"),
                                    "messages": _build_sign_lines(name),
                                }
                            ),
                            "back_text": Compound(
                                {
                                    "has_glowing_text": nbtlib.Byte(0),
                                    "color": String("black"),
                                    "messages": _build_sign_lines(),
                                }
                            ),
                        }
                    ),
                }
            )
        )

    structure_data = Compound(
        {
            "DataVersion": Int(DATA_VERSION),
            "size": List[Int]([Int(width), Int(height), Int(depth)]),
            "palette": palette,
            "blocks": blocks,
            "entities": List[Compound]([]),
        }
    )

    output_name = f"{_sanitize_file_component(name)}_placeholder.nbt"
    output_path = Path(__file__).resolve().parent / output_name
    nbtlib.File(structure_data).save(output_path, gzipped=True)
    return output_path


def parse_args() -> argparse.Namespace:
    """Parse command line arguments for standalone script usage."""
    parser = argparse.ArgumentParser(
        description="Generate a sparse Minecraft placeholder structure NBT file."
    )
    parser.add_argument("width", type=int, help="The structure X dimension.")
    parser.add_argument("depth", type=int, help="The structure Z dimension.")
    parser.add_argument("height", type=int, help="The structure Y dimension.")
    parser.add_argument("name", help="The text written to each corner sign.")
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    generated_path = generate_placeholder_structure(
        width=args.width,
        depth=args.depth,
        height=args.height,
        name=args.name,
    )
    print(f"Generated structure file: {generated_path}")