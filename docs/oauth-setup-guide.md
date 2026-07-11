# Google OAuth Setup — Step by Step

A click-by-click guide to authorizing the [upload script](../automation/upload_video.py)
to post to **your** YouTube channel. Everything here happens on **your own computer**
and in **your own browser** — your credentials never leave your machine, and no one
else (including any AI assistant) ever handles them.

⏱️ **Time:** ~10–15 minutes, one time only.
🔑 **What you'll end up with:** a `client_secret.json` file and a cached `token.json`,
both stored locally and both gitignored so they're never committed.

---

## Before you start

You'll need these on your own machine:

- [ ] **The repo cloned locally.**
  ```bash
  git clone https://github.com/davidfitzgerald27106-netizen/Davids-cloud.git
  cd Davids-cloud
  ```
- [ ] **Python 3.8+** installed. Check with `python --version` (or `python3 --version`).
- [ ] **The dependencies installed:**
  ```bash
  pip install -r automation/requirements.txt
  ```
- [ ] **Your YouTube channel already created** and signed in with the Google account
  you want to upload from (`@DavidFitzgerald1988`).

> Use the **same Google account** for the steps below that owns the channel.

---

## Step 1 — Create a Google Cloud project

1. Go to **https://console.cloud.google.com** and sign in with your channel's Google
   account.
2. At the top of the page, click the **project dropdown** (it may say "Select a
   project").
3. Click **New Project**.
4. Name it something memorable, e.g. `davids-cloud-uploads`. Leave "Organization" as
   is. Click **Create**.
5. Wait a few seconds, then make sure the new project is **selected** in that top
   dropdown before continuing.

---

## Step 2 — Enable the YouTube Data API v3

1. In the search bar at the top, type **YouTube Data API v3** and select it from the
   results (under "Marketplace" / APIs).
2. Click the blue **Enable** button.
3. Wait for it to finish — you'll land on the API's overview page.

---

## Step 3 — Configure the consent screen

This is the screen you'll see when you authorize. Google recently reorganized this
area under **APIs & Services → OAuth consent screen** (newer accounts may see it
branded as **Google Auth Platform** with tabs like *Branding*, *Audience*, *Clients*).
The steps are the same either way.

1. Go to **APIs & Services → OAuth consent screen** (left sidebar, or search it).
2. If prompted to choose a **User Type**, select **External**, then **Create**.
   *(External just means "a normal Google account," which is what you have. It does
   not make anything public.)*
3. Fill in the required fields:
   - **App name:** `Davids Cloud Uploader` (anything you like)
   - **User support email:** your email
   - **Developer contact email** (at the bottom): your email
   - Everything else can be left blank.
4. Click **Save and Continue** through the **Scopes** step (you don't need to add
   scopes here — the script requests them at runtime). Save and Continue again.
5. **Test users** (important!): click **Add Users** and add **your own Google email
   address** — the same account that owns the channel. **Save and Continue.**

   > While your app is in "Testing" mode, **only the emails listed as test users can
   > authorize it.** This is why you must add yourself here, or you'll get an
   > `access_denied` error later.

6. You do **not** need to publish the app or submit it for verification. Testing mode
   is fine for uploading to your own channel indefinitely.

---

## Step 4 — Create OAuth credentials (Desktop app)

1. Go to **APIs & Services → Credentials** (or the **Clients** tab).
2. Click **+ Create Credentials → OAuth client ID**.
3. **Application type:** choose **Desktop app**.
   *(This is the key choice — Desktop app enables the local-browser flow the script
   uses. Do not pick "Web application.")*
4. Name it `davids-cloud-desktop` (anything).
5. Click **Create**. A dialog appears with your client ID — click **Download JSON**.

---

## Step 5 — Place the credentials file

1. Rename the downloaded file to exactly **`client_secret.json`**.
2. Move it into the **`automation/`** folder of the repo, so the path is:
   ```
   Davids-cloud/automation/client_secret.json
   ```
3. **Confirm it won't be committed** (it's already gitignored):
   ```bash
   git status        # client_secret.json should NOT appear in the list
   git check-ignore automation/client_secret.json   # should print the path = ignored
   ```

   > 🔒 Never commit or share this file. It authorizes access to your channel.

---

## Step 6 — Authorize (the first run)

Now do a real (or test) upload. The **first** time, a browser window opens asking you
to sign in and approve.

```bash
cd Davids-cloud
python automation/upload_video.py \
  --file yourvideo.mp4 \
  --title "Test upload — please ignore" \
  --privacy private
```

What happens:

1. Your browser opens to a Google sign-in page. **Choose your channel's account.**
2. You'll likely see a **"Google hasn't verified this app"** warning — this is
   expected for a personal app in testing mode. Click **Advanced → Go to Davids Cloud
   Uploader (unsafe)**. *(It's "unsafe" only in the sense that Google hasn't reviewed
   it — it's your own app.)*
3. Approve the requested YouTube permission.
4. The browser shows "The authentication flow has completed." You can close it.
5. Back in the terminal, the upload runs, and a **`token.json`** is saved in
   `automation/` so you won't be asked again.

> 💡 Tip: for a first test, either use a throwaway clip or set `--privacy private` so
> nothing goes public while you're confirming it works.

---

## Step 7 — You're set — publish for real

From now on, every upload is a single command (no sign-in). Grab the ready-made
titles, descriptions, and tags from
[`publish-metadata.md`](./publish-metadata.md), e.g.:

```bash
python automation/upload_video.py \
  --file video1.mp4 \
  --title "What Is Soul Awakening? A Gentle Beginning" \
  --description "…(paste from publish-metadata.md)…" \
  --tags "soul awakening,spiritual awakening,positivity,intuition,inner peace" \
  --category 22 --privacy unlisted --thumbnail thumb1.jpg
```

Start with `--privacy unlisted` to review it on YouTube, then flip to public in
Studio (or upload with `--privacy public` when you're confident).

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| **`access_denied` / "app is in testing"** | Add your Google email under **OAuth consent screen → Test users** (Step 3.5). |
| **"Google hasn't verified this app"** | Expected. **Advanced → Go to … (unsafe)**. It's your own app in testing mode. |
| **`Missing client_secret.json`** | The file isn't in `automation/` or isn't named exactly `client_secret.json` (Step 5). |
| **`invalid_grant` / token errors** | Delete `automation/token.json` and run again to re-authorize. |
| **`quotaExceeded`** | Each upload costs 1,600 of 10,000 daily units (~6/day). Wait for the daily reset or request more quota in the Cloud Console. |
| **`redirect_uri_mismatch`** | You created a "Web application" client instead of **Desktop app**. Recreate it as Desktop app (Step 4.3). |
| **Wrong channel uploaded to** | You authorized with a different Google account. Delete `token.json` and re-run, choosing the correct account. |

---

## Security recap

- `client_secret.json` and `token.json` live **only on your machine** and are
  **gitignored** — they're never pushed to GitHub.
- No one else needs them, and you should never paste them into a chat, email, or
  share them. If you ever think one leaked, revoke it in the Cloud Console
  (Credentials → delete the client) and in your
  [Google Account permissions](https://myaccount.google.com/permissions).
