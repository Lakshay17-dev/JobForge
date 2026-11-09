package com.jobforge.worker;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class HandlerRegistry {
    private final Map<String, JobHandler> handlers;

    public HandlerRegistry(List<JobHandler> allHandlers) {
        this.handlers = allHandlers.stream().collect(Collectors.toUnmodifiableMap(
                JobHandler::type, Function.identity()));
    }

    public Optional<JobHandler> find(String type) {
        return Optional.ofNullable(handlers.get(type));
    }
}
