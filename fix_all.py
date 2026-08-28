import re

def fix_file():
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
        lines = f.readlines()
        
    out = []
    for line in lines:
        if "     else {" in line:
            out.append("                    } else {\n")
        elif "       " in line and "else {" not in line and line.strip() == "":
            pass # ignore
        elif " else {" in line and "}" not in line and not line.strip().startswith("else"):
            out.append("                } else {\n")
        elif "            } catch" in line and "}" not in line and not line.strip().startswith("}"):
            out.append(line.replace("            } catch", "            } catch")) # Wait
        else:
            out.append(line)
            
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
        f.writelines(out)

fix_file()
