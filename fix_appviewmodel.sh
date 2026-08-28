#!/bin/bash
sed -i 's/                    )//g' app/src/main/java/com/example/ui/AppViewModel.kt

# Wait, this is risky. Let's just fix it using awk.
