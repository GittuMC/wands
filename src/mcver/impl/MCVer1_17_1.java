#if MC>="1171"
package net.nicguzzo.wands.mcver.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.MutableComponent;
#if MC>="1190"
import net.minecraft.network.chat.Component;
#else
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
#endif
import net.nicguzzo.wands.PaletteScreenHandler;
import net.nicguzzo.wands.WandScreenHandler;
import net.nicguzzo.wands.WandsMod;
import net.nicguzzo.wands.WandsModClient;

import net.nicguzzo.wands.mcver.MCVer;

import java.util.function.Supplier;

public class MCVer1_17_1 extends MCVer {

    @Override
    public CreativeModeTab create_tab(ResourceLocation res){
        return CreativeTabRegistry.create(res, new Supplier<ItemStack>() {
            @Override
            public ItemStack get() {
                return new ItemStack(WandsMod.DIAMOND_WAND_ITEM.get() );
            }
        });
    }
    @Override
    public boolean is_creative(Player player) {
        return player.getAbilities().instabuild;
    }

    @Override
    public Inventory get_inventory(Player player) {
        return player.getInventory();
    }

    @Override
    public void set_color(float r, float g, float b, float a){
        RenderSystem.setShaderColor(r,g,b,a);
    }

    @Override
    public void set_pos_tex_shader() {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
    }

    @Override
    public void set_texture(ResourceLocation tex){
        RenderSystem.setShaderTexture(0, tex);
    }

    @Override
    public void set_render_quads_block(BufferBuilder bufferBuilder) {
        RenderSystem.setShader(GameRenderer::getBlockShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
    }

    @Override
    public void set_render_quads_pos_tex(BufferBuilder bufferBuilder) {
        RenderSystem.setShader(GameRenderer::getBlockShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
    }
    @Override
    public void set_render_lines(BufferBuilder bufferBuilder) {
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.lineWidth(5.0f);
        bufferBuilder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
    }

    @Override
    public void set_render_quads_pos_col(BufferBuilder bufferBuilder) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    @Override
    public void pre_render(PoseStack poseStack) {
        Minecraft client=Minecraft.getInstance();
        Camera camera = client.gameRenderer.getMainCamera();
        Vec3 c = camera.getPosition();
        //RenderSystem.pushMatrix();

        PoseStack poseStack2 = RenderSystem.getModelViewStack();
        poseStack2.pushPose();
        //if(WandsMod.is_forge)
        if(WandsMod.config.render_last)
        {
            poseStack2.mulPoseMatrix(poseStack.last().pose());
        }
        poseStack2.translate(-c.x,-c.y,-c.z);
        RenderSystem.applyModelViewMatrix();

        //RenderSystem.translatef((float)-c.x,(float) -c.y,(float) -c.z);
    }

    @Override
    public void post_render(PoseStack poseStack) {
        PoseStack poseStack2 = RenderSystem.getModelViewStack();
        poseStack2.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    @Override
    public void send_to_player(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buf) {
        NetworkManager.sendToPlayer(player, id, buf);
    }

    @Override
    public void open_palette(ServerPlayer player, ItemStack paletteItemStack) {
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider(){
            @Override
            public void saveExtraData(FriendlyByteBuf packetByteBuf) {
                packetByteBuf.writeItem(paletteItemStack);
            }
            @Override
            public Component getDisplayName(){
                return translatable(paletteItemStack.getItem().getDescriptionId());
            }
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
                return new PaletteScreenHandler(syncId, inv, paletteItemStack);
            }
        });
    }
    @Override
    public void open_wand_menu(ServerPlayer player, ItemStack wandItemStack) {
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider(){
            @Override
            public void saveExtraData(FriendlyByteBuf packetByteBuf) {
                packetByteBuf.writeItem(wandItemStack);
            }
            @Override
            public Component getDisplayName(){
                return translatable(wandItemStack.getItem().getDescriptionId());
            }
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
                return new WandScreenHandler(syncId, inv, wandItemStack);
            }
        });
    }
    @Override
    public void set_carried(Player player,AbstractContainerMenu menu,ItemStack itemStack){
        menu.setCarried(itemStack);
    }
    @Override
    public ItemStack get_carried(Player player,AbstractContainerMenu menu){
        return menu.getCarried();
    }

    @Override
    public void set_identity(PoseStack m) {
        m.setIdentity();
    }

    @Override
    public boolean shouldRenderFace(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, Direction direction, BlockPos blockPos2) {
        return Block.shouldRenderFace(blockState,blockGetter, blockPos, direction, blockPos2);
    }

    @Override
    public void register_key(KeyMapping k) {
        KeyMappingRegistry.register(k);
    }

    @Override
    public void render_info() {
        ClientGuiEvent.RENDER_HUD.register((pose, delta)->{ WandsModClient.render_wand_info(pose);});
    }

    @Override
    public  MutableComponent translatable(String key){
        #if MC>="1190"
        return Component.translatable(key);
        #else
        return new TranslatableComponent(key);
        #endif
    }
    @Override
    public  MutableComponent literal(String msg){
        #if MC>="1190"
        return Component.literal(msg);
        #else
        return new TextComponent(msg);
        #endif
    }

}
#endif