# Summoner Spell `asset_config` 示例

说明：

- 数据库存储的 `asset_config` 字段本质上是 **JSON 字符串**。
- 标准 JSON 不支持注释，所以建议使用 `_comment` 或 `_doc` 字段承载说明。
- 后端实际解析时只读取正式业务字段；说明字段仅供人工阅读。

## 通用模板

```json
{
  "_doc": "召唤师技能运行时配置。标准 JSON 不支持注释，所以这里使用 _doc / _comment 字段说明含义。",
  "skillId": "flash",
  "_comment_skillId": "技能唯一标识，建议与 fish_battle_summoner_spell.spell_id 保持一致。",
  "slot": "summonerD",
  "_comment_slot": "默认槽位。服务端创建运行时技能状态时会按实际 D/F 覆盖。",
  "name": "闪现",
  "_comment_name": "技能显示名称。",
  "initialLevel": 1,
  "_comment_initialLevel": "初始化等级。当前召唤师技能固定为 1。",
  "cast": {
    "type": "target_point",
    "_comment_type": "施法目标类型。可选值：self_cast / target_point / target_unit / directional。",
    "range": 10,
    "_comment_range": "最大施法距离，单位为战斗场景世界坐标。",
    "radius": 0,
    "_comment_radius": "范围技能半径。仅 target_point AOE 技能需要。",
    "width": 0,
    "_comment_width": "线性技能宽度。仅 directional 技能需要。",
    "castTimeMs": 0,
    "_comment_castTimeMs": "前摇时间（毫秒）。0 表示瞬发。",
    "backswingMs": 0,
    "_comment_backswingMs": "后摇时间（毫秒）。",
    "lockMovement": false,
    "_comment_lockMovement": "施法期间是否锁定移动。",
    "targetRules": {
      "allyOnly": false,
      "enemyOnly": false,
      "allowSelf": true,
      "_comment": "目标过滤规则。target_unit / radius 这类选目标技能会使用。"
    }
  },
  "cooldown": {
    "baseMs": 300000,
    "_comment_baseMs": "冷却时间，单位毫秒。若缺失，后端会回退到表字段 cooldown(秒) × 1000。"
  },
  "cost": {
    "resourceType": "none",
    "_comment_resourceType": "资源类型。召唤师技能通常为 none，也支持 mana。",
    "amount": 0,
    "_comment_amount": "资源消耗值。"
  },
  "effects": {
    "onActivate": [],
    "_comment_onActivate": "技能成功释放瞬间执行的效果列表。",
    "onImpact": [],
    "_comment_onImpact": "命中目标时执行的效果列表。当前无投射物时也可直接用于单体效果。",
    "onSuccessCast": [],
    "_comment_onSuccessCast": "释放成功后的附加效果列表。"
  }
}
```

## 闪现示例

```json
{
  "_doc": "闪现：朝目标点瞬移。",
  "skillId": "flash",
  "slot": "summonerD",
  "name": "闪现",
  "initialLevel": 1,
  "cast": {
    "type": "target_point",
    "range": 10,
    "castTimeMs": 0,
    "backswingMs": 0,
    "lockMovement": false
  },
  "cooldown": {
    "baseMs": 300000
  },
  "cost": {
    "resourceType": "none",
    "amount": 0
  },
  "effects": {
    "onActivate": [
      {
        "type": "Teleport",
        "_comment_type": "当前后端已支持 Teleport，会把英雄传送到 targetPoint。"
      }
    ],
    "onImpact": [],
    "onSuccessCast": []
  }
}
```

## 治疗示例

```json
{
  "_doc": "治疗：先治疗自己，再治疗身边队友，并给自己一个短暂加速。",
  "skillId": "heal",
  "slot": "summonerF",
  "name": "治疗",
  "initialLevel": 1,
  "cast": {
    "type": "self_cast",
    "range": 0,
    "castTimeMs": 0,
    "backswingMs": 0,
    "lockMovement": false
  },
  "cooldown": {
    "baseMs": 240000
  },
  "cost": {
    "resourceType": "none",
    "amount": 0
  },
  "effects": {
    "onActivate": [
      {
        "type": "Heal",
        "amount": 120,
        "targetMode": "self",
        "_comment": "给自己回复 120 点生命。"
      },
      {
        "type": "Heal",
        "amount": 90,
        "targetMode": "radius",
        "radius": 5,
        "targetRules": {
          "allyOnly": true,
          "allowSelf": false
        },
        "_comment": "给 5 范围内的友军回复 90 点生命，不包含自己。"
      },
      {
        "type": "ApplyStatus",
        "statusId": "generic_move_speed_up",
        "durationMs": 1000,
        "stacks": 1,
        "targetMode": "self",
        "_comment": "给自己施加一个 1 秒加速状态。"
      }
    ],
    "onImpact": [],
    "onSuccessCast": []
  }
}
```

## 疾跑示例

```json
{
  "_doc": "疾跑：给自己施加持续加速状态。",
  "skillId": "ghost",
  "slot": "summonerD",
  "name": "疾跑",
  "initialLevel": 1,
  "cast": {
    "type": "self_cast",
    "range": 0,
    "castTimeMs": 0,
    "backswingMs": 0,
    "lockMovement": false
  },
  "cooldown": {
    "baseMs": 180000
  },
  "cost": {
    "resourceType": "none",
    "amount": 0
  },
  "effects": {
    "onActivate": [
      {
        "type": "ApplyStatus",
        "statusId": "summoner_ghost_speed_up",
        "durationMs": 10000,
        "stacks": 1,
        "targetMode": "self",
        "_comment": "给自己施加 10 秒移动速度加成。"
      }
    ],
    "onImpact": [],
    "onSuccessCast": []
  }
}
```
