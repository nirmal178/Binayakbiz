#!/bin/bash
awk '
/Row.*horizontalArrangement = Arrangement.spacedBy/ {
    getline nextLine
    if (nextLine ~ /province/) {
        getline nextLine2 # district
        getline nextLine3 # }
        next
    } else {
        print
        print nextLine
        next
    }
}
{ print }
' app/src/main/java/com/example/ui/screens/AuthScreen.kt > temp_auth.kt && mv temp_auth.kt app/src/main/java/com/example/ui/screens/AuthScreen.kt
