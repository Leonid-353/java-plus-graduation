package ru.yandex.practicum.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.dto.request.enums.Status;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventRequestStatusUpdateRequest {
    @NotNull
    private List<Long> requestIds;
    @NotNull
    private Status status;
}