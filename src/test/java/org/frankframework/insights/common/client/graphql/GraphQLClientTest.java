package org.frankframework.insights.common.client.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class GraphQLClientTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpGraphQlClient httpGraphQlClientMock;

    private TestGraphQLClient testClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private record MockEntity(String id, String value) {}

    private record MockConnectionDTO(List<Map<String, Object>> edges, GraphQLPageInfoDTO pageInfo) {}

    private static final GraphQLQuery MOCK_QUERY = new GraphQLQuery() {
        @Override
        public String getDocumentName() {
            return "GetMockEntity";
        }

        @Override
        public String getRetrievePath() {
            return "data.mockEntity";
        }
    };

    private static final GraphQLQuery MOCK_PAGINATED_QUERY = new GraphQLQuery() {
        @Override
        public String getDocumentName() {
            return "GetMockEntities";
        }

        @Override
        public String getRetrievePath() {
            return "data.mockEntities";
        }
    };

    private class TestGraphQLClient extends GraphQLClient {
        public TestGraphQLClient(String baseUrl, Consumer<WebClient.Builder> configurer, ObjectMapper objectMapper) {
            super(baseUrl, configurer, objectMapper);
        }

        @Override
        protected HttpGraphQlClient getGraphQlClient() {
            return httpGraphQlClientMock;
        }
    }

    @BeforeEach
    public void setUp() {
        testClient = new TestGraphQLClient("https://api.example.com/graphql", builder -> {}, objectMapper);
    }

    @Test
    public void fetchSingleEntity_Success_ReturnsEntity() throws GraphQLClientException {
        MockEntity expectedEntity = new MockEntity("1", "test");
        when(httpGraphQlClientMock
                        .documentName(MOCK_QUERY.getDocumentName())
                        .variables(anyMap())
                        .retrieve(MOCK_QUERY.getRetrievePath())
                        .toEntity(MockEntity.class))
                .thenReturn(Mono.just(expectedEntity));

        MockEntity actualEntity = testClient.fetchSingleEntity(MOCK_QUERY, new HashMap<>(), MockEntity.class);

        assertThat(actualEntity).isEqualTo(expectedEntity);
    }

    @Test
    public void fetchSingleEntity_ApiCallFails_ThrowsGraphQLClientException() {
        RuntimeException apiException = new RuntimeException("API call failed");
        when(httpGraphQlClientMock
                        .documentName(MOCK_QUERY.getDocumentName())
                        .variables(anyMap())
                        .retrieve(MOCK_QUERY.getRetrievePath())
                        .toEntity(MockEntity.class))
                .thenReturn(Mono.error(apiException));

        assertThatThrownBy(() -> testClient.fetchSingleEntity(MOCK_QUERY, new HashMap<>(), MockEntity.class))
                .isInstanceOf(GraphQLClientException.class)
                .hasMessage("Failed GraphQL request for document: GetMockEntity")
                .hasCause(apiException);
    }

    @Test
    public void fetchSingleEntity_ApiReturnsNull_ReturnsNull() throws GraphQLClientException {
        when(httpGraphQlClientMock
                        .documentName(MOCK_QUERY.getDocumentName())
                        .variables(anyMap())
                        .retrieve(MOCK_QUERY.getRetrievePath())
                        .toEntity(MockEntity.class))
                .thenReturn(Mono.empty());

        MockEntity actualEntity = testClient.fetchSingleEntity(MOCK_QUERY, new HashMap<>(), MockEntity.class);

        assertThat(actualEntity).isNull();
    }

    @Test
    public void fetchPaginatedCollection_SinglePage_Success() throws GraphQLClientException {
        Map<String, Object> node1 = Map.of("id", "1", "value", "A");
        GraphQLPageInfoDTO pageInfo = new GraphQLPageInfoDTO(false, null);
        MockConnectionDTO response = new MockConnectionDTO(List.of(node1), pageInfo);

        ParameterizedTypeReference<MockConnectionDTO> responseType = new ParameterizedTypeReference<>() {};
        when(httpGraphQlClientMock
                        .documentName(MOCK_PAGINATED_QUERY.getDocumentName())
                        .variables(anyMap())
                        .retrieve(MOCK_PAGINATED_QUERY.getRetrievePath())
                        .toEntity(responseType))
                .thenReturn(Mono.just(response));

        Function<MockConnectionDTO, Collection<Map<String, Object>>> extractor = MockConnectionDTO::edges;
        Function<MockConnectionDTO, GraphQLPageInfoDTO> pageInfoExtractor = MockConnectionDTO::pageInfo;

        Set<MockEntity> result = testClient.fetchPaginatedCollection(
                MOCK_PAGINATED_QUERY, new HashMap<>(), MockEntity.class, responseType, extractor, pageInfoExtractor);

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().id()).isEqualTo("1");
    }

    @Test
    public void fetchPaginatedCollection_MultiplePages_Success() throws GraphQLClientException {
        Map<String, Object> node1 = Map.of("id", "1", "value", "A");
        GraphQLPageInfoDTO pageInfo1 = new GraphQLPageInfoDTO(true, "cursor1");
        MockConnectionDTO response1 = new MockConnectionDTO(List.of(node1), pageInfo1);

        Map<String, Object> node2 = Map.of("id", "2", "value", "B");
        GraphQLPageInfoDTO pageInfo2 = new GraphQLPageInfoDTO(false, null);
        MockConnectionDTO response2 = new MockConnectionDTO(List.of(node2), pageInfo2);

        ParameterizedTypeReference<MockConnectionDTO> responseType = new ParameterizedTypeReference<>() {};
        when(httpGraphQlClientMock
                        .documentName(MOCK_PAGINATED_QUERY.getDocumentName())
                        .variables(anyMap())
                        .retrieve(MOCK_PAGINATED_QUERY.getRetrievePath())
                        .toEntity(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response1), Mono.just(response2));

        Function<MockConnectionDTO, Collection<Map<String, Object>>> extractor = MockConnectionDTO::edges;
        Function<MockConnectionDTO, GraphQLPageInfoDTO> pageInfoExtractor = MockConnectionDTO::pageInfo;

        Set<MockEntity> result = testClient.fetchPaginatedCollection(
                MOCK_PAGINATED_QUERY, new HashMap<>(), MockEntity.class, responseType, extractor, pageInfoExtractor);

        assertThat(result).hasSize(2).extracting(MockEntity::id).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    public void fetchPaginatedCollection_EmptyResult_ReturnsEmptySet() throws GraphQLClientException {
        GraphQLPageInfoDTO pageInfo = new GraphQLPageInfoDTO(false, null);
        MockConnectionDTO emptyResponse = new MockConnectionDTO(Collections.emptyList(), pageInfo);

        ParameterizedTypeReference<MockConnectionDTO> responseType = new ParameterizedTypeReference<>() {};
        when(httpGraphQlClientMock
                        .documentName(MOCK_PAGINATED_QUERY.getDocumentName())
                        .variables(anyMap())
                        .retrieve(MOCK_PAGINATED_QUERY.getRetrievePath())
                        .toEntity(responseType))
                .thenReturn(Mono.just(emptyResponse));

        Function<MockConnectionDTO, Collection<Map<String, Object>>> extractor = MockConnectionDTO::edges;
        Function<MockConnectionDTO, GraphQLPageInfoDTO> pageInfoExtractor = MockConnectionDTO::pageInfo;

        Set<MockEntity> result = testClient.fetchPaginatedCollection(
                MOCK_PAGINATED_QUERY, new HashMap<>(), MockEntity.class, responseType, extractor, pageInfoExtractor);

        assertThat(result).isEmpty();
    }
}
