# 智慧烟感后端项目需求

## 技术栈
- Java 17或Java 21
- Spring Boot 3
- Maven
- MySQL 8
- Spring Web
- Spring Data JPA
- Lombok
- Validation

## 当前阶段目标
只实现第一阶段基础后端功能，不实现MQTT、WebSocket、AI视觉、RAG问答、广播联动。

## 第一阶段功能
1. 设备数据上传
2. 传感器数据入库
3. 更新设备最新状态
4. 烟雾浓度阈值判断
5. 超过阈值时生成告警记录
6. 查询设备最新数据
7. 查询设备历史数据
8. 查询告警记录

## 数据库表
- device：设备信息
- sensor_data：传感器历史数据
- alarm_record：告警记录

## 代码要求
- controller只负责接收请求和返回结果
- service负责业务逻辑
- repository负责数据库操作
- entity对应数据库表
- dto用于请求和响应
- 不要把业务逻辑写在controller里
- 先使用HTTP接口模拟硬件上传
- 烟雾阈值暂时写死为0.10