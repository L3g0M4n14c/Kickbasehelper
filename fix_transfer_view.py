import os

file_path = "KickbaseCore/Sources/KickbaseCore/TransferRecommendationsView.swift"

with open(file_path, "r") as f:
    content = f.read()

# 1. Remove the incorrectly inserted block (inside RecommendationFilters)
# The block starts with "// MARK: - Computed Bindings" and ends with closing brace of minConfidenceBinding
bad_block_start = content.find("// MARK: - Computed Bindings for Kotlin Compat")
if bad_block_start != -1:
    # Find the end of this block. It contains 3 vars.
    # We can scan until "var minConfidenceBinding" block ends.
    # Or just search for the specific content we added.
    bad_block_part = 'var minConfidenceBinding: Binding<String> {'
    idx_conf = content.find(bad_block_part, bad_block_start)
    if idx_conf != -1:
        # Find close of that var
        # It has nested {} for get/set and if.
        # Let's just find the next "enum SortOption" and cut everything between.
        enum_idx = content.find("enum SortOption:", idx_conf)
        # Actually I inserted it before "enum SortOption", so it is adjacent.
        # But I need to be careful not to delete other fields of RecommendationFilters if any were after it?
        # RecommendationFilters had "var minConfidence: Double?" before my block.
        # So I can strip from bad_block_start up to enum_idx BUT keep the closing brace of struct RecommendationFilters!
        
        # Original insertion was: content[:last_brace_idx] + props_code + content[last_brace_idx:]
        # So I pushed the closing brace '}' of RecommendationFilters AFTER my props code.
        # So I need to keep the last '}' before enum SortOption.
        
        # Let's find the closing brace before enum SortOption.
        last_brace_idx = -1
        for i in range(enum_idx - 1, 0, -1):
            if content[i] == '}':
                last_brace_idx = i
                break
        
        if last_brace_idx != -1:
            # The range from bad_block_start to last_brace_idx should be deleted.
            # But wait, last_brace_idx is the closing brace of struct. I want to keep it.
            # So delete from bad_block_start to last_brace_idx.
            content = content[:bad_block_start] + content[last_brace_idx:]
            print("Removed incorrect properties block")

# 2. Insert at correct location (before var body: some View)
body_idx = content.find("var body: some View {")
if body_idx == -1:
    print("Could not find var body")
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

new_content = content[:body_idx] + props_code + content[body_idx:]

with open(file_path, "w") as f:
    f.write(new_content)

print("Successfully moved properties to body")
