/*
Navicat MySQL Data Transfer

Source Server         : users
Source Server Version : 50744
Source Host           : localhost:3306
Source Database       : personal_health

Target Server Type    : MYSQL
Target Server Version : 50744
File Encoding         : 65001

Date: 2025-09-11 18:53:36
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for fabric
-- ----------------------------
DROP TABLE IF EXISTS `fabric`;
CREATE TABLE `fabric` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '面料名称（如：纯棉平纹、羊毛混纺）',
  `type` varchar(50) NOT NULL COMMENT '面料类型（如：棉、羊毛、丝绸、亚麻）',
  `image_url` varchar(255) NOT NULL COMMENT '面料图片URL（存储图片访问地址）',
  `description` varchar(500) DEFAULT NULL COMMENT '面料描述（如：透气性好，适合夏季）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_fabric_type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8 COMMENT='面料库表';

-- ----------------------------
-- Records of fabric
-- ----------------------------
INSERT INTO `fabric` VALUES ('1', '纯棉面料', '棉', 'http://localhost:21090/api/personal-heath/v1.0/file/getFile?fileName=04f86ab纯棉面料.jpg', '透气，舒适，弹性差', '2025-07-31 16:45:50', '2025-07-31 16:45:50');
INSERT INTO `fabric` VALUES ('2', '棉麻', '亚麻', 'http://localhost:21090/api/personal-heath/v1.0/file/getFile?fileName=1753eb1棉麻.webp', '可塑性强，透气', '2025-07-31 16:46:34', '2025-07-31 16:46:34');
INSERT INTO `fabric` VALUES ('3', '丝绸', '丝绸', 'http://localhost:21090/api/personal-heath/v1.0/file/getFile?fileName=0624965丝绸.webp', '光滑，贴身性强', '2025-07-31 16:47:03', '2025-07-31 16:47:03');
INSERT INTO `fabric` VALUES ('4', '涤纶', '混纺', 'http://localhost:21090/api/personal-heath/v1.0/file/getFile?fileName=b8b363b涤纶.webp', '混纺面料，经济实惠', '2025-07-31 16:47:39', '2025-07-31 16:47:39');
