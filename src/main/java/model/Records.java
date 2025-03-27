package model;

public class Records {
    private final String id;
    private final String code;
    private final String tag;

    public Records(String id, String code, String tag) {
        this.id = id;
        this.code = code;
        this.tag = tag;
    }

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getTag() { return tag; }

    @Override
    public String toString() {
        return "Record{" +
                "id='" + id + '\'' +
                ", code='" + code + '\'' +
                ", tag='" + tag + '\'' +
                '}';
    }
}

