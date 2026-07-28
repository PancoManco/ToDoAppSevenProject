package ru.pancomanco.scheduler.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record DailySummaryResponseDto(
        @NotNull
        List<@Valid UserTaskSummary> users
) {
    public record UserTaskSummary(
            @NotNull
            @Positive
            Long userId,

            @NotBlank
            @Size(max = 254)
            @Email
            String email,

            @NotBlank
            @Size(max = 100)
            String name,

            @PositiveOrZero
            int completedCount,

            @NotNull
            @Size(max = 5)
            List<
                    @NotBlank
                    @Size(max = 255)
                            String
                    > completedTitles,

            @PositiveOrZero
            int pendingCount,

            @NotNull
            @Size(max = 5)
            List<
                    @NotBlank
                    @Size(max = 255)
                            String
                    > pendingTitles
    ) {
    }
}
