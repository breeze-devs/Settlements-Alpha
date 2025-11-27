# Settlements

A Minecraft mod that enhances village-related experiences

Welcome to the Settlements documentation. This site is built with MkDocs and the Material for MkDocs theme.

## Quick start (local docs site)

1) Ensure Python 3.8+ is installed
2) Install MkDocs with the Material theme:
   - Windows/macOS/Linux: `python -m pip install mkdocs-material`
3) Serve the docs locally from the repository root:
   - `mkdocs serve`
4) Open http://127.0.0.1:8000 in your browser. The server reloads automatically when you edit files in the `docs/` folder or `mkdocs.yml`.

## Documentation structure

- Home: This page

You can add new pages under the `docs/` folder and wire them into the left-hand navigation by editing `mkdocs.yml` (the `nav:` section).

## Writing docs

- Use Markdown. Material for MkDocs supports many useful extensions already enabled in `mkdocs.yml` (admonitions, superfences, code copy, etc.).
- Keep pages short and focused. Prefer multiple small pages over one very long document.
- Place images in `docs/images/` (create it if it doesn’t exist) and reference them with relative paths, e.g. `![Alt text](images/example.png)`.

Tip: You can use callouts/admonitions like this:

```markdown
!!! note "Heads up"
    This is an admonition rendered by Material for MkDocs.
```

## Deploying the docs (optional)

Two common options:

1) GitHub Pages via MkDocs
   - Run `mkdocs gh-deploy` from the repository root.
   - This builds the site and pushes it to the `gh-pages` branch.
   - In your GitHub repository settings, enable Pages and select the `gh-pages` branch as the source.

2) CI workflow
   - Add a GitHub Actions workflow that runs `pip install mkdocs-material` and `mkdocs gh-deploy --force` on pushes to `main`.

Note: The generated site/ directory is ephemeral; you typically don’t commit it. If desired, add `site/` to `.gitignore`.
