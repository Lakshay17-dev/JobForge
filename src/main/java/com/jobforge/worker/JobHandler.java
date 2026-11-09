
package com.jobforge.worker;

import com.fasterxml.jackson.databind.JsonNode;

/** One kind of job. Throw any exception to signal failure. */
public interface JobHandler {
    /** The job type string this handler runs, e.g. "echo". */
    String type();

    void handle(JsonNode payload) throws Exception;
}
