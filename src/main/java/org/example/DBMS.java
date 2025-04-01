package org.example;


import model.Header;
import model.Records;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class DBMS {
    private static final String FILE_NAME = "database.txt";
    private static final Short BLOCK_SIZE = 50;  // 블록 크기
    private static final String jdbcUrl = "jdbc:mysql://localhost:3306/dbs_assignment";
    private static final String user = "root";
    private static final String password = "1111";

    // 정보를 주면 테이블 생성하고 거기서 메타데이터 가져와서 bin 파일로 저장
    public static void createFileAndTable(Scanner scanner) {
        // init pointer
        short firstBlockPointer_init = -1;

        // 1. 파일 이름 입력
        System.out.print("Enter file name: ");
        String fileName = scanner.nextLine().trim() + ".bin";
        String tableName = fileName.replace(".bin", "");

        // 2. 필드 이름 입력
        System.out.print("Enter field names separated by space: ");
        List<String> fieldNames = Arrays.asList(scanner.nextLine().trim().split(" "));

        // 3. 필드 크기 입력
        System.out.print("Enter field sizes separated by space (same order as field names): ");
        List<String> sizeStrings = Arrays.asList(scanner.nextLine().trim().split(" "));
        if (fieldNames.size() != sizeStrings.size()) {
            System.out.println("Error: Number of field names and field sizes do not match!");
            return;
        }

        List<Integer> fieldSizes = new ArrayList<>();
        for (String sizeStr : sizeStrings) {
            try {
                fieldSizes.add(Integer.parseInt(sizeStr));
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid field size input. Must be an integer.");
                return;
            }
        }
        // 4. 필드 개수 저장
        byte filedCount = (byte) fieldNames.size();

        // 5. 파일 순서 저장
        List<Integer> fieldOrder = new ArrayList<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            fieldOrder.add(i);
        }
        //


        // 테이블 생성
        createTableInDatabase(tableName, fieldNames, fieldSizes);
        // 헤더 정보 저장과 함께 bin파일 생성
        saveHeaderToBinary(fileName, firstBlockPointer_init, filedCount, fieldNames, fieldSizes, fieldOrder);
    }
    // 입력 정보 기반 MYSQL DB에 table 생성
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
        } catch (SQLException e) {
            System.out.println("Error while creating table: " + e.getMessage());
        }
    }
    // 입력 정보 기반 header 정보
    public static void saveHeaderToBinary(String fileName, Short firstBlockPointer, byte fieldCount, List<String> fieldNames,
                                          List<Integer> fieldSizes, List<Integer> fieldOrder) {
    // create file with header block.
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

    // 헤더 읽은 후 역직렬화를 통해 정보 추출
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

            return new Header(firstBlockPointer, fieldCount, fieldNames, fieldSizes, fieldOrder);

        } catch (IOException e) {
            System.out.println("Error reading binary file: " + e.getMessage());
            return null;
        }
    }


    public static List<Records> readDataFile() {
    // read dataFile.txt
        List<Records> records;
        try (BufferedReader br = new BufferedReader(new FileReader("DataFile.txt"))) {
            String fileName = br.readLine().trim();
            int recordCount = Integer.parseInt(br.readLine().trim());
            records = new ArrayList<>();
            for (int i = 0; i < recordCount; i++) {
                String line = br.readLine().trim();
                String[] fields = line.split(";"); // `;`로 필드 구분

                //  5. 레코드 객체 생성 및 리스트에 추가
                String id = fields[0];
                String code = fields[1].equals("null") ? null : fields[1];
                String tag = fields[2].equals("null") ? null : fields[2];

                records.add(new Records(id, code, tag));
            }

            //  6. 데이터 출력
            System.out.println("File Name: " + fileName);
            System.out.println("Record Count: " + recordCount);
            System.out.println("Records:");
            for (Records record : records) {
                System.out.println(record);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return records;

    }
    public static void saveRecordToBinaryFile(List<Records> records, String binFileName) {
        try (RandomAccessFile raf = new RandomAccessFile(binFileName, "rw")) {
            List<Long> recordPositions = new ArrayList<>();

            raf.seek(BLOCK_SIZE); // 헤더 영역 패스

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            long fileOffset = BLOCK_SIZE; // 실제 파일에 쓰여질 위치

            for (Records record : records) {
                ByteArrayOutputStream recordBuffer = new ByteArrayOutputStream();

                byte nullBitmap = 0;
                if (record.getCode() == null) nullBitmap |= 1 << 6;
                if (record.getTag() == null) nullBitmap |= 1 << 5;
                recordBuffer.write(nullBitmap);

                writeFixedString(recordBuffer, record.getId(), 5);
                if (record.getCode() != null) writeFixedString(recordBuffer, record.getCode(), 4);
                if (record.getTag() != null) writeFixedString(recordBuffer, record.getTag(), 3);

                // nextRecordPointer 자리 예약 (2 bytes)
                recordBuffer.write(0);
                recordBuffer.write(0);

                byte[] recordBytes = recordBuffer.toByteArray();

                // 블록 크기 초과 여부 체크
                if (buffer.size() + recordBytes.length > BLOCK_SIZE) {
                    int padding = BLOCK_SIZE - buffer.size();
                    if(padding> 0){
                        buffer.write(new byte[padding]);
                    }
                    raf.write(buffer.toByteArray());
                    fileOffset += BLOCK_SIZE;
                    buffer.reset();
                }

                recordPositions.add(fileOffset + buffer.size());
                buffer.write(recordBytes);
            }

            // 마지막 버퍼 쓰기
            if (buffer.size() > 0) {
                int padding = BLOCK_SIZE - buffer.size();
                if(padding>0){
                    buffer.write(new byte[padding]);
                }
                raf.write(buffer.toByteArray());
            }

            // nextRecordPointer 업데이트
            for (int i = 0; i < recordPositions.size() - 1; i++) {
                long currentPos = recordPositions.get(i);
                long nextPos = recordPositions.get(i + 1);
                raf.seek(currentPos);

                byte bitmap = raf.readByte();
                raf.skipBytes(5);
                if ((bitmap & (1 << 6)) == 0) raf.skipBytes(4);
                if ((bitmap & (1 << 5)) == 0) raf.skipBytes(3);

                raf.writeShort((short) nextPos);
            }

            // 첫 번째 레코드 offset 을 헤더에 기록
            if (!recordPositions.isEmpty()) {
                raf.seek(0);
                raf.writeShort((short)(long)recordPositions.getFirst());
            }
            // 마지막 레코드 nextPointer 를 명시적으로 -1로 써주기
            if (!recordPositions.isEmpty()) {
                long lastPos = recordPositions.getLast();
                raf.seek(lastPos);

                byte bitmap = raf.readByte();
                raf.skipBytes(5);
                if ((bitmap & (1 << 6)) == 0) raf.skipBytes(4);
                if ((bitmap & (1 << 5)) == 0) raf.skipBytes(3);

                raf.writeShort((short) -1); // 마지막 레코드만 -1로 명시
            }


            System.out.println("Records saved with buffering and nextRecordPointer info.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void writeFixedString(OutputStream os, String str, int length) throws IOException {
        byte[] bytes = str != null ? str.getBytes(StandardCharsets.UTF_8) : new byte[0];
        byte[] padded = Arrays.copyOf(bytes, length);
        os.write(padded);
    }



    public static List<Records> readRecordsFromBlock(String binFileName, int blockNumber) {
        Header header = readAndDeserializeHeaderFromBinary(binFileName);
        if (header == null) {
            System.out.println("Header is empty");
            return Collections.emptyList();
        }

        Integer idByte = header.getFieldSizes().get(0);
        Integer codeByte = header.getFieldSizes().get(1);
        Integer tagByte = header.getFieldSizes().get(2);

        long blockOffset = (long) blockNumber * BLOCK_SIZE;
        long blockEnd = blockOffset + BLOCK_SIZE;

        List<Records> records = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(binFileName, "r")) {
            raf.seek(blockOffset);
            // 마지막 블록 free space 읽기 방지 && 그 다음 블록 읽기 전까지
            while (blockOffset > -1 && blockOffset < blockEnd) {
                byte nullBitMap = raf.readByte();
                String id = readNByte(raf, idByte);

                String code = (isNullBitSet(nullBitMap, 6)) ? null : readNByte(raf, codeByte);
                String tag  = (isNullBitSet(nullBitMap, 5)) ? null : readNByte(raf, tagByte);

                short nextPointer = raf.readShort();
                blockOffset = nextPointer;
                records.add(new Records(id, code, tag, nextPointer));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return records;
    }
    private static String readNByte(RandomAccessFile raf, int size) throws IOException {
        byte[] bytes = new byte[size];
        raf.readFully(bytes);
        return new String(bytes).trim(); // 공백 제거 후 문자열 반환
    }
    private static boolean isNullBitSet(byte nullBitMap, int bitPosition) {
        return (nullBitMap & (1 << bitPosition)) != 0;
    }









    public static void searchField(Scanner scanner) {
        // 1. 파일 이름 입력
        System.out.println("Enter file name to search:");
        String fileName = scanner.nextLine().trim() + ".bin";

        // 3. 파일 존재 확인
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File not found: " + fileName);
            return;
        }
        // 2. 필드 이름 입력
        System.out.print("Enter field name to extract values (id/code/tag): ");
        String fieldName = scanner.nextLine().trim().toLowerCase();


        long fileLength = file.length();
        int totalBlocks = (int) (fileLength / BLOCK_SIZE);

        // 4. 필드 이름 유효성 체크
        if (!fieldName.equals("id") && !fieldName.equals("code") && !fieldName.equals("tag")) {
            System.out.println("Error: Field name '" + fieldName + "' does not exist. Use: id, code, or tag.");
            return;
        }

        System.out.println("Values of field '" + fieldName + "':");

        // 5. 블록 순회
        for (int blockNum = 1; blockNum < totalBlocks; blockNum++) {
            List<Records> recordsInBlock = readRecordsFromBlock(fileName, blockNum);

            for (Records record : recordsInBlock) {
                String value = switch (fieldName) {
                    case "id" -> record.getId();
                    case "code" -> record.getCode();
                    case "tag" -> record.getTag();
                    default -> null; // 이미 위에서 검사했지만 안정성용
                };

                System.out.println("\"" + value + "\"");

            }
        }
    }




    public static void searchRecord(Scanner scanner) {
        System.out.print("Enter file name to search: ");
        String fileName = scanner.nextLine().trim() + ".bin";

        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File not found: " + fileName);
            return;
        }

        System.out.print("Enter start ID (or MIN): ");
        String startInput = scanner.nextLine().trim();

        System.out.print("Enter end ID (or MAX): ");
        String endInput = scanner.nextLine().trim();

        int startId = 0;
        int endId = Integer.MAX_VALUE;

        try {
            if (!startInput.equalsIgnoreCase("MIN")) {
                startId = Integer.parseInt(startInput);
            }
            if (!endInput.equalsIgnoreCase("MAX")) {
                endId = Integer.parseInt(endInput);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid range input. Please enter numbers or MIN/MAX.");
            return;
        }

        long fileLength = file.length();
        int totalBlocks = (int) (fileLength / BLOCK_SIZE);

        List<Records> foundRecords = new ArrayList<>();

        outerLoop:
        for (int blockNum = 1; blockNum < totalBlocks; blockNum++) {
            List<Records> recordsInBlock = readRecordsFromBlock(fileName, blockNum);

            for (Records record : recordsInBlock) {
                try {
                    int recordId = Integer.parseInt(record.getId());

                    if (recordId > endId) {
                        // 이미 정렬되어 있으므로, 이후 데이터는 볼 필요 없음
                        break outerLoop;
                    }

                    if (recordId >= startId) {
                        foundRecords.add(record);
                    }
                    // recordId < startId면 스킵만 함 (continue 역할)
                } catch (NumberFormatException e) {
                    System.out.println("Invalid ID format in record: " + record.getId());
                }
            }
        }

        if (foundRecords.isEmpty()) {
            System.out.println("No records found in the given range.");
        } else {
            for (Records record : foundRecords) {
                System.out.println("Record found: " + record);
            }
        }
    }



}
