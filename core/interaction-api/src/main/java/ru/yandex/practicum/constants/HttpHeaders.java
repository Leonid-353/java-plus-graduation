package ru.yandex.practicum.constants;

public final class HttpHeaders {
    public static final String USER_ID_HEADER = "X-EWM-USER-ID";

    private HttpHeaders() {
        throw new IllegalStateException("Utility class");
    }
}
