package ru.yandex.practicum.service.constants;

public class ActionWeights {

    private ActionWeights() {
        throw new IllegalStateException("Utility class");
    }

    public static final Double VIEW = 0.4;

    public static final Double REGISTER = 0.8;

    public static final Double LIKE = 1.0;

    static {
        validateWeights();
    }

    private static void validateWeights() {
        if (VIEW < 0 || VIEW > 1) {
            throw new IllegalStateException("VIEW вес должен быть в диапазоне [0, 1]");
        }
        if (REGISTER < 0 || REGISTER > 1) {
            throw new IllegalStateException("REGISTER вес должен быть в диапазоне [0, 1]");
        }
        if (LIKE < 0 || LIKE > 1) {
            throw new IllegalStateException("LIKE вес должен быть в диапазоне [0, 1]");
        }
        if (!(VIEW < REGISTER && REGISTER < LIKE)) {
            throw new IllegalStateException("Веса должны возрастать: VIEW < REGISTER < LIKE");
        }
    }
}
