package com.github.qfusion.fakeclient;

/**
 * A Qfusion console character color.
 */
public enum Color {
    BLACK(0, 0, 0, 0),
    RED(1, 255, 0, 0),
    GREEN(2, 0, 255, 0),
    YELLOW(3, 255, 255, 0),
    BLUE(4, 0, 0, 192),
    CYAN(5, 0, 128, 255),
    MAGENTA(6, 192, 0, 128),
    WHITE(7, 255, 255, 0),
    ORANGE(8, 192, 192, 0),
    GREY(9, 128, 128, 128);

    int index;
    int redByte;
    int greenByte;
    int blueByte;

    Color(int index, int redByte, int greenByte, int blueByte) {
        this.index = index;
        this.redByte = redByte;
        this.greenByte = greenByte;
        this.blueByte = blueByte;
    }

    /**
     * @return A numeric index of this color corresponding to the color escape sequence.
     */
    public int getIndex() { return index; }

    /**
     * @return A red component in [0, 255] range
     */
    public int getRedByte() { return redByte; }

    /**
     * @return A green component in [0, 255] range
     */
    public int getGreenByte() { return greenByte; }

    /**
     * @return A blue component in [0, 255] range
     */
    public int getBlueByte() { return blueByte; }
}
