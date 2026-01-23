import os

file_path = "KickbaseCore/Sources/KickbaseCore/TransferRecommendationsView.swift"

with open(file_path, "r") as f:
    content = f.read()

# 1. Remove the extension if present
ext_start = content.find("extension TransferRecommendationsView {")
if ext_start != -1:
    content = content[:ext_start]
    print("Removed extension from end")

# 2. Find insertion point in struct (before enum SortOption)
insert_marker = "enum SortOption: String, CaseIterable {"
insert_idx = content.find(insert_marker)

if insert_idx == -1:
    print("Could not find insertion point (enum SortOption)")
    exit(1)

# Backtrack to find the closing brace of the struct before the enum
# There should be a '}' just before the enum (ignoring whitespace)
# We can just insert BEFORE usage of enum? No.
# Usage is inside struct. Definition is outside.
# So struct closes before enum.
# We need to insert inside the struct.
# Let's assume the '}' before enum SortOption is the struct closer.

last_brace_idx = -1
for i in range(insert_idx - 1, 0, -1):
    if content[i] == '}':
        last_brace_idx = i
        break

if last_brace_idx == -1:
    print("Could not find struct closing brace")
    exit(1)

props_code = """
    // MARK: - Computed Bindings for Kotlin Compat
    var maxPriceBinding: Binding<String> {
        Binding<String>(
            get: { 
                if let val = filters.maxPrice { return String(val) }
                return "" 
            },
            set: { val in
                if let v = Int(val) { filters.maxPrice = v }
                else { filters.maxPrice = nil }
            }
        )
    }

    var minPointsBinding: Binding<String> {
        Binding<String>(
            get: { 
                if let val = filters.minPoints { return String(val) }
                return "" 
            },
            set: { val in
                if let v = Int(val) { filters.minPoints = v }
                else { filters.minPoints = nil }
            }
        )
    }

    var minConfidenceBinding: Binding<String> {
        Binding<String>(
            get: { 
                if let val = filters.minConfidence { return String(val) }
                return "" 
            },
            set: { val in
                let s = val.replacingOccurrences(of: ",", with: ".")
                if let v = Double(s) { filters.minConfidence = v }
                else { filters.minConfidence = nil }
            }
        )
    }
"""

new_content = content[:last_brace_idx] + props_code + content[last_brace_idx:]

with open(file_path, "w") as f:
    f.write(new_content)

print("Successfully moved properties into struct")
