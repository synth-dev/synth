//package com.github.sieves.api
//
//import com.github.sieves.content.modules.*
//import net.minecraft.server.level.ServerPlayer
//import net.minecraft.world.entity.*
//import net.minecraft.world.entity.ai.goal.*
//import net.minecraft.world.entity.monster.Enemy
//import net.minecraft.world.entity.player.Player
//
//object ApiMixins {
//    /**
//     * Injected at the
//     */
//    fun injectRegisterGoals(goals: GoalSelector, mob: PathfinderMob) {
//        if (mob is Enemy)
//            mob.goalSelector.addGoal(
//                0,
//                AvoidEntityGoal(
//                    mob,
//                    Player::class.java,
//                    16.0f,
//                    1.5,
//                    1.8,
//                    ::onAvoidPlayerGoal
//                )
//            )
//    }
//
//    private fun onAvoidPlayerGoal(entity: LivingEntity): Boolean {
//        if (entity is ServerPlayer)
//            if (ScareModule.scaring.contains(entity.uuid)) {
//                return true
//            }
//        return false
//    }
//}