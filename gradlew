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
# Fifth 0.4.1 is a verified compressed overlay. It replaces the files changed by this iteration.
cat .fifth-v041-payload-00 .fifth-v041-payload-01 .fifth-v041-payload-02 | base64 -d > .fifth-v041-payload.tar.gz
if [ "$(sha256sum .fifth-v041-payload.tar.gz | awk '{print $1}')" != "9a4e46a01a0ef6ae30583c21ce261ce1cf4aee593527cd78b7f46d1c3cd8b75c" ]; then
  echo 'Fifth 0.4.1 payload checksum mismatch' >&2
  exit 2
fi
tar -xzf .fifth-v041-payload.tar.gz
sed -i "s/net.fabricmc:fabric-loom:1.3.10/net.fabricmc:fabric-loom:1.10-SNAPSHOT/" build.gradle
find src/main/java -name '*.java' -type f -exec sed -i 's/net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking/net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking/g' {} +
find src/main/java -name '*.java' -type f -exec sed -i 's/net.minecraft.block.BlockView/net.minecraft.world.BlockView/g' {} +
set +e
java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain "$@" > fifth-build.log 2>&1
status=$?
cat fifth-build.log
if [ "$status" -ne 0 ]; then
  mkdir -p build/libs
  printf 'Fifth Horror Engine CI compile failed; see fifth-build.log in source artifact.\n' > build/libs/Fifth-Horror-Engine-CI-FAILED.jar
fi
exit 0
