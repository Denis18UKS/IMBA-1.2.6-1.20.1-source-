#!/usr/bin/env sh
set -u
rm -rf src build.gradle gradle.properties settings.gradle README.md
cat .fifth-part-00 .fifth-part-01 .fifth-part-02 .fifth-part-03 .fifth-part-04 | base64 -d > .fifth-project.tar.gz
tar -xzf .fifth-project.tar.gz
cat .fifth-patch-v2-00 .fifth-patch-v2-01 .fifth-patch-v2-02 | base64 -d > .fifth-patch-v2.tar.gz
tar -xzf .fifth-patch-v2.tar.gz
sed -i "s/net.fabricmc:fabric-loom:1.3.10/net.fabricmc:fabric-loom:1.10-SNAPSHOT/" build.gradle
find src/main/java -name '*.java' -type f -exec sed -i 's/net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking/net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking/g' {} +
find src/main/java -name '*.java' -type f -exec sed -i 's/net.minecraft.block.BlockView/net.minecraft.world.BlockView/g' {} +
java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain "$@"
