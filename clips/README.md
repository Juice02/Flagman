# FlagMan clips

Explainer animations built with the stageshow skill (React + its `kit/`).

- `flagman-killswitch/`: a broken feature turned off without a deploy
- `flagman-hero/`: the dashboard and the app are separate; the app calls `/evaluate`
- `flagman-rollout/`: two users with fixed buckets (11 and 64) and a rollout slider
- `preview-page.html`: the page text and order used for the published preview

Each clip imports `../../kit`. To preview or film one, copy its folder into
`~/.claude/skills/stageshow/examples/` and run `npm run dev` there, or copy the
skill's `kit/` next to these folders and fix the import path.

Published preview: https://claude.ai/artifact/563WhEMptSKYWXppR33m47
