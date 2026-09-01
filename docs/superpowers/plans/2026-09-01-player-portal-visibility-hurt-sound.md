# Player Portal Visibility + Seeker Hurt Sound Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve the working v13/v10-backed player-portal appearance while making players behind it visible and use the vanilla player hurt sound exactly when a seeker loses one heart.

**Architecture:** Keep `MaskRenderHelper` and its `Blocks.NETHER_PORTAL` model path unchanged. Fix visibility by deferring only `MaskType.PORTAL` masks out of `PlayerEntityRenderer`'s entity-order pass and drawing those same masks at `WorldRenderer.render` TAIL after normal entities, using the existing entity vertex consumers and an explicit final `draw()`. Change only the fail-sound identifier from generic hurt to player hurt; keep the hook bound to `damageSeekerHeart`.

**Tech Stack:** Java 17, Fabric 1.20.1, Sponge Mixin, JUnit 5, Gradle/Loom 1.3.10.

**Spec:** User request in this conversation, 2026-09-01.

## Global Constraints

- Base branch/head: `fix/v10-working-base-portal-freeze-v13` from the working v13 build.
- Do not replace or redraw the portal model/texture; keep `MaskRenderHelper`'s `Blocks.NETHER_PORTAL` path.
- Preserve lobby timing, blackout, spreadplayers, portal transit isolation, and live freeze settings.
- Seeker miss sound must be `minecraft:entity.player.hurt` and only fire through the existing actual-heart-deduction method.
- Final deliverables: remapped `.jar`, sources ZIP, and `.docx` command table.

---

### Task 1: Seeker player-hurt sound

**Files:**
- Modify: `src/test/java/fable/hideseek/imba/game/PortalReturnTimingContractTest.java`
- Modify: `src/main/java/fable/hideseek/imba/mixin/GameplayFixesMixin.java`

**Interfaces:**
- Consumes: `GameManager.damageSeekerHeart(ServerPlayerEntity, String)`.
- Produces: `FAIL_SOUND_COMMAND = "playsound minecraft:entity.player.hurt player @a ~ ~ ~ 10 1"`.

- [ ] **Step 1: Write the failing test**

Change the existing sound assertion to:
```java
assertTrue(fixes.contains("playsound minecraft:entity.player.hurt player @a ~ ~ ~ 10 1"));
assertFalse(fixes.contains("playsound minecraft:entity.generic.hurt player @a ~ ~ ~ 10 1"));
```

- [ ] **Step 2: Run the test and verify RED**

Run full CI build because local Gradle dependencies are not guaranteed available:
```text
./gradlew clean build --no-daemon --stacktrace
```
Expected: the seeker sound contract fails because v13 still contains `entity.generic.hurt`.

- [ ] **Step 3: Minimal implementation**

Change only the sound identifier in `GameplayFixesMixin.FAIL_SOUND_COMMAND`:
```java
"playsound minecraft:entity.player.hurt player @a ~ ~ ~ 10 1";
```
Do not move the injection away from `damageSeekerHeart`.

- [ ] **Step 4: Verify GREEN for the sound contract**

Run the same full Gradle build. Expected: sound contract passes with no new failures.

### Task 2: Render player-portals after normal entities

**Files:**
- Create: `src/test/java/fable/hideseek/imba/game/PlayerPortalVisibilityContractTest.java`
- Modify: `src/main/java/fable/hideseek/imba/mixin/client/PlayerRendererMixin.java`
- Modify: `src/main/java/fable/hideseek/imba/mixin/client/WorldRendererMixin.java`
- Preserve unchanged: `src/main/java/fable/hideseek/imba/client/MaskRenderHelper.java`

**Interfaces:**
- `PlayerRendererMixin`: for `MaskType.PORTAL`, cancel vanilla player rendering but do not emit portal vertices in the per-entity pass.
- `WorldRendererMixin`: at `WorldRenderer.render` TAIL, iterate client players whose `ClientMaskData.TYPES` value is `MaskType.PORTAL`, translate from camera to interpolated player position, and call the existing `MaskRenderHelper.renderMask(...)`; flush `VertexConsumerProvider.Immediate` once after late portal rendering.

- [ ] **Step 1: Write the failing visibility contract**

Assert all of the following:
```java
assertTrue(playerRenderer.contains("MaskType.PORTAL"));
assertTrue(playerRenderer.contains("deferred"));
assertTrue(worldRenderer.contains("renderPortalMasksLate"));
assertTrue(worldRenderer.contains("MaskType.PORTAL"));
assertTrue(worldRenderer.contains("client.world.getPlayers()"));
assertTrue(worldRenderer.contains("vertexConsumers.draw()"));
assertTrue(maskHelper.contains("Blocks.NETHER_PORTAL.getDefaultState()"));
```
The production change that makes this pass is deferring portal-mask emission until the world-render tail while keeping the old model path.

- [ ] **Step 2: Run and verify RED**

Run the full Gradle CI build. Expected: only the new visibility contract fails before production changes.

- [ ] **Step 3: Minimal PlayerRenderer change**

After `ci.cancel()`, branch on portal type:
```java
if (ClientMaskData.TYPES.get(uuid) == MaskType.PORTAL) {
    // Deferred to WorldRenderer TAIL so translucent portal pixels cannot hide entities rendered later.
    return;
}
```
Continue using the existing rendering path for every non-portal mask.

- [ ] **Step 4: Minimal WorldRenderer late pass**

At the existing `render` TAIL hook, render all portal-masked client players first. For each player use interpolated world position minus `camera.getPos()`, existing mask light resolution, and `MaskRenderHelper.renderMask(...)`. Flush once after all portal masks. Keep the existing first-person non-portal rendering behavior; skip a second portal render there.

- [ ] **Step 5: Verify GREEN**

Run:
```text
./gradlew clean build --no-daemon --stacktrace
```
Expected: all tests pass, remap succeeds, artifacts upload.

### Task 3: Runtime verification and deliverables

**Files:**
- Generate: final remapped JAR from CI artifact.
- Generate: final sources ZIP from CI artifact.
- Generate locally: `IMBA-1.2.6-1.20.1-команды-мода-v14.docx`.

**Interfaces:**
- Command document sources: current `CommandInit.java` and `ImbaExtension.java` command registration code on the final head.

- [ ] **Step 1: Inspect final JAR**

Verify archive integrity and inspect bytecode/resources for:
```text
minecraft:entity.player.hurt
spreadplayers -131.49 148.72 2 5 under -29 false @a
PlayerRendererMixin
WorldRendererMixin
MaskRenderHelper
```
Compare `MaskRenderHelper.class` against the current v13 JAR and require byte-for-byte equality.

- [ ] **Step 2: Re-enumerate all mod commands from current source**

Read every Brigadier root/leaf registration from `CommandInit.java` and `ImbaExtension.java`; record command, purpose, syntax, example, permissions/notes. Do not rely on the previous document if source changed.

- [ ] **Step 3: Build and visually validate DOCX**

Create a landscape table with columns `Команда`, `Для чего используется`, `Синтаксис`, `Пример`, `Примечания`. Render the DOCX to PDF/images using the document skill workflow and inspect all pages for clipping/overflow.

- [ ] **Step 4: Final verification**

Confirm CI has a fresh `success`, both archives pass integrity checks, JAR SHA-256 is recorded, and the DOCX opens/renders correctly before claiming completion.
