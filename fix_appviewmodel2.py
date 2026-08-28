with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    lines = f.readlines()

fixes = {
    217: "                    }\n",
    220: "                }\n",
    223: "            }\n",
    246: "                }\n",
    287: "            }\n",
    290: "        }\n",
    319: "                }\n",
    330: "                    )\n                )\n",
    332: "            }\n",
    335: "        }\n",
    386: "                }\n",
    406: "                    )\n                )\n",
    408: "            }\n",
    411: "        }\n",
    479: "            }\n",
    486: "        }\n",
    800: "}\n"
}

# The problem is that when we insert, indices shift! So we insert from bottom to top.
for k in sorted(fixes.keys(), reverse=True):
    # the dictionary keys are 1-based indices from the file BEFORE our insertions.
    # We will insert AT the end of the line (i.e. between k and k+1)
    # wait, line 217 in cat output is lines[216].
    # So we want to insert AFTER lines[216], which means at index 217.
    idx = k
    lines.insert(idx, fixes[k])

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.writelines(lines)
print("Fixes applied.")
