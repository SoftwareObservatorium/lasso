/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.jn.langx.util.bit;


import java.util.Iterator;
import java.util.LinkedHashSet;
/**
 * @since 4.0.6
 */
public class MaskSet extends LinkedHashSet<Integer> {
    private int masks = 0;

    @Override
    public boolean add(Integer mask) {
        if (mask == null) {
            return false;
        }
        if (super.add(mask)) {
            this.masks = Masks.addOperand(masks, mask);
        }
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        int mask = ((Number) o).intValue();
        if (super.remove(o)) {
            this.masks = Masks.removeOperand(this.masks, mask);
        }
        return true;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        int mask = ((Number) o).intValue();
        return Masks.containsOperand(this.masks, mask);
    }

    @Override
    public Iterator<Integer> iterator() {
        return super.iterator();
    }

    @Override
    public int size() {
        return super.size();
    }
}
