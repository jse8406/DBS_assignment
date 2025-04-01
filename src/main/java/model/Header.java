package model;


import java.util.ArrayList;
import java.util.List;

public class Header {
    private short firstBlockPointer;  // 2바이트
    private byte fieldCount;          // 1바이트
    private String fieldNames;        // 9바이트 (2+4+3)
    private List<Integer> fieldSizes; // 3바이트
    private List<Integer> fieldOrder; // 3바이트

    public Header(short firstBlockPointer, byte fieldCount, String fieldNames,
                  List<Integer> fieldSizes, List<Integer> fieldOrder) {
        this.firstBlockPointer = firstBlockPointer;
        this.fieldCount = fieldCount;
        this.fieldNames = fieldNames;
        this.fieldSizes = fieldSizes;
        this.fieldOrder = fieldOrder;
    }

    public short getFirstBlockPointer() { return firstBlockPointer; }
    public byte getFieldCount() { return fieldCount; }
    public String getFieldNames() { return fieldNames; }
    public List<Integer> getFieldSizes() { return fieldSizes; }
    public List<Integer> getFieldOrder() { return fieldOrder; }
    public List<String> getFieldNamesList() {
        List<String> result = new ArrayList<>();

        int cursor = 0;
        for (int size : fieldSizes) {
            // fieldNames 문자열에서 사이즈만큼 잘라서 필드 이름 추출
            String field = fieldNames.substring(cursor, cursor + size);
            result.add(field);
            cursor += size;
        }
        return result;
    }

    @Override
    public String toString() {
        return "\n=== Header Metadata ===\n" +
                "First Block Pointer(2 bytes): " + firstBlockPointer + "\n" +
                "Field Count(1 byte): " + fieldCount + "\n" +
                "Field Names(9 bytes): " + fieldNames + "\n" +
                "Field Sizes(3 bytes): " + fieldSizes + "\n" +
                "Field Order(3 bytes): " + fieldOrder + "\n";
    }
}

