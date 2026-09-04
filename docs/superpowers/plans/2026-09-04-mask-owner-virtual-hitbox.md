# Mask Owner Virtual Hitbox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the masked owner move with vanilla player physics while preserving block-like mask hitboxes for other players and mask systems.

**Architecture:** Stop changing the server/local owner's `PlayerEntity` dimensions. Keep mask bounds in `MaskHitbox` and client remote-player dimensions / virtual mask geometry. Remove the v19-v21 movement workarounds that existed only because the owner carried a mask-sized real entity box.

**Tech Stack:** Java 17, Fabric 1.20.1, Yarn mappings, Mixin, JUnit 5, Gradle 8.2.

**Spec:** `docs/superpowers/specs/2026-09-04-mask-owner-virtual-hitbox-design.md`

## Global Constraints

- Minecraft/Fabric target stays 1.20.1 / Java 17.
- Change only hitbox/collision/targeting code required by the separation.
- Door external hitbox remains exactly `1.0 x 2.0`.
- `imba:potion_2d` keeps special non-block bounds.
- No changes to sounds, commands, portals, round rules, visuals or unrelated gameplay.

---

### Task 1: Lock the architecture with a failing contract

**Files:**
- Modify: `src/test/java/fable/hideseek/imba/game/MaskMovementCollisionContractTest.java`

**Interfaces:**
- Consumes: current `PlayerEntityMixin`, `PlayerEntityClientMixin`, `MaskHitbox`.
- Produces: regression contract requiring server/local owner vanilla dimensions and remote-client mask dimensions.

- [ ] **Step 1: Write the failing test**

Add a test that asserts:

```java
String serverPlayer = read("src/main/java/fable/hideseek/imba/mixin/PlayerEntityMixin.java");
String clientPlayer = read("src/main/java/fable/hideseek/imba/mixin/client/PlayerEntityClientMixin.java");
assertFalse(serverPlayer.contains("MaskHitbox.getDimensions"));
assertTrue(clientPlayer.contains("MinecraftClient.getInstance().player"));
assertTrue(clientPlayer.contains("if (self == localPlayer) return"));
assertTrue(clientPlayer.contains("MaskHitbox.getDimensions"));
```

Also assert the old whole-move workaround markers are absent from server/client `MaskedMovementCollisionMixin`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests fable.hideseek.imba.game.MaskMovementCollisionContractTest --no-daemon --stacktrace`
Expected: FAIL because masked owners still use `MaskHitbox.getDimensions` and the v21 whole-move swap still exists.

---

### Task 2: Separate owner physics from external mask dimensions

**Files:**
- Modify: `src/main/java/fable/hideseek/imba/mixin/PlayerEntityMixin.java`
- Modify: `src/main/java/fable/hideseek/imba/mixin/client/PlayerEntityClientMixin.java`
- Modify: `src/main/java/fable/hideseek/imba/mixin/MaskedMovementCollisionMixin.java`
- Modify: `src/main/java/fable/hideseek/imba/mixin/client/ClientMaskedMovementCollisionMixin.java`
- Modify: `src/main/java/fable/hideseek/imba/mixin/client/ClientPlayerMaskPushOutMixin.java`
- Modify: `src/main/java/fable/hideseek/imba/mixin/ServerPlayNetworkHandlerMixin.java`

**Interfaces:**
- Consumes: `MaskHitbox.getDimensions(MaskType, Block, Item)`, `ClientMaskData`.
- Produces: vanilla physical dimensions for the server owner and local client, mask dimensions for remote masked players.

- [ ] **Step 1: Remove server mask dimension overrides**

Delete the `getDimensions` and `getActiveEyeHeight` injections from `PlayerEntityMixin`; leave unrelated statue/gameplay injections unchanged.

- [ ] **Step 2: Restrict client mask dimensions to remote players**

At the start of client dimension/eye-height injections resolve:

```java
PlayerEntity self = (PlayerEntity) (Object) this;
PlayerEntity localPlayer = MinecraftClient.getInstance().player;
if (self == localPlayer) return;
```

Remote masked players keep the existing `MaskHitbox` / synced custom bounds behavior.

- [ ] **Step 3: Remove obsolete owner movement workarounds**

Make `MaskedMovementCollisionMixin`, `ClientMaskedMovementCollisionMixin` and `ClientPlayerMaskPushOutMixin` no-op/remove their owner-box and push-out injections. Remove the `isPlayerNotCollidingWithBlocks` owner-box `@ModifyVariable` from `ServerPlayNetworkHandlerMixin`; preserve unrelated restricted-item/statue packet code.

- [ ] **Step 4: Run focused test**

Run: `./gradlew test --tests fable.hideseek.imba.game.MaskMovementCollisionContractTest --no-daemon --stacktrace`
Expected: PASS.

---

### Task 3: Verify preserved external hitbox contracts

**Files:**
- Test: `src/test/java/fable/hideseek/imba/game/MaskMovementCollisionContractTest.java`
- Test: `src/test/java/fable/hideseek/imba/game/MaskHitboxAllBlocksContractTest.java`

**Interfaces:**
- Consumes: `MaskHitbox`, remote-client mask dimensions.
- Produces: evidence that door/potion/block bounds were not changed.

- [ ] **Step 1: Run hitbox tests**

Run: `./gradlew test --tests fable.hideseek.imba.game.MaskMovementCollisionContractTest --tests fable.hideseek.imba.game.MaskHitboxAllBlocksContractTest --no-daemon --stacktrace`
Expected: PASS, including door `1.0F, 2.0F` and special potion exclusions.

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
