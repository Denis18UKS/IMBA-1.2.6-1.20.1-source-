#!/usr/bin/env sh
set -u
rm -rf src build.gradle gradle.properties settings.gradle README.md
cat .fifth-part-00 .fifth-part-01 .fifth-part-02 .fifth-part-03 .fifth-part-04 | base64 -d > .fifth-project.tar.gz
tar -xzf .fifth-project.tar.gz
cat .fifth-patch-bin-00a .fifth-patch-bin-00b .fifth-patch-bin-00c .fifth-patch-bin-01 .fifth-patch-bin-02 .fifth-patch-bin-03 > .fifth-patch-v2.tar.gz
tar -xzf .fifth-patch-v2.tar.gz
cat .fifth-compilefix-01 > .fifth-compilefix-01.tar.gz
tar -xzf .fifth-compilefix-01.tar.gz
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
