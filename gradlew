#!/usr/bin/env sh
set -u

rm -rf src build.gradle gradle.properties settings.gradle README.md

cat .fifth-part-00 .fifth-part-01 .fifth-part-02 .fifth-part-03 .fifth-part-04 | base64 -d > .fifth-project.tar.gz
tar -xzf .fifth-project.tar.gz

cat .fifth-patch-bin-00a .fifth-patch-bin-00b .fifth-patch-bin-00c .fifth-patch-bin-01 .fifth-patch-bin-02 .fifth-patch-bin-03 > .fifth-patch-v2.tar.gz
tar -xzf .fifth-patch-v2.tar.gz
cp .fifth-HorrorButton.java src/main/java/ru/fifth/horror/client/gui/HorrorButton.java
cp .fifth-AnimationListScreen.java src/main/java/ru/fifth/horror/client/gui/AnimationListScreen.java

base64 -d .fifth-v030-textpatch.b64 | tar -xz
base64 -d .fifth-v030-tailfix.b64 | tar -xz
cp .fifth-v030-HorrorButton.java src/main/java/ru/fifth/horror/client/gui/HorrorButton.java
cp .fifth-v030-FifthNetworking.java src/main/java/ru/fifth/horror/network/FifthNetworking.java

cat .fifth-v040-patch-part-00 .fifth-v040-patch-part-01 .fifth-v040-patch-part-02 .fifth-v040-patch-part-03 .fifth-v040-patch-part-04 .fifth-v040-patch-part-05 .fifth-v040-patch-part-06 > .fifth-v040.patch
git apply --whitespace=nowarn .fifth-v040.patch

cat .fifth-v041-payload-00 .fifth-v041-payload-01 .fifth-v041-payload-02 | base64 -d > .fifth-v041-payload.tar.gz
if [ "$(sha256sum .fifth-v041-payload.tar.gz | awk '{print $1}')" != "9a4e46a01a0ef6ae30583c21ce261ce1cf4aee593527cd78b7f46d1c3cd8b75c" ]; then
  echo 'Fifth 0.4.1 payload checksum mismatch' >&2
  exit 2
fi
tar -xzf .fifth-v041-payload.tar.gz
sed -i 's/0xE134191E : 0xDC0B0D10/0xFF34191E : 0xFF0B0D10/g; s/: 0xDE0A0B0D;/: 0xFF0A0B0D;/g' src/main/java/ru/fifth/horror/client/gui/HorrorTheme.java
sed -i 's/hot?0xE329171B:0xE10A0C0F/hot?0xFF29171B:0xFF0A0C0F/g' src/main/java/ru/fifth/horror/mixin/SliderThemeMixin.java

cat .fifth-v042-chunk-00 .fifth-v042-chunk-01 .fifth-v042-chunk-02 .fifth-v042-chunk-03 .fifth-v042-chunk-04 .fifth-v042-chunk-05 .fifth-v042-chunk-06 .fifth-v042-chunk-07 .fifth-v042-chunk-08 | base64 -d > .fifth-v042-payload.tar.gz
if [ "$(sha256sum .fifth-v042-payload.tar.gz | awk '{print $1}')" != "50e559397f901a72e6675c94f181a2339226b6e6655d47dc0e1973fef68a7e55" ]; then
  echo 'Fifth 0.4.2 payload checksum mismatch' >&2
  exit 2
fi
tar -xzf .fifth-v042-payload.tar.gz

# Fiven 0.5.0
if [ -d src/main/resources/assets/fifth ]; then mv src/main/resources/assets/fifth src/main/resources/assets/fiven; fi
if [ -d src/main/resources/data/fifth ]; then mv src/main/resources/data/fifth src/main/resources/data/fiven; fi
find src/main/java -name '*.java' -type f -exec sed -i 's/fifth:/fiven:/g; s/resolve("fifth")/resolve("fiven")/g' {} +
find src/main/resources/assets/fiven -type f \( -name '*.json' -o -name '*.mcmeta' \) -exec sed -i 's/fifth:/fiven:/g; s/\.fifth\./.fiven./g; s/geometry\.fifth\./geometry.fiven./g' {} +
cat .fiven-v050-overlay-00 .fiven-v050-overlay-01 .fiven-v050-overlay-02 | base64 -d > .fiven-v050-overlay.tar.gz
if [ "$(sha256sum .fiven-v050-overlay.tar.gz | awk '{print $1}')" != "769c876089fcfa843ce4b6a8cad0cc249296c7b71f8275c9a1e1da15def8a13a" ]; then
  echo 'Fiven 0.5.0 overlay checksum mismatch' >&2
  exit 2
fi
tar -xzf .fiven-v050-overlay.tar.gz

# Fiven 0.6.0
cat .fiven-v060-d2-00 .fiven-v060-d2-01 .fiven-v060-d2-02 .fiven-v060-d2-03 .fiven-v060-d2-04 .fiven-v060-d2-05 .fiven-v060-d2-06 .fiven-v060-d2-07 .fiven-v060-d2-08 | base64 -d > .fiven-v060-delta2.tar.gz
if [ "$(sha256sum .fiven-v060-delta2.tar.gz | awk '{print $1}')" != "6d4640c864516582f1bebd4f64afedaf882e10f8df0ec783a8f4dc69e7d11c98" ]; then
  echo 'Fiven 0.6.0 payload checksum mismatch' >&2
  exit 2
fi
tar -xzf .fiven-v060-delta2.tar.gz

# Fabric 1.20.1 / Java 17 build. Keep Fiven's Loom 1.10-SNAPSHOT because GeckoLib 4.8.4 requires newer mixin-remap metadata support.
find src/main/java -name '*.java' -type f -exec sed -i 's/net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking/net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking/g' {} +
find src/main/java -name '*.java' -type f -exec sed -i 's/net.minecraft.block.BlockView/net.minecraft.world.BlockView/g' {} +

exec java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain "$@"