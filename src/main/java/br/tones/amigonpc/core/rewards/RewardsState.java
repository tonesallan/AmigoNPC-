package br.tones.amigonpc.core.rewards;

import java.util.HashSet;
import java.util.Set;

public final class RewardsState {
   public final Set<Integer> claimedRewardLevels = new HashSet<>();
   public int resetPoints = 0;
}
