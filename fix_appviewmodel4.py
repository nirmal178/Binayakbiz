import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

bad1 = """                        canViewReports = (role == "Business Owner")
                )
            } catch (e: Exception) {"""
good1 = """                        canViewReports = (role == "Business Owner")
                    )
                )
            } catch (e: Exception) {"""
content = content.replace(bad1, good1)

bad2 = """        throw IllegalArgumentException("Unknown ViewModel class")
}
    }
}"""
good2 = """        throw IllegalArgumentException("Unknown ViewModel class")
    }
}"""
content = content.replace(bad2, good2)

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.write(content)

