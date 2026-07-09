#!/bin/bash
# Move package declaration to line 1
sed -i '/package com.example.keyboard/d' app/src/main/java/com/example/keyboard/KeyboardComponents.kt
sed -i '1s/^/package com.example.keyboard\n/' app/src/main/java/com/example/keyboard/KeyboardComponents.kt

# Remove duplicate @Composable
sed -i 's/@Composable@Composable/@Composable/g' app/src/main/java/com/example/keyboard/KeyboardComponents.kt

# The emoji overlay ends with }}} but should only end with }}
# Let's fix this using python for better safety
python3 -c "
with open('app/src/main/java/com/example/keyboard/KeyboardComponents.kt', 'r') as f:
    text = f.read()

# Fix EmojiOverlay extra brace
text = text.replace('    }\n}\n}\n@Composable\nfun ClipboardOverlay', '    }\n}\n@Composable\nfun ClipboardOverlay')

with open('app/src/main/java/com/example/keyboard/KeyboardComponents.kt', 'w') as f:
    f.write(text)
"
