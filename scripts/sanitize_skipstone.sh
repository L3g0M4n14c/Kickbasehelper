#!/usr/bin/env bash
set -euo pipefail

# Sanitizer for common generated Kotlin issues seen in skipstone outputs.
# Applies conservative, reversible textual fixes to increase chance of Kotlin compile.

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
SKIP_OUT="$ROOT_DIR/KickbaseCore/.build/plugins/outputs"

if [ ! -d "$SKIP_OUT" ]; then
  echo "No skipstone outputs found at $SKIP_OUT" >&2
  exit 1
fi

# Backup before modifying
TMP_BACKUP="/tmp/skipstone_sanitize_backup_$(date +%s)"
mkdir -p "$TMP_BACKUP"
cp -R "$SKIP_OUT" "$TMP_BACKUP/" || true

changed=0

find "$SKIP_OUT" -name "*.kt" -print0 | while IFS= read -r -d '' file; do
  echo "Sanitizing $file"

  # 1) Ensure compareTo functions are marked as operator (avoid double-inserting)
  perl -0777 -pi -e 's/(?<!operator\s)fun\s+compareTo\s*\(/operator fun compareTo(/g' "$file" || true
  # Cleanup accidental double 'operator operator' (if present)
  perl -0777 -pi -e 's/operator\s+operator/operator/g' "$file" || true

  # 2) Remove trailing commas before ) or ] which can cause syntax errors
  perl -0777 -pi -e 's/,\s*([)\]])/\1/g' "$file" || true

  # 3) Replace nullable class literals '::class?' with '::class'
  perl -pi -e 's/::class\?/::class/g' "$file" || true

  # 4) Normalize Array instance checks/casts to use star projection to avoid "erased type" errors
  perl -0777 -pi -e 's/Array<Any\?>/Array<*>/g' "$file" || true
  perl -0777 -pi -e 's/Array<Any>/Array<*>/g' "$file" || true
  perl -0777 -pi -e 's/\bis\s+Array<\*/is Array<*>/g' "$file" || true
  perl -0777 -pi -e 's/as\?\s+Array<[^>]*>/as? Array<*>/g' "$file" || true

  # If the star-projected buffer was introduced in a constructor call (e.g., Array<*>()),
  # replace it with Array<Any?>() because projections are not allowed on type arguments
  # of function/constructor calls in Kotlin.
  perl -0777 -pi -e 's/Array<\*>\s*\(\s*\)/Array<Any?>()/g' "$file" || true

  # 5) Fix stray '>>>' token sequences in generics (e.g., Array<*>>>) -> Array<*>
  perl -0777 -pi -e 's/Array<\s*\*\s*>>+/Array<*>/g' "$file" || true
  perl -0777 -pi -e 's/Dictionary<\s*\*,\s*\*>+>/Dictionary<*, *>/' "$file" || true

  # 6) Replace .values.addAll(X.values) and .values.addAll(values) -> add .toList().filterNotNull() to avoid passing Array where Collection expected and to ensure non-null element types
  perl -0777 -pi -e 's/(\.values\.addAll\(\s*)([A-Za-z0-9_.]+\s*\.values)(\s*\))/\1\2.toList().filterNotNull()\3/g' "$file" || true
  perl -0777 -pi -e 's/(\.values\.addAll\(\s*)(values)(\s*\))/\1\2.toList().filterNotNull()\3/g' "$file" || true

  # 6.1) Also handle patterns where a value list is passed directly (e.g., .values.addAll(some.values.toList()))
  perl -0777 -pi -e 's/([A-Za-z0-9_.]+\.values\.toList\(\))/\1.filterNotNull()/g' "$file" || true

  # 7) Handle specific Sequence->Array append pattern produced by generator in Navigation.kt
  # Replace: constructor(elements: Sequence<*>) { path.append(contentsOf = elements as Sequence<Any>) }
  # With: constructor(elements: Sequence<*>) { val _tmp = elements.toList(); path = path + _tmp.toTypedArray() }
  perl -0777 -pi -e 's/constructor\(elements:\s*Sequence<\*\>\)\s*\{\s*path\.append\(contentsOf\s*=\s*elements\s+as\s+Sequence<Any>\)\s*\}/constructor(elements: Sequence<*>) {\n        val _tmp = elements.toList()\n        path = path + _tmp.toTypedArray()\n    }/g' "$file" || true

  # 8) Fix malformed KClass parameter lists where generator omitted closing '>' in KClass<...> and continued with additional params
  # Example bad: KClass<Array<*>, elementType: KClass<E>, from: Input>
  # Fix to:    KClass<Array<*>>, elementType: KClass<E>, from: Input
  perl -0777 -pi -e 's/KClass<([^>]+?)\s*,\s*([a-zA-Z_][a-zA-Z0-9_]*:)/KClass<\1>>, \2/g' "$file" || true

  # 8.1) Additional targeted fixes for common nested patterns produced by generator
  perl -0777 -pi -e "s/KClass<Array<\*>\s*,\s*elementType:/KClass<Array<*>>, elementType:/g" "$file" || true
  perl -0777 -pi -e "s/KClass<Array<\*>\s*,\s*elementType:\s*KClass<Array<\*>\s*,\s*nestedElementType:/KClass<Array<*>>, elementType: KClass<Array<*>>, nestedElementType:/g" "$file" || true
  # Extra pass to fix patterns split across lines and whitespace/newlines
  perl -0777 -pi -e "s/elementType:\s*:\s*KClass<\s*Array<\*>\s*,\s*nestedElementType:/elementType: KClass<Array<*>>, nestedElementType:/g" "$file" || true
  perl -0777 -pi -e "s/elementType:\s*KClass<\s*Array<\*>\s*,\s*nestedElementType:/elementType: KClass<Array<*>>, nestedElementType:/g" "$file" || true
  perl -0777 -pi -e "s/valueType:\s*KClass<\s*Array<\*>\s*,\s*nestedElementType:/valueType: KClass<Array<*>>, nestedElementType:/g" "$file" || true

  # 8.2) Fix common malformed Binding<Array<*>? tokens where generator missed a '>' or an optionality marker
  # e.g., 'internal val path: Binding<Array<*>?' -> 'internal val path: Binding<Array<*>>?'
  perl -0777 -pi -e "s/Binding<\s*Array<\*>\s*\?/Binding<Array<*>>?/g" "$file" || true
  # also fix casts like '(path as Binding<Array<*>?)' -> '(path as Binding<Array<*>>?)'
  perl -0777 -pi -e "s/Binding<\s*Array<\*>\s*\?\)/Binding<Array<*>>\)/g" "$file" || true
  # and fix previously applied 'Binding<Array<*>?>?' back to 'Binding<Array<*>>?'
  perl -0777 -pi -e "s/Binding<Array<\*>\?>\?/Binding<Array<*>>?/g" "$file" || true
  perl -0777 -pi -e "s/KClass<Dictionary<\s*\*,\s*\*>\s*,\s*keyType:/KClass<Dictionary<*, *>>, keyType:/g" "$file" || true

  # 9) Convert Swift half-open ranges 'start..<end' into Kotlin 'start until end'
  # Use a simpler and more reliable pattern matching the '..<' operator and the following expression
  perl -0777 -pi -e 's/([0-9A-Za-z_\)\]]+)\s*\.\.<\s*([^\s\)\}]+)/\1 until \2/g' "$file" || true

  # 10) Normalize some common mistaken generic tokens (safe heuristics)
  # e.g., replace 'List<' with 'kotlin.collections.List<' if unresolved in some contexts (optional)
  # perl -pi -e 's/\bList</kotlin.collections.List</g' "$file" || true

  # 10.1) If the generator produced a star-projected cast for localizedRecoveryOptions (as? Array<*>),
  #         adjust the declared return type to Array<*>? to avoid a return-type mismatch.
  perl -0777 -pi -e 's/open val localizedRecoveryOptions:\s*Array<String>\?\s*\n\s*get\(\)\s*=\s*userInfo\[NSLocalizedRecoveryOptionsErrorKey\]\s*as\?\s*Array<\*>/open val localizedRecoveryOptions: Array<*>?\n        get() = userInfo[NSLocalizedRecoveryOptionsErrorKey] as? Array<*>/g' "$file" || true

  # 6) Add import hints for common Compose symbols if not present (won't override existing imports)
  # Insert after package declaration
  if ! grep -q "androidx.compose" "$file" ; then
    awk 'NR==1{print;next} /^package /{print;print "\nimport androidx.compose.runtime.*\nimport androidx.compose.material3.*\nimport androidx.compose.foundation.layout.*\n";next}1' "$file" > "$file.tmp" && mv "$file.tmp" "$file"
  fi

  changed=1

done

if [ "$changed" -eq 1 ]; then
  echo "Sanitizer applied changes. Backup of originals at $TMP_BACKUP"
else
  echo "Sanitizer found no modifications necessary. Backup at $TMP_BACKUP"
fi

echo "Sanitizer finished. Run 'cd KickbaseCore && ./gradlew :compileKotlin' to test compilation."
