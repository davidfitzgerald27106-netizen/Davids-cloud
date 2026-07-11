# Publish-Ready Metadata

Copy-paste titles, descriptions, and tags for the first-month videos. Once a video is
filmed and exported, publishing is one step — either paste these into YouTube Studio,
or run the [`automation/upload_video.py`](../automation/upload_video.py) command shown
under each entry.

**Shared footer** (append to every description):

```
🔔 Subscribe for weekly reflections: https://www.youtube.com/@DavidFitzgerald1988

Davids Cloud — soul awakening, with positivity.

#SoulAwakening #Positivity #SpiritualAwakening
```

Category ID for all of these: **22** (People & Blogs). Privacy: start **unlisted** to
review, then flip to **public** when you're happy.

---

## Video 1 — What Is Soul Awakening? A Gentle Beginning
📄 Script: [`scripts/01-what-is-soul-awakening.md`](./scripts/01-what-is-soul-awakening.md)

**Title:** `What Is Soul Awakening? A Gentle Beginning`

**Description:**
```
Have you ever felt there's more to who you are? That quiet pull is your soul waking
up. In this gentle beginning, we explore what soul awakening really is, how it starts
with simply listening to your intuition, and how it naturally turns you toward a more
positive headspace. If you're at the very start of this path, you belong here.

⏱️ Chapters
0:00 There's more than this
1:00 What awakening actually is
2:30 The turn toward positivity
4:00 Gently rejecting what doesn't serve you
5:15 Your first small step
```

**Tags:** `soul awakening, spiritual awakening, awakening for beginners, positivity, positive mindset, intuition, spiritual growth, inner peace, self discovery, spiritual journey`

**One-command publish:**
```bash
python automation/upload_video.py --file video1.mp4 \
  --title "What Is Soul Awakening? A Gentle Beginning" \
  --description "$(sed -n '/^Have you/,/first small step/p' docs/publish-metadata.md)" \
  --tags "soul awakening,spiritual awakening,positivity,intuition,inner peace" \
  --category 22 --privacy unlisted --thumbnail thumb1.jpg
```

---

## Video 2 — 5-Minute Morning Reset Into a Positive Headspace
📄 Script: [`scripts/02-5-minute-positive-reset.md`](./scripts/02-5-minute-positive-reset.md)

**Title:** `5-Minute Morning Reset Into a Positive Headspace`

**Description:**
```
The first five minutes of your morning set the tone for everything after. This simple,
repeatable practice — breathe, give thanks, set your intention, release — helps you
choose your headspace instead of letting the day choose it for you. No app, no cost.
Just you and your breath. Do it with me, then keep it forever.

⏱️ Chapters
0:00 Give five minutes to your soul
0:50 Three slow breaths
1:50 One thing you're grateful for
3:00 Choose your word for the day
4:00 Release one worry
```

**Tags:** `positive mindset, morning routine, morning meditation, gratitude, positivity, mindset reset, calm, breathing exercise, daily affirmation, mental wellness`

**Publish:** same command as Video 1, swapping `--file video2.mp4`, the title, tags,
and `--thumbnail thumb2.jpg`.

---

## Video 3 — Trusting the Gut Feeling That Says "No"
📄 Script: [`scripts/03-trusting-the-gut-feeling.md`](./scripts/03-trusting-the-gut-feeling.md)

**Title:** `Trusting the Gut Feeling That Says "No"`

**Description:**
```
That quiet "no" you sometimes feel isn't paranoia — it's information. Part of soul
awakening is discernment: learning to feel the difference between what feeds your
spirit and what quietly harms it, and gently turning away — without fear. A simple
three-step practice: pause, name it, choose.

⏱️ Chapters
0:00 The quiet "no"
1:00 Your intuition is data
2:30 Why we override it
3:45 How to honor it: pause, name, choose
5:15 Discernment without fear
```

**Tags:** `intuition, trusting your intuition, discernment, gut feeling, spiritual protection, boundaries, inner guidance, spiritual awakening, self trust, positivity`

**Publish:** same command, `--file video3.mp4`, title, tags, `--thumbnail thumb3.jpg`.

---

## Video 4 — The Greatest Love: Learning to Love Your Own Soul
📄 Script: [`scripts/04-the-greatest-love.md`](./scripts/04-the-greatest-love.md)

**Title:** `The Greatest Love: Learning to Love Your Own Soul`

**Description:**
```
The greatest love of all really is learning to love yourself — not as vanity, but as
foundation. Your soul has been with you through everything, waiting for you to be kind
to it. This reflection is about self-love as the ground your whole awakening stands
on, and small, honest ways to begin.

⏱️ Chapters
0:00 The greatest love
1:00 The love you've been withholding
2:30 Why self-love is the foundation
4:00 How to begin
5:15 The greatest love is you
```

**Tags:** `self love, loving yourself, self worth, soul, positivity, healing, spiritual awakening, self compassion, inner peace, greatest love`

> ⚠️ **Music/copyright:** this video is inspired by the *spirit* of Whitney Houston's
> message — do not use her recording or lyrics. Use a licensed/royalty-free track.

**Publish:** same command, `--file video4.mp4`, title, tags, `--thumbnail thumb4.jpg`.

---

## Reminder: what you need before the publish command works
1. The channel created + verified on YouTube.
2. Google Cloud OAuth credentials saved as `automation/client_secret.json`
   (see [`automation/README.md`](../automation/README.md)).
3. The exported video file(s) and thumbnail image(s).

Then the first run opens a browser to authorize, and every upload after is one command.
