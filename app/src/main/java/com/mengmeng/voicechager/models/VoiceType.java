package com.mengmeng.voicechager.models;

public enum VoiceType {
    CHONGCHONG("chongchong", "虫虫-温柔女声"),
    XIAOWANZI("xiaowanzi", "小丸子-可爱女童"),
    CHAOGE("chaoge", "超哥-磁性男声"),
    NANNAN("nannan", "楠楠-可爱男童"),
    PENGFEI("pengfei", "小鹏-成熟男声"),
    QIGE("qige", "七哥-磁性男声"),
    XIAOSONG("xiaosong", "宋宝宝-亲切男声"),
    XIAOYAOZI("xiaoyaozi", "逍遥子-时尚男声"),
    YIFEI("yifei", "一菲-甜美女声"),
    CHENGCHENG("chengcheng", "程程-时尚女声"),
    XIAOYUAN("xiaoyuan", "小媛-时尚女声");

    private final String code;
    private final String description;

    VoiceType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
} 