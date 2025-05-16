package de.uni_mannheim.swt.lasso.service.controller.rank;

import de.uni_mannheim.swt.lasso.core.dto.RankRequest;
import de.uni_mannheim.swt.lasso.core.dto.RankResponse;
import de.uni_mannheim.swt.lasso.core.dto.RankingCriterion;
import de.uni_mannheim.swt.lasso.core.dto.SystemMeta;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.strategy.HybridNonDominatedSortingRanking;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Marcus Kessel
 */
public class RankingManagerTest {

    @Test
    public void testHds() throws IOException {
        RankingManager rankingManager = new RankingManager();

        RankRequest rankRequest = new RankRequest();
        rankRequest.setStrategy(HybridNonDominatedSortingRanking.ID);

        SystemMeta c1 = new SystemMeta();
        c1.setId("1");
        c1.setVariantId("original");
        c1.setAdapterId("0");
        SystemMeta c2 = new SystemMeta();
        c2.setId("2");
        c2.setVariantId("original");
        c2.setAdapterId("0");
        rankRequest.setCandidates(Arrays.asList(c1, c2));

        RankingCriterion entry1 = new RankingCriterion();
        entry1.setId("metric1");
        entry1.setObjective(0d);
        entry1.setPriority(1d);
        entry1.setWeight(0.5d);

        RankingCriterion entry2 = new RankingCriterion();
        entry2.setId("metric2");
        entry2.setObjective(0d);
        entry2.setPriority(1d);
        entry2.setWeight(0.5d);
        rankRequest.setCriteria(Arrays.asList(entry1, entry2));

        Map<String, Double> map1 = new HashMap<>();
        map1.put("metric1", 2d);
        map1.put("metric2", 2d);
        c1.setMeasures(map1);

        Map<String, Double> map2 = new HashMap<>();
        map2.put("metric1", 4d);
        map2.put("metric2", 4d);
        c2.setMeasures(map2);

        RankResponse rankResponse = rankingManager.rank(rankRequest);

        System.out.println(ToStringBuilder.reflectionToString(rankResponse.getRankedCandidates()));
    }
}
