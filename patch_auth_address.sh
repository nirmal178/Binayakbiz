#!/bin/bash
sed -i 's/import androidx.compose.ui.unit.dp/import androidx.compose.ui.unit.dp\nimport com.example.ui.components.AddressPicker/g' app/src/main/java/com/example/ui/screens/AuthScreen.kt

# Use awk to replace the fields
awk '
/OutlinedTextField\(value = address/ {
    print "                                Text("
    print "                                    text = \"Address (District, Municipality, Ward)\","
    print "                                    style = MaterialTheme.typography.labelMedium,"
    print "                                    color = MaterialTheme.colorScheme.onSurfaceVariant"
    print "                                )"
    print "                                AddressPicker("
    print "                                    selectedAddress = address,"
    print "                                    onAddressChange = { address = it }"
    print "                                )"
    skip = 1
    next
}
/Row.*province/ && skip {
    skip = 2
    next
}
/OutlinedTextField.*province/ && skip == 2 {
    next
}
/OutlinedTextField.*district/ && skip == 2 {
    next
}
/}/ && skip == 2 {
    skip = 0
    next
}
{ print }
' app/src/main/java/com/example/ui/screens/AuthScreen.kt > temp_auth.kt && mv temp_auth.kt app/src/main/java/com/example/ui/screens/AuthScreen.kt
