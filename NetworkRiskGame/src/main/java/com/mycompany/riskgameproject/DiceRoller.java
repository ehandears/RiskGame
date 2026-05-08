package com.mycompany.riskgameproject;

import java.util.Arrays;
import java.util.Random;

public final class DiceRoller {

    private static final Random RANDOM = new Random();

    private DiceRoller() {
    }

    public static int getAttackerDiceCount(int attackingArmies) {
        return Math.max(1, Math.min(3, attackingArmies - 1));
    }

    public static int getDefenderDiceCount(int defendingArmies) {
        return Math.max(1, Math.min(2, defendingArmies));
    }

    public static int rollDie() {
        return RANDOM.nextInt(6) + 1;
    }

    public static int[] rollDice(int count) {
        int[] dice = new int[count];
        for (int i = 0; i < dice.length; i++) {
            dice[i] = rollDie();
        }
        sortDescending(dice);
        return dice;
    }

    public static BattleResult compare(int[] attackerDice, int[] defenderDice) {
        int[] sortedAttack = Arrays.copyOf(attackerDice, attackerDice.length);
        int[] sortedDefense = Arrays.copyOf(defenderDice, defenderDice.length);
        sortDescending(sortedAttack);
        sortDescending(sortedDefense);

        int attackerLosses = 0;
        int defenderLosses = 0;
        int comparisons = Math.min(sortedAttack.length, sortedDefense.length);

        for (int i = 0; i < comparisons; i++) {
            if (sortedAttack[i] > sortedDefense[i]) {
                defenderLosses++;
            } else {
                attackerLosses++;
            }
        }

        return new BattleResult(attackerLosses, defenderLosses);
    }

    public static String formatDice(int[] dice) {
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < dice.length; i++) {
            if (i > 0) {
                text.append(",");
            }
            text.append(dice[i]);
        }
        text.append("]");
        return text.toString();
    }

    private static void sortDescending(int[] dice) {
        Arrays.sort(dice);
        for (int i = 0; i < dice.length / 2; i++) {
            int opposite = dice.length - 1 - i;
            int temp = dice[i];
            dice[i] = dice[opposite];
            dice[opposite] = temp;
        }
    }

    public static final class BattleResult {
        private final int attackerLosses;
        private final int defenderLosses;

        private BattleResult(int attackerLosses, int defenderLosses) {
            this.attackerLosses = attackerLosses;
            this.defenderLosses = defenderLosses;
        }

        public int getAttackerLosses() {
            return attackerLosses;
        }

        public int getDefenderLosses() {
            return defenderLosses;
        }
    }
}