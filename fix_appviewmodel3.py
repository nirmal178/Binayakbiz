import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

# Block 1
bad1 = """                    if (user != null && user.passwordHash == cleanPassword) {
                        userPrefs.setLoggedInSession(company.id, user.id)
                        _authError.value = null
                    }
                    } else {
                        _authError.value = "Incorrect username or password."
    
                }
 else {
                    _authError.value = "Company with this PAN not found. Switch to Sign Up."

            }
            } catch (e: Exception) {"""
good1 = """                    if (user != null && user.passwordHash == cleanPassword) {
                        userPrefs.setLoggedInSession(company.id, user.id)
                        _authError.value = null
                    } else {
                        _authError.value = "Incorrect username or password."
                    }
                } else {
                    _authError.value = "Company with this PAN not found. Switch to Sign Up."
                }
            } catch (e: Exception) {"""
content = content.replace(bad1, good1)

# Block 2
bad2 = """                userPrefs.setLoggedInSession(newCompany.id, adminUser.id)
                _authError.value = null
            }
            } catch (e: Exception) {
                _authError.value = e.message ?: "Registration failed."
            }
        }
        }
    }"""
good2 = """                userPrefs.setLoggedInSession(newCompany.id, adminUser.id)
                _authError.value = null
            } catch (e: Exception) {
                _authError.value = e.message ?: "Registration failed."
            }
        }
    }"""
content = content.replace(bad2, good2)

# Block 3 (addParty)
bad3 = """                    )
                )
                )
                onComplete()
            }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save ${type.name.lowercase()}.")
            }
        }
        }
    }"""
good3 = """                    )
                )
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save ${type.name.lowercase()}.")
            }
        }
    }"""
content = content.replace(bad3, good3)

# Block 4 (addItem)
bad4 = """                    )
                )
                )
                onComplete()
            }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to add item.")
            }
        }
        }
    }"""
good4 = """                    )
                )
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to add item.")
            }
        }
    }"""
content = content.replace(bad4, good4)

# Block 5 (getOrCreateCashParty)
bad5 = """                try {
                    repository.addParty(newParty)
            }
                    onReady(newParty)
 catch (e: Exception) {
                    // In case of race condition or conflict, find any
                    val fallback = (if (type == PartyType.CUSTOMER) customers.value else suppliers.value).firstOrNull()
                    if (fallback != null) onReady(fallback)

            }"""
good5 = """                try {
                    repository.addParty(newParty)
                    onReady(newParty)
                } catch (e: Exception) {
                    // In case of race condition or conflict, find any
                    val fallback = (if (type == PartyType.CUSTOMER) customers.value else suppliers.value).firstOrNull()
                    if (fallback != null) onReady(fallback)
                }"""
content = content.replace(bad5, good5)

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.write(content)

