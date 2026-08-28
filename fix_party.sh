#!/bin/bash
sed -i '/val entity = PartyEntity(/,/)/ s/panVatNumber = pan/pan = pan/' app/src/main/java/com/example/data/firebase/FirebaseSyncManager.kt
