package net.runelite.client.plugins.flux;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KeyName
{
    KN_1("1", 1),
    KN_2("2", 2),
    KN_3("3", 3),
    KN_4("4", 4),
    KN_5("5", 5),
    KN_6("6", 6),
    KN_7("7", 7),
    KN_UP("UP", 8),
    KN_DOWN("DOWN", 9),
    KN_KEYBIND("KEYBIND", 10);

    private final String name;
    private final int value;

    @Override
    public String toString() {
        return name;
    }

    public static KeyName fromIndex(int value) {
        for (KeyName k : KeyName.values()) {
            if (k.getValue() == value) {
                return k;
            }
        }
        throw new IllegalArgumentException("Invalid KeyName index: " + value);
    }

    public static KeyName getByValue(int val) {
    for (KeyName k : values()) {
        if (k.getValue() == val) return k;
    }
    return null;
    }
}
