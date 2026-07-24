# Deployment

The backend and frontend deploy as two separate pieces - a Docker web service and a static
site - not bundled together. This guide targets **Render**, chosen over Railway because Render
has a genuine free tier for both a web service and a short-lived free Postgres instance, with no
card required to start (Railway removed its free tier in 2023; it's now a $5/mo minimum, a
rougher on-ramp for a first deployment).

`render.yaml` at the repo root is a Render "Blueprint" - point Render at the repo and it
provisions the database + web service from that one file. Render's Blueprint schema can shift
over time, so if something in the dashboard doesn't match this doc, trust the dashboard and
update the file to match.

## 1. Push to GitHub

Render deploys from a GitHub repo. If this repo isn't already pushed, that's step zero.

## 2. Deploy the backend via Blueprint

1. Render dashboard → **New** → **Blueprint** → connect this repo. Render reads `render.yaml`
   and proposes two resources: a Postgres database (`microloan-db`) and a Docker web service
   (`microloan-platform`).
2. Apply the blueprint. Render provisions both; the web service will fail its first health
   check until step 3 is done (no database credentials yet) - that's expected.
3. **Manually wire the database credentials.** Render's managed Postgres exposes its own
   connection string format, not the JDBC URL this app expects, and Blueprints can't reformat
   one into the other - so this step is manual: open `microloan-db` → **Connect**, and note the
   host, port, database name, username, and password. Then, on the `microloan-platform` service
   → **Environment**, set:
   - `DB_URL` = `jdbc:postgresql://<host>:<port>/<database>`
   - `DB_USERNAME` = the username from the connection panel
   - `DB_PASSWORD` = the password from the connection panel
   - `CORS_ALLOWED_ORIGINS` = a placeholder for now (e.g. `http://localhost:5173`) - revisit
     this in step 4 once the frontend has a real URL.

   `JWT_SECRET` was already generated automatically by the blueprint (`generateValue: true`) -
   nothing to do there.
4. Save. Render redeploys; `/actuator/health` should now return 200 and the service goes green.

## 3. Deploy the frontend as a Static Site

1. Render dashboard → **New** → **Static Site** → same repo.
2. **Root directory**: `frontend`
3. **Build command**: `npm install && npm run build`
4. **Publish directory**: `dist`
5. **Environment**: `VITE_API_BASE_URL` = the backend's Render URL from step 2 (e.g.
   `https://microloan-platform.onrender.com`).

   Vite bakes environment variables into the build at build time, not at runtime - if the
   backend URL ever changes, the static site needs a fresh deploy, not just an env var edit.

## 4. Wire CORS to the real frontend URL

Back on `microloan-platform`'s **Environment** tab, set `CORS_ALLOWED_ORIGINS` to the static
site's actual Render URL (comma-separate if you need more than one origin) and save. Render
restarts the service with the new value.

## Before recording a demo

Render's free tier spins a web service down after a period of inactivity; the first request
after that can take up to ~50 seconds while it wakes back up. Hit the backend once (e.g.
`GET /actuator/health`) a minute before recording so the demo doesn't open on a long, confusing
pause.

## Railway, briefly

Railway can run the same `Dockerfile` directly (no blueprint needed - add a Postgres plugin,
point a service at the Dockerfile, wire the same environment variables by hand). The reason this
guide leads with Render is purely the free tier; the app itself has no Render-specific
dependency.
