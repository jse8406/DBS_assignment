과제 조건
search key는 null값을 갖지 않도록 한다.

metadata의 저장 및 access:
필드의 개수, 필드의 고정 길이 자료형, 필드의 순서 등
sequence 파일 생성 시 관련 메타데이터를 JDBC를 통해 mysql의 metadata access에 접근이 가능

가장 중요!!!!
RDB 저장 시스템의 block I/O는 일정 크기의 block 단위로 실행되어야함.

Block Size는 알아서...설정을 언제든지 바꿀 수 있도록 하는 것이 좋겠음.
Blocking factor <= 3정도 되도록, 하나의 block에 3개의 record가
저장될 정도로 해서 테스트할 것

삽입 레코드 수 총 10개, 총 레코드 블록 수 3,4개, 헤더 블록 1개

파일 레코드 구조는 비트맵이 젤 앞으로 오는 가변길이로 구성
파일 구조는 순차 파일 구조로, 맨 앞에 헤더 블록도 있음

search key 기반으로 min < key < max, 1 < key < 2 등이 실행될 수 있도록
meta data 는 필드의 갯수, 각 필드의 길이, 필드의 이름, 필드의 순서 등
헤더 블록에 meta데이터 같이 저장할 것.(metadata는 JDBC에서 제공해줌)


column은 총 3개로 각각의 이름은 id, code, tag로 각각 5 4 3 바이트로 결정. 예시)
 00001;A100;aab
nullbimap, field1, field2, field3, pointer
1 5 4 3 2 = 15
Record : 15 bytes

00001;A100;aab
00002;null;xxt
00003;null;null
00004;A100;aab

테스트 데이터는 각각 13, 9, 6, 13 바이트로 총 41 (test)
Header : 18 bytes
Block : 3~4 records = 50byte로 하자

---------------------------------------------------------------------------------

Error : readAndDeserializeFromBinary함수에서 읽으려고 설정한 파일 크기가 저장된 파일보다
크면 없는 부분을 읽는거나 마찬가지이기 때문에 null 에러남.

search record든 search field든 처음에 header를 읽고 시작해야하는 것은 똑같음

15 11 8 15
15 11 12
15 11 15 8
15 12 11
15


필드 검색  : 파일명, 필드 입력 시 해당 파일에 해당 필드 다 가져오기 f1 code => f1파일 code값 다 가져옴
-완-
레코드 검색 : 파일명, search key 값의 범위를 주면 그 범위 안의 레코드들 가져오기. MIN <= A <= MAX 다 가져옴