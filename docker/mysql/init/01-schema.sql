SET NAMES utf8mb4;

USE meter;

-- 1. 건물
CREATE TABLE building (
    buildingId      BIGINT        NOT NULL AUTO_INCREMENT,
    buildingCode    VARCHAR(20)   NOT NULL COMMENT '외부 식별자 (예: A)',
    name            VARCHAR(100)  NOT NULL COMMENT '본관 A동',
    contractKw      DECIMAL(10,2) NOT NULL COMMENT '계약전력 (kW)',
    createdAt       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (buildingId),
    UNIQUE KEY uqBuildingCode (buildingCode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='건물';

-- 2. 디바이스 (계량기) + 최신 상태
CREATE TABLE device (
    deviceId    BIGINT       NOT NULL AUTO_INCREMENT,
    buildingId  BIGINT       NOT NULL,
    deviceCode  VARCHAR(30)  NOT NULL COMMENT 'MQTT 메시지의 meterId (예: AHU-01)',
    name        VARCHAR(100) NOT NULL COMMENT '공조기 1호',
    category    VARCHAR(20)  NOT NULL COMMENT '수전반/공조/조명 등',
    isMain      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '건물 전체를 재는 메인 계량기 여부',
    createdAt   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (deviceId),
    UNIQUE KEY uqDeviceCode (deviceCode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='디바이스';

-- 3. MQTT 원본 값 (1분, 디바이스당 1건)
-- PK 를 (deviceId, measuredAt) 으로 두어 중복 메시지는 upsert 로 흡수하고, 기기별 기간 조회를 빠르게 한다.
CREATE TABLE rawReading (
    deviceId        BIGINT        NOT NULL,
    measuredAt      DATETIME      NOT NULL COMMENT '메시지의 ts',
    cumulativeKwh   DECIMAL(14,3) NOT NULL COMMENT '누적사용량',
    instantKw       DECIMAL(10,2) NOT NULL COMMENT '순시전력',
    seq             BIGINT        NULL     COMMENT '계량기 전송 순번. 빠진 메시지 확인용',
    meterStatus     VARCHAR(10)   NOT NULL DEFAULT 'OK',
    errorCodes      VARCHAR(255)  NULL,
    runState        VARCHAR(10)   NOT NULL DEFAULT 'UNKNOWN',
    receivedAt      DATETIME(3)   NOT NULL COMMENT '서버 수신 시각. 지연 도착 확인용',
    PRIMARY KEY (deviceId, measuredAt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQTT 원본 값';

-- 4. 15분 통계
CREATE TABLE usageStat15m (
    deviceId            BIGINT        NOT NULL,
    periodStart         DATETIME      NOT NULL COMMENT '구간 시작 (예: 14:15). 구간은 [시작, 시작+15분)',
    startCumulativeKwh  DECIMAL(14,3) NULL     COMMENT '구간 시작 시점 누적값',
    endCumulativeKwh    DECIMAL(14,3) NULL     COMMENT '구간 끝 시점 누적값',
    usageKwh            DECIMAL(12,3) NULL     COMMENT '사용량 = 끝 누적값 - 시작 누적값',
    avgKw               DECIMAL(10,2) NULL     COMMENT '평균전력 = 순시전력 표본 평균. 이 값이 수요전력',
    maxKw               DECIMAL(10,2) NULL     COMMENT '구간 내 최대 순시전력',
    minKw               DECIMAL(10,2) NULL     COMMENT '구간 내 최소 순시전력',
    sampleCount         SMALLINT      NOT NULL DEFAULT 0,
    expectedCount       SMALLINT      NOT NULL DEFAULT 15,
    quality             VARCHAR(10)   NOT NULL COMMENT 'OK/PARTIAL/MISSING/FAULT/MAINT',
    createdAt           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (deviceId, periodStart)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='15분 사용량 통계';

-- 5. 1시간 통계 (usageStat15m 4건을 합쳐서 생성)
CREATE TABLE usageStat1h (
    deviceId            BIGINT        NOT NULL,
    periodStart         DATETIME      NOT NULL COMMENT '구간 시작 (예: 13:00)',
    startCumulativeKwh  DECIMAL(14,3) NULL,
    endCumulativeKwh    DECIMAL(14,3) NULL,
    usageKwh            DECIMAL(12,3) NULL     COMMENT '15분 사용량 합계',
    avgKw               DECIMAL(10,2) NULL     COMMENT '표본 수로 가중한 평균전력',
    maxKw               DECIMAL(10,2) NULL     COMMENT '최대 순시전력',
    minKw               DECIMAL(10,2) NULL,
    peakDemandKw        DECIMAL(10,2) NULL     COMMENT '구간 내 최대 15분 평균전력 (최대수요전력)',
    sampleCount         SMALLINT      NOT NULL DEFAULT 0,
    expectedCount       SMALLINT      NOT NULL DEFAULT 60,
    quality             VARCHAR(10)   NOT NULL,
    createdAt           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (deviceId, periodStart)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='1시간 사용량 통계';

-- 6. 일 통계 (usageStat1h 24건을 합쳐서 생성)
CREATE TABLE usageStat1d (
    deviceId            BIGINT        NOT NULL,
    statDate            DATE          NOT NULL,
    startCumulativeKwh  DECIMAL(14,3) NULL,
    endCumulativeKwh    DECIMAL(14,3) NULL,
    usageKwh            DECIMAL(12,3) NULL,
    avgKw               DECIMAL(10,2) NULL,
    maxKw               DECIMAL(10,2) NULL,
    minKw               DECIMAL(10,2) NULL,
    peakDemandKw        DECIMAL(10,2) NULL     COMMENT '그날 최대수요전력',
    peakDemandAt        DATETIME      NULL     COMMENT '최대수요전력이 나온 15분 구간 시작',
    sampleCount         INT           NOT NULL DEFAULT 0,
    expectedCount       INT           NOT NULL DEFAULT 1440,
    quality             VARCHAR(10)   NOT NULL,
    createdAt           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (deviceId, statDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='일 사용량 통계';
