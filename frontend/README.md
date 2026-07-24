# Microloan Platform - Frontend

A minimal React + Tailwind UI over the Spring Boot backend in the repo root. Two views: a
borrower flow (create a profile, complete KYC, apply for a loan, acknowledge the agreement,
make repayments) and an admin flow (approve/reject applications, watch the outbox, trigger the
overdue check). See the repo root `README.md` for the full architecture and setup instructions.

## Run locally

```bash
npm install
cp .env.example .env   # point VITE_API_BASE_URL at your running backend
npm run dev
```

## Build

```bash
npm run build
```

### Known issue on Windows

Vite 8's bundled Rolldown build occasionally fails to install its native binding
(`Cannot find native binding` / `@rolldown/binding-win32-x64-msvc`) - a known npm optional-
dependency bug ([npm/cli#4828](https://github.com/npm/cli/issues/4828)), not anything specific
to this project. Fix: `npm install @rolldown/binding-win32-x64-msvc` (matching your `rolldown`
version) and re-run `npm run build`.
