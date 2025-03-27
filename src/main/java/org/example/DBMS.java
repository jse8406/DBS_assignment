package org.example;


import model.Header;
import model.Records;

import java.io.*;
import java.sql.*;
import java.util.*;

public class DBMS {
    private static final String FILE_NAME = "database.txt";
    private static final Short BLOCK_SIZE = 50;  // 블록 크기
    private static final String jdbcUrl = "jdbc:mysql://localhost:3306/dbs_assignment";
    private static final String user = "root";
    private static final String password = "0000";

// 정보를 주면 테이블 생성하고 거기서 메타데이터 가져와서 bin 파일로 저장
    public static void createFileAndTable(Scanner scanner) {

        // get file name
        System.out.print("Enter file name: ");
        String fileName = scanner.nextLine().trim() + ".bin";

        // get field names
        System.out.print("Enter field names separated by space: ");
        String fieldsInput = scanner.nextLine();
        List<String> fieldNames = Arrays.asList(fieldsInput.split(" "));

        // get field sizes
        List<Integer> fieldSizes = new ArrayList<>();
        System.out.print("Enter field sizes separated by space (same order as field names): ");
        String sizesInput = scanner.nextLine();
        List<String> sizeStrings = Arrays.asList(sizesInput.split(" "));

        // Ensure sizes and field names match
        if (fieldNames.size() != sizeStrings.size()) {
            System.out.println("Error: Number of field names and field sizes do not match!");
            return;
        }

        // Convert size inputs to integers
        for (String sizeStr : sizeStrings) {
            try {
                fieldSizes.add(Integer.parseInt(sizeStr));
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid field size input. Must be an integer.");
                return;
            }
        }

        createTableInDatabase(fileName, fieldNames, fieldSizes);
    }
    private static void createTableInDatabase(String tableName, List<String> fieldNames, List<Integer> fieldSizes) {
        tableName = tableName.replace(".bin", ""); // Remove .bin extension
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS dbs_assignment." + tableName + " (");

        for (int i = 0; i < fieldNames.size(); i++) {
            sql.append(fieldNames.get(i)).append(" CHAR(").append(fieldSizes.get(i)).append("), ");
        }
        sql.append("PRIMARY KEY (").append(fieldNames.getFirst()).append(")");
        sql.append(");");

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql.toString());
            System.out.println("Table Created in Database: " + tableName);
            getMetaDataFromTable(tableName);
        } catch (SQLException e) {
            System.out.println("Error while creating table: " + e.getMessage());
        }
    }
    public static void getMetaDataFromTable(String tableName) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
            DatabaseMetaData dbMetaData = conn.getMetaData();
            ResultSet columns = dbMetaData.getColumns(null, null, tableName, null);

            System.out.println("\n=== Table Metadata: " + tableName + " ===");

            List<String> fieldNamesList = new ArrayList<>();
            List<Integer> fieldSizesList = new ArrayList<>();
            List<Integer> fieldOrderList = new ArrayList<>();
            int fieldCount = 0;

            while (columns.next()) {
                fieldCount++;
                fieldNamesList.add(columns.getString("COLUMN_NAME"));
                fieldSizesList.add(columns.getInt("COLUMN_SIZE"));
                fieldOrderList.add(fieldCount - 1); // 필드 순서 (0부터 시작)
            }

            // 메타데이터 출력
            System.out.println("Field Count: " + fieldCount);
            System.out.println("Field Names: " + fieldNamesList);
            System.out.println("Field Sizes: " + fieldSizesList);
            System.out.println("Field Order: " + fieldOrderList);


            //bin 파일로 저장
            saveHeaderToBinary(tableName + ".bin", BLOCK_SIZE, (byte) fieldCount, fieldNamesList, fieldSizesList, fieldOrderList);

        } catch (SQLException e) {
            System.out.println("Error while getting metadata: " + e.getMessage());
        }
    }
    public static void saveHeaderToBinary(String fileName, Short firstBlockPointer, byte fieldCount, List<String> fieldNames,
                                          List<Integer> fieldSizes, List<Integer> fieldOrder) {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(fileName))) {

            dos.writeShort(firstBlockPointer);

            dos.writeByte(fieldCount);

            // string builder를 통해 name 하나의 문자열로 합치기
            StringBuilder fieldNamesString = new StringBuilder();
            for (String name : fieldNames) {
                fieldNamesString.append(name);
            }
            byte[] fieldNamesBytes = fieldNamesString.toString().getBytes();
            dos.write(fieldNamesBytes);

            for (int size : fieldSizes) {
                dos.writeByte(size);
            }

            for (int order : fieldOrder) {
                dos.writeByte(order);
            }
            // pointer 1, fieldCount 1, fieldNames 9, fieldSizes 3, fieldOrder 3
            int usedSpace = 2 + 1 + fieldNamesBytes.length + fieldSizes.size() + fieldOrder.size();
            int freeSpaceSize = BLOCK_SIZE - usedSpace;

            //  7. freeSpace 채우기
            if (freeSpaceSize > 0) {
                dos.write(new byte[freeSpaceSize]); // 남은 공간만큼 0으로 채우기
            }
            System.out.println("Header metadata saved to " + fileName);
        } catch (IOException e) {
            System.out.println("Error saving header to binary file: " + e.getMessage());
        }
    }


    public static Header readAndDeserializeHeaderFromBinary(String fileName) {

        if (!fileName.endsWith(".bin")) {
            fileName += ".bin";
        }

        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("Error: File '" + fileName + "' not found.");
            return null;
        }

        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            // 1. firstBlockPointer (2바이트)
            short firstBlockPointer = dis.readShort();

            // 2. fieldCount (1바이트)
            byte fieldCount = dis.readByte();

            // 3. fieldNames (9바이트 2+4+3 : id code tag)
            byte[] fieldNamesBytes = new byte[9];
            dis.readFully(fieldNamesBytes);
            String fieldNames = new String(fieldNamesBytes);

            // 4. fieldSizes (3바이트)
            List<Integer> fieldSizes = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                fieldSizes.add((int) dis.readByte());
            }

            // 5. fieldOrder (3바이트)
            List<Integer> fieldOrder = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                fieldOrder.add((int) dis.readByte());
            }
//            // 6. Free space (32바이트) → 무시
            byte[] freeSpace = new byte[32];
            dis.readFully(freeSpace);

//            System.out.println("\n=== Loaded Header Metadata ===");
//            System.out.println("First Block Pointer(2bytes): " + firstBlockPointer);
//            System.out.println("Field Count(1bytes): " + fieldCount);
//            System.out.println("Field Names(9bytes): " + fieldNames);
//            System.out.println("Field Sizes(3bytes): " + fieldSizes);
//            System.out.println("Field Order(3bytes): " + fieldOrder);
            return new Header(firstBlockPointer, fieldCount, fieldNames, fieldSizes, fieldOrder);

        } catch (IOException e) {
            System.out.println("Error reading binary file: " + e.getMessage());
            return null;
        }
    }
    public static void loadHeaderBlockFromFile(Scanner scanner) {
        System.out.print("Enter file name to get metadata: ");
        String fileName = scanner.nextLine().trim();

        if (!fileName.endsWith(".bin")) {
            fileName += ".bin";
        }

        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("Error: File '" + fileName + "' not found.");
            return;
        }
        Header header = readAndDeserializeHeaderFromBinary(fileName);
        assert header != null;
        System.out.println(header.getFirstBlockPointer());
        System.out.println(header.getFieldCount());
        System.out.println(header.getFieldNames());
        System.out.println(header.getFieldSizes());
        System.out.println(header.getFieldOrder());

    }



    public static void readDataFileAndBulkInsert() {
        List<Records> records;
        try (BufferedReader br = new BufferedReader(new FileReader("DataFile.txt"))) {
            String fileName = br.readLine().trim();
            int recordCount = Integer.parseInt(br.readLine().trim());
            records = new ArrayList<>();
            for (int i = 0; i < recordCount; i++) {
                String line = br.readLine().trim();
                String[] fields = line.split(";"); // `;`로 필드 구분

                // ✅ 5. 레코드 객체 생성 및 리스트에 추가
                String id = fields[0];
                String code = fields[1].equals("null") ? null : fields[1];
                String tag = fields[2].equals("null") ? null : fields[2];

                records.add(new Records(id, code, tag));
            }

            // ✅ 6. 데이터 출력
            System.out.println("File Name: " + fileName);
            System.out.println("Record Count: " + recordCount);
            System.out.println("Records:");
            for (Records record : records) {
                System.out.println(record);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        saveRecordToBinaryFile(records, "records.bin");

    }
    public static void saveRecordToBinaryFile(List<Records> records, String binFileName) {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(binFileName))) {
            for (Records record : records) {
                // nullbitmap 저장
                byte nullbitmap = 0;

                if (record.getCode() == null) {
                    nullbitmap |= 1 << 6;
                }

                if (record.getTag() == null) {
                    nullbitmap |= 1 << 5;
                }
                dos.writeByte(nullbitmap);

                //  ID (5바이트) 저장
                writeFixedString(dos, record.getId(), 5);

                //  Code (4바이트) 저장 (null이면 저장x 처리)
                if(record.getCode() != null){
                    writeFixedString(dos,record.getCode(), 4);
                }

                //  Tag (3바이트) 저장 (null이면 저장x 처리)
                if(record.getTag() != null){
                    writeFixedString(dos,record.getTag(), 3);
                }
            }
            System.out.println("Records saved to binary file: " + binFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeFixedString(DataOutputStream dos, String data, int size) throws IOException {
        byte[] bytes = data.getBytes(); // 문자열을 바이트 배열로 변환
        int length = Math.min(bytes.length, size); // 크기를 초과하면 잘라냄

        dos.write(bytes, 0, length); //  실제 데이터 쓰기

        //  남은 공간을 공백(0)으로 채움
        for (int i = length; i < size; i++) {
            dos.writeByte(0); // Padding (빈 공간)
        }
    }

    public static List<Records> readRecordsFromBinaryFile(String binFileName) {
        List<Records> records = new ArrayList<>();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(binFileName))) {
            while (dis.available() > 0) {
                //  1. nullBitMap (1바이트) 읽기
                byte nullBitMap = dis.readByte();

                //  2. ID (5바이트) 읽기
                String id = readFixedString(dis, 5);

                //  3. Code (4바이트) 읽기 (nullBitMap의 6번째 비트가 1이면 null)
                String code = (isNullBitSet(nullBitMap, 6)) ? null : readFixedString(dis, 4);

                //  4. Tag (3바이트) 읽기 (nullBitMap의 5번째 비트가 1이면 null)
                String tag = (isNullBitSet(nullBitMap, 5)) ? null : readFixedString(dis, 3);

                //  5. Record 객체 생성 및 리스트에 추가
                records.add(new Records(id, code, tag));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }

    /**
     *  `readFully()`를 사용하여 고정된 크기의 문자열을 읽는 함수
     */
    private static String readFixedString(DataInputStream dis, int size) throws IOException {
        byte[] bytes = new byte[size];
        dis.readFully(bytes);
        return new String(bytes).trim(); //  공백 제거 후 문자열 반환
    }

    /**
     *  특정 비트가 1인지 확인하는 함수
     */
    private static boolean isNullBitSet(byte nullBitMap, int bitPosition) {
        return (nullBitMap & (1 << bitPosition)) != 0;
    }









    public static void searchField(Scanner scanner) {
        System.out.print("Enter field value to search: ");
        String searchValue = scanner.nextLine();

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(searchValue)) {
                    System.out.println("Search result: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error while searching: " + e.getMessage());
        }
    }
    public static void searchRecord(Scanner scanner) {
        System.out.print("Enter Record ID to search: ");
        String searchId = scanner.nextLine();

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length > 0 && fields[0].equals(searchId)) {
                    System.out.println("Record found: " + line);
                    return;
                }
            }
            System.out.println("Record with the given ID not found.");
        } catch (IOException e) {
            System.out.println("Error while searching: " + e.getMessage());
        }
    }


}
