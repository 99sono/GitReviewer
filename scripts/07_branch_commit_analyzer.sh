#!/bin/bash

# =============================================================================
# Branch Commit Analyzer - Rebase Planning Tool
# =============================================================================
# 
# This script analyzes the current branch's commit history and provides
# information needed for planning rebase operations (like squashing commits).
#
# Magic Explained:
# 1. Detect current branch and find merge base with main
# 2. Count commits since branching from main
# 3. Display formatted commit log with details
# 4. Provide rebase commands and save to log file
# =============================================================================

# Set strict error handling - exit on any command failure
set -e

# =============================================================================
# STEP 1: Validate we're in a git repository and on a branch
# =============================================================================
# Check if we're in a git repository and on a proper branch (not detached HEAD)
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "❌ Error: Not in a git repository"
    exit 1
fi

# Get current branch name - if empty, we're in detached HEAD state
current_branch=$(git branch --show-current)
if [ -z "$current_branch" ]; then
    echo "❌ Error: Not on a branch (detached HEAD state)"
    echo "   Please checkout a branch to analyze its commits."
    exit 1
fi

echo "📋 Analyzing branch: $current_branch"

# =============================================================================
# STEP 2: Find the branching point (merge base with main)
# =============================================================================
# 
# 'git merge-base main HEAD' finds the common ancestor commit where
# the current branch diverged from main. This is the starting point
# for counting commits in the current branch.
branch_point=$(git merge-base main HEAD 2>/dev/null)

if [ $? -ne 0 ]; then
    echo "❌ Error: Could not find merge base with main"
    echo "   Please ensure main branch exists and try again."
    exit 1
fi

# =============================================================================
# STEP 3: Count commits since branching
# =============================================================================
# 
# 'git rev-list main..HEAD --count' counts all commits that exist in HEAD
# but not in main - these are the commits unique to the current branch
commit_count=$(git rev-list main..HEAD --count 2>/dev/null)

if [ $? -ne 0 ]; then
    echo "❌ Error: Could not count commits"
    exit 1
fi

echo "📈 Commits since main: $commit_count"

# Handle special case: no commits (branch is at main)
if [ "$commit_count" -eq 0 ]; then
    echo ""
    echo "ℹ️  Branch has no new commits compared to main"
    echo "   Branch: $current_branch"
    echo "   Status: Up to date with main"
    echo ""
    echo "💡 Nothing to rebase - branch is clean!"
    exit 0
fi

# =============================================================================
# STEP 4: Create output filename based on branch name
# =============================================================================
# 
# Transform branch name to safe filename (same logic as patch script)
# Replace '/' with '_' to make filesystem-safe filename
safe_filename=$(echo "$current_branch" | tr '/' '_')
log_filename="${safe_filename}_commits.log"

# =============================================================================
# STEP 5: Generate and display commit analysis
# =============================================================================
echo ""
echo "📊 === BRANCH COMMIT ANALYSIS ==="
echo "   Branch: $current_branch"
echo "   Merge base: ${branch_point:0:8}"
echo "   Total commits: $commit_count"
echo ""

# Display commit history in a clean format
echo "📝 === COMMIT HISTORY ==="
git log main..HEAD --pretty=format:"   %h - %s (%ad) <%an>" --date=short --no-merges

echo ""
echo ""

# =============================================================================
# STEP 6: Provide rebase guidance
# =============================================================================
echo "🔧 === REBASE PLANNING ==="
echo "   To squash ALL $commit_count commits into one:"
echo "   git rebase -i HEAD~$commit_count"
echo ""
echo "   To squash last 5 commits (keep older history):"
echo "   git rebase -i HEAD~5"
echo ""

# =============================================================================
# STEP 7: Save detailed log to file
# =============================================================================
# Create comprehensive log file with all details
{
    echo "================================================================================"
    echo "BRANCH COMMIT ANALYSIS REPORT"
    echo "================================================================================"
    echo "Generated: $(date)"
    echo "Branch: $current_branch"
    echo "Merge base with main: $branch_point"
    echo "Total commits since main: $commit_count"
    echo ""
    echo "================================================================================"
    echo "DETAILED COMMIT LOG"
    echo "================================================================================"
    echo ""
    
    # Full commit log with all details
    git log main..HEAD --pretty=format:"%h - %s%nAuthor: %an <%ae>%nDate: %ad%n%n" --date=iso --no-merges
    
    echo ""
    echo "================================================================================"
    echo "REBASE COMMANDS"
    echo "================================================================================"
    echo "To squash ALL $commit_count commits:"
    echo "  git rebase -i HEAD~$commit_count"
    echo ""
    echo "To view this branch vs main:"
    echo "  git diff main...HEAD"
    echo ""
    echo "To see what files changed in this branch:"
    echo "  git diff --stat main..HEAD"
    echo ""
    
} > "$log_filename"

# =============================================================================
# STEP 8: Final summary
# =============================================================================
echo "✅ Analysis complete!"
echo ""
echo "📁 Log file saved: $log_filename"
echo "   Full commit details and rebase commands are documented there"
echo ""
echo "💡 Quick rebase command for this branch:"
echo "   git rebase -i HEAD~$commit_count"
echo ""
echo "📖 For more options, check: $log_filename"
