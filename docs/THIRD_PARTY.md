# 第三方依赖 / Third-party dependencies

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2

自有源码遵循根目录LICENSE；第三方许可证不被本项目非商业许可替代。 / The root LICENSE covers original source only; third-party licenses remain intact.

- Spring Boot/Security/Data、Flyway Community：Apache-2.0；Hibernate ORM：LGPL-2.1-or-later；MySQL Connector/J：GPL-2.0 with Universal FOSS Exception。依据各组件发布的许可使用，不重写第三方版权。
- Vue、Vite、Lucide、qrcode、ESLint、Prettier：MIT；qrcode官方项目 https://github.com/soldair/node-qrcode ，二维码生成仅在本机浏览器执行，不调用远程二维码服务。
- MySQL Community Server容器：GPL-2.0；Nginx：BSD-2-Clause；Eclipse Temurin：GPL-2.0 with Classpath Exception。镜像包含其他OS组件，其通知保留在镜像内。

Package lock pins frontend dependencies; Maven parent/dependency management pins backend components. Dependency redistribution and commercial delivery require reviewing component notices as well as the original-source commercial authorization. No third-party code or asset copyright is replaced.
