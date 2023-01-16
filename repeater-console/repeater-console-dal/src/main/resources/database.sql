create DATABASE IF not EXISTS repeater DEFAULT CHARSET utf8 COLLATE utf8_general_ci;

drop table IF EXISTS record;
create table record (
                        id bigint(20) not null AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
                        gmt_create datetime not null COMMENT '创建时间',
                        gmt_record datetime not null comment '录制时间',
                        app_name varchar(255) not null COMMENT '应用名',
                        environment varchar(255) not null COMMENT '环境信息',
                        host varchar(36) not null COMMENT '机器IP',
                        trace_id varchar(32) not null COMMENT '链路追踪ID',
                        entrance_desc varchar(2000) not null COMMENT '链路追踪ID',
                        wrapper_record longtext not null COMMENT '记录序列化信息',
                        request longtext not null COMMENT '请求参数JSON',
                        response longtext not null COMMENT '返回值JSON'
) ENGINE = InnoDB COMMENT = '录制信息' DEFAULT CHARSET = utf8 AUTO_INCREMENT = 1;

drop table IF EXISTS replay;
create table replay (
                        id bigint(20) not null AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
                        gmt_create datetime not null COMMENT '创建时间',
                        gmt_modified datetime not null comment '修改时间',
                        app_name varchar(255) not null COMMENT '应用名',
                        environment varchar(255) not null COMMENT '环境信息',
                        ip varchar(36) not null COMMENT '机器IP',
                        repeat_id varchar(32) not null COMMENT '回放ID',
                        status tinyint not null COMMENT '回放状态',
                        trace_id varchar(32) COMMENT '链路追踪ID',
                        cost bigint(20) COMMENT '回放耗时',
                        diff_result longtext COMMENT 'diff结果',
                        response longtext COMMENT '回放结果',
                        mock_invocation longtext COMMENT 'mock过程',
                        success bit COMMENT '是否回放成功',
                        record_id bigint(20) COMMENT '外键'
) ENGINE = InnoDB COMMENT = '回放信息' DEFAULT CHARSET = utf8 AUTO_INCREMENT = 1;

drop table IF EXISTS module_info;
create table module_info (
                             id bigint(20) not null AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
                             gmt_create datetime not null COMMENT '创建时间',
                             gmt_modified datetime not null comment '修改时间',
                             app_name varchar(255) not null COMMENT '应用名',
                             environment varchar(255) not null COMMENT '环境信息',
                             ip varchar(36) not null COMMENT '机器IP',
                             port varchar(12) not null COMMENT '链路追踪ID',
                             version varchar(128) not null COMMENT '模块版本号',
                             status varchar(36) not null COMMENT '模块状态',
                             ext varchar(256) default '' COMMENT '扩展信息'
) ENGINE = InnoDB COMMENT = '在线模块信息' DEFAULT CHARSET = utf8 AUTO_INCREMENT = 1;

drop table IF EXISTS module_config;
create table module_config (
                               id bigint(20) not null AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
                               gmt_create datetime not null COMMENT '创建时间',
                               gmt_modified datetime not null comment '录制时间',
                               app_name varchar(255) not null COMMENT '应用名',
                               environment varchar(255) not null COMMENT '环境信息',
                               config longtext not null COMMENT '配置信息'
) ENGINE = InnoDB COMMENT = '模块配置信息' DEFAULT CHARSET = utf8 AUTO_INCREMENT = 1;