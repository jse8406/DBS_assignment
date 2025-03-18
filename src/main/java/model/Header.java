package model;

import java.io.Serializable;
import java.util.Arrays;

public class Header implements Serializable {
    private static final int BLOCK_SIZE = 50;  // 블록 크기
    private final short firstBlockPointer = BLOCK_SIZE;
    private byte fieldCount;
    private byte[] fieldNames;
    private byte[] fieldSizes;
    private byte[] fieldOrder;
    private final byte[] freeSpace = new byte[23]; // 남은 공간 (필요 시 확장 가능)

}
