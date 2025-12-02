package org.frankframework.insights.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    public void setUp() {
        rateLimitService = new RateLimitService();
    }

    @Test
    public void checkIfBlocked_shouldNotThrow_whenUserHasNoFailures() throws RateLimitExceededException {
        rateLimitService.checkIfBlocked("user1");
    }

    @Test
    public void checkIfBlocked_shouldNotThrow_whenUserHasFailuresRemaining() throws RateLimitExceededException {
        String userKey = "user1";

        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.trackFailedRequest(userKey);

        rateLimitService.checkIfBlocked(userKey);
    }

    @Test
    public void checkIfBlocked_shouldThrowException_whenUserExceededLimit() {
        String userKey = "user1";

        for (int i = 0; i < 5; i++) {
            rateLimitService.trackFailedRequest(userKey);
        }

        assertThatThrownBy(() -> rateLimitService.checkIfBlocked(userKey))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Too many failed requests")
                .hasMessageContaining("15 minutes");
    }

    @Test
    public void checkIfBlocked_shouldNotThrow_afterReset() throws RateLimitExceededException {
        String userKey = "user1";

        for (int i = 0; i < 5; i++) {
            rateLimitService.trackFailedRequest(userKey);
        }

        rateLimitService.resetRateLimit(userKey);

        rateLimitService.checkIfBlocked(userKey);
    }

    @Test
    public void trackFailedRequest_shouldCreateBucketForNewUser() {
        String userKey = "user1";

        assertThat(rateLimitService.getBucketCount()).isEqualTo(0);

        rateLimitService.trackFailedRequest(userKey);

        assertThat(rateLimitService.getBucketCount()).isEqualTo(1);
    }

    @Test
    public void trackFailedRequest_shouldConsumeTokens() throws RateLimitExceededException {
        String userKey = "user1";

        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.trackFailedRequest(userKey);

        rateLimitService.checkIfBlocked(userKey);

        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.trackFailedRequest(userKey);

        assertThatThrownBy(() -> rateLimitService.checkIfBlocked(userKey))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    public void trackFailedRequest_shouldAllowExactly5Failures() throws RateLimitExceededException {
        String userKey = "user1";

        for (int i = 0; i < 5; i++) {
            rateLimitService.trackFailedRequest(userKey);
            if (i < 4) {
                rateLimitService.checkIfBlocked(userKey);
            }
        }

        assertThatThrownBy(() -> rateLimitService.checkIfBlocked(userKey))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    public void trackFailedRequest_shouldHandleMultipleUsers() throws RateLimitExceededException {
        String user1 = "user1";
        String user2 = "user2";

        rateLimitService.trackFailedRequest(user1);
        rateLimitService.trackFailedRequest(user1);

        rateLimitService.trackFailedRequest(user2);

        assertThat(rateLimitService.getBucketCount()).isEqualTo(2);

        rateLimitService.checkIfBlocked(user1);

        rateLimitService.checkIfBlocked(user2);
    }

    @Test
    public void trackFailedRequest_shouldContinueTrackingAfterLimit() {
        String userKey = "user1";

        for (int i = 0; i < 7; i++) {
            rateLimitService.trackFailedRequest(userKey);
        }

        assertThatThrownBy(() -> rateLimitService.checkIfBlocked(userKey))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    public void resetRateLimit_shouldRemoveBucket() {
        String userKey = "user1";

        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.trackFailedRequest(userKey);

        assertThat(rateLimitService.getBucketCount()).isEqualTo(1);

        rateLimitService.resetRateLimit(userKey);

        assertThat(rateLimitService.getBucketCount()).isEqualTo(0);
    }

    @Test
    public void resetRateLimit_shouldAllowNewFailures() throws RateLimitExceededException {
        String userKey = "user1";

        for (int i = 0; i < 4; i++) {
            rateLimitService.trackFailedRequest(userKey);
        }

        rateLimitService.resetRateLimit(userKey);

        for (int i = 0; i < 5; i++) {
            rateLimitService.trackFailedRequest(userKey);
            if (i < 4) {
                rateLimitService.checkIfBlocked(userKey);
            }
        }

        assertThatThrownBy(() -> rateLimitService.checkIfBlocked(userKey))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    public void resetRateLimit_shouldHandleNonExistentUser() {
        String userKey = "nonexistent";

        assertThat(rateLimitService.getBucketCount()).isEqualTo(0);

        rateLimitService.resetRateLimit(userKey);

        assertThat(rateLimitService.getBucketCount()).isEqualTo(0);
    }

    @Test
    public void resetRateLimit_shouldUnblockUser() throws RateLimitExceededException {
        String userKey = "user1";

        for (int i = 0; i < 5; i++) {
            rateLimitService.trackFailedRequest(userKey);
        }

        assertThatThrownBy(() -> rateLimitService.checkIfBlocked(userKey))
                .isInstanceOf(RateLimitExceededException.class);

        rateLimitService.resetRateLimit(userKey);

        rateLimitService.checkIfBlocked(userKey);
    }

    @Test
    public void resetRateLimit_shouldOnlyAffectSpecificUser() throws RateLimitExceededException {
        String user1 = "user1";
        String user2 = "user2";

        rateLimitService.trackFailedRequest(user1);
        rateLimitService.trackFailedRequest(user2);
        rateLimitService.trackFailedRequest(user2);

        assertThat(rateLimitService.getBucketCount()).isEqualTo(2);

        rateLimitService.resetRateLimit(user1);

        assertThat(rateLimitService.getBucketCount()).isEqualTo(1);

        rateLimitService.checkIfBlocked(user2);
    }

    @Test
    public void cleanupOldBuckets_shouldNotRemoveActiveBuckets() {
        String user1 = "user1";
        String user2 = "user2";

        rateLimitService.trackFailedRequest(user1);
        rateLimitService.trackFailedRequest(user2);
        rateLimitService.trackFailedRequest(user2);

        assertThat(rateLimitService.getBucketCount()).isEqualTo(2);

        rateLimitService.cleanupOldBuckets();

        assertThat(rateLimitService.getBucketCount()).isEqualTo(2);
    }

    @Test
    public void cleanupOldBuckets_shouldRemoveFullyRefilledBuckets() {
        String userKey = "user1";

        rateLimitService.trackFailedRequest(userKey);
        assertThat(rateLimitService.getBucketCount()).isEqualTo(1);

        rateLimitService.resetRateLimit(userKey);
        assertThat(rateLimitService.getBucketCount()).isEqualTo(0);

        rateLimitService.cleanupOldBuckets();
        assertThat(rateLimitService.getBucketCount()).isEqualTo(0);
    }

    @Test
    public void cleanupOldBuckets_shouldHandleEmptyBuckets() {
        assertThat(rateLimitService.getBucketCount()).isEqualTo(0);

        rateLimitService.cleanupOldBuckets();

        assertThat(rateLimitService.getBucketCount()).isEqualTo(0);
    }

    @Test
    public void getBucketCount_shouldReturnZero_initially() {
        assertThat(rateLimitService.getBucketCount()).isEqualTo(0);
    }

    @Test
    public void getBucketCount_shouldReturnCorrectCount() {
        rateLimitService.trackFailedRequest("user1");
        rateLimitService.trackFailedRequest("user2");
        rateLimitService.trackFailedRequest("user3");

        assertThat(rateLimitService.getBucketCount()).isEqualTo(3);
    }

    @Test
    public void getBucketCount_shouldNotIncrement_forSameUser() {
        rateLimitService.trackFailedRequest("user1");
        rateLimitService.trackFailedRequest("user1");
        rateLimitService.trackFailedRequest("user1");

        assertThat(rateLimitService.getBucketCount()).isEqualTo(1);
    }

    @Test
    public void getBucketCount_shouldDecrement_afterReset() {
        rateLimitService.trackFailedRequest("user1");
        rateLimitService.trackFailedRequest("user2");
        assertThat(rateLimitService.getBucketCount()).isEqualTo(2);

        rateLimitService.resetRateLimit("user1");
        assertThat(rateLimitService.getBucketCount()).isEqualTo(1);

        rateLimitService.resetRateLimit("user2");
        assertThat(rateLimitService.getBucketCount()).isEqualTo(0);
    }

    @Test
    public void shouldHandleCompleteUserJourney() throws RateLimitExceededException {
        String userKey = "user1";

        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.trackFailedRequest(userKey);

        rateLimitService.checkIfBlocked(userKey);

        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.trackFailedRequest(userKey);

        assertThatThrownBy(() -> rateLimitService.checkIfBlocked(userKey))
                .isInstanceOf(RateLimitExceededException.class);

        rateLimitService.resetRateLimit(userKey);

        rateLimitService.checkIfBlocked(userKey);

        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.checkIfBlocked(userKey);
    }

    @Test
    public void shouldIsolateUsersFromEachOther() throws RateLimitExceededException {
        String user1 = "user1";
        String user2 = "user2";

        for (int i = 0; i < 5; i++) {
            rateLimitService.trackFailedRequest(user1);
        }

        assertThatThrownBy(() -> rateLimitService.checkIfBlocked(user1)).isInstanceOf(RateLimitExceededException.class);

        rateLimitService.checkIfBlocked(user2);

        rateLimitService.trackFailedRequest(user2);
        rateLimitService.checkIfBlocked(user2);
    }

    @Test
    public void shouldHandleConcurrentUsers() throws RateLimitExceededException {
        for (int i = 0; i < 10; i++) {
            rateLimitService.trackFailedRequest("user" + i);
        }

        assertThat(rateLimitService.getBucketCount()).isEqualTo(10);

        for (int i = 0; i < 10; i++) {
            rateLimitService.checkIfBlocked("user" + i);
        }
    }

    @Test
    public void shouldHandleRepeatedResetAndFailureCycles() throws RateLimitExceededException {
        String userKey = "user1";

        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.resetRateLimit(userKey);

        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.resetRateLimit(userKey);

        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.resetRateLimit(userKey);

        rateLimitService.checkIfBlocked(userKey);
    }

    @Test
    public void shouldHandleNullUserKey_checkIfBlocked() throws RateLimitExceededException {
        rateLimitService.checkIfBlocked(null);
    }

    @Test
    public void shouldHandleEmptyUserKey() throws RateLimitExceededException {
        String userKey = "";

        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.checkIfBlocked(userKey);
        rateLimitService.resetRateLimit(userKey);
    }

    @Test
    public void shouldHandleSpecialCharactersInUserKey() throws RateLimitExceededException {
        String userKey = "user@example.com";

        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.checkIfBlocked(userKey);
        rateLimitService.resetRateLimit(userKey);
    }

    @Test
    public void shouldHandleVeryLongUserKey() throws RateLimitExceededException {
        String userKey = "a".repeat(1000);

        rateLimitService.trackFailedRequest(userKey);
        rateLimitService.checkIfBlocked(userKey);
        rateLimitService.resetRateLimit(userKey);
    }
}
