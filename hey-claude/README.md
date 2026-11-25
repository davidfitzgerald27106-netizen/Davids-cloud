# Hey Claude

A small personal Android app. Say "Hey Claude" and it opens the Claude app.

This is an unofficial personal utility. It is not made by, endorsed by, or
affiliated with Anthropic.

## What it does

- Listens for the phrase on the phone itself. Audio is not recorded or sent anywhere.
- Opens the Claude app when it hears the phrase.
- Only analyses audio when there is sound above the room's background level.
- Steps aside while another app is using the microphone.
- Shows what it heard and how sure it was, with a slider for how sure it must be.
- Shows a permanent notification while it is listening, with a Stop button.

## What it does not do

- It cannot unlock the phone. From a locked screen it only wakes the display.
- It does not start Claude's voice mode unless you switch on the optional
  Accessibility permission. With it on, the app taps the voice button for you,
  only in the seconds after the wake phrase, and only inside the Claude app.
- It does not restart by itself after the phone reboots. It posts a reminder; one tap turns it back on.

## Install

1. Open the Releases page of this repository on the phone and download `hey-claude.apk`.
2. Allow the install when Android asks. On Samsung phones, Auto Blocker has to be
   off for the install (Settings, Security and privacy, Auto Blocker). It can be
   turned back on afterwards.
3. Open the app and work down the Setup list until every line says done.
4. Tap "Turn listening on".

Unless a signing key has been added to the repository's secrets, each build is
signed with a throwaway key, so to install a newer build, uninstall the old one first.

## How it is built

Pushing to this folder runs `.github/workflows/hey-claude.yml`, which downloads
the offline speech model, builds the app and publishes the install file as a release.

Speech recognition uses the open source Vosk library and its small English model.
