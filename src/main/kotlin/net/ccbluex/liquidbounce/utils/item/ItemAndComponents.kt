/*
 * This file is part of Tsunami, a fork of LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * Tsunami is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tsunami is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tsunami. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.item

import net.minecraft.core.Holder
import net.minecraft.core.TypedInstance
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * Represents the "id" of an [ItemStack]. Stacks sharing an [Item] and a
 * [DataComponentPatch] can be merged.
 *
 * Upstream kept this inside the InventoryCleaner module's package. It is
 * general-purpose item identity with no relationship to that module, and
 * ItemTags still needs it, so it lives here now.
 */
@JvmRecord
data class ItemAndComponents @JvmOverloads constructor(
    val item: Item,
    val componentsPatch: DataComponentPatch = DataComponentPatch.EMPTY,
) : TypedInstance<Item> {
    constructor(itemStack: ItemStack) : this(itemStack.item, itemStack.componentsPatch)

    override fun typeHolder(): Holder<Item> = BuiltInRegistries.ITEM.wrapAsHolder(this.item)

    fun toItemStack(count: Int): ItemStack {
        return ItemStack(this.typeHolder(), count, componentsPatch)
    }
}
