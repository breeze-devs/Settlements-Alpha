"""
Migrates flat settlements: resource location IDs to path-scoped equivalents.

  settlements:log_storage       -> settlements:building_definitions/log_storage
  settlements:ancient_scholar   -> settlements:specializations/ancient_scholar

Scans all .java and .json files under src/. Dry-run by default; pass --apply to write.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).parent.parent / "src"

# All known building IDs — includes definitions/ JSONs, meta.json orphans, and test fixtures.
# Anything that conceptually represents a BuildingDefinition gets the building_definitions/ scope.
BUILDING_IDS = {
    # definition JSONs (15)
    "barn", "barracks", "dock", "farmhouse", "fish_drying_rack",
    "house", "log_storage", "market_stall", "mine_entrance", "sawmill",
    "smelter", "tavern", "town_hall", "watchtower", "well",
    # meta.json references without a matching definition yet (8)
    "wheat_field", "stonecutter", "quarry", "mine", "lumber_camp",
    "fishing_dock", "fish_market", "farm",
    # test-only synthetic building IDs
    "forbidden_stone_building", "forbidden_stone", "primary_only_lumber", "bad_json_test",
    "missing_id", "unknown_trait_test", "unknown_resource_test",
    "preferred_tags_test", "valid_building", "invalid_building",
    "lumberjack",
}

SPECIALIZATION_IDS = {
    "ancient_scholar", "war_smith",
}


def build_pattern(ids: set[str]) -> re.Pattern:
    # Match settlements:ID only when not already followed by / (already-migrated guard)
    # and ensure we match the full token (no trailing word chars like settlements:barn_door)
    escaped = "|".join(re.escape(i) for i in sorted(ids, key=len, reverse=True))
    return re.compile(r'settlements:(' + escaped + r')(?=[^/\w]|$)')


BUILDING_PATTERN = build_pattern(BUILDING_IDS)
SPECIALIZATION_PATTERN = build_pattern(SPECIALIZATION_IDS)


def migrate_content(text: str) -> tuple[str, list[str]]:
    changes = []

    def replace_building(m: re.Match) -> str:
        old = m.group(0)
        new = f"settlements:building_definitions/{m.group(1)}"
        changes.append(f"  {old!r} -> {new!r}")
        return new

    def replace_specialization(m: re.Match) -> str:
        old = m.group(0)
        new = f"settlements:specializations/{m.group(1)}"
        changes.append(f"  {old!r} -> {new!r}")
        return new

    text = BUILDING_PATTERN.sub(replace_building, text)
    text = SPECIALIZATION_PATTERN.sub(replace_specialization, text)
    return text, changes


def process_file(path: Path, apply: bool) -> int:
    try:
        original = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return 0

    migrated, changes = migrate_content(original)
    if not changes:
        return 0

    print(f"\n{path.relative_to(ROOT.parent)}")
    for c in changes:
        print(c)

    if apply:
        path.write_text(migrated, encoding="utf-8")

    return len(changes)


def main() -> None:
    apply = "--apply" in sys.argv
    if not apply:
        print("DRY RUN — pass --apply to write changes\n")

    total_files = 0
    total_replacements = 0

    for ext in ("*.java", "*.json"):
        for path in ROOT.rglob(ext):
            count = process_file(path, apply)
            if count:
                total_files += 1
                total_replacements += count

    print(f"\n{'Applied' if apply else 'Would apply'} {total_replacements} replacement(s) across {total_files} file(s).")


if __name__ == "__main__":
    main()
