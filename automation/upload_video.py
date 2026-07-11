#!/usr/bin/env python3
"""Upload a video to YouTube using the YouTube Data API v3.

This automates video uploads with title, description, tags, category, privacy,
and (optionally) a custom thumbnail and playlist assignment.

Setup
-----
1. Create a project at https://console.cloud.google.com and enable the
   "YouTube Data API v3".
2. Create OAuth 2.0 credentials of type "Desktop app" and download the JSON as
   `client_secret.json` next to this script.
3. Install dependencies:
       pip install -r requirements.txt
4. Run it (a browser opens the first time to authorize; a token is cached in
   `token.json` afterwards):
       python upload_video.py --file video.mp4 --title "My Video" \
           --description "Hello" --tags "cloud,tutorial" --privacy private

See ./README.md for more detail.
"""

import argparse
import os
import sys

import google.auth.transport.requests
import google_auth_oauthlib.flow
from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
from googleapiclient.http import MediaFileUpload

# Full read/write access is needed to upload and set thumbnails.
SCOPES = ["https://www.googleapis.com/auth/youtube"]

HERE = os.path.dirname(os.path.abspath(__file__))
CLIENT_SECRET_FILE = os.path.join(HERE, "client_secret.json")
TOKEN_FILE = os.path.join(HERE, "token.json")


def get_authenticated_service():
    """Return an authorized YouTube API client, caching the token locally."""
    creds = None
    if os.path.exists(TOKEN_FILE):
        creds = Credentials.from_authorized_user_file(TOKEN_FILE, SCOPES)

    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            creds.refresh(google.auth.transport.requests.Request())
        else:
            if not os.path.exists(CLIENT_SECRET_FILE):
                sys.exit(
                    f"Missing {CLIENT_SECRET_FILE}. Download OAuth 'Desktop app' "
                    "credentials from Google Cloud Console and save them there."
                )
            flow = google_auth_oauthlib.flow.InstalledAppFlow.from_client_secrets_file(
                CLIENT_SECRET_FILE, SCOPES
            )
            creds = flow.run_local_server(port=0)
        with open(TOKEN_FILE, "w") as token:
            token.write(creds.to_json())

    return build("youtube", "v3", credentials=creds)


def upload_video(youtube, args):
    """Upload the video file and return the created video's ID."""
    tags = [t.strip() for t in args.tags.split(",")] if args.tags else []

    body = {
        "snippet": {
            "title": args.title,
            "description": args.description,
            "tags": tags,
            "categoryId": args.category,
        },
        "status": {
            "privacyStatus": args.privacy,
            "selfDeclaredMadeForKids": args.made_for_kids,
        },
    }

    media = MediaFileUpload(args.file, chunksize=-1, resumable=True)
    request = youtube.videos().insert(
        part="snippet,status", body=body, media_body=media
    )

    print(f"Uploading '{args.file}'...")
    response = None
    while response is None:
        status, response = request.next_chunk()
        if status:
            print(f"  {int(status.progress() * 100)}% uploaded")

    video_id = response["id"]
    print(f"Done. Video ID: {video_id}")
    print(f"Watch: https://youtu.be/{video_id}")
    return video_id


def set_thumbnail(youtube, video_id, thumbnail_path):
    """Attach a custom thumbnail (requires a verified account)."""
    print(f"Setting thumbnail '{thumbnail_path}'...")
    youtube.thumbnails().set(
        videoId=video_id, media_body=MediaFileUpload(thumbnail_path)
    ).execute()
    print("Thumbnail set.")


def add_to_playlist(youtube, video_id, playlist_id):
    """Add the uploaded video to an existing playlist."""
    print(f"Adding to playlist '{playlist_id}'...")
    youtube.playlistItems().insert(
        part="snippet",
        body={
            "snippet": {
                "playlistId": playlist_id,
                "resourceId": {"kind": "youtube#video", "videoId": video_id},
            }
        },
    ).execute()
    print("Added to playlist.")


def parse_args():
    p = argparse.ArgumentParser(description="Upload a video to YouTube.")
    p.add_argument("--file", required=True, help="Path to the video file.")
    p.add_argument("--title", required=True, help="Video title.")
    p.add_argument("--description", default="", help="Video description.")
    p.add_argument("--tags", default="", help="Comma-separated tags.")
    p.add_argument(
        "--category",
        default="22",
        help="Numeric category ID (default 22 = People & Blogs; 27 = Education, "
        "28 = Science & Tech, 20 = Gaming).",
    )
    p.add_argument(
        "--privacy",
        default="private",
        choices=["private", "unlisted", "public"],
        help="Privacy status (default: private).",
    )
    p.add_argument(
        "--made-for-kids",
        action="store_true",
        help="Mark the video as made for kids (COPPA).",
    )
    p.add_argument("--thumbnail", help="Path to a custom thumbnail image.")
    p.add_argument("--playlist-id", help="Add the video to this playlist ID.")
    return p.parse_args()


def main():
    args = parse_args()
    if not os.path.exists(args.file):
        sys.exit(f"Video file not found: {args.file}")

    youtube = get_authenticated_service()
    try:
        video_id = upload_video(youtube, args)
        if args.thumbnail:
            set_thumbnail(youtube, video_id, args.thumbnail)
        if args.playlist_id:
            add_to_playlist(youtube, video_id, args.playlist_id)
    except HttpError as e:
        sys.exit(f"YouTube API error: {e}")


if __name__ == "__main__":
    main()
