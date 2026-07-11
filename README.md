# Davids-cloud

Cloud of Ideas — the home base for launching and running a YouTube channel.

## What's here

| Folder | What it's for |
|--------|---------------|
| [`docs/`](./docs) | Channel planning, content calendar, and a launch checklist. |
| [`automation/`](./automation) | A Python script to upload videos via the YouTube Data API. |
| [`website/`](./website) | A landing page to promote the channel and embed videos. |

## Can we create a YouTube channel?

The channel itself has to be created by you, signing into your Google account on
[youtube.com](https://www.youtube.com) — that part can't be automated. This repo
gives you everything around it:

1. **Plan it** — start with [`docs/channel-plan.md`](./docs/channel-plan.md).
2. **Launch it** — follow [`docs/launch-checklist.md`](./docs/launch-checklist.md)
   step by step (Phase 1 is the actual channel creation).
3. **Schedule content** — track videos in
   [`docs/content-calendar.md`](./docs/content-calendar.md).
4. **Automate uploads** — set up [`automation/`](./automation) when you're ready to
   publish programmatically.
5. **Promote it** — open [`website/index.html`](./website/index.html) in a browser
   and customize it.

## Quick start

```bash
# Preview the landing page
open website/index.html        # macOS
xdg-open website/index.html    # Linux

# Set up upload automation (see automation/README.md for full steps)
cd automation
pip install -r requirements.txt
```

## Launch checklist (short version)

- [ ] Create the channel on YouTube (a Brand Account is recommended).
- [ ] Verify your account for custom thumbnails & longer videos.
- [ ] Add branding: profile picture, banner, description, links.
- [ ] Film 3–5 videos before launch.
- [ ] Publish a channel trailer and your first video.

Full details in [`docs/launch-checklist.md`](./docs/launch-checklist.md).
