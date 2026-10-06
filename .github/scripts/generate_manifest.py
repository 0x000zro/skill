#!/usr/bin/env python3
"""
Manifest Generator for CDP Learning App Headless CMS.
Scans study/, revision/, practice/, and test/ directories
and generates a structured manifest.json at the repository root.
"""

import os
import json
import re
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parents[2]
MANIFEST_PATH = ROOT_DIR / "manifest.json"

def extract_markdown_title(filepath: Path) -> str:
    """Extract first H1 markdown heading or clean filename."""
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line.startswith("# "):
                    return line.lstrip("# ").strip()
    except Exception as e:
        print(f"Warning reading {filepath}: {e}")
    # Fallback to readable filename
    clean_name = filepath.stem.replace("_", " ").replace("-", " ")
    return clean_name.title()

def scan_markdown_folder(folder_name: str, prefix_id: str, default_category: str):
    """Scans study or revision folder for .md files."""
    folder = ROOT_DIR / folder_name
    items = []
    if not folder.exists():
        return items

    for file in sorted(folder.glob("**/*.md")):
        rel_path = file.relative_to(ROOT_DIR).as_posix()
        item_id = f"{prefix_id}_{file.stem}"
        title = extract_markdown_title(file)
        
        # Determine category based on subfolder if present
        subfolder = file.parent.relative_to(folder).as_posix()
        category = default_category if subfolder == "." else subfolder.replace("_", " ").title()

        items.append({
            "id": item_id,
            "title": title,
            "path": rel_path,
            "category": category
        })
    return items

def scan_practice_folder():
    """Scans practice/ folder for MCQ .json files."""
    folder = ROOT_DIR / "practice"
    items = []
    if not folder.exists():
        return items

    for file in sorted(folder.glob("**/*.json")):
        rel_path = file.relative_to(ROOT_DIR).as_posix()
        try:
            with open(file, "r", encoding="utf-8") as f:
                data = json.load(f)
            
            title = data.get("title", file.stem.replace("_", " ").title())
            category = data.get("category", "General Pedagogy")
            questions = data.get("questions", [])
            total_questions = len(questions)

            items.append({
                "id": data.get("id", f"practice_{file.stem}"),
                "title": title,
                "path": rel_path,
                "category": category,
                "total_questions": total_questions
            })
        except Exception as e:
            print(f"Error parsing practice file {file}: {e}")
    return items

def scan_test_folder():
    """Scans test/ folder for Mock Test .json files."""
    folder = ROOT_DIR / "test"
    items = []
    if not folder.exists():
        return items

    for file in sorted(folder.glob("**/*.json")):
        rel_path = file.relative_to(ROOT_DIR).as_posix()
        try:
            with open(file, "r", encoding="utf-8") as f:
                data = json.load(f)

            title = data.get("title", file.stem.replace("_", " ").title())
            duration = data.get("duration_minutes", 30)
            questions = data.get("questions", [])
            total_questions = len(questions)

            items.append({
                "id": data.get("id", f"test_{file.stem}"),
                "title": title,
                "path": rel_path,
                "duration_minutes": duration,
                "total_questions": total_questions
            })
        except Exception as e:
            print(f"Error parsing test file {file}: {e}")
    return items

def main():
    print(f"Scanning Headless CMS content in: {ROOT_DIR}")

    manifest_data = {
        "version": 1,
        "last_updated": os.getenv("GITHUB_SHA", "local_build"),
        "study_modes": scan_markdown_folder("study", "study", "Child Development"),
        "revision_modes": scan_markdown_folder("revision", "rev", "Quick Revision"),
        "practice_modes": scan_practice_folder(),
        "test_modes": scan_test_folder()
    }

    with open(MANIFEST_PATH, "w", encoding="utf-8") as f:
        json.dump(manifest_data, f, indent=2, ensure_ascii=False)

    print(f"Manifest generated successfully at: {MANIFEST_PATH}")
    print(f"Summary: Study={len(manifest_data['study_modes'])}, "
          f"Revision={len(manifest_data['revision_modes'])}, "
          f"Practice={len(manifest_data['practice_modes'])}, "
          f"Test={len(manifest_data['test_modes'])}")

if __name__ == "__main__":
    main()
