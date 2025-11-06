#!/bin/bash

# =============================================================================
# Git Diff Branch to Origin - Dynamic Feature Branch Patch Generator
# =============================================================================
# 
# This script automatically detects the current feature branch and creates a
# patch file with a descriptive name based on the branch name.
#
# Magic Explained:
# 1. Get current branch name using 'git branch --show-current'
# 2. Transform branch name to filesystem-safe filename
# 3. Generate patch file with semantic naming
# =============================================================================

# Set strict error handling - exit on any command failure
set -e

# =============================================================================
# STEP 1: Get the current branch name
# =============================================================================
# 'git branch --show-current' returns the name of the currently checked-out branch
# Example: If you're on "feature/sono99_diff_parse", this returns "feature/sono99_diff_parse"
# If not in a git repository or in detached HEAD state, this will fail
current_branch=$(git branch --show-current)

# Check if we successfully got a branch name
if [ -z "$current_branch" ]; then
    echo "❌ Error: Not on a branch (detached HEAD state) or not in a git repository"
    echo "   Please ensure you're on a feature branch and try again."
    exit 1
fi

echo "📋 Detected current branch: $current_branch"

# =============================================================================
# STEP 2: Transform branch name to valid filename
# =============================================================================
# 
# The Transformation Process:
# 1. Replace forward slashes '/' with underscores '_'
#    - Filesystem can't have '/' in names, so "feature/branch" becomes "feature_branch"
# 2. This creates a clean, descriptive filename that's safe for all filesystems
#
# Example transformations:
#   "feature/sono99_diff_parse" → "feature_sono99_diff_parser"
#   "bugfix/issue-123"          → "bugfix_issue-123"
#   "hotfix/critical-fix"       → "hotfix_critical-fix"
# =============================================================================
safe_filename=$(echo "$current_branch" | tr '/' '_')

# =============================================================================
# STEP 3: Generate the patch file
# =============================================================================
# 
# The 'git diff main...HEAD' command:
# - 'main' is the base branch (you could make this configurable)
# - 'HEAD' represents the current commit
# - '...' (triple dot) shows changes between main and current branch
#   (only changes in the current branch, not commits on main since branching)
#
# The '>' redirects all output to the file instead of console
patch_filename="${safe_filename}.patch"

echo "🔧 Generating patch file: $patch_filename"
echo "   Changes from: main → $current_branch"

# Generate the diff and save to file
git diff main...HEAD > "$patch_filename"

# =============================================================================
# STEP 4: Provide user feedback
# =============================================================================
if [ $? -eq 0 ]; then
    echo "✅ Success! Patch file created: $patch_filename"
    
    # Show file size for user information
    if [ -f "$patch_filename" ]; then
        file_size=$(stat -f%z "$patch_filename" 2>/dev/null || stat -c%s "$patch_filename" 2>/dev/null || echo "unknown")
        echo "   File size: $file_size bytes"
    fi
else
    echo "❌ Failed to generate patch file"
    exit 1
fi

echo ""
echo "📁 You can now:"
echo "   • Review the patch: less $patch_filename"
echo "   • Apply elsewhere: git apply $patch_filename"
echo "   • Share for review: Upload $patch_filename"
