-- =========================================================================
-- 摸鱼大乱斗 游戏配置表 + 初始化数据
-- 配置统一由数据库管理，前端通过 REST API 获取
-- =========================================================================

-- 游戏配置表（单表大JSON模式，config_key 区分不同配置类型）
CREATE TABLE IF NOT EXISTS `fish_battle_config` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key`  VARCHAR(64)  NOT NULL COMMENT '配置唯一标识（如 map_default / game_default）',
    `config_data` LONGTEXT     NOT NULL COMMENT '配置数据（JSON格式，_doc字段为中文注释）',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '配置说明',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态（0禁用/1启用）',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='摸鱼大乱斗游戏配置表';


-- =========================================================================
-- 1. 地图场景配置 (config_key = 'map_default')
-- 包含：地图边界、出生点、建筑（塔/水晶/枢纽）、补血道具、草丛、泉水、
--       小兵属性、桥面视觉、中央遗迹 等全部场景数据
-- =========================================================================
INSERT INTO `fish_battle_config` (`config_key`, `config_data`, `description`) VALUES
('map_default', '{
    "_doc": "战斗地图共享配置（Single Source of Truth）。后端 Java 启动时加载为 BattleMapConfig Bean，前端通过 GET /fishBattle/mapConfig 拉取。修改此文件即可同时影响前后端，无需两边分别改代码。JSON 不支持注释，所有 _doc 字段仅供阅读，程序通过 @JsonIgnoreProperties(ignoreUnknown=true) 自动忽略。",
    "map": {
        "_doc": "地图基础尺寸与可走区域边界",
        "width": 160,
        "depth": 54,
        "bridgeWidth": 40,
        "bridgeLength": 270,
        "playableBounds": {
            "_doc": "英雄/小兵可走区域边界，超出会被钳位",
            "minX": -130,
            "maxX": 130,
            "minZ": -19.6,
            "maxZ": 19.6
        }
    },
    "spawnLayouts": {
        "_doc": "英雄初始出生编队坐标 [x, y, z]，最多 5 人",
        "blue": [
            [
                -125,
                0,
                -5
            ],
            [
                -120,
                0,
                -2
            ],
            [
                -125,
                0,
                0
            ],
            [
                -120,
                0,
                2
            ],
            [
                -125,
                0,
                5
            ]
        ],
        "red": [
            [
                125,
                0,
                -5
            ],
            [
                120,
                0,
                -2
            ],
            [
                125,
                0,
                0
            ],
            [
                120,
                0,
                2
            ],
            [
                125,
                0,
                5
            ]
        ]
    },
    "structures": {
        "_doc": "所有建筑定义。position=[x,y,z]，collisionRadius=碰撞半径，maxHp=最大血量，armor=护甲",
        "towers": [
            {
                "_doc": "蓝队外塔",
                "id": "tower_0",
                "team": "blue",
                "subType": "outer",
                "position": [
                    -25,
                    0,
                    0
                ],
                "collisionRadius": 2.5,
                "maxHp": 3000,
                "armor": 40,
                "attackDamage": 150,
                "attackRange": 20,
                "attackSpeed": 0.63
            },
            {
                "_doc": "蓝队内塔",
                "id": "tower_1",
                "team": "blue",
                "subType": "inner",
                "position": [
                    -55,
                    0,
                    0
                ],
                "collisionRadius": 2.5,
                "maxHp": 4000,
                "armor": 40,
                "attackDamage": 150,
                "attackRange": 20,
                "attackSpeed": 0.63
            },
            {
                "_doc": "蓝队左门牙塔",
                "id": "tower_2",
                "team": "blue",
                "subType": "nexusGuard",
                "position": [
                    -100,
                    0,
                    -5.4
                ],
                "collisionRadius": 2.5,
                "maxHp": 3500,
                "armor": 40,
                "attackDamage": 150,
                "attackRange": 15,
                "attackSpeed": 0.83
            },
            {
                "_doc": "蓝队右门牙塔",
                "id": "tower_3",
                "team": "blue",
                "subType": "nexusGuard",
                "position": [
                    -100,
                    0,
                    5.4
                ],
                "collisionRadius": 2.5,
                "maxHp": 3500,
                "armor": 40,
                "attackDamage": 150,
                "attackRange": 15,
                "attackSpeed": 0.83
            },
            {
                "_doc": "红队外塔",
                "id": "tower_4",
                "team": "red",
                "subType": "outer",
                "position": [
                    25,
                    0,
                    0
                ],
                "collisionRadius": 2.5,
                "maxHp": 3000,
                "armor": 40,
                "attackDamage": 150,
                "attackRange": 20,
                "attackSpeed": 0.63
            },
            {
                "_doc": "红队内塔",
                "id": "tower_5",
                "team": "red",
                "subType": "inner",
                "position": [
                    55,
                    0,
                    0
                ],
                "collisionRadius": 2.5,
                "maxHp": 4000,
                "armor": 40,
                "attackDamage": 150,
                "attackRange": 20,
                "attackSpeed": 0.63
            },
            {
                "_doc": "红队左门牙塔",
                "id": "tower_6",
                "team": "red",
                "subType": "nexusGuard",
                "position": [
                    100,
                    0,
                    -5.4
                ],
                "collisionRadius": 2.5,
                "maxHp": 3500,
                "armor": 40,
                "attackDamage": 150,
                "attackRange": 15,
                "attackSpeed": 0.83
            },
            {
                "_doc": "红队右门牙塔",
                "id": "tower_7",
                "team": "red",
                "subType": "nexusGuard",
                "position": [
                    100,
                    0,
                    5.4
                ],
                "collisionRadius": 2.5,
                "maxHp": 3500,
                "armor": 40,
                "attackDamage": 150,
                "attackRange": 15,
                "attackSpeed": 0.83
            }
        ],
        "towerVisual": {
            "_doc": "防御塔前端视觉参数（模型路径、动画、尺寸）。后端不使用此节。",
            "blue": {
                "outerModelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/blue_tower.glb",
                "outerDestroyedModelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/turrets_destroyed.glb",
                "innerModelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/blue_tower.glb",
                "innerDestroyedModelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/turrets_destroyed.glb",
                "nexusGuardModelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/blue_tower.glb",
                "nexusGuardDestroyedModelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/turrets_destroyed.glb",
                "rotationY": 1.5707963
            },
            "red": {
                "outerModelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/red_tower.glb",
                "outerDestroyedModelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/turrets_destroyed.glb",
                "innerModelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/red_tower.glb",
                "innerDestroyedModelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/turrets_destroyed.glb",
                "nexusGuardModelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/red_tower.glb",
                "nexusGuardDestroyedModelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/turrets_destroyed.glb",
                "rotationY": -1.5707963
            },
            "outer": {
                "targetHeight": 7.6,
                "modelScale": 2.5,
                "groundOffsetY": -8.3,
                "destroyedTargetHeight": 0,
                "destroyedModelScale": 0,
                "destroyedGroundOffsetY": -5,
                "idleClip": "Idle1_Base",
                "deathClip": "Idle1_Base"
            },
            "inner": {
                "targetHeight": 7.6,
                "modelScale": 2.5,
                "groundOffsetY": -8.3,
                "destroyedTargetHeight": 0,
                "destroyedModelScale": 0,
                "destroyedGroundOffsetY": -5,
                "idleClip": "Idle1_Base",
                "deathClip": "Idle1_Base"
            },
            "nexusGuard": {
                "targetHeight": 7.6,
                "modelScale": 2.5,
                "groundOffsetY": -8.3,
                "destroyedTargetHeight": 0,
                "destroyedModelScale": 0,
                "destroyedGroundOffsetY": -5,
                "idleClip": "Idle1_Base",
                "deathClip": "Idle1_Base"
            }
        },
        "inhibitors": [
            {
                "_doc": "蓝队兵营水晶",
                "id": "inhibitor_0",
                "team": "blue",
                "position": [
                    -80,
                    0,
                    0
                ],
                "collisionRadius": 5.5,
                "maxHp": 2500,
                "armor": 20
            },
            {
                "_doc": "红队兵营水晶",
                "id": "inhibitor_1",
                "team": "red",
                "position": [
                    80,
                    0,
                    0
                ],
                "collisionRadius": 5.5,
                "maxHp": 2500,
                "armor": 20
            }
        ],
        "inhibitorVisual": {
            "_doc": "兵营水晶前端视觉参数",
            "blue": {
                "modelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/blue_small_nexus.glb",
                "targetHeight": 5.0,
                "modelScale": 3.2,
                "groundOffsetY": -10.1,
                "rotationY": 0,
                "idleClip": "Idle_Normal1",
                "deathClip": "Idle_Hold"
            },
            "red": {
                "modelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/red_smaill_nexus.glb",
                "targetHeight": 5.0,
                "modelScale": 3.2,
                "groundOffsetY": -10.1,
                "rotationY": 3.1415926,
                "idleClip": "Idle_Normal1",
                "deathClip": "Idle_Hold"
            }
        },
        "nexuses": [
            {
                "_doc": "蓝队水晶枢纽",
                "id": "nexus_0",
                "team": "blue",
                "position": [
                    -110,
                    0,
                    0
                ],
                "collisionRadius": 6.5,
                "maxHp": 5000,
                "armor": 20
            },
            {
                "_doc": "红队水晶枢纽",
                "id": "nexus_1",
                "team": "red",
                "position": [
                    115,
                    0,
                    0
                ],
                "collisionRadius": 6.5,
                "maxHp": 5000,
                "armor": 20
            }
        ],
        "nexusVisual": {
            "_doc": "水晶枢纽前端视觉参数",
            "blueModelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/blue_main_nexus.glb",
            "redModelPath": "https://cdn.xiaojingge.com/3d-battle/models/towers/red_main_nexus.glb",
            "targetHeight": 8.5,
            "modelScale": 2.5,
            "groundOffsetY": -15,
            "blueRotationY": 0,
            "redRotationY": 3.1415926,
            "idleClip": "Idle1_Base",
            "damagedClip": "State2",
            "criticalClip": "State3",
            "deathClip": "Destroyed",
            "damagedThreshold": 0.66,
            "criticalThreshold": 0.33
        }
    },
    "healthRelics": {
        "_doc": "生命遗迹（补血道具）。position=[x,y,z]，healPercent=回复最大血量百分比，pickupRadius=拾取半径，respawnMs=刷新间隔毫秒",
        "items": [
            {
                "id": "relic_0",
                "position": [
                    -10,
                    0,
                    15
                ],
                "healPercent": 0.15,
                "pickupRadius": 2.5,
                "respawnMs": 90000
            },
            {
                "id": "relic_1",
                "position": [
                    10,
                    0,
                    -15
                ],
                "healPercent": 0.15,
                "pickupRadius": 2.5,
                "respawnMs": 90000
            },
            {
                "id": "relic_2",
                "position": [
                    -45,
                    0,
                    -15
                ],
                "healPercent": 0.15,
                "pickupRadius": 2.5,
                "respawnMs": 90000
            },
            {
                "id": "relic_3",
                "position": [
                    45,
                    0,
                    15
                ],
                "healPercent": 0.15,
                "pickupRadius": 2.5,
                "respawnMs": 90000
            }
        ],
        "visual": {
            "_doc": "生命遗迹前端视觉参数",
            "modelPath": "https://cdn.xiaojingge.com/3d-battle/models/other/plant_honeyfruit.glb",
            "targetHeight": 3.2,
            "rotationY": 0,
            "idleClip": null,
            "floatHeight": 0.5,
            "bobAmplitude": 0.3,
            "bobSpeed": 2,
            "ringOuterRadius": 1.2,
            "ringInnerRadius": 0.8
        }
    },
    "bushes": {
        "_doc": "草丛碰撞体（后端用于视野判定）与前端视觉参数。center=[x,z]，size=[宽,深]",
        "colliders": [
            {
                "_doc": "左上草丛",
                "center": [
                    -40,
                    -18
                ],
                "size": [
                    8,
                    3.5
                ]
            },
            {
                "_doc": "左下草丛",
                "center": [
                    -40,
                    18
                ],
                "size": [
                    8,
                    3.5
                ]
            },
            {
                "_doc": "中上草丛",
                "center": [
                    0,
                    -18
                ],
                "size": [
                    10,
                    4
                ]
            },
            {
                "_doc": "中下草丛",
                "center": [
                    0,
                    18
                ],
                "size": [
                    10,
                    4
                ]
            },
            {
                "_doc": "右上草丛",
                "center": [
                    40,
                    -18
                ],
                "size": [
                    8,
                    3.5
                ]
            },
            {
                "_doc": "右下草丛",
                "center": [
                    40,
                    18
                ],
                "size": [
                    8,
                    3.5
                ]
            }
        ],
        "visual": {
            "_doc": "草丛前端视觉参数（三组草丛区域：left/center/right）",
            "left": {
                "x": -40,
                "wallInset": 2,
                "size": [
                    8,
                    2.8,
                    3.5
                ],
                "modelPath": null,
                "targetHeight": 2.6,
                "rotationY": 0,
                "idleClip": null
            },
            "center": {
                "x": 0,
                "wallInset": 2,
                "size": [
                    10,
                    3,
                    4
                ],
                "modelPath": null,
                "targetHeight": 2.8,
                "rotationY": 0,
                "idleClip": null
            },
            "right": {
                "x": 40,
                "wallInset": 2,
                "size": [
                    8,
                    2.8,
                    3.5
                ],
                "modelPath": null,
                "targetHeight": 2.6,
                "rotationY": 0,
                "idleClip": null
            }
        },
        "grass": {
            "_doc": "实例化草片渲染参数",
            "modelPath": "https://cdn.xiaojingge.com/3d-battle/models/other/grass.glb",
            "count": 700,
            "scaleMin": 0.25,
            "scaleMax": 0.60,
            "heightScale": 2.0,
            "swayIntensity": 0.1
        }
    },
    "minions": {
        "_doc": "小兵配置。spawnIntervalMs=出兵间隔毫秒，spawnPoints=出生X坐标，zOffsets=纵向排列偏移",
        "spawnIntervalMs": 25000,
        "melee": {
            "_doc": "近战兵：高 HP、近距离攻击",
            "hp": 420,
            "attackDamage": 42,
            "attackRange": 1.35,
            "acquisitionRange": 9.0,
            "attackCooldownMs": 1200,
            "moveSpeed": 2.4,
            "collisionRadius": 0.45,
            "modelUrl": {
                "blue": "https://cdn.xiaojingge.com/3d-battle/models/other/melee_minion_order.glb",
                "red": "https://cdn.xiaojingge.com/3d-battle/models/other/melee_minion_chaos.glb"
            },
            "visual": {
                "_doc": "近战兵前端视觉参数",
                "blue": {
                    "targetHeight": 1.8,
                    "modelScale": 1.2,
                    "groundOffsetY": 0,
                    "rotationY": 0
                },
                "red": {
                    "targetHeight": 1.8,
                    "modelScale": 1.2,
                    "groundOffsetY": 0,
                    "rotationY": 0
                },
                "idleClip": "Idle1",
                "runClip": "Run",
                "attackClip": "Attack3",
                "deathClip": "minion_melee_death3"
            }
        },
        "caster": {
            "_doc": "远程兵（Caster）：低 HP、远距离攻击、带弹道",
            "hp": 280,
            "attackDamage": 50,
            "attackRange": 6.0,
            "acquisitionRange": 9.0,
            "attackCooldownMs": 1600,
            "moveSpeed": 2.4,
            "collisionRadius": 0.4,
            "modelUrl": {
                "blue": "https://cdn.xiaojingge.com/3d-battle/models/other/ranged_minion_order.glb",
                "red": "https://cdn.xiaojingge.com/3d-battle/models/other/ranged_minion_chaos.glb"
            },
            "visual": {
                "_doc": "远程兵前端视觉参数",
                "blue": {
                    "targetHeight": 1.6,
                    "modelScale": 1.5,
                    "groundOffsetY": 0,
                    "rotationY": 0
                },
                "red": {
                    "targetHeight": 1.6,
                    "modelScale": 1.5,
                    "groundOffsetY": 0,
                    "rotationY": 0
                },
                "idleClip": "Idle1",
                "runClip": "Run",
                "attackClip": "Attack1",
                "deathClip": "Death"
            }
        },
        "spawnPoints": {
            "blue": {
                "meleeX": -105,
                "casterX": -107
            },
            "red": {
                "meleeX": 105,
                "casterX": 107
            }
        },
        "zOffsets": [
            -2.0,
            0,
            2.0
        ],
        "healthBar": {
            "_doc": "小兵头顶血条参数",
            "width": 2,
            "height": 0.42,
            "offsetY": 3.2
        }
    },
    "fountain": {
        "blue": {
            "position": [
                -130,
                0,
                0
            ],
            "visual": {
                "modelPath": "https://cdn.xiaojingge.com/3d-battle/models/heroes/ahri/tft17_god_ahri.glb",
                "targetHeight": 5.8,
                "rotationY": 0,
                "idleClip": "Idle1"
            }
        },
        "red": {
            "position": [
                130,
                0,
                0
            ],
            "visual": {
                "modelPath": "https://cdn.xiaojingge.com/3d-battle/models/heroes/ahri/ahri_(tft_set_11).glb",
                "targetHeight": 5.8,
                "rotationY": 0,
                "idleClip": "Celebration"
            }
        }
    },
    "ruins": {
        "_doc": "桥面正中心的悬浮冰晶遗迹装饰",
        "modelPath": "https://cdn.xiaojingge.com/3d-battle/models/other/npc_worldscup.glb",
        "targetHeight": 3.0,
        "rotationY": 0,
        "idleClip": "Idle"
    }
}', '地图场景完整配置（地图边界+建筑+草丛+泉水+小兵+桥面视觉等）');


-- =========================================================================
-- 2. 游戏主配置 (config_key = 'game_default')
-- 包含：相机、输入、渲染、音效、视野、HUD、表情、英雄默认动画、联机、调试
-- =========================================================================
INSERT INTO `fish_battle_config` (`config_key`, `config_data`, `description`) VALUES
('game_default', '{
  "_doc": "游戏主配置（相机/渲染/音效/视野/HUD/输入/表情/英雄默认动画/联机/调试）",

  "camera": {
    "_doc": "相机配置",
    "fov": 50,
    "near": 0.1,
    "far": 500,
    "defaultLocked": true,
    "initialTarget": [0, 0, 0],
    "baseOffset": [-22, 44, 26],
    "initialZoom": 25,
    "introEnabled": true,
    "introStartZoom": 50,
    "introSpeed": 1,
    "lockToggleKey": "KeyY",
    "dragUnlocksCamera": true,
    "minZoom": 10,
    "maxZoom": 35,
    "zoomStep": 2,
    "dragPanSpeed": 0.01,
    "edgePanMargin": 24,
    "edgePanSpeed": 16,
    "enableEdgePan": true,
    "targetLerp": 8,
    "positionLerp": 7,
    "bounds": {
      "_doc": "自由镜头边界",
      "minX": -135,
      "maxX": 135,
      "minZ": -26,
      "maxZ": 26
    }
  },

  "input": {
    "_doc": "输入控制配置",
    "rightClickIndicator": {
      "_doc": "右键移动指示器配置",
      "durationMs": 850,
      "cursor": {
        "_doc": "鼠标指针配置",
        "enabled": true,
        "defaultPath": "/cursors/summoner/normal.cur",
        "clickPath": "/cursors/summoner/link.cur",
        "hotspotX": 6,
        "hotspotY": 4,
        "clickFeedbackMs": 160,
        "fallback": "auto"
      },
      "ground": {
        "_doc": "地面落点标识配置",
        "modelPath": null,
        "targetHeight": 1.2,
        "animationClipName": null,
        "offsetY": 0.045,
        "outerRadius": 0.55,
        "innerRadius": 0.54,
        "centerRadius": 0.2,
        "rippleScale": 1.38,
        "rippleOuterRadius": 0.6,
        "rippleFarOuterRadius": 0.7,
        "color": "0xa9d8f0",
        "emissive": "0x5cb6df",
        "emissiveIntensity": 1.2,
        "highlight": "0xeaf7ff"
      }
    },
    "spectator": {
      "_doc": "导播模式快捷键",
      "toggleModeKey": "KeyV",
      "previousTargetKey": "BracketLeft",
      "nextTargetKey": "BracketRight",
      "focusMeKey": "KeyF"
    }
  },

  "render": {
    "_doc": "渲染质量配置（low/medium/high/ultra）",
    "qualityPreset": "low",
    "dpr": [1, 1],
    "enableShadows": false,
    "shadowMapSize": 512,
    "enableBloom": false,
    "bloomIntensity": 0.28,
    "bloomThreshold": 0.9,
    "bloomSmoothing": 0.35,
    "enableSnow": false,
    "snowCount": 0,
    "toneMappingExposure": 1,
    "heroTargetHeight": 2.6,
    "showPerfMonitor": false
  },

  "audio": {
    "_doc": "空间音效配置",
    "enabled": true,
    "remoteVoiceVolumeMultiplier": 0.3,
    "innerRadius": 15,
    "outerRadius": 60,
    "distanceModel": "linear",
    "rolloffFactor": 1,
    "maxAudioDistance": 65
  },

  "vision": {
    "_doc": "简易视野配置",
    "enabled": true,
    "sightRadius": 35,
    "hysteresis": 3
  },

  "hud": {
    "_doc": "HUD显示配置",
    "overhead": {
      "_doc": "头顶HUD纹理与布局参数",
      "textureWidth": 420,
      "textureHeight": 164,
      "nameFontSize": 30,
      "secondaryNameFontSize": 28,
      "hpValueFontSize": 20,
      "mpValueFontSize": 15,
      "levelFontSize": 20,
      "hpSegments": 24,
      "hpSpritePositionY": 5.0,
      "hpSpriteScale": [5.4, 2.26, 1],
      "emoteSpritePositionY": 8.0,
      "emoteSpriteScale": [1.5, 1.5, 1]
    }
  },

  "emotes": {
    "_doc": "表情系统配置",
    "definitions": [
      {"id":"poro",      "emoji":"🐾", "label":"魄罗", "color":"#d8f3ff", "accent":"#79d9ff"},
      {"id":"laugh",     "emoji":"😄", "label":"大笑", "color":"#ffe19a", "accent":"#ffb348"},
      {"id":"cry",       "emoji":"😭", "label":"哭泣", "color":"#b8d9ff", "accent":"#6aa7ff"},
      {"id":"angry",     "emoji":"😠", "label":"愤怒", "color":"#ffb2a8", "accent":"#ff6a57"},
      {"id":"nice",      "emoji":"👍", "label":"Nice", "color":"#b8ffd6", "accent":"#4de191"},
      {"id":"love",      "emoji":"❤️",  "label":"爱心", "color":"#ffc3db", "accent":"#ff6ba3"},
      {"id":"surprised", "emoji":"😲", "label":"惊讶", "color":"#f8edb6", "accent":"#e7c84f"},
      {"id":"tease",     "emoji":"😜", "label":"调皮", "color":"#d9c8ff", "accent":"#a56eff"}
    ],
    "worldDisplayDurationMs": 1800,
    "wheel": {
      "_doc": "表情轮盘参数",
      "size":380, "outerRadius":116, "innerRadius":42, "selectionOverflow":26,
      "itemSize":46, "emojiFontSize":26, "labelFontSize":9,
      "centerTitleFontSize":9, "centerEmojiFontSize":22, "centerHintFontSize":10,
      "voiceRingOuterRadius":158, "voiceRingItemSize":40,
      "voiceRingEmojiFontSize":20, "voiceRingLabelFontSize":12, "voiceRingSelectionOverflow":30
    },
    "announcement": {
      "_doc": "表情发送侧栏轮播提示",
      "enabled":true, "side":"right", "horizontalOffsetPx":16, "topOffsetPx":80,
      "visibleCount":4, "maxQueue":8, "itemDisplayDurationMs":3000,
      "enterAnimationMs":300, "exitAnimationMs":400, "itemGapPx":6,
      "paddingX":12, "paddingY":6, "fontSize":13, "emojiFontSize":18
    }
  },

  "heroes": {
    "_doc": "英雄全局默认配置（动画模板/动作映射等，非单个英雄资产）",
    "defaultAnimations": {
      "_doc": "默认动画状态别名表（把模型片段名归一到统一标准状态）",
      "stateAliases": {
        "idle":    ["Idle", "idle", "Idle1", "stand"],
        "standby": ["Standby", "standby", "wait", "relax", "rest"],
        "run":     ["Run2", "Run", "run", "walk", "move"],
        "attack":  ["Attack1", "attack", "hit"],
        "cast":    ["Spell1A", "Spell", "cast", "spell", "skill"],
        "death":   ["Death", "death", "die", "dead"]
      },
      "stateClips": {
        "_doc": "标准状态到首选动画片段名的映射",
        "idle": "Idle",
        "standby": "Standby",
        "run": "Run",
        "attack": "Attack",
        "cast": "Cast",
        "death": "Death"
      },
      "actionClips": {
        "_doc": "业务动作槽位到动画片段名的映射",
        "basicAttack": "Attack",
        "recall": "Recall"
      },
      "actionPlaybackRates": {
        "_doc": "动作播放速率倍率",
        "basicAttack": 1,
        "recall": 1
      },
      "actionDurationsMs": {
        "_doc": "动作持续时间（毫秒）",
        "basicAttack": 450,
        "recall": 1200
      },
      "actionMovementLocks": {
        "_doc": "动作期间是否锁定移动",
        "basicAttack": true,
        "recall": true
      },
      "standbyDelayMs": 15000
    }
  },

  "multiplayer": {
    "_doc": "联机配置",
    "enabled": true,
    "simulationTickRate": 20,
    "snapshotRate": 20,
    "interpolationDelayMs": 100,
    "tickDt": 0.05,
    "renderDelayMs": 100,
    "positionSmoothing": 12,
    "rotationSmoothing": 16,
    "maxBufferedSnapshots": 8,
    "showDiagnosticsPanel": false,
    "showFps": false,
    "disconnectMessage": "已离开战场"
  },

  "debug": {
    "_doc": "调试配置",
    "worldCoordinates": {
      "_doc": "世界坐标调试标签",
      "enabled": false,
      "toggleKey": "KeyG",
      "showChampions": true,
      "showStructures": true,
      "precision": 2,
      "offsetY": 5.6,
      "fontSize": 28,
      "fontFamily": "黑体",
      "distanceFactor": 12
    },
    "animationHotkeys": {
      "_doc": "调试用动画热键",
      "KeyA": "basicAttack",
      "KeyB": "recall"
    },
    "spectator": {"showPanel": false},
    "freeCamera": {"enabled": false, "toggleKey": "KeyO"}
  }
}', '游戏主配置（相机/渲染/音效/视野/HUD/输入/表情/英雄默认动画/联机/调试）');


-- =========================================================================
-- 3. 更新 ashe 英雄的 asset_config
-- =========================================================================
UPDATE `fish_battle_hero` SET `asset_config` = '{
  "_doc": "艾希英雄展示资产配置",
  "label": "寒冰射手",
  "modelScale": 1.5,
  "groundOffsetY": -0.05,
  "overhead": {
    "_doc": "头顶HUD挂点覆盖",
    "hpSpritePositionY": 6.0,
    "emoteSpritePositionY": 9.0
  },
  "animations": {
    "_doc": "艾希动画配置",
    "stateAliases": {
      "idle": ["Idle", "idle", "Idle1"],
      "standby": ["Standby", "standby"],
      "run": ["Run", "run", "walk"],
      "attack": ["Attack1", "attack"],
      "death": ["Death", "death"]
    },
    "stateClips": {
      "idle": "Idle1",
      "standby": "Standby",
      "run": "Run",
      "attack": "Attack1",
      "death": "Death"
    },
    "actionClips": {
      "basicAttack": "Attack1",
      "recall": "Recall"
    },
    "actionPlaybackRates": {
      "basicAttack": 1,
      "recall": 1
    },
    "actionDurationsMs": {
      "basicAttack": 450,
      "recall": 1200
    },
    "actionMovementLocks": {
      "basicAttack": true,
      "recall": true
    },
    "standbyDelayMs": 15000
  },
  "voices": {
    "_doc": "艾希语音配置（暂为空，后续补充语音文件）",
    "volume": 0.5
  }
}' WHERE `hero_id` = 'ashe';
