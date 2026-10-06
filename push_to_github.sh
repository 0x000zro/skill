#!/data/data/com.termux/files/usr/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "🚀 Pushing CDP Learning App to GitHub..."
echo "=========================================="

# Stage all files respecting .gitignore
git add -A

# Commit
git commit -m "feat: complete CDP Learning App with headless CMS, Room cache, and Compose UI" || echo "Nothing new to commit"

# Push to origin main
git push -u origin main

echo ""
echo "=========================================="
echo "✅ Successfully pushed to GitHub!"
echo "=========================================="
