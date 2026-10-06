SET NAMES utf8mb4;

USE meter;

-- 테스트용 건물/디바이스. 여러 번 실행해도 같은 결과가 되도록 upsert 로 넣는다.

INSERT INTO building (buildingCode, name, contractKw) VALUES
    ('A', '본관 A동', 500.00),
    ('B', '별관 B동', 200.00)
AS new
ON DUPLICATE KEY UPDATE name = new.name, contractKw = new.contractKw;

INSERT INTO device (buildingId, deviceCode, name, category, isMain) VALUES
    ((SELECT buildingId FROM building WHERE buildingCode = 'A'), 'MAIN-A',   '본관 A동 메인 수전반', '수전반', 1),
    ((SELECT buildingId FROM building WHERE buildingCode = 'A'), 'AHU-01',   '공조기 1호',          '공조',   0),
    ((SELECT buildingId FROM building WHERE buildingCode = 'A'), 'AHU-02',   '공조기 2호',          '공조',   0),
    ((SELECT buildingId FROM building WHERE buildingCode = 'A'), 'LIGHT-01', '본관 1층 조명',       '조명',   0),
    ((SELECT buildingId FROM building WHERE buildingCode = 'B'), 'MAIN-B',   '별관 B동 메인 수전반', '수전반', 1),
    ((SELECT buildingId FROM building WHERE buildingCode = 'B'), 'ELEV-01',  '엘리베이터 1호기',    '동력',   0)
AS new
ON DUPLICATE KEY UPDATE buildingId = new.buildingId, name = new.name, category = new.category, isMain = new.isMain;
