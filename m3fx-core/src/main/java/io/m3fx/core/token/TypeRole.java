package io.m3fx.core.token;

/**
 * One of the 15 M3 type roles with its spec size, line height, tracking and weight.
 *
 * <p>See <a href="https://m3.material.io/styles/typography/overview">M3 typography spec</a>.
 */
public record TypeRole(String name, double size, double lineHeight, double tracking, int weight) {

    /**
     * Creates a type role.
     *
     * @param name role name such as {@code label-large}
     * @param size font size in sp
     * @param lineHeight line height in sp
     * @param tracking letter tracking in sp
     * @param weight font weight (400 or 500)
     */
    public TypeRole {}
}
