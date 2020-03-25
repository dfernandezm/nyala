
package com.tesco.substitutions.commons.routing;

import java.util.Collections;
import java.util.List;

public interface EndpointDefinition {
    EndpointDsl.Endpoint prepare();

    default List<EndpointDsl.Endpoint> prepareEndPoints() {
        return Collections.singletonList(this.prepare());
    }
}
