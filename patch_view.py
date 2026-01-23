import os

file_path = "KickbaseCore/Sources/KickbaseCore/TransferRecommendationsView.swift"

with open(file_path, "r") as f:
    content = f.read()

# 1. Replace the inner Section content
start_marker = 'Section("Werte-Filter") {'
start_index = content.find(start_marker)

if start_index == -1:
    print("Could not find Start Marker")
    exit(1)

# Find the matching closing brace for this section
open_braces = 0
found_start = False
end_index = -1

for i in range(start_index, len(content)):
    char = content[i]
    if char == '{':
        open_braces += 1
        found_start = True
    elif char == '}':
        open_braces -= 1
        if found_start and open_braces == 0:
            end_index = i + 1
            break

if end_index == -1:
    print("Could not find End Marker")
    exit(1)

new_section_content = """Section("Werte-Filter") {
                    HStack {
                        Text("Max. Preis")
                        Spacer()
                        TextField("€ Millionen", text: maxPriceBinding)
                            #if os(iOS)
                            .textFieldStyle(.roundedBorder)
                            .frame(width: 120)
                            #endif
                    }

                    HStack {
                        Text("Min. Punkte")
                        Spacer()
                        TextField("Punkte", text: minPointsBinding)
                            #if os(iOS)
                            .textFieldStyle(.roundedBorder)
                            .frame(width: 80)
                            #endif
                    }

                    HStack {
                        Text("Min. Vertrauen")
                        Spacer()
                        TextField("0.0 - 1.0", text: minConfidenceBinding)
                            #if os(iOS)
                            .textFieldStyle(.roundedBorder)
                            .frame(width: 100)
                            #endif
                    }
                }"""

new_content = content[:start_index] + new_section_content + content[end_index:]

# 2. Append the extension if it's missing (checking for unique var name)
if "var maxPriceBinding: Binding<String>" not in new_content:
    extension_code = """

extension TransferRecommendationsView {
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
}
"""
    new_content += extension_code

with open(file_path, "w") as f:
    f.write(new_content)

print("Successfully patched file.")
