package org.dimdev.dimdoors.shared.items;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.dimdev.dimdoors.shared.tileentities.TileEntityFloatingRift;
import org.dimdev.dimdoors.shared.tileentities.TileEntityRift;

public final class RayTraceHelper {
    public static boolean isFloatingRift(RayTraceResult hit, World world) {
        return hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK && world.getTileEntity(hit.getBlockPos()) instanceof TileEntityFloatingRift;
    }

    public static boolean isRift(RayTraceResult hit, World world) {
        return hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK && world.getTileEntity(hit.getBlockPos()) instanceof TileEntityRift;
    }

    public static boolean isLivingEntity(RayTraceResult hit) {
        return hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY && hit.entityHit instanceof EntityLivingBase;
    }
}
