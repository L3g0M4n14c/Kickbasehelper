import os

file_path = "KickbaseCore/Sources/KickbaseCore/TransferRecommendationsView.swift"

with open(file_path, "r") as f:
    content = f.read()

# 1. Extract and Remove the block from TransferRecommendationsView
block_start_marker = "// MARK: - Computed Bindings for Kotlin Compat"
start_idx = content.find(block_start_marker)

if start_idx == -1:
    print("Could not find block in TransferRecommendationsView")
    exit(1)

# Find the end of minConfidenceBinding
end_marker = "var minConfidenceBinding: Binding<String> {"
idx_conf = content.find(end_marker, start_idx)
if idx_conf == -1:
    print("Could not find minConfidenceBinding start")
    exit(1)

# Find the brace closing minConfidenceBinding
# It has nesting: { ... set: { ... } ...) }
# I'll cheat and look for the empty line before "var body: some View {" which was where I inserted it?
# Or count braces.
# Since I inserted it right before "var body: some View {", 
# I can just cut from start_idx to the start of "var body: some View {".
body_idx = content.find("var body: some View {")

if body_idx == -1:
     print("Could not find body")
     exit(1)

# Cut the block
code_block = content[start_idx:body_idx]
content = content[:start_idx] + content[body_idx:]

print("Extracted block.")

# 2. Insert into FilterSheet
# Find "struct FilterSheet: View {"
fs_start = content.find("struct FilterSheet: View {")
if fs_start == -1:
    print("Could not find FilterSheet")
    exit(1)

# Insert after the first brace of FilterSheet struct
# Find the opening brace
brace_idx = content.find("{", fs_start)
if brace_idx == -1:
    print("Could not find FilterSheet brace")
    exit(1)

# Insert after the brace
insertion_point = brace_idx + 1
new_content = content[:insertion_point] + "\n" + code_block + content[insertion_point:]

with open(file_path, "w") as f:
    f.write(new_content)

print("Moved properties to FilterSheet")
