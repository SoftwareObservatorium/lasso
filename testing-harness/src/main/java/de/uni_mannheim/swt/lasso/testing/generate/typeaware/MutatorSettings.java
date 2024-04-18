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
package de.uni_mannheim.swt.lasso.testing.generate.typeaware;

import de.uni_mannheim.swt.lasso.testing.generate.random.RandomObjectGenerator;

import java.util.Random;

/**
 *
 * @author Marcus Kessel
 */
public class MutatorSettings {

    private Random random = new Random();
    private RandomObjectGenerator randomObjectGenerator = new RandomObjectGenerator();

    private int byteBound = 10;
    private int byteFactorRange = 5;

    private int shortBound = 100;
    private int shortFactorRange = 50;

    private int intBound = 1000;
    private int intFactorRange = 50;

    private int longBound = 1000;
    private int longFactorRange = 50;

    private float floatFactor = 0.1f;

    private double doubleFactor = 0.1d;

    public int getIntBound() {
        return intBound;
    }

    public void setIntBound(int intBound) {
        this.intBound = intBound;
    }

    public int getIntFactorRange() {
        return intFactorRange;
    }

    public void setIntFactorRange(int intFactorRange) {
        this.intFactorRange = intFactorRange;
    }

    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public RandomObjectGenerator getRandomObjectGenerator() {
        return randomObjectGenerator;
    }

    public void setRandomObjectGenerator(RandomObjectGenerator randomObjectGenerator) {
        this.randomObjectGenerator = randomObjectGenerator;
    }

    public int getByteBound() {
        return byteBound;
    }

    public void setByteBound(int byteBound) {
        this.byteBound = byteBound;
    }

    public int getByteFactorRange() {
        return byteFactorRange;
    }

    public void setByteFactorRange(int byteFactorRange) {
        this.byteFactorRange = byteFactorRange;
    }

    public int getShortBound() {
        return shortBound;
    }

    public void setShortBound(int shortBound) {
        this.shortBound = shortBound;
    }

    public int getShortFactorRange() {
        return shortFactorRange;
    }

    public void setShortFactorRange(int shortFactorRange) {
        this.shortFactorRange = shortFactorRange;
    }

    public int getLongBound() {
        return longBound;
    }

    public void setLongBound(int longBound) {
        this.longBound = longBound;
    }

    public int getLongFactorRange() {
        return longFactorRange;
    }

    public void setLongFactorRange(int longFactorRange) {
        this.longFactorRange = longFactorRange;
    }

    public double getDoubleFactor() {
        return doubleFactor;
    }

    public void setDoubleFactor(double doubleFactor) {
        this.doubleFactor = doubleFactor;
    }

    public float getFloatFactor() {
        return floatFactor;
    }

    public void setFloatFactor(float floatFactor) {
        this.floatFactor = floatFactor;
    }
}
