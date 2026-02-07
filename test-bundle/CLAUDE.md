# Test Bundle

## Architecture

The app uses a **shell + bundle** architecture:

- **Shell** (`app/` module): A minimal launcher that downloads, validates, and launches bundles. It handles the bootstrap lifecycle (check → download → validate → launch) and background update checks. Implemented by `AppController`.
- **Bundle** (`test-bundle/` module): The actual application code, packaged and signed. Bundles are versioned and can self-update by using `BundleUpdater` via the `UpdateManager`.

The shell passes a `BundleLaunchConfig` (JSON) as `args[0]` to the bundle's main class, containing the base URL, public key, storage paths, and current build number.

## Update Flow

1. Shell downloads bundle 1 from `bundles/1/`, validates it, and launches it
2. Bundle 1 starts its own `UpdateManager` pointing at `bundles/2/`
3. `UpdateManager` uses `BundleUpdater.downloadLatest()` to download the new bundle into the shared storage directory
4. Bundle 1 shows a "Restart to Update" button when the download succeeds
5. Clicking restart spawns a new JVM process (re-entering `ManualUpdateTest.main()`) and calls `exitProcess(0)`
6. The new process starts the shell again — it validates storage, finds bundle 2 already downloaded, and launches it
7. Bundle 2's `UpdateManager` checks its own URL, finds it's already up to date

## Test Bundles

- **Bundle 1** (`testbundle.bundle1.Main`): Shows "Bundle v1 - Build #1". Checks `bundles/2/` for updates. Shows restart button when update is ready.
- **Bundle 2** (`testbundle.bundle2.Main`): Shows "Bundle v2 - Build #2" in green. Checks its own URL (already up to date).

Both bundles use `DISPOSE_ON_CLOSE` so closing the window doesn't kill the JVM (the shell process manages lifecycle).

## Building

```bash
# Rebuild both bundles (after code changes)
./gradlew :test-bundle:createBundles
```

This compiles the test-bundle jar, stages it into separate directories with version markers, then runs `BundleCreatorTask` to produce signed bundles in `bundles/1/` and `bundles/2/`.

## Running

```bash
# Manual update test (interactive)
./gradlew :app:runManualUpdateTest

# Automated integration test
./gradlew :app:jvmTest --tests "*AppControllerUpdateIntegrationTest*"
```

The manual test launches the shell pointing at `bundles/1/`. Bundle 1 discovers bundle 2, downloads it, and shows a restart button. Clicking restart spawns a new process that picks up bundle 2.
