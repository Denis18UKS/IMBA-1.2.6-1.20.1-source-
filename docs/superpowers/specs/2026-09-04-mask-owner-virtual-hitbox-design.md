# Mask Owner Virtual Hitbox Design

## Goal

Allow a masked player to move through normal one-block passages exactly like a vanilla player without shrinking the mask hitbox seen/used by other players.

## Root cause

The current implementation overrides `PlayerEntity.getDimensions()` with mask-sized dimensions. That makes the mask-sized box the player's real physics box, so Minecraft uses it in many unrelated world-collision and movement checks. v19-v21 tried to narrow individual collision stages, but the large entity box still leaks into other vanilla paths.

## Design

Separate owner physics from external mask geometry.

- The server-side masked owner always keeps vanilla player dimensions for world movement.
- The local client player also keeps vanilla player dimensions so prediction matches the server.
- Remote masked players may still expose mask-sized dimensions on clients so seeker targeting/debug representation remains block-like.
- `MaskHitbox` remains the canonical external target/debug bounds. Door remains exactly `1.0 x 2.0`; `potion_2d` keeps its special non-block bounds.
- `MaskCollisionShapes` remains the canonical world-facing collision geometry used by the mod for seekers/statue masks.
- The old movement-box swap, push-out cancellation and server post-move validation workarounds are removed because the owner no longer carries the oversized mask box through vanilla physics.
- Seeker hit detection must not depend only on the server player's vanilla physics box. Existing remote-client mask dimensions and server virtual-mask fallback are used so block-like mask hitboxes remain hittable.

## Constraints

- Minecraft/Fabric target stays 1.20.1 / Java 17.
- Change only hitbox/collision/targeting code required by the separation.
- Do not change visuals, commands, game rules, seeker heart logic, sounds, portal behavior or other gameplay systems.
- Door external hitbox remains 1 block wide and 2 blocks high.
- `imba:potion_2d` remains excluded from block-like hitbox behavior.
- Owner passage must work in survival while walking on the ground, not only in creative flight.

## Verification

1. RED contract proves current server/local-client code still overrides the masked owner's physical dimensions.
2. GREEN contract proves server/local owner retain vanilla dimensions while remote masked players still use mask dimensions.
3. Existing door and potion hitbox contracts remain green.
4. Full `./gradlew clean build --no-daemon --stacktrace` succeeds.
5. Runtime JAR is built from the exact final branch head and supplied for in-game survival testing.
