package org.vivecraft.gameplay;

import net.minecraft.util.math.Vec3d;
import org.vivecraft.api.VRData;

public class OpenVRPlayer {
    public VRData vrdata_world_pre;
    public VRData vrdata_world_post;
    public VRData vrdata_world_render;
    public Vec3d roomOrigin;

    public static OpenVRPlayer get() { throw new AbstractMethodError(); }
}
