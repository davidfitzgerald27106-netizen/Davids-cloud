# Bremskerl Material Selection Tool — Mobile App (PWA)

The material selection tool packaged as an installable app for **Android and iPhone**.
It is a Progressive Web App (PWA): once it's hosted at a web address, anyone can add
it to their phone's home screen where it gets its own icon, opens full-screen like a
native app, and **works completely offline** — no app store, no developer accounts,
no per-device installs to manage.

## Step 1 — Put it online (one time, ~2 minutes)

The app needs a web address before phones can install it. GitHub Pages hosts it free
from this repository:

1. Merge this branch into `main`.
2. On GitHub, open **Settings → Pages** for this repository.
3. Under **Build and deployment**, set Source to **Deploy from a branch**, pick
   branch **`main`** and folder **`/ (root)`**, and save.
4. After a minute or two the tool is live at:

   `https://davidfitzgerald27106-netizen.github.io/Davids-cloud/material-selector/`

(Alternatively, the folder can be copied to any web host — e.g.
`bremskerl.com/tools/material-selector/` — the app has no server requirements at all.)

## Step 2 — Install it on a phone

Share the address above with the team (a QR code works well). Then:

**iPhone / iPad** — open the address in **Safari**, tap the **Share** button,
then **Add to Home Screen**. (The app shows this tip automatically the first time.)

**Android** — open the address in **Chrome** and tap **Install** when the banner
appears (or menu ⋮ → *Add to Home screen*).

That's it. The Bremskerl icon appears on the home screen and the app works from
then on even with no signal — useful on plant floors.

## What's in this folder

| File | Purpose |
|------|---------|
| `index.html` | The entire tool — logic, catalog data, and styles in one file |
| `manifest.webmanifest` | Tells phones the app's name, icon, and colors for install |
| `sw.js` | Service worker — caches the app so it loads instantly and works offline |
| `icons/` | App icons (Android, iOS, and maskable variants) |

## Updating the tool

Edit the `CATALOG` block near the top of `index.html`'s script section, then bump
`CACHE_VERSION` in `sw.js` (e.g. `v1` → `v2`) so installed phones fetch the new
version. Push to `main` and GitHub Pages redeploys automatically.

Material data: Bremskerl catalog Edition 8/15. Contact David Fitzgerald
(BKNA National Sales Manager) for content changes.
