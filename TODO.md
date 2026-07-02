# TODO

## 功能验证

- [ ] **出生点覆盖层** — 进游戏开全屏地图，Toggle ON 看 spawn 有无覆盖层
- [ ] **探索新区块** — 跑远后看新区块是否随 ChunkEvent.Load 自动加入渲染
- [ ] **维度切换** — 去下界/末地再回主世界，覆盖层需恢复正常
- [ ] **互斥切换** — 多个 overlay 之间切换，每次只有一个 ON

## 功能增强

- [ ] **Toggle 按钮图标** — 3 个 PNG 纹理文件缺失（`textures/gui/overlay_rock_layers.png` 等），按钮显示紫黑格
- [ ] **实时更新** — 新区块加载后需重新 Toggle 才能看到。可加服务端 `CacheUpdated` 通知或 DisplayUpdateEvent 刷新
- [ ] **持久化缓存** — 游戏重启后出生点需重新 warmup。后续可考虑存档级持久化

## 代码改进

- [ ] `JMTFCServerPlugin` 残留无用的 `@SubscribeEvent` import
- [ ] 稳定后把部分 INFO 日志降为 DEBUG（warmup 每次请求都打）
