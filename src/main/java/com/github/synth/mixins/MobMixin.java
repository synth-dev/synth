//package com.github.synth.mixins;
//
//import com.github.sieves.api.ApiMixins;
//import com.github.sieves.util.Log;
//import net.minecraft.world.entity.*;
//import net.minecraft.world.entity.ai.goal.GoalSelector;
//import net.minecraft.world.level.Level;
//import org.spongepowered.asm.mixin.Final;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.gen.Accessor;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//import javax.annotation.Nullable;
//
//@Mixin(value = Mob.class, remap = false)
//public abstract class MobMixin {
//    @Shadow
//    @Final
//    public GoalSelector goalSelector;
//
//    /**
//     * Redirect the creation of mobs on the server side to our api
//     *
//     * @param type  the entity type
//     * @param level the level
//     * @param ci    the callback info
//     */
//    @Inject(method = "<init>", at = @At(value = "TAIL"))
//    public void injectMobRegisterGoals(EntityType type, Level level, CallbackInfo ci) {
//        var mob = (Mob) ((Object) this);
//        if (mob instanceof PathfinderMob)
//            if (!level.isClientSide) ApiMixins.INSTANCE.injectRegisterGoals(this.goalSelector, (PathfinderMob) mob);
//    }
//
//}
