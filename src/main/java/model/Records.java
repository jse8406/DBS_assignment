package model;

public class Records {
    private final String id;
    private final String code;
    private final String tag;
    private final short nextPointer;

    public Records(String id, String code, String tag, short nextPointer) {
        this.id = id;
        this.code = code;
        this.tag = tag;
        this.nextPointer = nextPointer;
    }
    // overloading constructor
    public Records(String id, String code, String tag) {
        this(id, code, tag, (short) -1);  // nextPointer 기본값 -1
    }


    public String getId() { return id; }
    public String getCode() { return code; }
    public String getTag() { return tag; }
    @Override
    public String toString() {
        return "Record : " +
                "id='" + id + '\'' +
                ", code='" + code + '\'' +
                ", tag='" + tag + '\'' +
                ", nextPointer=" + nextPointer;
    }
}

