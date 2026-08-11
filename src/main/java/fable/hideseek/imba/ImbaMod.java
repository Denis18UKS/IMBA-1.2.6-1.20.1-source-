package fable.hideseek.imba;

import fable.hideseek.imba.block.*;
import fable.hideseek.imba.block.entity.StartBlockEntity;
import fable.hideseek.imba.commands.CommandInit;
import fable.hideseek.imba.config.AttachmentConfig;
import fable.hideseek.imba.config.BreakRulesConfig;
import fable.hideseek.imba.config.PortalConfig;
import fable.hideseek.imba.config.GameSettingsConfig;
import fable.hideseek.imba.config.LocationSettingsConfig;
import fable.hideseek.imba.config.MaskBlockConfig;
import fable.hideseek.imba.config.MaskAutoPositionConfig;
import fable.hideseek.imba.config.TeleportConfig;
import fable.hideseek.imba.game.GameManager;
import fable.hideseek.imba.game.GameplayRulesHandler;
import fable.hideseek.imba.item.HideButtonHandler;
import fable.hideseek.imba.item.HideItem;
import fable.hideseek.imba.item.ModelEquipHandler;
import fable.hideseek.imba.item.TeleportToolHandler;
import fable.hideseek.imba.net.MaskNetworking;
import fable.hideseek.imba.net.TeleportToolNetworking;
import fable.hideseek.imba.net.MaskBlockConfigNetworking;
import fable.hideseek.imba.net.MaskAutoPositionNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ImbaMod implements ModInitializer {

        public static final Item HIDE_BUTTON = new HideItem(new Item.Settings());
        public static final Item MODEL_TOKEN = new Item(new Item.Settings().maxCount(1));
        public static final Item PUMPKIN_STEM_ICON = new Item(new Item.Settings().maxCount(1));
        public static final Item POTION_2D = new Item(new Item.Settings().maxCount(1));
        public static final Item TELEPORT_TOOL = new Item(new Item.Settings().maxCount(1));
        public static final Item POTION_OFFSET_TOOL = new Item(new Item.Settings().maxCount(1));
        public static final Item LOCATION_CAMERA = new Item(new Item.Settings().maxCount(1));
        public static final Item MASK_BLOCK_CONFIG_TOOL = new Item(new Item.Settings().maxCount(1));
        public static final Item MASK_AUTOPOSITION_TOOL = new Item(new Item.Settings().maxCount(1));
        public static final Block WATER_MASK = Registry.register(
                        Registries.BLOCK,
                        new Identifier("imba", "water_mask"),
                        new Block(Block.Settings.copy(Blocks.GLASS).nonOpaque()));
        public static final Block LAVA_MASK = Registry.register(
                        Registries.BLOCK,
                        new Identifier("imba", "lava_mask"),
                        new Block(Block.Settings.copy(Blocks.GLASS).nonOpaque().luminance(state -> 15)));
        public static final Block LADDER_MASK = Registry.register(
                        Registries.BLOCK,
                        new Identifier("imba", "ladder_mask"),
                        new Block(Block.Settings.copy(Blocks.LADDER).nonOpaque().luminance(state -> 15)));
        public static final Block POTION_RENDER_BLOCK = Registry.register(
                        Registries.BLOCK,
                        new Identifier("imba", "potion_render_block"),
                        new Block(Block.Settings.copy(Blocks.BREWING_STAND).nonOpaque().noCollision()));
        public static final Block HANGING_LANTERN = Registry.register(
                        Registries.BLOCK,
                        new Identifier("imba", "hanging_lantern"),
                        new net.minecraft.block.LanternBlock(Block.Settings.copy(Blocks.LANTERN)));
        public static final Block SETTINGS_PANEL=Registry.register(Registries.BLOCK,new Identifier("imba","settings_panel"),new SettingsPanelBlock(Block.Settings.copy(Blocks.DARK_OAK_PLANKS)));
        public static final Block START_BLOCK = Registry.register(
                        Registries.BLOCK,
                        new Identifier("imba", "start_block"),
                        new StartBlock(Block.Settings.copy(Blocks.DARK_OAK_PLANKS)));
        public static final BlockEntityType<StartBlockEntity> START_BLOCK_ENTITY = Registry.register(
                        Registries.BLOCK_ENTITY_TYPE,
                        new Identifier("imba", "start_block"),
                        FabricBlockEntityTypeBuilder.create(StartBlockEntity::new, START_BLOCK).build());
        public static final Block INVISIBLE_SIGN = Registry.register(
                        Registries.BLOCK,
                        new Identifier("imba", "invisible_sign"),
                        new InvisibleSignBlock(Block.Settings.copy(Blocks.OAK_SIGN).nonOpaque().noCollision()));
        public static final Block GLOWBERRIES = Registry.register(Registries.BLOCK,new Identifier("imba", "glowberries"),new GlowberriesBlock(Block.Settings.copy(Blocks.OAK_LEAVES)));
        public static final Block GRASS = Registry.register(Registries.BLOCK,new Identifier("imba", "grass"),new GrassBlock(Block.Settings.copy(Blocks.GRASS)));
        public static final Block LADDER = Registry.register(Registries.BLOCK,new Identifier("imba", "ladder"),new LadderBlock(Block.Settings.copy(Blocks.LADDER)));
        public static final Block STONRCUTTER_BLOCK = Registry.register(Registries.BLOCK,new Identifier("imba", "stonrcutter_block"),new StonercutterBlockBlock(Block.Settings.copy(Blocks.STONECUTTER)));
        public static final Block STONRCUTTER_LEZVIE = Registry.register(Registries.BLOCK,new Identifier("imba", "stonrcutter_lezvie"),new StonercutterBlockLezvie(Block.Settings.copy(Blocks.STONECUTTER)));

        @Override
        public void onInitialize() {
                Registry.register(Registries.ITEM, new Identifier("imba", "hide_button"), HIDE_BUTTON);
                Registry.register(Registries.ITEM, new Identifier("imba", "model_token"), MODEL_TOKEN);
                Registry.register(Registries.ITEM, new Identifier("imba", "pumpkin_stem_icon"), PUMPKIN_STEM_ICON);
                Registry.register(Registries.ITEM, new Identifier("imba", "potion_2d"), POTION_2D);
                Registry.register(Registries.ITEM, new Identifier("imba", "teleport_tool"), TELEPORT_TOOL);
                Registry.register(Registries.ITEM, new Identifier("imba", "potion_offset_tool"), POTION_OFFSET_TOOL);
                Registry.register(Registries.ITEM, new Identifier("imba", "location_camera"), LOCATION_CAMERA);
                Registry.register(Registries.ITEM, new Identifier("imba", "mask_block_config_tool"), MASK_BLOCK_CONFIG_TOOL);
                Registry.register(Registries.ITEM, new Identifier("imba", "mask_autoposition_tool"), MASK_AUTOPOSITION_TOOL);

                registerBlockItem("glowberries", GLOWBERRIES);
                registerBlockItem("grass", GRASS);
                registerBlockItem("ladder", LADDER);
                registerBlockItem("stonrcutter_block", STONRCUTTER_BLOCK);
                registerBlockItem("stonrcutter_lezvie", STONRCUTTER_LEZVIE);
                registerBlockItem("hanging_lantern", HANGING_LANTERN);
                registerBlockItem("settings_panel", SETTINGS_PANEL);
                registerBlockItem("start_block", START_BLOCK);
                registerBlockItem("invisible_sign", INVISIBLE_SIGN);

                Registry.register(Registries.ITEM_GROUP, new Identifier("imba", "main"), FabricItemGroup.builder()
                                .displayName(Text.translatable("itemGroup.imba.main"))
                                .icon(() -> new ItemStack(HIDE_BUTTON))
                                .entries((context, entries) -> {
                                        entries.add(HIDE_BUTTON);
                                        entries.add(POTION_2D);
                                        entries.add(TELEPORT_TOOL);
                                        entries.add(POTION_OFFSET_TOOL);
                                        entries.add(LOCATION_CAMERA);
                                        entries.add(MASK_BLOCK_CONFIG_TOOL);
                                        entries.add(MASK_AUTOPOSITION_TOOL);
                                        entries.add(GLOWBERRIES);
                                        entries.add(GRASS);
                                        entries.add(LADDER);
                                        entries.add(STONRCUTTER_BLOCK);
                                        entries.add(STONRCUTTER_LEZVIE);
                                        entries.add(HANGING_LANTERN);
                                        entries.add(SETTINGS_PANEL);
                                        entries.add(START_BLOCK);
                                        entries.add(INVISIBLE_SIGN);
                                }).build());

                PortalConfig.load();
                AttachmentConfig.load();
                BreakRulesConfig.load();
                GameSettingsConfig.load();
                LocationSettingsConfig.load();
                TeleportConfig.load();
                MaskBlockConfig.load();
                MaskAutoPositionConfig.load();

                CommandInit.register();
                MaskNetworking.register();
                TeleportToolNetworking.register();
                MaskBlockConfigNetworking.register();
                MaskAutoPositionNetworking.register();
                HideButtonHandler.register();
                ModelEquipHandler.register();
                TeleportToolHandler.register();
                GameManager.register();
                GameplayRulesHandler.register();
        }

        private void registerBlockItem(String name, Block block) {
                Registry.register(Registries.ITEM, new Identifier("imba", name), new BlockItem(block, new Item.Settings()));
        }
}
