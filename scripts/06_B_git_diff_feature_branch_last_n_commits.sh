#!/bin/bash

# =============================================================================
# Git Diff Feature Branch Last N Commits - Dynamic Patch Generator
# =============================================================================
# 
# This script generates a patch file for the last N commits from the current
# feature branch with a descriptive name based on the branch name and commit count.
#
# Magic Explained:
# 1. Get current branch name using 'git branch --show-current'
# 2. Get configurable number of commits to include
# 3. Transform branch name to filesystem-safe filename
# 4. Generate patch file for last N commits with semantic naming
# =============================================================================

# Set strict error handling - exit on any command failure
set -e

# =============================================================================
# CONFIGURATION: Number of commits to include in patch
# =============================================================================
# 
# Configure how many commits back from HEAD to include in the patch.
# Default: 11 commits (covers most recent work sessions)
# 
# Examples:
#   NUM_COMMITS=5   # Last 5 commits
#   NUM_COMMITS=11  # Last 11 commits (default)
#   NUM_COMMITS=20  # Last 20 commits
# =============================================================================
NUM_COMMITS=11

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
echo "📊 Including last $NUM_COMMITS commits in patch"

# =============================================================================
# STEP 2: Validate commit count
# =============================================================================
# Ensure the requested number of commits is valid (not more than available)
# This prevents errors when trying to go back further than repository history
if [ "$NUM_COMMITS" -le 0 ]; then
    echo "❌ Error: NUM_COMMITS must be a positive number"
    echo "   Please set NUM_COMMITS to a value >= 1"
    exit 1
fi

# Check if we have enough commits in the repository
available_commits=$(git rev-list --count HEAD)
if [ "$NUM_COMMITS" -gt "$available_commits" ]; then
    echo "❌ Error: Requested $NUM_COMMITS commits but only $available_commits available"
    echo "   Please set NUM_COMMITS to $available_commits or less"
    exit 1
fi

# =============================================================================
# STEP 3: Transform branch name to valid filename
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
# STEP 4: Generate the patch file
# =============================================================================
# 
# The 'git diff HEAD~${NUM_COMMITS}..HEAD' command:
# - 'HEAD~${NUM_COMMITS}' represents the commit N steps back from current HEAD
# - 'HEAD' represents the current commit
# - '..' (double dot) shows changes between the two commits
# - This captures exactly the changes made in the last N commits
#
# The '>' redirects all output to the file instead of console
patch_filename="${safe_filename}_last_${NUM_COMMITS}_commits.patch"

echo "🔧 Generating patch file: $patch_filename"
echo "   Changes from: HEAD~$NUM_COMMITS → HEAD"
echo "   Commits included: $NUM_COMMITS"

# Generate the diff and save to file
git diff HEAD~${NUM_COMMITS}..HEAD > "$patch_filename"

# =============================================================================
# STEP 5: Provide user feedback
# =============================================================================
if [ $? -eq 0 ]; then
    echo "✅ Success! Patch file created: $patch_filename"
    
    # Show file size for user information
    if [ -f "$patch_filename" ]; then
        file_size=$(stat -f%z "$patch_filename" 2>/dev/null || stat -c%s "$patch_filename" 2>/dev/null || echo "unknown")
        echo "   File size: $file_size bytes"
        
        # Show number of lines changed for additional context
        if command -v wc >/dev/null 2>&1; then
            lines_added=$(grep -c '^+' "$patch_filename" 2>/dev/null || echo "0")
            lines_removed=$(grep -c '^-' "$patch_filename" 2>/dev/null || echo "0")
            echo "   Lines added: $lines_added, Lines removed: $lines_removed"
        fi
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
echo ""
echo "🔄 To change commit count, edit NUM_COMMITS at the top of this script"
