# How to Build ReVanced Manager

This document explains how to build the ReVanced Manager Android app from source on a
clean machine (Linux, macOS or Windows). It is written for people who have never built
this project before.

> TL;DR
> ```bash
> export JAVA_HOME=/path/to/jdk-17
> export ANDROID_HOME=/path/to/Android/Sdk
> ./gradlew :app:assembleDebug \
>     -PgithubPackagesUsername=<your-github-username> \
>     -PgithubPackagesPassword=<github-token-with-read:packages>
> ```
> Output APK: `app/build/outputs/apk/debug/revanced-manager-<version>.apk`

---

## 1. Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | **17** | The build targets Java 17 (`jvmToolchain(17)`). JDK 21+ is **not** guaranteed to work. |
| Android SDK | Platform **36**, Build-Tools **36.x** | `compileSdk = 36`, `minSdk = 26`, `targetSdk = 36`. |
| Gradle | **9.4.0** | Do **not** install manually. Use the bundled wrapper (`./gradlew`); it downloads the correct version automatically (~150 MB on first run). |
| GitHub account + token | token with **`read:packages`** | Required to download ReVanced's own libraries. See section 3. This is the step most people miss. |

You do **not** need Android Studio, the NDK, Rust, or any native toolchain. This is a
pure Kotlin/Compose app and builds with Gradle alone.

### 1.1 Install a JDK 17

- Linux (Debian/Ubuntu): `sudo apt install openjdk-17-jdk`
- macOS (Homebrew): `brew install openjdk@17`
- Windows: install "Eclipse Temurin 17" (Adoptium) or equivalent.

Point `JAVA_HOME` at it:

```bash
# Linux example
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
# macOS example
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
```
```powershell
# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17"
```

### 1.2 Install the Android SDK

Easiest path is to install the **command-line tools** and then the required packages:

```bash
# after installing cmdline-tools and setting ANDROID_HOME:
sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"
```

Set `ANDROID_HOME` (a.k.a. `ANDROID_SDK_ROOT`) to the SDK location:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"        # Linux
export ANDROID_HOME="$HOME/Library/Android/sdk" # macOS
```
```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"  # Windows
```

Alternatively, create a `local.properties` file in the project root:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

If a required SDK platform/build-tools package is missing, AGP will offer to download it
automatically (you must accept the SDK licenses once: `sdkmanager --licenses`).

---

## 2. Getting the code

```bash
git clone https://github.com/kotlin-benchmark/revanced-manager.git
cd revanced-manager
```

---

## 3. GitHub Packages authentication (required)

ReVanced Manager depends on two libraries that ReVanced publishes **only** to their
GitHub Packages Maven registry:

- `app.revanced:patcher-android`
- `app.revanced:library-android`

Gradle needs credentials to download them. Without credentials the build fails during
dependency resolution with:

```
Could not GET '.../patcher-android-...pom'. Received status code 401 from server: Unauthorized
```

You need a GitHub token that has the **`read:packages`** scope. Both a fine-grained or a
classic Personal Access Token work; the packages are public, so any valid token with
`read:packages` can read them.

### 3.1 Create / obtain a token

Option A — reuse the GitHub CLI token (if you use `gh`):

```bash
gh auth refresh -h github.com -s read:packages   # adds the scope, keeps existing ones
gh auth token                                     # prints the token value
```

Option B — create a classic Personal Access Token:
GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic) →
Generate new token → enable **`read:packages`**.

### 3.2 Provide the credentials to Gradle

Pick **one** of these:

**(a) Command-line flags (quickest):**
```bash
./gradlew :app:assembleDebug \
    -PgithubPackagesUsername=<your-github-username> \
    -PgithubPackagesPassword=<your-token>
```

**(b) Global Gradle properties (recommended for repeated builds).**
Add to `~/.gradle/gradle.properties` (create the file if it does not exist):
```properties
githubPackagesUsername=<your-github-username>
githubPackagesPassword=<your-token>
```
Then you can simply run `./gradlew :app:assembleDebug`.

**(c) Environment variables:**
```bash
export ORG_GRADLE_PROJECT_githubPackagesUsername=<your-github-username>
export ORG_GRADLE_PROJECT_githubPackagesPassword=<your-token>
```

> Never commit your token. Prefer option (b) in your home directory, which is outside the
> repository.

---

## 4. Building

### Debug APK (typical)

```bash
./gradlew :app:assembleDebug
```

For a fully clean, cache-free build (useful for verification):

```bash
./gradlew clean :app:assembleDebug --no-build-cache --rerun-tasks
```

### Release APK

```bash
./gradlew :app:assembleRelease
```
(Release builds are minified with R8/ProGuard and expect a signing configuration.)

### Windows

Use `gradlew.bat` instead of `./gradlew`:

```powershell
.\gradlew.bat :app:assembleDebug -PgithubPackagesUsername=<user> -PgithubPackagesPassword=<token>
```

---

## 5. Output

The built APK is written to:

```
app/build/outputs/apk/debug/revanced-manager-<version>.apk
```

Install it on a connected device/emulator with:

```bash
adb install -r app/build/outputs/apk/debug/revanced-manager-*.apk
```

---

## 6. Troubleshooting

**`401 Unauthorized` when resolving `app.revanced:patcher-android` / `library-android`**
Your token is missing or lacks the `read:packages` scope. Re-check section 3. Verify with:
```bash
curl -s -o /dev/null -w "%{http_code}\n" -u "<user>:<token>" \
  https://maven.pkg.github.com/revanced/registry/app/revanced/patcher-android/22.0.2-dev.1/patcher-android-22.0.2-dev.1.pom
```
`302` = authenticated OK. `401` = token/scope problem.

**`The following Gradle properties are missing for 'githubPackages' credentials`**
You ran the build without providing `githubPackagesUsername` / `githubPackagesPassword`.
Supply them via any method in section 3.2. (Dummy values pass this specific check but will
then fail with `401` when the real download happens.)

**`Cannot lock ... checksums cache ... already been locked by this process`**
A previous Gradle run was killed and left a stale daemon/lock. Fix:
```bash
./gradlew --stop
pkill -f GradleDaemon   # if needed
```
Then rebuild. Adding `--no-daemon` avoids leaving daemons behind.

**`Failed to find target with hash string 'android-36'` / missing build-tools**
Install the packages: `sdkmanager "platforms;android-36" "build-tools;36.0.0"` and accept
licenses with `sdkmanager --licenses`.

**Wrong Java version (`Unsupported class file major version` or toolchain errors)**
Ensure `JAVA_HOME` points to a **JDK 17**. Check with `java -version`.

**First build is slow**
The first run downloads Gradle 9.4.0, the Android Gradle Plugin, and all dependencies
(including the ReVanced libraries). Subsequent builds are much faster thanks to caching.
