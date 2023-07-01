package net.nicguzzo.wands.wand;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.nicguzzo.wands.WandsMod;
import net.nicguzzo.wands.utils.WandUtils;
import net.nicguzzo.wands.wand.modes.*;

public class WandProps {
    public enum Mode{
        DIRECTION {
            public String toString() {return "wands.modes.direction";}
            public WandMode get_mode(){return new DirectionMode();}
            public boolean can_target_air(){return false;}
            public int n_clicks(){return 1;}
        },
        ROW_COL   {
            public String toString() {return "wands.modes.row_col";}
            public WandMode get_mode(){return new RowColMode();}
            public boolean can_target_air(){return true;}
            public int n_clicks(){return 1;}
        },
        FILL      {
            public String toString() {return "wands.modes.fill";}
            public WandMode get_mode(){return new FillMode();}
            public boolean can_target_air(){return true;}
            public int n_clicks(){return 2;}
        },
        AREA      {
            public String toString() {return "wands.modes.area";}
            public WandMode get_mode(){return new AreaMode();}
            public boolean can_target_air(){return false;}
            public int n_clicks(){return 1;}
        },
        GRID      {
            public String toString() {return "wands.modes.grid";}
            public WandMode get_mode(){return new GridMode();}
            public boolean can_target_air(){return true;}
            public int n_clicks(){return 1;}
        },
        LINE      {
            public String toString() {return "wands.modes.line";}
            public WandMode get_mode(){return new LineMode();}
            public boolean can_target_air(){return true;}
            public int n_clicks(){return 2;}
        },
        CIRCLE    {
            public String toString() {return "wands.modes.circle";}
            public WandMode get_mode(){return new CircleMode();}
            public boolean can_target_air(){return true;}
            public int n_clicks(){return 2;}
        },
        COPY      {
            public String toString() {return "wands.modes.copy";}
            public WandMode get_mode(){return new CopyMode();}
            public boolean can_target_air(){return true;}
            public int n_clicks(){return 2;}
        },
        PASTE     {
            public String toString() {return "wands.modes.paste";}
            public WandMode get_mode(){return new PasteMode();}
            public boolean can_target_air(){return true;}
            public int n_clicks(){return 1;}
        },
        TUNNEL    {
            public String toString() {return "wands.modes.tunnel";}
            public WandMode get_mode(){return new TunnelMode();}
            public boolean can_target_air(){return true;}
            public int n_clicks(){return 1;}
        },
        VEIN      {
            public String toString() {return "wands.modes.vein";}
            public WandMode get_mode(){return new VeinMode();}
            public boolean can_target_air(){return false;}
            public int n_clicks(){return 1;}
        },
        BLAST     {
            public String toString() {return "wands.modes.blast";}
            public WandMode get_mode(){return new BlastMode();}
            public boolean can_target_air(){return true;}
            public int n_clicks(){return 1;}
        };

        public abstract WandMode get_mode();
        public abstract boolean can_target_air();
        public abstract int n_clicks();
    }
    public enum Orientation {
        ROW { public String toString() {return "wands.orientation.row";}},
        COL { public String toString() {return "wands.orientation.col";}};
    }
    public enum Plane {
        XZ,XY,YZ
    }
    public enum MirrorAxis {
        NONE,LEFT_RIGHT,FRONT_BACK
    }
    public enum StateMode {
        CLONE  { public String toString() { return "wands.state_mode.clone";}},
        APPLY  { public String toString() { return "wands.state_mode.apply";}},
        TARGET { public String toString() { return "wands.state_mode.target";}}
    }
    public enum Action{
        PLACE  { public String toString() { return "wands.action.place";   }},
        REPLACE{ public String toString() { return "wands.action.replace"; }},
        DESTROY{ public String toString() { return "wands.action.destroy"; }},
        USE    { public String toString() { return "wands.action.use";     }}
    }
    public enum Flag{
        INVERTED    { public String toString() {return "inverted";}      public boolean get_default(){return false;}; },
        CFILLED     { public String toString() {return "cfill";}         public boolean get_default(){return false;}; },
        EVEN        { public String toString() {return "circle_even";}   public boolean get_default(){return false;}; },
        DIAGSPREAD  { public String toString() {return "diag_spread";}   public boolean get_default(){return false;}; },
        MATCHSTATE  { public String toString() {return "match_state";}   public boolean get_default(){return false;}; },
        INCSELBLOCK { public String toString() {return "inc_sel_block";} public boolean get_default(){return false;}; },
        STAIRSLAB   { public String toString() {return "stair_slab";}    public boolean get_default(){return false;}; },
        RFILLED     { public String toString() {return "rfill";}         public boolean get_default(){return  true;}; },
        TARGET_AIR  { public String toString() {return "target_air";}    public boolean get_default(){return false;}; };
        public abstract boolean get_default();
    }
    public enum Value{
        MULTIPLIER { public String toString() {return  "multiplier" ;}},
        ROWCOLLIM  { public String toString() {return  "row_col_limit" ;}},
        AREALIM    { public String toString() {return  "area_limit" ;}},
        BLASTRAD   { public String toString() {return  "blast_radius" ;}},
        GRIDM      { public String toString() {return  "grid_m" ;}},
        GRIDN      { public String toString() {return  "grid_n" ;}},
        GRIDMS     { public String toString() {return  "grid_msp" ;}},
        GRIDNS     { public String toString() {return  "grid_nsp" ;}},
        GRIDMOFF   { public String toString() {return  "grid_moff" ;}},
        GRIDNOFF   { public String toString() {return  "grid_noff" ;}},
        MIRRORAXIS { public String toString() {return  "mirror_axis" ;}},
        SKIPBLOCK  { public String toString() {return  "skip_block" ;}},
        TUNNEL_W      { public String toString() {return  "tunnel_w" ;}},
        TUNNEL_H      { public String toString() {return  "tunnel_h" ;}},
        TUNNEL_OX     { public String toString() {return  "tunnel_ox" ;}},
        TUNNEL_OY     { public String toString() {return  "tunnel_oy" ;}},
        TUNNEL_DEPTH  { public String toString() {return  "tunnel_d" ;}};
        public int def=0;
        public int min=0;
        public int max=2048;
        public Value coval=null;
        static{
            MULTIPLIER.def=1; MULTIPLIER.min=1; MULTIPLIER.max=16;
            ROWCOLLIM.def=0; ROWCOLLIM.min=0;
            BLASTRAD.def=4; BLASTRAD.min=4; BLASTRAD.max=16;
            GRIDM.coval=GRIDN;
            GRIDN.coval=GRIDM;
            GRIDM.min=1;
            GRIDN.min=1;
            MIRRORAXIS.min=0;
            MIRRORAXIS.max=2;
            SKIPBLOCK.max=100;
            TUNNEL_W.min=1;
            TUNNEL_H.min=1;
            TUNNEL_H.def=2;
            TUNNEL_DEPTH.def=3;
            TUNNEL_DEPTH.min=1;
        }
    }

    static public Mode[] modes=Mode.values();
    static public Action[] actions=Action.values();
    static public Orientation[] orientations=Orientation.values();
    static public Plane[] planes=Plane.values();
    static public Direction.Axis[] axes=Direction.Axis.values();
    static public Rotation[] rotations=Rotation.values();
    static public StateMode[] state_modes=StateMode.values();
    static public MirrorAxis[] mirrorAxes=MirrorAxis.values();

    static public boolean getFlag(ItemStack stack, Flag flag) {
        if(WandUtils.is_wand(stack)) {
            CompoundTag tag=stack.getOrCreateTag();
            if(tag.contains(flag.toString())) {
                return tag.getBoolean(flag.toString());
            }else{
                tag.putBoolean(flag.toString(),flag.get_default());
                return flag.get_default();
            }
        }
        return false;
    }
    static public void setFlag(ItemStack stack,Flag flag,boolean f) {
        if(WandUtils.is_wand(stack)) {
            stack.getOrCreateTag().putBoolean(flag.toString(), f);
        }
    }
    static public void toggleFlag(ItemStack stack,Flag flag) {
        if(WandUtils.is_wand(stack)) {
            CompoundTag tag=stack.getOrCreateTag();
            boolean b=getFlag(stack,flag);
            tag.putBoolean(flag.toString(), !b);
        }
    }

    static public void setVal(ItemStack stack,Value v, int n) {
        if(WandUtils.is_wand(stack)) {
            if(n>v.max) n=v.max;
            if(n<v.min) n=v.min;
            stack.getOrCreateTag().putInt(v.toString(), n);
        }
    }
    static public void incVal(ItemStack stack,Value v, int inc,int max) {
        if(WandUtils.is_wand(stack)) {
            int n=stack.getOrCreateTag().getInt(v.toString());
            if(n>v.max) n=v.max;
            if(n<v.min) n=v.min;
            if(n+inc<=max)
                stack.getOrCreateTag().putInt(v.toString(), n+inc);
        }
    }
    static public void incVal(ItemStack stack,Value v, int inc) {
        incVal(stack,v,inc,v.max);
    }
    static public void decVal(ItemStack stack,Value v, int inc,int min) {
        if(WandUtils.is_wand(stack)) {
            int n=stack.getOrCreateTag().getInt(v.toString());
            if(n>v.max) n=v.max;
            if(n<v.min) n=v.min;
            if(n-inc>=min)
                stack.getOrCreateTag().putInt(v.toString(), n-inc);
        }
    }
    static public void decVal(ItemStack stack,Value v, int dec) {
        decVal(stack,v,dec,v.min);
    }
    static public int getVal(ItemStack stack,Value v) {
        if(!WandUtils.is_wand(stack)) {
            return -1;
        }
        int i= stack.getOrCreateTag().getInt(v.toString());
        if(i>v.max){
            i=v.max;
            stack.getOrCreateTag().putInt(v.toString(),i);
        }
        if(i<v.min) {
            i = v.min;
            stack.getOrCreateTag().putInt(v.toString(), i);
        }
        if(i<v.min){
            return v.min;
        }else if(i>v.max){
            return v.max;
        }
        return i;
    }

    static public Mode getMode(ItemStack stack) {
        if(!WandUtils.is_wand(stack)) {
            return Mode.DIRECTION;
        }
        int m=stack.getOrCreateTag().getInt("mode");
        if(m>=0 && m<modes.length) {
            return modes[m];
        }
        return Mode.DIRECTION;
    }
    static public void setMode(ItemStack stack,Mode mode) {
        if(!WandUtils.is_wand(stack)) {
            return;
        }
        stack.getOrCreateTag().putInt("mode", mode.ordinal());
    }
    static public void nextMode(ItemStack stack,boolean can_blast) {
        if(!WandUtils.is_wand(stack)){
            return;
        }
        CompoundTag tag=stack.getOrCreateTag();
        int mode=(tag.getInt("mode")+1) % (modes.length);

        if(mode==Mode.VEIN.ordinal()  && !WandsMod.config.enable_vein_mode){
            mode = Mode.BLAST.ordinal();
        }
        if( (!can_blast && mode==Mode.BLAST.ordinal())
            || (!WandsMod.config.enable_blast_mode && mode==Mode.BLAST.ordinal())
        ){
            mode=Mode.DIRECTION.ordinal();
        }
        tag.putInt("mode", mode);
    }
    static public void prevMode(ItemStack stack,boolean can_blast) {
        if(!WandUtils.is_wand(stack)) {
            return;
        }
        CompoundTag tag=stack.getOrCreateTag();
        int mode=tag.getInt("mode")-1;
        if(mode<0){
            mode=modes.length-1;
        }
        if( (!can_blast && mode==Mode.BLAST.ordinal())
                || (!WandsMod.config.enable_blast_mode && mode==Mode.BLAST.ordinal())
        ){
            mode=Mode.BLAST.ordinal()-1;
        }
        if(mode==Mode.VEIN.ordinal()  && !WandsMod.config.enable_vein_mode){
            mode = Mode.VEIN.ordinal()-1;
        }
        tag.putInt("mode", mode);
    }

    static public Orientation getOrientation(ItemStack stack) {
        if(!WandUtils.is_wand(stack)) {
            return Orientation.ROW;
        }
        return orientations[stack.getOrCreateTag().getInt("orientation")];
    }
    static public void setOrientation(ItemStack stack,Orientation o) {
        if(!WandUtils.is_wand(stack)) {
            return;
        }
        stack.getOrCreateTag().putInt("orientation", o.ordinal());
    }
    static public void nextOrientation(ItemStack stack) {
        if(!WandUtils.is_wand(stack)) {
            return;
        }
        CompoundTag tag=stack.getOrCreateTag();
        int o=(tag.getInt("orientation")+1) %2;
        tag.putInt("orientation", o);
    }
    static public Plane getPlane(ItemStack stack) {
        Plane plane=Plane.XZ;
        if(!WandUtils.is_wand(stack)) {
            return plane;
        }
        int p=stack.getOrCreateTag().getInt("plane");
        if(p>=0 && p<planes.length)
            plane=planes[p];
        return plane;
    }
    static public void setPlane(ItemStack stack,Plane p) {
        if(!WandUtils.is_wand(stack)) {
            return;
        }
        stack.getOrCreateTag().putInt("plane", p.ordinal());
    }
    static public void nextPlane(ItemStack stack) {
        if(!WandUtils.is_wand(stack)) {
            return;
        }
        CompoundTag tag=stack.getOrCreateTag();
        int plane=(tag.getInt("plane")+1) % 3;
        tag.putInt("plane", plane);
    }

    static public Rotation getRotation(ItemStack stack) {
        if(!WandUtils.is_wand(stack)) {
            return Rotation.NONE;
        }
        return rotations[stack.getOrCreateTag().getInt("rotation")];
    }
    static public void nextRotation(ItemStack stack) {
        if(!WandUtils.is_wand(stack)) {
            return;
        }
        CompoundTag tag=stack.getOrCreateTag();
        int rot=(tag.getInt("rotation")+1) % rotations.length;
        tag.putInt("rotation", rot);
    }
    static public void setRotation(ItemStack stack,Rotation rot) {
        if(!WandUtils.is_wand(stack)) {
            return;
        }
        stack.getOrCreateTag().putInt("rotation", rot.ordinal());
    }
    static public Action getAction(ItemStack stack) {
        if(WandUtils.is_wand(stack)) {
            int m=stack.getOrCreateTag().getInt("action");
            if(m<actions.length)
                return actions[m];
        }
        return Action.PLACE;
    }
    static public void setAction(ItemStack stack,Action a) {
        if(WandUtils.is_wand(stack)){
            CompoundTag tag=stack.getOrCreateTag();
            if(WandsMod.config.disable_destroy_replace && (a== Action.DESTROY ||a== Action.REPLACE)){
                a=Action.PLACE;
            }
            tag.putInt("action", a.ordinal());
        }
    }
    static public void nextAction(ItemStack stack) {
        if(WandUtils.is_wand(stack)){
            CompoundTag tag=stack.getOrCreateTag();
            int a=(tag.getInt("action")+1) % (actions.length);
            if(WandsMod.config.disable_destroy_replace && (a== Action.DESTROY.ordinal() ||a== Action.REPLACE.ordinal())){
                a=Action.USE.ordinal();
            }
            tag.putInt("action", a);
        }
    }
    static public void prevAction(ItemStack stack) {
        if(WandUtils.is_wand(stack)){
            CompoundTag tag=stack.getOrCreateTag();
            int a=tag.getInt("action")-1;
            if(a<0){
                a=actions.length-1;
            }
            if(WandsMod.config.disable_destroy_replace && (a== Action.DESTROY.ordinal() ||a== Action.REPLACE.ordinal())){
                a=Action.PLACE.ordinal();
            }
            tag.putInt("action", a);
        }
    }

    static public Direction.Axis getAxis(ItemStack stack) {
        Direction.Axis axis= Direction.Axis.Y;
        if(WandUtils.is_wand(stack)){
            int p=stack.getOrCreateTag().getInt("axis");
            if(p>=0 && p< axes.length)
                axis=axes[p];
        }
        return axis;
    }
    static public void setAxis(ItemStack stack,Direction.Axis axis) {
        if(WandUtils.is_wand(stack)){
            CompoundTag tag=stack.getOrCreateTag();
            tag.putInt("axis", axis.ordinal());
        }
    }
    static public void setAxis(ItemStack stack,int a) {
        if (stack != null && WandUtils.is_wand(stack) && !stack.isEmpty()) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt("axis", a);
        }
    }
    static public void nextAxis(ItemStack stack) {
        if(WandUtils.is_wand(stack)){
            CompoundTag tag=stack.getOrCreateTag();
            int axis=(tag.getInt("axis")+1) % 3;
            tag.putInt("axis", axis);
        }
    }
    static public StateMode getStateMode(ItemStack stack) {
        if(WandUtils.is_wand(stack)) {
            int m=stack.getOrCreateTag().getInt("state_mode");
            if(m<state_modes.length)
                return state_modes[m];
        }
        return StateMode.TARGET;
    }
    static public void setStateMode(ItemStack stack,StateMode mode) {
        if(WandUtils.is_wand(stack)) {
            CompoundTag tag=stack.getOrCreateTag();
            tag.putInt("state_mode", mode.ordinal());
        }
    }
    static public void incGrid(ItemStack stack,Value v, int n,int limit) {
        if(WandUtils.is_wand(stack)) {
            CompoundTag tag=stack.getOrCreateTag();
            if(v.coval!=null){
                int c=stack.getOrCreateTag().getInt(v.coval.toString());
                if( (c*n)<=limit) {
                    int nn=stack.getOrCreateTag().getInt(v.toString());
                    tag.putInt(v.toString(), nn+n);
                }
            }
        }
    }
}
