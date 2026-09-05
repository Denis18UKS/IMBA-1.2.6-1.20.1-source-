# Mask Owner Virtual Hitbox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the masked owner move with vanilla player physics while preserving block-like mask hitboxes for other players and mask systems.

**Architecture:** Stop changing the server/local owner's `PlayerEntity` dimensions. Keep mask bounds in `MaskHitbox`, keep remote-player mask dimensions on observer clients, and keep mask-specific eye height unchanged. Remove the v19-v21 movement workarounds that existed only because the owner carried a mask-sized real entity box.

**Tech Stack:** Java 17, Fabric 1.20.1, Yarn mappings, Mixin, JUnit 5, Gradle 8.2.

**Spec:** `docs/superpowers/specs/2026-09-04-mask-owner-virtual-hitbox-design.md`

## Global Constraints

- Minecraft/Fabric target stays 1.20.1 / Java 17.
- Change only hitbox/collision code required by the separation.
- Door external hitbox remains exactly `1.0 x 2.0`.
- `imba:potion_2d` keeps special non-block bounds.
- Preserve mask eye height/camera behavior.
- No changes to sounds, commands, portals, round rules, visuals or unrelated gameplay.

---

### Task 1: Lock the architecture with a failing contract

**Files:**
- Modify: `src/test/java/fable/hideseek/imba/game/MaskMovementCollisionContractTest.java`

**Interfaces:**
- Consumes: current `PlayerEntityMixin`, `PlayerEntityClientMixin`, `MaskHitbox`, active mixin list.
- Produces: regression contract requiring server/local owner vanilla dimensions, remote-client mask dimensions and unchanged eye height.

- [ ] **Step 1: Write the failing test**

Assert that server `PlayerEntityMixin` no longer calls `MaskHitbox.getDimensions` but still calls `MaskHitbox.getEyeHeight`; local-client dimensions return before mask sizing; remote client still calls `MaskHitbox.getDimensions`; obsolete movement/push-out/open-door hooks are absent from `imba.mixins.json`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests fable.hideseek.imba.game.MaskMovementCollisionContractTest --no-daemon --stacktrace`
Expected: FAIL because masked owners still use mask dimensions and the v19-v21 workaround mixins are still active.

---

### Task 2: Separate owner physics from external mask dimensions

**Files:**
- Modify: `src/main/java/fable/hideseek/imba/mixin/PlayerEntityMixin.java`
- Modify: `src/main/java/fable/hideseek/imba/mixin/client/PlayerEntityClientMixin.java`
- Modify: `src/main/java/fable/hideseek/imba/mixin/ServerPlayNetworkHandlerMixin.java`
- Modify: `src/main/resources/imba.mixins.json`
- Delete: `src/main/java/fable/hideseek/imba/mask/MaskMovementCollision.java`
- Delete: `src/main/java/fable/hideseek/imba/mixin/MaskedMovementCollisionMixin.java`
- Delete: `src/main/java/fable/hideseek/imba/mixin/client/ClientMaskedMovementCollisionMixin.java`
- Delete: `src/main/java/fable/hideseek/imba/mixin/client/ClientPlayerMaskPushOutMixin.java`
- Delete: `src/main/java/fable/hideseek/imba/mixin/DoorCollisionMixin.java`
- Delete: `src/main/java/fable/hideseek/imba/mixin/client/ClientDoorCollisionMixin.java`

**Interfaces:**
- Consumes: `MaskHitbox.getDimensions(MaskType, Block, Item)`, `MaskHitbox.getEyeHeight(...)`, `ClientMaskData`.
- Produces: vanilla physical dimensions for server/local owner, mask dimensions for remote masked players, unchanged mask eye height.

- [ ] **Step 1: Remove only the server dimension override**

Delete the `getDimensions` injection from `PlayerEntityMixin`. Keep the existing `getActiveEyeHeight` injection unchanged and preserve every unrelated statue/gameplay injection.

- [ ] **Step 2: Restrict only client dimensions to remote players**

In `clientDimensions`, resolve:

```java
PlayerEntity self = (PlayerEntity) (Object) this;
PlayerEntity localPlayer = MinecraftClient.getInstance().player;
if (self == localPlayer) return;
```

Then keep all existing remote mask sizing logic. Leave `clientEyeHeight` mask behavior unchanged for both local and remote players.

- [ ] **Step 3: Remove obsolete owner-world collision workarounds**

Delete the three movement/push-out mixins, the two open-door owner-bypass mixins and `MaskMovementCollision`. Remove their entries from `imba.mixins.json`. Remove only the `isPlayerNotCollidingWithBlocks` `@ModifyVariable` and its now-unused imports from `ServerPlayNetworkHandlerMixin`; preserve restricted-item and statue packet handling.

- [ ] **Step 4: Run focused test**

Run: `./gradlew test --tests fable.hideseek.imba.game.MaskMovementCollisionContractTest --no-daemon --stacktrace`
Expected: PASS.

---

### Task 3: Verify preserved external hitbox contracts

**Files:**
- Test: `src/test/java/fable/hideseek/imba/game/MaskMovementCollisionContractTest.java`
- Test: `src/test/java/fable/hideseek/imba/game/MaskHitboxAllBlocksContractTest.java`

**Interfaces:**
- Consumes: `MaskHitbox`, remote-client mask dimensions, `MaskCollisionShapes`.
- Produces: evidence that door/potion/hanging-lantern external bounds were not changed.

- [ ] **Step 1: Run hitbox tests**

Run: `./gradlew test --tests fable.hideseek.imba.game.MaskMovementCollisionContractTest --tests fable.hideseek.imba.game.MaskHitboxAllBlocksContractTest --no-daemon --stacktrace`
Expected: PASS, including door `1.0F, 2.0F`, hanging-lantern full-block bounds and special potion exclusions.

---

### Task 4: Full verification and artifact

**Files:**
- No additional production changes.

**Interfaces:**
- Produces: final remapped runtime JAR and source archive from the exact branch head.

- [ ] **Step 1: Run full build**

Run: `./gradlew clean build --no-daemon --stacktrace`
Expected: `BUILD SUCCESSFUL`, zero failed tests, `remapJar` completed.

- [ ] **Step 2: Inspect diff**

Compare against v21 head `da44162f00874475b46e9596ab0f7d1280075007`; verify only docs/tests and collision/hitbox mixins changed.

- [ ] **Step 3: Download workflow artifacts**

Download `imba-built-jar` and `imba-fixed-sources` from the successful workflow run and provide the runtime JAR for survival testing.
