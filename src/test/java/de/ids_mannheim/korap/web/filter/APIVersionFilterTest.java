package de.ids_mannheim.korap.web.filter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriInfo;

public class APIVersionFilterTest {

	@Test
    public void testEmptyPathSegmentsThrowNotFound () {
        APIVersionFilter filter = createFilter(Set.of("v1.0", "v2.0"));
        ContainerRequestContext request = createRequest(Collections.emptyList());

        assertThrows(NotFoundException.class, () -> filter.filter(request));
    }

    @Test
    public void testUnsupportedVersionThrowNotFound () {
        APIVersionFilter filter = createFilter(Set.of("v1.0", "v2.0"));
        ContainerRequestContext request = createRequest(List.of(segment("v9.9")));

        assertThrows(NotFoundException.class, () -> filter.filter(request));
    }

    @Test
    public void testSupportedVersionPasses () {
        APIVersionFilter filter = createFilter(Set.of("v1.0", "v2.0"));
        ContainerRequestContext request = createRequest(List.of(segment("v1.0")));

        assertDoesNotThrow(() -> filter.filter(request));
    }

    private APIVersionFilter createFilter (Set<String> supportedVersions) {
        APIVersionFilter filter = new APIVersionFilter();
        KustvaktConfiguration filterConfig = new KustvaktConfiguration();
        filterConfig.setSupportedVersions(supportedVersions);
        ReflectionTestUtils.setField(filter, "config", filterConfig);
        return filter;
    }

    private ContainerRequestContext createRequest (List<PathSegment> pathSegments) {
        UriInfo uriInfo = (UriInfo) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { UriInfo.class },
                (proxy, method, args) -> {
                    if ("getPathSegments".equals(method.getName())) {
                        return pathSegments;
                    }
                    throw new UnsupportedOperationException(
                            "Unexpected UriInfo method: " + method.getName());
                });

        return (ContainerRequestContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] { ContainerRequestContext.class },
                (proxy, method, args) -> {
                    if ("getUriInfo".equals(method.getName())) {
                        return uriInfo;
                    }
                    throw new UnsupportedOperationException(
                            "Unexpected request method: " + method.getName());
                });
    }

    private PathSegment segment (String path) {
        return new PathSegment() {
            @Override
            public String getPath () {
                return path;
            }

            @Override
            public jakarta.ws.rs.core.MultivaluedMap<String, String> getMatrixParameters () {
                throw new UnsupportedOperationException();
            }
        };
    }
}
