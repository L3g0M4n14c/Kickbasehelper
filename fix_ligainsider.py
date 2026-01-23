import re

path = "KickbaseCore/Sources/KickbaseCore/Services/LigainsiderService.swift"

with open(path, "r") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    # Fix 1: Lines that look like 'of: "..."' with no closing paren and no comma
    # Regex: indentation, 'of:', space, quote, content, quote, optional space, newline
    match = re.search(r'^(\s*of:\s*"[^"]+")(\s*)$', line)
    if match:
        # Check if previous line had "range("
        if i > 0 and "range(" in lines[i-1]:
            # It's a broken range call. Add closing paren.
            # But wait, if it's 'range(of: "foo", range: ...)' the next line would be 'range: ...'
            # Swift requires comma if multi-arg.
            # If the next line starts with 'range:', we need a comma.
            # If the next line is '{' or 'let' or something else, we need a paren.
            
            # Look ahead
            if i + 1 < len(lines):
                next_stripped = lines[i+1].strip()
                if next_stripped.startswith("range:") or next_stripped.startswith("options:"):
                     # Needs comma
                     line = match.group(1) + ",\n"
                else:
                     # Needs closing paren (or paren + comma if in existing list?)
                     # If it's "if let x = range(...), let y = ..."
                     # Then we need "),".
                     # If it's "if let x = range(...) {"
                     # We need ")".
                     
                     if next_stripped.startswith("let ") or next_stripped.startswith("guard "):
                            line = match.group(1) + "),\n"
                     elif next_stripped == "{":
                            line = match.group(1) + ")\n"
                     else:
                            # Default to )
                            line = match.group(1) + ")\n"
    
    # Fix 2: Dangling comma "of: "...", " where options were removed?
    # This might happen if 'options:' was last.
    # But usually 'options:' is 2nd arg.
    # If I removed 'options: .caseInsensitive' from end, I might have left 'of: "...", )' ?
    # Or 'of: "...",'
    
    new_lines.append(line)

with open(path, "w") as f:
    f.writelines(new_lines)

print("Scanned and patched LigainsiderService")
