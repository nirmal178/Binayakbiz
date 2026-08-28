with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    text = f.read()

open_b = text.count('{')
close_b = text.count('}')
print(f"Open: {open_b}, Close: {close_b}")
