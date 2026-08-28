#!/bin/bash
awk '
/id = v.id/ {
    print
    getline
    print
    print "                }"
    next
}
{ print }
' app/src/main/java/com/example/ui/AppViewModel.kt > temp2.kt && mv temp2.kt app/src/main/java/com/example/ui/AppViewModel.kt
