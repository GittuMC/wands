package net.nicguzzo.wands;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.function.Consumer;

import io.netty.buffer.Unpooled;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.NbtType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nicguzzo.wands.PaletteItem.PaletteMode;
import net.nicguzzo.wands.WandItem.Orientation;

public class Wand {
    public float x = 0.0f;
    public float y = 0.0f;
    public float z = 0.0f;
    public float x1 = 0.0f;
    public float y1 = 0.0f;
    public float z1 = 0.0f;
    public float x2 = 0.0f;
    public float y2 = 0.0f;
    public float z2 = 0.0f;
    public BlockPos p1 = null;
    public boolean  p2=false;
    public BlockState p1_state = null;    
    
    public boolean valid = false;
    public static final int MAX_UNDO = 2048;
    public static final int MAX_LIMIT = 32768;    
    Player player;
    Level level;
    BlockState block_state;
    BlockState offhand_state=null;
    BlockPos pos;
    public Direction side = Direction.UP;
    Vec3 hit;
    WandItem wand_item;
    ItemStack wand_stack;
    public float y0 = 0.0f;
    public float block_height = 1.0f;
    boolean is_slab_top = false;
    boolean is_slab_bottom = false;
    boolean destroy;
    
    public boolean is_double_slab=false;
    public ItemStack palette=null;
    private class PaletteSlot{
        public ItemStack stack=null;
        public BlockState state=null;
        public int slot=0;
        PaletteSlot(int s,BlockState b,ItemStack stk){
            slot=s;
            state=b;
            stack=stk;
        }
    }
    private class PaletteAccounting{
        public int placed=0;
        public int needed=0;
        public int in_player=0;
    }
    public int slot=0;
    public Vector<PaletteSlot> palette_slots= new Vector<PaletteSlot>();
    public Map<Item,PaletteAccounting> palette_accounting= new HashMap<Item,PaletteAccounting>();
    public BlockBuffer block_buffer=new BlockBuffer(MAX_LIMIT);
    public CircularBuffer undo_buffer = new CircularBuffer(MAX_UNDO);

    private BlockPos.MutableBlockPos tmp_pos=new BlockPos.MutableBlockPos();

    int MAX_COPY_VOL=20*20*20;
    class CopyPasteBuffer{
        public BlockPos pos=null;
        public BlockState state=null;
        public CopyPasteBuffer(BlockPos pos,BlockState state){
            this.pos=pos;
            this.state=state;
        }
    }
    Vector<CopyPasteBuffer> copy_paste_buffer=new Vector<CopyPasteBuffer>();
    public BlockPos copy_pos1=null;
    public BlockPos copy_pos2=null;
    //public boolean  copied=false;
    boolean preview;
    public int mode;
    boolean prnt=false;
    private void log(String s){
        WandsMod.log(s,prnt);
    }

    public void clear() {
        p1 = null;
        p1_state=null;
        valid = false;
        block_height = 1.0f;
        y0 = 0.0f;
        //log("wand cleared");
        copy_pos1=null;
        copy_pos2=null;
        //copied=false;
        copy_paste_buffer.clear();
        if(player!=null)
            player.displayClientMessage(new TextComponent("Wand Cleared").withStyle(ChatFormatting.GREEN),false);
    }

    public void do_or_preview(
        Player player,
        Level level,
        BlockState block_state,
        BlockPos pos,
        Direction side,
        Vec3 hit,
        ItemStack wand_stack,
        boolean prnt) {
        this.player=player;
        this.level=level;
        this.block_state=block_state;
        this.pos=pos;
        this.side=side;
        this.hit=hit;
        this.wand_stack=wand_stack;
        this.prnt=prnt;
        y0 =0.0f;
        block_height = 1.0f;
        is_slab_top=false;
        is_double_slab=false;
        is_slab_bottom=false;
        preview = level.isClientSide();
        if (!preview) {
            log("server");
        }
        if(block_state==null || pos==null || side==null || level==null || player==null || hit==null|| wand_stack==null){
            return;
        }

        //TODO: copy/paste mode
        //TODO: show preview only if there's enough items
        //TODO: add water source if water bucket on offhand
        //TODO: allow/deny list
        // TODO: make snow add layers if there's snow already
        boolean creative = player.getAbilities().instabuild;


        wand_item = (WandItem) wand_stack.getItem();
        mode = WandItem.getMode(wand_stack);
        
        if (block_state.getBlock() instanceof SlabBlock) {
            if (!preview) {
                is_double_slab = block_state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE;
            }
            is_slab_top = block_state.getValue(SlabBlock.TYPE) == SlabType.TOP;
            is_slab_bottom = block_state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM;
        } else {
            if (block_state.getBlock() instanceof CrossCollisionBlock) {
                if (mode == 0) {
                    valid=false;
                    valid=false;
                    if(!preview){
                        player.displayClientMessage(new TextComponent("can't place this item on this mode"),false);
                    }
                    return;
                }
            }else{

                if (block_state.getBlock() instanceof SnowLayerBlock) {
                    //SnowLayerBlock snow=(SnowLayerBlock)block_state.getBlock();
                    int layers=block_state.getValue(SnowLayerBlock.LAYERS);
                    block_height=layers/8.0f;
                    if(layers<8){
                        //valid=false;
                        //return;
                    }
                }
            }
        }
        
        if (is_slab_top || is_slab_bottom) {
            block_height = 0.5f;
            if (is_slab_top) {
                y0 = 0.5f;
            }
        }
        //WandsMod.log("block_height "+block_height,prnt);
        valid = false;
        this.destroy = WandUtils.can_destroy(player, block_state,false);

        ItemStack offhand = player.getOffhandItem();
        ItemStack item_stack = null;

        if (WandUtils.is_shulker(offhand)) {
            valid=false;
            if(!preview){
                player.displayClientMessage(new TextComponent("offhand can't be a shulkerbox! "),false);
            }
            return;
        }
        palette = null;
        boolean has_palette=false;
        if (!preview) {
            if (offhand.getItem() instanceof PaletteItem) {
                if (mode > 0) {
                    palette = offhand;
                    has_palette=true;
                }
            }
        }
        if (item_stack == null) {
            item_stack = Item.byBlock(block_state.getBlock()).getDefaultInstance();
        }
        int placed = 0;
        switch (mode) {
            case 0:
                boolean invert = WandItem.isInverted(wand_stack);
                placed += mode0(invert);
                break;
            case 1:
                Orientation orientation = WandItem.getOrientation(wand_stack);
                placed += mode1(orientation);
                break;
            case 2:
                placed += mode2();
                break;
            case 3:
                placed += mode3();
                break;
            case 4:
                placed += mode4();
                break;
            case 5:
                int plane = WandItem.getPlane(wand_stack).ordinal();
                boolean fill = WandItem.isCircleFill(wand_stack);
                placed += mode5(plane, fill);
                break;
            case 6:
                placed += mode6();
                break;

        }

        if (!preview ) {
            int limit = MAX_LIMIT;
            if (!creative) {
                limit = wand_item.limit;
            }
            log("has_palette: "+has_palette);
            palette_slots.clear();
            palette_accounting.clear();
            if (has_palette && !destroy) {

                ListTag palette_inv = palette.getOrCreateTag().getList("Palette", NbtType.COMPOUND);
                //log("palette_inv: "+palette_inv);
                int s = palette_inv.size();
                for (int i = 0; i < s; i++) {
                    CompoundTag stackTag = (CompoundTag) palette_inv.get(i);
                    ItemStack stack = ItemStack.of(stackTag.getCompound("Block"));
                    if (!stack.isEmpty()) {
                        Block blk = Block.byItem(stack.getItem());
                        if (blk != Blocks.AIR) {
                            PaletteSlot psl=new PaletteSlot(i,blk.defaultBlockState(),stack);

                            if(!palette_slots.stream().anyMatch(pp-> (pp.stack.sameItem(stack)) ) ) {
                                palette_accounting.put(stack.getItem(),new PaletteAccounting());
                            }
                            palette_slots.add(psl);
                        }
                    }
                }
                log("palette: "+palette_slots);
                log("palette u: "+palette_accounting);
                PaletteMode palette_mode = PaletteItem.getMode(palette);
                CompoundTag tag = palette.getTag();
                slot=0;
                for (int a = 0; a < block_buffer.get_length() && a < limit && a < MAX_LIMIT; a++) {
                    if (!WandUtils.can_place(player.level.getBlockState(block_buffer.get(a)), wand_item.removes_water, wand_item.removes_lava)) {
                        block_buffer.state[a] = null;
                        continue;
                    }
                    int bound = palette_slots.size();
                    if (palette_mode == PaletteMode.RANDOM) {
                        slot = level.random.nextInt(bound);
                    } else if (palette_mode == PaletteMode.ROUND_ROBIN) {
                        slot = (slot + 1) % bound;
                    }
                    PaletteSlot ps=palette_slots.get(slot);
                    PaletteAccounting pa=palette_accounting.get(ps.stack.getItem());
                    if(pa==null){
                        log("no palette accounting found for "+ps.stack.getItem());
                        continue;
                    }
                    pa.needed++;

                    if (!ps.stack.isEmpty()) {

                        block_buffer.state[a]=ps.state;
                        block_buffer.item[a]=ps.stack.getItem();
                        if (ps.state.getBlock() instanceof SlabBlock) {
                            if (ps.state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
                                pa.needed++;
                            }
                        }
                        if (palette_mode == PaletteItem.PaletteMode.RANDOM) {
                            if (ps.state.getBlock() instanceof SnowLayerBlock) {
                                int sn=player.level.random.nextInt(7);
                                pa.needed+=sn;
                                block_buffer.state[a] = ps.state.setValue(SnowLayerBlock.LAYERS, sn+1);
                            }
                            if (PaletteItem.getRotate(palette)) {
                                block_buffer.state[a] = ps.state.getBlock().rotate(ps.state, Rotation.getRandom(level.random));
                            }
                        }
                    } else {
                        block_buffer.state[a] = null;
                        block_buffer.item[a] = null;
                    }

                }
            }else{
                if (!has_palette && !destroy && !offhand.isEmpty()) {
                    Block offhand_block = Block.byItem(offhand.getItem());
                    if (offhand_block != Blocks.AIR) {
                        block_state = offhand_block.defaultBlockState();
                    }
                }

                palette_slots.add(new PaletteSlot(0,block_state,item_stack));
                palette_accounting.put(item_stack.getItem(),new PaletteAccounting());
            }
            log("block_state "+block_state);
            log( "palette_slots "+palette_slots.size());
            boolean missing_blocks=palette_slots.size()==0;
            if(!creative && !destroy)
            {
                //int count[] = WandUtils.count_in_player(player, item_stack);
                //int count_in_player = count[0];
                //int count_in_shulker = count[1];
                for (var pa : palette_accounting.entrySet()) {
                    log(pa.getKey()+" in player "+pa.getValue().in_player);
                }
                ItemStack stack;
                for (int i = 0; i < 36; ++i) {
                    stack = player.getInventory().getItem(i);
                    if (WandUtils.is_shulker(stack)) {
                        //count_in_shulker += count_in_shulker(stack, item_stack);
                        for (var pa : palette_accounting.entrySet()) {
                            pa.getValue().in_player +=WandUtils.count_in_shulker(stack, pa.getKey());
                        }
                    } else {
                        for (var pa : palette_accounting.entrySet()) {
                            Item item = pa.getKey();
                            if (stack != null && item != null && !stack.isEmpty() && item == stack.getItem()) {
                                pa.getValue().in_player += stack.getCount();
                            }
                        }
                    }
                }
                for (var pa : palette_accounting.entrySet()) {
                    if (pa.getValue().in_player < pa.getValue().needed) {
                        MutableComponent mc = new TextComponent("Not enough ").withStyle(ChatFormatting.RED).append(pa.getKey().getDescriptionId());
                        mc.append(" needed: " + pa.getValue().needed);
                        mc.append(" in player: " + pa.getValue().in_player);
                        player.displayClientMessage(mc, false);
                        missing_blocks = true;
                    }
                    log(pa.getKey().getDescriptionId()+" needed: "+pa.getValue().needed+" player has: "+pa.getValue().in_player);
                }
            }
            log("block_buffer.length: " + block_buffer.get_length());
            log("wand limit: " + wand_item.limit);
            log( "limit "+limit);
            log( "missing_blocks "+missing_blocks);
            placed=0;
            int a=0;
            if(!missing_blocks || destroy) {
                AABB bb = player.getBoundingBox();
                for (a = 0; a < block_buffer.get_length() && a < limit && a < MAX_LIMIT; a++) {
                    tmp_pos.set(block_buffer.buffer_x[a], block_buffer.buffer_y[a], block_buffer.buffer_z[a]);
                    if (has_palette) {
                        block_state = block_buffer.state[a];
                    }
                    if (!destroy && bb.intersects(tmp_pos.getX(), tmp_pos.getY(), tmp_pos.getZ(), tmp_pos.getX() + 1, tmp_pos.getY() + 1, tmp_pos.getZ() + 1)) {
                        continue;
                    }
                    if (place_block(tmp_pos, block_state)) {
                        if(!destroy) {
                            Item item = block_buffer.item[a];
                            if (item != null) {
                                PaletteAccounting pa = palette_accounting.get(item);
                                if (pa != null) {
                                    pa.placed++;
                                }
                            }
                        }
                        placed++;
                    }
                }

                if (!creative && !destroy && placed>0) {
                    ItemStack stack = null;
                    ItemStack stack_item = null;
                    //look for items on shulker boxes first
                    for (int pi = 0; pi < 36; ++pi) {
                        stack = player.getInventory().getItem(pi);
                        if (WandUtils.is_shulker(stack)) {
                            CompoundTag shulker_tag = stack.getTagElement("BlockEntityTag");
                            if (shulker_tag != null) {
                                ListTag shulker_items = shulker_tag.getList("Items", 10);
                                for (int j = 0, len = shulker_items.size(); j < len; ++j) {
                                    CompoundTag itemTag = shulker_items.getCompound(j);
                                    stack_item = ItemStack.of(itemTag);
                                    if (stack_item != null && !stack_item.isEmpty()) {
                                        PaletteAccounting pa=palette_accounting.get(stack_item.getItem());
                                        if(pa!=null && pa.placed>0){
                                            if(pa.placed<=stack_item.getCount()){
                                                stack_item.setCount(stack_item.getCount() - pa.placed);
                                                pa.placed = 0;
                                            }else{
                                                stack_item.setCount(0);
                                                pa.placed = pa.placed - stack_item.getCount();
                                            }
                                            shulker_items.set(j, stack_item.save(itemTag));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    //now look for items on player inv
                    for (int i = 0; i < 36; ++i) {
                        stack_item = player.getInventory().getItem(i);
                        if (!WandUtils.is_shulker(stack_item)) {
                            PaletteAccounting pa=palette_accounting.get(stack_item.getItem());
                            if(pa!=null && pa.placed>0){
                                if(pa.placed<=stack_item.getCount()){
                                    stack_item.setCount(stack_item.getCount() - pa.placed);
                                    pa.placed = 0;
                                }else{
                                    stack_item.setCount(0);
                                    pa.placed = pa.placed - stack_item.getCount();
                                }
                            }
                        }
                    }
                }
            }
            //log("a: " + a);
            log("placed: " + placed);
            if (placed > 0) {
                //log("placed: " + placed);

                FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
                packet.writeBlockPos(pos);
                packet.writeBoolean(destroy);
                if (p1_state != null) {
                    packet.writeItem(p1_state.getBlock().asItem().getDefaultInstance());
                } else {
                    if (block_state != null) {
                        packet.writeItem(block_state.getBlock().asItem().getDefaultInstance());
                    }
                }
                NetworkManager.sendToPlayer((ServerPlayer) player, WandsMod.SND_PACKET, packet);
            }
        }
        if(p2){
            p1 = null;
            p2=false;
            valid=false;
        }
    }

    public int mode0(boolean invert) {
        
        Direction dirs[] = getDirMode0(side, y0, block_height, hit.x, hit.y, hit.z);
        if (invert) {
            if (dirs[0] != null)
                dirs[0] = dirs[0].getOpposite();
            if (dirs[1] != null)
                dirs[1] = dirs[1].getOpposite();
        }
        Direction d1 = dirs[0];
        Direction d2 = dirs[1];
        if (preview) {
            x = pos.getX();
            y = pos.getY();
            z = pos.getZ();
            float o = 0.01f;
            switch (side) {
                case UP:
                    y += block_height + o;
                    break;
                case DOWN:
                    y -= o;
                    break;
                case SOUTH:
                    z += 1 + o;
                    break;
                case NORTH:
                    z -= o;
                    break;
                case EAST:
                    x += 1 + o;
                    break;
                case WEST:
                    x -= o;
                    break;
            }
            //x = x;
            //y = y;
            //z = z;
            //side = side;
            //h = block_height;
            //y0 = y0;
        }
        if (d1 != null) {
            BlockPos dest = null;
            if (d2 != null) {
                dest = WandUtils.find_next_diag(player.level, block_state, d1, d2, pos, wand_item, destroy, offhand_state);
            } else {
                dest = WandUtils.find_next_pos(player.level, block_state, d1, pos, wand_item, destroy, offhand_state);
            }
            if (dest != null) {
                if (preview) {
                    x1 = dest.getX();
                    y1 = dest.getY();
                    z1 = dest.getZ();
                    x2 = x1 + 1;
                    y2 = y1 + 1;
                    z2 = z1 + 1;
                    valid = true;
                } else {
                    block_buffer.reset();
                    block_buffer.add(dest);
                    //if (place_block(dest,block_state)) {
//                        return 1;
//                    }
                }
            }
        }
        return 0;
    }
    public int mode1(Orientation orientation) {
        boolean preview = player.level.isClientSide();
        Direction dir = Direction.EAST;
        BlockPos pos_m = pos.relative(side, 1);
        BlockState state = player.level.getBlockState(pos_m);
        WandItem wand = (WandItem) wand_stack.getItem();

        if (state.isAir() || WandUtils.is_fluid(state, wand.removes_water, wand.removes_lava) || destroy) {
            BlockPos pos0 = pos;
            BlockPos pos1 = pos_m;
            BlockPos pos2 = pos;
            BlockPos pos3 = pos_m;
            int offx = 0;
            int offy = 0;
            int offz = 0;
            
            switch (side) {
                case UP:
                case DOWN:
                    switch (orientation) {
                        case HORIZONTAL:
                            dir = Direction.SOUTH;
                            offz = -1;
                            break;
                        case VERTICAL:
                            dir = Direction.EAST;
                            offx = -1;
                            break;
                    }
                    break;
                case SOUTH:
                case NORTH:
                    switch (orientation) {
                        case HORIZONTAL:
                            dir = Direction.EAST;
                            offx = -1;
                            break;
                        case VERTICAL:
                            dir = Direction.UP;
                            offy = -1;
                            break;
                    }
                    break;
                case EAST:
                case WEST:
                    switch (orientation) {
                        case HORIZONTAL:
                            dir = Direction.SOUTH;
                            offz = -1;
                            break;
                        case VERTICAL:
                            dir = Direction.UP;
                            offy = -1;
                            break;
                    }
                    break;
            }

            Direction op = dir.getOpposite();
            int i = wand.limit - 1;
            int k = 0;
            boolean stop1 = false;
            boolean stop2 = false;
            // boolean intersects = false;

            boolean dont_check_state = false;
            boolean eq = false;
            while (k < wand.limit && i > 0) {
                if (!stop1 && i > 0) {
                    BlockState bs0 = player.level.getBlockState(pos0.relative(dir));
                    BlockState bs1 = player.level.getBlockState(pos1.relative(dir));
                    if (dont_check_state) {
                        eq = bs0.getBlock().equals(block_state.getBlock());
                    } else {
                        eq = bs0.equals(block_state);
                    }
                    if (eq && (bs1.isAir() || WandUtils.is_fluid(bs1, wand.removes_water, wand.removes_lava))) {
                        pos0 = pos0.relative(dir);
                        pos1 = pos1.relative(dir);
                        i--;
                    } else {
                        stop1 = true;
                    }
                }
                if (!stop2 && i > 0) {
                    BlockState bs2 = player.level.getBlockState(pos2.relative(op));
                    BlockState bs3 = player.level.getBlockState(pos3.relative(op));
                    if (dont_check_state) {
                        eq = bs2.getBlock().equals(block_state.getBlock());
                    } else {
                        eq = bs2.equals(block_state);
                    }
                    if (eq && (bs3.isAir() || WandUtils.is_fluid(bs3, wand.removes_water, wand.removes_lava))) {
                        pos2 = pos2.relative(op);
                        pos3 = pos3.relative(op);
                        i--;
                    } else {
                        stop2 = true;
                    }
                }
                k++;
                if (stop1 && stop2) {
                    k = 1000000;
                }
            }
            if (destroy) {
                pos1 = pos1.relative(side.getOpposite());
                pos3 = pos3.relative(side.getOpposite());
            }
            if (preview) {
                x1 = pos1.getX() - offx;
                y1 = pos1.getY() - offy;
                z1 = pos1.getZ() - offz;
                x2 = pos3.getX() + offx + 1;
                y2 = pos3.getY() + offy + 1;
                z2 = pos3.getZ() + offz + 1;
                valid = true;
            } else {
                return fill(pos1, pos3);
            }
        } else {
            valid = false;
        }
        return 0;
    }
    public int mode2() {
        int placed=0;
        if (p1 != null  &&  (p2||preview)) {
            valid = true;
            x1 = p1.getX();
            y1 = p1.getY();
            z1 = p1.getZ();
            x2 = pos.getX();
            y2 = pos.getY();
            z2 = pos.getZ();
            if (!p1.equals(pos)) {
                if (x1 >= x2) {
                    x1 += 1;
                } else {
                    x2 += 1;
                }
                if (y1 >= y2) {
                    y1 += 1;
                } else {
                    y2 += 1;
                }
                if (z1 >= z2) {
                    z1 += 1;
                } else {
                    z2 += 1;
                }
            } else {
                x2 = x1 + 1;
                y2 = y1 + 1;
                z2 = z1 + 1;
            }
            placed = fill(p1,pos);
        }
        
        return placed;
    }
    public int mode3() {
        block_buffer.reset();
        WandUtils.add_neighbour(block_buffer, wand_item, pos, block_state, player.level, side);
        int i = 0;
        int placed = 0;
        int found=1;
        while (i < wand_item.limit && i < MAX_LIMIT && found< wand_item.limit) {
            if (i < block_buffer.get_length()) {
                BlockPos p = block_buffer.get(i).relative(side, -1);
                found+=WandUtils.find_neighbours(block_buffer, wand_item, p, block_state, player.level, side);
            }
            i++;
        }
        log("found: "+found);
        if (destroy) {
            for (int a = 0; a < block_buffer.get_length(); a++) {
                block_buffer.set(a, block_buffer.get(a).relative(side, -1));
            }
        }
        placed = from_buffer();
        return placed;
    }
    public int mode4() {
        block_buffer.reset();
        if (p1 != null  &&  (p2||preview)) {
            int x1 = p1.getX();
            int y1 = p1.getY();
            int z1 = p1.getZ();
            int x2 = pos.getX();
            int y2 = pos.getY();
            int z2 = pos.getZ();
            int n=0;
            int dx, dy, dz, xs, ys, zs, lp1, lp2;
            dx = Math.abs(x2 - x1);
            dy = Math.abs(y2 - y1);
            dz = Math.abs(z2 - z1);
            if (x2 > x1) {
                xs = 1;
            } else {
                xs = -1;
            }
            if (y2 > y1) {
                ys = 1;
            } else {
                ys = -1;
            }
            if (z2 > z1) {
                zs = 1;
            } else {
                zs = -1;
            }
            block_buffer.add(x1, y1, z1);
            n++;
            // X
            if (dx >= dy && dx >= dz) {
                lp1 = 2 * dy - dx;
                lp2 = 2 * dz - dx;
                while (x1 != x2) {
                    x1 += xs;
                    if (lp1 >= 0) {
                        y1 += ys;
                        lp1 -= 2 * dx;
                    }
                    if (lp2 >= 0) {
                        z1 += zs;
                        lp2 -= 2 * dx;
                    }
                    lp1 += 2 * dy;
                    lp2 += 2 * dz;
                    block_buffer.add(x1, y1, z1);
                    n++;
                    if(n>= wand_item.limit)
                        break;
                }
            } else if (dy >= dx && dy >= dz) {
                lp1 = 2 * dx - dy;
                lp2 = 2 * dz - dy;
                while (y1 != y2) {
                    y1 += ys;
                    if (lp1 >= 0) {
                        x1 += xs;
                        lp1 -= 2 * dy;
                    }
                    if (lp2 >= 0) {
                        z1 += zs;
                        lp2 -= 2 * dy;
                    }
                    lp1 += 2 * dx;
                    lp2 += 2 * dz;
                    block_buffer.add(x1, y1, z1);
                    n++;
                    if(n>= wand_item.limit)
                        break;
                }
            } else {
                lp1 = 2 * dy - dz;
                lp2 = 2 * dx - dz;
                while (z1 != z2) {
                    z1 += zs;
                    if (lp1 >= 0) {
                        y1 += ys;
                        lp1 -= 2 * dz;
                    }
                    if (lp2 >= 0) {
                        x1 += xs;
                        lp2 -= 2 * dz;
                    }
                    lp1 += 2 * dy;
                    lp2 += 2 * dx;
                    block_buffer.add(x1, y1, z1);
                    n++;
                    if(n>= wand_item.limit)
                        break;
                }
            }
        }
        return from_buffer();
    }
    public int mode5(int plane, boolean fill) {
        block_buffer.reset();

        if (p1 != null && (p2||preview)) {
            int xc = p1.getX();
            int yc = p1.getY();
            int zc = p1.getZ();
            int px = pos.getX() - xc;
            int py = pos.getY() - yc;
            int pz = pos.getZ() - zc;            
            // log("circle plane:"+plane+ " fill: "+fill);
            int r = (int) Math.sqrt(px * px + py * py + pz * pz);

            if (plane == 0) {// XZ;
                int x = 0, y = 0, z = r;
                int d = 3 - 2 * r;
                drawCircle(xc, yc, zc, x, y, z, plane);

                while (z >= x) {
                    x++;
                    if (d > 0) {
                        z--;
                        d = d + 4 * (x - z) + 10;
                    } else
                        d = d + 4 * x + 6;
                    drawCircle(xc, yc, zc, x, y, z, plane);
                }
                if (fill) {
                    int r2 = r * r;
                    
                    for (z = -r; z <= r; z++) {
                        for (x = -r; x <= r; x++) {
                            int det = (x * x) + (z * z);
                            if (det <= r2) {
                                block_buffer.add(xc + x, yc, zc + z);
                            }
                        }
                    }
                }
            } else if (plane == 1) {// XY;
                int x = 0, y = r, z = 0;
                int d = 3 - 2 * r;
                drawCircle(xc, yc, zc, x, y, z, plane);
                while (y >= x) {
                    x++;
                    if (d > 0) {
                        y--;
                        d = d + 4 * (x - y) + 10;
                    } else
                        d = d + 4 * x + 6;
                    drawCircle(xc, yc, zc, x, y, z, plane);
                }
                if (fill) {
                    int r2 = r * r;                    
                    for (y = -r; y <= r; y++) {
                        for (x = -r; x <= r; x++) {
                            if ((x * x) + (y * y) <= r2) {
                                block_buffer.add(xc + x, yc + y, zc);
                            }
                        }
                    }
                }
            } else if (plane == 2) {// YZ;
                int x = 0, y = 0, z = r;
                int d = 3 - 2 * r;
                drawCircle(xc, yc, zc, x, y, z, plane);
                while (z >= y) {
                    y++;
                    if (d > 0) {
                        z--;
                        d = d + 4 * (y - z) + 10;
                    } else
                        d = d + 4 * y + 6;
                    drawCircle(xc, yc, zc, x, y, z, plane);
                }
                if (fill) {
                    int r2 = r * r;                    
                    for (z = -r; z <= r; z++) {
                        for (y = -r; y <= r; y++) {
                            if ((y * y) + (z * z) <= r2) {
                                block_buffer.add(xc, yc + y, zc + z);
                            }
                        }
                    }
                }
            }
        }
        return from_buffer();
    }
    public int mode6(){
        int placed=0;
        if(!preview){
            WandsMod.log("mode6 copy_pos1: "+copy_pos1+" copy_pos2: "+copy_pos2 + " copy_paste_buffer: "+copy_paste_buffer.size(),prnt);
        }
        if (copy_pos1 != null  &&  preview) {
            
            valid = true;
            x1 = copy_pos1.getX();
            y1 = copy_pos1.getY();
            z1 = copy_pos1.getZ();
            if(copy_pos2==null){
                x2 = pos.getX();
                y2 = pos.getY();
                z2 = pos.getZ();
            }else{
                x2 = copy_pos2.getX();
                y2 = copy_pos2.getY();
                z2 = copy_pos2.getZ();
            }
            if (!copy_pos1.equals(pos)) {
                if (x1 >= x2) {
                    x1 += 1;
                } else {
                    x2 += 1;
                }
                if (y1 >= y2) {
                    y1 += 1;
                } else {
                    y2 += 1;
                }
                if (z1 >= z2) {
                    z1 += 1;
                } else {
                    z2 += 1;
                }
            } else {
                x2 = x1 + 1;
                y2 = y1 + 1;
                z2 = z1 + 1;
            }
        }
        /*if (copy_pos1 != null  &&  copy_pos2!=null && !preview ) {
            if(copy_paste_buffer.size()==0){ //copy
                int xs, ys, zs, xe, ye, ze;

                if (copy_pos1.getX() >= copy_pos2.getX()) {
                    xs = copy_pos2.getX();
                    xe = copy_pos1.getX();
                } else {
                    xs = copy_pos1.getX();
                    xe = copy_pos2.getX();
                }
                if (copy_pos1.getY() >= copy_pos2.getY()) {
                    ys = copy_pos2.getY();
                    ye = copy_pos1.getY();
                } else {
                    ys = copy_pos1.getY();
                    ye = copy_pos2.getY();
                }
                if (copy_pos1.getZ() >= copy_pos2.getZ()) {
                    zs = copy_pos2.getZ();
                    ze = copy_pos1.getZ();
                } else {
                    zs = copy_pos1.getZ();
                    ze = copy_pos2.getZ();
                }
            
                WandsMod.log("mode6 copy",true);
                int ll = ((xe - xs) + 1) * ((ye - ys) + 1) * ((ze - zs) + 1);
                if (ll <= MAX_COPY_VOL) {
                    BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
                    for (int z = zs; z <= ze; z++) {
                        for (int y = ys; y <= ye; y++) {
                            for (int x = xs; x <= xe; x++) {
                                bp.set(x, y, z);
                                BlockState bs=level.getBlockState(bp);
                                copy_paste_buffer.add(new CopyPasteBuffer(new BlockPos(x,y,z),bs) );
                                //if () {
                                    //(bp.set(x, y, z)) 
                                    //placed++;
                                //}
                            }
                        }
                    }
                    //copied=true;
                }
            }else{//paste
                WandsMod.log("mode6 paste",true);
                BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
                for (CopyPasteBuffer b: copy_paste_buffer) {
                    block_state=b.state;
                    bp.set(
                        pos.getX()+(pos.getX()-b.pos.getX()), 
                        pos.getY()+(pos.getY()-b.pos.getY()), 
                        pos.getZ()+(pos.getZ()-b.pos.getZ())
                    );
                    if (place_block(bp)) {
                        placed++;
                    }
                }
            }
        }*/
        return placed;
    }
    private void drawCircle( int xc, int yc, int zc, int x, int y, int z, int plane) {
        switch (plane) {
            case 0:// XZ
                block_buffer.add(xc + x, yc, zc + z);
                block_buffer.add(xc - x, yc, zc + z);
                block_buffer.add(xc + x, yc, zc - z);
                block_buffer.add(xc - x, yc, zc - z);
                block_buffer.add(xc + z, yc, zc + x);
                block_buffer.add(xc - z, yc, zc + x);
                block_buffer.add(xc + z, yc, zc - x);
                block_buffer.add(xc - z, yc, zc - x);
                break;
            case 1:// XY
                block_buffer.add(xc + x, yc + y, zc);
                block_buffer.add(xc - x, yc + y, zc);
                block_buffer.add(xc + x, yc - y, zc);
                block_buffer.add(xc - x, yc - y, zc);
                block_buffer.add(xc + y, yc + x, zc);
                block_buffer.add(xc - y, yc + x, zc);
                block_buffer.add(xc + y, yc - x, zc);
                block_buffer.add(xc - y, yc - x, zc);
                break;
            case 2:// YZ
                block_buffer.add(xc, yc + y, zc + z);
                block_buffer.add(xc, yc - y, zc + z);
                block_buffer.add(xc, yc + y, zc - z);
                block_buffer.add(xc, yc - y, zc - z);
                block_buffer.add(xc, yc + z, zc + y);
                block_buffer.add(xc, yc - z, zc + y);
                block_buffer.add(xc, yc + z, zc - y);
                block_buffer.add(xc, yc - z, zc - y);
                break;
        }
    }
    public void undo(int n) {
        if (undo_buffer != null) {
            for (int i = 0; i < n && i < undo_buffer.size(); i++) {
                CircularBuffer.P p = undo_buffer.pop();
                WandsMod.log("undo",true);
                if (p != null) {
                    if (!p.destroyed) {
                        level.setBlockAndUpdate(p.pos, Blocks.AIR.defaultBlockState());
                    } else {
                        level.setBlockAndUpdate(p.pos, p.state);
                    }
                }
            }
            // u.print();
        }
    }
    public void redo(int n) {
        if (undo_buffer != null) {
            for (int i = 0; i < n && undo_buffer.can_go_forward(); i++) {
                undo_buffer.forward();
                CircularBuffer.P p = undo_buffer.peek();
                if (p != null && p.pos != null && p.state != null) {
                    if (!p.destroyed) {
                        level.setBlockAndUpdate(p.pos, p.state);
                    } else {
                        level.setBlockAndUpdate(p.pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
            // u.print();            
        }
    }
    /*public void prepare_palette(){
        if(palette!=null) {
            palette_slots.clear();
            ListTag palette_inv = palette.getOrCreateTag().getList("Palette", NbtType.COMPOUND);
            //log("palette_inv: "+palette_inv);
            int s = palette_inv.size();
            for (int i = 0; i < s; i++) {
                CompoundTag stackTag = (CompoundTag) palette_inv.get(i);
                ItemStack stack = ItemStack.of(stackTag.getCompound("Block"));
                if (!stack.isEmpty()) {
                    Block blk = Block.byItem(stack.getItem());
                    if (blk != Blocks.AIR) {
                        palette_slots.add(new PaletteSlot(i,blk.defaultBlockState()));
                    }
                }
            }
        }
    }
    public ItemStack get_item_from_palette(){
        if(palette==null){
            return ItemStack.EMPTY;
        }
        PaletteMode palatte_mode=PaletteItem.getMode(palette);
        int bound=palette_slots.get(slot).slot;
        if (palatte_mode == PaletteMode.RANDOM) {
            slot = random.nextInt(bound);
        } else if (palatte_mode == PaletteMode.ROUND_ROBIN) {
            slot = (slot + 1) % bound;
        }

        PaletteMode palatte_mode=PaletteItem.getMode(palette);
        CompoundTag tag= palette.getTag();
        ListTag palette_inv = tag.getList("Palette", NbtType.COMPOUND);
        if(palette_slots.size()>0){
            nextSlot(palatte_mode, player.level.random,palette_slots.size());
            CompoundTag stackTag = (CompoundTag) palette_inv.get(slots.get(slot));
            ItemStack stack = ItemStack.of(stackTag.getCompound("Block"));
            if(stack!=ItemStack.EMPTY){
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
    void nextSlot(PaletteMode palatte_mode,Random random,int bound) {
		if (palatte_mode == PaletteMode.RANDOM) {
			slot = random.nextInt(bound);
		} else if (palatte_mode == PaletteMode.ROUND_ROBIN) {
			slot = (slot + 1) % bound;
		}
	}*/
    public int from_buffer() {
        int placed = 0;
        if (preview) {
            //log("block_buffer "+block_buffer.get_length());
            valid = (block_buffer.get_length() > 0) && block_buffer.get_length()<= wand_item.limit;

        } else {
            //for (int a = 0; a < block_buffer.get_length() && a < wand_item.limit && a < MAX_LIMIT; a++) {
            //    if (place_block(block_buffer.get(a))) {
            //        placed++;
            //    }
            //}
        }
        return placed;
    }
    public int fill(BlockPos from, BlockPos to) {
        //log("fill from: "+from+" to: "+to);
        int placed = 0;
        int xs, ys, zs, xe, ye, ze;

        if (from.getX() >= to.getX()) {
            xs = to.getX();
            xe = from.getX();
        } else {
            xs = from.getX();
            xe = to.getX();
        }
        if (from.getY() >= to.getY()) {
            ys = to.getY();
            ye = from.getY();
        } else {
            ys = from.getY();
            ye = to.getY();
        }
        if (from.getZ() >= to.getZ()) {
            zs = to.getZ();
            ze = from.getZ();
        } else {
            zs = from.getZ();
            ze = to.getZ();
        }

        int limit = MAX_LIMIT;
        if (!player.getAbilities().instabuild) {
            limit = wand_item.limit;
        }

        int ll = ((xe - xs) + 1) * ((ye - ys) + 1) * ((ze - zs) + 1);

        if (ll <= limit) {
            block_buffer.reset();
            for (int z = zs; z <= ze; z++) {
                for (int y = ys; y <= ye; y++) {
                    for (int x = xs; x <= xe; x++) {
                        block_buffer.add(x, y, z);
                    }
                }
            }
        }else{
            if(!preview)
                player.displayClientMessage(new TextComponent("Wand limit reached: "+ll+"("+limit + ")" ),false);
        }
        return placed;
    }
    public boolean place_block(BlockPos block_pos,BlockState state) {
        boolean placed = false;
        //log("place_block "+block_pos+" state: "+state + " destroy: " + destroy);
        Level level = player.level;
        boolean creative = player.getAbilities().instabuild;
        if (level.isClientSide) {
            return false;
        }
        if(state==null && !destroy){
            //log("state is null");
            return false;
        }
        p1_state=state;
        if (creative) {
            if (undo_buffer != null) {
                undo_buffer.put(block_pos, state, destroy);
            }
            if (destroy) {
                if (level.destroyBlock(block_pos, false)) {
                    return true;
                }
            } else {
                if (level.setBlockAndUpdate(block_pos, state)) {
                    return true;
                }
            }
        } else {
            //if (destroy)
            {

                float xp = WandUtils.calc_xp(player.experienceLevel, player.experienceProgress);
                float dec = 0.0f;
                float BLOCKS_PER_XP = WandsMod.config.blocks_per_xp;
                if (BLOCKS_PER_XP != 0) {
                    dec = (1.0f / BLOCKS_PER_XP);
                }
                ItemStack wand_stack = player.getMainHandItem();
                ItemStack offhand = player.getOffhandItem();
                int wand_durability = wand_stack.getMaxDamage() - wand_stack.getDamageValue();
                if ((wand_durability > 1 || WandsMod.config.allow_wand_to_break)
                        && (BLOCKS_PER_XP == 0 || (xp - dec) >= 0)) {
                    if (destroy) {
                        BlockState st = level.getBlockState(block_pos);
                        if (WandUtils.can_destroy(player, st,true)) {
                            //log("can destroy: "+st);
                            int offhand_durability = offhand.getMaxDamage() - offhand.getDamageValue();
                            if (offhand_durability > 1 || WandsMod.config.allow_offhand_to_break) {
                                placed = level.destroyBlock(block_pos, false);
                                //log("destroyed: "+placed);
                                if (placed && WandsMod.config.destroy_in_survival_drop) {
                                    int silk_touch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,
                                            offhand);
                                    int fortune = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,
                                            offhand);
                                    if (fortune > 0 || silk_touch > 0) {
                                        st.getBlock().playerDestroy(level, player, pos, st, null, offhand);
                                    }
                                }
                            }
                        }else{
                            //log("can't destroy: "+st);
                        }
                    } else {
                        boolean is_tool = offhand != null && !offhand.isEmpty()
                                && offhand.getItem() instanceof DiggerItem;
                        if (!is_tool) {
                            ItemStack stack2 = null;
                            if (level.setBlockAndUpdate(block_pos, state)) {
                                placed=true;
                            }
                        }
                    }
                }

                if (placed) {
                    if (destroy) {
                        offhand.hurtAndBreak(1, (LivingEntity) player, (Consumer<LivingEntity>) ((p) -> {
                            ((LivingEntity) p).broadcastBreakEvent(InteractionHand.OFF_HAND);
                        }));
                    }
                    wand_stack.hurtAndBreak(1, (LivingEntity) player, (Consumer<LivingEntity>) ((p) -> {
                        ((LivingEntity) p).broadcastBreakEvent(InteractionHand.MAIN_HAND);
                    }));
                    if (BLOCKS_PER_XP != 0) {
                        float diff = WandUtils.calc_xp_to_next_level(player.experienceLevel);
                        float prog = player.experienceProgress;
                        if (diff > 0 && BLOCKS_PER_XP != 0.0f) {
                            float a = (1.0f / diff) / BLOCKS_PER_XP;
                            if (prog - a > 0) {
                                prog = prog - a;
                            } else {
                                if (prog > 0.0f) {
                                    prog = 1.0f + (a - prog);
                                } else {
                                    prog = 1.0f;
                                }
                                if (player.experienceLevel > 0) {
                                    player.experienceLevel--;
                                    diff = WandUtils.calc_xp_to_next_level(player.experienceLevel);
                                    a = (1.0f / diff) / BLOCKS_PER_XP;
                                    if (prog - a > 0) {
                                        prog = prog - a;
                                    }
                                }

                                // WandsMod.compat.send_xp_to_player(player);
                            }
                        }
                    }
                }
            }
        }
        //log("place_block placed: "+placed);
        return placed;
    }

    public Direction[] getDirMode0(Direction side, float y0, float h, double hit_x, double hit_y, double hit_z) {
        Direction ret[] = new Direction[2];
        ret[0] = null;
        ret[1] = null;
        float x = WandUtils.unitCoord((float) hit_x);
        float y = WandUtils.unitCoord((float) hit_y);
        float z = WandUtils.unitCoord((float) hit_z);
        float a = 0.25f;
        float b = 0.75f;
        float a2 = y0 + a * h;
        float b2 = y0 + b * h;
        switch (side) {
            case UP:
            case DOWN:
                if (x >= a && x <= b) {
                    if (z <= a) {
                        ret[0] = Direction.NORTH;
                    } else {
                        if (z >= b) {
                            ret[0] = Direction.SOUTH;
                        } else {
                            ret[0] = side.getOpposite();
                        }
                    }
                } else {
                    if (z >= a && z <= b) {
                        if (x <= a) {
                            ret[0] = Direction.WEST;
                        } else {
                            if (x >= b) {
                                ret[0] = Direction.EAST;
                            }
                        }
                    } else {
                        if (x <= a && z <= a) {
                            ret[0] = Direction.WEST;
                            ret[1] = Direction.NORTH;
                        }
                        if (x >= b && z <= a) {
                            ret[0] = Direction.EAST;
                            ret[1] = Direction.NORTH;
                        }
                        if (x >= b && z >= b) {
                            ret[0] = Direction.EAST;
                            ret[1] = Direction.SOUTH;
                        }
                        if (x <= a && z >= b) {
                            ret[0] = Direction.WEST;
                            ret[1] = Direction.SOUTH;
                        }
                    }
                }
                break;
            case EAST:
            case WEST:

                if (z >= a && z <= b) {
                    if (y <= a2) {
                        ret[0] = Direction.DOWN;
                    } else {
                        if (y >= b2) {
                            ret[0] = Direction.UP;
                        } else {
                            ret[0] = side.getOpposite();
                        }
                    }
                } else {
                    if (y >= a2 && y <= b2) {
                        if (z <= a) {
                            ret[0] = Direction.NORTH;
                            return ret;
                        } else {
                            if (z >= b) {
                                ret[0] = Direction.SOUTH;
                                return ret;
                            }
                        }
                    } else {
                        if (y <= a2 && z <= a) {
                            ret[0] = Direction.DOWN;
                            ret[1] = Direction.NORTH;
                        }
                        if (y >= b2 && z <= a) {
                            ret[0] = Direction.UP;
                            ret[1] = Direction.NORTH;
                        }
                        if (y >= b2 && z >= b) {
                            ret[0] = Direction.UP;
                            ret[1] = Direction.SOUTH;
                        }
                        if (y <= a2 && z >= b) {
                            ret[0] = Direction.DOWN;
                            ret[1] = Direction.SOUTH;
                        }
                    }
                }
                break;
            case NORTH:
            case SOUTH:
                if (x >= a && x <= b) {
                    if (y <= a2) {
                        ret[0] = Direction.DOWN;
                    } else {
                        if (y >= b2) {
                            ret[0] = Direction.UP;
                        } else {
                            ret[0] = side.getOpposite();
                        }
                    }
                } else {
                    if (y >= a2 && y <= b2) {
                        if (x <= a) {
                            ret[0] = Direction.WEST;
                        } else {
                            if (x >= b2) {
                                ret[0] = Direction.EAST;
                            }
                        }
                    } else {
                        if (y <= a2 && x <= a) {
                            ret[0] = Direction.DOWN;
                            ret[1] = Direction.WEST;
                        }
                        if (y >= b2 && x <= a) {
                            ret[0] = Direction.UP;
                            ret[1] = Direction.WEST;
                        }
                        if (y >= b2 && x >= b) {
                            ret[0] = Direction.UP;
                            ret[1] = Direction.EAST;
                        }
                        if (y <= a2 && x >= b) {
                            ret[0] = Direction.DOWN;
                            ret[1] = Direction.EAST;
                        }
                    }
                }
                break;
        }
        return ret;
    }

}
