package com.tronai.supervisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.hybrid.HybridRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupervisorServiceUnitTest {

    @Mock SupervisorPlanRepository plans;
    @Mock SupervisorMilestoneRepository milestones;
    @Mock SupervisorEventRepository events;
    @Mock HybridRoutingService hybrid;

    SupervisorService service;

    @BeforeEach
    void setUp() {
        service = new SupervisorService(plans, milestones, events, hybrid, new ObjectMapper(), null);
        ReflectionTestUtils.setField(service, "localModel", "qwen3:8b");
        lenient().when(plans.save(any())).thenAnswer(inv -> {
            SupervisorPlanEntity p = inv.getArgument(0);
            lenient().when(plans.findByIdAndUserId(p.getId(), p.getUserId())).thenReturn(java.util.Optional.of(p));
            return p;
        });
        lenient().when(milestones.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(events.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(milestones.findByPlanIdOrderByMilestoneIndexAsc(any())).thenReturn(java.util.List.of());
        lenient().when(events.findByPlanIdOrderByCreatedAtAsc(any())).thenReturn(java.util.List.of());
    }

    @Test
    void createPlanPersistsImmutableGoal() {
        when(hybrid.selectSupervisorRoute()).thenReturn("CLOUD_GATEWAY");
        when(hybrid.tryAcquireAiSlot()).thenReturn(true);

        Map<String, Object> view = service.createPlan(42L, "Build a three-file website", null);

        assertThat(view.get("originalGoal")).isEqualTo("Build a three-file website");
        assertThat(view.get("status")).isEqualTo("PLANNED");
        assertThat(view.get("engine")).isNotNull();
    }

    @Test
    void createPlanFailsWhenPaused() {
        when(hybrid.selectSupervisorRoute()).thenReturn("PAUSED");
        assertThatThrownBy(() -> service.createPlan(1L, "anything", null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void createPlanFailsWhenQueueSaturated() {
        when(hybrid.selectSupervisorRoute()).thenReturn("LOCAL_OLLAMA");
        when(hybrid.tryAcquireAiSlot()).thenReturn(false);
        assertThatThrownBy(() -> service.createPlan(1L, "anything", null))
                .isInstanceOf(ResponseStatusException.class);
    }
}
