package io.lvoxx.ssurl.dashboard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;

import io.lvoxx.ssurl.dashboard.repository.DashboardQueryRepository.RollupKey;
import io.lvoxx.ssurl.dashboard.repository.impl.DashboardQueryRepositoryImpl;
import io.lvoxx.ssurl.test_starter.AbstractPostgresContainer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DataR2dbcTest
@ActiveProfiles("repo")
@Import(DashboardQueryRepositoryImpl.class)
@DisplayName("Dashboard Query Repository Tests")
@Tags({ @Tag("Repository"), @Tag("Integration") })
class DashboardQueryRepositoryTest extends AbstractPostgresContainer {

    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    @Autowired
    private DashboardQueryRepository repo;

    @Autowired
    private DatabaseClient db;

    @BeforeEach
    void seed() {
        // Clean + seed: alice owns 'a1' (Hello) & 'a2'; bob owns 'b1'.
        exec("DELETE FROM click_daily_rollup");
        exec("DELETE FROM click_events");
        exec("DELETE FROM urls");
        exec("DELETE FROM users");

        exec("INSERT INTO users (id, username, is_active) VALUES (1,'alice',true),(2,'bob',true)");
        exec("INSERT INTO urls (id, short_code, user_id, title, is_active) VALUES "
                + "(1,'a1',1,'Hello',true),(2,'a2',1,'World',false),(3,'b1',2,'Bobs',true)");

        // Rollup: a1 → 5 today + 3 yesterday; a2 → 2 today; b1 → 9 today
        exec("INSERT INTO click_daily_rollup (short_code, day, clicks) VALUES "
                + "('a1','" + TODAY + "',5),('a1','" + YESTERDAY + "',3),"
                + "('a2','" + TODAY + "',2),('b1','" + TODAY + "',9)");

        // Raw events for referer/unique aggregation (alice's a1)
        exec("INSERT INTO click_events (short_code, ip, referer, created_at) VALUES "
                + "('a1','1.1.1.1','https://news.com', NOW()),"
                + "('a1','1.1.1.1','https://news.com', NOW()),"
                + "('a1','2.2.2.2','', NOW()),"
                + "('a1','3.3.3.3','https://news.com', NOW())");
    }

    private void exec(String sql) {
        db.sql(sql).fetch().rowsUpdated().block(Duration.ofSeconds(10));
    }

    @Test
    void totalClicks_scopedToOwner_sumsRollup() {
        // alice: a1(5+3) + a2(2) = 10 over the last 7 days
        repo.totalClicks("alice", TODAY.minusDays(6))
                .as(StepVerifier::create)
                .assertNext(n -> assertThat(n).isEqualTo(10L))
                .verifyComplete();
    }

    @Test
    void totalClicks_doesNotLeakOtherUsersData() {
        repo.totalClicks("bob", TODAY.minusDays(6))
                .as(StepVerifier::create)
                .assertNext(n -> assertThat(n).isEqualTo(9L))
                .verifyComplete();
    }

    @Test
    void activeLinks_countsOnlyActiveOwned() {
        // alice has a1 (active) + a2 (inactive) → 1
        repo.activeLinks("alice")
                .as(StepVerifier::create)
                .assertNext(n -> assertThat(n).isEqualTo(1L))
                .verifyComplete();
    }

    @Test
    void timeseries_groupsByDay() {
        repo.timeseries("alice", null, TODAY.minusDays(6), TODAY)
                .collectList()
                .as(StepVerifier::create)
                .assertNext(points -> {
                    assertThat(points).hasSize(2);
                    assertThat(points.get(0).date()).isEqualTo(YESTERDAY);
                    assertThat(points.get(0).clicks()).isEqualTo(3L);
                    assertThat(points.get(1).date()).isEqualTo(TODAY);
                    assertThat(points.get(1).clicks()).isEqualTo(7L); // a1(5)+a2(2)
                })
                .verifyComplete();
    }

    @Test
    void topLinks_rankedByClicks_withTitles() {
        repo.topLinks("alice", TODAY.minusDays(6), 10)
                .collectList()
                .as(StepVerifier::create)
                .assertNext(items -> {
                    assertThat(items).hasSize(2);
                    assertThat(items.get(0).key()).isEqualTo("a1");
                    assertThat(items.get(0).label()).isEqualTo("Hello");
                    assertThat(items.get(0).clicks()).isEqualTo(8L);
                })
                .verifyComplete();
    }

    @Test
    void topReferers_aggregatesRawEvents_withDirectFallback() {
        repo.topReferers("alice", null, TODAY.minusDays(6).atStartOfDay(), 10)
                .collectList()
                .as(StepVerifier::create)
                .assertNext(items -> {
                    assertThat(items.get(0).label()).isEqualTo("https://news.com");
                    assertThat(items.get(0).clicks()).isEqualTo(3L);
                    assertThat(items).anyMatch(i -> i.label().equals("(direct)") && i.clicks() == 1L);
                })
                .verifyComplete();
    }

    @Test
    void uniqueVisitors_countsDistinctIps() {
        repo.uniqueVisitors("alice", "a1", TODAY.minusDays(6).atStartOfDay())
                .as(StepVerifier::create)
                .assertNext(n -> assertThat(n).isEqualTo(3L))
                .verifyComplete();
    }

    @Test
    void ownsCode_trueForOwner_falseForOther() {
        repo.ownsCode("alice", "a1").as(StepVerifier::create).expectNext(true).verifyComplete();
        repo.ownsCode("bob", "a1").as(StepVerifier::create).expectNext(false).verifyComplete();
    }

    @Test
    void upsertDailyRollup_incrementsExistingBucket() {
        repo.upsertDailyRollup(Map.of(new RollupKey("a1", TODAY), 4L))
                .then(repo.totalClicks("alice", TODAY))
                .as(StepVerifier::create)
                // a1 today was 5, +4 = 9, plus a2 today 2 = 11
                .assertNext(n -> assertThat(n).isEqualTo(11L))
                .verifyComplete();
    }

    @Test
    void upsertDailyRollup_insertsNewBucket() {
        repo.upsertDailyRollup(Map.of(new RollupKey("a1", TODAY.minusDays(2)), 7L))
                .then(repo.timeseries("alice", "a1", TODAY.minusDays(6), TODAY).collectList())
                .as(StepVerifier::create)
                .assertNext(points -> assertThat(points).anyMatch(p -> p.clicks() == 7L))
                .verifyComplete();
    }

    @Test
    void upsertDailyRollup_emptyMap_isNoOp() {
        repo.upsertDailyRollup(Map.of())
                .as(StepVerifier::create)
                .verifyComplete();
        StepVerifier.create(Mono.just("ok")).expectNext("ok").verifyComplete();
    }
}
