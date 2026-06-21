## 🎁 What's new

1.19.0 is a follow-up to the 1.18.0 per-world economy release. It fixes two issues that stopped the new economy from working cleanly in a common setup. InvSwitcher now functions as a true **standalone economy** — you no longer need a separate economy plugin such as EssentialsX for it to take effect — and admin economy commands now report the **correct balance for offline players** instead of a stale value.

## ✨ Highlights

### 🐛 Standalone economy now works on its own
InvSwitcher registers its own per-world Vault economy, but BentoBox hooks Vault during its early startup, *before* addons enable. When InvSwitcher was the only economy on the server, that early hook found nothing and was discarded, so BentoBox's `getVault()` stayed empty — and economy-dependent addons such as **Bank** disabled themselves with "Vault is required" even though a working per-world economy was present. InvSwitcher now registers a fresh Vault hook with BentoBox once its provider is live, so it works as the server's only economy.

> Companion framework PRs harden this further: [BentoBox #2995](https://github.com/BentoBoxWorld/BentoBox/pull/2995) retries the Vault hook after addons enable, and [Bank #67](https://github.com/BentoBoxWorld/Bank/pull/67) retries before disabling.

### 🐛 Correct balance reported for offline economy transactions
Admin `eco give`, `eco set`, and `eco take` on an offline player reported a stale balance — for example "New balance: 0.00" right after giving 2,000. The money was always stored correctly, but the confirmation message re-read the balance from the database before the asynchronous save had flushed, returning the pre-transaction value. The commands now report the authoritative balance returned by the transaction itself.

This release also hardens the underlying offline read-after-write path: offline saves are tracked while in flight so two rapid sequential transactions on the same offline player can no longer load independent stale copies and lose an update — without blocking the main thread or caching offline players indefinitely.

## ⚙️ Compatibility

✔️ BentoBox 3.17.0
✔️ Paper Minecraft 1.21.5 – 26.1.2
✔️ Java 21

## 📥 How to update
1. Take backups of your server, for safety.
2. Stop the server.
3. Drop the new InvSwitcher jar into the addons folder and remove the old one.
4. Restart the server.
5. You should be good to go!

## What's Changed
* 🐛 Register a fresh Vault hook so InvSwitcher works as a standalone economy by @tastybento in https://github.com/BentoBoxWorld/InvSwitcher/commit/d929649
* 🐛 Report offline economy balances correctly and harden offline saves by @tastybento in https://github.com/BentoBoxWorld/InvSwitcher/commit/a15c645
* Add MC 26.1.2 to Modrinth game-versions by @tastybento in https://github.com/BentoBoxWorld/InvSwitcher/pull/52

**Full Changelog**: https://github.com/BentoBoxWorld/InvSwitcher/compare/1.18.0...1.19.0
