package sample;

import java.io.Serializable;

public class HelloWorld extends Exception implements Cloneable, Serializable {
    private static final int intValue = 10;

    public void test() {
        int a = 1;
        int b = 2;
        int c = a + b;
    }
}
