package net.alloyggp.tournament.internal.runner;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.internal.InternalMatchResult;
import net.alloyggp.tournament.internal.admin.InternalAdminAction;

/**
 * This is a process-wide cache meant to short-circuit some of
 * the computation involved in computing match setups and
 * rankings. Stage runners may use it to store the state at the
 * end of each round and reuse this state in later computations.
 */
public class TournamentStateCache {
    private static final AtomicBoolean CACHE_ENABLED = new AtomicBoolean(true);
    private static final LoadingCache<CacheKey, SortedMap<Integer, List<CacheEntry>>> STAGE_CACHES =
            CacheBuilder.newBuilder()
            .expireAfterAccess(5L, TimeUnit.MINUTES)
            .maximumSize(100L)
            .build(new CacheLoader<CacheKey, SortedMap<Integer, List<CacheEntry>>>() {
                @Override
                public SortedMap<Integer, List<CacheEntry>> load(CacheKey key) throws Exception {
                    return Collections.synchronizedSortedMap(Maps.<Integer, Integer, List<CacheEntry>>newTreeMap(
                            Ordering.<Integer>natural().reverse()));
                }
            });

    /**
     * A key to find the appropriate sub-cache that stores entries
     * for a particular stage in a tournament. The seeding and results
     * before that stage are included, to prevent returning incorrect
     * results if "counterfactual" computations have been performed.
     */
    //TODO: Should we be using the tournament initial seeding instead of
    //the stage initial seeding? This would matter if we allowed scores
    //to carry over meaningfully in some way aside from the between-stage
    //seedings. Currently it should not, and the per-stage seeding is
    //much more accessible.
    private static class CacheKey {
        private final String tournamentInternalName;
        private final ImmutableList<InternalAdminAction> adminActions;
        private final TSeeding initialSeeding;
        private final int stageNum;
        private final ImmutableSet<InternalMatchResult> resultsFromEarlierStages;

        public CacheKey(String tournamentInternalName, ImmutableList<InternalAdminAction> adminActions,
                TSeeding initialSeeding, int stageNum,
                ImmutableSet<InternalMatchResult> resultsFromEarlierStages) {
            this.tournamentInternalName = tournamentInternalName;
            this.adminActions = adminActions;
            this.initialSeeding = initialSeeding;
            this.stageNum = stageNum;
            this.resultsFromEarlierStages = resultsFromEarlierStages;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((adminActions == null) ? 0 : adminActions.hashCode());
            result = prime * result + ((initialSeeding == null) ? 0 : initialSeeding.hashCode());
            result = prime * result + ((resultsFromEarlierStages == null) ? 0 : resultsFromEarlierStages.hashCode());
            result = prime * result + stageNum;
            result = prime * result + ((tournamentInternalName == null) ? 0 : tournamentInternalName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CacheKey other = (CacheKey) obj;
            if (adminActions == null) {
                if (other.adminActions != null)
                    return false;
            } else if (!adminActions.equals(other.adminActions))
                return false;
            if (initialSeeding == null) {
                if (other.initialSeeding != null)
                    return false;
            } else if (!initialSeeding.equals(other.initialSeeding))
                return false;
            if (resultsFromEarlierStages == null) {
                if (other.resultsFromEarlierStages != null)
                    return false;
            } else if (!resultsFromEarlierStages.equals(other.resultsFromEarlierStages))
                return false;
            if (stageNum != other.stageNum)
                return false;
            if (tournamentInternalName == null) {
                if (other.tournamentInternalName != null)
                    return false;
            } else if (!tournamentInternalName.equals(other.tournamentInternalName))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "CacheKey [tournamentInternalName=" + tournamentInternalName + ", adminActions=" + adminActions
                    + ", initialSeeding=" + initialSeeding + ", stageNum=" + stageNum + ", resultsFromEarlierStages="
                    + resultsFromEarlierStages + "]";
        }
    }

    private static class CacheEntry {
        public final ImmutableSet<InternalMatchResult> resultsSoFarInStage;
        public final EndOfRoundState state;

        private CacheEntry(ImmutableSet<InternalMatchResult> resultsSoFarInStage, EndOfRoundState state) {
            this.resultsSoFarInStage = resultsSoFarInStage;
            this.state = state;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((resultsSoFarInStage == null) ? 0 : resultsSoFarInStage.hashCode());
            result = prime * result + ((state == null) ? 0 : state.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CacheEntry other = (CacheEntry) obj;
            if (resultsSoFarInStage == null) {
                if (other.resultsSoFarInStage != null) {
                    return false;
                }
            } else if (!resultsSoFarInStage.equals(other.resultsSoFarInStage)) {
                return false;
            }
            if (state == null) {
                if (other.state != null) {
                    return false;
                }
            } else if (!state.equals(other.state)) {
                return false;
            }
            return true;
        }
    }

    /**
     * Enables or disables caching. Tests use this for testing the cache.
     */
    public static void setEnabled(boolean enabled) {
        CACHE_ENABLED.set(enabled);
    }

    public static void cacheEndOfRoundState(String tournamentInternalName, TSeeding initialSeeding,
            ImmutableList<InternalAdminAction> adminActions,
            ImmutableSet<InternalMatchResult> resultsFromEarlierStages, int stageNum, Set<InternalMatchResult> resultsInStage, EndOfRoundState state) {
        if (!CACHE_ENABLED.get()) {
            return;
        }
        CacheKey key = new CacheKey(tournamentInternalName, adminActions, initialSeeding, stageNum, resultsFromEarlierStages);

        SortedMap<Integer, List<CacheEntry>> stageCache = STAGE_CACHES.getUnchecked(key);
        int size = resultsInStage.size();
        synchronized (stageCache) {
            if (!stageCache.containsKey(size)) {
                stageCache.put(size, Collections.synchronizedList(Lists.<CacheEntry>newArrayList()));
            }
            stageCache.get(size).add(new CacheEntry(ImmutableSet.copyOf(resultsInStage), state));
        }
    }

    public static @Nullable EndOfRoundState getLatestCachedEndOfRoundState(String tournamentInternalName,
            TSeeding initialSeeding, ImmutableList<InternalAdminAction> adminActions,
            ImmutableSet<InternalMatchResult> resultsFromEarlierStages,  int stageNum, ImmutableSet<InternalMatchResult> resultsInStage) {
        if (!CACHE_ENABLED.get()) {
            return null;
        }
        CacheKey key = new CacheKey(tournamentInternalName, adminActions, initialSeeding, stageNum, resultsFromEarlierStages);

        SortedMap<Integer, List<CacheEntry>> results = STAGE_CACHES.getUnchecked(key);
        synchronized (results) {
            //Key is the size of the "resultsInStage" set
            //This goes from highest to lowest, so we'll find the largest subset
            for (Entry<Integer, List<CacheEntry>> entry : results.entrySet()) {
                if (entry.getKey() > resultsInStage.size()) {
                    continue;
                }
                synchronized (entry.getValue()) {
                    for (CacheEntry value : entry.getValue()) {
                        if (resultsInStage.containsAll(value.resultsSoFarInStage)) {
                            return value.state;
                        }
                    }
                }
            }
        }
        return null;
    }
}
