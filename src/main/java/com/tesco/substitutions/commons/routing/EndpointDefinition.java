//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.tesco.substitutions.commons.routing;

import com.tesco.personalisation.commons.routing.EndpointDsl.Endpoint;
import java.util.Collections;
import java.util.List;

public interface EndpointDefinition {
    Endpoint prepare();

    default List<Endpoint> prepareEndPoints() {
        return Collections.singletonList(this.prepare());
    }
}
