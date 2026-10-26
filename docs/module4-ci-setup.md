# Module 4 Phase 3 — GitHub Actions CI and Docker Hub publishing

This is an optional stretch goal for the NTU Module 4 assignment. The mandatory TaskFlow API is already deployed to Render. **This workflow never deploys to Render**, and does not modify the CareersAtlas worker or workspace.

## Workflow behavior

- On pull requests targeting `main`: install Java 21, run `./mvnw clean verify` (actual tests), and build the existing multi-stage Dockerfile. **No registry credentials or image publishing.**
- On pushes to `main`: run the same verification; publish two Docker Hub image tags **only when** the repository variable `DOCKERHUB_USERNAME` is configured and `DOCKERHUB_TOKEN` secret is valid.
- Manual runs are available through **Actions → TaskFlow CI and Docker Hub → Run workflow**, on `main`.
- The image is named `<dockerhub-username>/taskflow-api`; published tags are `sha-<first-12-commit-characters>` and `latest`.
- `actions/checkout` and `actions/setup-java` are pinned to immutable commit SHAs. The GitHub token is limited to `contents: read`.

## Before enabling Docker Hub publishing

1. Sign in at https://hub.docker.com/ and create a **public** repository named `taskflow-api` under your own Docker Hub username. If an existing repository has a different name, update the workflow deliberately rather than blindly reusing that repository.
2. Create a Docker Hub personal access token with minimum suitable scope for pushing to that repository. Keep it private and do not put it in shell output, README, screenshots, commit messages or chat.
3. Under this GitHub repository's **Settings → Secrets and variables → Actions → Secrets**, add `DOCKERHUB_TOKEN` containing the access token.
4. Under **Settings → Secrets and variables → Actions → Variables**, add `DOCKERHUB_USERNAME` containing your Docker Hub username. **Add the token first and the username variable second**, so that a push cannot start publishing with an unset token.
5. On the `main` branch, trigger **Actions → TaskFlow CI and Docker Hub → Run workflow**. Check that the `verify` and `publish` jobs succeed.
6. Verify the resulting `latest` and `sha-...` tags on Docker Hub, and copy the successful GitHub Actions run URL and Docker Hub repository URL into README.md. Only then mark stretch goal 1 complete.

## Observability and troubleshooting

- If `DOCKERHUB_USERNAME` is unset, the publishing job is **skipped by design**. CI build/test success alone does **not** fulfill the assignment's Docker Hub publishing requirement.
- If the username is set but the token is unset, the publishing job fails explicitly without attempting an insecure login.
- If `docker login` fails, check the username and token scope. Never print the token to debug.
- If a Docker push fails, check that the public `taskflow-api` repository already exists under the provided username.
- The Dockerfile itself uses `mvn clean package -DskipTests`; the separate `verify` job actually runs the Maven tests beforehand.
- Render auto-deploy remains off. The live public API and the existing Dockerfile are not modified by this CI setup.

## Evidence checklist

- [ ] Pull-request verify job completed successfully.
- [ ] Main-branch verify job completed successfully.
- [ ] Docker Hub publish job completed successfully after credentials were configured.
- [ ] Docker Hub `latest` and commit-SHA image tags verified.
- [ ] README updated with real workflow run URL, Docker Hub URL, and evidence; only then check the optional goal as complete.
