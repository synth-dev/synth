package com.github.sieves.api.multiblock

/**
 * Used for tracking the different types of blocks within a multiblock structure.
 * slave
 */
enum class StructureBlockVariant {
    Side,           //Outer is used for anything that isn't on the perimeter of the multiblock and is out the outer side of the block
    Inner,          //Anything that is inside the multiblock
    VerticalEdge,   //Anything that is in one of the 4 vertical corner
    HorizontalEdge, //Anything that is on the edge and isn't in one of the 4 corners (8 different slices 2 per side)
    Corner,         //Should be 8 total, anything that is a corner
}