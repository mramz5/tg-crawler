package org.drinkless.tdlib;

import lombok.AllArgsConstructor;

import java.awt.*;

@AllArgsConstructor
public class Constant {
    public static final String GCS = "gcs";
    public static final String GC = "gc";
    public static final String ME = "me";
    public static final String SM = "sm";
    public static final String SCW = "scw";
    public static final String LO = "lo";
    public static final String Q = "q";

    public static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
    public static final Color BLACK_GREY = new Color(30, 30, 30);
    public static final Color BONE = new Color(197, 197, 197);
    public static final Font TERMINAL_FONT = new Font("Tahoma", Font.BOLD, 15);

}
