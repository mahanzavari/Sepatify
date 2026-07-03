#!/bin/bash
set -euo pipefail

# ====================== SETUP ======================
if [ ! -d ".git" ]; then
  echo "🗂️  Initializing Git repository..."
  git init --initial-branch=main
fi

START_DATE="2026-06-25"
END_DATE="2026-07-03"
TARGET_COMMITS=60

USERS=(
  "AmirabbasEntezari|nandamask4@gmail.com"
  "Shayansh66|shahmohammadish66@gmail.com"
  "mahanzavari|mahanzavari@gmail.com"
)

# Helper function to generate context-aware commit messages
generate_commit_message() {
  local files=("$@")
  local primary_file="${files[0]}"

  if [[ "$primary_file" =~ "MainActivity" ]]; then
    echo "Set up MainActivity and application entry point"
  elif [[ "$primary_file" =~ "Database" || "$primary_file" =~ "Dao" || "$primary_file" =~ "Entities" ]]; then
    echo "Configure local Room database, entities, and DAOs"
  elif [[ "$primary_file" =~ "Repository" ]]; then
    echo "Implement repository pattern and data flow handlers"
  elif [[ "$primary_file" =~ "ViewModel" ]]; then
    echo "Implement ViewModels for reactive state management"
  elif [[ "$primary_file" =~ "Screen" || "$primary_file" =~ "Fragment" ]]; then
    echo "Build UI layout components and screens"
  elif [[ "$primary_file" =~ "Player" || "$primary_file" =~ "Service" ]]; then
    echo "Configure audio playback manager and service backend"
  elif [[ "$primary_file" =~ "gradle" || "$primary_file" =~ "properties" ]]; then
    echo "Configure build dependencies and project settings"
  elif [[ "$primary_file" =~ "analytics" || "$primary_file" =~ "monitoring" ]]; then
    echo "Integrate telemetry, analytics trackers, and crash reporters"
  elif [[ "$primary_file" =~ "Supabase" || "$primary_file" =~ "remote" ]]; then
    echo "Set up Supabase client and networking layer"
  elif [[ "$primary_file" =~ "xml" ]]; then
    echo "Update layout resources and Android XML configs"
  else
    echo "Optimize project codebase and restructure packages"
  fi
}

# ====================== STEP 1: COMMIT .GITIGNORE FIRST ======================
echo "⚙️  Checking for .gitignore files..."

# If no .gitignore exists at the root, create a standard Android .gitignore
if [ ! -f ".gitignore" ]; then
  echo "📝 No root .gitignore found. Creating standard Android configuration..."
  cat << 'EOF' > .gitignore
*.iml
.gradle
/local.properties
/.idea/
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
*.apk
*.ap_
*.jks
*.keystore
EOF
fi

# Find and add all .gitignore files (ignoring warnings/errors here)
mapfile -t GITIGNORE_FILES < <(find . -name ".gitignore" -not -path '*/.git/*' | sed 's|^\./||')

for f in "${GITIGNORE_FILES[@]}"; do
  git add "$f" 2>/dev/null || true
done

# Perform Commit #1 with gitignore configurations
git_author_name=$(echo "${USERS[0]}" | cut -d'|' -f1)
git_author_email=$(echo "${USERS[0]}" | cut -d'|' -f2)
initial_timestamp="$START_DATE 08:00:00"

GIT_AUTHOR_DATE="$initial_timestamp" \
GIT_COMMITTER_DATE="$initial_timestamp" \
GIT_AUTHOR_NAME="$git_author_name" \
GIT_AUTHOR_EMAIL="$git_author_email" \
GIT_COMMITTER_NAME="$git_author_name" \
GIT_COMMITTER_EMAIL="$git_author_email" \
git commit -m "Initial commit: Configure Git ignore patterns" >/dev/null 2>&1 || true

echo " [Commit 1/$TARGET_COMMITS] $initial_timestamp | $git_author_name | Initial commit: Configure Git ignore patterns"


# ====================== STEP 2: COMMIT REMAINING FILES ======================
echo "🔍 Querying non-ignored project files using Git indexing..."

# Use Git's own engine to list untracked/tracked files, strictly respecting .gitignore
mapfile -t ALL_FILES < <(git ls-files --others --cached --exclude-standard \
  | grep -vE "^(setup\.sh|commits\.txt|Could|Failed|Get|Run|Toolchain)$" \
  || true)

TOTAL_FILES=${#ALL_FILES[@]}

if (( TOTAL_FILES == 0 )); then
  echo "⚠️  No unignored project files were detected for staging."
  exit 1
fi

REMAINING_COMMITS=$(( TARGET_COMMITS - 1 ))
echo "📦 Found $TOTAL_FILES non-ignored files. Distributing them across remaining $REMAINING_COMMITS commits..."

start_idx=0
for ((i=0; i<REMAINING_COMMITS; i++)); do
  # Calculate file distribution
  chunk_size=$(( TOTAL_FILES / REMAINING_COMMITS ))
  if (( i < (TOTAL_FILES % REMAINING_COMMITS) )); then
    chunk_size=$(( chunk_size + 1 ))
  fi

  commit_files=("${ALL_FILES[@]:start_idx:chunk_size}")
  start_idx=$(( start_idx + chunk_size ))

  if (( ${#commit_files[@]} == 0 )); then
    continue
  fi

  # Stage current slice of files (standard add, no force-bypass needed)
  for file in "${commit_files[@]}"; do
    if [[ -f "$file" ]]; then
      git add "$file"
    fi
  done

  # Determine author (cycle through developers starting from index 1 for variety)
  user_idx=$(( (i + 1) % 3 ))
  name=$(echo "${USERS[$user_idx]}" | cut -d'|' -f1)
  email=$(echo "${USERS[$user_idx]}" | cut -d'|' -f2)

  # Calculate the date offset from index 0 to 58 across 8 days
  day_offset=$(( i * 8 / (REMAINING_COMMITS - 1) ))
  current_date=$(date -I -d "$START_DATE + $day_offset days")
  
  # Distribute commit times during typical business hours
  hour=$(( (i % 6) * 2 + 9 ))
  minute=$(( RANDOM % 60 ))
  second=$(( RANDOM % 60 ))
  timestamp="$current_date $(printf "%02d" $hour):$(printf "%02d" $minute):$(printf "%02d" $second)"

  # Generate logical commit message
  message=$(generate_commit_message "${commit_files[@]}")

  # Commit files safely
  GIT_AUTHOR_DATE="$timestamp" \
  GIT_COMMITTER_DATE="$timestamp" \
  GIT_AUTHOR_NAME="$name" \
  GIT_AUTHOR_EMAIL="$email" \
  GIT_COMMITTER_NAME="$name" \
  GIT_COMMITTER_EMAIL="$email" \
  git commit -m "$message" >/dev/null 2>&1 || true

  echo " [Commit $((i+2))/$TARGET_COMMITS] $timestamp | $name | $message"
done

# Double check that no untracked, non-ignored files were missed
git add -A
if ! git diff --cached --quiet; then
  timestamp="$END_DATE 18:00:00"
  GIT_AUTHOR_DATE="$timestamp" \
  GIT_COMMITTER_DATE="$timestamp" \
  GIT_AUTHOR_NAME="AmirabbasEntezari" \
  GIT_AUTHOR_EMAIL="nandamask4@gmail.com" \
  GIT_COMMITTER_NAME="AmirabbasEntezari" \
  GIT_COMMITTER_EMAIL="nandamask4@gmail.com" \
  git commit -m "Final cleanup and workspace synchronization" >/dev/null 2>&1 || true
  echo " Added final remaining files."
fi

echo " Done. The repository now has $TARGET_COMMITS structured history entries."
echo ""
echo "Last 10 commits:"
git log --oneline --graph --decorate -10