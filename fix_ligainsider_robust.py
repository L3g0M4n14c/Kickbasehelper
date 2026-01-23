import re
import os

path = "KickbaseCore/Sources/KickbaseCore/Services/LigainsiderService.swift"

with open(path, "r") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    # Regex to capture 'of: "string"' handling escaped quotes
    # ^ indentation + 'of:' + space + " + (non-quote OR escaped char)* + " + space + end
    match = re.search(r'^(\s*of:\s*"(?:[^"\\]|\\.)*")(\s*)$', line)
    
    if match:
        content = match.group(1)
        trailing = match.group(2)
        
        # Check context
        needs_fix = False
        if i > 0 and ("range(" in lines[i-1] or "range(" in lines[i]):
             needs_fix = True
        
        if needs_fix:
            # Look ahead
            suffix = ")"
            if i + 1 < len(lines):
                next_stripped = lines[i+1].strip()
                if next_stripped.startswith("range:") or next_stripped.startswith("options:"):
                     suffix = ","
                elif next_stripped.startswith("let ") or next_stripped.startswith("guard ") or next_stripped.startswith("if "):
                     suffix = "),"
            
            # If line ends with newline, preserve it
            end_char = "\n" if line.endswith("\n") else ""
            line = content + suffix + end_char
            print(f"Fixed line {i+1}: {line.strip()}")

    new_lines.append(line)

with open(path, "w") as f:
    f.writelines(new_lines)

print("Scanned and patched LigainsiderService (Robust)")
