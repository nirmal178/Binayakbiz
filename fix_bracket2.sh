#!/bin/bash
sed -i 's/                }//g' app/src/main/java/com/example/ui/AppViewModel.kt
sed -i 's/                    )//g' app/src/main/java/com/example/ui/AppViewModel.kt
awk '
/id = v.id/ {
    print
    print "                        )"
    print "                    )"
    print "                }"
    next
}
{ print }
' app/src/main/java/com/example/ui/AppViewModel.kt > temp3.kt && mv temp3.kt app/src/main/java/com/example/ui/AppViewModel.kt
