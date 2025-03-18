package org.example;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class DBMS {
    private static final String FILE_NAME = "database.txt";
    private static final Short BLOCK_SIZE = 50;  // 블록 크기
    private static final String jdbcUrl = "jdbc:mysql://localhost:3306/dbs_assignment";
    private static final String user = "root";
    private static final String password = "0000";


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
//            // Header 객체 생성
//            byte[] fieldNames = convertToByteArray(fieldNamesList);
//            byte[] fieldSizes = convertToByteArray(fieldSizesList);
//            byte[] fieldOrder = convertToByteArray(fieldOrderList);

//            Header header = new Header((byte) fieldCount, fieldNames, fieldSizes, fieldOrder);
//
//            // Header 객체를 직렬화하여 파일로 저장

        } catch (SQLException e) {
            System.out.println("Error while getting metadata: " + e.getMessage());
        }
    }


    public static void saveHeaderToBinary(String fileName, Short firstBlockPointer, byte fieldCount, List<String> fieldNames,
                                          List<Integer> fieldSizes, List<Integer> fieldOrder) {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(fileName))) {

            dos.writeShort(firstBlockPointer);

            // 1️⃣ 필드 개수 저장 (1바이트)
            dos.writeByte(fieldCount);

            // 2️⃣ 필드 이름을 하나의 문자열로 저장
            StringBuilder fieldNamesString = new StringBuilder();
            for (String name : fieldNames) {
                fieldNamesString.append(name);
            }
            dos.writeBytes(fieldNamesString.toString());

            // 3️⃣ 필드 크기 저장 (각 1바이트)
            for (int size : fieldSizes) {
                dos.writeByte(size);
            }

            // 4️⃣ 필드 순서 저장 (각 1바이트)
            for (int order : fieldOrder) {
                dos.writeByte(order);
            }
            byte[] freeSpace = new byte[32]; // 50바이트 - 18바이트 = 32바이트
            dos.write(freeSpace);

            System.out.println("Header metadata saved to " + fileName);
        } catch (IOException e) {
            System.out.println("Error saving header to binary file: " + e.getMessage());
        }
    }

    public static void readAndDeserializeFromBinary(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("Error: File '" + fileName + "' not found.");
            return;
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

            System.out.println("\n=== Loaded Header Metadata ===");
            System.out.println("First Block Pointer(2bytes): " + firstBlockPointer);
            System.out.println("Field Count(1bytes): " + fieldCount);
            System.out.println("Field Names(9bytes): " + fieldNames);
            System.out.println("Field Sizes(3bytes): " + fieldSizes);
            System.out.println("Field Order(3bytes): " + fieldOrder);

        } catch (IOException e) {
            System.out.println("Error reading binary file: " + e.getMessage());
        }
    }


    public static void loadHeaderFromFile(Scanner scanner) {
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
        readAndDeserializeFromBinary(fileName); //endbyte - 1까지 읽음

    }



    public static void readDataFileAndBulkInsert() {
        try(BufferedReader br = new BufferedReader(new FileReader("DataFile.txt"))){
            String fileName = br.readLine().trim();
            int recordCount = Integer.parseInt(br.readLine().trim());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
