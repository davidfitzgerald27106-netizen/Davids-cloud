# Upload Automation

A small Python script that uploads videos to YouTube via the **YouTube Data API v3**,
with title, description, tags, category, privacy, custom thumbnail, and playlist
assignment.

## One-time setup

1. **Create a Google Cloud project**
   - Go to the [Google Cloud Console](https://console.cloud.google.com).
   - Create a new project (or reuse one).

2. **Enable the API**
   - APIs & Services → Library → search **"YouTube Data API v3"** → **Enable**.

3. **Configure the OAuth consent screen**
   - APIs & Services → OAuth consent screen.
   - User type: **External**. Fill in the required app name / email.
   - Add your Google account under **Test users** (so you can authorize while the
     app is in "testing" mode).

4. **Create credentials**
   - APIs & Services → Credentials → **Create credentials** → **OAuth client ID**.
   - Application type: **Desktop app**.
   - Download the JSON and save it as `client_secret.json` in this folder.

5. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

## Usage

```bash
python upload_video.py \
  --file video.mp4 \
  --title "My First Video" \
  --description "Welcome to the channel!" \
  --tags "cloud,tutorial,intro" \
  --category 27 \
  --privacy private \
  --thumbnail thumb.jpg \
  --playlist-id PLxxxxxxxx
```

The **first run** opens a browser window to authorize access. After that, a
`token.json` file is cached locally so you won't be prompted again.

### Flags

| Flag | Required | Default | Description |
|------|----------|---------|-------------|
| `--file` | yes | — | Path to the video file. |
| `--title` | yes | — | Video title. |
| `--description` | no | `""` | Video description. |
| `--tags` | no | `""` | Comma-separated tags. |
| `--category` | no | `22` | Category ID (22=People&Blogs, 27=Education, 28=Science&Tech, 20=Gaming). |
| `--privacy` | no | `private` | `private`, `unlisted`, or `public`. |
| `--made-for-kids` | no | off | Mark as made for kids (COPPA). |
| `--thumbnail` | no | — | Custom thumbnail image (requires a verified account). |
| `--playlist-id` | no | — | Add the video to this playlist. |

## Quota note

The YouTube Data API has a default quota of **10,000 units/day**, and a single
video upload costs **1,600 units** — roughly **6 uploads/day** before you need to
request more quota. Plan batch uploads accordingly.

## Security

Never commit `client_secret.json` or `token.json` — they authorize access to your
channel. They're already covered by the repo's [`.gitignore`](../.gitignore).
