//package com.github.sieves.content.sieve
//
//import net.minecraft.world.inventory.SimpleContainerData
//
//class SieveContainerData(amount: Int, val tile: SieveTile) : SimpleContainerData(amount) {
//    override fun get(pIndex: Int): Int {
//        return when (pIndex) {
//            0 -> tile.percent
//            1 -> tile.energy
//            else -> 0
//        }
//    }
//
//    override fun set(pIndex: Int, pValue: Int) {
//        when (pIndex) {
//            0 -> {
//                tile.percent = pValue
//            }
//            1 -> {
//                tile.energy = pValue
//            }
//        }
//    }
//}