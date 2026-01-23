import re

path = "KickbaseCore/Sources/KickbaseCore/Services/LigainsiderService.swift"

with open(path, "r") as f:
    content = f.read()

# Replace .range(of: with .findRange(of:
content = content.replace(".range(of:", ".findRange(of:")

# Replace , range: with , in:
# Only do this if we are relatively sure. 
content = content.replace(", range:", ", in:")

with open(path, "w") as f:
    f.write(content)

print("Refactored search methods")
